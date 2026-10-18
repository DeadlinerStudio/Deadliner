// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

// Split verbatim from upstream GlassStyles.kt (PR #423, ec4c96963abf).
internal object GlassStylesTokens1 {

    /** The `Glass_Common_Small_Thin` token. */
    @Stable
    val CommonSmallThin: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.0f,
            curveC = 0.0f,
            curveD = 1.0f,
            amount = 0.2f,
            saturation = 2.0f,
            brightness = 0.24f,
            darker = 0.26f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.15f,
            colorWhite = 0.0f,
            colorMix = 0.4f,
            colorPow = 1.5f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 50.0f,
            pow = 4.0f,
            thickness = 60.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 2.0f, strength = 0.6f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 1.4f,
            oppositeIntensity = 0.8f,
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

    /** The `Glass_Common_Small_Normal` token. */
    @Stable
    val CommonSmallNormal: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.0f,
            curveB = 2.0f,
            curveC = 0.5f,
            curveD = 0.8f,
            amount = 0.15f,
            saturation = 2.0f,
            brightness = 0.26f,
            darker = 0.26f,
            darkerStart = 0.6f,
            darkerEnd = 1.0f,
        ),
        inner = GlassInner(
            bottom = 0.0f,
            tint = Color(0.0f, 0.0f, 0.0f),
            tintStrength = 0.42f,
            colorWhite = 0.15f,
            colorMix = 0.4f,
            colorPow = 1.2f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 16.0f,
            pow = 1.4f,
            thickness = 100.0f,
            reflectOffset = 1000.0f,
        ),
        reflect = GlassReflect(lighten = 0.75f, strength = 1.5f),
        light = GlassLight(
            directionX = 0.6f,
            directionY = -1.0f,
            directionZ = 0.0f,
            intensity = 1.0f,
            oppositeIntensity = 1.4f,
            angleRange = 0.8f,
        ),
        refract = GlassRefract(ior = 3.0f),
        background = GlassBackground(
            saturation = 0.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 150.0f, big = 500.0f),
    )

    /** The `Glass_Common_Small_Regular` token. */
    @Stable
    val CommonSmallRegular: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.0f,
            curveC = 0.8f,
            curveD = 1.2f,
            amount = 0.24f,
            saturation = 1.4f,
            brightness = 0.14f,
            darker = 0.2f,
            darkerStart = 0.6f,
            darkerEnd = 0.9f,
        ),
        inner = GlassInner(
            bottom = 0.01f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.05f,
            colorWhite = 0.3f,
            colorMix = 0.5f,
            colorPow = 1.5f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 18.0f,
            pow = 1.6f,
            thickness = 100.0f,
            reflectOffset = 110.0f,
        ),
        reflect = GlassReflect(lighten = 0.8f, strength = 1.0f),
        light = GlassLight(
            directionX = -0.5f,
            directionY = 0.5f,
            directionZ = 0.0f,
            intensity = 1.1f,
            oppositeIntensity = 0.5f,
            angleRange = 0.3f,
        ),
        refract = GlassRefract(ior = 1.4f),
        background = GlassBackground(
            saturation = 1.0f,
            brightness = 0.08f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 8.0f, big = 500.0f),
    )

    /** The `Glass_Common_Small_Semibold` token. */
    @Stable
    val CommonSmallSemibold: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.0f,
            curveC = 0.0f,
            curveD = 1.0f,
            amount = 0.2f,
            saturation = 2.6f,
            brightness = 0.22f,
            darker = 0.26f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.0f,
            colorWhite = 0.0f,
            colorMix = 0.4f,
            colorPow = 0.97f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 60.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 2.0f, strength = 0.8f),
        light = GlassLight(
            directionX = -0.5f,
            directionY = 0.5f,
            directionZ = -0.8f,
            intensity = 1.2f,
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

    /** The `Glass_Common_Small_Bold` token. */
    @Stable
    val CommonSmallBold: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.42f,
            curveB = 0.22f,
            curveC = 0.24f,
            curveD = 0.0f,
            amount = 0.24f,
            saturation = 1.4f,
            brightness = 0.28f,
            darker = 0.4f,
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

    /** The `Glass_Common_Medium_Thin_Low` token. */
    @Stable
    val CommonMediumThinLow: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.0f,
            curveC = 0.0f,
            curveD = 1.0f,
            amount = 0.2f,
            saturation = 1.5f,
            brightness = 0.2f,
            darker = 0.1f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.05f,
            colorWhite = 0.0f,
            colorMix = 0.3f,
            colorPow = 1.2f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 60.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.6f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 1.2f,
            oppositeIntensity = 0.8f,
            angleRange = 1.4f,
        ),
        refract = GlassRefract(ior = 2.0f),
        background = GlassBackground(
            saturation = 1.0f,
            brightness = 0.0f,
            burn = 0.0f,
            unShade = 0.0f,
        ),
        blur = GlassBlur(small = 36.0f, big = 500.0f),
    )

    /** The `Glass_Common_Medium_Thin_High` token. */
    @Stable
    val CommonMediumThinHigh: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.6f,
            curveC = 0.0f,
            curveD = 0.3f,
            amount = 0.2f,
            saturation = 1.5f,
            brightness = 0.24f,
            darker = 0.1f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.05f,
            tint = Color(1.0f, 1.0f, 1.0f),
            tintStrength = 0.1f,
            colorWhite = 0.0f,
            colorMix = 0.3f,
            colorPow = 1.2f,
            alpha = 1.0f,
        ),
        edge = GlassEdge(
            width = 60.0f,
            pow = 4.0f,
            thickness = 60.0f,
            reflectOffset = 600.0f,
        ),
        reflect = GlassReflect(lighten = 1.0f, strength = 0.6f),
        light = GlassLight(
            directionX = -0.4f,
            directionY = 0.6f,
            directionZ = -0.8f,
            intensity = 0.9f,
            oppositeIntensity = 0.3f,
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

    /** The `Glass_Common_Medium_Regular_Low_Light` token. */
    @Stable
    val CommonMediumRegularLowLight: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.0f,
            curveC = 0.0f,
            curveD = 1.0f,
            amount = 0.2f,
            saturation = 2.0f,
            brightness = 0.2f,
            darker = 0.0f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.04f,
            tint = Color(0.9f, 0.9f, 0.9f),
            tintStrength = 1.0f,
            colorWhite = 0.0f,
            colorMix = 1.0f,
            colorPow = 1.0f,
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
            intensity = 1.4f,
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

    /** The `Glass_Common_Medium_Regular_High_Light` token. */
    @Stable
    val CommonMediumRegularHighLight: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.0f,
            curveC = 0.0f,
            curveD = 1.0f,
            amount = 0.2f,
            saturation = 2.0f,
            brightness = 0.1f,
            darker = 0.0f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.02f,
            tint = Color(0.9f, 0.9f, 0.9f),
            tintStrength = 1.0f,
            colorWhite = 0.0f,
            colorMix = 1.0f,
            colorPow = 1.0f,
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
            intensity = 1.4f,
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

    /** The `Glass_Common_Medium_Regular_Dark` token. */
    @Stable
    val CommonMediumRegularDark: GlassStyle = GlassStyle(
        blend = GlassBlend(
            curveA = 0.5f,
            curveB = 1.0f,
            curveC = 0.0f,
            curveD = 1.0f,
            amount = 0.2f,
            saturation = 3.0f,
            brightness = 0.2f,
            darker = 0.0f,
            darkerStart = 0.6f,
            darkerEnd = 0.8f,
        ),
        inner = GlassInner(
            bottom = 0.04f,
            tint = Color(0.06f, 0.06f, 0.06f),
            tintStrength = 0.4f,
            colorWhite = 0.0f,
            colorMix = 0.8f,
            colorPow = 1.5f,
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
            intensity = 2.4f,
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
        blur = GlassBlur(small = 36.0f, big = 500.0f),
    )
}
