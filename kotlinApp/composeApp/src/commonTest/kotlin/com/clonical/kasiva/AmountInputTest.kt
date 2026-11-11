package com.clonical.kasiva

import androidx.compose.ui.text.AnnotatedString
import com.clonical.kasiva.core.ui.GroupedAmountTransformation
import com.clonical.kasiva.core.ui.sanitizeAmountInput
import com.clonical.kasiva.core.util.minorAmount
import kotlin.test.Test
import kotlin.test.assertEquals

/** BUG-01: field nominal tidak boleh menerima huruf dan tampil dengan pemisah ribuan. */
class AmountInputTest {
    @Test fun lettersAreDropped() {
        assertEquals("", sanitizeAmountInput("Test", "IDR"))
        assertEquals("300000", sanitizeAmountInput("3a0b0c000", "IDR"))
    }

    @Test fun commaOrDotBecomesSingleDecimalSeparator() {
        assertEquals("1500.5", sanitizeAmountInput("1500,5", "IDR"))
        // Dua titik = pemisah ribuan (format tempel), bukan dua desimal.
        assertEquals("150025", sanitizeAmountInput("150.0.25", "IDR"))
        // Satu pemisah diikuti tepat 3 digit = ribuan; 1–2 digit = desimal.
        assertEquals("1500259", sanitizeAmountInput("1500.259", "USD"))
        assertEquals("1500.25", sanitizeAmountInput("1500.25", "USD"))
        assertEquals("0.5", sanitizeAmountInput(",5", "USD"))
    }

    @Test fun zeroDecimalCurrencyRejectsSeparator() {
        assertEquals("1500", sanitizeAmountInput("1500.5", "JPY"))
    }

    @Test fun leadingZerosTrimmed() {
        assertEquals("7", sanitizeAmountInput("007", "IDR"))
        assertEquals("0", sanitizeAmountInput("0", "IDR"))
    }

    @Test fun sanitizedValueParsesAsMinorAmount() {
        assertEquals(30_000_000L, minorAmount(sanitizeAmountInput("300000", "IDR"), "IDR"))
        assertEquals(150_050L, minorAmount(sanitizeAmountInput("1500,5", "IDR"), "IDR"))
    }

    @Test fun displayUsesIndonesianGrouping() {
        val out = GroupedAmountTransformation.filter(AnnotatedString("2000000.5"))
        assertEquals("2.000.000,5", out.text.text)
        // Kursor di akhir nilai mentah harus di akhir teks tampilan, dan sebaliknya.
        assertEquals(11, out.offsetMapping.originalToTransformed(9))
        assertEquals(9, out.offsetMapping.transformedToOriginal(11))
        // Kursor setelah digit pertama ("2|000000") berada setelah titik pertama ("2.|000.000").
        assertEquals(2, out.offsetMapping.originalToTransformed(1))
        for (o in 0..9) assertEquals(o, out.offsetMapping.transformedToOriginal(out.offsetMapping.originalToTransformed(o)))
    }

    @Test fun shortValuesAreNotGrouped() {
        assertEquals("300", GroupedAmountTransformation.filter(AnnotatedString("300")).text.text)
        assertEquals("", GroupedAmountTransformation.filter(AnnotatedString("")).text.text)
    }
}
