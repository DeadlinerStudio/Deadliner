package com.aritxonly.deadliner.localutils

import android.content.SharedPreferences
import com.aritxonly.deadliner.model.resolveImmersiveExperiencePreference

internal object ImmersiveExperiencePreferences {
    private const val CurrentKey = "miuix_immersive_experience"
    private const val LegacyFloatingTabBarKey = "miuix_floating_tab_bar"

    fun read(preferences: SharedPreferences): Boolean = resolveImmersiveExperiencePreference(
        currentValue = preferences.optionalBoolean(CurrentKey),
        legacyFloatingTabBarValue = preferences.optionalBoolean(LegacyFloatingTabBarKey),
    )

    fun write(
        editor: SharedPreferences.Editor,
        enabled: Boolean,
    ) {
        editor.putBoolean(CurrentKey, enabled)
        editor.remove(LegacyFloatingTabBarKey)
    }

    private fun SharedPreferences.optionalBoolean(key: String): Boolean? =
        if (contains(key)) getBoolean(key, false) else null
}
