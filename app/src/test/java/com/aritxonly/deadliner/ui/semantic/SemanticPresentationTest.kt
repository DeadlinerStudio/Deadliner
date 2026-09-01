package com.aritxonly.deadliner.ui.semantic

import androidx.compose.ui.graphics.toArgb
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.semantic.SemanticTone
import com.aritxonly.deadliner.ui.base.TopBarMaterialRevealRangeFraction
import com.aritxonly.deadliner.ui.base.TopBarMaterialRevealStartFraction
import com.aritxonly.deadliner.ui.base.resolveTopBarButtonMaterialProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticPresentationTest {
    @Test
    fun tonesMapToExistingProgressIndicatorRoles() {
        assertEquals(DDLStatus.UNDERGO, SemanticTone.ACCENT.progressColorRole().status)
        assertEquals(DDLStatus.COMPLETED, SemanticTone.SUCCESS.progressColorRole().status)
        assertEquals(DDLStatus.NEAR, SemanticTone.WARNING.progressColorRole().status)
        assertEquals(DDLStatus.PASSED, SemanticTone.DANGER.progressColorRole().status)
        assertFalse(SemanticTone.CALM.progressColorRole().useDisabledStyle)
        assertTrue(SemanticTone.NEUTRAL.progressColorRole().useDisabledStyle)
    }

    @Test
    fun atmosphereUsesFixedHeightAndFadesAcrossMaterialProgress() {
        assertEquals(224f, SemanticAtmosphereHeightDp, 0.0001f)
        assertEquals(1f, resolveSemanticAtmosphereAlpha(-1f), 0.0001f)
        assertEquals(0.5f, resolveSemanticAtmosphereAlpha(0.5f), 0.0001f)
        assertEquals(0f, resolveSemanticAtmosphereAlpha(1f), 0.0001f)
        assertEquals(0f, resolveSemanticAtmosphereAlpha(2f), 0.0001f)
    }

    @Test
    fun atmosphereFadeUsesTheSoftGlassRevealThresholds() {
        val glassAtRevealStart = resolveTopBarButtonMaterialProgress(
            collapsedFraction = TopBarMaterialRevealStartFraction,
            scrollProgress = null,
            overrideProgress = null,
        )
        val glassAtRevealEnd = resolveTopBarButtonMaterialProgress(
            collapsedFraction = TopBarMaterialRevealStartFraction + TopBarMaterialRevealRangeFraction,
            scrollProgress = null,
            overrideProgress = null,
        )

        assertEquals(1f, resolveSemanticAtmosphereAlpha(glassAtRevealStart), 0.0001f)
        assertEquals(0f, resolveSemanticAtmosphereAlpha(glassAtRevealEnd), 0.0001f)
    }

    @Test
    fun otherRootPagesUseHarmonyModuleColorsInLightAndDarkThemes() {
        assertModuleColors(SemanticAtmosphereModule.OVERVIEW, 0xFF0FA6A6, 0xFF41D1C7)
        assertModuleColors(SemanticAtmosphereModule.CAPTURE, 0xFF5856D6, 0xFF8E8BFF)
    }

    private fun assertModuleColors(
        module: SemanticAtmosphereModule,
        expectedLight: Long,
        expectedDark: Long,
    ) {
        assertEquals(expectedLight.toInt(), resolveSemanticModuleColor(module, darkTheme = false).toArgb())
        assertEquals(expectedDark.toInt(), resolveSemanticModuleColor(module, darkTheme = true).toArgb())
    }
}
