package com.aritxonly.deadliner.ui.settings

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UpdateDownloadDialogTest {
    @get:Rule val compose = createComposeRule()
    private val available = UpdateState.Available("5.0.0-RC", "v5.0.0", "## 更新内容\n- 更顺滑的体验", "https://example.invalid/update.apk")

    @Composable private fun Page(dark: Boolean, onDownload: () -> Unit, overlay: @Composable () -> Unit) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
            MiuixTheme(colors = if (dark) top.yukonga.miuix.kmp.theme.darkColorScheme()
                else top.yukonga.miuix.kmp.theme.lightColorScheme()) {
                CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = true)) {
                    UpdatePage("5.0.0-RC", available, {}, {}, { onDownload() }, {}, overlay)
                }
            }
        }
    }

    @Test fun installActionOpensProjectDialogAndSupportsCancelRetryAndReadyInstallation() {
        val state = mutableStateOf<UpdateDownloadState?>(null)
        var retries = 0
        var installs = 0
        compose.setContent { Page(false, { state.value = UpdateDownloadState.Downloading(37, 100) }) {
            UpdateDownloadDialog(state.value, { state.value = null }, {
                retries++; state.value = UpdateDownloadState.Downloading()
            }, { installs++; state.value = null })
        } }
        compose.onNodeWithTag("update-primary").performClick()
        compose.onNodeWithTag("update-download-dialog").assertIsDisplayed()
        compose.onNodeWithTag("update-download-confirm").assertIsNotEnabled().performClick()
        assertEquals(0, installs)
        compose.onNodeWithTag("update-download-cancel").performClick()
        compose.onNodeWithTag("update-download-dialog").assertDoesNotExist()
        compose.runOnIdle { state.value = UpdateDownloadState.Failed }
        compose.onNodeWithTag("update-download-confirm").assertIsEnabled().performClick()
        assertEquals(1, retries)
        compose.onNodeWithTag("update-download-confirm").assertIsNotEnabled()
        compose.runOnIdle { state.value = UpdateDownloadState.Ready(File("ready.apk"), permissionRequired = true) }
        compose.onNodeWithTag("update-download-confirm").assertIsEnabled().performClick()
        assertEquals(1, installs)
        compose.onNodeWithTag("update-download-dialog").assertDoesNotExist()
    }

    @Test fun progressFailureAndPermissionRenderInBothThemesIncludingUnknownSize() {
        val state = mutableStateOf<UpdateDownloadState?>(UpdateDownloadState.Downloading(18_500_000, 50_000_000))
        val dark = mutableStateOf(false)
        compose.setContent { Page(dark.value, {}) { UpdateDownloadDialog(state.value, {}, {}, {}) } }
        save("download-light")
        compose.runOnIdle { dark.value = true }
        save("download-dark")
        compose.runOnIdle { state.value = UpdateDownloadState.Downloading(2048, -1) }
        compose.onNodeWithTag("update-download-progress").assertIsDisplayed()
        save("download-indeterminate-dark")
        compose.runOnIdle { state.value = UpdateDownloadState.Failed }
        save("download-failed-dark")
        compose.runOnIdle { state.value = UpdateDownloadState.Ready(File("ready.apk"), permissionRequired = true) }
        save("download-permission-dark")
        compose.runOnIdle { dark.value = false }
        save("download-permission-light")
    }

    private fun save(name: String) {
        val dir = File("build/reports/update-page").apply { mkdirs() }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
