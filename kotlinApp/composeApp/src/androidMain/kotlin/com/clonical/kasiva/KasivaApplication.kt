package com.clonical.kasiva

import android.app.Application
import com.clonical.kasiva.core.i18n.I18nContext

class KasivaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        I18nContext.app = this
    }
}
