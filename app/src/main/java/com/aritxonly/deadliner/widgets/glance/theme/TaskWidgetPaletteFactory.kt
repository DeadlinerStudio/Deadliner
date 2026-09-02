package com.aritxonly.deadliner.widgets.glance.theme

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.aritxonly.deadliner.model.AppearanceDesignMode
import com.aritxonly.deadliner.model.AppearancePreferences
import com.aritxonly.deadliner.model.ModernColorPalette
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetVisualState

internal object TaskWidgetPaletteFactory {
    fun create(
        appearance: AppearancePreferences,
        isDark: Boolean,
        systemAccent: Int,
        devicePalette: ModernColorPalette,
        visualState: TaskWidgetVisualState = TaskWidgetVisualState.NORMAL,
    ): TaskWidgetColors {
        val accent = parseSeedColor(appearance.seedColorHex) ?: systemAccent
        val neutrals = when (appearance.designMode) {
            AppearanceDesignMode.Modern -> modernNeutrals(
                palette = if (appearance.modernUseDevicePaletteStrategy) {
                    devicePalette
                } else {
                    appearance.modernColorPalette
                },
                isDark = isDark,
            )

            AppearanceDesignMode.Vivid -> vividNeutrals(accent = accent, isDark = isDark)
        }
        return createColors(
            neutrals = neutrals,
            accent = accent,
            isDark = isDark,
            visualState = visualState,
            vivid = appearance.designMode == AppearanceDesignMode.Vivid,
        )
    }

    private fun createColors(
        neutrals: WidgetNeutrals,
        accent: Int,
        isDark: Boolean,
        visualState: TaskWidgetVisualState,
        vivid: Boolean,
    ): TaskWidgetColors {
        // MIUIX has an error role but no warning/success family. Near and completed are
        // therefore explicit Deadliner semantic colors instead of invented theme tokens.
        val overdue = if (isDark) 0xFFF12522.toInt() else 0xFFE94634.toInt()
        val near = if (isDark) 0xFFFF9F0A.toInt() else 0xFFFF9500.toInt()
        val completed = if (isDark) 0xFF30D158.toInt() else 0xFF34C759.toInt()
        val stateAccent = when (visualState) {
            TaskWidgetVisualState.EMPTY -> completed
            TaskWidgetVisualState.OVERDUE -> overdue
            TaskWidgetVisualState.NEAR -> near
            TaskWidgetVisualState.NORMAL -> accent
        }
        val tintRatio = when {
            isDark && vivid -> 0.12f
            isDark -> 0.07f
            vivid -> 0.11f
            else -> 0.075f
        }
        val normalBase = blend(neutrals.surface, neutrals.container, if (isDark) 0.84f else 0.92f)
        val normalHighlight = withAlpha(Color.WHITE, if (isDark) 0.03f else 0.42f)
        val normalStroke = withAlpha(Color.WHITE, if (isDark) 0.08f else 0.74f)

        return TaskWidgetColors(
            isDark = isDark,
            backgroundBase = neutrals.surface,
            backgroundTint = blend(neutrals.surface, stateAccent, tintRatio),
            backgroundGlow = withAlpha(Color.WHITE, if (isDark) 0.02f else 0.12f),
            backgroundHighlight = withAlpha(Color.WHITE, if (isDark) 0.04f else 0.30f),
            decoration = stateAccent,
            badgeBackground = withAlpha(if (isDark) Color.WHITE else Color.BLACK, if (isDark) 0.05f else 0.06f),
            brand = accent,
            brandSoft = withAlpha(accent, if (isDark) 0.72f else 0.66f),
            near = near,
            overdue = overdue,
            completed = completed,
            textPrimary = neutrals.text,
            textSecondary = neutrals.textSecondary,
            normalRow = TaskWidgetRowColors(
                base = normalBase,
                tint = withAlpha(accent, if (isDark) 0.018f else 0.03f),
                highlight = normalHighlight,
                stroke = normalStroke,
                indicator = accent,
                timeText = neutrals.textSecondary,
            ),
            nearRow = statusRow(
                surface = neutrals.surface,
                normalBase = normalBase,
                statusColor = near,
                isDark = isDark,
            ),
            overdueRow = statusRow(
                surface = neutrals.surface,
                normalBase = normalBase,
                statusColor = overdue,
                isDark = isDark,
            ),
        )
    }

    private fun statusRow(
        surface: Int,
        normalBase: Int,
        statusColor: Int,
        isDark: Boolean,
    ): TaskWidgetRowColors = TaskWidgetRowColors(
        base = if (isDark) blend(surface, statusColor, 0.10f) else normalBase,
        tint = withAlpha(statusColor, if (isDark) 0.07f else 0.08f),
        highlight = withAlpha(Color.WHITE, if (isDark) 0.04f else 0.24f),
        stroke = withAlpha(statusColor, if (isDark) 0.28f else 0.20f),
        indicator = statusColor,
        timeText = statusColor,
    )

    private fun modernNeutrals(palette: ModernColorPalette, isDark: Boolean): WidgetNeutrals {
        val configured = if (isDark) palette.dark else palette.light
        if (configured != null) {
            return WidgetNeutrals(
                surface = configured.surfaceHex.toColorInt(),
                container = configured.surfaceContainerHex.toColorInt(),
                text = configured.textHex.toColorInt(),
                textSecondary = configured.textSecondaryHex.toColorInt(),
            )
        }
        return if (isDark) {
            WidgetNeutrals(
                surface = Color.BLACK,
                container = 0xFF242424.toInt(),
                text = 0xFFF2F2F7.toInt(),
                textSecondary = 0x80FFFFFF.toInt(),
            )
        } else {
            WidgetNeutrals(
                surface = Color.WHITE,
                container = Color.WHITE,
                text = 0xFF191919.toInt(),
                textSecondary = 0x99000000.toInt(),
            )
        }
    }

    private fun vividNeutrals(accent: Int, isDark: Boolean): WidgetNeutrals {
        val surface = if (isDark) 0xFF08090C.toInt() else Color.WHITE
        return WidgetNeutrals(
            surface = blend(surface, accent, if (isDark) 0.06f else 0.025f),
            container = blend(surface, accent, if (isDark) 0.12f else 0.055f),
            text = if (isDark) 0xFFF7F7FA.toInt() else 0xEB000000.toInt(),
            textSecondary = if (isDark) 0xADFFFFFF.toInt() else 0x85000000.toInt(),
        )
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

    private data class WidgetNeutrals(
        val surface: Int,
        val container: Int,
        val text: Int,
        val textSecondary: Int,
    )
}
