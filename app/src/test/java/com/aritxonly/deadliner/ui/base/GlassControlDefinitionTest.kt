package com.aritxonly.deadliner.ui.base

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import com.aritxonly.deadliner.ui.material.glass.LocalProgressiveTopBarMaterialHosted
import com.aritxonly.deadliner.ui.material.glass.SoftGlassDefaults
import com.aritxonly.deadliner.ui.material.glass.SoftGlassIconButton
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import com.aritxonly.deadliner.ui.material.glass.TopBarGlassShadowHost
import com.aritxonly.deadliner.ui.navigation.floatingNavigationShadow
import com.aritxonly.deadliner.ui.symbolPainterResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.feedback.ProvideMiuixPressFeedback
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassControlDefinitionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun actualTopBarControlsGainFarDiffusionWithoutHeavyNearEdges() {
        val background = Color(0xFFF3F3F3)
        compose.setContent {
            MiuixTheme {
                MaterialTheme(colorScheme = lightColorScheme(onSurface = Color.Black)) {
                    ProvideMiuixPressFeedback {
                        CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = true)) {
                            Column {
                                listOf(false, true).forEach { compensated ->
                                    Box(Modifier.size(144.dp).background(background).testTag("diffuse$compensated"), contentAlignment = Alignment.Center) {
                                        CompositionLocalProvider(LocalProgressiveTopBarMaterialHosted provides compensated) {
                                            TopBarGlassShadowHost { SoftGlassIconButton(onClick = {}) {} }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val before = compose.onNodeWithTag("diffusefalse").captureToImage().toPixelMap()
        val after = compose.onNodeWithTag("diffusetrue").captureToImage().toPixelMap()
        val farGains = mutableListOf<Float>()
        val nearGains = mutableListOf<Float>()
        for (y in 0 until after.height) for (x in 0 until after.width) {
            val dx = x + 0.5f - after.width / 2f
            val dy = y + 0.5f - after.height / 2f
            val radius = kotlin.math.sqrt(dx * dx + dy * dy)
            val gain = before[x, y].red - after[x, y].red
            if (radius in 34f..42f) farGains.add(gain)
            if (radius in 24f..27f) nearGains.add(gain)
        }
        assertTrue("The host must render the compensated far shadow", farGains.average() > 0.0005)
        assertTrue("Keep the near halo restrained", nearGains.average() < 0.008)
    }

    @Test fun topBarPortalAndBottomBarRenderTheSameShadowForMatchingGeometry() {
        val background = Color(0xFFF3F3F3)
        compose.setContent {
            MiuixTheme {
                MaterialTheme(colorScheme = lightColorScheme(onSurface = Color.Black)) {
                    ProvideMiuixPressFeedback {
                        CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = true)) {
                            Column {
                                Box(Modifier.size(144.dp).background(background).testTag("top"), contentAlignment = Alignment.Center) {
                                    TopBarGlassShadowHost { SoftGlassIconButton(onClick = {}) {} }
                                }
                                Box(Modifier.size(144.dp).background(background).testTag("bottom"), contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(44.dp).floatingNavigationShadow(SoftGlassDefaults.Shape, isDark = false)) {
                                        SoftGlassSurface(Modifier.matchParentSize()) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val top = compose.onNodeWithTag("top").captureToImage().toPixelMap()
        val bottom = compose.onNodeWithTag("bottom").captureToImage().toPixelMap()
        // Compare the full region outside the lens, including ambient and near shadows.
        var shadowPixels = 0
        for (y in 0 until top.height) for (x in 0 until top.width) {
            val dx = x + 0.5f - top.width / 2f
            val dy = y + 0.5f - top.height / 2f
            if (dx * dx + dy * dy < 24f * 24f) continue
            assertEquals("Shadow at $x,$y", bottom[x, y].red, top[x, y].red, 1f / 255f)
            if (background.red - top[x, y].red > 0.003f) shadowPixels++
        }
        assertTrue("Compare visible shadows rather than two empty areas", shadowPixels > 40)
    }

    @Test fun navigationGlassKeepsItsSoftShadowOutsideTheClippedPressSurface() {
        val background = Color(0xFFF3F3F3)
        compose.setContent {
            MiuixTheme {
                MaterialTheme(colorScheme = lightColorScheme(onSurface = Color.Black)) {
                    ProvideMiuixPressFeedback {
                        CompositionLocalProvider(LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(enabled = true)) {
                            Box(Modifier.size(144.dp).background(background).testTag("scene"),
                                contentAlignment = Alignment.Center) {
                                ImmersiveNavigationIcon(materialAlpha = 1f) {
                                    Icon(symbolPainterResource(R.drawable.msr_arrow_back_wght400), null,
                                        Modifier.size(24.dp), tint = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
        val image = compose.onNodeWithTag("scene").captureToImage()
        val pixels = image.toPixelMap()
        val cx = pixels.width / 2
        val cy = pixels.height / 2
        // The 44dp lens ends at +/-22dp. This sample is beyond its press/shape clip.
        val outside = pixels[cx + 25, cy]
        assertTrue("Glass elevation must survive outside the press clip", background.red - outside.red > 0.004f)
        assertTrue("Keep a diffuse shadow rather than a dark outline", background.red - outside.red < 0.14f)
        val body = pixels[cx + 12, cy]
        assertTrue("The glass body must stay light", body.red > 0.90f)
    }
}
