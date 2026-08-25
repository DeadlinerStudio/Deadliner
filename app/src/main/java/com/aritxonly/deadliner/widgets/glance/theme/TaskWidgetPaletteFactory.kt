package com.aritxonly.deadliner.widgets.glance.theme

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.aritxonly.deadliner.model.AppearanceDesignMode
import com.aritxonly.deadliner.model.AppearancePreferences
import com.aritxonly.deadliner.model.ModernColorPalette

internal object TaskWidgetPaletteFactory {
    fun create(
        appearance: AppearancePreferences,
        isDark: Boolean,
        systemAccent: Int,
        devicePalette: ModernColorPalette,
    ): TaskWidgetColors {
        val accent = parseSeedColor(appearance.seedColorHex) ?: systemAccent
        return when (appearance.designMode) {
            AppearanceDesignMode.Modern -> modern(
                palette = if (appearance.modernUseDevicePaletteStrategy) {
                    devicePalette
                } else {
                    appearance.modernColorPalette
                },
                accent = accent,
                isDark = isDark,
            )

            AppearanceDesignMode.Vivid -> vivid(accent = accent, isDark = isDark)
        }
    }

    private fun modern(
        palette: ModernColorPalette,
        accent: Int,
        isDark: Boolean,
    ): TaskWidgetColors {
        val neutrals = modernNeutrals(palette, isDark)
        return TaskWidgetColors(
            backgroundStart = neutrals.surface,
            backgroundEnd = blend(neutrals.surface, accent, if (isDark) 0.18f else 0.09f),
            decoration = accent,
            itemBackground = withAlpha(neutrals.container, if (isDark) 0.88f else 0.82f),
            brand = accent,
            urgent = if (isDark) 0xFFFF8A80.toInt() else 0xFFD93025.toInt(),
            textPrimary = neutrals.text,
            textSecondary = neutrals.textSecondary,
        )
    }

    private fun vivid(accent: Int, isDark: Boolean): TaskWidgetColors {
        val base = if (isDark) Color.BLACK else Color.WHITE
        val textPrimary = if (isDark) 0xFFF7F2FA.toInt() else 0xFF211A1D.toInt()
        val textSecondary = if (isDark) 0xFFD0C4C8.toInt() else 0xFF6F5A61.toInt()
        return TaskWidgetColors(
            backgroundStart = blend(base, accent, if (isDark) 0.24f else 0.13f),
            backgroundEnd = blend(base, accent, if (isDark) 0.43f else 0.27f),
            decoration = accent,
            itemBackground = withAlpha(
                if (isDark) 0xFF241D20.toInt() else Color.WHITE,
                if (isDark) 0.82f else 0.76f,
            ),
            brand = accent,
            urgent = if (isDark) 0xFFFFB4AB.toInt() else 0xFFBA1A1A.toInt(),
            textPrimary = textPrimary,
            textSecondary = textSecondary,
        )
    }

    private fun modernNeutrals(palette: ModernColorPalette, isDark: Boolean): ModernNeutrals {
        val configured = if (isDark) palette.dark else palette.light
        if (configured != null) {
            return ModernNeutrals(
                surface = configured.surfaceHex.toColorInt(),
                container = configured.surfaceContainerHex.toColorInt(),
                text = configured.textHex.toColorInt(),
                textSecondary = configured.textSecondaryHex.toColorInt(),
            )
        }
        return if (isDark) {
            ModernNeutrals(
                surface = Color.BLACK,
                container = 0xFF1C1C1E.toInt(),
                text = 0xFFF2F2F7.toInt(),
                textSecondary = 0xFFAEAEB2.toInt(),
            )
        } else {
            ModernNeutrals(
                surface = Color.WHITE,
                container = Color.WHITE,
                text = 0xFF191919.toInt(),
                textSecondary = 0xFF6F6F6F.toInt(),
            )
        }
    }

    private fun parseSeedColor(raw: String?): Int? {
        val normalized = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching {
            Color.parseColor(if (normalized.startsWith('#')) normalized else "#$normalized")
        }.getOrNull()
    }

    private fun String.toColorInt(): Int = Color.parseColor("#$this")

    private fun blend(base: Int, overlay: Int, ratio: Float): Int =
        ColorUtils.blendARGB(base, overlay, ratio)

    private fun withAlpha(color: Int, alpha: Float): Int =
        ColorUtils.setAlphaComponent(color, (alpha.coerceIn(0f, 1f) * 255).toInt())

    private data class ModernNeutrals(
        val surface: Int,
        val container: Int,
        val text: Int,
        val textSecondary: Int,
    )
}
