package com.aritxonly.deadliner.localutils

import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedMaterialPresetJsonTest {
    @Test fun defaultsMatchEveryFieldOfTheUserProvidedPreset() {
        val expected = javaClass.getResourceAsStream("/material/default-preset-v6.json")!!.bufferedReader().use {
            JsonParser.parseString(it.readText()).asJsonObject
        }
        val actual = JsonParser.parseString(AdvancedMaterialPresetJson.encode(
            com.aritxonly.deadliner.model.AdvancedMaterialTuning.DefaultBlurRadius,
            AdvancedMaterialFineTuning.Default,
        )).asJsonObject
        assertEquals(expected["blurRadius"].asFloat, actual["blurRadius"].asFloat, 0f)
        val expectedTuning = expected.getAsJsonObject("fineTuning")
        val actualTuning = actual.getAsJsonObject("fineTuning")
        assertEquals(expectedTuning.keySet(), actualTuning.keySet())
        expectedTuning.entrySet().forEach { (key, value) ->
            if (value.asJsonPrimitive.isBoolean) assertEquals(key, value.asBoolean, actualTuning[key].asBoolean)
            else assertEquals(key, value.asFloat, actualTuning[key].asFloat, 0f)
        }
    }

    @Test
    fun exportContainsVersionRadiusAndAllFineTuningGroups() {
        val json = AdvancedMaterialPresetJson.encode(
            blurRadius = 64f,
            fineTuning = AdvancedMaterialFineTuning.Default.copy(
                glassTintLightGray = 0.91f,
                glassTintDarkGray = 0.12f,
                edgeHighlightGray = 0.78f,
                refractionAmountDp = 21f,
                dialogBackgroundAlpha = 0.54f,
                bottomSheetBlurRadiusDp = 58f,
            ),
        )
        val root = JsonParser.parseString(json).asJsonObject
        val tuning = root.getAsJsonObject("fineTuning")

        assertEquals(AdvancedMaterialPresetJson.SchemaVersion, root["schemaVersion"].asInt)
        assertEquals(AdvancedMaterialPresetJson.PresetType, root["type"].asString)
        assertEquals(64f, root["blurRadius"].asFloat, 0.0001f)
        assertEquals(0.91f, tuning["glassTintLightGray"].asFloat, 0.0001f)
        assertEquals(0.12f, tuning["glassTintDarkGray"].asFloat, 0.0001f)
        assertEquals(0.78f, tuning["edgeHighlightGray"].asFloat, 0.0001f)
        assertEquals(21f, tuning["refractionAmountDp"].asFloat, 0.0001f)
        assertEquals(0.54f, tuning["dialogBackgroundAlpha"].asFloat, 0.0001f)
        assertEquals(58f, tuning["bottomSheetBlurRadiusDp"].asFloat, 0.0001f)
        assertTrue(tuning.has("postRefractionBlurRadiusDp"))
    }
}
