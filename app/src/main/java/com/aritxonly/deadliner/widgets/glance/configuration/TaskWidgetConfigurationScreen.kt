package com.aritxonly.deadliner.widgets.glance.configuration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.Slider
import com.aritxonly.deadliner.ui.settings.CollapsingTopBarScaffold
import com.aritxonly.deadliner.ui.settings.RadioOptionText
import com.aritxonly.deadliner.ui.settings.SettingsRadioGroupItem
import com.aritxonly.deadliner.ui.settings.SettingsScrollColumn
import com.aritxonly.deadliner.ui.settings.SettingsSection
import com.aritxonly.deadliner.ui.settings.SettingsSwitchItem
import com.aritxonly.deadliner.ui.symbolPainterResource
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.editor.DeadlineBottomActions
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetColorScheme
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfiguration
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetConfigurationLimits
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetThemeMode
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetVisualState
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetThemeResolver
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetKind
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetPaddingPreset
import com.aritxonly.deadliner.widgets.glance.theme.TaskWidgetColorSchemePalette
import kotlin.math.roundToInt

@Composable
internal fun TaskWidgetConfigurationScreen(
    kind: TaskWidgetKind,
    initialConfiguration: TaskWidgetConfiguration,
    onCancel: () -> Unit,
    onSave: (TaskWidgetConfiguration) -> Unit,
) {
    val limits = remember(kind) { TaskWidgetConfigurationLimits.forKind(kind) }
    var configuration by remember(kind, initialConfiguration) {
        mutableStateOf(initialConfiguration.sanitized(kind))
    }
    val title = stringResource(
        if (kind == TaskWidgetKind.SQUARE) {
            R.string.widget_configuration_square_title
        } else {
            R.string.widget_configuration_capsule_title
        }
    )

    CollapsingTopBarScaffold(
        title = title,
        collapsible = false,
        topBarStyle = TopAppBarStyle.CENTER,
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(
                    painter = symbolPainterResource(R.drawable.msr_close_wght400),
                    contentDescription = stringResource(R.string.cancel),
                )
            }
        },
        bottomBar = {
            DeadlineBottomActions(
                showSaveToCalendar = false,
                onSave = { onSave(configuration.sanitized(kind)) },
                onSaveToCalendar = {},
            )
        },
    ) { innerPadding ->
        SettingsScrollColumn(contentPadding = innerPadding) {
            TaskWidgetConfigurationPreview(kind, configuration)

            SettingsSection(topLabel = stringResource(R.string.widget_configuration_theme_mode)) {
                SettingsRadioGroupItem(
                    options = listOf(
                        RadioOptionText(TaskWidgetThemeMode.FOLLOW_APP, stringResource(R.string.widget_theme_follow_app)),
                        RadioOptionText(TaskWidgetThemeMode.LIGHT, stringResource(R.string.widget_theme_light)),
                        RadioOptionText(TaskWidgetThemeMode.DARK, stringResource(R.string.widget_theme_dark)),
                    ),
                    selectedKey = configuration.themeMode,
                    onOptionSelected = { configuration = configuration.copy(themeMode = it) },
                )
            }

            SettingsSection(topLabel = stringResource(R.string.widget_configuration_color)) {
                TaskWidgetColorSchemeSelector(
                    selected = configuration.colorScheme,
                    themeMode = configuration.themeMode,
                    onSelected = { configuration = configuration.copy(colorScheme = it) },
                )
            }

            SettingsSection(topLabel = stringResource(R.string.widget_configuration_background_transparency)) {
                ConfigurationSliderRow(
                    label = stringResource(R.string.widget_configuration_background_transparency),
                    value = configuration.backgroundTransparencyPercent,
                    range = 0..100,
                    valueText = "${configuration.backgroundTransparencyPercent}%",
                    onValueChange = {
                        configuration = configuration.copy(backgroundTransparencyPercent = it).sanitized(kind)
                    },
                )
                Text(
                    text = stringResource(R.string.widget_configuration_background_transparency_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                )
            }

            SettingsSection(topLabel = stringResource(R.string.widget_configuration_corner)) {
                ConfigurationSliderRow(
                    label = stringResource(R.string.widget_configuration_corner_value),
                    value = configuration.cornerRadiusDp,
                    range = limits.minCornerRadiusDp..limits.maxCornerRadiusDp,
                    onValueChange = {
                        configuration = configuration.copy(cornerRadiusDp = it).sanitized(kind)
                    },
                )
            }

            if (kind == TaskWidgetKind.SQUARE) {
                SettingsSection(topLabel = stringResource(R.string.widget_configuration_margin_preset)) {
                    SettingsRadioGroupItem(
                        options = listOf(
                            RadioOptionText(TaskWidgetPaddingPreset.Standard, stringResource(R.string.settings_widget_padding_standard)),
                            RadioOptionText(TaskWidgetPaddingPreset.Grid4x6, stringResource(R.string.settings_widget_padding_4x6)),
                            RadioOptionText(TaskWidgetPaddingPreset.Grid4x7, stringResource(R.string.settings_widget_padding_4x7)),
                            RadioOptionText(TaskWidgetPaddingPreset.Custom, stringResource(R.string.widget_configuration_custom_margin)),
                        ),
                        selectedKey = configuration.marginPreset,
                        onOptionSelected = {
                            configuration = configuration.copy(marginPreset = it).sanitized(kind)
                        },
                    )
                }
            }

            if (kind == TaskWidgetKind.CAPSULE || configuration.marginPreset == TaskWidgetPaddingPreset.Custom) {
                SettingsSection(topLabel = stringResource(R.string.widget_configuration_custom_margin)) {
                    ConfigurationSliderRow(
                        label = stringResource(R.string.widget_configuration_top_margin),
                        value = configuration.customTopMarginDp,
                        range = 0..limits.maxSingleMarginDp,
                        onValueChange = {
                            configuration = configuration.copy(customTopMarginDp = it).sanitized(kind)
                        },
                    )
                    ConfigurationSliderRow(
                        label = stringResource(R.string.widget_configuration_bottom_margin),
                        value = configuration.customBottomMarginDp,
                        range = 0..limits.maxSingleMarginDp,
                        onValueChange = {
                            configuration = configuration.copy(customBottomMarginDp = it).sanitized(kind)
                        },
                    )
                }
            }

            if (kind == TaskWidgetKind.SQUARE) {
                SettingsSection(topLabel = stringResource(R.string.widget_configuration_list)) {
                    SettingsSwitchItem(
                        label = R.string.widget_configuration_scrollable,
                        checked = configuration.isScrollable,
                        onCheckedChange = {
                            configuration = configuration.copy(isScrollable = it).sanitized(kind)
                        },
                    )
                }
            }

        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ConfigurationSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    valueText: String = "${value}dp",
    onValueChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueText,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

@Composable
private fun TaskWidgetColorSchemeSelector(
    selected: TaskWidgetColorScheme,
    themeMode: TaskWidgetThemeMode,
    onSelected: (TaskWidgetColorScheme) -> Unit,
) {
    val colors = TaskWidgetThemeResolver.resolve(
        context = LocalContext.current,
        visualState = TaskWidgetVisualState.NORMAL,
        themeMode = themeMode,
    )
    val dark = colors.isDark
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TaskWidgetColorScheme.entries.forEach { scheme ->
            val pair = TaskWidgetColorSchemePalette.resolve(scheme, dark)
            val background = pair?.let { Color(it.background) } ?: Color(colors.backgroundBase)
            val accent = pair?.let { Color(it.accent) } ?: Color(colors.brand)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelected(scheme) },
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(background, accent)))
                        .then(
                            if (selected == scheme) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected == scheme) {
                        Icon(
                            painter = symbolPainterResource(R.drawable.msr_check_wght400),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = colorSchemeLabel(scheme),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun colorSchemeLabel(scheme: TaskWidgetColorScheme): String = stringResource(
    when (scheme) {
        TaskWidgetColorScheme.FOLLOW_APP -> R.string.widget_color_follow_app
        TaskWidgetColorScheme.BLUE -> R.string.widget_color_miuix_blue
        TaskWidgetColorScheme.PURPLE -> R.string.widget_color_purple
        TaskWidgetColorScheme.GREEN -> R.string.widget_color_green
        TaskWidgetColorScheme.ORANGE -> R.string.widget_color_orange
        TaskWidgetColorScheme.GRAPHITE -> R.string.widget_color_graphite
    }
)

@Composable
private fun TaskWidgetConfigurationPreview(
    kind: TaskWidgetKind,
    configuration: TaskWidgetConfiguration,
) {
    val colors = TaskWidgetThemeResolver.resolve(
        context = LocalContext.current,
        visualState = TaskWidgetVisualState.NORMAL,
        widgetColorScheme = configuration.colorScheme,
        themeMode = configuration.themeMode,
    )
    val background = Color(colors.backgroundBase)
    val accent = Color(colors.brand)
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (kind == TaskWidgetKind.SQUARE) 0.58f else 0.88f)
                .aspectRatio(if (kind == TaskWidgetKind.SQUARE) 1f else 2.14f)
                .padding(
                    top = configuration.topMarginDp().dp,
                    bottom = configuration.bottomMarginDp().dp,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(configuration.cornerRadiusDp.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                background.copy(alpha = configuration.backgroundAlpha()),
                                Color(colors.backgroundTint).copy(alpha = configuration.backgroundAlpha()),
                            ),
                        ),
                    )
                    .padding(if (kind == TaskWidgetKind.SQUARE) 12.dp else 14.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                if (kind == TaskWidgetKind.SQUARE) {
                    Text("Deadliner   3", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(colors.normalRow.base).copy(alpha = 0.56f)),
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.glance_task_widget_summary_pending), color = Color(colors.textPrimary), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.glance_task_widget_keep_going), color = accent, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("3", color = accent, fontWeight = FontWeight.Bold, fontSize = 34.sp)
                    }
                }
            }
        }
    }
}
