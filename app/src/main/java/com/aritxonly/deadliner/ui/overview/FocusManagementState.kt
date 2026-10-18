package com.aritxonly.deadliner.ui.overview

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.db.repository.focus.FocusManagementRepository
import com.deadliner.domain.focus.FocusSession
import com.deadliner.domain.focus.management.FocusManagementEngine
import com.deadliner.domain.focus.management.FocusManagementSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId

/** Legacy test adapter; all business date/duration calculation lives in commonMain. */
internal data class FocusManagementLegacySnapshot(val sessions: List<FocusSession>, val todaySeconds: Long, val todayCount: Int, val totalSeconds: Long)
internal fun focusManagementSnapshot(sessions: List<FocusSession>, today: LocalDate, zone: ZoneId): FocusManagementLegacySnapshot {
    val data = FocusManagementEngine(sessions, emptyList(), emptyList(), emptyList(), kotlinx.datetime.TimeZone.of(zone.id))
        .snapshot(kotlinx.datetime.LocalDate.parse(today.toString()))
    return FocusManagementLegacySnapshot(data.records.map { it.session }, data.today.totalDurationSeconds, data.today.sessionCount, data.allTime.summary.totalDurationSeconds)
}
internal fun maximumFocusAdjustment(session: FocusSession): Long = com.deadliner.domain.focus.management.maximumFocusAdjustment(session)

/** Raw SQL invalidations + local-date/zone boundaries; never re-load history on timer ticks. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
internal fun rememberFocusManagement(retry: Int): State<DashboardLoad<FocusManagementSnapshot>> {
    val owner = LocalLifecycleOwner.current
    val repository = remember { FocusManagementRepository(KmpV6Runtime.database) }
    return produceState(DashboardLoad<FocusManagementSnapshot>(), owner, retry) {
        value = value.copy(loading = true, failed = false)
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val boundaries = flow {
                var date = LocalDate.now(); var zone = ZoneId.systemDefault()
                emit(Unit)
                while (currentCoroutineContext().isActive) {
                    delay(30_000)
                    val nextDate = LocalDate.now(); val nextZone = ZoneId.systemDefault()
                    if (nextDate != date || nextZone != zone) { date = nextDate; zone = nextZone; emit(Unit) }
                }
            }
            merge(KmpV6Runtime.database.observeOverviewChanges(), boundaries).conflate().collectLatest {
                try { value = DashboardLoad(data = withContext(Dispatchers.IO) { repository.snapshot() }, loading = false) }
                catch (error: Exception) {
                    if (error is CancellationException) throw error
                    value = value.copy(loading = false, failed = true)
                }
            }
        }
    }
}
