plugins {
    kotlin("multiplatform") version "2.3.20" apply false
    kotlin("native.cocoapods") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.20" apply false
    kotlin("plugin.compose") version "2.3.20" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
    id("com.android.application") version "8.13.2" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.8" apply false
    id("dev.icerock.mobile.multiplatform-resources") version "0.27.1" apply false
}
