package com.aritxonly.deadliner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.UiStyle
import com.aritxonly.deadliner.ui.material.glass.LocalImmersiveExperienceEnabled
import com.aritxonly.deadliner.ui.LocalMaterialSymbolStyle
import com.aritxonly.deadliner.ui.MaterialSymbolStyle
import com.aritxonly.deadliner.ui.MaterialSymbolAxisPolicy
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

enum class AppDesignSystem {
    MATERIAL3,
    MIUIX,
}

val LocalAppDesignSystem = staticCompositionLocalOf { AppDesignSystem.MATERIAL3 }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeadlinerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    val designSystem = if (appearance.usesMiuixTheme) AppDesignSystem.MIUIX else AppDesignSystem.MATERIAL3
    val modernColorPalette = if (appearance.modernUseDevicePaletteStrategy) {
        GlobalUtils.getCurrentDeviceColorPalette()
    } else {
        appearance.modernColorPalette
    }
    val tokens = DeadlinerColorTokenFactory.rememberTokens(
        seedColorHex = appearance.seedColorHex,
        colorSource = appearance.effectiveColorSource,
        dynamicPaletteStyle = appearance.dynamicPaletteStyle,
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        modernColorPalette = modernColorPalette,
        usePureMiuixAccent = appearance.usesMiuixTheme && appearance.usePureMiuixAccent,
        useMiuixNeutralSurfaces = appearance.usesMiuixTheme && appearance.useMiuixNeutralSurfaces,
    )
    val materialColorScheme = DeadlinerMaterial3ColorSchemeFactory.create(
        tokens = tokens,
        darkTheme = darkTheme,
    )
    val miuixColorScheme = DeadlinerMiuixColorSchemeFactory.create(
        tokens = tokens,
        darkTheme = darkTheme,
    )
    val advancedMaterialSpec = createAdvancedMaterialSpec(
        enabled = appearance.useAdvancedMaterial,
        blurRadius = appearance.advancedMaterialBlurRadius,
        fineTuning = appearance.advancedMaterialFineTuning,
    )

    CompositionLocalProvider(
        LocalAppDesignSystem provides designSystem,
        LocalHomeFabColors provides homeFabColors(
            colorSource = appearance.effectiveColorSource,
            seedColorHex = appearance.seedColorHex,
            darkTheme = darkTheme,
            fallbackContainer = materialColorScheme.primary,
            useContrastingPresetFab = appearance.useContrastingHomeFab,
        ),
        LocalAdvancedMaterialSpec provides advancedMaterialSpec,
        LocalImmersiveExperienceEnabled provides (
            appearance.useImmersiveExperience && designSystem == AppDesignSystem.MIUIX
        ),
        LocalMaterialSymbolStyle provides MaterialSymbolStyle(
            weight = if (
                appearance.designMode == com.aritxonly.deadliner.model.AppearanceDesignMode.Modern &&
                appearance.useImmersiveExperience
            ) {
                MaterialSymbolAxisPolicy.ImmersiveModernWeight
            } else {
                MaterialSymbolAxisPolicy.DefaultWeight
            },
            grade = if (
                appearance.designMode == com.aritxonly.deadliner.model.AppearanceDesignMode.Modern &&
                appearance.useImmersiveExperience
            ) {
                MaterialSymbolAxisPolicy.ImmersiveModernGrade
            } else {
                MaterialSymbolAxisPolicy.DefaultGrade
            },
            usesModernBackIcon = appearance.designMode == com.aritxonly.deadliner.model.AppearanceDesignMode.Modern,
        ),
    ) {
        MiuixTheme(colors = miuixColorScheme) {
            MaterialExpressiveTheme(
                colorScheme = materialColorScheme,
                typography = Typography,
            ) {
                if (designSystem == AppDesignSystem.MIUIX) {
                    MiuixScaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                        ProvideAdvancedMaterialDialogBlurHost {
                            content()
                        }
                    }
                } else {
                    content()
                }
            }
        }
    }
}
