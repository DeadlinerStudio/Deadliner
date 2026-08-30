package com.aritxonly.deadliner.localutils

import androidx.core.content.edit
import java.util.UUID

enum class CloudSyncProvider(val storageKey: String) {
    WEB_DAV("webdav"),
    BYOS("byos");

    companion object {
        fun fromStorageKey(value: String?): CloudSyncProvider =
            entries.firstOrNull { it.storageKey == value } ?: WEB_DAV
    }
}

/** Android-hosted provider configuration. Secrets never cross into Core persistence or logs. */
object CloudSyncPreferences {
    var provider: CloudSyncProvider
        get() = CloudSyncProvider.fromStorageKey(preferences.getString(KEY_PROVIDER, null))
        set(value) {
            preferences.edit { putString(KEY_PROVIDER, value.storageKey) }
        }

    var byosBaseUrl: String
        get() = preferences.getString(KEY_BYOS_BASE_URL, "").orEmpty()
        set(value) {
            preferences.edit { putString(KEY_BYOS_BASE_URL, value) }
        }

    var byosAccessToken: String
        get() = preferences.getString(KEY_BYOS_ACCESS_TOKEN, "").orEmpty()
        set(value) {
            preferences.edit { putString(KEY_BYOS_ACCESS_TOKEN, value) }
        }

    val byosDeviceUid: String
        get() = stableValue(KEY_BYOS_DEVICE_UID, prefix = "")

    val byosScopeId: String
        get() = stableValue(KEY_BYOS_SCOPE_ID, prefix = "byos-")

    fun resetByosScopeId() {
        preferences.edit { putString(KEY_BYOS_SCOPE_ID, "byos-${UUID.randomUUID()}") }
    }

    private val preferences
        get() = GlobalUtils.preferences

    private fun stableValue(key: String, prefix: String): String {
        val existing = preferences.getString(key, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = prefix + UUID.randomUUID()
        preferences.edit { putString(key, generated) }
        return generated
    }

    private const val KEY_PROVIDER = "cloud_sync_provider"
    private const val KEY_BYOS_BASE_URL = "byos_base_url"
    private const val KEY_BYOS_ACCESS_TOKEN = "byos_access_token"
    private const val KEY_BYOS_DEVICE_UID = "byos_device_uid"
    private const val KEY_BYOS_SCOPE_ID = "byos_scope_id"
}
