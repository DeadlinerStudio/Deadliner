package com.aritxonly.deadliner.ui

import com.aritxonly.deadliner.R
import java.io.File
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class InterfaceIconPackTest {
    @Test fun everyRegisteredSymbolHasAHybridMappingExceptPreservedOriginals() {
        val symbols = R.drawable::class.java.fields.mapNotNull { MaterialSymbolRegistry.definitionFor(it.getInt(null)) }
        assertTrue(symbols.size > 100)
        symbols.filter { it.name !in setOf("android", "house") }.forEach {
            assertNotNull("Missing Hybrid ${it.name}", HybridIconRegistry.resource(it.name, it.filled))
            it.modernName?.let { name -> assertNotNull(name, HybridIconRegistry.resource(name, it.filled)) }
        }
        assertNull(HybridIconRegistry.resource("android"))
        assertNull(HybridIconRegistry.resource("house"))
        assertNotNull(HybridIconRegistry.resource("home"))
        assertNull(MaterialSymbolRegistry.definitionFor(R.drawable.ic_lifi))
    }
    @Test fun dynamicActionsAreCoveredAndUnknownNamesKeepTheirMaterialFallback() {
        listOf("auto_awesome","repeat","notes","notifications","expand_more","open_in_full","side_navigation","timer")
            .forEach { assertNotNull(it, HybridIconRegistry.resource(it)) }
        assertNull(HybridIconRegistry.resource("unmapped_future_icon"))
    }
    @Test fun favoriteAndStarContentAndFilledStatesCannotBeConfused() {
        val root = listOf(File("../scripts/interface_icon_map.json"), File("scripts/interface_icon_map.json"))
            .first { it.exists() }
        val rows = Json.parseToJsonElement(root.readText()).jsonObject.getValue("icons").jsonObject
        fun icon(key: String) = rows.getValue(key).jsonObject.getValue("icon").jsonPrimitive.content
        assertEquals("Favorites", icon("favorite"))
        assertEquals("FavoritesFill", icon("favorite#filled"))
        assertEquals("Starred", icon("star"))
        assertEquals("star", icon("star#filled"))
        assertNotEquals(HybridIconRegistry.resource("star"), HybridIconRegistry.resource("star", true))
        assertNotEquals(HybridIconRegistry.resource("favorite"), HybridIconRegistry.resource("favorite", true))
        listOf("favorite","favorite#filled","star","star#filled","bookmark").forEach {
            assertEquals("strict-content", rows.getValue(it).jsonObject.getValue("matchPolicy").jsonPrimitive.content)
        }
    }
    @Test fun phosphorOpticalCalibrationIsExplicitPerIconAndMiuixIsUntouched() {
        val path = listOf(File("../scripts/interface_icon_map.json"), File("scripts/interface_icon_map.json")).first { it.exists() }
        val rows = Json.parseToJsonElement(path.readText()).jsonObject.getValue("icons").jsonObject
        val aliases = mutableMapOf<String, Double>()
        rows.forEach { (key, value) ->
            val row = value.jsonObject
            if (!row.getValue("family").jsonPrimitive.content.startsWith("phosphor-")) {
                assertFalse(key, "opticalScale" in row)
            } else {
                val scale = row.getValue("opticalScale").jsonPrimitive.double
                assertTrue(key, scale in .8..1.3)
                assertTrue(key, row.getValue("opticalReview").jsonPrimitive.content.isNotBlank())
                val signature = listOf("family", "icon", "overlay", "cutoutCircle", "cutoutRect", "baseTransform", "overlayTransform")
                    .joinToString { row[it].toString() }
                aliases[signature]?.let { assertEquals(key, it, scale, .00001) }
                aliases[signature] = scale
            }
        }
        assertNotEquals(HybridIconRegistry.resource("check_circle"), HybridIconRegistry.resource("check_circle", true))
    }
    @Test fun rtlOnlyMirrorsDirectionalActionsAndPreservesObjectsAndMediaControls() {
        listOf("arrow_back","arrow_back_ios_new","chevron_left","send","undo")
            .forEach { assertTrue(it, InterfaceIconDirection.mirrorsInRtl(it)) }
        listOf("favorite","star","play_arrow","clock","android","notifications","money_off")
            .forEach { assertFalse(it, InterfaceIconDirection.mirrorsInRtl(it)) }
    }
}
