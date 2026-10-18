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
        assertEquals(52f, AdvancedMaterialFineTuningPreferences.readBlurRadius(
            preferences,
            AdvancedMaterialLevel.Soft,
        ), 0.0001f)
        assertEquals(1.096648f, tuning.saturationMultiplier, 0.0001f)
        assertEquals(0.94692737f, tuning.edgeHighlightAlpha, 0.0001f)
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

    @Test fun versionFourDefaultsMigrateButCustomBlurValuesSurvive() {
        preferences.edit()
            .putInt("advanced_material_fine_defaults_version", 4)
            .putFloat("advanced_material_blur_radius", 92f)
            .putFloat("advanced_material_fine_glass_blur_radius_multiplier", 0.25f).commit()
        assertEquals(52f, AdvancedMaterialFineTuningPreferences.readBlurRadius(
            preferences, AdvancedMaterialLevel.Soft), 0.0001f)
        assertEquals(0.4f, AdvancedMaterialFineTuningPreferences.read(preferences).glassBlurRadiusMultiplier, 0.0001f)
        preferences.edit().putFloat("advanced_material_blur_radius", 72f)
            .putFloat("advanced_material_fine_glass_blur_radius_multiplier", 0.32f).commit()
        assertEquals(72f, AdvancedMaterialFineTuningPreferences.readBlurRadius(
            preferences, AdvancedMaterialLevel.Soft), 0.0001f)
        assertEquals(0.32f, AdvancedMaterialFineTuningPreferences.read(preferences).glassBlurRadiusMultiplier, 0.0001f)
        preferences.edit().putFloat("advanced_material_blur_radius", 240f).commit()
        assertEquals(92f, AdvancedMaterialFineTuningPreferences.readBlurRadius(
            preferences, AdvancedMaterialLevel.Deep), 0.0001f)
    }

    @Test fun currentMaximumIsNotMistakenForAnOldDefault() {
        AdvancedMaterialFineTuningPreferences.write(preferences.edit(), AdvancedMaterialFineTuning.Default)
            .putFloat("advanced_material_blur_radius", 92f).commit()
        assertEquals(92f, AdvancedMaterialFineTuningPreferences.readBlurRadius(
            preferences, AdvancedMaterialLevel.Soft), 0.0001f)
    }

    @Test fun versionFiveDefaultsMigrateToUserPresetWhileCustomizedValuesSurvive() {
        preferences.edit().putInt("advanced_material_fine_defaults_version", 5)
            .putFloat("advanced_material_fine_saturation_multiplier", 1.05f)
            .putFloat("advanced_material_fine_glass_tint_light_gray", 0.99f)
            .putFloat("advanced_material_fine_edge_highlight_alpha", 0.95f)
            .putFloat("advanced_material_fine_dark_highlight_multiplier", 0.20f).commit()
        assertEquals(AdvancedMaterialFineTuning.Default, AdvancedMaterialFineTuningPreferences.read(preferences))
        preferences.edit().putFloat("advanced_material_fine_saturation_multiplier", 1.12f)
            .putFloat("advanced_material_fine_glass_tint_light_gray", 0.93f)
            .putFloat("advanced_material_fine_edge_highlight_alpha", 0.71f)
            .putFloat("advanced_material_fine_dark_highlight_multiplier", 0.37f).commit()
        val customized = AdvancedMaterialFineTuningPreferences.read(preferences)
        assertEquals(1.12f, customized.saturationMultiplier, 0f)
        assertEquals(0.93f, customized.glassTintLightGray, 0f)
        assertEquals(0.71f, customized.edgeHighlightAlpha, 0f)
        assertEquals(0.37f, customized.darkHighlightMultiplier, 0f)
        AdvancedMaterialFineTuningPreferences.write(preferences.edit(), customized).commit()
        assertEquals(6, preferences.getInt("advanced_material_fine_defaults_version", 0))
        assertEquals(customized, AdvancedMaterialFineTuningPreferences.read(preferences))
    }

    @Test fun currentCustomValueMatchingHistoricalDefaultIsNotMigrated() {
        val custom = AdvancedMaterialFineTuning.Default.copy(saturationMultiplier = 1.05f,
            glassTintLightGray = 0.99f, edgeHighlightAlpha = 0.95f, darkHighlightMultiplier = 0.20f)
        AdvancedMaterialFineTuningPreferences.write(preferences.edit(), custom).commit()
        assertEquals(custom, AdvancedMaterialFineTuningPreferences.read(preferences))
    }

    private companion object {
        const val PreferencesName = "advanced-material-default-migration-test"
    }
}
