package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.base.feedback.MaterialShapeFeedback
import com.aritxonly.deadliner.ui.base.feedback.ProvideMiuixPressFeedback
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
class MaterialShapeFeedbackTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nativeConnectedToggleKeepsItsAsymmetricOutlineAndOwnFeedback() {
        var clicks = 0
        var scopedRipple = false
        var globalRippleDisabled = false
        compose.setContent {
            MaterialTheme {
                ProvideMiuixPressFeedback {
                    val global = LocalRippleConfiguration.current
                    SideEffect { globalRippleDisabled = global == null }
                    Row(Modifier.background(Color.Magenta)) {
                        // A connected segment has square inner corners, not a capsule.
                        val shape = RoundedCornerShape(20.dp, 2.dp, 2.dp, 20.dp)
                        val shapes = ToggleButtonShapes(shape = shape, pressedShape = shape, checkedShape = shape)
                        val colors = ToggleButtonDefaults.toggleButtonColors(containerColor = Color.White)
                        ToggleButton(checked = false, onCheckedChange = {}, shapes = shapes, colors = colors,
                            modifier = Modifier.size(100.dp, 48.dp).testTag("native")) { Box(Modifier.size(1.dp)) }
                        MaterialShapeFeedback {
                            val scoped = LocalRippleConfiguration.current
                            SideEffect { scopedRipple = scoped != null }
                            ToggleButton(checked = false, onCheckedChange = { clicks++ }, shapes = shapes, colors = colors,
                                modifier = Modifier.size(100.dp, 48.dp).testTag("wrapped")) { Box(Modifier.size(1.dp)) }
                        }
                    }
                }
            }
        }
        val native = compose.onNodeWithTag("native").captureToImage().toPixelMap()
        val wrapped = compose.onNodeWithTag("wrapped").captureToImage().toPixelMap()
        assertEquals(native.width, wrapped.width)
        assertEquals(native.height, wrapped.height)
        // A capsule clip wrongly removes this inner corner; compare to the original native control.
        for ((x, y) in listOf(1 to 1, (wrapped.width - 5) to 4, (wrapped.width / 2) to 4)) {
            assertEquals(native[x, y], wrapped[x, y])
        }
        assertTrue(wrapped[wrapped.width - 5, 4].green > 0.8f)
        compose.onNodeWithTag("wrapped").performClick()
        compose.runOnIdle {
            assertEquals(1, clicks)
            assertTrue(scopedRipple)
            assertTrue(globalRippleDisabled)
        }
    }
}
