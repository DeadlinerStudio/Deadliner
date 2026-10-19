package com.aritxonly.deadliner.ui.category

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
class CategoryDropdownTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val select get() = context.getString(R.string.category_select)
    private val uncategorized get() = context.getString(R.string.category_uncategorized)
    private val manage get() = context.getString(R.string.category_manage)
    private val categories = listOf(
        CategoryPresentation("work", "Work", "briefcase", "#0088FF", false, 0),
        CategoryPresentation("study", "Study", "book", "#FF8800", false, 1),
    )

    @Test fun selectedValueAndMenuRetainCategoryGlyphsAndColors() {
        compose.setContent {
            MiuixTheme { MaterialTheme {
                CategoryDropdownField("study", categories, {}, {}, Modifier.testTag("editor-dropdown"))
            } }
        }
        val field = compose.onNodeWithTag("editor-dropdown").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText(select, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(16f, (title.left - field.left).value, .5f)
        compose.onNodeWithTag("category-icon-study", useUnmergedTree = true).assertExists()
        assertLabelColor("Study", CategoryPresentationRegistry.color("#FF8800"))
        compose.onNodeWithText(select).performClick()
        compose.onNode(hasTestTag("category-icon-work") and hasAnyAncestor(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,
                androidx.compose.ui.semantics.Role.RadioButton)), useUnmergedTree = true).assertExists()
        compose.onNode(hasTestTag("category-icon-study") and hasAnyAncestor(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,
                androidx.compose.ui.semantics.Role.RadioButton)), useUnmergedTree = true).assertExists()
        compose.onNode(hasTestTag("category-icon-uncategorized") and hasAnyAncestor(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,
                androidx.compose.ui.semantics.Role.RadioButton)), useUnmergedTree = true).assertExists()
        compose.onNode(hasTestTag("category-icon-manage") and hasAnyAncestor(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,
                androidx.compose.ui.semantics.Role.RadioButton)), useUnmergedTree = true).assertExists()
        assertLabelColor("Work", CategoryPresentationRegistry.color("#0088FF"), menu = true)
        assertLabelColor("Study", CategoryPresentationRegistry.color("#FF8800"), menu = true)
        compose.onNodeWithText("Study").assertIsSelected()
    }

    private fun assertLabelColor(label: String, expected: androidx.compose.ui.graphics.Color, menu: Boolean = false) {
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        val matcher = hasText(label) and if (menu) hasAnyAncestor(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,
                androidx.compose.ui.semantics.Role.RadioButton)) else SemanticsMatcher("any") { true }
        compose.onNode(matcher, useUnmergedTree = true).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(expected, layouts.single().layoutInput.style.color)
    }

    @Test fun editorDropdownSelectsByUidAndCanClearCategory() {
        val selected = mutableStateOf<String?>("work")
        compose.setContent {
            MiuixTheme { MaterialTheme {
                CategoryDropdownField(selected.value, categories, { selected.value = it }, {})
            } }
        }
        compose.onNodeWithText(select).performClick()
        compose.onNodeWithText("Work").assertIsSelected()
        compose.onNodeWithText("Study").performTouchInput { click() }
        compose.waitForIdle()
        assertEquals("study", selected.value)
        compose.onNodeWithText(select).performClick()
        compose.onNodeWithText("Study").assertIsSelected()
        compose.onNodeWithText(uncategorized).performTouchInput { click() }
        compose.waitForIdle()
        assertNull(selected.value)
        compose.onNodeWithText(uncategorized).assertIsDisplayed()
    }

    @Test fun emptyListStillSupportsManageAndDismissalDoesNotChangeSelection() {
        var selected: String? = "missing"
        var managed = 0
        var wasShowingWhenManaged = true
        compose.setContent {
            MiuixTheme { MaterialTheme {
                CategoryDropdownField(selected, emptyList(), { selected = it }, {
                    managed++
                    wasShowingWhenManaged = ShadowDialog.getLatestDialog().isShowing
                })
            } }
        }
        compose.onNodeWithText(select).performClick()
        compose.onNodeWithText(uncategorized).assertIsSelected()
        compose.runOnIdle { ShadowDialog.getLatestDialog().onBackPressed() }
        compose.waitForIdle()
        assertEquals("missing", selected)
        compose.onNodeWithText(select).performClick()
        compose.onNodeWithText(manage).performTouchInput { click() }
        compose.waitForIdle()
        assertEquals(1, managed)
        assertFalse("Management opens after the popup window exits", wasShowingWhenManaged)
        assertEquals("missing", selected)
    }

    @Test fun compactAnchorKeepsPopupAliveWhenClearingRemovesItsChip() {
        val selected = mutableStateOf<String?>("study")
        compose.setContent {
            MiuixTheme { MaterialTheme {
                CategoryDropdownMenu(categories, selected.value, { selected.value = it }, {}) { anchor, open, _ ->
                    if (selected.value != null) Text("chip", Modifier.padding(20.dp).then(anchor)
                        .clickable(onClick = open).testTag("chip"))
                }
            } }
        }
        compose.onNodeWithTag("chip").performClick()
        compose.onNodeWithText(uncategorized).performTouchInput { click() }
        compose.waitForIdle()
        assertNull(selected.value)
        compose.onNodeWithTag("chip").assertDoesNotExist()
        compose.runOnIdle { assertFalse(ShadowDialog.getLatestDialog().isShowing) }
    }

    @Test fun duplicateNamesAndUpdatedOrderingStillSelectByUid() {
        val selected = mutableStateOf<String?>("work")
        val options = mutableStateOf(categories.map { it.copy(name = "Same") })
        compose.setContent {
            MiuixTheme { MaterialTheme {
                CategoryDropdownField(selected.value, options.value, { selected.value = it }, {})
            } }
        }
        compose.onNodeWithText(select).performClick()
        compose.onAllNodesWithText("Same")[0].assertIsSelected()
        compose.onAllNodesWithText("Same")[1].performTouchInput { click() }
        compose.waitForIdle()
        assertEquals("study", selected.value)
        compose.runOnIdle { options.value = options.value.reversed() }
        compose.onNodeWithText(select).performClick()
        compose.onAllNodesWithText("Same")[0].assertIsSelected()
        compose.onAllNodesWithText("Same")[1].performTouchInput { click() }
        compose.waitForIdle()
        assertEquals("work", selected.value)
    }

    @Test fun disabledCompactAnchorDoesNotOpenDropdown() {
        compose.setContent {
            MiuixTheme { MaterialTheme {
                CategoryDropdownMenu(categories, null, {}, {}, enabled = false) { anchor, open, _ ->
                    Box(Modifier.padding(20.dp).then(anchor).clickable(onClick = open).testTag("disabled")) { Text("category") }
                }
            } }
        }
        compose.onNodeWithTag("disabled").performClick()
        compose.onNodeWithText("Work").assertDoesNotExist()
    }
}
