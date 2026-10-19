package com.aritxonly.deadliner.ui.settings

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.web.UpdateInfo
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UpdatePageTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = RuntimeEnvironment.getApplication()
    private val url = "https://example.invalid/deadliner.apk"
    private val available = UpdateState.Available("5.0.0-RC", "v5.0.0", "### 更顺滑的交互\n优化快速添加与下拉菜单。\n\n### 体验修复\n修复部分页面闪烁。", url)

    @Composable private fun Theme(dark: Boolean = false, content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme(surface = Color(0xFF101112), surfaceContainer = Color(0xFF202123))
            else lightColorScheme(surface = Color(0xFFF4F4F4), surfaceContainer = Color.White, primary = Color(0xFF3482FF))) {
            MiuixTheme(colors = if (dark) top.yukonga.miuix.kmp.theme.darkColorScheme()
                else top.yukonga.miuix.kmp.theme.lightColorScheme(), content = content)
        }
    }

    @Test fun failureRetryAndAvailableActionsUseTheRealRequestResult() {
        val first = CompletableDeferred<UpdateInfo>()
        val retry = CompletableDeferred<UpdateInfo>()
        var calls = 0
        val installs = mutableListOf<String>()
        val browser = mutableListOf<String>()
        compose.setContent { Theme {
            UpdateRoute("5.0.0-RC", {}, { if (++calls == 1) first.await() else retry.await() }, installs::add, browser::add)
        } }
        compose.onNodeWithTag("update-primary").assertIsNotEnabled()
        compose.onNodeWithTag("update-secondary").assertIsNotEnabled()
        assertEquals(1, calls)
        compose.runOnIdle { first.completeExceptionally(IOException("HTTP 503")) }
        compose.onNodeWithText(app.getString(R.string.update_error_label)).assertIsDisplayed()
        compose.onNodeWithText("HTTP 503").assertDoesNotExist()
        compose.onNodeWithTag("update-secondary").performClick()
        assertEquals(listOf(DeadlinerReleasesUrl), browser)
        compose.onNodeWithTag("update-primary").performClick()
        compose.onNodeWithTag("update-primary").assertIsNotEnabled()
        assertEquals(2, calls)
        compose.runOnIdle { retry.complete(UpdateInfo("5.0.0-RC", "v5.0.0", "Actual release notes", url)) }
        compose.onNodeWithText("5.0.0").assertIsDisplayed()
        compose.onNodeWithTag("update-primary").performClick()
        compose.onNodeWithTag("update-secondary").performClick()
        assertEquals(listOf(url), installs)
        assertEquals(listOf(DeadlinerReleasesUrl, url), browser)
    }

    @Test fun themeRecompositionDoesNotRefetchAndCheckingAgainDoes() {
        val dark = mutableStateOf(false)
        var calls = 0
        var opened: String? = null
        compose.setContent { Theme(dark.value) {
            UpdateRoute("5.0.0", {}, { calls++; UpdateInfo("5.0.0", "v5.0.0", "", "") }, {}, { opened = it })
        } }
        compose.onNodeWithText(app.getString(R.string.update_latest_label)).assertIsDisplayed()
        compose.onNodeWithTag("update-secondary").performClick()
        assertEquals("https://github.com/AritxOnly/Deadliner/releases", opened)
        compose.runOnIdle { dark.value = true }
        compose.waitForIdle()
        assertEquals(1, calls)
        compose.onNodeWithTag("update-primary").performClick()
        compose.waitForIdle()
        assertEquals(2, calls)
    }

    @Test fun leavingThePageCancelsItsPendingCheck() {
        val visible = mutableStateOf(true)
        var cancelled = false
        compose.setContent { Theme { if (visible.value) {
            UpdateRoute("5.0.0", {}, { try { awaitCancellation() } finally { cancelled = true } }, {}, {})
        } } }
        compose.onNodeWithTag("update-primary").assertIsNotEnabled()
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        assertTrue(cancelled)
    }

    @Test fun unavailableDownloadCannotInstallAndFallsBackToTheReleasePage() {
        val state = mutableStateOf<UpdateState>(available)
        val installs = mutableListOf<String>()
        val browser = mutableListOf<String>()
        compose.setContent { Theme { UpdatePage("5.0.0-RC", state.value, {}, {}, installs::add, browser::add) } }
        for (invalid in listOf("", "javascript:alert(1)", "https:///broken", "file:///tmp/update.apk")) {
            compose.runOnIdle { state.value = available.copy(downloadUrl = invalid) }
            compose.onNodeWithTag("update-primary").assertIsNotEnabled().performClick()
            compose.onNodeWithTag("update-secondary").performClick()
        }
        assertTrue(installs.isEmpty())
        assertEquals(List(4) { DeadlinerReleasesUrl }, browser)
        compose.runOnIdle { state.value = available.copy(downloadUrl = "  $url  ") }
        compose.onNodeWithTag("update-primary").assertIsEnabled().performClick()
        assertEquals(listOf(url), installs)
    }

    @Test fun longMarkdownScrollsClearOfTheFixedActionsAndLightDarkStatesRender() {
        val dark = mutableStateOf(false)
        val state = mutableStateOf<UpdateState>(available)
        compose.setContent { Theme(dark.value) { UpdatePage("5.0.0-RC", state.value, {}, {}, {}, {}) } }
        saveRender("available-light")
        compose.runOnIdle { dark.value = true }
        saveRender("available-dark")
        compose.runOnIdle { state.value = UpdateState.UpToDate("5.0.0-RC") }
        saveRender("latest-dark")
        compose.runOnIdle { state.value = UpdateState.Error("HTTP 503") }
        saveRender("error-dark")
        compose.runOnIdle { state.value = available.copy(notes = (1..60).joinToString("\n\n") { "### 改进 $it\n- 保留真实 Markdown 内容与滚动空间。" }) }
        compose.waitForIdle()
        val fixed = compose.onNodeWithTag("update-actions").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("update-content").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100000f) }
        compose.waitForIdle()
        val notes = compose.onNodeWithTag("update-release-notes").getUnclippedBoundsInRoot()
        assertTrue("Final release note stays above fixed controls: $notes / $fixed", notes.bottom.value <= fixed.top + 1f)
        assertEquals(fixed, compose.onNodeWithTag("update-actions").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun languageAndSectionFiltersSwitchCardsAndOriginalRemainsAvailable() {
        val bilingual = "#### 中文\n## 新增\n中文新功能\n## 修复\n中文修复\n#### English\n## Features\nEnglish feature\n## Fixes\nEnglish fix"
        compose.setContent { Theme { UpdatePage("5.0.0-RC", available.copy(notes = bilingual), {}, {}, {}, {}) } }
        saveRender("sections-light")
        // AndroidView Markdown text is verified by parser tests; these controls exercise the
        // actual selection / popup / saved-state path used by the production page.
        compose.onNodeWithTag("update-language-filter").assertDoesNotExist()
        compose.onNodeWithTag("update-log-filters").performClick()
        compose.onNodeWithTag("update-language-filter").performClick()
        compose.onNodeWithText("English", substring = true).performClick()
        compose.onNodeWithTag("update-section-filter").performClick()
        compose.onNodeWithText("Fixes").performClick()
        compose.onNodeWithTag("update-section-filter").assertExists()
        compose.onAllNodesWithTag("update-note-card").assertCountEquals(1)
        compose.onNodeWithTag("update-filters-done").performClick()
        compose.onNodeWithTag("update-original-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("update-language-filter").assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.update_formatted_notes)).assertIsDisplayed()
        compose.onNodeWithTag("update-original-toggle").performClick()
        compose.onNodeWithTag("update-log-filters").performClick()
        compose.onNodeWithTag("update-language-filter").assertExists()
        compose.onAllNodesWithTag("update-note-card").assertCountEquals(1)
        compose.onNodeWithTag("update-filters-done").performClick()
    }

    @Test fun exactUserReleaseRendersAndScrollsBehindAdvancedMaterialInBothLanguages() {
        val notes = javaClass.getResource("/updates/v5.0.0-bilingual.md")!!.readText()
        val dark = mutableStateOf(false)
        compose.setContent { Theme(dark.value) {
            CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = true)) {
                UpdatePage("5.0.0-RC", available.copy(notes = notes), {}, {}, {}, {})
            }
        } }
        compose.onAllNodesWithTag("update-note-card").assertCountEquals(7)
        saveRender("sample-chinese-light")
        compose.onNodeWithTag("update-log-filters").performClick()
        saveRender("log-filters-light")
        compose.onNodeWithTag("update-language-filter").performClick()
        compose.onNodeWithText("English", substring = true).performClick()
        compose.onAllNodesWithTag("update-note-card").assertCountEquals(7)
        compose.onNodeWithTag("update-filters-done").performClick()
        compose.runOnIdle { dark.value = true }
        saveRender("sample-english-dark")
        compose.onNodeWithTag("update-log-filters").performClick()
        saveRender("log-filters-dark")
        compose.onNodeWithTag("update-filters-done").performClick()
        val fixed = compose.onNodeWithTag("update-actions").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("update-content").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100000f) }
        compose.waitForIdle()
        assertTrue(compose.onNodeWithTag("update-release-notes").getUnclippedBoundsInRoot().bottom.value <= fixed.top + 1f)
        compose.onNodeWithTag("update-primary").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("update-secondary").assertIsDisplayed().assertIsEnabled()
        saveRender("sample-end-dark")
    }

    private fun saveRender(name: String) {
        val output = File("build/reports/update-page").apply { mkdirs() }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
