package com.aritxonly.deadliner.localutils

import android.content.SharedPreferences
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.aritxonly.deadliner.model.AdvancedMaterialLevel
import com.aritxonly.deadliner.model.AdvancedMaterialTuning

object AdvancedMaterialFineTuningPreferences {
    private const val Prefix = "advanced_material_fine_"
    private const val CurrentDefaultsVersion = 4
    private const val PreviousBlurRadiusDefault = 128f
    private const val BlurRadiusKey = "advanced_material_blur_radius"
    private const val LegacyLevelKey = "advanced_material_level"

    fun readBlurRadius(
        preferences: SharedPreferences,
        legacyLevel: AdvancedMaterialLevel,
    ): Float {
        if (!preferences.contains(BlurRadiusKey)) {
            return if (preferences.contains(LegacyLevelKey)) {
                legacyLevel.blurRadius
            } else {
                AdvancedMaterialTuning.DefaultBlurRadius
            }
        }
        val stored = AdvancedMaterialTuning.normalizeBlurRadius(
            preferences.getFloat(BlurRadiusKey, AdvancedMaterialTuning.DefaultBlurRadius),
        )
        val defaultsVersion = preferences.getInt(key("defaults_version"), 1)
        return if (defaultsVersion < CurrentDefaultsVersion && stored == PreviousBlurRadiusDefault) {
            AdvancedMaterialTuning.DefaultBlurRadius
        } else {
            stored
        }
    }

    fun read(preferences: SharedPreferences): AdvancedMaterialFineTuning {
        val defaults = AdvancedMaterialFineTuning.Default
        val defaultsVersion = preferences.getInt(key("defaults_version"), 1)
        val previousDefaults = when {
            defaultsVersion < 2 -> AdvancedMaterialFineTuning.DefaultsV1
            defaultsVersion < 3 -> AdvancedMaterialFineTuning.DefaultsV2
            else -> AdvancedMaterialFineTuning.DefaultsV3
        }
        val migrateDefaults = defaultsVersion < CurrentDefaultsVersion
        return AdvancedMaterialFineTuning(
            saturationMultiplier = preferences.migratedFloat(
                "saturation_multiplier",
                defaults.saturationMultiplier,
                previousDefaults.saturationMultiplier,
                migrateDefaults,
            ),
            noiseCoefficient = preferences.migratedFloat(
                "noise_coefficient",
                defaults.noiseCoefficient,
                previousDefaults.noiseCoefficient,
                migrateDefaults,
            ),
            brightness = preferences.migratedFloat(
                "brightness",
                defaults.brightness,
                previousDefaults.brightness,
                migrateDefaults,
            ),
            contrast = preferences.migratedFloat(
                "contrast",
                defaults.contrast,
                previousDefaults.contrast,
                migrateDefaults,
            ),
            topBarTintAlpha = preferences.migratedFloat(
                "top_bar_tint_alpha",
                defaults.topBarTintAlpha,
                previousDefaults.topBarTintAlpha,
                migrateDefaults,
            ),
            navigationTintAlpha = preferences.migratedFloat(
                "navigation_tint_alpha",
                defaults.navigationTintAlpha,
                previousDefaults.navigationTintAlpha,
                migrateDefaults,
            ),
            dialogBackgroundAlpha = preferences.migratedFloat(
                "dialog_background_alpha",
                defaults.dialogBackgroundAlpha,
                previousDefaults.dialogBackgroundAlpha,
                migrateDefaults,
            ),
            dialogBlurRadiusDp = preferences.migratedFloat(
                "dialog_blur_radius_dp",
                defaults.dialogBlurRadiusDp,
                previousDefaults.dialogBlurRadiusDp,
                migrateDefaults,
            ),
            bottomSheetBackgroundAlpha = preferences.migratedFloat(
                "bottom_sheet_background_alpha",
                defaults.bottomSheetBackgroundAlpha,
                previousDefaults.bottomSheetBackgroundAlpha,
                migrateDefaults,
            ),
            bottomSheetBlurRadiusDp = preferences.migratedFloat(
                "bottom_sheet_blur_radius_dp",
                defaults.bottomSheetBlurRadiusDp,
                previousDefaults.bottomSheetBlurRadiusDp,
                migrateDefaults,
            ),
            glassBlurRadiusMultiplier = preferences.migratedFloat(
                "glass_blur_radius_multiplier",
                defaults.glassBlurRadiusMultiplier,
                previousDefaults.glassBlurRadiusMultiplier,
                migrateDefaults,
            ),
            glassNoiseMultiplier = preferences.migratedFloat(
                "glass_noise_multiplier",
                defaults.glassNoiseMultiplier,
                previousDefaults.glassNoiseMultiplier,
                migrateDefaults,
            ),
            glassTintAlphaMultiplier = preferences.migratedFloat(
                "glass_tint_alpha_multiplier",
                defaults.glassTintAlphaMultiplier,
                previousDefaults.glassTintAlphaMultiplier,
                migrateDefaults,
            ),
            glassTintLightGray = preferences.migratedFloat(
                "glass_tint_light_gray",
                defaults.glassTintLightGray,
                previousDefaults.glassTintLightGray,
                migrateDefaults,
            ),
            glassTintDarkGray = preferences.migratedFloat(
                "glass_tint_dark_gray",
                defaults.glassTintDarkGray,
                previousDefaults.glassTintDarkGray,
                migrateDefaults,
            ),
            refractionEnabled = preferences.getBoolean(
                key("refraction_enabled"),
                defaults.refractionEnabled,
            ),
            refractionHeightDp = preferences.migratedFloat(
                "refraction_height_dp",
                defaults.refractionHeightDp,
                previousDefaults.refractionHeightDp,
                migrateDefaults,
            ),
            refractionAmountDp = preferences.migratedFloat(
                "refraction_amount_dp",
                defaults.refractionAmountDp,
                previousDefaults.refractionAmountDp,
                migrateDefaults,
            ),
            refractionDepthEffect = preferences.migratedFloat(
                "refraction_depth_effect",
                defaults.refractionDepthEffect,
                previousDefaults.refractionDepthEffect,
                migrateDefaults,
            ),
            chromaticAberrationDp = preferences.migratedFloat(
                "chromatic_aberration_dp",
                defaults.chromaticAberrationDp,
                previousDefaults.chromaticAberrationDp,
                migrateDefaults,
            ),
            edgeHighlightAlpha = preferences.migratedFloat(
                "edge_highlight_alpha",
                defaults.edgeHighlightAlpha,
                previousDefaults.edgeHighlightAlpha,
                migrateDefaults,
            ),
            edgeHighlightGray = preferences.migratedFloat(
                "edge_highlight_gray",
                defaults.edgeHighlightGray,
                previousDefaults.edgeHighlightGray,
                migrateDefaults,
            ),
            darkHighlightMultiplier = preferences.migratedFloat(
                "dark_highlight_multiplier",
                defaults.darkHighlightMultiplier,
                previousDefaults.darkHighlightMultiplier,
                migrateDefaults,
            ),
            fallbackEdgeWidthDp = preferences.migratedFloat(
                "fallback_edge_width_dp",
                defaults.fallbackEdgeWidthDp,
                previousDefaults.fallbackEdgeWidthDp,
                migrateDefaults,
            ),
            fallbackLightAlpha = preferences.migratedFloat(
                "fallback_light_alpha",
                defaults.fallbackLightAlpha,
                previousDefaults.fallbackLightAlpha,
                migrateDefaults,
            ),
            fallbackDarkAlpha = preferences.migratedFloat(
                "fallback_dark_alpha",
                defaults.fallbackDarkAlpha,
                previousDefaults.fallbackDarkAlpha,
                migrateDefaults,
            ),
            postRefractionBlurEnabled = preferences.getBoolean(
                key("post_refraction_blur_enabled"),
                defaults.postRefractionBlurEnabled,
            ),
            postRefractionBlurRadiusDp = preferences.migratedFloat(
                "post_refraction_blur_radius_dp",
                defaults.postRefractionBlurRadiusDp,
                previousDefaults.postRefractionBlurRadiusDp,
                migrateDefaults,
            ),
        ).normalized()
    }

