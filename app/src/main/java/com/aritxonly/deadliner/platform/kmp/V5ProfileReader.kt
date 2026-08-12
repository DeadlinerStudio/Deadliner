package com.aritxonly.deadliner.platform.kmp

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.v5UserProfileDataStore by preferencesDataStore(name = "user_profile")

/** One-time v5 DataStore reader. Runtime profile ownership belongs to KMP after import. */
internal suspend fun Context.readV5Profile(): V5DataMigrator.LegacyProfile {
    val preferences = v5UserProfileDataStore.data.first()
    return V5DataMigrator.LegacyProfile(
        nickname = preferences[stringPreferencesKey("nickname")].orEmpty(),
        avatarFileName = preferences[stringPreferencesKey("avatar_file_name")],
    )
}
