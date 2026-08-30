package com.aritxonly.deadliner.model

enum class AppearanceColorSource {
    SystemDynamic,
    SeedColor,
}

data class AppearancePreferences(
    val uiStyle: UiStyle = UiStyle.Simplified,
    val designMode: AppearanceDesignMode = AppearanceDesignMode.Modern,
    val themeStyle: AppThemeStyle = AppThemeStyle.Material3,
    val colorSource: AppearanceColorSource = AppearanceColorSource.SystemDynamic,
    val seedColorHex: String? = null,
    val dynamicPaletteStyle: DynamicPaletteStyle = DynamicPaletteStyle.TonalSpot,
    val modernUseDevicePaletteStrategy: Boolean = false,
    val modernColorPalette: ModernColorPalette = ModernColorPalette.HyperOs,
    val displayScalePreset: DisplayScalePreset = DisplayScalePreset.FollowSystem,
    val customDisplayScaleMultiplier: Float = 1.00f,
    val usePureMiuixAccent: Boolean = false,
    val useMiuixNeutralSurfaces: Boolean = true,
    val useMaterialTopAppBarInMiuix: Boolean = false,
    val useAdvancedMaterial: Boolean = false,
    val advancedMaterialLevel: AdvancedMaterialLevel = AdvancedMaterialLevel.Soft,
    val modernHideDivider: Boolean = true,
    val vividHideDivider: Boolean = false,
    val useSettingsHomepageColoredIcons: Boolean = true,
    val appIconMode: AppIconMode = AppIconMode.Default,
) {
    val supportsMiuixTheme: Boolean
        get() = true

    val usesMiuixThemePreference: Boolean
        get() = designMode == AppearanceDesignMode.Modern

    val effectiveThemeStyle: AppThemeStyle
        get() = if (!supportsMiuixTheme) {
            AppThemeStyle.Material3
        } else {
            when (designMode) {
                AppearanceDesignMode.Modern -> AppThemeStyle.Miuix
                AppearanceDesignMode.Vivid -> AppThemeStyle.Material3
            }
        }

    val usesMiuixTheme: Boolean
        get() = effectiveThemeStyle == AppThemeStyle.Miuix

    val activeHideDivider: Boolean
        get() = when (designMode) {
            AppearanceDesignMode.Modern -> modernHideDivider
            AppearanceDesignMode.Vivid -> vividHideDivider
        }
}
