package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.logging.AppLog
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeedbackScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current
    val exportScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { destination ->
        if (destination == null) return@rememberLauncherForActivityResult
        exportScope.launch(Dispatchers.IO) {
            runCatching {
                AppLog.event("diagnostics.export.started", domain = "app")
                checkNotNull(context.contentResolver.openOutputStream(destination)).use { output ->
                    ZipOutputStream(output).use { archive ->
                        AppLog.exportFiles().filter(File::isFile).forEach { file ->
                            archive.putNextEntry(ZipEntry(file.name))
                            file.inputStream().use { input -> input.copyTo(archive) }
                            archive.closeEntry()
                        }
                    }
                }
                AppLog.event("diagnostics.export.finished", domain = "app")
            }.onFailure { AppLog.e("diagnostics.export", "Unable to export diagnostics", it) }
        }
    }

    AdaptiveMaterialScaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_feedback),
                mode = TopAppBarStyle.SMALL,
                navigationIcon = {
                    IconButton(
                        onClick = navigateUp,
                        modifier = (navIconPaddingModifier).miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape)
                    ) {
                        Icon(
                            com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = expressiveTypeModifier
                        )
                    }
                },
                actions = {
                    IconButton(modifier = Modifier.miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape), onClick = { exportLauncher.launch("deadliner-diagnostics.zip") }) {
                        Icon(
                            com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_share_wght400),
                            contentDescription = "导出诊断日志",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = expressiveTypeModifier,
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { contentPadding ->
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                        }
                        settings.javaScriptEnabled = true
                        loadUrl("https://github.com/AritxOnly/Deadliner/issues")
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )

            if (isLoading) {
                LoadingIndicator(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
