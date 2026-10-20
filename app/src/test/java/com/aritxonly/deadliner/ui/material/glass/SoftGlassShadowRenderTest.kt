package com.aritxonly.deadliner.ui.material.glass

import com.aritxonly.deadliner.model.GlassShadowPreset
import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w400dp-h360dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SoftGlassShadowRenderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cleanPresetReducesDistantHazeWithoutDarkeningTheLens() {
        compose.setContent {
            Box(Modifier.size(360.dp, 280.dp).background(Color.White).testTag("comparison")) {
                for (updated in listOf(false, true)) {
                    val centerX = if (updated) 270 else 90
                    val circle = CircleShape
                    val bar = RoundedCornerShape(22.dp)
                    Box(Modifier.offset((centerX - 22).dp, 58.dp).size(44.dp)
                        .comparisonShadow(circle, updated, SoftGlassShadowProfile.TopBarControl)
                        .background(Color(0xFFF9F9F9), circle))
                    Box(Modifier.offset((centerX - 65).dp, 178.dp).size(130.dp, 44.dp)
                        .comparisonShadow(bar, updated, SoftGlassShadowProfile.Standard)
                        .background(Color(0xFFF9F9F9), bar))
                }
            }
        }
        val image = compose.onNodeWithTag("comparison").captureToImage()
        val pixels = image.toPixelMap()
        for (bottom in listOf(102, 222)) {
            fun density(centerX: Int): Double = (4..12).flatMap { distance ->
                (-6..6).map { dx -> 1.0 - pixels[centerX + dx, bottom + distance].red }
            }.average()
            val before = density(90)
            val after = density(270)
            assertTrue("Clean shadow must retain separation: $after", after > 0.002)
            assertTrue("Clean shadow must reduce surrounding haze: $before -> $after", after < before)
        }
        assertEquals(pixels[90, 80].red, pixels[270, 80].red, 0.001f)
        assertEquals(249f / 255f, pixels[270, 80].red, 0.005f)
        assertTrue("Distant page must remain white", pixels[180, 140].red > 0.99f)
        val output = File("build/reports/soft-glass-shadow").apply { mkdirs() }
        File(output, "white-before-after.png").outputStream().use {
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun Modifier.comparisonShadow(
        shape: Shape,
        updated: Boolean,
        profile: SoftGlassShadowProfile,
    ): Modifier {
        if (updated) return drawSoftGlassShadow(shape, false, 1f, false, profile, GlassShadowPreset.Clean)
        return drawSoftGlassShadow(shape, false, 1f, false, profile, GlassShadowPreset.Diffuse)
    }
}
