package com.aritxonly.deadliner.ui.main.modern

import com.aritxonly.deadliner.ui.main.shared.MainSection
import com.aritxonly.deadliner.ui.semantic.SemanticAtmosphereModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernSemanticAtmosphereTest {
    @Test
    fun rootSectionsMapToHarmonySemanticModules() {
        assertNull(semanticAtmosphereModuleFor(MainSection.LIST))
        assertEquals(
            SemanticAtmosphereModule.OVERVIEW,
            semanticAtmosphereModuleFor(MainSection.OVERVIEW),
        )
        assertEquals(
            SemanticAtmosphereModule.CAPTURE,
            semanticAtmosphereModuleFor(MainSection.CAPTURE),
        )
        assertNull(semanticAtmosphereModuleFor(MainSection.LIFI))
    }

    @Test
    fun lifiIsExcludedFromSemanticAtmosphere() {
        assertTrue(shouldShowSemanticAtmosphere(MainSection.LIST))
        assertTrue(shouldShowSemanticAtmosphere(MainSection.OVERVIEW))
        assertTrue(shouldShowSemanticAtmosphere(MainSection.CAPTURE))
        assertFalse(shouldShowSemanticAtmosphere(MainSection.LIFI))
    }

    @Test
    fun overviewAndCaptureReuseObservedTopBarScrollProgress() {
        assertEquals(
            0.75f,
            semanticAtmosphereMaterialProgress(MainSection.OVERVIEW, 0.1f, 0.75f),
            0.0001f,
        )
        assertEquals(
            0.6f,
            semanticAtmosphereMaterialProgress(MainSection.CAPTURE, 0.1f, 0.6f),
            0.0001f,
        )
        assertEquals(
            0.1f,
            semanticAtmosphereMaterialProgress(MainSection.LIST, 0.1f, 0.8f),
            0.0001f,
        )
        assertEquals(
            0f,
            semanticAtmosphereMaterialProgress(MainSection.LIFI, 0.8f, 0.8f),
            0.0001f,
        )
    }
}
