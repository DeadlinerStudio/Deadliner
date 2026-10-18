package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.base.feedback.ProvideMiuixPressFeedback
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
class SheetActionFeedbackTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sheetActionsSinkRestoreAndKeepDisabledOrCancelledClicksInactive() {
        var clicks = 0
        compose.setContent {
            MiuixTheme { MaterialTheme { ProvideMiuixPressFeedback { Scaffold {
                MiuixBottomSheet(true, title = "Sheet", onDismissRequest = {},
                    startAction = { action("action", true) { clicks++ } },
                    endAction = { action("disabled", false) { clicks++ } },
                ) { Text("Body") }
            } } } }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        val content = compose.onNodeWithTag("action-icon", useUnmergedTree = true)
        val width = content.fetchSemanticsNode().boundsInRoot.width
        compose.onNodeWithTag("action").performTouchInput { down(center) }
        compose.waitUntil(timeoutMillis = 3000) {
            compose.mainClock.advanceTimeBy(32)
            content.fetchSemanticsNode().boundsInRoot.width < width * 0.95f
        }
        compose.mainClock.advanceTimeBy(600)
        assertEquals(width * 0.94f, content.fetchSemanticsNode().boundsInRoot.width, 0.5f)
        compose.onNodeWithTag("action").performTouchInput { cancel() }
        compose.mainClock.advanceTimeBy(700)
        assertEquals(width, content.fetchSemanticsNode().boundsInRoot.width, 0.5f)
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { assertEquals(0, clicks) }
        compose.onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithTag("action").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Composable private fun action(tag: String, enabled: Boolean, onClick: () -> Unit) {
        TopBarIconButton(onClick = onClick, modifier = Modifier.testTag(tag), enabled = enabled) {
            Box(Modifier.size(24.dp).testTag("$tag-icon"))
        }
    }
}
