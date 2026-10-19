package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.logging.AppLog
import com.aritxonly.deadliner.ui.base.Scaffold
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

    // WebView owns native rendering; do not record it into a Compose glass backdrop.
    Scaffold(
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
        SettingsWebContent(
            url = "https://github.com/AritxOnly/Deadliner/issues",
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        )
    }
}
