package com.clonical.kasiva

import com.clonical.kasiva.app.App
import com.clonical.kasiva.core.platform.AppLifecycle
import com.clonical.kasiva.core.platform.NativeServices

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(nativeServices: NativeServices, lifecycle: AppLifecycle) = ComposeUIViewController {
    App(nativeServices, lifecycle)
}
