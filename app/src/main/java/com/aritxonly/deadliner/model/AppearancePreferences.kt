package com.aritxonly.deadliner.model

enum class AppearanceColorSource(val key: String) {
    SystemDynamic("system_dynamic"),
    SeedColor("seed_color"),
    Preset("preset");

    companion object {
        fun fromKey(value: String?): AppearanceColorSource? = entries.firstOrNull { it.key == value }

        fun resolvePersisted(value: String?, seedColorHex: String?): AppearanceColorSource =
            fromKey(value) ?: if (seedColorHex.isNullOrBlank()) SystemDynamic else SeedColor
    }
}

data class AppearancePreferences(
    val uiStyle: UiStyle = UiStyle.Simplified,
    val backgroundColorStrategy: BackgroundColorStrategy = BackgroundColorStrategy.MiuixHyperOs,
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
    val useImmersiveExperience: Boolean = false,
    val floatingNavigationBottomLift: Float = 0f,
    val useMyHyperModifierNavigationLift: Boolean = false,
    val useContrastingHomeFab: Boolean = true,
    val useAdvancedMaterial: Boolean = false,
    val advancedMaterialLevel: AdvancedMaterialLevel = AdvancedMaterialLevel.Light,
    val advancedMaterialBlurRadius: Float = AdvancedMaterialTuning.DefaultBlurRadius,
    val advancedMaterialFineTuning: AdvancedMaterialFineTuning = AdvancedMaterialFineTuning.Default,
    val modernHideDivider: Boolean = true,
    val useSettingsHomepageColoredIcons: Boolean = true,
    val semanticGlowEnabled: Boolean = true,
    val appIconMode: AppIconMode = AppIconMode.Default,
) {
    val usesMiuixTheme: Boolean
        get() = true

    val effectiveColorSource: AppearanceColorSource
        get() = colorSource

    val activeHideDivider: Boolean
        get() = modernHideDivider

    val showsContrastingHomeFabOption: Boolean
        get() = effectiveColorSource == AppearanceColorSource.Preset
}
