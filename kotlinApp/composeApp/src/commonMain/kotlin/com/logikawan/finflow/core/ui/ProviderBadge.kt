package com.logikawan.finflow.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import com.logikawan.finflow.resources.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.logikawan.finflow.core.i18n.text
import com.logikawan.finflow.core.model.Account
import com.logikawan.finflow.core.model.FinanceProvider
import com.logikawan.finflow.core.model.providerOf
import com.logikawan.finflow.i18n.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.coroutines.resume

/** Logo resmi penyedia (Wikimedia Commons, lihat licenses/ProviderLogos.md); penyedia lain memakai badge warna. */
private val providerLogos: Map<String, DrawableResource> = mapOf(
    "allo" to Res.drawable.provider_allo,
    "bca" to Res.drawable.provider_bca,
    "blu" to Res.drawable.provider_blu,
    "bni" to Res.drawable.provider_bni,
    "bri" to Res.drawable.provider_bri,
    "bsi" to Res.drawable.provider_bsi,
    "btn" to Res.drawable.provider_btn,
    "btpn" to Res.drawable.provider_btpn,
    "cimb" to Res.drawable.provider_cimb,
    "dana" to Res.drawable.provider_dana,
    "danamon" to Res.drawable.provider_danamon,
    "flip" to Res.drawable.provider_flip,
    "gopay" to Res.drawable.provider_gopay,
    "jago" to Res.drawable.provider_jago,
    "krom" to Res.drawable.provider_krom,
    "linkaja" to Res.drawable.provider_linkaja,
    "mandiri" to Res.drawable.provider_mandiri,
    "maybank" to Res.drawable.provider_maybank,
    "neo" to Res.drawable.provider_neo,
    "ocbc" to Res.drawable.provider_ocbc,
    "ovo" to Res.drawable.provider_ovo,
    "panin" to Res.drawable.provider_panin,
    "paypal" to Res.drawable.provider_paypal,
    "permata" to Res.drawable.provider_permata,
    "seabank" to Res.drawable.provider_seabank,
    "superbank" to Res.drawable.provider_superbank,
)

fun hasProviderLogo(id: String): Boolean = id in providerLogos

