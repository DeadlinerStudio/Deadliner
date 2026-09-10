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
import com.aritxonly.deadliner.model.BackgroundColorStrategy
import com.aritxonly.deadliner.ui.material.glass.LocalImmersiveExperienceEnabled
import com.aritxonly.deadliner.ui.LocalMaterialSymbolStyle
import com.aritxonly.deadliner.ui.MaterialSymbolStyle
import com.aritxonly.deadliner.ui.MaterialSymbolAxisPolicy
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeadlinerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    val usesMaterialMonetBackground =
        appearance.backgroundColorStrategy == BackgroundColorStrategy.MaterialMonet
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
        usePureMiuixAccent = appearance.usePureMiuixAccent,
        useMiuixNeutralSurfaces = !usesMaterialMonetBackground && appearance.useMiuixNeutralSurfaces,
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
        LocalHomeFabColors provides homeFabColors(
            colorSource = appearance.effectiveColorSource,
            seedColorHex = appearance.seedColorHex,
            darkTheme = darkTheme,
            fallbackContainer = materialColorScheme.primary,
            useContrastingPresetFab = appearance.useContrastingHomeFab,
        ),
        LocalAdvancedMaterialSpec provides advancedMaterialSpec,
        LocalImmersiveExperienceEnabled provides (
            appearance.useImmersiveExperience
        ),
        LocalMaterialSymbolStyle provides MaterialSymbolStyle(
            weight = if (
                appearance.useImmersiveExperience
            ) {
                MaterialSymbolAxisPolicy.ImmersiveModernWeight
            } else {
                MaterialSymbolAxisPolicy.DefaultWeight
            },
            grade = if (
                appearance.useImmersiveExperience
            ) {
                MaterialSymbolAxisPolicy.ImmersiveModernGrade
            } else {
                MaterialSymbolAxisPolicy.DefaultGrade
            },
            usesModernBackIcon = true,
        ),
    ) {
        MiuixTheme(colors = miuixColorScheme) {
            MaterialExpressiveTheme(
                colorScheme = materialColorScheme,
                typography = Typography,
            ) {
                MiuixScaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    ProvideAdvancedMaterialDialogBlurHost {
                        content()
                    }
                }
            }
        }
    }
}
