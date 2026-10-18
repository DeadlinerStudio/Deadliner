package com.aritxonly.deadliner.ui

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.core.content.res.use
import com.aritxonly.deadliner.R
import kotlin.math.min

/** Drawable bridge for XML layouts, menus, shortcuts, and widgets. */
class MaterialSymbolDrawable : Drawable() {
    private var symbolName = ""
    private var filled = false
    private var density = 1f
    private var drawableAlpha = 255
    private var sourceResources: Resources? = null
    private var sourceTheme: Resources.Theme? = null
    private var hybrid: Drawable? = null
    private var hybridResource = 0
    init { InterfaceIconPreferences.track(this) }
    private var typeface: Typeface? = null
    private var tint: ColorStateList? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        textAlign = Paint.Align.CENTER
        fontFeatureSettings = "liga"
    }

    override fun inflate(resources: Resources, parser: org.xmlpull.v1.XmlPullParser, attrs: AttributeSet, theme: Resources.Theme?) {
        super.inflate(resources, parser, attrs, theme)
        val attributes = theme?.obtainStyledAttributes(attrs, R.styleable.MaterialSymbolDrawable, 0, 0)
            ?: resources.obtainAttributes(attrs, R.styleable.MaterialSymbolDrawable)
        attributes.use {
            symbolName = it.getString(R.styleable.MaterialSymbolDrawable_symbolName).orEmpty()
            filled = it.getBoolean(R.styleable.MaterialSymbolDrawable_symbolFilled, false)
        }
        sourceResources = resources
        sourceTheme = theme
        density = resources.displayMetrics.density
        typeface = resources.getFont(R.font.material_symbols_rounded_variable)
        tint = theme?.let(::themeColorControlNormal)
    }

    override fun draw(canvas: Canvas) {
        if (symbolName.isEmpty()) return
        if (InterfaceIconPreferences.flow.value == InterfaceIconPack.HybridMiuix) {
            val resource = HybridIconRegistry.resource(symbolName, filled)
            if (resource != null && sourceResources != null) {
                if (hybridResource != resource) {
                    hybrid = sourceResources!!.getDrawable(resource, sourceTheme).mutate()
                    hybridResource = resource
                }
                hybrid?.let {
                    it.bounds = bounds
                    it.layoutDirection = layoutDirection
                    it.setTintList(tint ?: ColorStateList.valueOf(android.graphics.Color.BLACK))
                    it.state = state
                    it.alpha = drawableAlpha
                    it.colorFilter = paint.colorFilter
                    it.draw(canvas)
                    return
                }
            }
        }
        val rect = bounds
        val glyphSize = min(rect.width(), rect.height()).toFloat()
        if (glyphSize <= 0f) return
        paint.typeface = typeface
        paint.textSize = glyphSize
        paint.colorFilter = colorFilter
        val color = tint?.getColorForState(state, tint!!.defaultColor) ?: android.graphics.Color.BLACK
        paint.color = color
        paint.alpha = android.graphics.Color.alpha(color) * drawableAlpha / 255
        paint.fontVariationSettings = "'FILL' ${if (filled) 1 else 0}, 'wght' ${MaterialSymbolAxisPolicy.DefaultWeight}, 'GRAD' 0, 'opsz' 24"
        val baseline = rect.top + rect.height() / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        canvas.save()
        if (layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL && InterfaceIconDirection.mirrorsInRtl(symbolName)) {
            canvas.scale(-1f, 1f, rect.exactCenterX(), rect.exactCenterY())
        }
        canvas.drawText(symbolName, rect.exactCenterX(), baseline, paint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha
        invalidateSelf()
    }

    override fun getAlpha(): Int = drawableAlpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getColorFilter(): ColorFilter? = paint.colorFilter

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun isStateful(): Boolean = tint?.isStateful == true

    override fun onStateChange(state: IntArray): Boolean {
        if (tint?.isStateful != true) return false
        invalidateSelf()
        return true
    }

    override fun setTintList(tint: ColorStateList?) {
        this.tint = tint
        invalidateSelf()
    }

    override fun getIntrinsicWidth(): Int = (24 * density).toInt()

    override fun getIntrinsicHeight(): Int = (24 * density).toInt()

    private fun themeColorControlNormal(theme: Resources.Theme): ColorStateList? {
        val value = TypedValue()
        if (!theme.resolveAttribute(android.R.attr.colorControlNormal, value, true)) return null
        return if (value.resourceId != 0) {
            theme.resources.getColorStateList(value.resourceId, theme)
        } else {
            ColorStateList.valueOf(value.data)
        }
    }
}
