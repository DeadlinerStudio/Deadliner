package com.aritxonly.deadliner.ui.intro

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AdvancedMaterialLevel
import com.aritxonly.deadliner.model.AppearanceDesignMode
import com.aritxonly.deadliner.model.DynamicPaletteStyle
import com.aritxonly.deadliner.model.UiStyle
import com.aritxonly.deadliner.ui.settings.AdvancedMaterialPreviewCard
import com.aritxonly.deadliner.ui.settings.ColorfulIndicatorStylePicker
import com.aritxonly.deadliner.ui.settings.DesignColorPreviewCard
import com.aritxonly.deadliner.ui.settings.DynamicPaletteStylePicker
import com.aritxonly.deadliner.ui.settings.RadioOption
import com.aritxonly.deadliner.ui.settings.SettingsDetailSwitchItem
import com.aritxonly.deadliner.ui.settings.SettingsRadioGroupItem
import com.aritxonly.deadliner.ui.settings.SettingsSection
import com.aritxonly.deadliner.ui.settings.SettingsSectionDivider
import com.aritxonly.deadliner.ui.settings.SettingsSwitchItem
import com.aritxonly.deadliner.ui.settings.ThemeColorPicker
import com.aritxonly.deadliner.ui.settings.UiModeSelectionRow

@Composable
fun DesignIntroScreen(
    onRequestDesignModeChange: (AppearanceDesignMode) -> Unit,
) {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()

    IntroPageColumn(
        pageKey = "intro_design_page",
        title = R.string.intro_design_color_title,
        description = R.string.intro_design_color_description,
        contentSpacing = 8.dp,
        horizontalPadding = 16.dp,
        verticalPadding = 16.dp,
        bottomSpacer = 8.dp,
    ) {
        DesignColorPreviewCard(
            designMode = appearance.designMode,
            colorfulIndicator = GlobalUtils.presetIndicatorColor,
            colorfulIndicatorStyle = GlobalUtils.colorfulIndicatorStyle,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
        )

        SettingsSection(
            topLabel = stringResource(R.string.settings_design),
            modifier = Modifier.padding(vertical = 0.dp),
        ) {
            SettingsRadioGroupItem(
                options = listOf(
                    RadioOption(AppearanceDesignMode.Modern, R.string.settings_design_mode_modern),
                    RadioOption(AppearanceDesignMode.Vivid, R.string.settings_design_mode_vivid),
                ),
                selectedKey = appearance.designMode,
                onOptionSelected = { mode ->
                    if (mode != appearance.designMode) {
                        onRequestDesignModeChange(mode)
                    }
                }
            )
        }
    }
}

@Composable
fun ColorIntroScreen() {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    var selectedSeed by remember { mutableStateOf(GlobalUtils.seedColor) }
    var colorfulIndicatorEnabled by remember { mutableStateOf(GlobalUtils.presetIndicatorColor) }
    var colorfulIndicatorStyle by remember { mutableStateOf(GlobalUtils.colorfulIndicatorStyle) }

    IntroPageColumn(
        pageKey = "intro_color_page",
        title = R.string.intro_color_title,
        description = R.string.intro_color_description,
        contentSpacing = 8.dp,
        horizontalPadding = 16.dp,
        verticalPadding = 16.dp,
        bottomSpacer = 8.dp,
    ) {
        DesignColorPreviewCard(
            designMode = appearance.designMode,
            colorfulIndicator = colorfulIndicatorEnabled,
            colorfulIndicatorStyle = colorfulIndicatorStyle,
            simpleColorPreview = true,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
        )

        SettingsSection(
            topLabel = stringResource(R.string.settings_design_color_title),
            modifier = Modifier.padding(vertical = 0.dp),
        ) {
            ThemeColorPicker(
                currentSeed = selectedSeed,
                onColorSelected = {
                    GlobalUtils.seedColor = it
                    selectedSeed = it
                }
            )
            SettingsSectionDivider()
            DynamicPaletteStylePicker(
                selectedStyle = appearance.dynamicPaletteStyle,
                onStyleSelected = { style ->
                    if (style == DynamicPaletteStyle.System) {
                        GlobalUtils.seedColor = null
                        selectedSeed = null
                    }
                    GlobalUtils.dynamicPaletteStyle = style
                },
            )
            SettingsSectionDivider()
            SettingsDetailSwitchItem(
                headline = R.string.settings_preset_indicator,
                supportingText = R.string.settings_support_preset_indicator,
                checked = colorfulIndicatorEnabled,
                onCheckedChange = {
                    GlobalUtils.presetIndicatorColor = it
                    colorfulIndicatorEnabled = it
                }
            )
            if (colorfulIndicatorEnabled) {
                SettingsSectionDivider()
                ColorfulIndicatorStylePicker(
                    selectedKey = colorfulIndicatorStyle,
                    onStyleSelected = { style ->
                        GlobalUtils.colorfulIndicatorStyle = style
                        colorfulIndicatorStyle = style
                    }
                )
            }
        }
    }
}

