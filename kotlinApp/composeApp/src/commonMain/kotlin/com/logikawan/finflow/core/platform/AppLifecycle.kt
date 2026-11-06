package com.logikawan.finflow.core.platform

class AppLifecycle {
    var onBackground: (() -> Unit)? = null
    var onBack: (() -> Boolean)? = null
    fun back(): Boolean = onBack?.invoke() ?: false
    fun background() { onBackground?.invoke() }
}
