package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.aritxonly.deadliner.model.AppearanceColorSource
import com.aritxonly.deadliner.model.BackgroundColorStrategy
import com.materialkolor.ktx.harmonize
import org.junit.Assert.*
import org.junit.Test

class ComponentContainerColorsTest {
    private val material = lightColorScheme(secondary = Color(0xFF865D9B), secondaryContainer = Color(0xFFE1C5EF))

    @Test fun sheetRoleUsesTheSameBaseColorWithOneAlphaApplication() {
        for (dark in listOf(false, true)) for (source in AppearanceColorSource.entries)
            for (strategy in BackgroundColorStrategy.entries) {
                val roles = textFieldContainerColors(material, strategy, source, dark)
                assertEquals(componentContainerColor(material, strategy, source, dark), roles.textFieldContainer)
                assertEquals(roles.textFieldContainer.copy(alpha = 0.65f), roles.textFieldSheetContainer)
            }
    }

    @Test fun materialStrategyPreservesTheExistingSecondaryContainerForEveryColorSource() {
        for (dark in listOf(false, true)) for (source in AppearanceColorSource.entries) {
            assertEquals(material.secondaryContainer, componentContainerColor(material,
                BackgroundColorStrategy.MaterialMonet, source, dark))
        }
    }

    @Test fun presetMiuixUsesOfficialNeutralsAndOtherSourcesHarmonizeOnlyTheComponent() {
        val before = material.toString()
        for (dark in listOf(false, true)) {
            val official = (if (dark) top.yukonga.miuix.kmp.theme.darkColorScheme()
                else top.yukonga.miuix.kmp.theme.lightColorScheme()).secondaryContainer
            assertEquals(official, componentContainerColor(material, BackgroundColorStrategy.MiuixHyperOs,
                AppearanceColorSource.Preset, dark))
            for (source in listOf(AppearanceColorSource.SystemDynamic, AppearanceColorSource.SeedColor)) {
                assertEquals(official.harmonize(material.secondary), componentContainerColor(material,
                    BackgroundColorStrategy.MiuixHyperOs, source, dark))
            }
        }
        assertEquals(before, material.toString())
    }
}
