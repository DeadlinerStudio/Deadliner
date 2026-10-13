package com.aritxonly.deadliner.ui.overview

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MonthlyInsightRepositoryTest {
    private class Persistence : MonthlyInsightPersistence {
        val analyses = mutableMapOf<String, MonthlyAnalysisResult>()
        val attempted = mutableSetOf<String>()
        override fun read(month: String) = analyses[month]
        override fun wasAttempted(month: String) = month in attempted
        override fun markAttempted(month: String) { attempted += month }
        override fun save(analysis: MonthlyAnalysisResult) { analyses[analysis.month] = analysis }
    }
    private fun snapshot(month: String = "2026-09") = OverviewSnapshot(linkedMapOf(), linkedMapOf(), emptyList(),
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), month, month, emptyList(), "Metrics")
    private val today = { LocalDate.of(2026, 10, 12) }

    @Test fun neverAnalyzesAnUnfinishedOrFutureMonth() = runBlocking {
        var calls = 0
        val repository = MonthlyInsightRepository(Persistence(), { calls++; error("Unexpected analysis") }, today)
        assertNull(repository.load(snapshot("2026-10")))
        assertNull(repository.load(snapshot("2026-11"), retry = true))
        assertEquals(0, calls)
    }
    @Test fun savedMonthsSurviveRepositoryRecreationAndDoNotReanalyzeChangedMetrics() = runBlocking {
        val persistence = Persistence()
        var calls = 0
        val generator: suspend (OverviewSnapshot) -> MonthlyAnalysisResult = {
            calls++; MonthlyAnalysisResult(it.lastMonthKey, "Saved insight", emptyList())
        }
        val first = MonthlyInsightRepository(persistence, generator, today)
        first.load(snapshot())
        first.load(snapshot("2026-08"))
        val recreated = MonthlyInsightRepository(persistence, generator, today)
        assertEquals("Saved insight", recreated.load(snapshot().copy(metricsSummary = "Changed"), true)?.summary)
        assertEquals(2, calls)
        assertEquals(setOf("2026-08", "2026-09"), persistence.analyses.keys)
    }
    @Test fun failureDoesNotLoopAcrossReentryButAllowsExplicitRetry() = runBlocking {
        val persistence = Persistence()
        var calls = 0
        val generator: suspend (OverviewSnapshot) -> MonthlyAnalysisResult = {
            calls++
            if (calls == 1) error("Offline")
            MonthlyAnalysisResult(it.lastMonthKey, "Retried insight", emptyList())
        }
        assertNull(MonthlyInsightRepository(persistence, generator, today).load(snapshot()))
        val next = MonthlyInsightRepository(persistence, generator, today)
        assertNull(next.load(snapshot()))
        assertEquals(1, calls)
        assertNotNull(next.load(snapshot(), true))
        assertEquals(2, calls)
    }
    @Test fun simultaneousPreviewAndDetailShareOneAnalysisAndSurviveCallerCancellation() = runBlocking {
        var calls = 0
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val repository = MonthlyInsightRepository(Persistence(), {
            calls++; started.complete(Unit); finish.await()
            MonthlyAnalysisResult(it.lastMonthKey, "Shared insight", emptyList())
        }, today)
        val preview = async { repository.load(snapshot()) }
        started.await()
        preview.cancelAndJoin()
        val detail = async { repository.load(snapshot()) }
        finish.complete(Unit)
        assertEquals("Shared insight", detail.await()?.summary)
        assertEquals(1, calls)
    }
}
