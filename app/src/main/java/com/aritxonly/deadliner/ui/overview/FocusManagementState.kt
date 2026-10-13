package com.aritxonly.deadliner.ui.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.focus.FocusSession
import com.deadliner.domain.focus.MAX_ADJUSTED_DURATION_SECONDS
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

internal data class FocusManagementSnapshot(
    val sessions: List<FocusSession>,
    val todaySeconds: Long,
    val todayCount: Int,
    val totalSeconds: Long,
    val distribution: com.deadliner.domain.statistics.FocusStatisticsSnapshot? = null,
)

internal fun focusManagementSnapshot(sessions: List<FocusSession>, today: LocalDate, zone: ZoneId): FocusManagementSnapshot {
    val saved = sessions.filterNot { it.isDeleted }.sortedByDescending { Instant.parse(it.startedAt) }
    val todaySessions = saved.filter { Instant.parse(it.startedAt).atZone(zone).toLocalDate() == today }
    return FocusManagementSnapshot(saved, todaySessions.sumOf { it.durationSeconds }, todaySessions.size,
        saved.sumOf { it.durationSeconds })
}

internal fun maximumFocusAdjustment(session: FocusSession): Long = minOf(session.durationSeconds,
    java.time.Duration.between(Instant.parse(session.startedAt), Instant.parse(session.endedAt)).seconds,
    MAX_ADJUSTED_DURATION_SECONDS)

@Composable
internal fun rememberFocusManagement(retry: Int): State<DashboardLoad<FocusManagementSnapshot>> =
    rememberMonthlyProjection(YearMonth.now(), retry) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val snapshot = focusManagementSnapshot(KmpV6Runtime.database.focusSessions.list(), today, zone)
        if (snapshot.sessions.isEmpty()) snapshot else {
            val start = snapshot.sessions.minOf { Instant.parse(it.startedAt).atZone(zone).toLocalDate() }
            val end = maxOf(today, snapshot.sessions.maxOf { Instant.parse(it.startedAt).atZone(zone).toLocalDate() }).plusDays(1)
            snapshot.copy(distribution = KmpV6Runtime.database.focusStatistics.snapshot(
                com.deadliner.domain.statistics.FocusStatisticsQuery(
                    com.deadliner.domain.statistics.FocusStatisticsDateRange(start.toString(), end.toString()))))
        }
    }
