package com.aritxonly.deadliner.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import rikka.shizuku.Shizuku
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.notification.IslandBypassMode
import com.aritxonly.deadliner.notification.IslandWhitelistBypass
import com.aritxonly.deadliner.notification.LiveUpdateNotificationAdapter

@Composable
internal fun IslandNotificationSettings() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= 36) {
        SettingsSection(topLabel = stringResource(R.string.notification_live_update_section)) {
            SettingsDetailTextButtonItem(
                headline = R.string.notification_live_update_permission,
                supporting = R.string.notification_live_update_permission_desc,
                onClick = {
                    runCatching {
                        context.startActivity(Intent("android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS")
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }.onFailure { Toast.makeText(context, R.string.notification_settings_unavailable, Toast.LENGTH_SHORT).show() }
                },
            )
        }
    }
    if (!LiveUpdateNotificationAdapter.supportsIsland()) return
    var mode by remember { mutableStateOf(IslandWhitelistBypass.mode(context)) }
    val status by IslandWhitelistBypass.status.collectAsState()
    val displayStatus = when (status) {
        "Connected" -> stringResource(R.string.notification_island_connected)
        "Restored" -> stringResource(R.string.notification_island_restored)
        "Shizuku is not running" -> stringResource(R.string.notification_island_shizuku_unavailable)
        "Shizuku permission required" -> stringResource(R.string.notification_island_permission_required)
        "Restore pending; use Restore network", "Network restore failed" -> stringResource(R.string.notification_island_restore_failed)
        "Firewall backend rejected the bypass" -> stringResource(R.string.notification_island_unsupported)
        "Choose Shizuku or Root first" -> stringResource(R.string.notification_island_choose_mode)
        else -> status
    }
    DisposableEffect(context) {
        val listener = Shizuku.OnRequestPermissionResultListener { code, result ->
            if (code == 52002 && result == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                IslandWhitelistBypass.authorize(context)
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }
    SettingsSection(topLabel = stringResource(R.string.notification_island_section)) {
        SettingsDetailSwitchItem(
            headline = R.string.notification_island_shizuku,
            supportingText = R.string.notification_island_bypass_desc,
            checked = mode == IslandBypassMode.SHIZUKU,
            onCheckedChange = {
                mode = if (it) IslandBypassMode.SHIZUKU else IslandBypassMode.OFF
                IslandWhitelistBypass.setMode(context, mode)
            },
        )
        SettingsSectionDivider()
        SettingsDetailSwitchItem(
            headline = R.string.notification_island_root,
            supportingText = R.string.notification_island_root_desc,
            checked = mode == IslandBypassMode.ROOT,
            onCheckedChange = {
                mode = if (it) IslandBypassMode.ROOT else IslandBypassMode.OFF
                IslandWhitelistBypass.setMode(context, mode)
            },
        )
        SettingsSectionDivider()
        SettingsDetailSwitchItem(
            headline = R.string.notification_island_system_hook,
            supportingText = R.string.notification_island_system_hook_desc,
            checked = mode == IslandBypassMode.SYSTEM_HOOK,
            onCheckedChange = {
                mode = if (it) IslandBypassMode.SYSTEM_HOOK else IslandBypassMode.OFF
                IslandWhitelistBypass.setMode(context, mode)
            },
        )
        if (mode == IslandBypassMode.SHIZUKU || mode == IslandBypassMode.ROOT) {
            SettingsSectionDivider()
            SettingsDetailTextButtonItem(
                headlineText = stringResource(R.string.notification_island_authorize),
                supportingText = displayStatus ?: stringResource(R.string.notification_island_authorize_desc),
                onClick = {
                    if (mode == IslandBypassMode.SHIZUKU) IslandWhitelistBypass.requestShizukuPermission()
                    IslandWhitelistBypass.authorize(context)
                },
            )
        }
        SettingsSectionDivider()
        SettingsDetailTextButtonItem(
            headlineText = stringResource(R.string.notification_island_restore),
            supportingText = displayStatus ?: stringResource(R.string.notification_island_restore_desc),
            onClick = { IslandWhitelistBypass.restore(context) },
        )
    }
}
