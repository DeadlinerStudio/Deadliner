package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
class SheetLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fullSheetMeasuresTheAvailableHeightIncludingTheHandle() {
        var expectedHeight = 0f
        var expectedTop = 0f
        compose.setContent {
            val systemTop = maxOf(WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                WindowInsets.displayCutout.asPaddingValues().calculateTopPadding(),
                WindowInsets.captionBar.asPaddingValues().calculateTopPadding())
            expectedTop = (systemTop + 8.dp).value
            expectedHeight = (SheetHeight.FULL.bodyHeight(LocalWindowInfo.current.containerDpSize.height,
                systemTop) + 8.dp).value
            MiuixTheme { Scaffold {
                MiuixBottomSheet(true, title = "Full sheet", sheetHeight = SheetHeight.FULL,
                    modifier = Modifier.testTag("sheet"), onDismissRequest = {}) { Text("Body") }
            } }
        }
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("sheet", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        assertTrue(bounds.height > 600f)
        assertEquals(expectedHeight, bounds.height, 1f)
        assertEquals(expectedTop, bounds.top, 1f)
        compose.onNodeWithText("Body").assertIsDisplayed()
    }

    @Test fun defaultMediumSheetRetainsIts600DpBody() {
        compose.setContent { MiuixTheme { Scaffold {
            MiuixBottomSheet(true, title = "Medium sheet", modifier = Modifier.testTag("sheet"),
                onDismissRequest = {}) { Text("Medium body") }
        } } }
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("sheet", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        assertEquals(608f, bounds.height, 1f)
        compose.onNodeWithText("Medium body").assertIsDisplayed()
    }

    @Test fun sourceExpansionCompletesAndTheSheetStillDismissesNormally() {
        val visible = mutableStateOf(true)
        compose.setContent { MiuixTheme { Scaffold {
            MiuixBottomSheet(visible.value, title = "Expanded sheet", sheetHeight = SheetHeight.FULL,
                enterFrom = Rect(80f, 350f, 390f, 650f), modifier = Modifier.testTag("sheet"),
                onDismissRequest = { visible.value = false }) { Text("Expanded body") }
        } } }
        compose.waitForIdle()
        compose.onNodeWithText("Expanded body").assertIsDisplayed()
        val bounds = compose.onNodeWithTag("sheet", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        assertTrue(bounds.height > 600f)
        compose.runOnIdle {
            visible.value = false
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        }
        compose.waitForIdle()
        compose.onNodeWithText("Expanded body").assertDoesNotExist()
    }
}
