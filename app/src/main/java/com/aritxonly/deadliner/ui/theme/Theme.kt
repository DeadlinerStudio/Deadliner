package com.aritxonly.deadliner.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.materialkolor.rememberDynamicColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.scheme.DynamicScheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

enum class AppDesignSystem {
    MATERIAL3,
    MIUIX,
}

val LocalAppDesignSystem = staticCompositionLocalOf { AppDesignSystem.MATERIAL3 }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeadlinerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val seed by GlobalUtils.seedColorFlow.collectAsState()
    val miuixMode by GlobalUtils.miuixModeFlow.collectAsState()

    val miuixColor = GlobalUtils.miuixColorFlow.collectAsState()

    val designSystem = if (miuixMode) AppDesignSystem.MIUIX else AppDesignSystem.MATERIAL3
    val enableMiuixColors = miuixColor.value

    val parsedSeedColor: Color? = seed?.takeIf { it.isNotBlank() }?.let { hexString ->
        runCatching { Color(hexString.toColorInt()) }.getOrNull()
    }

    val m3ColorScheme = when {
        parsedSeedColor != null -> {
            rememberDynamicColorScheme(
                seedColor = parsedSeedColor,
                isDark = darkTheme,
                isAmoled = false,
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
                platform = DynamicScheme.Platform.PHONE
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalAppDesignSystem provides designSystem) {

        when (designSystem) {
            AppDesignSystem.MATERIAL3 -> {
                // M3 模式：原汁原味
                MaterialExpressiveTheme(
                    colorScheme = m3ColorScheme,
                    typography = Typography,
                ) {
                    content()
                }
            }
            AppDesignSystem.MIUIX -> {
                val miuixHybridColors = remember(m3ColorScheme, darkTheme, enableMiuixColors) {
                    if (enableMiuixColors) {
                        if (darkTheme) {
                            top.yukonga.miuix.kmp.theme.darkColorScheme(
                                primary = Color(0xFF0472DC)
                            )
                        } else {
                            top.yukonga.miuix.kmp.theme.lightColorScheme(
                                primary = Color(0xFF3481FF)
                            )
                        }
                    } else {
                        if (darkTheme) {
                            top.yukonga.miuix.kmp.theme.darkColorScheme(
                                primary = m3ColorScheme.primary,
                                onPrimary = m3ColorScheme.onPrimary,
                                primaryContainer = m3ColorScheme.primaryContainer,
                                onPrimaryContainer = m3ColorScheme.onPrimaryContainer,
                                primaryVariant = m3ColorScheme.inversePrimary,

                                secondary = m3ColorScheme.secondary,
                                onSecondary = m3ColorScheme.onSecondary,
                                secondaryContainer = m3ColorScheme.secondaryContainer,
                                onSecondaryContainer = m3ColorScheme.onSecondaryContainer,

                                secondaryVariant = m3ColorScheme.tertiary,
                                onSecondaryVariant = m3ColorScheme.onTertiary,
                                tertiaryContainer = m3ColorScheme.tertiaryContainer,
                                onTertiaryContainer = m3ColorScheme.onTertiaryContainer
                            )
                        } else {
                            top.yukonga.miuix.kmp.theme.lightColorScheme(
                                primary = m3ColorScheme.primary,
                                onPrimary = m3ColorScheme.onPrimary,
                                primaryContainer = m3ColorScheme.primaryContainer,
                                onPrimaryContainer = m3ColorScheme.onPrimaryContainer,
                                primaryVariant = m3ColorScheme.inversePrimary,

                                secondary = m3ColorScheme.secondary,
                                onSecondary = m3ColorScheme.onSecondary,
                                secondaryContainer = m3ColorScheme.secondaryContainer,
                                onSecondaryContainer = m3ColorScheme.onSecondaryContainer,

                                secondaryVariant = m3ColorScheme.tertiary,
                                onSecondaryVariant = m3ColorScheme.onTertiary,
                                tertiaryContainer = m3ColorScheme.tertiaryContainer,
                                onTertiaryContainer = m3ColorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                MiuixTheme(colors = miuixHybridColors) {
                    // 由于我们的 miuixHybridColors 已经准备妥当，
                    // 这里的反向映射逻辑无论是在“纯净澎湃”还是“混血”模式下，都能完美工作！
                    val finalM3ColorScheme = remember(miuixHybridColors, darkTheme) {
                        if (darkTheme) {
                            androidx.compose.material3.darkColorScheme(
                                primary = miuixHybridColors.primary,
                                onPrimary = miuixHybridColors.onPrimary,
                                primaryContainer = miuixHybridColors.primaryContainer,
                                onPrimaryContainer = miuixHybridColors.onPrimaryContainer,
                                inversePrimary = miuixHybridColors.primaryVariant,

                                secondary = miuixHybridColors.secondary,
                                onSecondary = miuixHybridColors.onSecondary,
                                secondaryContainer = miuixHybridColors.secondaryContainer,
                                onSecondaryContainer = miuixHybridColors.onSecondaryContainer,

                                tertiary = miuixHybridColors.secondaryVariant,
                                onTertiary = miuixHybridColors.onSecondaryVariant,
                                tertiaryContainer = miuixHybridColors.tertiaryContainer,
                                onTertiaryContainer = miuixHybridColors.onTertiaryContainer,

                                background = miuixHybridColors.background,
                                onBackground = miuixHybridColors.onBackground,

                                surface = miuixHybridColors.surface,
                                onSurface = miuixHybridColors.onSurface,
                                surfaceVariant = miuixHybridColors.surfaceVariant,
                                onSurfaceVariant = miuixHybridColors.onSurfaceVariantSummary,
                                surfaceTint = miuixHybridColors.primary,

                                inverseSurface = miuixHybridColors.onSurface,
                                inverseOnSurface = miuixHybridColors.surface,

                                error = miuixHybridColors.error,
                                onError = miuixHybridColors.onError,
                                errorContainer = miuixHybridColors.errorContainer,
                                onErrorContainer = miuixHybridColors.onErrorContainer,

                                outline = miuixHybridColors.outline,
                                outlineVariant = miuixHybridColors.dividerLine,
                                scrim = miuixHybridColors.windowDimming,

                                surfaceContainer = miuixHybridColors.surfaceContainer,
                                surfaceContainerHigh = miuixHybridColors.surfaceContainerHigh,
                                surfaceContainerHighest = miuixHybridColors.surfaceContainerHighest,
                                surfaceContainerLow = miuixHybridColors.surface,
                                surfaceContainerLowest = miuixHybridColors.background,
                                surfaceBright = miuixHybridColors.surfaceVariant,
                                surfaceDim = miuixHybridColors.surfaceContainerHigh
                            )
                        } else {
                            androidx.compose.material3.lightColorScheme(
                                primary = miuixHybridColors.primary,
                                onPrimary = miuixHybridColors.onPrimary,
                                primaryContainer = miuixHybridColors.primaryContainer,
                                onPrimaryContainer = miuixHybridColors.onPrimaryContainer,
                                inversePrimary = miuixHybridColors.primaryVariant,

                                secondary = miuixHybridColors.secondary,
                                onSecondary = miuixHybridColors.onSecondary,
                                secondaryContainer = miuixHybridColors.secondaryContainer,
                                onSecondaryContainer = miuixHybridColors.onSecondaryContainer,

                                tertiary = miuixHybridColors.secondaryVariant,
                                onTertiary = miuixHybridColors.onSecondaryVariant,
                                tertiaryContainer = miuixHybridColors.tertiaryContainer,
                                onTertiaryContainer = miuixHybridColors.onTertiaryContainer,

                                background = miuixHybridColors.background,
                                onBackground = miuixHybridColors.onBackground,

                                surface = miuixHybridColors.surface,
                                onSurface = miuixHybridColors.onSurface,
                                surfaceVariant = miuixHybridColors.surfaceVariant,
                                onSurfaceVariant = miuixHybridColors.onSurfaceVariantSummary,
                                surfaceTint = miuixHybridColors.primary,

                                inverseSurface = miuixHybridColors.onSurface,
                                inverseOnSurface = miuixHybridColors.surface,

                                error = miuixHybridColors.error,
                                onError = miuixHybridColors.onError,
                                errorContainer = miuixHybridColors.errorContainer,
                                onErrorContainer = miuixHybridColors.onErrorContainer,

                                outline = miuixHybridColors.outline,
                                outlineVariant = miuixHybridColors.dividerLine,
                                scrim = miuixHybridColors.windowDimming,

                                surfaceContainer = miuixHybridColors.surfaceContainer,
                                surfaceContainerHigh = miuixHybridColors.surfaceContainerHigh,
                                surfaceContainerHighest = miuixHybridColors.surfaceContainerHighest,
                                surfaceContainerLow = miuixHybridColors.surface,
                                surfaceContainerLowest = miuixHybridColors.background,
                                surfaceBright = miuixHybridColors.surfaceVariant,
                                surfaceDim = miuixHybridColors.surfaceContainerHigh
                            )
                        }
                    }

                    MaterialExpressiveTheme(
                        colorScheme = finalM3ColorScheme,
                        typography = Typography,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
