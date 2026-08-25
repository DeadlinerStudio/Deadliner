package com.aritxonly.deadliner.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImmersiveExperiencePreferenceTest {
    @Test
    fun currentPreferenceOverridesLegacyValue() {
        assertFalse(
            resolveImmersiveExperiencePreference(
                currentValue = false,
                legacyFloatingTabBarValue = true,
            ),
        )
    }

    @Test
    fun legacyFloatingBarPreferenceMigratesWhenCurrentValueIsMissing() {
        assertTrue(
            resolveImmersiveExperiencePreference(
                currentValue = null,
                legacyFloatingTabBarValue = true,
            ),
        )
    }

    @Test
    fun freshInstallDefaultsToDisabled() {
        assertFalse(
            resolveImmersiveExperiencePreference(
                currentValue = null,
                legacyFloatingTabBarValue = null,
            ),
        )
    }
}
