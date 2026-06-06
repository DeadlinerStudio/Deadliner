package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AdvancedMaterialLevel
import com.aritxonly.deadliner.ui.TintedGradientImage
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedMaterialSettingsScreen(
    navigateUp: () -> Unit,
) {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    val levelOptions = remember {
        listOf(
            RadioOption(AdvancedMaterialLevel.CrystalClear, R.string.settings_advanced_material_level_clear),
            RadioOption(AdvancedMaterialLevel.Light, R.string.settings_advanced_material_level_light),
            RadioOption(AdvancedMaterialLevel.Soft, R.string.settings_advanced_material_level_soft),
            RadioOption(AdvancedMaterialLevel.Hazy, R.string.settings_advanced_material_level_hazy),
            RadioOption(AdvancedMaterialLevel.Deep, R.string.settings_advanced_material_level_deep),
        )
    }

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
            AdvancedMaterialPreviewCard(
                level = appearance.advancedMaterialLevel,
                enabled = appearance.useAdvancedMaterial,
                modifier = Modifier.padding(vertical = 8.dp),
            )

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
                    SettingsRadioGroupItem(
                        options = levelOptions,
                        selectedKey = appearance.advancedMaterialLevel,
                        onOptionSelected = { level ->
                            GlobalUtils.advancedMaterialLevel = level
                        }
                    )
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun AdvancedMaterialPreviewCard(
    level: AdvancedMaterialLevel,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    SettingsHeroSection(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            TintedGradientImage(
                drawableId = R.drawable.dashboard_background,
                tintColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(),
            )

            val blurRadius = (level.blurRadius / 18f).coerceIn(3f, 18f).dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                TintedGradientImage(
                    drawableId = R.drawable.dashboard_background,
                    tintColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (enabled) blurRadius else 0.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = if (enabled) 0.52f else 0.82f
                            )
                        )
                )
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(levelTitleRes(level)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_advanced_material_level_summary,
                            level.blurRadius.toInt(),
                            level.blurSaturation
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun levelTitleRes(level: AdvancedMaterialLevel): Int = when (level) {
    AdvancedMaterialLevel.CrystalClear -> R.string.settings_advanced_material_level_clear
    AdvancedMaterialLevel.Light -> R.string.settings_advanced_material_level_light
    AdvancedMaterialLevel.Soft -> R.string.settings_advanced_material_level_soft
    AdvancedMaterialLevel.Hazy -> R.string.settings_advanced_material_level_hazy
    AdvancedMaterialLevel.Deep -> R.string.settings_advanced_material_level_deep
}
