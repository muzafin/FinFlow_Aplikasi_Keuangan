package com.logikawan.finflow

import android.app.Application
import com.logikawan.finflow.core.i18n.I18nContext

class FinFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        I18nContext.app = this
    }
}
