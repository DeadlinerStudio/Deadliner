package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.CloudSyncProvider
import com.aritxonly.deadliner.localutils.CloudSyncPreferences
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.sync.SyncScheduler
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@Composable
fun CloudSyncSettingsScreen(
    navigateToByos: () -> Unit,
    navigateToWebDav: () -> Unit,
    navigateUp: () -> Unit,
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(GlobalUtils.cloudSyncEnable) }
    var provider by remember { mutableStateOf(CloudSyncPreferences.provider) }
    var interval by remember { mutableIntStateOf(GlobalUtils.syncIntervalMinutes.coerceAtLeast(0)) }
    var wifiOnly by remember { mutableStateOf(GlobalUtils.syncWifiOnly) }
    var chargingOnly by remember { mutableStateOf(GlobalUtils.syncChargingOnly) }
    var retentionDays by remember { mutableIntStateOf(GlobalUtils.tombstoneRetentionDays) }

    fun updateSchedule() {
        KmpV6Runtime.updateAutomaticSync()
        if (GlobalUtils.cloudSyncEnable) SyncScheduler.enqueuePeriodic(context)
        else SyncScheduler.cancelAll(context)
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_cloud_and_sync),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = (navIconPaddingModifier).miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape)) {
                Icon(com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                    stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier)
            }
        },
    ) { innerPadding ->
        SettingsLazyColumn(contentPadding = innerPadding) {
            item {
                SettingsSection(mainContent = true, enabled = enabled) {
                    SettingsSwitchItem(R.string.settings_enable_cloud_sync, enabled, {
                        enabled = it
                        GlobalUtils.cloudSyncEnable = it
                        updateSchedule()
                    }, mainSwitch = true)
                }
            }
            item { SvgCard(R.drawable.svg_cloud_sync, Modifier.padding(vertical = 8.dp)) }
            item {
                SettingsSection(topLabel = stringResource(R.string.settings_sync_provider)) {
                    SettingsDropdownItem(
                        title = stringResource(R.string.settings_sync_provider),
                        options = listOf(RadioOption(CloudSyncProvider.BYOS, R.string.settings_sync_only_byos),
                            RadioOption(CloudSyncProvider.WEB_DAV, R.string.settings_sync_only_webdav)),
                        selectedKey = provider,
                        onOptionSelected = {
                            provider = it
                            CloudSyncPreferences.provider = it
                            updateSchedule()
                        },
                    )
                    SettingsSectionDivider()
                    SettingItem(stringResource(R.string.settings_byos), stringResource(R.string.settings_support_byos),
                        modifier = Modifier.fillMaxWidth().clickable(onClick = navigateToByos))
                    SettingsSectionDivider()
                    SettingItem(stringResource(R.string.settings_webdav), stringResource(R.string.settings_support_webdav),
                        modifier = Modifier.fillMaxWidth().clickable(onClick = navigateToWebDav))
                }
            }
            item {
                CloudSyncAutomationOptions(interval, wifiOnly, chargingOnly,
                    onIntervalChange = { interval = it; GlobalUtils.syncIntervalMinutes = it; updateSchedule() },
                    onWifiOnlyChange = { wifiOnly = it; GlobalUtils.syncWifiOnly = it; updateSchedule() },
                    onChargingOnlyChange = { chargingOnly = it; GlobalUtils.syncChargingOnly = it; updateSchedule() })
            }
            item {
                SettingsSection(topLabel = stringResource(R.string.settings_tombstone_retention)) {
                    SettingsSliderItemWithLabel(label = R.string.settings_tombstone_retention,
                        value = retentionDays.toFloat(), valueRange = 0f..90f, steps = 89,
                        onValueChange = { retentionDays = it.toInt(); GlobalUtils.tombstoneRetentionDays = it.toInt() })
                }
                androidx.compose.material3.Text(stringResource(R.string.settings_tombstone_retention_description, retentionDays),
                    Modifier.padding(horizontal = SettingsItemDefaults.HorizontalPadding), style = MaterialTheme.typography.bodySmall)
            }
            item { Spacer(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) }
        }
    }
}
