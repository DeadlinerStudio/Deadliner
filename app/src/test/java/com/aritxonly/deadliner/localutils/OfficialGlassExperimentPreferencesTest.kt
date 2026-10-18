package com.aritxonly.deadliner.localutils

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.ui.theme.createAdvancedMaterialSpec
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class OfficialGlassExperimentPreferencesTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
        GlobalUtils.init(context)
    }

    @After fun tearDown() {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
        GlobalUtils.init(context)
    }

    @Test fun pipelineChoicePersistsWithoutRevivingTheRetiredExperiment() {
        val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        preferences.edit().putBoolean("official_miuix_glass_experiment", true).commit()
        GlobalUtils.init(context)
        assertFalse(GlobalUtils.appearancePreferences.useOfficialMiuixGlass)
        GlobalUtils.updateAppearance(commit = true) {
            it.copy(useAdvancedMaterial = true, advancedMaterialBlurRadius = 72f,
                advancedMaterialFineTuning = it.advancedMaterialFineTuning.copy(refractionAmountDp = 21f))
        }
        val custom = GlobalUtils.appearancePreferences
        GlobalUtils.updateAppearance(commit = true) { it.copy(useOfficialMiuixGlass = true) }
        assertFalse(preferences.contains("official_miuix_glass_experiment"))
        GlobalUtils.init(context)
        val official = GlobalUtils.appearanceFlow.value
        assertEquals(custom.copy(useOfficialMiuixGlass = true), official)
        val spec = createAdvancedMaterialSpec(official.useAdvancedMaterial, official.advancedMaterialBlurRadius,
            official.advancedMaterialFineTuning, official.useOfficialMiuixGlass)
        assertTrue(spec.useOfficialMiuixGlass)
        assertTrue(spec.textureBlurEnabled)
        GlobalUtils.updateAppearance(commit = true) { it.copy(useOfficialMiuixGlass = false) }
        GlobalUtils.init(context)
        assertEquals(custom, GlobalUtils.appearanceFlow.value)
    }
}
