// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

// Split verbatim from upstream GlassStyles.kt (PR #423, ec4c96963abf).
internal object GlassStylesTokens2 {

    /** The `Glass_Common_Medium_Bold_Light` token. */
    @Stable
    val CommonMediumBoldLight: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.0f,
            curveC = 0.0f,
            curveD = 1.0f,
            amount = 0.5f,
            saturation = 1.2f,
            brightness = 0.06f,
            darker = 0.1f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 2.0f,
            colorWhite = 0.0f,
            colorMix = 0.3f,
            colorPow = 1.2f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 72.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.8f),
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
        blur = GlassBlur(small = 40.0f, big = 500.0f),
    )

    /** The `Glass_Common_Medium_Bold_Dark` token. */
    @Stable
    val CommonMediumBoldDark: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.0f,
            curveC = 0.0f,
            curveD = 1.0f,
            amount = 0.2f,
            saturation = 2.0f,
            brightness = 0.14f,
            darker = 0.1f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.02f,
            tint = Color(0.27f, 0.27f, 0.27f),
            tintStrength = 0.6f,
            colorWhite = 0.0f,
            colorMix = 0.2f,
            colorPow = 1.2f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 72.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.8f),
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

    /** The `Glass_Common_Medium_Bold_Low` token. */
    @Stable
    val CommonMediumBoldLow: GlassStyle = GlassStyle(
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
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.1f,
            colorWhite = 0.26f,
            colorMix = 0.3f,
            colorPow = 1.8f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 50.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 1000.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.8f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 0.8f,
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
        blur = GlassBlur(small = 40.0f, big = 500.0f),
    )

    /** The `Glass_Common_Large_Regular` token. */
    @Stable
    val CommonLargeRegular: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.0f,
            curveB = 2.0f,
            curveC = 0.5f,
            curveD = 0.8f,
            amount = 0.15f,
            saturation = 2.2f,
            brightness = 0.08f,
            darker = 0.2f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.1f,
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
            intensity = 1.6f,
            oppositeIntensity = 0.8f,
            angleRange = 1.0f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 80.0f, big = 500.0f),
    )

    /** The `Glass_Common_Large_Demibold` token. */
    @Stable
    val CommonLargeDemibold: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 1.55f,
            curveB = 2.0f,
            curveC = 0.5f,
            curveD = 0.8f,
            amount = 0.56f,
            saturation = 2.0f,
            brightness = 0.18f,
            darker = 0.26f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 1.0f,
            colorWhite = 0.6f,
            colorMix = 0.9f,
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
        blur = GlassBlur(small = 50.0f, big = 500.0f),
    )

    /** The `Glass_Bionic_Medium_Thin` token. */
    @Stable
    val BionicMediumThin: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.0f,
            curveB = 2.0f,
            curveC = 0.5f,
            curveD = 0.8f,
            amount = 0.15f,
            saturation = 1.4f,
            brightness = 0.08f,
            darker = 0.2f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.15f,
            colorWhite = 0.15f,
            colorMix = 0.4f,
            colorPow = 1.2f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 60.0f,
            reflectOffset = 1000.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.8f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 1.2f,
            oppositeIntensity = 0.8f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 2.4f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 40.0f, big = 500.0f),
    )

    /** The `Glass_Bionic_Medium_Normal` token. */
    @Stable
    val BionicMediumNormal: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.42f,
            curveB = 0.22f,
            curveC = 0.24f,
            curveD = 0.0f,
            amount = 0.24f,
            saturation = 1.4f,
            brightness = -0.02f,
            darker = 0.3f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.1f,
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
            intensity = 1.6f,
            oppositeIntensity = 0.8f,
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

    /** The `Glass_Bionic_Medium_Normal_Motion` token. */
    @Stable
    val BionicMediumNormalMotion: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.42f,
            curveB = 0.22f,
            curveC = 0.24f,
            curveD = 0.0f,
            amount = 0.24f,
            saturation = 1.4f,
            brightness = -0.02f,
            darker = 0.3f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.1f,
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
            intensity = 1.6f,
            oppositeIntensity = 0.8f,
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

    /** The `Glass_Bionic_Medium_Regular` token. */
    @Stable
    val BionicMediumRegular: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.78f,
            curveB = 0.36f,
            curveC = 0.0f,
            curveD = 1.0f,
            amount = 0.3f,
            saturation = 1.8f,
            brightness = -0.18f,
            darker = 0.1f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.1f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.15f,
            colorWhite = 0.16f,
            colorMix = 0.3f,
            colorPow = 1.1f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 80.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.8f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 2.6f,
            oppositeIntensity = 1.2f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 2.0f,
            brightness = -0.2f,
            burn = 1.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 36.0f, big = 500.0f),
    )

    /** The `Glass_Bionic_Medium_Demibold_Motion` token. */
    @Stable
    val BionicMediumDemiboldMotion: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.42f,
            curveB = 0.22f,
            curveC = 0.24f,
            curveD = 0.0f,
            amount = 0.2f,
            saturation = 2.0f,
            brightness = -0.02f,
            darker = 0.3f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.2f,
            colorWhite = 0.0f,
            colorMix = 0.2f,
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
}
