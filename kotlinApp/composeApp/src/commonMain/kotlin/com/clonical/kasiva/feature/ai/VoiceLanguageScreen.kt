package com.clonical.kasiva.feature.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonical.kasiva.core.i18n.AppLocale
import com.clonical.kasiva.core.i18n.appLanguages
import com.clonical.kasiva.core.i18n.text
import com.clonical.kasiva.core.model.LocalPreferenceState
import com.clonical.kasiva.core.model.LocalPreferences
import com.clonical.kasiva.core.ui.FFColors
import com.clonical.kasiva.core.ui.FFIcon
import com.clonical.kasiva.core.ui.FFType
import com.clonical.kasiva.i18n.*

/** Bahasa pengenalan suara: tag BCP-47 + nama asli (autonim, tidak diterjemahkan). Tujuh pertama mengikuti urutan Ollo. */
val voiceLanguages: List<Pair<String, String>> = listOf(
    "id-ID" to "Bahasa Indonesia",
    "ms-MY" to "Bahasa Melayu",
    "en-US" to "English (US)",
    "vi-VN" to "Tiếng Việt",
    "ja-JP" to "日本語",
    "zh-CN" to "中文 (普通话)",
    "ko-KR" to "한국어",
    "de-DE" to "Deutsch",
    "fr-FR" to "Français",
    "es-ES" to "Español",
    "hi-IN" to "हिन्दी",
)

/** Tag pengenalan suara untuk tiap bahasa aplikasi (dipakai saat preferensi = "system"). */
val appSpeechLocales: Map<String, String> = mapOf(
    "id" to "id-ID", "en" to "en-US", "ms" to "ms-MY", "de" to "de-DE", "fr" to "fr-FR",
    "ja" to "ja-JP", "es" to "es-ES", "hi" to "hi-IN", "zh" to "zh-CN",
)

/** Locale yang dikirim ke `NativeServices.recognizeSpeech`: pilihan pengguna, atau mengikuti bahasa aplikasi. */
fun resolveSpeechLocale(voiceLanguage: String, appLanguage: String): String =
    voiceLanguage.takeIf { pref -> pref != "system" && voiceLanguages.any { it.first == pref } }
        ?: appSpeechLocales[appLanguage] ?: "id-ID"

@Composable
fun VoiceLanguageScreen() {
    val preferences = LocalPreferences.current
    val store = LocalPreferenceState.current
    fun choose(tag: String) { if (tag != preferences.voiceLanguage) store?.update(preferences.copy(voiceLanguage = tag)) }
    val appLanguageName = appLanguages.firstOrNull { it.tag == AppLocale.current }?.nativeName ?: AppLocale.current
    Column(Modifier.fillMaxSize().background(FFColors.background).verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 24.dp).testTag("voice-language"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VoiceRow(MR.strings.pd_voice_follow_app.text(), MR.strings.pd_voice_follow_app_caption.text(appLanguageName),
            preferences.voiceLanguage == "system", "voice-language-system") { choose("system") }
        voiceLanguages.forEach { (tag, name) ->
            VoiceRow(name, null, preferences.voiceLanguage == tag, "voice-language-$tag") { choose(tag) }
        }
    }
}

@Composable
private fun VoiceRow(title: String, caption: String?, selected: Boolean, tag: String, onClick: () -> Unit) {
    // Ollo: kartu 55dp, radius 18dp, teks 17sp; terpilih ditebalkan dengan tanda centang di kanan.
    Row(Modifier.fillMaxWidth().heightIn(min = 55.dp).clip(RoundedCornerShape(18.dp)).background(FFColors.surface)
        .selectable(selected, role = Role.RadioButton, onClick = onClick)
        .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = FFType.bodyMain.copy(fontSize = 17.sp, lineHeight = 24.sp),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = FFColors.onSurface)
            caption?.let { Text(it, style = FFType.metaData, color = FFColors.onSurfaceVariant) }
        }
        if (selected) FFIcon("hi_check_circle", FFColors.onSurface, 24.dp, contentDescription = MR.strings.pd_voice_selected.text())
    }
}
