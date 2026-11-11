package com.clonical.kasiva.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.model.AppPreferences
import com.clonical.kasiva.core.model.BackupCodec
import com.clonical.kasiva.core.model.FinanceBackup
import com.clonical.kasiva.core.model.LocalPreferences
import com.clonical.kasiva.core.model.cardGradientColors
import com.clonical.kasiva.core.model.cardGradientPresets
import com.clonical.kasiva.core.model.menuThemeColors
import com.clonical.kasiva.core.model.menuThemePresets
import com.clonical.kasiva.core.model.themeModePresetOf
import com.clonical.kasiva.core.model.themeModePresets
import com.clonical.kasiva.core.platform.AppLifecycle
import com.clonical.kasiva.core.platform.NativeServices
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.core.ui.FigmaAsset
import com.clonical.kasiva.core.util.formatBalance
import com.clonical.kasiva.core.util.minorAmount
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.i18n.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Hasil wizard pengaturan awal yang disimpan Lead. */
data class SetupResult(
    val name: String,
    val language: String,
    val currency: String,
    val walletName: String,
    val openingBalanceMinor: Long,
    val photoBytes: ByteArray?,
    val preferences: AppPreferences,
)

/** Satu pilihan bahasa pada langkah kedua. */
data class SetupLanguage(val tag: String, val label: String, val enabled: Boolean)

/** Seluruh bahasa antarmuka yang tersedia, dengan nama aslinya. */
val setupLanguages = listOf(
    SetupLanguage("id", "Bahasa Indonesia", true),
    SetupLanguage("en", "English", true),
    SetupLanguage("ms", "Bahasa Melayu", true),
    SetupLanguage("de", "Deutsch", true),
    SetupLanguage("fr", "Français", true),
    SetupLanguage("ja", "日本語", true),
    SetupLanguage("es", "Español", true),
    SetupLanguage("hi", "हिन्दी", true),
    SetupLanguage("zh", "中文（简体）", true),
)

internal data class SetupCurrency(val code: String, val label: String, val symbol: String)

internal val setupCurrencies = listOf(
    SetupCurrency("USD", "US Dollar", "\$"),
    SetupCurrency("EUR", "Euro", "€"),
    SetupCurrency("IDR", "Indonesian Rupiah", "Rp"),
    SetupCurrency("MYR", "Malaysian Ringgit", "RM"),
    SetupCurrency("JPY", "Japanese Yen", "¥"),
    SetupCurrency("CNY", "Chinese Yuan", "¥"),
)

/** Daftar lengkap pada dialog "Lihat Semua", disalin dari LocalSetupScreen. */
internal val setupAllCurrencyCodes = listOf(
    "AED", "AUD", "BDT", "BHD", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "EGP", "EUR", "GBP", "HKD", "HUF",
    "IDR", "ILS", "INR", "ISK", "JPY", "KRW", "KWD", "LKR", "MAD", "MXN", "MYR", "NOK", "NZD", "OMR", "PHP",
    "PKR", "PLN", "QAR", "RON", "RUB", "SAR", "SEK", "SGD", "THB", "TRY", "TWD", "UAH", "USD", "VND", "ZAR",
)

private val setupIncomeColors = listOf("green" to 0xFF4CAF50L, "teal" to 0xFF009688L, "blue" to 0xFF2196F3L)
private val setupExpenseColors = listOf("red" to 0xFFFF5252L, "orange" to 0xFFFF9800L, "purple" to 0xFF9C27B0L)

private val setupInk = Color(0xFF1D1E26)
private val setupMuted = Color(0xFF818185)
private val setupSurface = Color(0xFFF7F7F8)
private val setupSelectedSurface = Color(0xFFEFEFEF)
private val setupLine = Color(0xFFE5E7EB)
private val setupField = Color(0xFFF5F5F7)

