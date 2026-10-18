package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning

import org.junit.Assert.*
import org.junit.Test

class GlassEdgeHighlightTest {
    @Test fun defaultLightTintIsExactlyGray249() {
        val tint = AdvancedMaterialFineTuning.Default.softGlassTintColor(false)
        assertEquals(0xFFF9F9F9.toInt(), tint.toArgb())
        assertEquals(249f / 255f, tint.red, 0f)
        assertEquals(tint.red, tint.green, 0f)
        assertEquals(tint.red, tint.blue, 0f)
    }

    @Test fun lightRimIsBrighterWhileDarkModeKeepsItsIndependentAttenuation() {
        val edge = GlassEdgeOpticsSpec()
        assertTrue(edge.highlightAlpha(false) > edge.highlightAlpha * 0.85f)
        assertEquals(edge.highlightAlpha(false) * edge.darkHighlightMultiplier, edge.highlightAlpha(true), 0.0001f)
        assertTrue(edge.highlightAlpha(true) < 0.2f)
        assertTrue(edge.fallbackAlpha(false) > edge.fallbackLightAlpha * 0.45f)
        assertEquals(edge.fallbackDarkAlpha * 0.45f, edge.fallbackAlpha(true), 0.0001f)
    }

    @Test fun disabledHighlightsStayDisabledAndCustomDarkIntensityIsBounded() {
        val hidden = GlassEdgeOpticsSpec(highlightAlpha = 0f, fallbackLightAlpha = 0f, fallbackDarkAlpha = 0f)
        for (dark in listOf(false, true)) {
            assertEquals(0f, hidden.highlightAlpha(dark), 0f)
            assertEquals(0f, hidden.fallbackAlpha(dark), 0f)
        }
        assertEquals(1f, GlassEdgeOpticsSpec(highlightAlpha = 1f, darkHighlightMultiplier = 2f)
            .highlightAlpha(true), 0f)
    }
    @Test fun strongerLightEdgeRemainsSubDpAndDoesNotChangeRefractionGeometry() {
        val edge = GlassEdgeOpticsSpec()
        assertTrue(edge.edgeStrokeWidth(false) <= 0.75.dp)
        assertEquals(0.375.dp, edge.edgeStrokeWidth(true))
        assertEquals(0.dp, edge.copy(fallbackWidth = 0.dp).edgeStrokeWidth(false))
        assertEquals(0.6.dp, edge.rimWidth)
        assertEquals(18.dp, GlassRefractionSpec().height)
        assertEquals(1.dp, GlassRefractionSpec().chromaticAberration)
    }

}
