package com.aritxonly.deadliner.ui.overview

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth

internal interface MonthlyInsightPersistence {
    fun read(month: String): MonthlyAnalysisResult?
    fun wasAttempted(month: String): Boolean
    fun markAttempted(month: String)
    fun save(analysis: MonthlyAnalysisResult)
}

/** Successful monthly analyses are immutable; failures require an explicit retry. */
internal class MonthlyInsightRepository(
    private val persistence: MonthlyInsightPersistence,
    private val generate: suspend (OverviewSnapshot) -> MonthlyAnalysisResult,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val mutex = Mutex()
    private val requests = mutableMapOf<String, Deferred<MonthlyAnalysisResult?>>()

    suspend fun load(snapshot: OverviewSnapshot, retry: Boolean = false): MonthlyAnalysisResult? {
        val month = runCatching { YearMonth.parse(snapshot.lastMonthKey) }.getOrNull() ?: return null
        if (month >= YearMonth.from(today())) return null
        val request = mutex.withLock {
            persistence.read(snapshot.lastMonthKey)?.takeIf { it.month == snapshot.lastMonthKey && it.summary.isNotBlank() }
                ?.let { return@withLock CompletableDeferred<MonthlyAnalysisResult?>(it) }
            requests[snapshot.lastMonthKey]?.takeUnless { it.isCompleted }?.let { return@withLock it }
            if (!retry && persistence.wasAttempted(snapshot.lastMonthKey)) {
                return@withLock CompletableDeferred<MonthlyAnalysisResult?>().apply { complete(null) }
            }
            persistence.markAttempted(snapshot.lastMonthKey)
            scope.async {
                try {
                    val result = generate(snapshot).takeIf {
                        it.month == snapshot.lastMonthKey && it.summary.isNotBlank()
                    }
                    if (result != null) persistence.save(result)
                    result
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    null
                }
            }.also { requests[snapshot.lastMonthKey] = it }
        }
        // This job belongs to the application, so scrolling a card away cannot restart analysis.
        return request.await()
    }
}
