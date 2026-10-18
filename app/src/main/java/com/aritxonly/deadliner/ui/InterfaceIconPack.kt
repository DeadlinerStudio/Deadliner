package com.aritxonly.deadliner.ui

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

enum class InterfaceIconPack(val key: String) {
    MaterialSymbols("material_symbols"), HybridMiuix("hybrid_miuix");
    companion object {
        fun fromKey(key: String?): InterfaceIconPack = entries.firstOrNull { it.key == key } ?: MaterialSymbols
    }
}

val LocalInterfaceIconPack = staticCompositionLocalOf { InterfaceIconPack.MaterialSymbols }

/** Shared preference for Compose and legacy XML: changes invalidate existing XML icon instances too. */
object InterfaceIconPreferences {
    internal const val Key = "interface_icon_pack"
    private var preferences: SharedPreferences? = null
    private val state = MutableStateFlow(InterfaceIconPack.MaterialSymbols)
    val flow: StateFlow<InterfaceIconPack> get() = state
    private val drawables = mutableListOf<WeakReference<Drawable>>()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == Key) publish(InterfaceIconPack.fromKey(prefs.getString(Key, null)))
    }
    fun initialize(prefs: SharedPreferences) {
        preferences?.unregisterOnSharedPreferenceChangeListener(listener)
        preferences = prefs
        publish(InterfaceIconPack.fromKey(prefs.getString(Key, null)))
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }
    fun select(pack: InterfaceIconPack) {
        publish(pack)
        preferences?.edit()?.putString(Key, pack.key)?.apply()
    }
    internal fun track(drawable: Drawable) {
        synchronized(drawables) {
            drawables.removeAll { it.get() == null }
            drawables.add(WeakReference(drawable))
        }
    }
    private fun publish(pack: InterfaceIconPack) {
        if (state.value == pack) return
        state.value = pack
        val live = synchronized(drawables) { drawables.mapNotNull { it.get() } }
        live.forEach { it.invalidateSelf() }
    }
}

internal object InterfaceIconDirection {
    private val mirrored = setOf("arrow_back", "arrow_back_ios_new", "chevron_left", "chevron_right",
        "undo", "send", "side_navigation")
    fun mirrorsInRtl(name: String): Boolean = name in mirrored
}
