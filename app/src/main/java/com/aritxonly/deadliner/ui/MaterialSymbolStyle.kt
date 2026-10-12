package com.aritxonly.deadliner.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.aritxonly.deadliner.R
import kotlin.math.min

/** Central Material Symbols axis tuning point. */
internal object MaterialSymbolAxisPolicy {
    const val DefaultWeight = 400
    const val DefaultGrade = 0
    const val ImmersiveModernWeight = 400
    const val ImmersiveModernGrade = -80
    const val OpticalSize = 24
}

internal data class MaterialSymbolStyle(
    val weight: Int = MaterialSymbolAxisPolicy.DefaultWeight,
    val grade: Int = MaterialSymbolAxisPolicy.DefaultGrade,
    val opticalSize: Int = MaterialSymbolAxisPolicy.OpticalSize,
    val usesModernBackIcon: Boolean = false,
)

internal val LocalMaterialSymbolStyle = staticCompositionLocalOf { MaterialSymbolStyle() }

/**
 * Renders a Material Symbol directly from the official variable font.
 * Resource IDs remain the call-site key so brand drawables can continue to
 * use [painterResource] without a separate API.
 */
@Composable
fun symbolPainterResource(@DrawableRes sourceId: Int): Painter {
    val definition = MaterialSymbolRegistry.definitionFor(sourceId) ?: return painterResource(sourceId)
    val context = androidx.compose.ui.platform.LocalContext.current
    val style = LocalMaterialSymbolStyle.current
    val symbolName = if (style.usesModernBackIcon) definition.modernName ?: definition.name else definition.name
    return remember(symbolName, definition.filled, style) {
        MaterialSymbolFontPainter(
            context = context.applicationContext,
            symbolName = symbolName,
            filled = definition.filled,
            style = style,
        )
    }
}

/** Named symbols for transient actions, rendered through the same project font and axes. */
@Composable
fun materialSymbolPainter(name: String): Painter {
    val context = androidx.compose.ui.platform.LocalContext.current
    val style = LocalMaterialSymbolStyle.current
    return remember(name, style) {
        MaterialSymbolFontPainter(context.applicationContext, name, false, style)
    }
}

private class MaterialSymbolFontPainter(
    context: Context,
    private val symbolName: String,
    private val filled: Boolean,
    private val style: MaterialSymbolStyle,
) : Painter() {
    private val typeface: Typeface = context.resources.getFont(R.font.material_symbols_rounded_variable)

    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val glyphSize = min(size.width, size.height)
        if (glyphSize <= 0f) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = this@MaterialSymbolFontPainter.typeface
            color = android.graphics.Color.WHITE
            textSize = glyphSize
            textAlign = Paint.Align.CENTER
            fontFeatureSettings = "liga"
            fontVariationSettings = "'FILL' ${if (filled) 1 else 0}, 'wght' ${this@MaterialSymbolFontPainter.style.weight}, " +
                "'GRAD' ${this@MaterialSymbolFontPainter.style.grade}, 'opsz' ${this@MaterialSymbolFontPainter.style.opticalSize}"
        }
        val baseline = size.height / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()
            if (layoutDirection == LayoutDirection.Rtl) {
                nativeCanvas.scale(-1f, 1f, size.width / 2f, size.height / 2f)
            }
            if (symbolName == "arrow_back_ios_new") {
                nativeCanvas.scale(0.92f, 0.92f, size.width / 2f, size.height / 2f)
                nativeCanvas.translate(-glyphSize * 30f / 960f, 0f)
            }
            nativeCanvas.drawText(symbolName, size.width / 2f, baseline, paint)
            nativeCanvas.restore()
        }
    }
}
