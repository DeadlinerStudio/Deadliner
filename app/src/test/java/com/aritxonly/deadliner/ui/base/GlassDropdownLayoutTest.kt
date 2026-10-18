package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
class GlassDropdownLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun openingMeasuresTheWholeListAndDisplaysEveryOption() {
        assertOpenedOptions(glass = false)
    }

    @Test fun glassPathMeasuresTheWholeListAndExposesEveryOption() {
        assertOpenedOptions(glass = true)
    }

    @Test fun selectingAnOptionClosesThePopupAndRestoresTheSourceValue() {
        val selected = mutableIntStateOf(1)
        showPreference(selected)
        compose.onNodeWithText("Menu").performClick()
        compose.waitForIdle()
        // Only the menu item is accessible; both decorative source copies stay hidden.
        compose.onAllNodesWithText("Medium").assertCountEquals(1)
        compose.onNodeWithText("Small").performTouchInput { click() }
        compose.waitForIdle()
        compose.onNodeWithText("Large").assertDoesNotExist()
        compose.onNodeWithText("Small").assertIsDisplayed()
        org.junit.Assert.assertEquals(0, selected.intValue)
    }

    @Test fun draggingAcrossOptionsDoesNotSelectAndATapStillCloses() {
        val selected = mutableIntStateOf(1)
        showPreference(selected)
        compose.onNodeWithText("Menu").performClick()
        compose.waitForIdle()
        val panel = compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Menu"))
        val panelBounds = panel.fetchSemanticsNode().boundsInRoot
        val from = compose.onNodeWithText("Small").fetchSemanticsNode().boundsInRoot.center - panelBounds.topLeft
        val to = compose.onNodeWithText("Large").fetchSemanticsNode().boundsInRoot.center - panelBounds.topLeft
        panel.performTouchInput {
            down(from)
            moveTo(from + (to - from) * 0.5f, delayMillis = 100)
            moveTo(to, delayMillis = 100)
            up()
        }
        compose.waitForIdle()
        org.junit.Assert.assertEquals(1, selected.intValue)
        compose.onNodeWithText("Small").assertIsDisplayed()
        compose.onNodeWithText("Medium").assertIsSelected()
        compose.onNodeWithText("Large").assertIsNotSelected()
        compose.onNodeWithText("Small").performTouchInput { click() }
        compose.waitForIdle()
        compose.onNodeWithText("Large").assertDoesNotExist()
        compose.onNodeWithText("Small").assertIsDisplayed()
    }

    @Test fun platformWindowCannotPlayASecondDismissAnimation() {
        showPreference(mutableIntStateOf(1))
        compose.onNodeWithText("Menu").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            val attributes = org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.attributes
            org.junit.Assert.assertEquals(0, attributes.windowAnimations)
            org.junit.Assert.assertEquals(0f, attributes.dimAmount, 0.001f)
            org.junit.Assert.assertEquals(0,
                attributes.flags and android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    @Test fun optionTextUsesTheCompact14SpSize() {
        showPreference(mutableIntStateOf(1))
        compose.onNodeWithText("Menu").performClick()
        compose.waitForIdle()
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Small", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        org.junit.Assert.assertEquals(14.sp, results.single().layoutInput.style.fontSize)
        org.junit.Assert.assertEquals(20.sp, results.single().layoutInput.style.lineHeight)
    }

    @Test fun compactRowsFillThePanelAndAlignTheIndicatorAtTheTrailingInset() {
        showPreference(mutableIntStateOf(1))
        compose.onNodeWithText("Menu").performClick()
        compose.waitForIdle()
        val panel = compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Menu"))
            .fetchSemanticsNode().boundsInWindow
        val row = compose.onNodeWithText("Medium").fetchSemanticsNode().boundsInWindow
        val text = compose.onNode(hasText("Medium") and hasAnyAncestor(
            SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Menu")), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInWindow
        val indicator = compose.onNodeWithTag("dropdown-selected-indicator", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInWindow
        org.junit.Assert.assertEquals(192f, panel.width, 0.01f)
        // Preserve taller font metrics instead of clipping accessibility fonts to 48dp.
        org.junit.Assert.assertEquals(maxOf(48f, text.height + 20f, indicator.height + 20f),
            row.height, 0.01f)
        org.junit.Assert.assertEquals(row.height * 3f + 16f, panel.height, 0.01f)
        org.junit.Assert.assertEquals(panel.left + 20f, text.left, 0.01f)
        org.junit.Assert.assertEquals(panel.right - 20f, indicator.right, 0.01f)
    }

    @Test fun originalTextKeepsItsNodeAndPositionAcrossBothAnimationEndpoints() {
        showPreference(mutableIntStateOf(1))
        val original = compose.onNodeWithTag("dropdown-source-value", useUnmergedTree = true)
            .fetchSemanticsNode()
        val originalBounds = original.boundsInWindow
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Menu").performClick()
        fun assertOriginalFrames(count: Int) {
            repeat(count) {
                compose.mainClock.advanceTimeByFrame()
                val current = compose.onNodeWithTag("dropdown-source-value", useUnmergedTree = true)
                    .fetchSemanticsNode()
                org.junit.Assert.assertEquals(original.id, current.id)
                org.junit.Assert.assertEquals(originalBounds, current.boundsInWindow)
                compose.onAllNodesWithTag("dropdown-source-copy", useUnmergedTree = true)
                    .assertCountEquals(0)
            }
        }
        assertOriginalFrames(30)
        compose.onNodeWithText("Medium").performClick()
        assertOriginalFrames(24)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText("Large").assertDoesNotExist()
        compose.onNodeWithText("Medium").assertIsDisplayed()
    }

    private fun showPreference(selected: androidx.compose.runtime.MutableIntState) {
        compose.setContent {
            MiuixTheme {
                Column(Modifier.padding(top = 160.dp)) {
                    SoftGlassDropdownPreference("Menu", listOf("Small", "Medium", "Large"),
                        selected.intValue, { selected.intValue = it })
                }
            }
        }
    }

    private fun assertOpenedOptions(glass: Boolean) {
        compose.setContent {
            MiuixTheme {
                CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = glass)) {
                    WindowGlassBackdropHost {
                        SoftGlassDropdownPopup(true, "Menu", Rect(220f, 600f, 360f, 624f),
                            listOf("Small", "Medium", "Large"), 1, {}, {}, {})
                    }
                }
            }
        }
        compose.waitForIdle()
        val panel = compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Menu"))
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Panel must contain three complete rows: $panel", panel.height >= 156f)
        for (label in listOf("Small", "Medium", "Large")) {
            compose.onNodeWithText(label).assertIsDisplayed()
        }
    }
}
