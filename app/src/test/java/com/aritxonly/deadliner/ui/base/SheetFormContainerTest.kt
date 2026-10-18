package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.aritxonly.deadliner.ui.theme.LocalTextFieldContainerColors
import com.aritxonly.deadliner.ui.theme.TextFieldContainerColors
import com.aritxonly.deadliner.ui.theme.textFieldContainer
import com.aritxonly.deadliner.ui.theme.textFieldSheetContainer
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
class SheetFormContainerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sharedSheetProvidesItsSemanticColorWithoutChangingThePageOrGlobalPalette() {
        val base = Color(0xFFE6E0E8)
        val colors = TextFieldContainerColors(base, base.copy(alpha = 0.65f))
        var page = Color.Unspecified
        var sheet = Color.Unspecified
        var namedSheet = Color.Unspecified
        var sheetScope = false
        compose.setContent {
            MiuixTheme {
                MaterialTheme {
                    CompositionLocalProvider(LocalTextFieldContainerColors provides colors) {
                        Scaffold {
                            ProvideAdvancedMaterialDialogBlurHost {
                                val pageColor = formContainerColor(Color.Red)
                                val namedPage = MaterialTheme.colorScheme.textFieldContainer
                                SideEffect { page = pageColor; assertEquals(base, namedPage) }
                                MiuixBottomSheet(show = true, onDismissRequest = {}) {
                                    val resolved = formContainerColor(Color.Green)
                                    val named = MaterialTheme.colorScheme.textFieldSheetContainer
                                    val scope = LocalSheetFormContainers.current
                                    SideEffect { sheet = resolved; namedSheet = named; sheetScope = scope }
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(base, page)
            assertTrue(sheetScope)
            assertEquals(colors.textFieldSheetContainer, sheet)
            assertEquals(sheet, namedSheet)
            assertEquals(0.65f, sheet.alpha, 0.005f)
        }
    }
}
