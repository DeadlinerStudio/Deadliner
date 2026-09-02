package com.aritxonly.deadliner.widgets.glance.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import com.aritxonly.deadliner.widgets.glance.model.TaskWidgetStatus

internal object TaskWidgetBackgroundRenderer {
    private const val SQUARE_SIZE = 240
    private const val CAPSULE_WIDTH = 300
    private const val CAPSULE_HEIGHT = 128
    private const val ROW_WIDTH = 180
    private const val ROW_HEIGHT = 40

    fun renderSquare(colors: TaskWidgetColors): Bitmap =
        renderWidgetBackground(SQUARE_SIZE, SQUARE_SIZE, colors)

    fun renderCapsule(colors: TaskWidgetColors): Bitmap =
        renderWidgetBackground(CAPSULE_WIDTH, CAPSULE_HEIGHT, colors)

    fun renderRow(colors: TaskWidgetColors, status: TaskWidgetStatus): Bitmap {
        val row = colors.rowColors(status)
        val bitmap = Bitmap.createBitmap(ROW_WIDTH, ROW_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(1f, 1f, ROW_WIDTH - 1f, ROW_HEIGHT - 1f)
        val radius = 13f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = row.base
        canvas.drawRoundRect(bounds, radius, radius, paint)

        paint.shader = LinearGradient(
            0f,
            0f,
            ROW_WIDTH.toFloat(),
            ROW_HEIGHT.toFloat(),
            row.highlight,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(bounds, radius, radius, paint)

        paint.shader = null
        paint.color = row.tint
        canvas.drawRoundRect(bounds, radius, radius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = row.stroke
        canvas.drawRoundRect(bounds, radius, radius, paint)
        return bitmap
    }

    private fun renderWidgetBackground(
        width: Int,
        height: Int,
        colors: TaskWidgetColors,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val widthF = width.toFloat()
        val heightF = height.toFloat()

        paint.shader = LinearGradient(
            0f,
            0f,
            widthF,
            heightF,
            colors.backgroundBase,
            colors.backgroundTint,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, widthF, heightF, paint)

        paint.shader = LinearGradient(
            0f,
            0f,
            widthF,
            heightF,
            colors.backgroundGlow,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, widthF, heightF, paint)

        paint.shader = RadialGradient(
            widthF * 1.04f,
            -heightF * 0.08f,
            widthF * 0.72f,
            colors.backgroundHighlight,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, widthF, heightF, paint)

        paint.shader = null
        paint.color = ColorUtils.setAlphaComponent(colors.decoration, if (colors.isDark) 6 else 18)
        canvas.drawCircle(widthF * 0.88f, -heightF * 0.16f, heightF * 0.56f, paint)

        paint.color = ColorUtils.setAlphaComponent(colors.decoration, if (colors.isDark) 10 else 14)
        canvas.drawCircle(widthF * 0.92f, heightF * 0.76f, heightF * 0.66f, paint)
        return bitmap
    }
}
