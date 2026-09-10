package com.aritxonly.deadliner.ui.theme

import androidx.compose.ui.graphics.Color

/** Shared modal dimming so Material and MIUIX overlays retain identical contrast. */
internal fun modalScrim(darkTheme: Boolean): Color = Color.Black.copy(
    alpha = if (darkTheme) 0.6f else 0.3f,
)
