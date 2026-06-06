package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.ui.base.RegisterAdvancedMaterialDialogBlur
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    nav: NavHostController,
    navigateUp: () -> Unit,
) {
    var progressDirEnabled by remember { mutableStateOf(GlobalUtils.progressDir) }
    var motivationalQuotesEnabled by remember { mutableStateOf(GlobalUtils.motivationalQuotes) }
    var fireworksOnFinishEnabled by remember { mutableStateOf(GlobalUtils.fireworksOnFinish) }
    var detailDisplayEnabled by remember { mutableStateOf(GlobalUtils.detailDisplayMode) }
    var showWelcome by rememberSaveable {
        mutableStateOf(!GlobalUtils.appearanceRefactorIntroSeen)
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_interface_display),
        navigationIcon = { DefaultBackButton(navigateUp) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsScrollColumn(
                contentPadding = padding,
                modifier = Modifier,
            ) {
                SvgCard(R.drawable.svg_interface, modifier = Modifier.padding(vertical = 8.dp))

                SettingsSection(topLabel = stringResource(R.string.settings_design)) {
                    listOf(
                        SettingsRoute.AppearanceDesign,
                        SettingsRoute.AppearanceMaterial,
                    ).forEachIndexed { index, route ->
                        SettingItem(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .fillMaxWidth()
                                .clickable { nav.navigate(route.route) },
                            headlineText = stringResource(route.titleRes),
                            supportingText = stringResource(route.supportRes!!),
                            trailingContent = null,
                        )
                        if (index == 0) {
                            SettingsSectionDivider()
                        }
                    }
                }

                SettingsSection {
                    SettingItem(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth()
                            .clickable { nav.navigate(SettingsRoute.AppIcon.route) },
                        headlineText = stringResource(SettingsRoute.AppIcon.titleRes),
                        supportingText = stringResource(SettingsRoute.AppIcon.supportRes!!),
                        trailingContent = null,
                    )
                }

                SettingsSection(topLabel = stringResource(R.string.settings_interface_display)) {
                    listOf(
                        SettingsRoute.UI,
                        SettingsRoute.DisplayScale,
                    ).forEachIndexed { index, route ->
                        SettingItem(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .fillMaxWidth()
                                .clickable { nav.navigate(route.route) },
                            headlineText = stringResource(route.titleRes),
                            supportingText = stringResource(route.supportRes!!),
                            trailingContent = null,
                        )
                        if (index == 0) {
                            SettingsSectionDivider()
                        }
                    }
                }

                SettingsSection {
                    SettingsDetailSwitchItem(
                        headline = R.string.settings_progress_dir_main,
                        supportingText = R.string.settings_support_progress_dir,
                        checked = progressDirEnabled,
                        onCheckedChange = {
                            GlobalUtils.progressDir = it
                            progressDirEnabled = it
                        }
                    )
                    SettingsSectionDivider()
                    SettingsDetailSwitchItem(
                        headline = R.string.settings_excitement,
                        supportingText = R.string.settings_support_excitement,
                        checked = motivationalQuotesEnabled,
                        onCheckedChange = {
                            GlobalUtils.motivationalQuotes = it
                            motivationalQuotesEnabled = it
                        }
                    )
                    SettingsSectionDivider()
                    SettingsDetailSwitchItem(
                        headline = R.string.settings_fireworks,
                        supportingText = R.string.settings_support_fireworks,
                        checked = fireworksOnFinishEnabled,
                        onCheckedChange = {
                            GlobalUtils.fireworksOnFinish = it
                            fireworksOnFinishEnabled = it
                        }
                    )
                    SettingsSectionDivider()
                    SettingsDetailSwitchItem(
                        headline = R.string.settings_detail_display,
                        supportingText = R.string.settings_support_detail_display,
                        checked = detailDisplayEnabled,
                        onCheckedChange = {
                            GlobalUtils.detailDisplayMode = it
                            detailDisplayEnabled = it
                        }
                    )
                }

                Spacer(Modifier.navigationBarsPadding())
            }

            if (showWelcome) {
                AppearanceWelcomeOverlay(
                    onDismiss = {
                        GlobalUtils.appearanceRefactorIntroSeen = true
                        showWelcome = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DefaultBackButton(
    navigateUp: () -> Unit,
) {
    IconButton(
        onClick = navigateUp,
        modifier = navIconPaddingModifier
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = expressiveTypeModifier
        )
    }
}

@Composable
private fun AppearanceWelcomeOverlay(
    onDismiss: () -> Unit,
) {
    RegisterAdvancedMaterialDialogBlur()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_welcome_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.settings_welcome_points),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.i_know_and_continue))
            }
        }
    )
}
