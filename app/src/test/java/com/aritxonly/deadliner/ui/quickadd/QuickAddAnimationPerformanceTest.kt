package com.aritxonly.deadliner.ui.quickadd

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
class QuickAddAnimationPerformanceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun morphFramesAndReversalDoNotRecomposeOrRemeasureTheInput() {
        val progress = mutableFloatStateOf(0.1f)
        val bottom = mutableFloatStateOf(100f)
        val height = mutableIntStateOf(220)
        var compositions = 0
        var measurements = 0
        compose.setContent {
            QuickAddMorphLayout(progress, Rect(340f, 710f, 400f, 770f), 11f, bottom,
                surface = { QuickAddMorphSurface(progress, it) }) {
                SideEffect { compositions++ }
                Box(Modifier.testTag("input").height(height.intValue.dp).layout { measurable, constraints ->
                    measurements++
                    val child = measurable.measure(constraints)
                    layout(child.width, child.height) { child.place(0, 0) }
                })
            }
        }
        compose.waitForIdle()
        val initialCompositions = compositions
        val initialMeasurements = measurements
        // Also cover a reversal before settling, rather than only jumping to the final size.
        for (p in listOf(0.2f, 0.4f, 0.6f, 0.3f, 0.1f, 0.5f, 0.8f, 0.95f, 1.04f, 1f)) {
            compose.runOnIdle { progress.floatValue = p }
            compose.waitForIdle()
        }
        assertEquals("Morph must not recompose the input", initialCompositions, compositions)
        assertEquals("Morph must not remeasure the input", initialMeasurements, measurements)
        val before = compose.onNodeWithTag("input").fetchSemanticsNode().boundsInRoot
        assertEquals(387f, before.width, 0.01f)
        compose.runOnIdle { height.intValue = 300 }
        compose.waitForIdle()
        val taller = compose.onNodeWithTag("input").fetchSemanticsNode().boundsInRoot
        assertEquals(300f, taller.height, 0.01f)
        assertEquals(before.bottom, taller.bottom, 0.01f)
        assertTrue(measurements > initialMeasurements)
        compose.runOnIdle { bottom.floatValue += 180f }
        compose.waitForIdle()
        val aboveIme = compose.onNodeWithTag("input").fetchSemanticsNode().boundsInRoot
        assertEquals(taller.bottom - 180f, aboveIme.bottom, 0.01f)
    }

    @Test fun transitionClockDoesNotRecomposeItsOverlayOnEveryFrameAndStillUnmounts() {
        val visible = mutableStateOf(false)
        var compositions = 0
        var active = false
        var progress: State<Float>? = null
        compose.setContent {
            val morph = quickAddMorphTransition(visible.value)
            SideEffect { compositions++; active = morph.active; progress = morph.progress }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { visible.value = true }
        repeat(6) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
        val start = compositions
        repeat(4) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
        assertTrue(active)
        assertTrue(requireNotNull(progress).value > 0f)
        assertEquals("Continuous progress stays out of the overlay composition", start, compositions)
        compose.runOnIdle { visible.value = false }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertFalse(active)
        assertEquals(0f, requireNotNull(progress).value, 0.001f)
    }
}
