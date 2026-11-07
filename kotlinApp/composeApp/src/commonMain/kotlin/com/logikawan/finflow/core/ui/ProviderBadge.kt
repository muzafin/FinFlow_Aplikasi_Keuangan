package com.logikawan.finflow.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.model.Account
import com.logikawan.finflow.core.model.FinanceProvider
import com.logikawan.finflow.core.model.providerOf

/** Badge bulat warna merek dengan singkatan bank/e-wallet. */
@Composable
fun ProviderBadge(provider: FinanceProvider, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clip(CircleShape).background(Color(provider.color)), contentAlignment = Alignment.Center) {
        val factor = when (provider.short.length) { 1, 2 -> .38f; 3 -> .30f; else -> .25f }
        Text(
            provider.short,
            color = if (provider.dark) Color(0xFF1A1A1A) else Color.White,
            fontSize = (maxWidth.value * factor).sp,
            lineHeight = (maxWidth.value * factor).sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

/** Isi lingkaran ikon dompet: badge penyedia bila ada, selain itu ikon biasa. Dipakai di dalam Box ber-clip lingkaran. */
@Composable
fun BoxScope.AccountIconContent(account: Account, icon: String, tint: Color, size: Dp) {
    val provider = providerOf(account.icon)
    if (provider != null) ProviderBadge(provider, Modifier.matchParentSize()) else FFIcon(icon, tint, size)
}

/** Ikon dompet tanpa wadah (mis. baris pilihan dompet). */
@Composable
fun AccountIcon(account: Account?, fallback: String, tint: Color, size: Dp) {
    val provider = providerOf(account?.icon)
    if (provider != null) ProviderBadge(provider, Modifier.size(size * 1.25f)) else FFIcon(account?.icon ?: fallback, tint, size)
}
