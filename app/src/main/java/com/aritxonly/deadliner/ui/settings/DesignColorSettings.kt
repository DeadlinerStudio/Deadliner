package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.AppearanceDesignMode
import com.aritxonly.deadliner.model.ColorfulIndicatorStyle
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DynamicPaletteStyle
import com.aritxonly.deadliner.model.ModernColorPalette
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.base.Switch
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.aritxonly.deadliner.ui.theme.rememberTaskIndicatorColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignColorSettingsScreen(
    navigateUp: () -> Unit,
    onRequestDesignModeChange: (AppearanceDesignMode) -> Unit,
) {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    var selectedSeed by remember { mutableStateOf(GlobalUtils.seedColor) }
    var colorfulIndicatorEnabled by remember { mutableStateOf(GlobalUtils.presetIndicatorColor) }
    var colorfulIndicatorStyle by remember { mutableStateOf(GlobalUtils.colorfulIndicatorStyle) }
    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_design_color_title),
        collapsible = false,
        topBarStyle = TopAppBarStyle.SMALL,
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
            DesignColorPreviewCard(
                designMode = appearance.designMode,
                colorfulIndicator = colorfulIndicatorEnabled,
                colorfulIndicatorStyle = colorfulIndicatorStyle,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            SettingsSection(topLabel = stringResource(R.string.settings_design)) {
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

            SettingsSection(topLabel = stringResource(R.string.settings_design_color_title)) {
                ThemeColorPicker(
                    currentSeed = selectedSeed,
                    onColorSelected = {
                        GlobalUtils.seedColor = it
                        selectedSeed = it
                    }
                )
            }

            SettingsSection(topLabel = stringResource(R.string.settings_design_mode_section_title, stringResource(modeTitleRes(appearance.designMode)))) {
                SettingsDetailSwitchItem(
                    headline = R.string.settings_hide_divider,
                    supportingText = R.string.settings_support_hide_divider,
                    checked = appearance.activeHideDivider,
                    onCheckedChange = { checked ->
                        GlobalUtils.updateAppearance { current ->
                            when (current.designMode) {
                                AppearanceDesignMode.Modern -> current.copy(modernHideDivider = checked)
                                AppearanceDesignMode.Vivid -> current.copy(vividHideDivider = checked)
                            }
                        }
                    }
                )
                SettingsSectionDivider()
                SettingsDetailSwitchItem(
                    headline = R.string.settings_colored_settings_home_icons,
                    supportingText = R.string.settings_support_colored_settings_home_icons,
                    checked = appearance.useSettingsHomepageColoredIcons,
                    onCheckedChange = { checked ->
                        GlobalUtils.settingsHomepageColoredIcons = checked
                    }
                )
                SettingsSectionDivider()
                if (appearance.designMode == AppearanceDesignMode.Modern) {
                    val devicePalette = GlobalUtils.getCurrentDeviceColorPalette()
                    SettingsDetailSwitchItem(
                        headline = R.string.settings_background_strategy_title,
                        supportingRawText = if (appearance.modernUseDevicePaletteStrategy) {
                            "${stringResource(R.string.settings_background_strategy_value)} (${devicePalette.label()})"
                        } else {
                            stringResource(R.string.settings_modern_palette_support)
                        },
                        checked = appearance.modernUseDevicePaletteStrategy,
                        onCheckedChange = { checked ->
                            GlobalUtils.modernUseDevicePaletteStrategy = checked
                        }
                    )
                    if (!appearance.modernUseDevicePaletteStrategy) {
                        SettingsSectionDivider()
                        ModernColorPalettePicker(
                            selectedPalette = appearance.modernColorPalette,
                            onSelect = { palette ->
                                GlobalUtils.modernColorPalette = palette
                            },
                        )
                        SettingsSectionDivider(onContainer = false)
                    } else {
                        SettingsSectionDivider()
                    }
                } else {
                    SettingsSectionDivider()
                }
                StaticSettingStrategyRow(
                    headline = stringResource(R.string.settings_color_generation_strategy_title),
                    supporting = "${stringResource(R.string.settings_color_generation_strategy_value)} · ${stringResource(appearance.dynamicPaletteStyle.labelRes())}"
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
                        },
                    )
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun ColorfulIndicatorStylePicker(
    selectedKey: ColorfulIndicatorStyle,
    onStyleSelected: (ColorfulIndicatorStyle) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedKey) {
        val selectedIndex = ColorfulIndicatorStyle.entries.indexOf(selectedKey)
        if (selectedIndex >= 0) {
            listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
        }
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        state = listState,
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(ColorfulIndicatorStyle.entries, key = { it.key }) { style ->
            val selected = style == selectedKey
            Card(
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                ),
                modifier = Modifier
                    .clickable { onStyleSelected(style) }
                    .widthIn(min = 160.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(style.labelRes()),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DesignColorPreviewCard(
    designMode: AppearanceDesignMode,
    colorfulIndicator: Boolean,
    colorfulIndicatorStyle: ColorfulIndicatorStyle,
    simpleColorPreview: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var previewSwitchChecked by remember(designMode) {
        mutableStateOf(designMode == AppearanceDesignMode.Modern)
    }
    val designBadgeColor = if (designMode == AppearanceDesignMode.Modern) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
    }

    SettingsHeroSection(modifier = modifier) {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(modeTitleRes(designMode)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_design_preview_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color = designBadgeColor,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_design_preview_chip),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                }
            }

            ColorRoleShowcaseRow()

            if (!simpleColorPreview) {
                CompactTaskPreview(
                    status = if (colorfulIndicator) DDLStatus.NEAR else DDLStatus.UNDERGO,
                    colorfulIndicator = colorfulIndicator,
                    colorfulIndicatorStyle = colorfulIndicatorStyle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            IndicatorPaletteShowcase(
                colorfulIndicator = colorfulIndicator,
                colorfulIndicatorStyle = colorfulIndicatorStyle,
                modifier = Modifier.fillMaxWidth()
            )

            if (!simpleColorPreview) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_design_preview_switch_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Switch(
                                checked = previewSwitchChecked,
                                onCheckedChange = { previewSwitchChecked = it }
                            )
                        }
                    }
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(0.72f)
                    ) {
                        Text(text = stringResource(R.string.settings_design_preview_button_label))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTaskPreview(
    status: DDLStatus,
    colorfulIndicator: Boolean,
    colorfulIndicatorStyle: ColorfulIndicatorStyle,
    modifier: Modifier = Modifier,
) {
    val previewColors = indicatorPreviewColors(
        status = status,
        colorfulIndicator = colorfulIndicator,
        colorfulIndicatorStyle = colorfulIndicatorStyle,
    )
    val indicatorColor = previewColors.indicatorColor
    val progress = 0.58f

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(previewColors.baseBackgroundColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(previewColors.overlayBackgroundColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                indicatorColor.copy(alpha = indicatorColor.alpha * 0.45f),
                                indicatorColor
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .fillMaxWidth(0.18f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.66f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f))
                )
            }
        }
    }
}

@Composable
private fun ColorRoleShowcaseRow() {
    androidx.compose.foundation.layout.Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.settings_design_preview_role_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ColorRoleCard(
                label = stringResource(R.string.settings_design_preview_primary),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            ColorRoleCard(
                label = stringResource(R.string.settings_design_preview_secondary),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            ColorRoleCard(
                label = stringResource(R.string.settings_design_preview_tertiary),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ColorRoleCard(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun IndicatorPaletteShowcase(
    colorfulIndicator: Boolean,
    colorfulIndicatorStyle: ColorfulIndicatorStyle,
    modifier: Modifier = Modifier,
) {
    val previewItems = listOf(
        IndicatorPreviewItem(
            status = DDLStatus.UNDERGO,
            labelRes = R.string.settings_design_preview_status_undergo,
        ),
        IndicatorPreviewItem(
            status = DDLStatus.NEAR,
            labelRes = R.string.settings_design_preview_status_near,
        ),
        IndicatorPreviewItem(
            status = DDLStatus.PASSED,
            labelRes = R.string.settings_design_preview_status_passed,
        ),
        IndicatorPreviewItem(
            status = DDLStatus.COMPLETED,
            labelRes = R.string.settings_design_preview_status_completed,
        ),
        IndicatorPreviewItem(
            status = DDLStatus.COMPLETED,
            labelRes = R.string.settings_design_preview_status_abandoned,
            useDisabledCompletedStyle = true,
        ),
    )

    androidx.compose.foundation.layout.Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.settings_design_preview_indicator_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(previewItems, key = { "${it.status.name}_${it.useDisabledCompletedStyle}_${it.labelRes}" }) { item ->
                IndicatorStatusCard(
                    status = item.status,
                    labelRes = item.labelRes,
                    colorfulIndicator = colorfulIndicator,
                    colorfulIndicatorStyle = colorfulIndicatorStyle,
                    useDisabledCompletedStyle = item.useDisabledCompletedStyle,
                )
            }
        }
    }
}

private data class IndicatorPreviewItem(
    val status: DDLStatus,
    val labelRes: Int,
    val useDisabledCompletedStyle: Boolean = false,
)

@Composable
private fun IndicatorStatusCard(
    status: DDLStatus,
    labelRes: Int,
    colorfulIndicator: Boolean,
    colorfulIndicatorStyle: ColorfulIndicatorStyle,
    useDisabledCompletedStyle: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val previewColors = indicatorPreviewColors(
        status = status,
        colorfulIndicator = colorfulIndicator,
        colorfulIndicatorStyle = colorfulIndicatorStyle,
        useDisabledCompletedStyle = useDisabledCompletedStyle,
    )
    val indicatorColor = previewColors.indicatorColor
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(previewColors.baseBackgroundColor)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(previewColors.overlayBackgroundColor)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun indicatorPreviewColors(
    status: DDLStatus,
    colorfulIndicator: Boolean,
    colorfulIndicatorStyle: ColorfulIndicatorStyle,
    useDisabledCompletedStyle: Boolean = false,
): com.aritxonly.deadliner.ui.theme.TaskIndicatorColors {
    val colors = rememberTaskIndicatorColors(
        status = status,
        colorfulIndicatorEnabled = colorfulIndicator,
        colorfulIndicatorStyle = colorfulIndicatorStyle,
        useDisabledCompletedStyle = useDisabledCompletedStyle,
    )
    return colors
}

@Composable
private fun StaticSettingStrategyRow(
    headline: String,
    supporting: String,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModernColorPalettePicker(
    selectedPalette: ModernColorPalette,
    onSelect: (ModernColorPalette) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedPalette) {
        val selectedIndex = ModernColorPalette.entries.indexOf(selectedPalette)
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            state = listState,
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(ModernColorPalette.entries, key = { it.key }) { palette ->
                ModernColorPaletteCard(
                    palette = palette,
                    selected = palette == selectedPalette,
                    onClick = { onSelect(palette) },
                )
            }
        }
    }
}

@Composable
private fun ModernColorPaletteCard(
    palette: ModernColorPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val preview = palette.previewLight
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(min = 140.dp)
                .padding(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaletteDot(preview.surfaceHex, Modifier.weight(1f))
                PaletteDot(preview.surfaceContainerHex, Modifier.weight(1f))
                PaletteDot(preview.searchBarHex, Modifier.weight(1f))
                PaletteDot(preview.textSecondaryHex, Modifier.weight(1f))
            }
            Text(
                text = palette.label(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = palette.summary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PaletteDot(
    hex: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color(if (hex.startsWith("#")) hex.toColorInt() else "#$hex".toColorInt()))
    )
}

@Composable
private fun ModernColorPalette.label(): String = when (this) {
    ModernColorPalette.HyperOs -> stringResource(R.string.settings_modern_palette_hyperos)
    ModernColorPalette.Honor -> stringResource(R.string.settings_modern_palette_honor)
    ModernColorPalette.Oppo -> stringResource(R.string.settings_modern_palette_oppo)
    ModernColorPalette.Vivo -> stringResource(R.string.settings_modern_palette_vivo)
}

@Composable
private fun ModernColorPalette.summary(): String = when (this) {
    ModernColorPalette.HyperOs -> stringResource(R.string.settings_modern_palette_hyperos_support)
    ModernColorPalette.Honor -> stringResource(R.string.settings_modern_palette_honor_support)
    ModernColorPalette.Oppo -> stringResource(R.string.settings_modern_palette_oppo_support)
    ModernColorPalette.Vivo -> stringResource(R.string.settings_modern_palette_vivo_support)
}

@Composable
fun DynamicPaletteStylePicker(
    selectedStyle: DynamicPaletteStyle,
    onStyleSelected: (DynamicPaletteStyle) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedStyle) {
        val selectedIndex = DynamicPaletteStyle.entries.indexOf(selectedStyle)
        if (selectedIndex >= 0) {
            listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
        }
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        state = listState,
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(DynamicPaletteStyle.entries, key = { it.key }) { style ->
            val selected = style == selectedStyle
            Card(
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                ),
                modifier = Modifier
                    .clickable { onStyleSelected(style) }
                    .widthIn(min = 124.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(style.labelRes()),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

private fun DynamicPaletteStyle.labelRes(): Int = when (this) {
    DynamicPaletteStyle.System -> R.string.settings_palette_style_system
    DynamicPaletteStyle.TonalSpot -> R.string.settings_palette_style_tonal_spot
    DynamicPaletteStyle.Neutral -> R.string.settings_palette_style_neutral
    DynamicPaletteStyle.Vibrant -> R.string.settings_palette_style_vibrant
    DynamicPaletteStyle.Expressive -> R.string.settings_palette_style_expressive
    DynamicPaletteStyle.Rainbow -> R.string.settings_palette_style_rainbow
    DynamicPaletteStyle.FruitSalad -> R.string.settings_palette_style_fruit_salad
    DynamicPaletteStyle.Monochrome -> R.string.settings_palette_style_monochrome
    DynamicPaletteStyle.Fidelity -> R.string.settings_palette_style_fidelity
    DynamicPaletteStyle.Content -> R.string.settings_palette_style_content
}

private fun ColorfulIndicatorStyle.labelRes(): Int = when (this) {
    ColorfulIndicatorStyle.Harmonize -> R.string.settings_indicator_style_harmonize
    ColorfulIndicatorStyle.Morandi -> R.string.settings_indicator_style_morandi
}

private fun modeTitleRes(mode: AppearanceDesignMode): Int = when (mode) {
    AppearanceDesignMode.Modern -> R.string.settings_design_mode_modern
    AppearanceDesignMode.Vivid -> R.string.settings_design_mode_vivid
}
