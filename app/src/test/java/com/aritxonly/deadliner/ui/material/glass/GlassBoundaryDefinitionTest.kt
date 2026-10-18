package com.aritxonly.deadliner.ui.material.glass

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassBoundaryDefinitionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun whiteBackgroundCircleUsesNeutralPresetWithoutDarkOverlay() = compare(44.dp, CircleShape, Color.White)
    @Test fun grayBackgroundCapsuleUsesNeutralPresetWithoutDarkOverlay() = compare(180.dp, RoundedCornerShape(22.dp), Color(0xFFF3F3F3))

    private fun compare(width: Dp, shape: Shape, background: Color) {
        compose.setContent {
            Column {
                sample("before", width, shape, background, updated = false)
                sample("after", width, shape, background, updated = true)
            }
        }
        val before = compose.onNodeWithTag("before").captureToImage().toPixelMap()
        val after = compose.onNodeWithTag("after").captureToImage().toPixelMap()
        val x = after.width / 2
        val y = after.height / 2
        assertEquals("Do not add any uniform darkening to the preset body", before[x, y].red, after[x, y].red, 0f)
        assertTrue("No dark inner transition should remain", after[x, y + 20].red >= after[x, y].red - 1f / 255f)
        assertTrue("Thin outer highlight stays brighter than the body", after[x, y + 21].red > after[x, y].red + 0.005f)
        val beforeShadow = mutableListOf<Float>()
        val afterShadow = mutableListOf<Float>()
        for (yy in 0 until after.height) for (xx in 0 until after.width) {
            if (yy < y - 23 || yy > y + 23 || xx < 40 || xx >= after.width - 40) {
                beforeShadow.add(background.red - before[xx, yy].red)
                afterShadow.add(background.red - after[xx, yy].red)
            }
        }
        // Low-alpha native shadows dither by up to two 8-bit levels at different screen offsets.
        // Check overall density tightly as well, so an enlarged or missing halo cannot pass.
        assertTrue("Reference shadow must be visible", beforeShadow.average() > 0.0003)
        assertEquals("Outer shadow density must remain unchanged", beforeShadow.average(), afterShadow.average(), 0.00005)
        assertTrue("No new pixels outside the existing soft shadow",
            beforeShadow.zip(afterShadow).maxOf { kotlin.math.abs(it.first - it.second) } <= 2.01f / 255f)
        // Outside a circular corner, the new body overlay must follow the shape.
        if (width == 44.dp) assertEquals(before[x + 20, y + 20].red, after[x + 20, y + 20].red, 1f / 255f)
    }

    @Composable private fun sample(tag: String, width: Dp, shape: Shape, background: Color, updated: Boolean) {
        val material = AdvancedMaterialSpec(enabled = true)
        val recipe = DeadlinerGlassRecipes.floatingNavigation(material.fineTuning)
        val tint = material.fineTuning.softGlassTintColor(false).copy(alpha = material.navigationTintAlpha)
        val appearance = if (updated) Modifier.deadlinerGlassMaterial(material, null, shape, tint,
            isDark = false, recipe = recipe)
        else Modifier.background(resolveGlassTintComposite(tint.copy(alpha = tint.alpha * recipe.blur.tintAlphaMultiplier),
            GlassTintStyle.Neutral, false).fallbackTint, shape)
            .border(recipe.edgeOptics.edgeStrokeWidth(false), recipe.edgeOptics.edgeBrush(false, optical = false), shape)
        Box(Modifier.size(width + 80.dp, 124.dp).background(background).testTag(tag), contentAlignment = Alignment.Center) {
            // Production chrome owns elevation on a separate outer frame.
            Box(Modifier.size(width, 44.dp).drawSoftGlassShadow(shape, false, 1f, false)) {
                Box(Modifier.matchParentSize().clip(shape).then(appearance))
            }
        }
    }

    @Test fun disablingTheRimLeavesNoOutline() {
        compose.setContent {
            Row {
                val off = GlassEdgeOpticsSpec(highlightAlpha = 0f, fallbackLightAlpha = 0f)
                listOf(off to false, off to true, GlassEdgeOpticsSpec(fallbackWidth = 0.dp) to true)
                    .forEachIndexed { index, (spec, optical) ->
                        Box(Modifier.size(44.dp).background(Color(0xFFE5E5E5)).testTag("off$index")
                            .glassBoundary(CircleShape, spec, isDark = false, optical = optical))
                    }
            }
        }
        repeat(3) {
            val pixels = compose.onNodeWithTag("off$it").captureToImage().toPixelMap()
            for (y in 0 until pixels.height) for (x in 0 until pixels.width)
                assertEquals(Color(0xFFE5E5E5).red, pixels[x, y].red, 0f)
        }
    }

}
