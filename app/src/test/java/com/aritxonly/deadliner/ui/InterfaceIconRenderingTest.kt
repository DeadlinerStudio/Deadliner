package com.aritxonly.deadliner.ui

import android.app.Application
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.test.junit4.createComposeRule
import com.aritxonly.deadliner.R
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
class InterfaceIconRenderingTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = RuntimeEnvironment.getApplication()
    private val prefs get() = app.getSharedPreferences("interface-icon-test", Context.MODE_PRIVATE)
    @Before fun reset() {
        prefs.edit().clear().commit()
        InterfaceIconPreferences.initialize(prefs)
    }
    @After fun cleanup() { InterfaceIconPreferences.select(InterfaceIconPack.MaterialSymbols) }

    @Test fun preferencePersistsRestoresAndRecoversUnknownValues() {
        assertEquals(InterfaceIconPack.MaterialSymbols, InterfaceIconPreferences.flow.value)
        InterfaceIconPreferences.select(InterfaceIconPack.HybridMiuix)
        assertEquals("hybrid_miuix", prefs.getString(InterfaceIconPreferences.Key, null))
        InterfaceIconPreferences.initialize(prefs)
        assertEquals(InterfaceIconPack.HybridMiuix, InterfaceIconPreferences.flow.value)
        prefs.edit().putString(InterfaceIconPreferences.Key, "unknown").commit()
        assertEquals(InterfaceIconPack.MaterialSymbols, InterfaceIconPreferences.flow.value)
    }
    @Test fun composeSwitchesBackendsImmediatelyWithoutRecreatingTheScreen() {
        var current: Painter? = null
        compose.setContent {
            val pack by InterfaceIconPreferences.flow.collectAsState()
            CompositionLocalProvider(LocalInterfaceIconPack provides pack) {
                current = symbolPainterResource(R.drawable.msr_star_fill_wght400)
                Icon(current!!, contentDescription = "Star")
            }
        }
        compose.runOnIdle { assertTrue(current!!.javaClass.simpleName.contains("MaterialSymbolFontPainter")) }
        compose.runOnIdle { InterfaceIconPreferences.select(InterfaceIconPack.HybridMiuix) }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(current!!.javaClass.simpleName.contains("VectorPainter")) }
        compose.runOnIdle { InterfaceIconPreferences.select(InterfaceIconPack.MaterialSymbols) }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(current!!.javaClass.simpleName.contains("MaterialSymbolFontPainter")) }
    }
    @Test fun explicitMaterialSendArrowKeepsItsBackendInHybridMode() {
        var current: Painter? = null
        compose.setContent {
            CompositionLocalProvider(LocalInterfaceIconPack provides InterfaceIconPack.HybridMiuix) {
                current = originalMaterialSymbolPainter("arrow_upward")
                Icon(current!!, contentDescription = "Send")
            }
        }
        compose.runOnIdle { assertTrue(current!!.javaClass.simpleName.contains("MaterialSymbolFontPainter")) }
    }
    @Test fun allGeneratedVectorsInflateIncludingCompositeClipPaths() {
        val ids = (HybridIconRegistry.names.mapNotNull { name ->
            HybridIconRegistry.resource(name.substringBefore('#'), name.endsWith("#filled"))
        } + listOf(R.drawable.ic_lifi_new_conversation, R.drawable.ic_miuix_home_add,
            R.drawable.ic_miuix_overview_previous, R.drawable.ic_miuix_overview_next, R.drawable.ic_miuix_quick_expand)).distinct()
        assertTrue(ids.size > 100)
        val canvas = Canvas(Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888))
        ids.forEach { id ->
            val drawable = app.resources.getDrawable(id, app.theme)
            assertEquals(app.resources.getResourceEntryName(id), 24, drawable.intrinsicWidth)
            drawable.setBounds(0, 0, 24, 24)
            drawable.draw(canvas)
        }
    }
    @Test fun existingXmlDrawableInvalidatesAndKeepsItsTintAndAlphaWhenThePackChanges() {
        val drawable = app.resources.getDrawable(R.drawable.msr_star_fill_wght400, app.theme)
        assertTrue(drawable is MaterialSymbolDrawable)
        var invalidations = 0
        drawable.callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) { invalidations++ }
            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = Unit
            override fun unscheduleDrawable(who: Drawable, what: Runnable) = Unit
        }
        drawable.alpha = 120
        drawable.setTintList(ColorStateList.valueOf(android.graphics.Color.BLUE))
        val before = invalidations
        InterfaceIconPreferences.select(InterfaceIconPack.HybridMiuix)
        assertTrue(invalidations > before)
        assertEquals(120, drawable.alpha)
        drawable.setBounds(0,0,24,24)
        val canvas = Canvas(Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888))
        drawable.draw(canvas)
        InterfaceIconPreferences.select(InterfaceIconPack.MaterialSymbols)
        drawable.draw(canvas)
        assertEquals(120, drawable.alpha)
    }
}
