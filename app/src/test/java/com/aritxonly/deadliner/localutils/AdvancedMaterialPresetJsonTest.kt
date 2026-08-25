package com.aritxonly.deadliner.localutils

import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedMaterialPresetJsonTest {
    @Test
    fun exportContainsVersionRadiusAndAllFineTuningGroups() {
        val json = AdvancedMaterialPresetJson.encode(
            blurRadius = 144f,
            fineTuning = AdvancedMaterialFineTuning.Default.copy(
                glassTintLightGray = 0.91f,
                glassTintDarkGray = 0.12f,
                edgeHighlightGray = 0.78f,
                refractionAmountDp = 21f,
            ),
        )
        val root = JsonParser.parseString(json).asJsonObject
        val tuning = root.getAsJsonObject("fineTuning")

        assertEquals(AdvancedMaterialPresetJson.SchemaVersion, root["schemaVersion"].asInt)
        assertEquals(AdvancedMaterialPresetJson.PresetType, root["type"].asString)
        assertEquals(144f, root["blurRadius"].asFloat, 0.0001f)
        assertEquals(0.91f, tuning["glassTintLightGray"].asFloat, 0.0001f)
        assertEquals(0.12f, tuning["glassTintDarkGray"].asFloat, 0.0001f)
        assertEquals(0.78f, tuning["edgeHighlightGray"].asFloat, 0.0001f)
        assertEquals(21f, tuning["refractionAmountDp"].asFloat, 0.0001f)
        assertTrue(tuning.has("postRefractionBlurRadiusDp"))
    }
}
