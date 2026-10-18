package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class SoftGlassPressFeedbackTest {
    @Test
    fun floatingActionButtonUsesExpandedImmersiveSize() {
        assertEquals(58.dp, SoftGlassFloatingActionButtonDefaults.Size)
        assertEquals(30.dp, SoftGlassFloatingActionButtonDefaults.IconSize)
    }

}
