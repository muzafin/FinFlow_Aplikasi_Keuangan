package com.clonical.kasiva

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.clonical.kasiva.core.i18n.I18nContext

class KasivaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        I18nContext.app = this
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(packageName == "com.clonical.kasiva" && !debuggable)
        // App Check: Play Integrity untuk build Play, provider debug untuk build lokal (token debug dicetak di logcat
        // "DebugAppCheckProvider" lalu didaftarkan di Console). Belum di-enforce di server; lihat SETUP_STATUS.md.
        runCatching {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                if (debuggable) DebugAppCheckProviderFactory.getInstance() else PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        PushMessaging.createChannels(this)
    }
}
