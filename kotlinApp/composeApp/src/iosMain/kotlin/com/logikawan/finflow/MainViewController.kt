package com.logikawan.finflow

import com.logikawan.finflow.app.App
import com.logikawan.finflow.core.platform.AppLifecycle
import com.logikawan.finflow.core.platform.NativeServices

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(nativeServices: NativeServices, lifecycle: AppLifecycle) = ComposeUIViewController {
    App(nativeServices, lifecycle)
}