    fun write(
        editor: SharedPreferences.Editor,
        value: AdvancedMaterialFineTuning,
    ) = with(value.normalized()) {
        editor.putInt(key("defaults_version"), CurrentDefaultsVersion)
        editor.putFloat(key("saturation_multiplier"), saturationMultiplier)
        editor.putFloat(key("noise_coefficient"), noiseCoefficient)
        editor.putFloat(key("brightness"), brightness)
        editor.putFloat(key("contrast"), contrast)
        editor.putFloat(key("top_bar_tint_alpha"), topBarTintAlpha)
        editor.putFloat(key("navigation_tint_alpha"), navigationTintAlpha)
        editor.putFloat(key("dialog_background_alpha"), dialogBackgroundAlpha)
        editor.putFloat(key("dialog_blur_radius_dp"), dialogBlurRadiusDp)
        editor.putFloat(key("bottom_sheet_background_alpha"), bottomSheetBackgroundAlpha)
        editor.putFloat(key("bottom_sheet_blur_radius_dp"), bottomSheetBlurRadiusDp)
        editor.putFloat(key("glass_blur_radius_multiplier"), glassBlurRadiusMultiplier)
        editor.putFloat(key("glass_noise_multiplier"), glassNoiseMultiplier)
        editor.putFloat(key("glass_tint_alpha_multiplier"), glassTintAlphaMultiplier)
        editor.putFloat(key("glass_tint_light_gray"), glassTintLightGray)
        editor.putFloat(key("glass_tint_dark_gray"), glassTintDarkGray)
        editor.putBoolean(key("refraction_enabled"), refractionEnabled)
        editor.putFloat(key("refraction_height_dp"), refractionHeightDp)
        editor.putFloat(key("refraction_amount_dp"), refractionAmountDp)
        editor.putFloat(key("refraction_depth_effect"), refractionDepthEffect)
        editor.putFloat(key("chromatic_aberration_dp"), chromaticAberrationDp)
        editor.putFloat(key("edge_highlight_alpha"), edgeHighlightAlpha)
        editor.putFloat(key("edge_highlight_gray"), edgeHighlightGray)
        editor.putFloat(key("dark_highlight_multiplier"), darkHighlightMultiplier)
        editor.putFloat(key("fallback_edge_width_dp"), fallbackEdgeWidthDp)
        editor.putFloat(key("fallback_light_alpha"), fallbackLightAlpha)
        editor.putFloat(key("fallback_dark_alpha"), fallbackDarkAlpha)
        editor.putBoolean(key("post_refraction_blur_enabled"), postRefractionBlurEnabled)
        editor.putFloat(key("post_refraction_blur_radius_dp"), postRefractionBlurRadiusDp)
    }

    private fun SharedPreferences.migratedFloat(
        suffix: String,
        default: Float,
        previousDefault: Float,
        migrateDefaults: Boolean,
    ): Float {
        val preferenceKey = key(suffix)
        if (!contains(preferenceKey)) return default
        val stored = getFloat(preferenceKey, default)
        return if (migrateDefaults && stored == previousDefault) default else stored
    }

    private fun key(suffix: String): String = Prefix + suffix
}
