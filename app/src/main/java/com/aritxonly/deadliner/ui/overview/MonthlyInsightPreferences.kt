package com.aritxonly.deadliner.ui.overview

import android.content.SharedPreferences
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.google.gson.Gson

internal class MonthlyInsightPreferences(private val preferences: SharedPreferences) : MonthlyInsightPersistence {
    private val gson = Gson()

    override fun read(month: String): MonthlyAnalysisResult? =
        preferences.getString("analysis/$month", null)?.let { json ->
            runCatching { gson.fromJson(json, MonthlyAnalysisResult::class.java) }.getOrNull()
        }

    override fun wasAttempted(month: String): Boolean = preferences.getBoolean("attempted/$month", false)

    override fun markAttempted(month: String) {
        check(preferences.edit().putBoolean("attempted/$month", true).commit())
    }

    override fun save(analysis: MonthlyAnalysisResult) {
        check(preferences.edit().putString("analysis/${analysis.month}", gson.toJson(analysis))
            .putBoolean("attempted/${analysis.month}", true).commit())
    }

    fun migrateLegacy() {
        val old = GlobalUtils.OverviewSettings.monthlyAnalysisJson?.let {
            runCatching { gson.fromJson(it, MonthlyAnalysisResult::class.java) }.getOrNull()
        } ?: return
        val month = runCatching { java.time.YearMonth.parse(old.month) }.getOrNull() ?: return
        if (month < java.time.YearMonth.now() && old.summary.isNotBlank() && read(old.month) == null) save(old)
    }
}
