package com.aritxonly.deadliner.ui.settings

import android.app.Application
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.*
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.aritxonly.deadliner.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Resetter
import org.robolectric.shadows.ShadowWebView

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, shadows = [SettingsWebContentTest.PaintWebView::class],
    qualifiers = "w411dp-h891dp-mdpi")
class SettingsWebContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val url = "https://example.invalid/wiki"

    @Test fun firstPageWaitsForAVisualFrameAndThemeUpdatesDoNotReloadTheBrowser() {
        val dark = mutableStateOf(true)
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                SettingsWebContent(url, Modifier.fillMaxSize())
            }
        }
        val view = browser()
        val shadow = shadowOf(view)
        assertEquals(url, shadow.lastLoadedUrl)
        compose.onNodeWithTag("settings-web-cover").assertExists()
        compose.runOnIdle {
            shadow.webViewClient.onPageStarted(view, url, null)
            shadow.webViewClient.onPageCommitVisible(view, url)
        }
        compose.onNodeWithTag("settings-web-cover").assertExists()
        compose.runOnIdle { PaintWebView.completeNext() }
        compose.onNodeWithTag("settings-web-cover").assertDoesNotExist()
        val background = shadow.backgroundColor
        compose.runOnIdle { dark.value = false }
        assertSame(view, browser())
        assertNotEquals(background, shadow.backgroundColor)
        assertEquals(url, shadow.lastLoadedUrl)
        assertEquals(0, shadow.reloadInvocations)
        // Internal links retain the previous page instead of flashing the full-page cover again.
        compose.runOnIdle { shadow.webViewClient.onPageStarted(view, url, null) }
        compose.onNodeWithTag("settings-web-cover").assertDoesNotExist()
    }

    @Test fun redirectCannotRevealAStaleFrame() {
        compose.setContent { MaterialTheme { SettingsWebContent(url, Modifier.fillMaxSize()) } }
        val view = browser()
        val client = shadowOf(view).webViewClient
        compose.runOnIdle {
            client.onPageStarted(view, url, null)
            client.onPageCommitVisible(view, url)
            val next = "$url/redirected"
            view.loadUrl(next)
            client.onPageStarted(view, next, null)
            PaintWebView.completeNext()
        }
        compose.onNodeWithTag("settings-web-cover").assertExists()
        compose.runOnIdle {
            // A late finish from the previous URL must not queue a reveal for the new document.
            client.onPageFinished(view, url)
            assertTrue(PaintWebView.pending.isEmpty())
            client.onPageCommitVisible(view, "$url/redirected")
            PaintWebView.completeNext()
        }
        compose.onNodeWithTag("settings-web-cover").assertDoesNotExist()
    }

    @Test fun mainFrameFailureSupportsRetryAndLeavingDestroysTheNativeBrowser() {
        val visible = mutableStateOf(true)
        compose.setContent { MaterialTheme { if (visible.value) SettingsWebContent(url, Modifier.fillMaxSize()) } }
        val view = browser()
        val shadow = shadowOf(view)
        compose.runOnIdle {
            shadow.webViewClient.onPageStarted(view, url, null)
            shadow.webViewClient.onPageCommitVisible(view, url)
            shadow.webViewClient.onReceivedError(view, request(main = false), null)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.settings_web_retry)).assertDoesNotExist()
        compose.runOnIdle { shadow.webViewClient.onReceivedError(view, request(main = true), null) }
        compose.onNodeWithText(compose.activity.getString(R.string.settings_web_retry)).performClick()
        assertEquals(1, shadow.reloadInvocations)
        compose.runOnIdle { PaintWebView.completeNext() }
        compose.onNodeWithTag("settings-web-cover").assertExists()
        compose.runOnIdle {
            shadow.webViewClient.onPageStarted(view, url, null)
            shadow.webViewClient.onPageCommitVisible(view, url)
            visible.value = false
        }
        compose.waitForIdle()
        assertTrue(shadow.wasDestroyCalled())
        compose.runOnIdle { PaintWebView.completeNext() }
        compose.onNodeWithTag("settings-web-cover").assertDoesNotExist()
    }

    private fun browser(): WebView {
        fun find(view: View): WebView? {
            if (view is WebView) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
            return null
        }
        var found: WebView? = null
        compose.runOnIdle { found = find(compose.activity.window.decorView) }
        return requireNotNull(found)
    }

    private fun request(main: Boolean) = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(this@SettingsWebContentTest.url)
        override fun isForMainFrame() = main
        override fun isRedirect() = false
        override fun hasGesture() = false
        override fun getMethod() = "GET"
        override fun getRequestHeaders(): MutableMap<String, String> = mutableMapOf()
    }

    @Implements(WebView::class)
    class PaintWebView : ShadowWebView() {
        @Implementation fun postVisualStateCallback(id: Long, callback: WebView.VisualStateCallback) {
            pending.add(id to callback)
        }
        companion object {
            val pending = mutableListOf<Pair<Long, WebView.VisualStateCallback>>()
            fun completeNext() { val (id, callback) = pending.removeAt(0); callback.onComplete(id) }
            @Resetter @JvmStatic fun clearPending() { pending.clear() }
        }
    }
}
