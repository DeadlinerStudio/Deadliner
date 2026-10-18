package com.aritxonly.deadliner.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingItem(
    headlineText: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    expressiveTypeModifier: Modifier = Modifier
        .size(com.aritxonly.deadliner.ui.interfaceIconSize(24.dp, 16.dp))
        .clip(CircleShape)
        .let { baseModifier -> baseModifier },
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = {
        SettingsNavigationChevron(modifier = expressiveTypeModifier.padding(start = 2.dp))
    },
    colors: ListItemColors = ListItemDefaults.colors(containerColor = Color.Transparent),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
    horizontalPadding: Dp = SettingsItemDefaults.HorizontalPadding,
) = ListItem(
    // Material ListItem has a fixed 16dp content inset; supplement it to our shared inset.
    modifier = modifier.padding(horizontal = (horizontalPadding - 16.dp).coerceAtLeast(0.dp)),
    headlineContent = {
        Text(
            text = headlineText,
            maxLines = 1,
            style = MaterialTheme.typography.titleMediumEmphasized
        )
    },
    supportingContent = {
        Text(
            text = supportingText,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
    },
    leadingContent = leadingContent,
    trailingContent = trailingContent,
    colors = colors,
    tonalElevation = tonalElevation,
    shadowElevation = shadowElevation
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailTextButtonItem(
    modifier: Modifier = Modifier,
    @StringRes headline: Int,
    @StringRes supporting: Int,
    @DrawableRes iconRes: Int? = null,
    navigates: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = SettingsItemDefaults.HorizontalPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigates && iconRes != null) {
                Icon(com.aritxonly.deadliner.ui.symbolPainterResource(iconRes), null,
                    Modifier.padding(end = 12.dp).size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(headline),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(supporting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (navigates) SettingsNavigationChevron()
            else if (iconRes != null) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.symbolPainterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailTextButtonItem(
    modifier: Modifier = Modifier,
    headlineText: String,
    supportingText: String,
    @DrawableRes iconRes: Int? = null,
    navigates: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = SettingsItemDefaults.HorizontalPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigates && iconRes != null) {
                Icon(com.aritxonly.deadliner.ui.symbolPainterResource(iconRes), null,
                    Modifier.padding(end = 12.dp).size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (navigates) SettingsNavigationChevron()
            else if (iconRes != null) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.symbolPainterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


/** Shared affordance for app page navigation, matching the homepage's optical icon size. */
@Composable
internal fun SettingsNavigationChevron(modifier: Modifier = Modifier.size(
    com.aritxonly.deadliner.ui.interfaceIconSize(24.dp, 16.dp))) {
    Icon(com.aritxonly.deadliner.ui.iconResource(R.drawable.msr_chevron_right_wght400), null,
        modifier.testTag("settings-navigation-chevron"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
