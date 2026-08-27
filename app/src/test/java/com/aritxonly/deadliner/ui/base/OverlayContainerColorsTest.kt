package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayContainerColorsTest {
    @Test
    fun overlayUsesTheActiveDesignSystemsSurfaceContainer() {
        val material = Color(0xFF202124)
        val miuix = Color(0xFF242424)

        assertEquals(
            material,
            resolveOverlayContainerColor(AppDesignSystem.MATERIAL3, material, miuix),
        )
        assertEquals(
            miuix,
            resolveOverlayContainerColor(AppDesignSystem.MIUIX, material, miuix),
        )
    }
}
