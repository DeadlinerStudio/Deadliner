package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.scheme.DynamicScheme

/** Stable non-Monet colors for the modern design system. */
object MiuixPresetColorSchemes {
    val LightSeed = Color(0xFF3382FF)
    val DarkSeed = Color(0xFF277AF7)
    val HomeFab = Color(0xFFFF6D6D)

    fun seed(darkTheme: Boolean): Color = if (darkTheme) DarkSeed else LightSeed

    fun create(darkTheme: Boolean): ColorScheme {
        val primarySeed = seed(darkTheme)
        return dynamicColorScheme(
            primary = primarySeed,
            isDark = darkTheme,
            isAmoled = false,
            style = PaletteStyle.TonalSpot,
            specVersion = ColorSpec.SpecVersion.SPEC_2021,
            platform = DynamicScheme.Platform.PHONE,
        ).copy(
            primary = primarySeed,
            onPrimary = Color.White,
        )
    }
}