private val setupTitleStyle: TextStyle @Composable get() = FFType.appTitle.copy(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
private val setupSubtitleStyle: TextStyle @Composable get() = FFType.bodyMain.copy(fontSize = 16.sp, lineHeight = 24.sp)
private val setupBodyStyle: TextStyle @Composable get() = FFType.bodyMain.copy(fontSize = 15.sp, lineHeight = 22.sp)
private val setupSmallStyle: TextStyle @Composable get() = FFType.metaData.copy(fontSize = 13.sp, lineHeight = 20.sp)

/**
 * Wizard pengaturan awal enam langkah mengikuti alur Ollo: welcome, bahasa, mata uang,
 * notifikasi, gaya, lalu dompet pertama. Semua data dikumpulkan sebagai [SetupResult];
 * penyimpanan ke repository dilakukan pemanggil lewat [onComplete].
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun OlloSetupFlow(
    native: NativeServices,
    lifecycle: AppLifecycle,
    busy: Boolean,
    onComplete: suspend (SetupResult) -> Unit,
    onSignIn: () -> Unit,
    onRestore: suspend (FinanceBackup) -> Unit,
    onEmailSignIn: () -> Unit = onSignIn,
) = EntryTheme {
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    // Bahasa & mata uang mengikuti pengaturan HP; dapat diubah nanti di Pengaturan.
    val language = remember { native.deviceLanguage().takeIf { tag -> setupLanguages.any { it.tag == tag && it.enabled } } ?: "en" }
    var currency by rememberSaveable { mutableStateOf(native.deviceCurrency()) }
    var name by rememberSaveable { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var balance by rememberSaveable { mutableStateOf("") }
    var photo by remember { mutableStateOf<ByteArray?>(null) }
    val initialPreferences = LocalPreferences.current
    var preferences by remember { mutableStateOf(initialPreferences) }
    var backup by remember { mutableStateOf<FinanceBackup?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reminderOn by remember { mutableStateOf(native.reminderEnabled()) }
    var reminderError by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var restoreBusy by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    val strings = setupStrings()
    val photoPreview = remember(photo) { photo?.let { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() } }
    val disabled = busy || submitting

    LaunchedEffect(step) { scroll.scrollTo(0) }

    DisposableEffect(lifecycle, step, backup, showCurrencyDialog, disabled, restoreBusy) {
        lifecycle.onBack = {
            when {
                backup != null && !restoreBusy -> { backup = null; true }
                showCurrencyDialog -> { showCurrencyDialog = false; true }
                step > 0 -> { if (!disabled) { step = setupOrder.getOrElse(setupOrder.indexOf(step) - 1) { 0 }; error = null }; true }
                else -> false
            }
        }
        onDispose { lifecycle.onBack = null }
    }

    fun changeReminder(on: Boolean) {
        reminderOn = on
        reminderError = null
        native.setReminder(on, 20, 0) { failure ->
            if (failure != null) {
                reminderError = failure
                reminderOn = native.reminderEnabled()
            }
        }
    }

    fun pickPhoto() {
        native.pickProfilePhoto { bytes, failure ->
            if (bytes != null) { photo = bytes; error = null }
            else if (failure != null) error = failure
        }
    }

    fun pickBackup() {
        native.pickCsv { content, failure ->
            if (content != null) {
                try { backup = BackupCodec.decode(content); error = null }
                catch (e: Exception) { error = strings.restoreFailed + " " + e.message.orEmpty() }
            } else if (failure != null) error = failure
        }
    }

    fun finish() {
        if (disabled || completed) return
        // Pertahanan ganda: wizard tidak boleh selesai tanpa nama lengkap.
        if (name.trim().isEmpty()) {
            step = 0
            nameError = strings.nameRequired
            return
        }
        error = null
        val opening = try {
            minorAmount(balance.trim().ifBlank { "0" }, currency)
        } catch (e: Exception) {
            error = strings.balanceInvalid + " " + e.message.orEmpty()
            return
        }
        val result = SetupResult(
            name = name.trim(),
            language = language,
            currency = currency,
            walletName = "Cash",
            openingBalanceMinor = opening,
            photoBytes = photo,
            preferences = preferences.copy(language = "system"),
        )
        submitting = true
        scope.launch {
            try {
                completed = true
                onComplete(result)
            } catch (e: CancellationException) {
                completed = false
                throw e
            } catch (e: Exception) {
                completed = false
                error = e.message ?: strings.saveFailed
            } finally {
                submitting = false
            }
        }
    }

    fun next() {
        if (disabled) return
        error = null
        if (step == 0 && name.trim().isEmpty()) {
            nameError = strings.nameRequired
            return
        }
        val position = setupOrder.indexOf(step)
        if (position < setupOrder.lastIndex) step = setupOrder[position + 1] else finish()
    }

    Column(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding().imePadding().testTag("setup-flow")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(setupOrder.size) { index ->
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (index <= setupOrder.indexOf(step)) setupInk else setupLine))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 24.dp).testTag("setup-scroll")) {
            Spacer(Modifier.height(16.dp))
            Text(setupStepTitle(step, strings), style = setupTitleStyle, color = setupInk)
            Spacer(Modifier.height(8.dp))
            val stepSubtitle = setupStepSubtitle(step, strings)
            if (stepSubtitle.isNotBlank()) Text(stepSubtitle, style = setupSubtitleStyle, color = setupMuted)
            Spacer(Modifier.height(24.dp))
            when (step) {
                0 -> WelcomeStep(
                    strings = strings,
                    name = name,
                    nameError = nameError,
                    onName = { value ->
                        if (value.length <= 100) {
                            name = value
                            if (value.isNotBlank()) nameError = null
                        }
                    },
                    photo = photoPreview,
                    onPickPhoto = { pickPhoto() },
                    onRestore = { pickBackup() },
                    onSignIn = onSignIn,
                    onEmailSignIn = onEmailSignIn,
                    enabled = !disabled && !restoreBusy,
                )
                3 -> NotificationStep(strings, reminderOn, { changeReminder(it) }, reminderError)
                4 -> StyleStep(strings, preferences, currency) { preferences = it }
                else -> WalletStep(strings, balance) { if (it.length <= 24) balance = it }
            }
            error?.let { message ->
                Spacer(Modifier.height(14.dp))
                Text(
                    message,
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(12.dp),
                    style = setupSmallStyle,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        SetupPrimaryButton(
            label = if (step == 5) strings.startNow else strings.next,
            busy = disabled || restoreBusy,
            onClick = { next() },
        )
    }

    backup?.let { value ->
        AlertDialog(
            onDismissRequest = { if (!restoreBusy) backup = null },
            title = { Text(strings.restoreDialogTitle, fontWeight = FontWeight.Bold) },
            text = {
                Text(MR.strings.auth_setup_restore_summary.text(value.data.profile?.name.orEmpty(), value.data.transactions.size) + "\n\n" + strings.restoreDialogNote)
            },
            confirmButton = {
                TextButton(enabled = !restoreBusy, onClick = {
                    restoreBusy = true
                    scope.launch {
                        try {
                            onRestore(value)
                            backup = null
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: strings.restoreFailed
                        } finally {
                            restoreBusy = false
                        }
                    }
                }) { Text(strings.restoreConfirm) }
            },
            dismissButton = {
                TextButton(enabled = !restoreBusy, onClick = { backup = null }) { Text(strings.cancel) }
            },
        )
    }

    if (showCurrencyDialog) OlloCurrencyDialog(
        title = strings.currencyDialogTitle,
        closeLabel = strings.close,
        selected = currency,
        onSelect = { currency = it; showCurrencyDialog = false },
        onDismiss = { showCurrencyDialog = false },
    )
}

/** Urutan langkah: welcome, notifikasi, gaya, dompet (bahasa & mata uang mengikuti HP). */
private val setupOrder = listOf(0, 3, 4, 5)

private fun setupStepTitle(step: Int, strings: SetupStrings): String = when (step) {
    0 -> strings.welcomeTitle
    1 -> strings.languageTitle
    2 -> strings.currencyTitle
    3 -> strings.notificationTitle
    4 -> strings.styleTitle
    else -> strings.walletTitle
}

private fun setupStepSubtitle(step: Int, strings: SetupStrings): String = when (step) {
    0 -> strings.welcomeSubtitle
    1 -> strings.languageSubtitle
    2 -> strings.currencySubtitle
    3 -> strings.notificationSubtitle
    4 -> ""
    else -> strings.walletSubtitle
}

@Composable
private fun SetupPrimaryButton(label: String, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).height(56.dp).testTag("setup-next"),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = setupInk,
            contentColor = Color.White,
            disabledContainerColor = setupInk.copy(alpha = .4f),
            disabledContentColor = Color.White,
        ),
    ) {
        Text(label, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

/* ---------- Langkah 1: welcome ---------- */

@Composable
private fun WelcomeStep(
    strings: SetupStrings,
    name: String,
    nameError: String?,
    onName: (String) -> Unit,
    photo: ImageBitmap?,
    onPickPhoto: () -> Unit,
    onRestore: () -> Unit,
    onSignIn: () -> Unit,
    onEmailSignIn: () -> Unit,
    enabled: Boolean,
) {
    Column(Modifier.fillMaxWidth().testTag("setup-step-welcome"), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(120.dp)) {
            Box(Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFF3F3F5)), contentAlignment = Alignment.Center) {
                if (photo != null) Image(photo, MR.strings.auth_setup_photo_cd.text(), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else FFIcon("person_outline", Color(0xFF91929A), 54.dp)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFF3B1FBF))))
                    .clickable(enabled = enabled, onClick = onPickPhoto).testTag("setup-photo"),
                contentAlignment = Alignment.Center,
            ) { CameraGlyph(Color.White, 18.dp) }
        }
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = name,
            enabled = enabled,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag("setup-name"),
            placeholder = { Text(strings.fullNamePlaceholder, style = setupBodyStyle.copy(fontSize = 17.sp), color = setupMuted) },
            isError = nameError != null,
            supportingText = if (nameError != null) {
                {
                    Text(
                        nameError.orEmpty(),
                        Modifier.testTag("setup-name-error"),
                        style = setupSmallStyle.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else null,
            leadingIcon = { FFIcon("person_outline", setupMuted, 22.dp) },
            trailingIcon = if (name.isNotEmpty()) {
                {
                    Box(Modifier.size(40.dp).clickable { onName("") }, contentAlignment = Alignment.Center) {
                        FFIcon("clear", setupMuted, 20.dp)
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            textStyle = setupBodyStyle.copy(fontSize = 17.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = setupField,
                unfocusedContainerColor = setupField,
                // Latar field tetap abu saat error; border merah datang dari isError.
                errorContainerColor = setupField,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
        )
        Spacer(Modifier.height(26.dp))
        Text(strings.lookingForOldData, style = setupSmallStyle.copy(fontSize = 14.sp), color = setupMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        SetupOutlineButton(onClick = onRestore, enabled = enabled, modifier = Modifier.testTag("setup-restore")) {
            FFIcon("history_outlined", setupInk, 20.dp)
            Spacer(Modifier.width(10.dp))
            Text(strings.restoreFromBackup, style = setupBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = setupInk)
        }
        Spacer(Modifier.height(10.dp))
        SetupOutlineButton(onClick = onSignIn, enabled = enabled, modifier = Modifier.testTag("setup-command-google")) {
            FigmaAsset("fig_117_354_google", Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(strings.signInWithGoogle, style = setupBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = setupInk)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            strings.cloudSyncNote,
            style = setupSmallStyle.copy(fontSize = 12.sp, lineHeight = 19.sp),
            color = setupMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            strings.signInWithEmail,
            Modifier.clickable(enabled = enabled, onClick = onEmailSignIn).padding(6.dp).testTag("setup-command-email"),
            style = setupSmallStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            color = setupInk,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CameraGlyph(tint: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.075f
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.08f, h * 0.3f),
            size = Size(w * 0.84f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(stroke),
        )
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.3f, h * 0.13f),
            size = Size(w * 0.34f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(stroke),
        )
        drawCircle(tint, radius = w * 0.13f, center = Offset(w * 0.5f, h * 0.59f), style = Stroke(stroke))
    }
}

@Composable
private fun SetupOutlineButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, setupLine),
        contentPadding = PaddingValues(horizontal = 18.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = setupInk),
        content = content,
    )
}

/* ---------- Langkah 2: pilih bahasa ---------- */

@Composable
private fun LanguageStep(strings: SetupStrings, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("setup-step-language"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        setupLanguages.forEach { choice ->
            val isSelected = choice.tag == selected
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) setupSelectedSurface else setupSurface)
                    .border(2.dp, if (isSelected) setupInk else Color.Transparent, RoundedCornerShape(20.dp))
                    .clickable(enabled = choice.enabled) { onSelect(choice.tag) }
                    .padding(horizontal = 20.dp, vertical = 17.dp)
                    .testTag("lang-" + choice.tag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    choice.label,
                    style = setupBodyStyle.copy(fontSize = 17.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (choice.enabled) setupInk else setupMuted,
                )
                Spacer(Modifier.weight(1f))
                if (!choice.enabled) {
                    Text(
                        strings.languageSoon,
                        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFEDEDF0)).padding(horizontal = 8.dp, vertical = 3.dp),
                        style = setupSmallStyle.copy(fontSize = 11.sp),
                        color = setupMuted,
                    )
                } else {
                    RadioCheck(isSelected)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(strings.languageNote, style = setupSmallStyle.copy(fontSize = 12.sp, lineHeight = 19.sp), color = setupMuted)
    }
}

@Composable
private fun RadioCheck(selected: Boolean) {
    Box(
        Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, if (selected) setupInk else Color(0xFFD5D6DB), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) FFIcon("check_rounded", setupInk, 15.dp)
    }
}

/* ---------- Langkah 3: pilih mata uang ---------- */

@Composable
private fun CurrencyStep(strings: SetupStrings, selected: String, onSelect: (String) -> Unit, onViewAll: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("setup-step-currency")) {
        setupCurrencies.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { entry ->
                    CurrencyCard(entry, entry.code == selected, { onSelect(entry.code) }, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(
            onClick = onViewAll,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("setup-view-all"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, setupLine),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = setupInk),
        ) {
            FFIcon("search_outlined", setupInk, 20.dp)
            Spacer(Modifier.width(10.dp))
            Text(strings.viewAll, style = setupBodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = setupInk)
        }
        Spacer(Modifier.height(14.dp))
        SetupInfoBanner(strings.currencySettingsNote)
    }
}

@Composable
private fun CurrencyCard(entry: SetupCurrency, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.testTag("currency-" + entry.code).clip(RoundedCornerShape(20.dp))
            .background(if (selected) setupSelectedSurface else setupSurface)
            .border(2.dp, if (selected) setupInk else Color.Transparent, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(if (selected) setupInk else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                entry.symbol,
                style = setupBodyStyle.copy(fontSize = if (selected) 15.sp else 19.sp, fontWeight = FontWeight.SemiBold),
                color = if (selected) Color.White else setupInk,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.label,
                style = setupBodyStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = setupInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(entry.code, style = setupSmallStyle.copy(fontSize = 12.sp), color = setupMuted)
        }
        if (selected) {
            Spacer(Modifier.width(4.dp))
            FFIcon("check_rounded", setupInk, 16.dp)
        }
    }
}

@Composable
private fun SetupInfoBanner(text: String, container: Color = setupSurface) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(container).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        FFIcon("info_outline_rounded", setupMuted, 18.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = setupSmallStyle.copy(fontSize = 13.sp, lineHeight = 20.sp), color = setupMuted)
    }
}

