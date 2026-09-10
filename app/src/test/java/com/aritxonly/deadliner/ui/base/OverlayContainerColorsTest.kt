package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayContainerColorsTest {
    @Test
    fun overlayUsesMiuixSurfaceContainer() {
        val miuix = Color(0xFF242424)

        assertEquals(miuix, resolveOverlayContainerColor(miuix))
    }
}