/** Logo bank/e-wallet dalam lingkaran; bila belum ada logo, badge warna merek dengan singkatan. */
@Composable
fun ProviderBadge(provider: FinanceProvider, modifier: Modifier = Modifier) {
    val logo = providerLogos[provider.id]
    if (logo != null) {
        Image(painterResource(logo), contentDescription = provider.name, modifier = modifier.clip(CircleShape), contentScale = ContentScale.Fit)
        return
    }
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

// ---------- Ikon dompet kustom ("Upload Custom Icon") ----------

/** `Account.icon` bernilai `upload:{accountId}` berarti ikon gambar unggahan untuk dompet itu. */
const val UPLOADED_ICON_PREFIX = "upload:"
/** Batas thumbnail di klien; Storage menerima ≤ 100 KB. */
const val WALLET_ICON_MAX_BYTES = 64 * 1024
/** ID dompet yang dapat dipakai sebagai nama berkas ikon (sesuai storage.rules; "upload:" + ID ≤ 100 karakter). */
val walletIconIdPattern = Regex("[A-Za-z0-9_-]{1,93}")
fun uploadedIconRef(accountId: String): String = UPLOADED_ICON_PREFIX + accountId
fun isUploadedIcon(icon: String?): Boolean = icon?.startsWith(UPLOADED_ICON_PREFIX) == true

/**
 * Penyimpan ikon dompet kustom untuk satu pemilik data (disediakan App lewat [LocalWalletIcons]).
 * Bitmap disimpan di memori per sesi; berkas aslinya disimpan/diunduh oleh NativeServices.
 */
@Stable
class WalletIconStore(
    private val pickImage: ((ByteArray?, String?) -> Unit) -> Unit,
    private val saveIcon: (String, ByteArray, (String?) -> Unit) -> Unit,
    private val loadIcon: (String, (ByteArray?) -> Unit) -> Unit,
) {
    private val bitmaps = mutableStateMapOf<String, ImageBitmap>()
    private val requested = mutableSetOf<String>()

    fun bitmap(accountId: String): ImageBitmap? = bitmaps[accountId]
    fun request(accountId: String) {
        if (walletIconIdPattern.matches(accountId) && requested.add(accountId)) loadIcon(accountId) { bytes -> decode(bytes)?.let { bitmaps[accountId] = it } }
    }
    /** Buka pemilih gambar; hasilnya thumbnail JPEG ≤ [WALLET_ICON_MAX_BYTES]. */
    fun pick(result: (ByteArray?, String?) -> Unit) = pickImage(result)
    /** Unggah (cloud) / simpan (lokal) ikon untuk [accountId]; melempar galat bila gagal. */
    suspend fun save(accountId: String, bytes: ByteArray) {
        require(walletIconIdPattern.matches(accountId)) { MR.strings.r3_icon_invalid_id.text() }
        require(bytes.isNotEmpty() && bytes.size <= 100 * 1024) { MR.strings.r3_icon_too_large.text() }
        val failure = suspendCancellableCoroutine<String?> { continuation ->
            saveIcon(accountId, bytes) { message -> if (continuation.isActive) continuation.resume(message) }
        }
        check(failure == null) { failure ?: MR.strings.r3_icon_upload_failed.text() }
        requested.add(accountId)
        decode(bytes)?.let { bitmaps[accountId] = it }
    }

    companion object {
        @OptIn(ExperimentalResourceApi::class)
        fun decode(bytes: ByteArray?): ImageBitmap? = bytes?.takeIf { it.isNotEmpty() }?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
    }
}

val LocalWalletIcons = staticCompositionLocalOf<WalletIconStore?> { null }

/** Gambar ikon unggahan [accountId]; [fallback] selama belum dimuat atau di luar App. */
@Composable
fun WalletUploadedIcon(accountId: String, modifier: Modifier, fallback: @Composable () -> Unit) {
    val store = LocalWalletIcons.current
    LaunchedEffect(store, accountId) { store?.request(accountId) }
    val bitmap = store?.bitmap(accountId)
    if (bitmap != null) Image(bitmap, MR.strings.r3_icon_custom.text(), modifier.clip(CircleShape), contentScale = ContentScale.Crop)
    else fallback()
}

/** Isi lingkaran ikon dompet: ikon unggahan, badge penyedia bila ada, selain itu ikon biasa. Dipakai di dalam Box ber-clip lingkaran. */
@Composable
fun BoxScope.AccountIconContent(account: Account, icon: String, tint: Color, size: Dp) {
    if (isUploadedIcon(account.icon)) {
        WalletUploadedIcon(account.icon.removePrefix(UPLOADED_ICON_PREFIX), Modifier.matchParentSize()) {
            FFIcon(if (isUploadedIcon(icon)) "hi_wallet" else icon, tint, size)
        }
        return
    }
    val provider = providerOf(account.icon)
    if (provider != null) ProviderBadge(provider, Modifier.matchParentSize()) else FFIcon(icon, tint, size)
}

/** Ikon dompet tanpa wadah (mis. baris pilihan dompet). */
@Composable
fun AccountIcon(account: Account?, fallback: String, tint: Color, size: Dp) {
    if (account != null && isUploadedIcon(account.icon)) {
        Box(Modifier.size(size * 1.25f).clip(CircleShape), contentAlignment = Alignment.Center) {
            WalletUploadedIcon(account.icon.removePrefix(UPLOADED_ICON_PREFIX), Modifier.matchParentSize()) { FFIcon("hi_wallet", tint, size) }
        }
        return
    }
    val provider = providerOf(account?.icon)
    if (provider != null) ProviderBadge(provider, Modifier.size(size * 1.25f)) else FFIcon(account?.icon ?: fallback, tint, size)
}
