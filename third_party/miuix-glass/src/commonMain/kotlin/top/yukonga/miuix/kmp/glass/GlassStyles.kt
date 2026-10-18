// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/** Official token table, mechanically split to keep source files below 1000 lines. */
object GlassStyles {
    @Stable
    val CommonSmallThin: GlassStyle get() = GlassStylesTokens1.CommonSmallThin

    @Stable
    val CommonSmallNormal: GlassStyle get() = GlassStylesTokens1.CommonSmallNormal

    @Stable
    val CommonSmallRegular: GlassStyle get() = GlassStylesTokens1.CommonSmallRegular

    @Stable
    val CommonSmallSemibold: GlassStyle get() = GlassStylesTokens1.CommonSmallSemibold

    @Stable
    val CommonSmallBold: GlassStyle get() = GlassStylesTokens1.CommonSmallBold

    @Stable
    val CommonMediumThinLow: GlassStyle get() = GlassStylesTokens1.CommonMediumThinLow

    @Stable
    val CommonMediumThinHigh: GlassStyle get() = GlassStylesTokens1.CommonMediumThinHigh

    @Stable
    val CommonMediumRegularLowLight: GlassStyle get() = GlassStylesTokens1.CommonMediumRegularLowLight

    @Stable
    val CommonMediumRegularHighLight: GlassStyle get() = GlassStylesTokens1.CommonMediumRegularHighLight

    @Stable
    val CommonMediumRegularDark: GlassStyle get() = GlassStylesTokens1.CommonMediumRegularDark

    @Stable
    val CommonMediumBoldLight: GlassStyle get() = GlassStylesTokens2.CommonMediumBoldLight

    @Stable
    val CommonMediumBoldDark: GlassStyle get() = GlassStylesTokens2.CommonMediumBoldDark

    @Stable
    val CommonMediumBoldLow: GlassStyle get() = GlassStylesTokens2.CommonMediumBoldLow

    @Stable
    val CommonLargeRegular: GlassStyle get() = GlassStylesTokens2.CommonLargeRegular

    @Stable
    val CommonLargeDemibold: GlassStyle get() = GlassStylesTokens2.CommonLargeDemibold

    @Stable
    val BionicMediumThin: GlassStyle get() = GlassStylesTokens2.BionicMediumThin

    @Stable
    val BionicMediumNormal: GlassStyle get() = GlassStylesTokens2.BionicMediumNormal

    @Stable
    val BionicMediumNormalMotion: GlassStyle get() = GlassStylesTokens2.BionicMediumNormalMotion

    @Stable
    val BionicMediumRegular: GlassStyle get() = GlassStylesTokens2.BionicMediumRegular

    @Stable
    val BionicMediumDemiboldMotion: GlassStyle get() = GlassStylesTokens2.BionicMediumDemiboldMotion

    @Stable
    val BionicMediumSemiboldMotion: GlassStyle get() = GlassStylesTokens3.BionicMediumSemiboldMotion

    @Stable
    val TintPurple: GlassStyle get() = GlassStylesTokens3.TintPurple

    @Stable
    val TintOrange: GlassStyle get() = GlassStylesTokens3.TintOrange

    @Stable
    val TintBlue: GlassStyle get() = GlassStylesTokens3.TintBlue

    @Stable
    val TintYellow: GlassStyle get() = GlassStylesTokens3.TintYellow

    @Stable
    val TintBrown: GlassStyle get() = GlassStylesTokens3.TintBrown

    @Stable
    val TintBlackThin: GlassStyle get() = GlassStylesTokens3.TintBlackThin

    @Stable
    val TintBlackNormal: GlassStyle get() = GlassStylesTokens3.TintBlackNormal

    @Stable
    val TintBlackRegular: GlassStyle get() = GlassStylesTokens3.TintBlackRegular

    @Stable
    val TintBlackDemibold: GlassStyle get() = GlassStylesTokens3.TintBlackDemibold

    @Stable
    val TintBlackHeavy: GlassStyle get() = GlassStylesTokens4.TintBlackHeavy

    @Stable
    val TintRedMotion: GlassStyle get() = GlassStylesTokens4.TintRedMotion

    @Stable
    val TintGreenMotion: GlassStyle get() = GlassStylesTokens4.TintGreenMotion

    @Stable
    val TintBlueMotion: GlassStyle get() = GlassStylesTokens4.TintBlueMotion

    @Stable
    val TintBlueNormal: GlassStyle get() = GlassStylesTokens4.TintBlueNormal

    @Stable
    fun forTheme(isDark: Boolean, light: GlassStyle, dark: GlassStyle): GlassStyle = if (isDark) dark else light
}
