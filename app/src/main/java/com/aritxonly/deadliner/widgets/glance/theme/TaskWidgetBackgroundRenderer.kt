package com.aritxonly.deadliner.widgets.glance.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.graphics.ColorUtils

internal object TaskWidgetBackgroundRenderer {
    private const val BITMAP_SIZE = 360

    fun render(colors: TaskWidgetColors): Bitmap {
        val size = BITMAP_SIZE.toFloat()
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f,
            0f,
            size,
            size,
            colors.backgroundStart,
            colors.backgroundEnd,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, size, size, paint)

        paint.shader = null
        paint.color = ColorUtils.setAlphaComponent(colors.decoration, 16)
        canvas.drawCircle(size * 0.84f, -size * 0.02f, size * 0.47f, paint)

        paint.color = ColorUtils.setAlphaComponent(colors.decoration, 25)
        canvas.drawCircle(size * 0.82f, size * 0.20f, size * 0.31f, paint)

        return bitmap
    }
}
