package com.aritxonly.deadliner.ui.settings

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.TextButton

/** Native web content stays outside page-backdrop recorders. Only reveal a committed visual frame. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsWebContent(url: String, modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.surface
    var webView by remember { mutableStateOf<WebView?>(null) }
    var firstFrameReady by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var released by remember { mutableStateOf(false) }
    Box(modifier.background(background)) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(background.toArgb())
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        private var navigation = 0L

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            if (released) return
                            navigation++
                            failed = false
                            // Keep the previous committed page visible while following links.
                        }

                        private fun revealCommittedFrame(view: WebView?, url: String?) {
                            if (released || failed || view == null || (url != null && url != view.url)) return
                            val request = navigation
                            view.postVisualStateCallback(request, object : WebView.VisualStateCallback() {
                                override fun onComplete(requestId: Long) {
                                    if (!released && requestId == navigation && !failed) firstFrameReady = true
                                }
                            })
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) = revealCommittedFrame(view, url)
                        override fun onPageFinished(view: WebView?, url: String?) = revealCommittedFrame(view, url)

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (!released && request?.isForMainFrame == true) {
                                navigation++ // Invalidate pending paint callbacks before a possible retry.
                                failed = true
                            }
                        }
                    }
                    webView = this
                    loadUrl(url)
                }
            },
            update = { it.setBackgroundColor(background.toArgb()) },
            onRelease = {
                released = true
                it.stopLoading()
                it.webViewClient = WebViewClient()
                it.destroy()
                webView = null
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (!firstFrameReady || failed) {
            // An opaque, theme-matched cover avoids flashing WebView's empty/white first frame.
            // It does not remove, remeasure or recreate the browser when loading changes.
            Box(Modifier.fillMaxSize().background(background).testTag("settings-web-cover"),
                contentAlignment = Alignment.Center) {
                if (failed) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.settings_web_load_failed), color = MaterialTheme.colorScheme.onSurface)
                        TextButton(onClick = { failed = false; webView?.reload() }) {
                            Text(stringResource(R.string.settings_web_retry))
                        }
                    }
                } else LoadingIndicator(Modifier.size(64.dp), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