/* ---------- Langkah 4: notifikasi pintar ---------- */

@Composable
private fun NotificationStep(strings: SetupStrings, enabled: Boolean, onToggle: (Boolean) -> Unit, error: String?) {
    Column(Modifier.fillMaxWidth().testTag("setup-step-notification")) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(setupSurface).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFEDE7FB)), contentAlignment = Alignment.Center) {
                FFIcon("notifications_rounded", setupInk, 32.dp)
            }
            Spacer(Modifier.height(16.dp))
            Text(strings.dailyReminderTitle, style = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = setupInk)
            Spacer(Modifier.height(6.dp))
            Text(
                strings.dailyReminderDescription,
                style = setupSmallStyle.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = setupMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, if (enabled) setupInk else Color(0xFFD5D6DB), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (enabled) FFIcon("check_rounded", setupInk, 15.dp)
                }
                Spacer(Modifier.width(12.dp))
                Text(strings.enable, style = setupBodyStyle.copy(fontWeight = FontWeight.SemiBold), color = setupInk)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = setupInk,
                        uncheckedThumbColor = Color(0xFF8A8B91),
                        uncheckedTrackColor = Color(0xFFE3E4E8),
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, Modifier.fillMaxWidth(), style = setupSmallStyle.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/* ---------- Langkah 5: pilih gaya ---------- */

