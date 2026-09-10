package com.aritxonly.deadliner.localutils

import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.aritxonly.deadliner.model.AdvancedMaterialTuning
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

object AdvancedMaterialPresetJson {
    const val SchemaVersion = 1
    const val PresetType = "deadliner_advanced_material"
    const val SuggestedFileName = "deadliner-advanced-material-preset.json"

    fun encode(
        blurRadius: Float,
        fineTuning: AdvancedMaterialFineTuning,
    ): String {
        val tuning = fineTuning.normalized()
        val tuningJson = JsonObject().apply {
            addProperty("saturationMultiplier", tuning.saturationMultiplier)
            addProperty("noiseCoefficient", tuning.noiseCoefficient)
            addProperty("brightness", tuning.brightness)
            addProperty("contrast", tuning.contrast)
            addProperty("topBarTintAlpha", tuning.topBarTintAlpha)
            addProperty("navigationTintAlpha", tuning.navigationTintAlpha)
            addProperty("dialogBackgroundAlpha", tuning.dialogBackgroundAlpha)
            addProperty("dialogBlurRadiusDp", tuning.dialogBlurRadiusDp)
            addProperty("bottomSheetBackgroundAlpha", tuning.bottomSheetBackgroundAlpha)
            addProperty("bottomSheetBlurRadiusDp", tuning.bottomSheetBlurRadiusDp)
            addProperty("glassBlurRadiusMultiplier", tuning.glassBlurRadiusMultiplier)
            addProperty("glassNoiseMultiplier", tuning.glassNoiseMultiplier)
            addProperty("glassTintAlphaMultiplier", tuning.glassTintAlphaMultiplier)
            addProperty("glassTintLightGray", tuning.glassTintLightGray)
            addProperty("glassTintDarkGray", tuning.glassTintDarkGray)
            addProperty("refractionEnabled", tuning.refractionEnabled)
            addProperty("refractionHeightDp", tuning.refractionHeightDp)
            addProperty("refractionAmountDp", tuning.refractionAmountDp)
            addProperty("refractionDepthEffect", tuning.refractionDepthEffect)
            addProperty("chromaticAberrationDp", tuning.chromaticAberrationDp)
            addProperty("edgeHighlightAlpha", tuning.edgeHighlightAlpha)
            addProperty("edgeHighlightGray", tuning.edgeHighlightGray)
            addProperty("darkHighlightMultiplier", tuning.darkHighlightMultiplier)
            addProperty("fallbackEdgeWidthDp", tuning.fallbackEdgeWidthDp)
            addProperty("fallbackLightAlpha", tuning.fallbackLightAlpha)
            addProperty("fallbackDarkAlpha", tuning.fallbackDarkAlpha)
            addProperty("postRefractionBlurEnabled", tuning.postRefractionBlurEnabled)
            addProperty("postRefractionBlurRadiusDp", tuning.postRefractionBlurRadiusDp)
        }
        val root = JsonObject().apply {
            addProperty("schemaVersion", SchemaVersion)
            addProperty("type", PresetType)
            addProperty("blurRadius", AdvancedMaterialTuning.normalizeBlurRadius(blurRadius))
            add("fineTuning", tuningJson)
        }
        return GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
            .toJson(root)
    }
}
