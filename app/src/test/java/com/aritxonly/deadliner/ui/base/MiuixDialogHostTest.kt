package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
class MiuixDialogHostTest {
    @get:Rule val compose = createComposeRule()
    private var host: AdvancedMaterialDialogBlurHostState? = null
    private var backdrop: WindowGlassBackdrop? = null

    @Test fun conditionallyRemovedCallerStaysVisibleUntilTheMiuixExitFinishes() {
        val visible = mutableStateOf(true)
        setDialog(visible, conditional = true, glass = false)
        compose.onNodeWithText("Dialog title").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, backdrop!!.consumers) }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            visible.value = false
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        }
        // Allow the caller recomposition/disposal and host recomposition to commit.
        compose.mainClock.advanceTimeBy(48)
        compose.onNodeWithText("Dialog title").assertExists()
        compose.runOnIdle { assertEquals(1, host!!.dialogs.size); assertFalse(host!!.dialogs.values.single().show) }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Dialog title").assertExists()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Dialog title").assertDoesNotExist()
        compose.runOnIdle { assertTrue(host!!.dialogs.isEmpty()) }
    }

    @Test fun stateBasedCloseAlsoReleasesTheSharedGlassRecorderAfterExit() {
        val visible = mutableStateOf(true)
        setDialog(visible, conditional = false, glass = true)
        compose.onNodeWithText("Dialog title").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, backdrop!!.consumers) }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            visible.value = false
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle { assertEquals(1, backdrop!!.consumers) }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Dialog title").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, backdrop!!.consumers) }
    }

    @Test fun confirmClosesAConditionalDialogWithoutInvokingItsActionTwice() {
        val visible = mutableStateOf(true)
        var confirms = 0
        setDialog(visible, conditional = true, glass = false) { confirms++ }
        compose.onNodeWithText("Confirm").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Dialog title").assertDoesNotExist()
        assertEquals(1, confirms)
    }

    @Test fun retainedContentKeepsTheCallingCompositionLocals() {
        val label = staticCompositionLocalOf { "Wrong root context" }
        compose.setContent {
            MiuixTheme {
                Scaffold {
                    ProvideAdvancedMaterialDialogBlurHost {
                        CompositionLocalProvider(label provides "Scoped dialog content") {
                            AlertDialog(true, {}, confirmButton = { TextButton(onClick = {}) { Text("Action") } },
                                miuixTitle = "Scoped title", text = { Text(label.current) })
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Scoped dialog content").assertIsDisplayed()
        compose.onNodeWithText("Wrong root context").assertDoesNotExist()
    }

    @Test fun dialogFromSheetUsesWholeWindowBackdropAndItsOwnAndroidWindow() {
        val showDialog = mutableStateOf(false)
        var windowBackdrop: WindowGlassBackdrop? = null
        var sheetBackdrop: WindowGlassBackdrop? = null
        var dialogBackdrop: WindowGlassBackdrop? = null
        var pageView: android.view.View? = null
        var sheetView: android.view.View? = null
        var dialogView: android.view.View? = null
        compose.setContent {
            MiuixTheme {
                CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = true)) {
                    ProvideAdvancedMaterialDialogBlurHost {
                        val source = LocalWindowGlassBackdrop.current
                        val page = androidx.compose.ui.platform.LocalView.current
                        SideEffect { windowBackdrop = source; pageView = page }
                        Scaffold {
                            androidx.compose.foundation.layout.Box {
                                MiuixBottomSheet(show = true, title = "Underlying sheet", onDismissRequest = {}) { padding ->
                                    val sheetSource = LocalWindowGlassBackdrop.current
                                    val sheet = androidx.compose.ui.platform.LocalView.current
                                    SideEffect { sheetBackdrop = sheetSource; sheetView = sheet }
                                    TextButton(modifier = Modifier.padding(padding), onClick = { showDialog.value = true }) { Text("Open from sheet") }
                                    if (showDialog.value) AlertDialog(true, { showDialog.value = false },
                                        miuixTitle = "Above the sheet", confirmButton = {
                                            TextButton(onClick = { showDialog.value = false }) { Text("Close dialog") }
                                        }, text = {
                                            val dialogSource = LocalWindowGlassBackdrop.current
                                            val view = androidx.compose.ui.platform.LocalView.current
                                            SideEffect { dialogBackdrop = dialogSource; dialogView = view }
                                            Text("Dialog body")
                                        })
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Open from sheet").performClick()
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 3000) { compose.onNodeWithText("Above the sheet").isDisplayed() }
        compose.onNodeWithText("Above the sheet").assertIsDisplayed()
        compose.onNode(isDialog()).assertExists()
        compose.runOnIdle {
            assertNotNull(windowBackdrop)
            assertSame(windowBackdrop, sheetBackdrop)
            assertSame(windowBackdrop, dialogBackdrop)
            assertSame(pageView!!.rootView, sheetView!!.rootView)
            assertNotSame(pageView!!.rootView, dialogView!!.rootView)
            assertEquals(1, windowBackdrop!!.consumers)
        }
        compose.onNodeWithText("Close dialog").performClick()
        compose.onNodeWithText("Above the sheet").assertDoesNotExist()
        compose.onNodeWithText("Open from sheet").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, windowBackdrop!!.consumers) }
    }

    @Test fun darkDialogDefaultsUseDialogColorsInsteadOfCapturedBlackText() = verifyDialogColors(true)
    @Test fun lightDialogDefaultsUseDialogColorsAndKeepExplicitColors() = verifyDialogColors(false)

    private fun verifyDialogColors(dark: Boolean) {
        var materialColor = androidx.compose.ui.graphics.Color.Unspecified
        var miuixColor = androidx.compose.ui.graphics.Color.Unspecified
        var explicitColor = androidx.compose.ui.graphics.Color.Unspecified
        var actionColor = androidx.compose.ui.graphics.Color.Unspecified
        var expected = androidx.compose.ui.graphics.Color.Unspecified
        var expectedAction = androidx.compose.ui.graphics.Color.Unspecified
        compose.setContent {
            val colors = if (dark) top.yukonga.miuix.kmp.theme.darkColorScheme()
                else top.yukonga.miuix.kmp.theme.lightColorScheme()
            MiuixTheme(colors = colors) {
                Scaffold {
                    ProvideAdvancedMaterialDialogBlurHost {
                        expected = MiuixTheme.colorScheme.onSurface
                        expectedAction = MiuixTheme.colorScheme.onPrimary
                        CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides androidx.compose.ui.graphics.Color.Black,
                            androidx.compose.material3.LocalTextStyle provides androidx.compose.ui.text.TextStyle(color = androidx.compose.ui.graphics.Color.Black),
                        ) {
                            AlertDialog(true, {}, miuixTitle = "Theme check", confirmButton = {
                                TextButton(onClick = {}) { Text("Action", onTextLayout = { actionColor = it.layoutInput.style.color }) }
                            }, text = {
                                Text("Material default", onTextLayout = { materialColor = it.layoutInput.style.color })
                                top.yukonga.miuix.kmp.basic.Text("MIUIX default", onTextLayout = { miuixColor = it.layoutInput.style.color })
                                Text("Explicit", color = androidx.compose.ui.graphics.Color.Red,
                                    onTextLayout = { explicitColor = it.layoutInput.style.color })
                            })
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(expected, materialColor)
            assertEquals(expected, miuixColor)
            assertEquals(androidx.compose.ui.graphics.Color.Red, explicitColor)
            assertEquals(expectedAction, actionColor)
        }
    }

    private fun setDialog(visible: MutableState<Boolean>, conditional: Boolean, glass: Boolean,
                          onConfirm: () -> Unit = {}) {
        compose.setContent {
            MiuixTheme {
                CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = glass)) {
                    Scaffold {
                        ProvideAdvancedMaterialDialogBlurHost {
                            val currentHost = LocalAdvancedMaterialDialogBlurHostState.current
                            val currentBackdrop = LocalWindowGlassBackdrop.current
                            SideEffect { host = currentHost; backdrop = currentBackdrop }
                            Text("Page behind the glass")
                            if (!conditional || visible.value) {
                                AlertDialog(show = visible.value, onDismissRequest = { visible.value = false },
                                    miuixTitle = "Dialog title", miuixSummary = "Dialog description",
                                    confirmButton = {
                                        TextButton(onClick = { onConfirm(); visible.value = false }) { Text("Confirm") }
                                    })
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }
}
