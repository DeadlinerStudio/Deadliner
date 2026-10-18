package com.aritxonly.deadliner.ui.lifi

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.aritxonly.deadliner.ui.base.feedback.ProvideMiuixPressFeedback
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LifiFeedbackClippingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun carouselPressChangesInteriorButKeepsRoundedCornersOutsideTheFeedback() =
        verifyClipping("安排一个任务", carousel = true)

    @Test fun memoryActionPressKeepsItsRoundedOutlineAndStillClicks() =
        verifyClipping("记忆与偏好  ›", carousel = false)

    private fun verifyClipping(label: String, carousel: Boolean) {
        var clicks = 0
        compose.setContent {
            MiuixTheme {
                MaterialTheme(colorScheme = lightColorScheme(surfaceContainer = Color.White, onSurface = Color.Black)) {
                    ProvideMiuixPressFeedback {
                        Box(Modifier.fillMaxSize().background(Color.Magenta)) {
                            if (carousel) LifiWelcome(onSuggestion = { clicks++ }, enabled = true, autoAdvance = false)
                            else LifiMemoryButton(onClick = { clicks++ })
                        }
                    }
                }
            }
        }
        val target = compose.onNodeWithText(label)
        val idle = target.captureToImage().toPixelMap()
        compose.mainClock.autoAdvance = false
        target.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(700)
        val pressed = target.captureToImage().toPixelMap()
        // A strong contrasting background exposes rectangular feedback leaking into a corner.
        assertColorNear(Color.Magenta, idle[1, 1])
        assertColorNear(idle[1, 1], pressed[1, 1])
        val x = pressed.width / 2
        val y = pressed.height - 3
        assertTrue("A real press must change the interior", pressed[x, y].red < idle[x, y].red - 0.03f)
        target.performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    private fun assertColorNear(expected: Color, actual: Color) {
        assertEquals(expected.red, actual.red, 0.02f)
        assertEquals(expected.green, actual.green, 0.02f)
        assertEquals(expected.blue, actual.blue, 0.02f)
    }
}
