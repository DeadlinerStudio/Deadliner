package com.aritxonly.deadliner.localutils

import com.aritxonly.deadliner.model.ModernColorPalette
import org.junit.Assert.assertEquals
import org.junit.Test

class ModernColorPaletteResolverTest {
    @Test
    fun honorDeviceUsesHonorPalette() {
        assertEquals(
            ModernColorPalette.Honor,
            ModernColorPaletteResolver.resolve(brand = "HONOR", manufacturer = "HONOR")
        )
    }

    @Test
    fun oppoFamilyUsesOppoPalette() {
        assertEquals(
            ModernColorPalette.Oppo,
            ModernColorPaletteResolver.resolve(brand = "OnePlus", manufacturer = "OPPO")
        )
    }

    @Test
    fun oplusFamilyUsesOppoPalette() {
        assertEquals(
            ModernColorPalette.Oppo,
            ModernColorPaletteResolver.resolve(brand = "OPLUS", manufacturer = "OPLUS")
        )
    }

    @Test
    fun vivoFamilyUsesVivoPalette() {
        assertEquals(
            ModernColorPalette.Vivo,
            ModernColorPaletteResolver.resolve(brand = "iQOO", manufacturer = "vivo")
        )
    }

    @Test
    fun huaweiBrandUsesHonorPalette() {
        assertEquals(
            ModernColorPalette.Honor,
            ModernColorPaletteResolver.resolve(brand = "HUAWEI", manufacturer = "HUAWEI")
        )
    }

    @Test
    fun unknownDeviceFallsBackToHyperOsPalette() {
        assertEquals(
            ModernColorPalette.HyperOs,
            ModernColorPaletteResolver.resolve(brand = "google", manufacturer = "google")
        )
    }
}
