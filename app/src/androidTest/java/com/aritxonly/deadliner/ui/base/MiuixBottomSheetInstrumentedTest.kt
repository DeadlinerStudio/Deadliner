package com.aritxonly.deadliner.ui.base

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aritxonly.deadliner.OverviewActivity
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.material.glass.LocalTopBarButtonMaterialProgress
import com.aritxonly.deadliner.ui.material.glass.LocalProgressiveTopBarMaterialHosted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import java.io.File
import androidx.compose.material3.LocalContentColor as MaterialContentColor
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixContentColor

/** Component fixtures only: no business-data changes. */
@RunWith(AndroidJUnit4::class)
class MiuixBottomSheetInstrumentedTest {
    @get:Rule val ui = createAndroidComposeRule<OverviewActivity>()

    @Test fun lightGlass() = verify(dark = false, glass = true)
    @Test fun darkGlass() = verify(dark = true, glass = true)
    @Test fun lightFallback() = verify(dark = false, glass = false)
    @Test fun darkFallback() = verify(dark = true, glass = false)

    private fun verify(dark: Boolean, glass: Boolean) {
        val colors = if (dark) darkColorScheme() else lightColorScheme()
        var materialContent = Color.Unspecified
        var miuixContent = Color.Unspecified
        var actionProgress = Float.NaN
        var actionGlassHosted = true
        ui.runOnUiThread {
            ui.activity.setContent {
                MiuixTheme(colors = colors) {
                    MaterialTheme {
                        val pageBackdrop = rememberLayerBackdrop { drawRect(colors.surface); drawContent() }
                        CompositionLocalProvider(
                            LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = glass),
                            LocalAdvancedMaterialBackdrop provides pageBackdrop,
                        ) {
                            Scaffold(containerColor = colors.surface, contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
                                Box(Modifier.fillMaxSize().layerBackdrop(pageBackdrop).background(colors.surface)) {
                                    Text("Page behind the glass", Modifier.padding(24.dp), color = colors.onSurface)
                                }
                                MiuixBottomSheet(show = true, title = "Sheet title", onDismissRequest = {},
                                    modifier = Modifier.testTag("sheet-surface"),
                                    headerContent = {
                                        Box(Modifier.fillMaxWidth().height(40.dp).testTag("sheet-tabs")) { Text("Pinned tabs") }
                                    },
                                    sampleContentForBottomBar = true,
                                    bottomBar = { com.aritxonly.deadliner.ui.editor.DeadlineBottomActions(false, {}, {}) },
                                    endAction = {
                                    val progress = LocalTopBarButtonMaterialProgress.current
                                    val hosted = LocalProgressiveTopBarMaterialHosted.current
                                    SideEffect { actionProgress = progress; actionGlassHosted = hosted }
                                    TopBarIconButton(onClick = {}) { Text("×") }
                                }) { padding ->
                                    val listState = rememberLazyListState()
                                    val material = MaterialContentColor.current
                                    val miuix = MiuixContentColor.current
                                    SideEffect { materialContent = material; miuixContent = miuix }
                                    LazyColumn(Modifier.fillMaxSize().syncTopBarMaterialScroll(listState).testTag("sheet-list"), state = listState, contentPadding = PaddingValues(
                                        top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding(),
                                    )) {
                                        items(30) { index ->
                                            Text("Row $index", Modifier.fillMaxWidth().height(64.dp).padding(20.dp).testTag("row-$index"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ui.waitUntil(8000) { ui.onAllNodesWithText("Sheet title").fetchSemanticsNodes().isNotEmpty() }
        ui.waitForIdle()
        ui.runOnIdle {
            assertEquals(colors.onSurface, materialContent)
            assertEquals(colors.onSurface, miuixContent)
            assertEquals(0f, actionProgress, 0.0001f)
            assertFalse(actionGlassHosted)
        }
        val header = ui.onNodeWithText("Sheet title").fetchSemanticsNode().boundsInRoot
        val sheet = ui.onNodeWithTag("sheet-surface").fetchSemanticsNode().boundsInRoot
        val tabs = ui.onNodeWithTag("sheet-tabs").fetchSemanticsNode().boundsInRoot
        assertEquals(with(ui.density) { 36.dp.toPx() }, header.center.y - sheet.top, 1f)
        assertTrue(ui.onNodeWithTag("row-0").fetchSemanticsNode().boundsInRoot.top >= header.bottom)
        capture("${if (dark) "dark" else "light"}-${if (glass) "glass" else "fallback"}")
        ui.onNodeWithTag("sheet-list").performScrollToIndex(10)
        ui.onNodeWithText("Sheet title").assertIsDisplayed()
        ui.runOnIdle { assertEquals(0f, actionProgress, 0.0001f); assertFalse(actionGlassHosted) }
        assertEquals(header, ui.onNodeWithText("Sheet title").fetchSemanticsNode().boundsInRoot)
        assertEquals(tabs, ui.onNodeWithTag("sheet-tabs").fetchSemanticsNode().boundsInRoot)
        capture("${if (dark) "dark" else "light"}-${if (glass) "glass" else "fallback"}-scrolled")
    }

    private fun capture(name: String) {
        ui.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val directory = File(ui.activity.getExternalFilesDir(null), "sheet-acceptance").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
