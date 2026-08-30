package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.CloudSyncProvider
import com.aritxonly.deadliner.localutils.CloudSyncPreferences
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@Composable
fun CloudSyncSettingsScreen(
    navigateToByos: () -> Unit,
    navigateToWebDav: () -> Unit,
    navigateUp: () -> Unit,
) {
    val activeProvider = CloudSyncPreferences.provider
    val activeSummary = if (GlobalUtils.cloudSyncEnable) {
        stringResource(
            if (activeProvider == CloudSyncProvider.BYOS) {
                R.string.settings_sync_active_byos
            } else {
                R.string.settings_sync_active_webdav
            },
        )
    } else {
        stringResource(R.string.settings_sync_disabled)
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_cloud_and_sync),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = navIconPaddingModifier) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier,
                )
            }
        },
    ) { innerPadding ->
        SettingsLazyColumn(contentPadding = innerPadding) {
            item {
                SettingsSection(topLabel = stringResource(R.string.settings_sync_current)) {
                    SettingItem(
                        headlineText = stringResource(R.string.settings_sync_status),
                        supportingText = activeSummary,
                        trailingContent = null,
                    )
                }
            }
            item {
                SettingsSection(topLabel = stringResource(R.string.settings_sync_provider)) {
                    SettingItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = navigateToByos),
                        headlineText = stringResource(R.string.settings_byos),
                        supportingText = stringResource(R.string.settings_support_byos),
                        leadingContent = {
                            Icon(
                                painter = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_deployed_code_wght400),
                                contentDescription = null,
                            )
                        },
                    )
                    SettingsSectionDivider()
                    SettingItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = navigateToWebDav),
                        headlineText = stringResource(R.string.settings_webdav),
                        supportingText = stringResource(R.string.settings_support_webdav),
                        leadingContent = {
                            Icon(
                                painter = com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_cloud_wght400),
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
            item { Spacer(modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp)) }
        }
    }
}
