package com.aritxonly.deadliner.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.aritxonly.deadliner.model.MiuixModalMaterialTuning
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.base.MiuixDialog
import com.aritxonly.deadliner.ui.base.Slider
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import kotlin.math.roundToInt

@Composable
fun MiuixModalMaterialLab(
    value: AdvancedMaterialFineTuning,
    enabled: Boolean,
    onValueChangeFinished: (AdvancedMaterialFineTuning) -> Unit,
) {
    var draft by remember { mutableStateOf(value) }
    var showDialogPreview by remember { mutableStateOf(false) }
    var showSheetPreview by remember { mutableStateOf(false) }
    val advancedMaterial = LocalAdvancedMaterialSpec.current

    LaunchedEffect(value) {
        draft = value
    }
    LaunchedEffect(enabled) {
        if (!enabled) {
            showDialogPreview = false
            showSheetPreview = false
        }
    }

    CompositionLocalProvider(
        LocalAdvancedMaterialSpec provides advancedMaterial.copy(
            fineTuning = draft.normalized(),
        ),
    ) {
        SettingsSection(topLabel = stringResource(R.string.settings_miuix_modal_material_title)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        if (enabled) {
                            R.string.settings_miuix_modal_material_summary
                        } else {
                            R.string.settings_miuix_modal_material_disabled_summary
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )

                DialogMaterialControls(
                    value = draft,
                    enabled = enabled,
                    onValueChange = { draft = it },
                    onValueChangeFinished = { onValueChangeFinished(draft.normalized()) },
                )

                SettingsSectionDivider()

                SheetMaterialControls(
                    value = draft,
                    enabled = enabled,
                    onValueChange = { draft = it },
                    onValueChangeFinished = { onValueChangeFinished(draft.normalized()) },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { showDialogPreview = true },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settings_miuix_preview_dialog))
                    }
                    Button(
                        onClick = { showSheetPreview = true },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settings_miuix_preview_sheet))
                    }
                }

                TextButton(
                    onClick = {
                        val resetValue = draft.copy(
                            dialogBackgroundAlpha = MiuixModalMaterialTuning.DialogDefaultBackgroundAlpha,
                            dialogBlurRadiusDp = MiuixModalMaterialTuning.DialogDefaultBlurRadiusDp,
                            bottomSheetBackgroundAlpha =
                                MiuixModalMaterialTuning.BottomSheetDefaultBackgroundAlpha,
                            bottomSheetBlurRadiusDp =
                                MiuixModalMaterialTuning.BottomSheetDefaultBlurRadiusDp,
                        ).normalized()
                        draft = resetValue
                        onValueChangeFinished(resetValue)
                    },
                    enabled = enabled,
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.settings_miuix_modal_reset))
                }
            }
        }

        MiuixDialog(
            show = showDialogPreview,
            onDismissRequest = { showDialogPreview = false },
            title = stringResource(R.string.settings_miuix_preview_dialog_title),
            summary = stringResource(R.string.settings_miuix_preview_dialog_summary),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = stringResource(
                        R.string.settings_miuix_preview_value,
                        (draft.dialogBackgroundAlpha * 100f).roundToInt(),
                        draft.dialogBlurRadiusDp.roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                DialogMaterialControls(
                    value = draft,
                    enabled = true,
                    onValueChange = { draft = it },
                    onValueChangeFinished = { onValueChangeFinished(draft.normalized()) },
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showDialogPreview = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        }

        MiuixBottomSheet(
            show = showSheetPreview,
            title = stringResource(R.string.settings_miuix_preview_sheet_title),
            onDismissRequest = { showSheetPreview = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = stringResource(R.string.settings_miuix_preview_sheet_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.settings_miuix_preview_value,
                        (draft.bottomSheetBackgroundAlpha * 100f).roundToInt(),
                        draft.bottomSheetBlurRadiusDp.roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                SheetMaterialControls(
                    value = draft,
                    enabled = true,
                    onValueChange = { draft = it },
                    onValueChangeFinished = { onValueChangeFinished(draft.normalized()) },
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showSheetPreview = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun DialogMaterialControls(
    value: AdvancedMaterialFineTuning,
    enabled: Boolean,
    onValueChange: (AdvancedMaterialFineTuning) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    ModalMaterialSlider(
        label = R.string.settings_miuix_dialog_background_alpha,
        value = value.dialogBackgroundAlpha,
        valueRange = MiuixModalMaterialTuning.MinBackgroundAlpha..
            MiuixModalMaterialTuning.MaxBackgroundAlpha,
        valueText = "${(value.dialogBackgroundAlpha * 100f).roundToInt()}%",
        enabled = enabled,
        onValueChange = { onValueChange(value.copy(dialogBackgroundAlpha = it)) },
        onValueChangeFinished = onValueChangeFinished,
    )
    ModalMaterialSlider(
        label = R.string.settings_miuix_dialog_blur_radius,
        value = value.dialogBlurRadiusDp,
        valueRange = MiuixModalMaterialTuning.MinBlurRadiusDp..
            MiuixModalMaterialTuning.MaxBlurRadiusDp,
        valueText = "${value.dialogBlurRadiusDp.roundToInt()} dp",
        enabled = enabled,
        onValueChange = { onValueChange(value.copy(dialogBlurRadiusDp = it)) },
        onValueChangeFinished = onValueChangeFinished,
    )
}

@Composable
private fun SheetMaterialControls(
    value: AdvancedMaterialFineTuning,
    enabled: Boolean,
    onValueChange: (AdvancedMaterialFineTuning) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    ModalMaterialSlider(
        label = R.string.settings_miuix_sheet_background_alpha,
        value = value.bottomSheetBackgroundAlpha,
        valueRange = MiuixModalMaterialTuning.MinBackgroundAlpha..
            MiuixModalMaterialTuning.MaxBackgroundAlpha,
        valueText = "${(value.bottomSheetBackgroundAlpha * 100f).roundToInt()}%",
        enabled = enabled,
        onValueChange = { onValueChange(value.copy(bottomSheetBackgroundAlpha = it)) },
        onValueChangeFinished = onValueChangeFinished,
    )
    ModalMaterialSlider(
        label = R.string.settings_miuix_sheet_blur_radius,
        value = value.bottomSheetBlurRadiusDp,
        valueRange = MiuixModalMaterialTuning.MinBlurRadiusDp..
            MiuixModalMaterialTuning.MaxBlurRadiusDp,
        valueText = "${value.bottomSheetBlurRadiusDp.roundToInt()} dp",
        enabled = enabled,
        onValueChange = { onValueChange(value.copy(bottomSheetBlurRadiusDp = it)) },
        onValueChangeFinished = onValueChangeFinished,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ModalMaterialSlider(
    @StringRes label: Int,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
