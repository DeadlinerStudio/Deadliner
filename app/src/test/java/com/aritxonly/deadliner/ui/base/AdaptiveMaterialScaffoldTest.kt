package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveMaterialScaffoldTest {
    private val miuixSurface = Color(0xFFF7F7F7)

    @Test
    fun `opaque container is recorded unchanged`() {
        val container = Color(0xFF123456)

        assertEquals(
            container,
            resolveBackdropCaptureColor(
                containerColor = container,
                miuixSurfaceColor = miuixSurface,
            ),
        )
    }

    @Test
    fun `transparent Miuix container records Miuix surface`() {
        assertEquals(
            miuixSurface,
            resolveBackdropCaptureColor(
                containerColor = Color.Transparent,
                miuixSurfaceColor = miuixSurface,
            ),
        )
    }

    @Test
    fun `translucent container records Miuix surface`() {
        assertEquals(
            miuixSurface,
            resolveBackdropCaptureColor(
                containerColor = Color.White.copy(alpha = 0.5f),
                miuixSurfaceColor = miuixSurface,
            ),
        )
    }

}
