package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.aritxonly.deadliner.model.AdvancedMaterialTuning
import com.aritxonly.deadliner.ui.base.Slider
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedMaterialSettingsScreen(
    navigateUp: () -> Unit,
) {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    var previewBlurRadius by remember {
        mutableFloatStateOf(appearance.advancedMaterialBlurRadius)
    }
    var previewFineTuning by remember {
        mutableStateOf(appearance.advancedMaterialFineTuning)
    }
    LaunchedEffect(appearance.advancedMaterialBlurRadius) {
        previewBlurRadius = appearance.advancedMaterialBlurRadius
    }
    LaunchedEffect(appearance.advancedMaterialFineTuning) {
        previewFineTuning = appearance.advancedMaterialFineTuning
    }
    val exportPreset = rememberAdvancedMaterialPresetExportAction(
        blurRadius = previewBlurRadius,
        fineTuning = previewFineTuning,
    )

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_advanced_material),
        allowAdvancedMaterialTopBarBlur = false,
        forceOverlayTopBar = true,
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
            SettingsSection(
                mainContent = true,
                enabled = appearance.useAdvancedMaterial,
            ) {
                SettingsSwitchItem(
                    label = R.string.settings_advanced_material,
                    checked = appearance.useAdvancedMaterial,
                    onCheckedChange = { enabled ->
                        GlobalUtils.updateAppearance { current ->
                            current.copy(useAdvancedMaterial = enabled)
                        }
                    },
                    mainSwitch = true
                )
            }

            AdvancedMaterialPreviewCard(
                blurRadius = previewBlurRadius,
                fineTuning = previewFineTuning,
                enabled = appearance.useAdvancedMaterial,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (appearance.useAdvancedMaterial) {
                SettingsSection(topLabel = stringResource(R.string.settings_advanced_material_blur_size)) {
                    AdvancedMaterialRadiusSlider(
                        value = previewBlurRadius,
                        saturationMultiplier = previewFineTuning.saturationMultiplier,
                        onValueChange = { previewBlurRadius = it },
                        onValueChangeFinished = {
                            GlobalUtils.updateAppearance { appearance ->
                                AdvancedMaterialTuning.withBlurRadius(
                                    appearance,
                                    previewBlurRadius,
                                )
                            }
                        },
                    )
                }

                if (AdvancedMaterialFineTuningAccess.isUnlocked) {
                    SettingsSection(
                        topLabel = stringResource(
                            R.string.settings_advanced_material_advanced_options,
                        ),
                    ) {
                        AdvancedMaterialFineTuningPanel(
                            value = previewFineTuning,
                            onValueChange = { previewFineTuning = it.normalized() },
                            onValueChangeFinished = {
                                GlobalUtils.updateAppearance { current ->
                                    current.copy(
                                        advancedMaterialFineTuning = previewFineTuning.normalized(),
                                    )
                                }
                            },
                            onImmediateChange = { updated ->
                                val normalized = updated.normalized()
                                previewFineTuning = normalized
                                GlobalUtils.updateAppearance { current ->
                                    current.copy(advancedMaterialFineTuning = normalized)
                                }
                            },
                            onExport = exportPreset,
                            onReset = {
                                previewBlurRadius = AdvancedMaterialTuning.DefaultBlurRadius
                                previewFineTuning = AdvancedMaterialFineTuning.Default
                                GlobalUtils.updateAppearance { current ->
                                    AdvancedMaterialTuning.withBlurRadius(
                                        appearance = current.copy(
                                            advancedMaterialFineTuning =
                                                AdvancedMaterialFineTuning.Default,
                                        ),
                                        value = AdvancedMaterialTuning.DefaultBlurRadius,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AdvancedMaterialRadiusSlider(
    value: Float,
    saturationMultiplier: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val saturation = AdvancedMaterialTuning.saturationForBlurRadius(value) * saturationMultiplier
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_advanced_material_blur_radius),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.settings_advanced_material_radius_saturation_value,
                    value.roundToInt(),
                    saturation,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = AdvancedMaterialTuning.MinBlurRadius..AdvancedMaterialTuning.MaxBlurRadius,
            steps = 0,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
