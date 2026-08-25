package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.AdvancedMaterialFineTuning
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.deadlinerGlassMaterial
import com.aritxonly.deadliner.ui.material.glass.softGlassTintColor
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.createAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlin.math.roundToInt

@Composable
fun AdvancedMaterialPreviewCard(
    blurRadius: Float,
    fineTuning: AdvancedMaterialFineTuning,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val backdrop = rememberLayerBackdrop()
    val material = createAdvancedMaterialSpec(
        enabled = enabled,
        blurRadius = blurRadius,
        fineTuning = fineTuning,
    )
    val glassRecipe = DeadlinerGlassRecipes.floatingNavigation(material.fineTuning)
    val topTint = MaterialTheme.colorScheme.surface.copy(
        alpha = if (enabled) material.topBarTintAlpha else 0.90f,
    )
    val topBlurColors = material.rememberBlurColors(
        additionalBlendColors = listOf(BlendColorEntry(topTint)),
    )
    val capsuleShape = RoundedCornerShape(percent = 50)
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val capsuleTint = material.fineTuning.softGlassTintColor(isDark).copy(
        alpha = if (enabled) material.navigationTintAlpha else 0.92f,
    )

    SettingsHeroSection(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(204.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            PreviewBackdrop(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .then(
                        if (enabled) {
                            Modifier.advancedTextureBlur(
                                advancedMaterial = material,
                                backdrop = backdrop,
                                shape = RectangleShape,
                                colors = topBlurColors,
                            )
                        } else {
                            Modifier.background(topTint)
                        },
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_advanced_material_top_preview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(
                        R.string.settings_advanced_material_radius_value,
                        blurRadius.roundToInt(),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .height(66.dp)
                    .clip(capsuleShape)
                    .deadlinerGlassMaterial(
                        advancedMaterial = material,
                        backdrop = backdrop,
                        shape = capsuleShape,
                        tint = capsuleTint,
                        isDark = isDark,
                        recipe = glassRecipe,
                    )
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.settings_advanced_material_glass_preview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.settings_advanced_material_glass_value,
                        (blurRadius * glassRecipe.blur.radiusMultiplier).roundToInt(),
                        material.blurSaturation,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PreviewBackdrop(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.some_background),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
