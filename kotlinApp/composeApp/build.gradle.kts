import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kunci upload Play Store dibaca dari keystore.properties (tidak di-commit).
val releaseKeystore = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { f ->
    Properties().apply { f.inputStream().use { load(it) } }
}

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.application")
    id("com.google.gms.google-services")
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    iosArm64()
    iosSimulatorArm64()
    cocoapods {
        version = "1.0.0"
        summary = "FinFlow shared application"
        homepage = "https://github.com/muzafin/FinFlow_Aplikasi_Keuangan"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")
        framework { baseName = "FinFlowShared"; isStatic = true }
        pod("FirebaseCore", "11.7.0")
        pod("FirebaseAuth", "11.7.0")
        pod("FirebaseFirestore", "11.7.0")
        pod("FirebaseStorage", "11.7.0")
        pod("FirebaseFunctions", "11.7.0")
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation("io.github.kyant0:backdrop:2.0.0")
            implementation(compose.components.resources)
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation("dev.gitlive:firebase-auth:2.4.0")
            implementation("dev.gitlive:firebase-firestore:2.4.0")
            implementation("dev.gitlive:firebase-functions:2.4.0")
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.15.0"))
            implementation("com.google.firebase:firebase-storage")
            implementation("androidx.activity:activity-compose:1.10.1")
            implementation("androidx.fragment:fragment-ktx:1.8.6")
            implementation("androidx.appcompat:appcompat:1.7.0")
            implementation("androidx.biometric:biometric:1.1.0")
            implementation("androidx.work:work-runtime-ktx:2.10.0")
            implementation("androidx.credentials:credentials:1.5.0")
            implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
            implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
            implementation("androidx.core:core-ktx:1.15.0")
            implementation("com.revenuecat.purchases:purchases:11.1.0")
        }
        commonTest.dependencies { implementation(kotlin("test")); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2") }
        androidInstrumentedTest.dependencies {
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.15.0"))
            implementation("androidx.compose.ui:ui-test-junit4:1.10.5")
            implementation("androidx.test:runner:1.7.0")
        }
        androidUnitTest.dependencies {
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.15.0"))
        }
    }
}

compose.resources {
    packageOfResClass = "com.logikawan.finflow.resources"
}

android {
    namespace = "com.logikawan.finflow"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.logikawan.finflow"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        if (releaseKeystore != null) create("release") {
            storeFile = rootProject.file(releaseKeystore.getProperty("storeFile"))
            storePassword = releaseKeystore.getProperty("storePassword")
            keyAlias = releaseKeystore.getProperty("keyAlias")
            keyPassword = releaseKeystore.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Tanpa keystore.properties: ditandatangani kunci debug hanya untuk uji lokal (Play akan menolak).
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.5")
}
