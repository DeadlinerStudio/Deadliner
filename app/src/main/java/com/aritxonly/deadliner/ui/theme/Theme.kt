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

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeadlinerTheme(
    designSystem: AppDesignSystem = AppDesignSystem.MIUIX,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val seed by GlobalUtils.seedColorFlow.collectAsState()

    val parsedSeedColor: Color? = seed?.takeIf { it.isNotBlank() }?.let { hexString ->
        runCatching { Color(hexString.toColorInt()) }.getOrNull()
    }

    val m3ColorScheme = when {
        parsedSeedColor != null -> {
            rememberDynamicColorScheme(
                seedColor = parsedSeedColor,
                isDark = darkTheme,
                isAmoled = false
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 2. 将设计系统状态广播下去
    CompositionLocalProvider(LocalAppDesignSystem provides designSystem) {

        when (designSystem) {
            AppDesignSystem.MATERIAL3 -> {
                // M3 模式：原汁原味，直接使用计算好的 m3ColorScheme
                MaterialExpressiveTheme(
                    colorScheme = m3ColorScheme,
                    typography = Typography,
                ) {
                    content()
                }
            }
            AppDesignSystem.MIUIX -> {
                if (dynamicColor) {
                    val controller = remember {
                        ThemeController(
                            ColorSchemeMode.MonetSystem,
                            keyColor = parsedSeedColor
                        )
                    }

                    // MIUIX 模式：外层包裹 MiuixTheme 保证 MIUIX 组件正常工作
                    MiuixTheme(controller) {
                        MaterialExpressiveTheme(
                            colorScheme = m3ColorScheme,
                            typography = Typography,
                        ) {
                            content()
                        }
                    }
                } else {
                    val controller = remember { ThemeController(ColorSchemeMode.System) }

                    val mappedColorScheme = if (darkTheme) {
                        val miuixColors = controller.darkColors
                        darkColorScheme(
                            primary = miuixColors.primary,
                            onPrimary = miuixColors.onPrimary,
                            primaryContainer = miuixColors.primaryContainer,
                            onPrimaryContainer = miuixColors.onPrimaryContainer,
                            inversePrimary = miuixColors.primaryVariant, // MIUIX 无对应，用 primaryVariant 替代

                            secondary = miuixColors.secondary,
                            onSecondary = miuixColors.onSecondary,
                            secondaryContainer = miuixColors.secondaryContainer,
                            onSecondaryContainer = miuixColors.onSecondaryContainer,

                            // MIUIX 只有 tertiaryContainer，没有基础 tertiary。这里用 secondaryVariant 兜底
                            tertiary = miuixColors.secondaryVariant,
                            onTertiary = miuixColors.onSecondaryVariant,
                            tertiaryContainer = miuixColors.tertiaryContainer,
                            onTertiaryContainer = miuixColors.onTertiaryContainer,

                            background = miuixColors.background,
                            onBackground = miuixColors.onBackground,

                            surface = miuixColors.surface,
                            onSurface = miuixColors.onSurface,
                            surfaceVariant = miuixColors.surfaceVariant,

                            // MIUIX 拆分得很细，用 onSurfaceVariantSummary 最贴合 M3 的次要文本语义
                            onSurfaceVariant = miuixColors.onSurfaceVariantSummary,
                            surfaceTint = miuixColors.primary,

                            // M3 反转色：将 surface 和 onSurface 对调即可
                            inverseSurface = miuixColors.onSurface,
                            inverseOnSurface = miuixColors.surface,

                            error = miuixColors.error,
                            onError = miuixColors.onError,
                            errorContainer = miuixColors.errorContainer,
                            onErrorContainer = miuixColors.onErrorContainer,

                            outline = miuixColors.outline,
                            // M3 的 outlineVariant 常用于分割线，完美对应 MIUIX 的 dividerLine
                            outlineVariant = miuixColors.dividerLine,
                            // M3 的 scrim 用于弹窗遮罩，完美对应 MIUIX 的 windowDimming
                            scrim = miuixColors.windowDimming,

                            // Expressive API 容器层级色
                            surfaceContainer = miuixColors.surfaceContainer,
                            surfaceContainerHigh = miuixColors.surfaceContainerHigh,
                            surfaceContainerHighest = miuixColors.surfaceContainerHighest,

                            // MIUIX 没有 Low 和 Lowest，根据 M3 规范向下平替
                            surfaceContainerLow = miuixColors.surface,
                            surfaceContainerLowest = miuixColors.background,
                            surfaceBright = miuixColors.surfaceVariant,
                            surfaceDim = miuixColors.surfaceContainerHigh
                        )
                    } else {
                        val miuixColors = controller.lightColors
                        lightColorScheme(
                            primary = miuixColors.primary,
                            onPrimary = miuixColors.onPrimary,
                            primaryContainer = miuixColors.primaryContainer,
                            onPrimaryContainer = miuixColors.onPrimaryContainer,
                            inversePrimary = miuixColors.primaryVariant,

                            secondary = miuixColors.secondary,
                            onSecondary = miuixColors.onSecondary,
                            secondaryContainer = miuixColors.secondaryContainer,
                            onSecondaryContainer = miuixColors.onSecondaryContainer,

                            tertiary = miuixColors.secondaryVariant,
                            onTertiary = miuixColors.onSecondaryVariant,
                            tertiaryContainer = miuixColors.tertiaryContainer,
                            onTertiaryContainer = miuixColors.onTertiaryContainer,

                            background = miuixColors.background,
                            onBackground = miuixColors.onBackground,

                            surface = miuixColors.surface,
                            onSurface = miuixColors.onSurface,
                            surfaceVariant = miuixColors.surfaceVariant,
                            onSurfaceVariant = miuixColors.onSurfaceVariantSummary,
                            surfaceTint = miuixColors.primary,
                            inverseSurface = miuixColors.onSurface,
                            inverseOnSurface = miuixColors.surface,

                            error = miuixColors.error,
                            onError = miuixColors.onError,
                            errorContainer = miuixColors.errorContainer,
                            onErrorContainer = miuixColors.onErrorContainer,

                            outline = miuixColors.outline,
                            outlineVariant = miuixColors.dividerLine,
                            scrim = miuixColors.windowDimming,

                            surfaceContainer = miuixColors.surfaceContainer,
                            surfaceContainerHigh = miuixColors.surfaceContainerHigh,
                            surfaceContainerHighest = miuixColors.surfaceContainerHighest,
                            surfaceContainerLow = miuixColors.surface,
                            surfaceContainerLowest = miuixColors.background,
                            surfaceBright = miuixColors.surfaceVariant,
                            surfaceDim = miuixColors.surfaceContainerHigh
                        )
                    }
                    MiuixTheme(controller) {
                        MaterialExpressiveTheme(
                            colorScheme = mappedColorScheme,
                            typography = Typography,
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}