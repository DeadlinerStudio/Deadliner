package com.aritxonly.deadliner.ui.main

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.InterfaceIconPack
import com.aritxonly.deadliner.ui.LocalInterfaceIconPack
import com.aritxonly.deadliner.ui.category.CategoryBadge
import com.aritxonly.deadliner.ui.category.CategoryPresentation
import com.aritxonly.deadliner.ui.iconResource
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
class TaskCardMetadataTest {
    @get:Rule val compose = createComposeRule()
    private val category = CategoryPresentation("study", "Study", "education", "#7C68DD", false, 0)
    @Before fun initialize() { GlobalUtils.init(RuntimeEnvironment.getApplication()) }

    @Test fun starPrecedesTitleAndOnlyThisHybridGlyphShrinksWithoutMovingText() {
        val pack = mutableStateOf(InterfaceIconPack.MaterialSymbols)
        compose.setContent { MaterialTheme { MiuixTheme {
            CompositionLocalProvider(LocalInterfaceIconPack provides pack.value) {
                Column {
                    DDLItemCardSimplified("Task title", "3 days", "Note", 0.5f, true, Modifier.width(320.dp))
                    Icon(iconResource(R.drawable.msr_star_fill_wght400), null, Modifier.size(24.dp).testTag("other-star"))
                }
            }
        } } }
        val star = compose.onNodeWithTag("task-card-star").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("Task title").fetchSemanticsNode().boundsInRoot
        val time = compose.onNodeWithText("3 days").fetchSemanticsNode().boundsInRoot
        assertTrue(star.right <= title.left)
        assertTrue(title.right <= time.left)
        assertEquals(24f, star.width, 0.01f)
        compose.runOnIdle { pack.value = InterfaceIconPack.HybridMiuix }
        val hybrid = compose.onNodeWithTag("task-card-star").fetchSemanticsNode().boundsInRoot
        assertEquals(21.6f, hybrid.width, 0.1f)
        assertEquals(title.left, compose.onNodeWithText("Task title").fetchSemanticsNode().boundsInRoot.left, 0.01f)
        assertEquals(24f, compose.onNodeWithTag("other-star").fetchSemanticsNode().boundsInRoot.width, 0.01f)
    }

    @Test fun categorySubtasksAndNoteShareOneLineAndCountsRefresh() {
        val completed = mutableIntStateOf(2)
        compose.setContent { MaterialTheme { MiuixTheme {
            DDLItemCardSimplified("Task", "3 days", "A note", 0.5f, false,
                Modifier.width(320.dp).testTag("card"),
                categoryContent = { CategoryBadge(category, Modifier.testTag("category"), compact = true) },
                completedSubtasks = completed.intValue, totalSubtasks = 5)
        } } }
        val categoryBounds = compose.onNodeWithTag("category", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val badge = compose.onNodeWithTag("task-card-subtasks").fetchSemanticsNode().boundsInRoot
        val note = compose.onNodeWithText("A note").fetchSemanticsNode().boundsInRoot
        assertTrue(categoryBounds.right < badge.left)
        assertTrue(badge.right < note.left)
        assertEquals(categoryBounds.center.y, badge.center.y, 0.1f)
        assertEquals(76f, compose.onNodeWithTag("card").fetchSemanticsNode().boundsInRoot.height, 0.01f)
        compose.onNodeWithText("2/5").assertIsDisplayed()
        compose.onNodeWithTag("task-card-star").assertDoesNotExist()
        compose.runOnIdle { completed.intValue = 5 }
        compose.onNodeWithText("5/5").assertIsDisplayed()
    }

    @Test fun emptyMetadataHidesTheBadgeAndLongTextKeepsRoomForSubtasks() {
        val total = mutableIntStateOf(0)
        val showLongMetadata = mutableStateOf(false)
        val note = "A very long note that must remain inside the task card and be ellipsized"
        compose.setContent { MaterialTheme { MiuixTheme {
            DDLItemCardSimplified("Very long task title that needs truncation", "3 days",
                if (showLongMetadata.value) note else "", 0f, false,
                Modifier.width(280.dp).testTag("card"),
                categoryContent = if (showLongMetadata.value) ({
                    CategoryBadge(category.copy(name = "A very long category name"), compact = true)
                }) else null,
                completedSubtasks = 0, totalSubtasks = total.intValue)
        } } }
        compose.onNodeWithTag("task-card-subtasks").assertDoesNotExist()
        compose.runOnIdle { total.intValue = 3 }
        compose.onNodeWithText("0/3").assertIsDisplayed()
        compose.runOnIdle { showLongMetadata.value = true }
        val badge = compose.onNodeWithTag("task-card-subtasks").fetchSemanticsNode().boundsInRoot
        val noteBounds = compose.onNodeWithText(note).fetchSemanticsNode().boundsInRoot
        val card = compose.onNodeWithTag("card").fetchSemanticsNode().boundsInRoot
        assertTrue(noteBounds.width > 0f)
        assertTrue(badge.right < noteBounds.left)
        assertTrue(noteBounds.right <= card.right - 15.5f)
        assertEquals(badge.center.y, noteBounds.center.y, 0.1f)
    }
}
