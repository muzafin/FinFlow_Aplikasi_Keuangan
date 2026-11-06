# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.logikawan.finflow.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.logikawan.finflow.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.logikawan.finflow.**$$serializer { *; }

# Firebase (GitLive memakai serializer, Firestore Android memakai refleksi untuk POJO)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class dev.gitlive.firebase.** { *; }
-dontwarn dev.gitlive.firebase.**

# RevenueCat
-keep class com.revenuecat.purchases.** { *; }
-dontwarn com.revenuecat.purchases.**

# Credentials / Google ID
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# WorkManager worker & notification listener dibuat lewat refleksi
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep class com.logikawan.finflow.notificationimport.** { *; }

# Stack trace yang bisa dibaca di Play Console
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
