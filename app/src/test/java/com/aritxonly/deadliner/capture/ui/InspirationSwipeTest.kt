package com.aritxonly.deadliner.capture.ui

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.deadliner.domain.capture.CaptureItem
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-w411dp-h891dp-mdpi")
class InspirationSwipeTest {
    @get:Rule val compose = createComposeRule()
    private var tasks = 0
    private var habits = 0
    private var deletes = 0
    private var opens = 0
    private val selection = mutableStateOf(false)
    @Before fun initializeSettings() {
        com.aritxonly.deadliner.localutils.GlobalUtils.init(RuntimeEnvironment.getApplication())
    }
    private fun label(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun showCard() {
        compose.setContent {
            var active by remember { mutableStateOf<String?>(null) }
            MaterialTheme {
                InspirationLightCard(
                    CaptureItem("inspiration", "An idea", createdAt = "2026-10-17T00:00:00Z", updatedAt = "2026-10-17T00:00:00Z"),
                    selected = false, activeSwipeKey = active, onActiveSwipeChange = { active = it },
                    inMultiSelectMode = selection.value,
                    modifier = Modifier.width(360.dp).testTag("inspiration"),
                    onClick = { opens++ }, onDelete = { deletes++ }, onAiTask = { tasks++ }, onAiHabit = { habits++ },
                )
            }
        }
    }
    private fun swipeRight() {
        compose.onNodeWithTag("inspiration").performTouchInput {
            swipe(Offset(width * .1f, centerY), Offset(width * .95f, centerY), 400)
        }
        compose.waitForIdle()
    }
    @Test fun longRightSwipeOnlyRevealsAiActionsAndClickingRunsTheChosenAction() {
        showCard()
        swipeRight()
        assertEquals(0, tasks)
        assertEquals(0, habits)
        compose.onNodeWithContentDescription(label(R.string.capture_swipe_ai_task)).assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.capture_swipe_ai_habit)).performClick()
        compose.runOnIdle { assertEquals(1, habits); assertEquals(0, tasks); assertEquals(0, deletes) }
        compose.waitForIdle()
        swipeRight()
        compose.onNodeWithContentDescription(label(R.string.capture_swipe_ai_task)).performClick()
        compose.runOnIdle { assertEquals(1, tasks) }
    }
    @Test fun leftSwipeOffersOnlyDeleteAndSelectionDisablesSwiping() {
        showCard()
        compose.onNodeWithTag("inspiration").performTouchInput {
            swipe(Offset(width * .8f, centerY), Offset(width * .5f, centerY), 400)
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(label(R.string.edit)).assertDoesNotExist()
        compose.onNodeWithContentDescription(label(R.string.delete)).performClick()
        compose.runOnIdle { assertEquals(1, deletes); selection.value = true }
        compose.waitForIdle()
        swipeRight()
        compose.onNodeWithContentDescription(label(R.string.capture_swipe_ai_task)).assertDoesNotExist()
        val beforeTap = opens
        compose.onNodeWithText("An idea").performClick()
        compose.runOnIdle { assertEquals(beforeTap + 1, opens); assertEquals(1, deletes); assertEquals(0, tasks); assertEquals(0, habits) }
    }
}
