// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

// Split verbatim from upstream GlassStyles.kt (PR #423, ec4c96963abf).
internal object GlassStylesTokens3 {

    /** The `Glass_Bionic_Medium_Semibold_Motion` token. */
    @Stable
    val BionicMediumSemiboldMotion: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 1.3f,
            curveB = 0.64f,
            curveC = 0.24f,
            curveD = 0.0f,
            amount = 0.52f,
            saturation = 2.0f,
            brightness = -0.02f,
            darker = 0.3f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 1.4f,
            colorWhite = 0.15f,
            colorMix = 0.9f,
            colorPow = 0.4f,
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
            oppositeIntensity = 2.0f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 4.0f),
        background = GlassBackground(
            saturation = 2.6f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 120.0f, big = 120.0f),
    )

    /** The `Glass_Tint_Purple` token. */
    @Stable
    val TintPurple: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.2f,
            curveC = 0.06f,
            curveD = 1.0f,
            amount = 0.3f,
            saturation = 1.0f,
            brightness = 0.6f,
            darker = 0.2f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.0f,
            tint = Color(0.44f, 0.37f, 1.0f),
            tintStrength = 0.6f,
            colorWhite = 0.5f,
            colorMix = 0.8f,
            colorPow = 1.6f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.6f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 1.4f,
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
        blur = GlassBlur(small = 36.0f, big = 500.0f),
    )

    /** The `Glass_Tint_Orange` token. */
    @Stable
    val TintOrange: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.2f,
            curveC = 0.06f,
            curveD = 1.0f,
            amount = 0.3f,
            saturation = 1.0f,
            brightness = 0.6f,
            darker = 0.2f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.0f,
            tint = Color(0.99f, 0.41f, 0.15f),
            tintStrength = 0.6f,
            colorWhite = 0.5f,
            colorMix = 0.8f,
            colorPow = 1.6f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.6f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 1.0f,
            oppositeIntensity = 0.8f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 36.0f, big = 500.0f),
    )

    /** The `Glass_Tint_Blue` token. */
    @Stable
    val TintBlue: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.2f,
            curveC = 0.06f,
            curveD = 1.0f,
            amount = 0.3f,
            saturation = 1.0f,
            brightness = 0.6f,
            darker = 0.2f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(0.35f, 0.45f, 0.65f),
            tintStrength = 0.8f,
            colorWhite = 0.5f,
            colorMix = 0.6f,
            colorPow = 1.45f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 0.75f, strength = 0.8f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 1.4f,
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
        blur = GlassBlur(small = 36.0f, big = 500.0f),
    )

    /** The `Glass_Tint_Yellow` token. */
    @Stable
    val TintYellow: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.2f,
            curveC = 0.06f,
            curveD = 1.0f,
            amount = 0.3f,
            saturation = 1.0f,
            brightness = 0.6f,
            darker = 0.2f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(0.99f, 0.78f, 0.15f),
            tintStrength = 0.6f,
            colorWhite = 0.5f,
            colorMix = 0.6f,
            colorPow = 1.45f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 0.75f, strength = 0.8f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 1.0f,
            oppositeIntensity = 0.6f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 36.0f, big = 500.0f),
    )

    /** The `Glass_Tint_Brown` token. */
    @Stable
    val TintBrown: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.2f,
            curveC = 0.06f,
            curveD = 1.0f,
            amount = 0.3f,
            saturation = 2.0f,
            brightness = 0.6f,
            darker = 0.2f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(0.38f, 0.26f, 0.22f),
            tintStrength = 0.8f,
            colorWhite = 0.5f,
            colorMix = 0.6f,
            colorPow = 1.45f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 0.75f, strength = 0.8f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 1.8f,
            oppositeIntensity = 1.2f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 36.0f, big = 500.0f),
    )

    /** The `Glass_Tint_Black_Thin` token. */
    @Stable
    val TintBlackThin: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.0f,
            curveB = 2.0f,
            curveC = 0.5f,
            curveD = 0.8f,
            amount = 0.15f,
            saturation = 2.0f,
            brightness = 0.18f,
            darker = 0.26f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(0.0f, 0.0f, 0.0f),
            tintStrength = 0.3f,
            colorWhite = 0.15f,
            colorMix = 0.4f,
            colorPow = 1.2f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 50.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.8f),
        light = GlassLight(
            directionX = -0.6f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 2.0f,
            oppositeIntensity = 1.6f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 50.0f, big = 500.0f),
    )

    /** The `Glass_Tint_Black_Normal` token. */
    @Stable
    val TintBlackNormal: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.0f,
            curveB = 2.0f,
            curveC = 0.5f,
            curveD = 0.8f,
            amount = 0.15f,
            saturation = 2.0f,
            brightness = 0.3f,
            darker = 0.2f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.02f,
            tint = Color(0.06f, 0.06f, 0.06f),
            tintStrength = 0.5f,
            colorWhite = 0.15f,
            colorMix = 0.4f,
            colorPow = 1.36f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 72.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 1000.0f,
        ),
        reflect = GlassReflect(lighten = 1.4f, strength = 0.8f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 2.6f,
            oppositeIntensity = 1.6f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 3.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 40.0f, big = 500.0f),
    )

    /** The `Glass_Tint_Black_Regular` token. */
    @Stable
    val TintBlackRegular: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.42f,
            curveB = 0.22f,
            curveC = 0.24f,
            curveD = 0.0f,
            amount = 0.2f,
            saturation = 1.4f,
            brightness = 0.1f,
            darker = 0.3f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.02f,
            tint = Color(0.06f, 0.06f, 0.06f),
            tintStrength = 0.7f,
            colorWhite = 0.2f,
            colorMix = 0.3f,
            colorPow = 1.0f,
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
            intensity = 3.4f,
            oppositeIntensity = 2.6f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 4.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 120.0f, big = 120.0f),
    )

    /** The `Glass_Tint_Black_Demibold` token. */
    @Stable
    val TintBlackDemibold: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.0f,
            curveB = 2.0f,
            curveC = 0.5f,
            curveD = 0.8f,
            amount = 0.15f,
            saturation = 3.0f,
            brightness = 0.3f,
            darker = 0.2f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.0f,
            tint = Color(0.06f, 0.06f, 0.06f),
            tintStrength = 0.6f,
            colorWhite = 0.15f,
            colorMix = 0.4f,
            colorPow = 1.36f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 1000.0f,
        ),
        reflect = GlassReflect(lighten = 1.2f, strength = 0.6f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 3.0f,
            oppositeIntensity = 2.0f,
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
}
