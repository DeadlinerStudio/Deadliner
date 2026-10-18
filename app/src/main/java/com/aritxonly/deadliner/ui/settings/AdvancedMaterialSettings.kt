package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.aritxonly.deadliner.model.AdvancedMaterialTuning
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

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

            if (appearance.useAdvancedMaterial) {
                SettingsSection(topLabel = stringResource(R.string.settings_advanced_material_blur_size)) {
                    AdvancedMaterialRadiusSlider(
                        value = previewBlurRadius,
                        enabled = !appearance.useOfficialMiuixGlass,
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

                SettingsSection {
                    SettingsDropdownItem(
                        title = stringResource(R.string.settings_material_pipeline),
                        options = listOf(
                            RadioOption(false, R.string.settings_material_pipeline_deadliner),
                            RadioOption(true, R.string.settings_material_pipeline_miuix),
                        ),
                        selectedKey = appearance.useOfficialMiuixGlass,
                        onOptionSelected = { official ->
                            GlobalUtils.updateAppearance { it.copy(useOfficialMiuixGlass = official) }
                        },
                    )
                }
            }

            AdvancedMaterialPreviewCard(
                blurRadius = previewBlurRadius,
                fineTuning = previewFineTuning,
                enabled = appearance.useAdvancedMaterial,
                useOfficialMiuixGlass = appearance.useOfficialMiuixGlass,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (appearance.useAdvancedMaterial && !appearance.useOfficialMiuixGlass) {
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
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = SettingsItemDefaults.HorizontalPadding, vertical = 12.dp),
    ) {
        Text(stringResource(R.string.settings_advanced_material_blur_size),
            style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = AdvancedMaterialTuning.MinBlurRadius..AdvancedMaterialTuning.MaxBlurRadius,
            showKeyPoints = true,
            keyPoints = listOf(AdvancedMaterialTuning.DefaultBlurRadius),
            modifier = Modifier.fillMaxWidth(),
        )
        if (enabled) BlurDefaultKeyPointMarker()
        if (!enabled) Text(stringResource(R.string.settings_material_miuix_preset_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp))
    }
}

/** The native track's point is covered by the thumb at default; keep its position visible below. */
@Composable
private fun BlurDefaultKeyPointMarker() {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxWidth().height(7.dp)) {
        val fraction = (AdvancedMaterialTuning.DefaultBlurRadius - AdvancedMaterialTuning.MinBlurRadius) /
            (AdvancedMaterialTuning.MaxBlurRadius - AdvancedMaterialTuning.MinBlurRadius)
        val inset = SliderDefaults.MinHeight.toPx() / 2f
        val x = inset + (if (rtl) 1f - fraction else fraction) * (size.width - inset * 2f).coerceAtLeast(0f)
        drawLine(color, Offset(x, 2.dp.toPx()), Offset(x, 5.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    }
}
