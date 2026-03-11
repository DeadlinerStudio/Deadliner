package com.aritxonly.deadliner.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toColorLong
import com.aritxonly.deadliner.model.AppColorScheme

val defaultAppColorScheme: AppColorScheme
    @Composable get() = AppColorScheme(
        primary = MaterialTheme.colorScheme.primary.toColorLong().toInt(),
        onPrimary = MaterialTheme.colorScheme.onPrimary.toColorLong().toInt(),
        primaryContainer = MaterialTheme.colorScheme.primaryContainer.toColorLong().toInt(),
        surface = MaterialTheme.colorScheme.surface.toColorLong().toInt(),
        onSurface = MaterialTheme.colorScheme.onSurface.toColorLong().toInt(),
        surfaceContainer = MaterialTheme.colorScheme.surfaceContainer.toColorLong().toInt(),
        secondary = MaterialTheme.colorScheme.secondary.toColorLong().toInt(),
        onSecondary = MaterialTheme.colorScheme.onSecondary.toColorLong().toInt(),
        secondaryContainer = MaterialTheme.colorScheme.secondaryContainer.toColorLong().toInt(),
        onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer.toColorLong().toInt(),
        tertiary = MaterialTheme.colorScheme.tertiary.toColorLong().toInt(),
        onTertiary = MaterialTheme.colorScheme.onTertiary.toColorLong().toInt(),
        tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer.toColorLong().toInt(),
        onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer.toColorLong().toInt(),
    )