package com.aritxonly.deadliner.localutils

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.aritxonly.deadliner.model.AdvancedMaterialLevel
import com.aritxonly.deadliner.model.AdvancedMaterialTuning
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AdvancedMaterialFineTuningPreferencesTest {
    private lateinit var context: Context

    private val preferences
        get() = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferences.edit().clear().commit()
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun freshInstallUsesPinnedRadiusWhileLegacyLevelRemainsCompatible() {
        assertEquals(
            AdvancedMaterialTuning.DefaultBlurRadius,
            AdvancedMaterialFineTuningPreferences.readBlurRadius(
                preferences,
                AdvancedMaterialLevel.Soft,
            ),
            0.0001f,
        )

        preferences.edit()
            .putString("advanced_material_level", AdvancedMaterialLevel.Light.key)
            .commit()
        assertEquals(
            AdvancedMaterialLevel.Light.blurRadius,
            AdvancedMaterialFineTuningPreferences.readBlurRadius(
                preferences,
                AdvancedMaterialLevel.Light,
            ),
            0.0001f,
        )
    }

    @Test
    fun versionTwoDefaultsMigrateButCustomValuesRemain() {
        preferences.edit()
            .putInt("advanced_material_fine_defaults_version", 2)
            .putFloat("advanced_material_blur_radius", 128f)
            .putFloat("advanced_material_fine_saturation_multiplier", 1f)
            .putFloat("advanced_material_fine_edge_highlight_alpha", 0.18f)
            .putFloat("advanced_material_fine_contrast", 0.87f)
            .commit()

        val tuning = AdvancedMaterialFineTuningPreferences.read(preferences)
        assertEquals(92f, AdvancedMaterialFineTuningPreferences.readBlurRadius(
            preferences,
            AdvancedMaterialLevel.Soft,
        ), 0.0001f)
        assertEquals(1.05f, tuning.saturationMultiplier, 0.0001f)
        assertEquals(0.95f, tuning.edgeHighlightAlpha, 0.0001f)
        assertEquals(0.87f, tuning.contrast, 0.0001f)
        assertEquals(0.65f, tuning.dialogBackgroundAlpha, 0.0001f)
        assertEquals(80f, tuning.bottomSheetBlurRadiusDp, 0.0001f)
    }

    @Test
    fun versionThreeModalDefaultsMigrateWhileCustomValuesRemain() {
        preferences.edit()
            .putInt("advanced_material_fine_defaults_version", 3)
            .putFloat("advanced_material_fine_dialog_background_alpha", 0.62f)
            .putFloat("advanced_material_fine_dialog_blur_radius_dp", 48f)
            .putFloat("advanced_material_fine_bottom_sheet_background_alpha", 0.68f)
            .putFloat("advanced_material_fine_bottom_sheet_blur_radius_dp", 64f)
            .commit()

        val tuning = AdvancedMaterialFineTuningPreferences.read(preferences)

        assertEquals(0.65f, tuning.dialogBackgroundAlpha, 0.0001f)
        assertEquals(80f, tuning.dialogBlurRadiusDp, 0.0001f)
        assertEquals(0.68f, tuning.bottomSheetBackgroundAlpha, 0.0001f)
        assertEquals(80f, tuning.bottomSheetBlurRadiusDp, 0.0001f)
    }

    @Test
    fun modalMaterialValuesRoundTripAndNormalize() {
        AdvancedMaterialFineTuningPreferences.write(
            preferences.edit(),
            AdvancedMaterialFineTuning.Default.copy(
                dialogBackgroundAlpha = 0.55f,
                dialogBlurRadiusDp = 42f,
                bottomSheetBackgroundAlpha = 0.58f,
                bottomSheetBlurRadiusDp = 56f,
            ),
        ).commit()

        val tuning = AdvancedMaterialFineTuningPreferences.read(preferences)
        assertEquals(0.55f, tuning.dialogBackgroundAlpha, 0.0001f)
        assertEquals(42f, tuning.dialogBlurRadiusDp, 0.0001f)
        assertEquals(0.58f, tuning.bottomSheetBackgroundAlpha, 0.0001f)
        assertEquals(56f, tuning.bottomSheetBlurRadiusDp, 0.0001f)
    }

    private companion object {
        const val PreferencesName = "advanced-material-default-migration-test"
    }
}
