package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
class DropdownAnimationPerformanceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fallbackIntermediateFramesDoNotRecomposeOrRemeasure() = assertFrames(glass = false)

    @Test fun windowBackdropIntermediateFramesDoNotRecomposeOrRemeasure() = assertFrames(glass = true)

    private fun assertFrames(glass: Boolean) {
        var measurements = 0
        var compositions = 0
        val show = mutableStateOf(true)
        var closed = 0
        val progress = Animatable(0f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MiuixTheme {
                CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = glass)) {
                    WindowGlassBackdropHost {
                        SoftGlassDropdownPopup(show.value, "Menu", Rect(220f, 500f, 360f, 524f),
                            listOf("Small", "Medium", "Large"), 1, { show.value = false }, { closed++ }, {}, progress = progress,
                            itemLeading = {
                                SideEffect { compositions++ }
                                Box(Modifier.size(20.dp).layout { measurable, constraints ->
                                    measurements++
                                    val child = measurable.measure(constraints)
                                    layout(child.width, child.height) { child.place(0, 0) }
                                })
                            })
                    }
                }
            }
        }
        // Pump actual frames (including UI placement callbacks), not only the animation clock.
        repeat(6) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
        assertTrue("Measure an active entrance, not an idle popup", progress.value > 0f && progress.value < 1f)
        val initialMeasures = measurements
        val initialComposes = compositions
        val recomposers = Recomposer.runningRecomposers.value.toList()
        val changes = recomposers.sumOf { it.changeCount }
        repeat(4) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
        assertEquals("No menu remeasurement per frame", initialMeasures, measurements)
        assertEquals("No option recomposition per frame", initialComposes, compositions)
        assertEquals("The popup material must not rebuild per frame", changes, recomposers.sumOf { it.changeCount })
        // Closing mid-entrance must still dispose the window and invoke completion exactly once.
        val previousCloses = closed
        compose.runOnIdle { show.value = false }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(previousCloses + 1, closed)
    }
}
