package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Stable non-Monet colors for the modern design system. */
object MiuixPresetColorSchemes {
    val HomeFab = Color(0xFFF08A7E)

    fun create(darkTheme: Boolean): ColorScheme = if (darkTheme) dark() else light()

    private fun light(): ColorScheme = lightColorScheme(
        primary = Color(0xFF3482FF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF5D9BFF),
        onPrimaryContainer = Color.White,
        inversePrimary = Color(0xFFAECDFF),
        secondary = Color(0xFFA8443B),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDAD6),
        onSecondaryContainer = Color(0xFF410002),
        tertiary = Color(0xFF006A60),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFF74F8E5),
        onTertiaryContainer = Color(0xFF00201C),
    )

    private fun dark(): ColorScheme = darkColorScheme(
        primary = Color(0xFF277AF7),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF338FE4),
        onPrimaryContainer = Color.White,
        inversePrimary = Color(0xFF99C7F1),
        secondary = Color(0xFFFFB4AB),
        onSecondary = Color(0xFF690005),
        secondaryContainer = Color(0xFF93000A),
        onSecondaryContainer = Color(0xFFFFDAD6),
        tertiary = Color(0xFF53DBC8),
        onTertiary = Color(0xFF003731),
        tertiaryContainer = Color(0xFF005048),
        onTertiaryContainer = Color(0xFF74F8E5),
    )
}

