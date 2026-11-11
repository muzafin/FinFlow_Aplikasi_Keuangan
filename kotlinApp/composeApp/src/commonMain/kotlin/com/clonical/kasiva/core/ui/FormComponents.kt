package com.clonical.kasiva.core.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun Field(label: String, value: String, change: (String) -> Unit, modifier: Modifier = Modifier, singleLine: Boolean = true, enabled: Boolean = true, readOnly: Boolean = false, keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
          error: String? = null, visualTransformation: VisualTransformation = VisualTransformation.None) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = FFType.bodyMain, fontWeight = FontWeight.Medium, color = FFColors.onSurfaceVariant)
        OutlinedTextField(value, change, modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }, singleLine = singleLine, enabled = enabled, readOnly = readOnly, keyboardOptions = keyboardOptions,
            isError = error != null, visualTransformation = visualTransformation,
            supportingText = error?.let { { Text(it, modifier = Modifier.testTag("field-error")) } },
            shape = RoundedCornerShape(16.dp), textStyle = FFType.bodyMain,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = FFColors.surface, focusedContainerColor = FFColors.surface,
                unfocusedBorderColor = Color.Transparent, focusedBorderColor = FFColors.primary,
                errorContainerColor = FFColors.surface,
            ))
    }
}

/**
 * Field nominal: keypad angka, hanya menerima digit (+ satu pemisah desimal bila mata uangnya punya
 * desimal) dan menampilkan pemisah ribuan gaya Indonesia (`300.000,50`). Nilai mentah yang disimpan
 * tetap format [com.clonical.kasiva.core.util.minorAmount] (`300000.50`).
 */
@Composable
fun AmountField(label: String, value: String, change: (String) -> Unit, currency: String, modifier: Modifier = Modifier,
                enabled: Boolean = true, error: String? = null) {
    Field(label, value, { change(sanitizeAmountInput(it, currency)) }, modifier, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        error = error, visualTransformation = GroupedAmountTransformation)
}

/** Buang karakter selain digit; `,`/`.` pertama menjadi pemisah desimal `.` (dibatasi jumlah desimal mata uang). */
fun sanitizeAmountInput(input: String, currency: String): String {
    val digits = com.clonical.kasiva.core.util.currencyDigits(currency)
    // Teks bergaya ribuan ("1.500.000", "25.000", "12.500,50") — biasanya hasil tempel — dinormalkan dulu (P3-03).
    val trimmed = input.trim()
    val groupedLike = trimmed.count { it == '.' } > 1 || trimmed.count { it == ',' } > 1 || ('.' in trimmed && ',' in trimmed) ||
        (digits != 3 && Regex("[0-9]+[.,][0-9]{3}").matches(trimmed))
    val source = if (groupedLike) com.clonical.kasiva.core.util.normalizeAmountText(trimmed, digits).orEmpty().removePrefix("-") else input
    val out = StringBuilder()
    var seenSeparator = false
    var fraction = 0
    for (c in source) {
        when {
            c.isDigit() && c in '0'..'9' -> if (!seenSeparator) out.append(c) else if (fraction < digits) { out.append(c); fraction++ }
            // Mata uang tanpa desimal (JPY): pemisah & digit sesudahnya dibuang, bukan digabung ke angka bulat.
            (c == '.' || c == ',') && !seenSeparator -> { seenSeparator = true; if (digits > 0) { if (out.isEmpty()) out.append('0'); out.append('.') } }
        }
    }
    // Buang nol di depan ("007" → "7") tapi pertahankan "0" dan "0.x".
    val text = out.toString()
    val whole = text.substringBefore('.').trimStart('0').ifEmpty { if (text.isEmpty()) "" else "0" }
    return if (seenSeparator && digits > 0) whole + "." + text.substringAfter('.') else whole
}

/** Tampilkan `1234567.5` sebagai `1.234.567,5` tanpa mengubah nilai mentah. */
object GroupedAmountTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val whole = raw.substringBefore('.')
        val out = StringBuilder()
        val origToTrans = IntArray(raw.length + 1)
        val transToOrig = ArrayList<Int>()
        raw.forEachIndexed { i, c ->
            if (i in 1 until whole.length && (whole.length - i) % 3 == 0) { out.append('.'); transToOrig += i }
            origToTrans[i] = out.length
            out.append(if (c == '.') ',' else c)
            transToOrig += i
        }
        origToTrans[raw.length] = out.length
        transToOrig += raw.length
        return TransformedText(AnnotatedString(out.toString()), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = origToTrans[offset.coerceIn(0, raw.length)]
            override fun transformedToOriginal(offset: Int) = transToOrig[offset.coerceIn(0, transToOrig.size - 1)]
        })
    }
}

@Composable
fun FinanceCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FFColors.surface, contentColor = FFColors.onSurface), border = BorderStroke(.5.dp, FFColors.outlineVariant), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
