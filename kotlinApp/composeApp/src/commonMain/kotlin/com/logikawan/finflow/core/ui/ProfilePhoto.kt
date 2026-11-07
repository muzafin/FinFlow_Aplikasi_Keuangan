package com.logikawan.finflow.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.logikawan.finflow.core.model.Profile
import com.logikawan.finflow.core.platform.NativeServices
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import com.logikawan.finflow.i18n.*
import com.logikawan.finflow.core.i18n.text

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ProfilePhoto(profile: Profile?, native: NativeServices, modifier: Modifier = Modifier) {
    var bitmap by remember(profile?.id, profile?.photoPath, profile?.photoUpdatedAt) { mutableStateOf<ImageBitmap?>(null) }
    DisposableEffect(profile?.id, profile?.photoPath, profile?.photoUpdatedAt) {
        var active = true
        if (!profile?.photoPath.isNullOrBlank()) native.loadProfilePhoto(profile!!.photoPath) { bytes, _ ->
            if (active && bytes != null) bitmap = runCatching { bytes.decodeToImageBitmap() }.getOrNull()
        }
        onDispose { active = false }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it, MR.strings.app_profile_photo.text(), Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: FFIcon("hi_user", FFColors.onSurfaceVariant, 40.dp)
    }
}
