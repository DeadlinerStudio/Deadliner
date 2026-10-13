package com.aritxonly.deadliner.ui.overview

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.deadliner.ai.AIUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class MonthlyInsightState(val analysis: MonthlyAnalysisResult? = null, val loading: Boolean = true)

@Volatile private var monthlyInsightRepository: MonthlyInsightRepository? = null

private fun monthlyInsightRepository(context: Context): MonthlyInsightRepository =
    monthlyInsightRepository ?: synchronized(MonthlyInsightRepository::class.java) {
        monthlyInsightRepository ?: run {
            val persistence = MonthlyInsightPreferences(context.applicationContext
                .getSharedPreferences("overview_monthly_reviews", Context.MODE_PRIVATE))
            persistence.migrateLegacy()
            MonthlyInsightRepository(persistence, generate = { snapshot ->
                AIUtils.generateMonthlyAnalysis(snapshot.lastMonthKey, snapshot.lastMonthName,
                    snapshot.metricsSummary, snapshot.completedTaskNames)
            }).also { monthlyInsightRepository = it }
        }
    }

/** Only a different completed month or an explicit retry starts a request. */
@Composable
internal fun rememberMonthlyInsight(snapshot: OverviewSnapshot?, retry: Int = 0): State<MonthlyInsightState> {
    val context = LocalContext.current.applicationContext
    return produceState(MonthlyInsightState(), snapshot?.lastMonthKey, retry) {
        value = MonthlyInsightState()
        if (snapshot == null) return@produceState
        val analysis = try {
            withContext(Dispatchers.IO) { monthlyInsightRepository(context).load(snapshot, retry > 0) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            null
        }
        value = MonthlyInsightState(analysis, false)
    }
}
