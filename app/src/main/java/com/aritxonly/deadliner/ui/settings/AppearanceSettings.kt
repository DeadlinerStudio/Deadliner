package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import com.aritxonly.deadliner.ui.base.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    nav: SettingsNavigator,
    navigateUp: () -> Unit,
) {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
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
                                .fillMaxWidth()
                                .clickable { nav.navigate(route.route) },
                            headlineText = stringResource(route.titleRes),
                            supportingText = stringResource(route.supportRes!!),
                        )
                        if (index == 0) {
                            SettingsSectionDivider()
                        }
                    }
                }

                SettingsSection {
                    SettingItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(SettingsRoute.AppIcon.route) },
                        headlineText = stringResource(SettingsRoute.AppIcon.titleRes),
                        supportingText = stringResource(SettingsRoute.AppIcon.supportRes!!),
                    )
                }

                SettingsSection(topLabel = stringResource(R.string.settings_interface_display)) {
                    listOf(
                        SettingsRoute.UI,
                        SettingsRoute.DisplayScale,
                    ).forEachIndexed { index, route ->
                        SettingItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nav.navigate(route.route) },
                            headlineText = stringResource(route.titleRes),
                            supportingText = stringResource(route.supportRes!!),
                        )
                        if (index == 0) {
                            SettingsSectionDivider()
                        }
                    }
                }

                SettingsSection {
                    SettingsDetailSwitchItem(
                        headline = R.string.settings_semantic_glow,
                        supportingText = R.string.settings_support_semantic_glow,
                        checked = appearance.semanticGlowEnabled,
                        onCheckedChange = { enabled ->
                            GlobalUtils.updateAppearance { current ->
                                current.copy(semanticGlowEnabled = enabled)
                            }
                        }
                    )
                    SettingsSectionDivider()
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
        modifier = (navIconPaddingModifier).miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape)
    ) {
        Icon(
            painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
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
    AlertDialog(
        show = true,
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
