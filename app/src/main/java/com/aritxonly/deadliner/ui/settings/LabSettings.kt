package com.aritxonly.deadliner.ui.settings

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@Composable
fun LabSettingsScreen(
    onClickCustomDisplayScale: () -> Unit,
    onClickCustomFilter: () -> Unit,
    onClickCancelAll: () -> Unit,
    onClickShowIntro: () -> Unit,
    navigateUp: () -> Unit
) {
    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_lab),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = navIconPaddingModifier
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { padding ->
        SettingsScrollColumn(
            contentPadding = padding,
            modifier = Modifier,
        ) {
            SvgCard(R.drawable.svg_developer_avatar, modifier = Modifier.padding(vertical = 8.dp))

            SettingsSection(topLabel = stringResource(R.string.settings_experimental)) {
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_display_size_custom_title,
                    supporting = R.string.settings_support_display_size_lab
                ) { onClickCustomDisplayScale() }
            }

            SettingsSection(topLabel = stringResource(R.string.settings_advance)) {
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_custom_filter_list,
                    supporting = R.string.settings_support_custom_filter_list
                ) { onClickCustomFilter() }
            }
        }
    }
}
