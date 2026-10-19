package com.aritxonly.deadliner.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.TextButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator

@Composable
internal fun UpdateDownloadDialog(state: UpdateDownloadState?, onCancel: () -> Unit, onRetry: () -> Unit, onInstall: () -> Unit) {
    val downloading = state as? UpdateDownloadState.Downloading
    val ready = state as? UpdateDownloadState.Ready
    val failed = state == UpdateDownloadState.Failed
    val colors = MaterialTheme.colorScheme
    AlertDialog(show = state != null,
        onDismissRequest = { if (downloading == null) onCancel() },
        miuixTitle = stringResource(when {
            failed -> R.string.update_download_failed_title
            ready?.permissionRequired == true -> R.string.update_install_permission_title
            ready != null -> R.string.update_download_ready_title
            else -> R.string.download_update
        }),
        text = {
            Column(Modifier.fillMaxWidth().testTag("update-download-dialog"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(when {
                    failed -> R.string.update_download_failed_summary
                    ready?.installFailed == true -> R.string.update_install_failed_summary
                    ready?.permissionRequired == true -> R.string.update_install_permission_summary
                    ready != null -> R.string.update_download_ready_summary
                    else -> R.string.update_download_summary
                }), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                if (downloading != null) {
                    val fraction = if (downloading.total > 0) (downloading.bytes.toDouble() / downloading.total).toFloat().coerceIn(0f, 1f) else null
                    LinearProgressIndicator(progress = fraction, modifier = Modifier.testTag("update-download-progress"))
                    val context = LocalContext.current
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (fraction == null) stringResource(R.string.update_download_connecting) else "${(fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        val bytes = Formatter.formatShortFileSize(context, downloading.bytes)
                        Text(if (downloading.total > 0) "$bytes / ${Formatter.formatShortFileSize(context, downloading.total)}" else bytes,
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("update-download-cancel")) {
                Text(stringResource(if (downloading != null) R.string.cancel else R.string.close))
            }
        },
        confirmButton = {
            TextButton(onClick = if (failed) onRetry else onInstall, enabled = downloading == null,
                modifier = Modifier.testTag("update-download-confirm")) {
                Text(stringResource(if (failed) R.string.update_download_retry else R.string.install))
            }
        },
    )
}
