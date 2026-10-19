package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DropdownMorphDrawingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun drawingPhaseMorphMatchesTheOriginalClipAndShadowThroughBothDirections() {
        val progress = Animatable(0f)
        val reference = mutableStateOf(false)
        val source = Rect(40f, 80f, 130f, 104f)
        compose.setContent {
            Box(Modifier.testTag("fixture").size(260.dp, 320.dp).background(Color.White).padding(40.dp)) {
                val morph = if (reference.value) {
                    val shape = DropdownMorphShape(source, progress.value)
                    Modifier.dropShadow(shape, Shadow(20.dp, Color.Black,
                        offset = DpOffset(0.dp, 4.dp), alpha = 0.12f * progress.value)).clip(shape)
                } else Modifier.dropdownMorphDrawing(source, progress, dark = false)
                Box(Modifier.size(180.dp, 240.dp).then(morph).background(Color(0xFFF4F4F4)))
            }
        }
        for (fraction in listOf(0f, 0.15f, 0.5f, 1f, 0.65f, 0f)) {
            compose.runOnIdle { runBlocking { progress.snapTo(fraction) }; reference.value = false }
            val actual = compose.onNodeWithTag("fixture").captureToImage().toPixelMap()
            compose.runOnIdle { reference.value = true }
            val expected = compose.onNodeWithTag("fixture").captureToImage().toPixelMap()
            var maxDifference = 0f
            for (y in 0 until actual.height) for (x in 0 until actual.width) {
                maxDifference = maxOf(maxDifference, abs(actual[x, y].red - expected[x, y].red))
            }
            assertTrue("Clip/shadow must remain identical at $fraction (error=$maxDifference)",
                maxDifference <= 2f / 255f)
        }
    }
}
