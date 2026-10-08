package com.aritxonly.deadliner.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.logging.AppLog
import com.aritxonly.deadliner.notification.NotificationDiagnosticController
import com.aritxonly.deadliner.notification.NotificationDiagnostics
import com.aritxonly.deadliner.notification.NotificationTestPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun NotificationDiagnosticSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by NotificationDiagnosticController.state.collectAsState()
    val label = stringResource(when (state.phase) {
        NotificationTestPhase.IDLE -> R.string.notification_test_idle
        NotificationTestPhase.SCHEDULED -> if (state.detail == "inexact") R.string.notification_test_inexact else R.string.notification_test_scheduled
        NotificationTestPhase.RUNNING -> R.string.notification_test_running
        NotificationTestPhase.FINISHED -> R.string.notification_test_finished
        NotificationTestPhase.BLOCKED -> R.string.notification_test_blocked
        NotificationTestPhase.FAILED -> R.string.notification_test_failed
    })
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { destination ->
        if (destination != null) scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    NotificationDiagnostics.snapshot(context, state.run)
                    NotificationDiagnostics.event("diagnostics.export", "run" to state.run)
                    val log = AppLog.exportFiles().first { it.extension == "log" && it.isFile }
                    val output = checkNotNull(context.contentResolver.openOutputStream(destination)) { "Output stream unavailable" }
                    output.use { sink -> log.inputStream().use { it.copyTo(sink) } }
                }
            }
            result.onFailure { NotificationDiagnostics.failure("diagnostics.export_failed", it) }
            Toast.makeText(context, if (result.isSuccess) R.string.notification_test_export_success else R.string.notification_test_export_failed, Toast.LENGTH_SHORT).show()
        }
    }
    SettingsSection(topLabel = stringResource(R.string.notification_test_section)) {
        SettingsDetailTextButtonItem(
            headlineText = stringResource(R.string.notification_test_immediate),
            supportingText = label,
            onClick = { NotificationDiagnosticController.start(context, throughAlarm = false) },
        )
        SettingsSectionDivider()
        SettingsDetailTextButtonItem(
            headline = R.string.notification_test_alarm, supporting = R.string.notification_test_alarm_desc,
            onClick = { NotificationDiagnosticController.start(context, throughAlarm = true) },
        )
        SettingsSectionDivider()
        SettingsDetailTextButtonItem(
            headline = R.string.notification_test_stop, supporting = R.string.notification_test_stop_desc,
            onClick = { NotificationDiagnosticController.stop(context) },
        )
        SettingsSectionDivider()
        SettingsDetailTextButtonItem(
            headline = R.string.notification_test_export, supporting = R.string.notification_test_export_desc,
            onClick = {
                export.launch("deadliner-notification-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.txt")
            },
        )
    }
}
