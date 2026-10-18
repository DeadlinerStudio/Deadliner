package com.aritxonly.deadliner.ui.material.glass

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDefaults
import top.yukonga.miuix.kmp.glass.GlassMaterials
import top.yukonga.miuix.kmp.glass.GlassShape
import top.yukonga.miuix.kmp.glass.GlassStrokes
import top.yukonga.miuix.kmp.glass.glass

/** Application roles mapped to the unmodified material primitives from MIUIX PR #423. */
enum class GlassSurfaceRole { Control, FloatingBar, Panel, TopBar }

/** No custom shader/blur/edge parameters are fed into the official presets. */
internal fun Modifier.officialMiuixGlassMaterial(
    backdrop: Backdrop,
    shape: Shape,
    isDark: Boolean,
    role: GlassSurfaceRole,
    tint: Color = Color.Unspecified,
): Modifier {
    val glassShape = when (shape) {
        is GlassShape -> shape
        is CornerBasedShape -> GlassShape(shape.topStart, shape.topEnd, shape.bottomEnd, shape.bottomStart)
        RectangleShape -> GlassShape(0.dp)
        // App capsule shapes use half the minimum dimension; CornerSize(50) has that contract.
        else -> GlassShape(CornerSize(50), CornerSize(50), CornerSize(50), CornerSize(50))
    }
    val material = when (role) {
        GlassSurfaceRole.Control -> null
        GlassSurfaceRole.FloatingBar -> GlassMaterials.puredThinGlass(isDark)
        GlassSurfaceRole.Panel -> GlassMaterials.popupViewGlass(isDark)
        GlassSurfaceRole.TopBar -> GlassMaterials.actionBarMask(isDark)
    }
    return glass(
        backdrop = backdrop,
        shape = glassShape,
        style = GlassDefaults.style(isDark),
        material = material,
        stroke = if (role == GlassSurfaceRole.TopBar) null else GlassStrokes.forTheme(isDark),
        shading = role == GlassSurfaceRole.Control,
        tint = tint,
    )
}
