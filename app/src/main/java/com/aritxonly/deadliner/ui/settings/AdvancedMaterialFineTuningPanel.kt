package com.aritxonly.deadliner.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.aritxonly.deadliner.ui.base.Slider
import com.aritxonly.deadliner.ui.base.Switch
import kotlin.math.roundToInt

@Composable
fun AdvancedMaterialFineTuningPanel(
    value: AdvancedMaterialFineTuning,
    onValueChange: (AdvancedMaterialFineTuning) -> Unit,
    onValueChangeFinished: () -> Unit,
    onImmediateChange: (AdvancedMaterialFineTuning) -> Unit,
    onExport: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_advanced_material_advanced_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        )

        FineTuningSectionHeader(R.string.settings_advanced_material_base_parameters)
        FineTuningSlider(
            label = R.string.settings_advanced_material_saturation_multiplier,
            value = value.saturationMultiplier,
            range = 0.75f..1.25f,
            format = FineTuningValueFormat.Multiplier,
            onValueChange = { onValueChange(value.copy(saturationMultiplier = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_noise_coefficient,
            value = value.noiseCoefficient,
            range = 0f..0.20f,
            onValueChange = { onValueChange(value.copy(noiseCoefficient = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_brightness,
            value = value.brightness,
            range = -0.20f..0.20f,
            onValueChange = { onValueChange(value.copy(brightness = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_contrast,
            value = value.contrast,
            range = 0.75f..1.25f,
            onValueChange = { onValueChange(value.copy(contrast = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_top_tint_alpha,
            value = value.topBarTintAlpha,
            range = 0.20f..1f,
            onValueChange = { onValueChange(value.copy(topBarTintAlpha = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_navigation_tint_alpha,
            value = value.navigationTintAlpha,
            range = 0.20f..1f,
            onValueChange = { onValueChange(value.copy(navigationTintAlpha = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )

        FineTuningSectionHeader(R.string.settings_advanced_material_soft_glass_parameters)
        FineTuningSlider(
            label = R.string.settings_advanced_material_glass_blur_multiplier,
            value = value.glassBlurRadiusMultiplier,
            range = 0.10f..1f,
            format = FineTuningValueFormat.Multiplier,
            onValueChange = { onValueChange(value.copy(glassBlurRadiusMultiplier = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_glass_noise_multiplier,
            value = value.glassNoiseMultiplier,
            range = 0f..2f,
            format = FineTuningValueFormat.Multiplier,
            onValueChange = { onValueChange(value.copy(glassNoiseMultiplier = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_glass_tint_multiplier,
            value = value.glassTintAlphaMultiplier,
            range = 0.50f..1.50f,
            format = FineTuningValueFormat.Multiplier,
            onValueChange = { onValueChange(value.copy(glassTintAlphaMultiplier = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_glass_tint_light_gray,
            value = value.glassTintLightGray,
            range = 0f..1f,
            format = FineTuningValueFormat.Gray,
            onValueChange = { onValueChange(value.copy(glassTintLightGray = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_glass_tint_dark_gray,
            value = value.glassTintDarkGray,
            range = 0f..1f,
            format = FineTuningValueFormat.Gray,
            onValueChange = { onValueChange(value.copy(glassTintDarkGray = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSwitch(
            label = R.string.settings_advanced_material_refraction_enabled,
            checked = value.refractionEnabled,
            onCheckedChange = { onImmediateChange(value.copy(refractionEnabled = it)) },
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_refraction_height,
            value = value.refractionHeightDp,
            range = 0f..32f,
            format = FineTuningValueFormat.Dp,
            enabled = value.refractionEnabled,
            onValueChange = { onValueChange(value.copy(refractionHeightDp = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_refraction_amount,
            value = value.refractionAmountDp,
            range = 0f..32f,
            format = FineTuningValueFormat.Dp,
            enabled = value.refractionEnabled,
            onValueChange = { onValueChange(value.copy(refractionAmountDp = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_refraction_depth,
            value = value.refractionDepthEffect,
            range = 0f..0.60f,
            enabled = value.refractionEnabled,
            onValueChange = { onValueChange(value.copy(refractionDepthEffect = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_chromatic_aberration,
            value = value.chromaticAberrationDp,
            range = 0f..4f,
            format = FineTuningValueFormat.Dp,
            enabled = value.refractionEnabled,
            onValueChange = { onValueChange(value.copy(chromaticAberrationDp = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_edge_highlight,
            value = value.edgeHighlightAlpha,
            range = 0f..1f,
            onValueChange = { onValueChange(value.copy(edgeHighlightAlpha = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_edge_highlight_gray,
            value = value.edgeHighlightGray,
            range = 0f..1f,
            format = FineTuningValueFormat.Gray,
            onValueChange = { onValueChange(value.copy(edgeHighlightGray = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_dark_highlight_multiplier,
            value = value.darkHighlightMultiplier,
            range = 0.10f..2f,
            format = FineTuningValueFormat.Multiplier,
            onValueChange = { onValueChange(value.copy(darkHighlightMultiplier = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_fallback_edge_width,
            value = value.fallbackEdgeWidthDp,
            range = 0f..2f,
            format = FineTuningValueFormat.Dp,
            onValueChange = { onValueChange(value.copy(fallbackEdgeWidthDp = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_fallback_light_alpha,
            value = value.fallbackLightAlpha,
            range = 0f..1f,
            onValueChange = { onValueChange(value.copy(fallbackLightAlpha = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_fallback_dark_alpha,
            value = value.fallbackDarkAlpha,
            range = 0f..1f,
            onValueChange = { onValueChange(value.copy(fallbackDarkAlpha = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )
        FineTuningSwitch(
            label = R.string.settings_advanced_material_post_blur_enabled,
            checked = value.postRefractionBlurEnabled,
            onCheckedChange = {
                onImmediateChange(value.copy(postRefractionBlurEnabled = it))
            },
        )
        FineTuningSlider(
            label = R.string.settings_advanced_material_post_blur_radius,
            value = value.postRefractionBlurRadiusDp,
            range = 0f..2f,
            format = FineTuningValueFormat.Dp,
            enabled = value.postRefractionBlurEnabled,
            onValueChange = { onValueChange(value.copy(postRefractionBlurRadiusDp = it)) },
            onValueChangeFinished = onValueChangeFinished,
        )

        Row(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            TextButton(onClick = onExport) {
                Text(stringResource(R.string.settings_advanced_material_export_preset))
            }
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.settings_advanced_material_reset_parameters))
            }
        }
    }
}

@Composable
private fun FineTuningSectionHeader(@StringRes label: Int) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FineTuningSlider(
    @StringRes label: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: FineTuningValueFormat = FineTuningValueFormat.Decimal,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = when (format) {
                    FineTuningValueFormat.Gray -> stringResource(
                        format.stringResource,
                        (value * 255f).roundToInt(),
                    )

                    else -> stringResource(format.stringResource, value)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f),
            )
        }
        Spacer(Modifier.height(2.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FineTuningSwitch(
    @StringRes label: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private enum class FineTuningValueFormat(@param:StringRes val stringResource: Int) {
    Decimal(R.string.settings_advanced_material_value_decimal),
    Multiplier(R.string.settings_advanced_material_value_multiplier),
    Dp(R.string.settings_advanced_material_value_dp),
    Gray(R.string.settings_advanced_material_value_gray),
}
