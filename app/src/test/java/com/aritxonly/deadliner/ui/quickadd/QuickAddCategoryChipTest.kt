package com.aritxonly.deadliner.ui.quickadd

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.aritxonly.deadliner.ui.category.CategoryPresentation
import com.aritxonly.deadliner.ui.category.CategoryPresentationRegistry
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
class QuickAddCategoryChipTest {
    @get:Rule val compose = createComposeRule()
    private val category = CategoryPresentation("study", "Study", "education", "#FF8800", false, 0)

    @Test fun selectedCategoryUsesItsGlyphColorAndTintedBackgroundAndKeepsBothActions() {
        var opened = 0
        var removed = 0
        val enabled = mutableStateOf(true)
        compose.setContent {
            MiuixTheme { MaterialTheme {
                Box(Modifier.background(Color.White)) {
                    QuickAddCategoryChip(category, enabled.value, { removed++ }, Modifier.testTag("chip")) { opened++ }
                }
            } }
        }
        compose.onNodeWithTag("quick-category-icon-study", useUnmergedTree = true).assertExists()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Study", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(CategoryPresentationRegistry.color(category.colorToken), layouts.single().layoutInput.style.color)
        val pixels = compose.onNodeWithTag("chip").captureToImage().toPixelMap()
        val tint = pixels[pixels.width - 20, 4]
        assertTrue("The capsule carries the category's orange tint", tint.red > tint.green && tint.green > tint.blue)
        compose.onNodeWithText("Study").performTouchInput { click() }
        compose.onNodeWithContentDescription("移除Study").performTouchInput { click() }
        assertEquals(1, opened)
        assertEquals(1, removed)
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithText("Study").performTouchInput { click() }
        compose.onNodeWithContentDescription("移除Study").performTouchInput { click() }
        assertEquals(1, opened)
        assertEquals(1, removed)
    }
}
