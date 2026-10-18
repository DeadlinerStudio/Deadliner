package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.base.feedback.ProvideMiuixPressFeedback
import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import com.aritxonly.deadliner.ui.base.feedback.miuixControlFeedback
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.yukonga.miuix.kmp.utils.MiuixIndication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
class PressFeedbackTest {
    @get:Rule val compose = createComposeRule()

    @Test fun themeUsesMiuixFeedbackAndDisablesMaterialRipple() {
        var checked = false
        compose.setContent {
            MaterialTheme {
                ProvideMiuixPressFeedback {
                    val indication = LocalIndication.current
                    val ripple = LocalRippleConfiguration.current
                    SideEffect {
                        assertTrue(indication is MiuixIndication)
                        assertNull(ripple)
                        checked = true
                    }
                }
            }
        }
        compose.runOnIdle { assertTrue(checked) }
    }

    @Test fun materialPressKeepsSingleClickDisabledAndCancellationBehavior() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                ProvideMiuixPressFeedback {
                    Column {
                        Button(onClick = { clicks++ }, modifier = Modifier
                            .testTag("button").miuixPressFeedback()) { Text("Add") }
                        Button(onClick = { clicks++ }, enabled = false, modifier = Modifier
                            .testTag("disabled").miuixPressFeedback(enabled = false)) { Text("Disabled") }
                    }
                }
            }
        }
        compose.onNodeWithTag("button").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks) }
        compose.onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithTag("button").performTouchInput { down(center); advanceEventTime(200); cancel() }
        compose.runOnIdle { assertEquals(1, clicks) }
        compose.onNodeWithTag("button").performTouchInput { click() }
        compose.runOnIdle { assertEquals(2, clicks) }
    }

    @Test fun foundationFeedbackPreservesLongClickWithoutAlsoClicking() {
        var clicks = 0
        var longClicks = 0
        compose.setContent {
            MaterialTheme {
                ProvideMiuixPressFeedback {
                    Box(Modifier.size(96.dp).testTag("target").combinedClickable(
                        onClick = { clicks++ }, onLongClick = { longClicks++ },
                    ))
                }
            }
        }
        compose.onNodeWithTag("target").performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(1, longClicks); assertEquals(0, clicks) }
        compose.onNodeWithTag("target").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun listPressKeepsGeometryWhileButtonUsesOneSinkAndRestoresAfterCancel() {
        compose.setContent {
            MaterialTheme {
                ProvideMiuixPressFeedback {
                    Column {
                        Box(Modifier.size(100.dp).testTag("row").clickable {}) {
                            Box(Modifier.size(80.dp).testTag("row-content"))
                        }
                        Box(Modifier.testTag("host").miuixControlFeedback()) {
                            CompositionLocalProvider(LocalTopBarButtonMaterialHosted provides true) {
                                Button(onClick = {}, modifier = Modifier.miuixPressFeedback()) {
                                    Box(Modifier.size(80.dp).testTag("button-content"))
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.mainClock.autoAdvance = false
        val row = compose.onNodeWithTag("row-content", useUnmergedTree = true)
        val button = compose.onNodeWithTag("button-content", useUnmergedTree = true)
        val rowWidth = row.fetchSemanticsNode().boundsInRoot.width
        val buttonWidth = button.fetchSemanticsNode().boundsInRoot.width
        compose.onNodeWithTag("row").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(600)
        assertEquals(rowWidth, row.fetchSemanticsNode().boundsInRoot.width, 0.1f)
        compose.onNodeWithTag("row").performTouchInput { cancel() }
        compose.onNodeWithTag("host").performTouchInput { down(center) }
        compose.waitUntil(timeoutMillis = 3000) {
            compose.mainClock.advanceTimeBy(32)
            button.fetchSemanticsNode().boundsInRoot.width < buttonWidth * 0.95f
        }
        compose.mainClock.advanceTimeBy(600)
        assertEquals(buttonWidth * 0.94f, button.fetchSemanticsNode().boundsInRoot.width, 0.5f)
        compose.onNodeWithTag("host").performTouchInput { cancel() }
        compose.mainClock.advanceTimeBy(700)
        assertEquals(buttonWidth, button.fetchSemanticsNode().boundsInRoot.width, 0.5f)
        compose.mainClock.autoAdvance = true
    }

}
