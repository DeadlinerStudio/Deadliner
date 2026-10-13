package com.aritxonly.deadliner.ui.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.viewmodel.overview.OverviewUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** Shared current statistics used only by the Task details page. */
@Composable
internal fun rememberDashboardTasks(retry: Int): State<DashboardLoad<OverviewUiState>> =
    rememberMonthlyProjection(java.time.YearMonth.now(), retry) { KmpV6Runtime.overviewViewModel.loadNow().also { check(it.error == null) { it.error.orEmpty() } } }

data class DashboardLoad<T>(val data: T? = null, val loading: Boolean = true, val failed: Boolean = false)

/** One month key is shared by calendar, cards and secondary routes. */
@Composable
internal fun rememberMonthTasks(month: java.time.YearMonth, retry: Int): State<DashboardLoad<com.deadliner.viewmodel.overview.OverviewMonthUiState>> =
    rememberMonthlyProjection(month, retry) { KmpV6Runtime.overviewViewModel.loadMonthNow(month.atDay(1).toString()) }

@Composable
internal fun rememberMonthFocus(month: java.time.YearMonth, retry: Int): State<DashboardLoad<com.deadliner.domain.statistics.FocusStatisticsSnapshot>> =
    rememberMonthlyProjection(month, retry) {
        KmpV6Runtime.database.focusStatistics.snapshot(com.deadliner.domain.statistics.FocusStatisticsQuery(
            com.deadliner.domain.statistics.FocusStatisticsDateRange(month.atDay(1).toString(), month.plusMonths(1).atDay(1).toString())))
    }

@Composable
internal fun <T> rememberMonthlyProjection(month: java.time.YearMonth, retry: Int, load: () -> T): State<DashboardLoad<T>> {
    val owner = LocalLifecycleOwner.current
    return produceState(DashboardLoad<T>(), owner, month, retry) {
        value = DashboardLoad()
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var date = LocalDate.now()
            var zone = ZoneId.systemDefault()
            while (true) {
                val collector = launch {
                    try {
                        // Raw invalidation must not be deduplicated by another domain’s statistics.
                        KmpV6Runtime.database.observeOverviewChanges().collect {
                            try { value = DashboardLoad(data = withContext(Dispatchers.IO) { load() }, loading = false) }
                            catch (error: Exception) {
                                if (error is kotlinx.coroutines.CancellationException) throw error
                                value = DashboardLoad(loading = false, failed = true)
                            }
                        }
                    } catch (error: Exception) {
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        value = DashboardLoad(loading = false, failed = true)
                    }
                }
                while (date == LocalDate.now() && zone == ZoneId.systemDefault()) delay(30_000)
                collector.cancel()
                collector.join()
                date = LocalDate.now(); zone = ZoneId.systemDefault()
            }
        }
    }
}
