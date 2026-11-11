package com.clonical.kasiva

import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException

internal fun technicalException(error: Throwable): Throwable = RuntimeException(error.javaClass.name).apply {
    stackTrace = error.stackTrace
}

internal fun reportTechnicalError(operation: String, error: Throwable) {
    if (error is CancellationException) return
    val details = CustomKeysAndValues.Builder().putString("operation", operation)
    if (error is FirebaseFirestoreException) details.putString("firestore_code", error.code.name)
    // Pesan dan rantai cause bisa berisi data pengguna; kirim jenis error dan lokasi kode saja.
    runCatching { FirebaseCrashlytics.getInstance().recordException(technicalException(error), details.build()) }
}
