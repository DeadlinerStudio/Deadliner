package com.aritxonly.deadliner.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aritxonly.deadliner.R

internal val CloudSyncIntervals = listOf(
    RadioOption(0, R.string.sync_interval_manual),
    RadioOption(15, R.string.sync_interval_15min),
    RadioOption(30, R.string.sync_interval_30min),
    RadioOption(60, R.string.sync_interval_1h),
    RadioOption(180, R.string.sync_interval_3h),
    RadioOption(360, R.string.sync_interval_6h),
    RadioOption(720, R.string.sync_interval_12h),
    RadioOption(1440, R.string.sync_interval_24h),
)

@Composable
internal fun CloudSyncAutomationOptions(
    interval: Int, wifiOnly: Boolean, chargingOnly: Boolean,
    onIntervalChange: (Int) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onChargingOnlyChange: (Boolean) -> Unit,
) {
    SettingsSection(topLabel = stringResource(R.string.settings_web_auto_sync)) {
        SettingsDetailSwitchItem(headline = R.string.settings_web_wifi_only,
            supportingText = R.string.settings_support_web_wifi_only,
            checked = wifiOnly, onCheckedChange = onWifiOnlyChange)
        SettingsSectionDivider()
        SettingsDetailSwitchItem(headline = R.string.settings_web_charging_only,
            supportingText = R.string.settings_support_web_charging_only,
            checked = chargingOnly, onCheckedChange = onChargingOnlyChange)
        SettingsSectionDivider()
        SettingsDropdownItem(title = stringResource(R.string.settings_web_auto_sync_time),
            options = CloudSyncIntervals, selectedKey = interval, onOptionSelected = onIntervalChange)
    }
}
