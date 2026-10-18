package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.feedback.ProvideMiuixPressFeedback
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarGlassShadowHost
import com.aritxonly.deadliner.ui.material.glass.TopBarGlassShadowState
import com.aritxonly.deadliner.ui.symbolPainterResource
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalMaterial3Api::class)
class TopBarShadowSlotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun actionShadowCrossesRealScaffoldBarBoundaryAndDisappearsWithItsAction() {
        val visible = mutableStateOf(true)
        val reveal = mutableFloatStateOf(1f)
        val background = Color(0xFFF3F3F3)
        var host: TopBarGlassShadowState? = null
        var clicks = 0
        compose.setContent {
            MiuixTheme {
                MaterialTheme(colorScheme = lightColorScheme(surface = background, onSurface = Color.Black)) {
                    ProvideMiuixPressFeedback {
                        CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = true)) {
                            Box(Modifier.height(180.dp).testTag("scene")) {
                                Scaffold(containerColor = background, contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
                                    Box(Modifier.testTag("bar")) {
                                        TopAppBar(title = "Screen", buttonMaterialRevealProgress = reveal.floatValue,
                                            navigationIcon = {
                                                TopBarIconButton(onClick = {}) {
                                                    Icon(symbolPainterResource(R.drawable.msr_arrow_back_wght400), "back")
                                                }
                                            }, actions = {
                                                val current = LocalTopBarGlassShadowHost.current
                                                SideEffect { host = current }
                                                if (visible.value) TopBarIconButton(onClick = { clicks++; visible.value = false }) {
                                                    Icon(symbolPainterResource(R.drawable.msr_close_wght400), "action")
                                                }
                                            })
                                    }
                                }) { Box(Modifier.fillMaxSize()) }
                            }
                        }
                    }
                }
            }
        }
        val bar = compose.onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithContentDescription("action").fetchSemanticsNode().boundsInRoot
        val capture = compose.onNodeWithTag("scene").captureToImage()
        val initial = capture.toPixelMap()
        val x = action.center.x.toInt()
        val y = bar.bottom.toInt()
        assertTrue("The test samples outside the original bar's clip", y >= bar.bottom)
        assertTrue("Action shadow should continue into the body area", background.red - initial[x, y].red > 0.003f)
        compose.runOnIdle { assertNotNull(host); assertEquals(2, host!!.shadows.size) }
        compose.runOnIdle { reveal.floatValue = 0f }
        val hidden = compose.onNodeWithTag("scene").captureToImage().toPixelMap()
        assertEquals("Unrevealed material must not retain elevation", background.red, hidden[x, y].red, 0.003f)
        compose.runOnIdle { reveal.floatValue = 1f }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("action").performClick()
        compose.onNodeWithContentDescription("action").assertDoesNotExist()
        val removed = compose.onNodeWithTag("scene").captureToImage().toPixelMap()
        compose.runOnIdle { assertEquals(1, clicks); assertEquals(1, host!!.shadows.size) }
        assertEquals("Disposed action must not leave a shadow", background.red, removed[x, y].red, 0.003f)
        assertEquals("Do not expand the bar's occupied height", bar.height,
            compose.onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.height, 0.01f)
    }
}