@Composable
fun AdvancedMaterialIntroScreen() {
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

    IntroPageColumn(
        pageKey = "intro_advanced_material_page",
        title = R.string.intro_advanced_material_title,
        description = R.string.intro_advanced_material_description,
        contentSpacing = 8.dp,
        horizontalPadding = 16.dp,
        verticalPadding = 16.dp,
        bottomSpacer = 8.dp,
    ) {
        AdvancedMaterialPreviewCard(
            level = appearance.advancedMaterialLevel,
            enabled = appearance.useAdvancedMaterial,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
        )

        SettingsSection(
            topLabel = stringResource(R.string.settings_advanced_material),
            modifier = Modifier.padding(vertical = 0.dp),
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
            SettingsSection(
                topLabel = stringResource(R.string.settings_advanced_material_blur_size),
                modifier = Modifier.padding(vertical = 0.dp),
            ) {
                SettingsRadioGroupItem(
                    options = levelOptions,
                    selectedKey = appearance.advancedMaterialLevel,
                    onOptionSelected = { level ->
                        GlobalUtils.advancedMaterialLevel = level
                    }
                )
            }
        }
    }
}

@Composable
fun UiModeScreen() {
    val currentStyle by GlobalUtils.styleFlow.collectAsState()

    IntroPageColumn(
        pageKey = "intro_ui_style_page",
        title = R.string.intro_ui_style_title,
        description = if (currentStyle == UiStyle.Classic) {
            R.string.intro_ui_style_description_classic
        } else {
            R.string.intro_ui_style_description
        },
        contentSpacing = 12.dp,
        horizontalPadding = 16.dp,
        verticalPadding = 16.dp,
        bottomSpacer = 8.dp,
    ) {
        if (currentStyle == UiStyle.Classic) {
            ClassicFarewellCard()
        }

        SettingsSection(
            topLabel = stringResource(R.string.settings_ui_mode_title),
            modifier = Modifier.padding(vertical = 0.dp),
        ) {
            UiModeSelectionRow(
                currentStyle = currentStyle.key,
                onStyleChange = {
                    GlobalUtils.setStyle(UiStyle.fromKey(it))
                },
                invertColorFilter = null,
                inIntroPage = true,
            )
        }
    }
}

@Composable
private fun IntroPageColumn(
    pageKey: String,
    @StringRes title: Int,
    @StringRes description: Int? = null,
    contentSpacing: androidx.compose.ui.unit.Dp = 20.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 24.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    bottomSpacer: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberSaveable(pageKey, saver = ScrollState.Saver) {
        ScrollState(initial = 0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(contentSpacing),
        content = {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            description?.let { descriptionRes ->
                Text(
                    text = stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
            Spacer(modifier = Modifier.height(bottomSpacer))
        }
    )
}

@Composable
private fun ClassicFarewellCard() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.intro_classic_farewell_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.intro_classic_farewell_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.86f)
            )
        }
    }
}
