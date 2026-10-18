package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.base.feedback.ProvideMiuixPressFeedback
import com.aritxonly.deadliner.ui.main.modern.components.ModernListTopAppBar
import com.aritxonly.deadliner.ui.material.glass.*
import com.aritxonly.deadliner.ui.symbolPainterResource
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBarState
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class CustomTopBarShadowTest {
    @get:Rule val compose = createComposeRule()
    private val background = Color(0xFFF3F3F3)

    @Test fun listNativeBarSharesForegroundHostOutsideItsClipAndTransition() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).get()
        var state: TopAppBarState? = null
        var host: TopBarGlassShadowState? = null
        scene {
            host = LocalTopBarGlassShadowHost.current
            AnimatedContent(targetState = true, label = "list-header") {
                if (it) {
                    val barState = rememberTopAppBarState()
                    SideEffect { state = barState }
                    ModernListTopAppBar(activity = activity, selectedPage = DeadlineType.TASK,
                        onSelectedPageChange = {}, avatarPainter = null,
                        scrollBehavior = MiuixScrollBehavior(barState),
                        showPageTabs = false, transparentBackground = true)
                }
            }
        }
        compose.runOnIdle { state!!.heightOffset = state!!.heightOffsetLimit }
        compose.waitForIdle()
        val bar = compose.onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            assertNotNull(host)
            assertEquals("Both raw MIUIX bar controls must register", 2, host!!.shadows.size)
        }
        val bounds = host!!.shadows.values.maxBy { it.bounds.left }.bounds
        val pixels = compose.onNodeWithTag("scene").captureToImage().toPixelMap()
        // At this opacity an 8-bit screenshot dithers the far tail across adjacent pixels.
        val outsideShadow = (-12..12).map { dx ->
            background.red - pixels[bounds.center.x.toInt() + dx, bar.bottom.toInt()].red
        }.average()
        assertTrue("List shadow must cross native bar's bottom clip", outsideShadow > 0.0005f)
    }

    @Test fun lifiRowActionsHaveOutsideShadowsAndUnregisterWithoutChangingHeight() {
        val visible = mutableStateOf(true)
        var host: TopBarGlassShadowState? = null
        scene {
            host = LocalTopBarGlassShadowHost.current
            // Same foreground geometry as LifiAIPage, which does not use the common TopAppBar.
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                action("history") {}
                Spacer(Modifier.width(8.dp))
                if (visible.value) action("new") { visible.value = false }
                else Spacer(Modifier.size(44.dp))
            }
        }
        val bar = compose.onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithContentDescription("new").fetchSemanticsNode().boundsInRoot
        val x = action.center.x.toInt()
        // The new low-density shadow naturally fades before the Row's 10dp bottom padding ends.
        // Sample outside the lens/press clip; host registration below checks the outer ownership.
        val y = action.bottom.toInt() + 3
        val before = compose.onNodeWithTag("scene").captureToImage().toPixelMap()
        assertTrue("Lifi shadow should remain visible outside the action clip", background.red - before[x, y].red > 0.003f)
        compose.runOnIdle { assertNotNull(host); assertEquals(2, host!!.shadows.size) }
        compose.onNodeWithContentDescription("new").performClick()
        val after = compose.onNodeWithTag("scene").captureToImage().toPixelMap()
        compose.runOnIdle { assertEquals(1, host!!.shadows.size) }
        assertTrue("Removed action must release its shadow", after[x, y].red > before[x, y].red + 0.003f)
        assertEquals(bar.height, compose.onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.height, 0.01f)
    }

    @Test fun nestedCommonBarReusesOutermostHost() {
        var outer: TopBarGlassShadowState? = null
        var inner: TopBarGlassShadowState? = null
        scene {
            outer = LocalTopBarGlassShadowHost.current
            TopAppBar(title = "Settings", buttonMaterialRevealProgress = 1f, actions = {
                inner = LocalTopBarGlassShadowHost.current
                action("settings") {}
            })
        }
        compose.runOnIdle {
            assertNotNull(outer)
            assertSame("Common bar must not recapture elevation inside its parent", outer, inner)
            assertEquals(1, outer!!.shadows.size)
        }
    }

    @Composable private fun action(description: String, onClick: () -> Unit) {
        TopBarIconButton(onClick = onClick) {
            Icon(symbolPainterResource(R.drawable.msr_close_wght400), description)
        }
    }

    private fun scene(content: @Composable BoxScope.() -> Unit) {
        compose.setContent {
            MiuixTheme {
                MaterialTheme(colorScheme = lightColorScheme(surface = background, onSurface = Color.Black)) {
                    ProvideMiuixPressFeedback {
                        CompositionLocalProvider(
                            LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = true),
                            LocalTopBarScrollProgress provides 1f,
                            LocalTopBarButtonMaterialProgress provides 1f,
                        ) {
                            Box(Modifier.height(220.dp).testTag("scene").fillMaxWidth()) {
                                androidx.compose.foundation.Canvas(Modifier.matchParentSize()) { drawRect(background) }
                                ProgressiveTopBarMaterial(Modifier.testTag("bar"), content)
                            }
                        }
                    }
                }
            }
        }
    }
}
