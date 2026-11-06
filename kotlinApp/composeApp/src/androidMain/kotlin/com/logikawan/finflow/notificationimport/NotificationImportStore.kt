package com.logikawan.finflow.notificationimport

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class NotificationSource(val packageName: String, val accountId: String, val currency: String)
data class NotificationCandidate(val id: String, val appName: String, val accountId: String, val type: String,
                                 val amountMinor: Long, val currency: String, val postedAt: Long)

class NotificationImportStore(context: Context) {
    val preferences: SharedPreferences = context.applicationContext.getSharedPreferences("transaction_notifications", Context.MODE_PRIVATE)
    fun owner(): String? = preferences.getString("owner", null)
    fun disclosureAccepted(): Boolean = preferences.getBoolean("disclosure.v1", false)
    fun enabled(): Boolean = owner() != null && disclosureAccepted() && preferences.getBoolean("enabled", false)
    fun consentAt(): Long = preferences.getLong("consentAt", Long.MAX_VALUE)

    fun setOwner(uid: String?) = synchronized(lock) {
        if (uid != owner()) {
            // Kandidat dan pilihan sumber tidak ikut terbaca oleh akun FinFlow berikutnya.
            preferences.edit().clear().apply { if (uid != null) putString("owner", uid) }.apply()
        }
    }
    fun setEnabled(enabled: Boolean) = synchronized(lock) {
        preferences.edit().putBoolean("enabled", enabled && owner() != null && disclosureAccepted())
            .apply { if (enabled) putLong("consentAt", System.currentTimeMillis()) }.apply()
    }
    fun acceptDisclosure() { preferences.edit().putBoolean("disclosure.v1", true).apply() }
    fun revokeConsent() = synchronized(lock) {
        val uid = owner()
        preferences.edit().clear().apply { if (uid != null) putString("owner", uid) }.apply()
    }
    fun sources(): List<NotificationSource> = readArray("sources").objects().mapNotNull { value -> runCatching {
        NotificationSource(value.getString("package"), value.getString("account"), value.getString("currency"))
    }.getOrNull() }
    fun source(packageName: String) = sources().firstOrNull { it.packageName == packageName }
    fun selectSource(packageName: String, accountId: String, currency: String) = synchronized(lock) {
        require(owner() != null && packageName.isNotBlank() && accountId.isNotBlank() && currency.matches(Regex("[A-Z]{3}")))
        val values = sources().filterNot { it.packageName == packageName } + NotificationSource(packageName, accountId, currency)
        preferences.edit().putString("sources", JSONArray().apply { values.forEach { put(JSONObject()
            .put("package", it.packageName).put("account", it.accountId).put("currency", it.currency)) } }.toString()).apply()
    }
    fun removeSource(packageName: String) = synchronized(lock) {
        val values = readArray("sources").objects().filterNot { it.optString("package") == packageName }
        preferences.edit().putString("sources", JSONArray(values).toString()).apply()
    }
    fun candidates(): List<NotificationCandidate> = synchronized(lock) {
        val stored = readArray("candidates").objects()
        val fresh = stored.filter { it.optLong("postedAt") >= System.currentTimeMillis() - RETENTION }
        if (fresh.size != stored.size) preferences.edit().putString("candidates", JSONArray(fresh).toString()).apply()
        fresh.mapNotNull { value -> runCatching {
            NotificationCandidate(value.getString("id"), value.getString("app"), value.getString("account"), value.getString("type"),
                value.getLong("amount"), value.getString("currency"), value.getLong("postedAt"))
        }.getOrNull() }.sortedByDescending { it.postedAt }
    }

    fun add(uid: String, source: NotificationSource, candidate: NotificationCandidate) = synchronized(lock) {
        if (!enabled() || owner() != uid || this.source(source.packageName) != source || candidate.postedAt < consentAt()) return@synchronized
        val seen = readArray("seen").objects().filter { it.optLong("time") >= System.currentTimeMillis() - RETENTION }
        if (seen.any { it.optString("id") == candidate.id }) return@synchronized
        val values = (candidates() + candidate).sortedByDescending { it.postedAt }.take(MAX_CANDIDATES)
        preferences.edit().putString("candidates", JSONArray().apply { values.forEach { put(JSONObject()
            .put("id", it.id).put("app", it.appName).put("account", it.accountId).put("type", it.type)
            .put("amount", it.amountMinor).put("currency", it.currency).put("postedAt", it.postedAt)) } }.toString())
            .putString("seen", JSONArray((seen + JSONObject().put("id", candidate.id).put("time", candidate.postedAt)).takeLast(1000)).toString()).apply()
    }
    fun dismiss(id: String) = synchronized(lock) {
        val values = readArray("candidates").objects().filterNot { it.optString("id") == id }
        preferences.edit().putString("candidates", JSONArray(values).toString()).apply()
    }
    fun clearCandidates() = synchronized(lock) { preferences.edit().remove("candidates").apply() }
    private fun readArray(key: String) = runCatching { JSONArray(preferences.getString(key, "[]")) }.getOrElse { JSONArray() }
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
    companion object {
        private val lock = Any()
        private const val RETENTION = 30L * 24 * 60 * 60 * 1000
        const val MAX_CANDIDATES = 300
    }
}
