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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassRimBalanceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun circleReliesOnContinuousRimRatherThanDenseShadow() = compare(44.dp, CircleShape)
    @Test fun capsuleKeepsSeparationWithLessPeripheralHaze() = compare(180.dp, RoundedCornerShape(22.dp))

    private fun compare(width: Dp, shape: Shape) {
        compose.setContent {
            Column {
                sample("old", width, shape, previous = true)
                sample("new", width, shape, previous = false)
            }
        }
        val old = compose.onNodeWithTag("old").captureToImage().toPixelMap()
        val new = compose.onNodeWithTag("new").captureToImage().toPixelMap()
        val x = new.width / 2
        val y = new.height / 2
        // Under the lower rim, over the same neutral glass body.
        assertTrue("The lower rim must be brighter", new[x, y + 21].red > old[x, y + 21].red + 0.005f)
        // Both close and far outside the lens: reduce density without deleting separation.
        val background = Color(0xFFF3F3F3).red
        val near = background - new[x, y + 25].red
        val oldNear = background - old[x, y + 25].red
        assertTrue("Keep a visible soft shadow", near > 0.003f)
        assertTrue("Remove at least a third of the close halo", near < oldNear * 0.67f)
        val far = background - new[x, y + 33].red
        val oldFar = background - old[x, y + 33].red
        assertTrue("Distant haze should fade more quickly", far < oldFar * 0.67f)
    }

    @Composable private fun sample(tag: String, width: Dp, shape: Shape, previous: Boolean) {
        val edge = GlassEdgeOpticsSpec()
        val shadow = if (previous) Modifier.previousShadow(shape)
            else Modifier.drawSoftGlassShadow(shape, isDark = false, alpha = 1f, useOfficialMiuixGlass = false)
        val brush = if (previous) Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.95f),
            0.38f to Color.White.copy(alpha = 0.95f * 0.62f),
            0.78f to Color.Black.copy(alpha = 0.95f * 0.055f),
            1f to Color.White.copy(alpha = 0.95f * 0.55f),
        ) else edge.edgeBrush(isDark = false, optical = true)
        Box(Modifier.size(width + 80.dp, 124.dp).background(Color(0xFFF3F3F3)).testTag(tag),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(width, 44.dp).then(shadow)
                .background(Color(0xFFE9E9E9), shape)
                .border(if (previous) 0.6.dp else edge.edgeStrokeWidth(false), brush, shape))
        }
    }

    // Frozen previous look, so the comparison fails if the old heavy halo returns.
    private fun Modifier.previousShadow(shape: Shape): Modifier =
        dropShadow(shape, Shadow(radius = 24.dp, spread = (-1).dp, color = Color.Black, offset = DpOffset.Zero, alpha = 0.045f))
            .dropShadow(shape, Shadow(radius = 12.dp, spread = (-0.5).dp, color = Color.Black, offset = DpOffset.Zero, alpha = 0.10f))
            .dropShadow(shape, Shadow(radius = 4.5.dp, spread = (-0.5).dp, color = Color.Black, offset = DpOffset.Zero, alpha = 0.020f))
}
