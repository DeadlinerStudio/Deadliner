// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

// Split verbatim from upstream GlassStyles.kt (PR #423, ec4c96963abf).
internal object GlassStylesTokens4 {

    /** The `Glass_Tint_Black_Heavy` token. */
    @Stable
    val TintBlackHeavy: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 2.0f,
            curveB = 2.0f,
            curveC = 0.5f,
            curveD = 0.8f,
            amount = 0.2f,
            saturation = 1.2f,
            brightness = 0.0f,
            darker = 0.4f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.02f,
            tint = Color(0.06f, 0.06f, 0.06f),
            tintStrength = 0.4f,
            colorWhite = 0.26f,
            colorMix = 0.3f,
            colorPow = 1.8f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 72.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 1000.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.8f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 1.6f,
            oppositeIntensity = 1.0f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 40.0f, big = 500.0f),
    )

    /** The `Glass_Tint_Red_Motion` token. */
    @Stable
    val TintRedMotion: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.42f,
            curveB = 0.22f,
            curveC = 0.24f,
            curveD = 0.0f,
            amount = 0.8f,
            saturation = 2.0f,
            brightness = 0.2f,
            darker = 0.6f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.06f,
            tint = Color(1.0f, 0.31f, 0.27f),
            tintStrength = 1.0f,
            colorWhite = 0.3f,
            colorMix = 1.0f,
            colorPow = 1.2f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 1200.0f,
        ),
        reflect = GlassReflect(lighten = 1.2f, strength = 1.0f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 3.0f,
            oppositeIntensity = 1.0f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 4.0f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 120.0f, big = 120.0f),
    )

    /** The `Glass_Tint_Green_Motion` token. */
    @Stable
    val TintGreenMotion: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.42f,
            curveB = 0.22f,
            curveC = 0.24f,
            curveD = 0.0f,
            amount = 0.8f,
            saturation = 2.0f,
            brightness = 0.2f,
            darker = 0.6f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.06f,
            tint = Color(0.25f, 0.84f, 0.35f),
            tintStrength = 1.0f,
            colorWhite = 0.3f,
            colorMix = 1.0f,
            colorPow = 1.2f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 1200.0f,
        ),
        reflect = GlassReflect(lighten = 1.2f, strength = 1.0f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 3.0f,
            oppositeIntensity = 1.0f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 4.0f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 120.0f, big = 120.0f),
    )

    /** The `Glass_Tint_Blue_Motion` token. */
    @Stable
    val TintBlueMotion: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.42f,
            curveB = 0.22f,
            curveC = 0.24f,
            curveD = 0.0f,
            amount = 0.8f,
            saturation = 2.0f,
            brightness = 0.2f,
            darker = 0.6f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.06f,
            tint = Color(0.2f, 0.51f, 1.0f),
            tintStrength = 1.0f,
            colorWhite = 0.3f,
            colorMix = 1.0f,
            colorPow = 1.2f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 1200.0f,
        ),
        reflect = GlassReflect(lighten = 1.2f, strength = 1.0f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 3.0f,
            oppositeIntensity = 1.0f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 4.0f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 120.0f, big = 120.0f),
    )

    /** The `Glass_Tint_Blue_Normal` token. */
    @Stable
    val TintBlueNormal: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.0f,
            curveC = 0.0f,
            curveD = 1.0f,
            amount = 0.2f,
            saturation = 2.0f,
            brightness = 1.0f,
            darker = 0.0f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.0f,
            tint = Color(0.2f, 0.51f, 1.0f),
            tintStrength = 1.5f,
            colorWhite = 0.0f,
            colorMix = 0.4f,
            colorPow = 0.92f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 60.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 2.0f, strength = 0.6f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 1.0f,
            oppositeIntensity = 0.6f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 1.5f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 30.0f, big = 500.0f),
    )
}
