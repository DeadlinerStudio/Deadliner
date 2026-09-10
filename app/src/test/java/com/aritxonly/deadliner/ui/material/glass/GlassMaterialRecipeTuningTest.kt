package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GlassMaterialRecipeTuningTest {
    @Test
    fun floatingNavigationUsesPinnedDefaultPreset() {
        val recipe = DeadlinerGlassRecipes.floatingNavigation(
            AdvancedMaterialFineTuning.Default,
        )

        assertEquals(0.25f, recipe.blur.radiusMultiplier, 0.0001f)
        assertEquals(18.dp, recipe.refraction.height)
        assertEquals(18.dp, recipe.refraction.amount)
        assertEquals(0.60f, recipe.refraction.depthEffect, 0.0001f)
        assertEquals(1.dp, recipe.refraction.chromaticAberration)
        assertEquals(0.95f, recipe.edgeOptics.highlightAlpha, 0.0001f)
        assertEquals(0.20f, recipe.edgeOptics.darkHighlightMultiplier, 0.0001f)
        assertEquals(0.5.dp, recipe.layering.postRefractionBlurRadius)
    }

    @Test
    fun floatingNavigationMapsFineTuningIntoRecipe() {
        val recipe = DeadlinerGlassRecipes.floatingNavigation(
            AdvancedMaterialFineTuning.Default.copy(
                glassBlurRadiusMultiplier = 0.32f,
                refractionEnabled = false,
                refractionHeightDp = 22f,
                refractionAmountDp = 19f,
                chromaticAberrationDp = 1.5f,
                edgeHighlightAlpha = 0.18f,
                edgeHighlightGray = 0.72f,
                postRefractionBlurRadiusDp = 0.8f,
            ),
        )

        assertEquals(0.32f, recipe.blur.radiusMultiplier, 0.0001f)
        assertFalse(recipe.refraction.enabled)
        assertEquals(22.dp, recipe.refraction.height)
        assertEquals(19.dp, recipe.refraction.amount)
        assertEquals(1.5.dp, recipe.refraction.chromaticAberration)
        assertEquals(0.18f, recipe.edgeOptics.highlightAlpha, 0.0001f)
        assertEquals(0.72f, recipe.edgeOptics.highlightGray, 0.0001f)
        assertEquals(0.8.dp, recipe.layering.postRefractionBlurRadius)
    }

    @Test
    fun modalBlurScalesFromTheUsersSoftGlassRadius() {
        val tuning = AdvancedMaterialFineTuning.Default.copy(
            glassBlurRadiusMultiplier = 0.25f,
        )

        val dialog = DeadlinerGlassRecipes.dialog(tuning)
        val bottomSheet = DeadlinerGlassRecipes.bottomSheet(tuning)

        assertEquals(1f, dialog.blur.radiusMultiplier, 0.0001f)
        assertEquals(2f, bottomSheet.blur.radiusMultiplier, 0.0001f)
    }

    @Test
    fun modalBlurPreservesCustomSoftGlassTuning() {
        val tuning = AdvancedMaterialFineTuning.Default.copy(
            glassBlurRadiusMultiplier = 0.4f,
        )

        val dialog = DeadlinerGlassRecipes.dialog(tuning)
        val bottomSheet = DeadlinerGlassRecipes.bottomSheet(tuning)

        assertEquals(1.6f, dialog.blur.radiusMultiplier, 0.0001f)
        assertEquals(3.2f, bottomSheet.blur.radiusMultiplier, 0.0001f)
    }

    @Test
    fun immersiveDialogUsesAdvancedMaterialAndWeakHighlightBorder() {
        val recipe = DeadlinerGlassRecipes.immersiveDialogAdvancedHighlight(
            AdvancedMaterialFineTuning.Default,
        )

        assertFalse(recipe.refraction.enabled)
        assertEquals(0.5.dp, recipe.edgeOptics.fallbackWidth)
        assertEquals(0.16f, recipe.edgeOptics.fallbackLightAlpha, 0.0001f)
        assertEquals(0.10f, recipe.edgeOptics.fallbackDarkAlpha, 0.0001f)
    }
}
