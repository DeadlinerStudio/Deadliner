package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.theme.AdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Preview(
    name = "Deadliner Glass - Light",
    widthDp = 380,
    heightDp = 260,
    showBackground = true,
)
@Composable
private fun DeadlinerGlassLightPreview() {
    GlassMaterialPreviewScene(darkTheme = false)
}

@Preview(
    name = "Deadliner Glass - Dark",
    widthDp = 380,
    heightDp = 260,
    showBackground = true,
    backgroundColor = 0xFF101114,
)
@Composable
private fun DeadlinerGlassDarkPreview() {
    GlassMaterialPreviewScene(darkTheme = true)
}

@Composable
private fun GlassMaterialPreviewScene(darkTheme: Boolean) {
    MiuixTheme(colors = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        MaterialTheme {
            val backdrop = rememberLayerBackdrop()
            val contentColor = MiuixTheme.colorScheme.onSurfaceContainer
            val isDark = contentColor.luminance() > 0.5f
            val baseColor = if (isDark) {
                lerp(MiuixTheme.colorScheme.surfaceContainer, Color.Black, 0.28f)
            } else {
                MiuixTheme.colorScheme.surfaceContainer
            }
            val tint = baseColor.copy(alpha = if (darkTheme) 0.58f else 0.68f)
            val shape = RoundedCornerShape(percent = 50)

            CompositionLocalProvider(
                LocalAdvancedMaterialSpec provides AdvancedMaterialSpec(
                    enabled = true,
                    blurRadius = 48f,
                    noiseCoefficient = 0.07f,
                    navigationTintAlpha = tint.alpha,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (darkTheme) Color(0xFF101114) else Color(0xFFF0F1F4)),
                ) {
                    GlassPreviewBackdrop(
                        darkTheme = darkTheme,
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        GlassPreviewPill(
                            label = "UNIFIED BLUR + LENS",
                            backdrop = backdrop,
                            shape = shape,
                            tint = tint,
                            isDark = isDark,
                            contentColor = contentColor,
                            recipe = DeadlinerGlassRecipes.FloatingNavigation,
                        )
                        GlassPreviewPill(
                            label = "STRONG EDGE REFRACTION",
                            backdrop = backdrop,
                            shape = shape,
                            tint = tint,
                            isDark = isDark,
                            contentColor = contentColor,
                            recipe = DeadlinerGlassRecipes.FloatingNavigation.copy(
                                refraction = DeadlinerGlassRecipes.FloatingNavigation.refraction.copy(
                                    height = 22.dp,
                                    amount = 21.dp,
                                    depthEffect = 0.32f,
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassPreviewPill(
    label: String,
    backdrop: LayerBackdrop,
    shape: RoundedCornerShape,
    tint: Color,
    isDark: Boolean,
    contentColor: Color,
    recipe: GlassMaterialRecipe,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .deadlinerGlassMaterial(
                advancedMaterial = LocalAdvancedMaterialSpec.current,
                backdrop = backdrop,
                shape = shape,
                tint = tint,
                isDark = isDark,
                recipe = recipe,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun GlassPreviewBackdrop(
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = if (darkTheme) {
        listOf(Color(0xFF2C4A82), Color(0xFF9A4E72), Color(0xFFB78234), Color(0xFF315D58))
    } else {
        listOf(Color(0xFF76A8F7), Color(0xFFF184B1), Color(0xFFFFC263), Color(0xFF67B8AD))
    }
    Column(
        modifier = modifier.padding(vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "BACKGROUND CONTENT  ·  BACKGROUND CONTENT",
            color = if (darkTheme) Color.White else Color.Black,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
        ) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color),
                )
            }
        }
        Text(
            text = "0123456789  LIQUID  0123456789",
            color = if (darkTheme) Color.White else Color.Black,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}
