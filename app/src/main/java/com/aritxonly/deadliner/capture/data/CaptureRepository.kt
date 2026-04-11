package com.aritxonly.deadliner.capture.data

import android.content.Context
import com.aritxonly.deadliner.capture.model.InspirationItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CaptureRepository(context: Context) {
    private val prefs = context.getSharedPreferences("capture_inspiration", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "items_json"

    fun load(): List<InspirationItem> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<InspirationItem>>() {}.type
        return runCatching { gson.fromJson<List<InspirationItem>>(raw, type) }
            .getOrDefault(emptyList())
            .sortedByDescending { it.updatedAt }
    }

    fun save(items: List<InspirationItem>) {
        prefs.edit().putString(key, gson.toJson(items)).apply()
    }
}
