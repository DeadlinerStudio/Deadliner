package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
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
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_lab),
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

            MiuixModalMaterialLab(
                value = appearance.advancedMaterialFineTuning,
                enabled = appearance.usesMiuixTheme && appearance.useAdvancedMaterial,
                onValueChangeFinished = { updated ->
                    GlobalUtils.updateAppearance { current ->
                        current.copy(advancedMaterialFineTuning = updated.normalized())
                    }
                },
            )

            SettingsSection(topLabel = stringResource(R.string.settings_advance)) {
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_custom_filter_list,
                    supporting = R.string.settings_support_custom_filter_list
                ) { onClickCustomFilter() }
            }
        }
    }
}
