package com.aritxonly.deadliner.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.picker.DateTimePickerHost
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.BackgroundColorStrategy
import com.aritxonly.deadliner.ui.navigation.LocalFloatingNavigationBottomLift
import com.aritxonly.deadliner.ui.navigation.rememberMyHyperModifierNavigationLift
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
    editorLaunchRequest: android.content.Intent? = null,
    onEditorLaunchRequestConsumed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val appearance by GlobalUtils.appearanceFlow.collectAsState()
    val iconPack by com.aritxonly.deadliner.ui.InterfaceIconPreferences.flow.collectAsState()
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
        useOfficialMiuixGlass = appearance.useOfficialMiuixGlass,
    )
    val sharedNavigationLift = rememberMyHyperModifierNavigationLift(
        appearance.useMyHyperModifierNavigationLift,
    )
    val floatingNavigationLift = if (appearance.useMyHyperModifierNavigationLift) {
        sharedNavigationLift
    } else {
        appearance.floatingNavigationBottomLift.coerceIn(0f, 48f).dp
    }

    CompositionLocalProvider(
        LocalHomeFabColors provides homeFabColors(
            colorSource = appearance.effectiveColorSource,
            seedColorHex = appearance.seedColorHex,
            darkTheme = darkTheme,
            fallbackContainer = materialColorScheme.primary,
            useContrastingPresetFab = appearance.useContrastingHomeFab,
        ),
        LocalAdvancedMaterialSpec provides advancedMaterialSpec,
        LocalTextFieldContainerColors provides textFieldContainerColors(
            material = materialColorScheme,
            strategy = appearance.backgroundColorStrategy,
            colorSource = appearance.effectiveColorSource,
            darkTheme = darkTheme,
        ),
        LocalFloatingNavigationBottomLift provides floatingNavigationLift,
        com.aritxonly.deadliner.ui.LocalInterfaceIconPack provides iconPack,
        LocalMaterialSymbolStyle provides MaterialSymbolStyle(
            weight = MaterialSymbolAxisPolicy.ImmersiveModernWeight,
            grade = MaterialSymbolAxisPolicy.ImmersiveModernGrade,
            usesModernBackIcon = true,
        ),
    ) {
        MiuixTheme(colors = miuixColorScheme) {
            MaterialExpressiveTheme(
                colorScheme = materialColorScheme,
                typography = Typography,
            ) {
                com.aritxonly.deadliner.ui.base.feedback.ProvideMiuixPressFeedback {
                    // Own the recorder and retained dialogs above Scaffold's popup host,
                    // so Sheet callers inherit the same whole-window sampling source.
                    ProvideAdvancedMaterialDialogBlurHost {
                        MiuixScaffold(
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        ) {
                            com.aritxonly.deadliner.ui.editor.DeadlineEditorEntryHost(
                                editorLaunchRequest, onEditorLaunchRequestConsumed,
                            ) {
                                content()
                                DateTimePickerHost()
                            }
                        }
                    }
                }
            }
        }
    }
}
