package com.aritxonly.deadliner.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.UiStyle
import com.aritxonly.deadliner.ui.base.RadioButton
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@Composable
fun UiSettingsScreen(
    navigateUp: () -> Unit
) {
    val currentStyle by GlobalUtils.styleFlow.collectAsState()
    val onStyleChange: (String) -> Unit = {
        GlobalUtils.setStyle(UiStyle.fromKey(it))
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_ui_mode_title),
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
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            UiModeSelectionRow(
                currentStyle = currentStyle.key,
                onStyleChange = onStyleChange,
                invertColorFilter = null,
            )

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

private enum class UiModeOption(
    val key: String,
    @StringRes val labelRes: Int,
    @StringRes val supportingRes: Int,
    @StringRes val fitRes: Int,
    val keywordResList: List<Int>,
) {
    Simplified(
        key = UiStyle.Simplified.key,
        labelRes = R.string.ui_style_simplified,
        supportingRes = R.string.ui_style_simplified_support,
        fitRes = R.string.ui_style_simplified_fit,
        keywordResList = listOf(
            R.string.ui_style_simplified_keyword_focus,
            R.string.ui_style_simplified_keyword_switch,
            R.string.ui_style_simplified_keyword_light,
        ),
    ),
    Miuix(
        key = UiStyle.Miuix.key,
        labelRes = R.string.ui_style_miuix,
        supportingRes = R.string.ui_style_miuix_support,
        fitRes = R.string.ui_style_miuix_fit,
        keywordResList = listOf(
            R.string.ui_style_miuix_keyword_navigation,
            R.string.ui_style_miuix_keyword_entry,
            R.string.ui_style_miuix_keyword_power,
        ),
    ),
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun UiModeSelectionRow(
    currentStyle: String,
    onStyleChange: (String) -> Unit,
    invertColorFilter: ColorFilter?,
    inIntroPage: Boolean = false
) {
    val visibleModes = listOf(
        UiModeOption.Simplified,
        UiModeOption.Miuix,
    )
    val selectedVisibleStyle = visibleModes.firstOrNull { it.key == currentStyle }?.key

    val edgePadding = if (!inIntroPage) 16.dp else 2.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (currentStyle == UiStyle.Classic.key) {
            HiddenClassicModeNotice(
                modifier = Modifier.padding(horizontal = edgePadding)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edgePadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            visibleModes.forEach { mode ->
                UiModeOptionCard(
                    option = mode,
                    selected = selectedVisibleStyle == mode.key,
                    onClick = { onStyleChange(mode.key) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HiddenClassicModeNotice(
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.ui_style_classic_hidden_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.ui_style_classic_hidden_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UiModeOptionCard(
    option: UiModeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))

    Card(
        modifier = modifier
            .wrapContentHeight()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                    enabled = true
                )
                Column(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(option.labelRes),
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(option.supportingRes),
                        minLines = 2,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ui_style_best_for),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(option.fitRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        option.keywordResList.forEach { keywordRes ->
                            UiModeKeywordChip(text = stringResource(keywordRes))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UiModeKeywordChip(
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