@Composable
private fun StyleStep(strings: SetupStrings, preferences: AppPreferences, currency: String, onPreferences: (AppPreferences) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("setup-step-style")) {
        BalancePreview(strings, preferences, currency)
        Spacer(Modifier.height(18.dp))
        MenuPreviewRow(strings, preferences)
        Spacer(Modifier.height(26.dp))
        SetupSectionTitle(strings.themeMode)
        Spacer(Modifier.height(12.dp))
        val currentTheme = themeModePresetOf(preferences.theme, preferences.palette)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            themeModePresets.forEach { preset ->
                ThemeModeTile(preset.id, preset.accent, preset.id == currentTheme, Modifier.testTag("theme-" + preset.id)) {
                    onPreferences(preferences.copy(theme = preset.theme, palette = preset.palette))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SetupSectionTitle(strings.lightCardStyle)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            cardGradientPresets.forEach { preset ->
                CardGradientSwatch(preset.colors, preset.id == preferences.cardGradient, Modifier.testTag("card-" + preset.id)) {
                    onPreferences(preferences.copy(cardGradient = preset.id))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SetupSectionTitle(strings.menuTheme)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            menuThemePresets.forEach { preset ->
                MenuThemeTile(preset.colors, preset.id == preferences.menuTheme, Modifier.testTag("menu-" + preset.id)) {
                    onPreferences(preferences.copy(menuTheme = preset.id))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SetupSectionTitle(strings.transactionColors)
        Spacer(Modifier.height(10.dp))
        TransactionColorRow(strings.income, "income", setupIncomeColors, preferences.incomeColor) { color ->
            onPreferences(preferences.copy(incomeColor = color))
        }
        TransactionColorRow(strings.expense, "expense", setupExpenseColors, preferences.expenseColor) { color ->
            onPreferences(preferences.copy(expenseColor = color))
        }
    }
}

@Composable
private fun SetupSectionTitle(text: String) {
    Text(text, style = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = setupInk)
}

@Composable
private fun BalancePreview(strings: SetupStrings, preferences: AppPreferences, currency: String) {
    val gradient = cardGradientColors(preferences.cardGradient).ifEmpty { listOf(0xFF2196F3L, 0xFF26C6DAL) }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(gradient.map { Color(it) }))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                MR.strings.auth_setup_total_balance.text(currency),
                style = setupSmallStyle.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = .85f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatBalance(1_245_000, currency, preferences),
                style = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PreviewAmountCard(strings.income, "arrow_upward_rounded", formatBalance(320_000, currency, preferences), Modifier.weight(1f))
                PreviewAmountCard(strings.expense, "arrow_downward_rounded", formatBalance(115_000, currency, preferences), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PreviewAmountCard(label: String, icon: String, amount: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .18f)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
                FFIcon(icon, Color.White, 12.dp)
            }
            Spacer(Modifier.width(6.dp))
            Text(label, style = setupSmallStyle.copy(fontSize = 12.sp), color = Color.White.copy(alpha = .9f), maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            amount,
            style = setupBodyStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuPreviewRow(strings: SetupStrings, preferences: AppPreferences) {
    val colors = menuThemeColors(preferences.menuTheme).ifEmpty { listOf(0xFFF6A609L, 0xFF4CAF50L, 0xFFFF5252L, 0xFF9C27B0L) }
    val menus = listOf(
        Triple(strings.budget, "pie_chart_outline_rounded", 0),
        Triple(strings.savings, "savings_rounded", 1),
        Triple(strings.bills, "receipt_long_rounded", 2),
        Triple(strings.debts, "credit_card_rounded", 3),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        menus.forEach { item ->
            val color = Color(colors[item.third % colors.size])
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                    FFIcon(item.second, color, 24.dp)
                }
                Spacer(Modifier.height(6.dp))
                Text(item.first, style = setupSmallStyle.copy(fontSize = 11.sp), color = setupInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ThemeModeTile(id: String, accent: Long, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val background = if (selected) setupSelectedSurface else setupSurface
    Box(
        modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(background)
            .border(2.dp, if (selected) setupInk else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (id) {
            "light" -> SunGlyph(setupInk, 24.dp)
            "dark" -> MoonGlyph(setupInk, background, 22.dp)
            else -> ThemePreviewDots(Color(accent))
        }
    }
}

@Composable
private fun ThemePreviewDots(accent: Color) {
    Column(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(accent))
        repeat(2) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { column ->
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (column == 2) accent.copy(alpha = .55f) else accent))
                }
            }
        }
    }
}

@Composable
private fun SunGlyph(tint: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val extent = this.size.minDimension
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(tint, radius = extent * 0.2f, center = center)
        repeat(8) { index ->
            val angle = index * PI / 4.0
            val inner = extent * 0.34f
            val outer = extent * 0.47f
            drawLine(
                tint,
                start = Offset(center.x + (cos(angle) * inner).toFloat(), center.y + (sin(angle) * inner).toFloat()),
                end = Offset(center.x + (cos(angle) * outer).toFloat(), center.y + (sin(angle) * outer).toFloat()),
                strokeWidth = extent * 0.08f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun MoonGlyph(tint: Color, background: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val extent = this.size.minDimension
        drawCircle(tint, radius = extent * 0.38f, center = Offset(extent * 0.42f, extent * 0.52f))
        drawCircle(background, radius = extent * 0.36f, center = Offset(extent * 0.58f, extent * 0.4f))
    }
}

@Composable
private fun CardGradientSwatch(colors: List<Long>, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(56.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.matchParentSize().clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(colors.ifEmpty { listOf(0xFF2196F3L, 0xFF26C6DAL) }.map { Color(it) })),
        )
        if (selected) {
            Box(
                Modifier.align(Alignment.TopStart).offset(x = (-2).dp, y = (-2).dp).size(20.dp)
                    .clip(CircleShape).background(setupInk),
                contentAlignment = Alignment.Center,
            ) { FFIcon("check_rounded", Color.White, 12.dp) }
        }
    }
}

@Composable
private fun MenuThemeTile(colors: List<Long>, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) setupSelectedSurface else setupSurface)
            .border(2.dp, if (selected) setupInk else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            colors.take(5).forEach { color -> Box(Modifier.size(9.dp).clip(CircleShape).background(Color(color))) }
        }
    }
}

@Composable
private fun TransactionColorRow(label: String, prefix: String, choices: List<Pair<String, Long>>, selected: Long, onSelect: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = setupBodyStyle.copy(fontWeight = FontWeight.SemiBold), color = setupInk)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            choices.forEach { choice ->
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color(choice.second))
                        .then(if (choice.second == selected) Modifier.border(2.dp, setupInk, CircleShape) else Modifier)
                        .clickable { onSelect(choice.second) }
                        .testTag(prefix + "-" + choice.first),
                    contentAlignment = Alignment.Center,
                ) {
                    if (choice.second == selected) FFIcon("check_rounded", Color.White, 20.dp)
                }
            }
        }
    }
}

/* ---------- Langkah 6: dompet pertama ---------- */

@Composable
private fun WalletStep(strings: SetupStrings, balance: String, onBalance: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("setup-step-wallet")) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFFF5F8FC)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFDCEBFF)), contentAlignment = Alignment.Center) {
                    FFIcon("wallet_rounded", Color(0xFF3F6DA8), 24.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(strings.cashWalletName, style = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold), color = setupInk)
                    Text(strings.firstWallet, style = setupSmallStyle.copy(fontSize = 14.sp), color = setupMuted)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(strings.openingBalance, style = setupBodyStyle.copy(fontWeight = FontWeight.SemiBold), color = setupInk)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = balance,
                onValueChange = onBalance,
                modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).testTag("setup-balance"),
                placeholder = { Text("0", style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold), color = Color(0xFFC2C3C9)) },
                textStyle = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = setupInk,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                ),
            )
            Spacer(Modifier.height(16.dp))
            SetupInfoBanner(strings.walletNote, container = Color.White.copy(alpha = .72f))
        }
    }
}
