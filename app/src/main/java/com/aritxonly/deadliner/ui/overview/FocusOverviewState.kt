package com.aritxonly.deadliner.ui.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.statistics.FocusOverviewStatistics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class FocusOverviewState(
    val data: FocusOverviewStatistics? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)

/** One subscription for all three tabs, stopped with the page lifecycle. */
@Composable
internal fun rememberFocusOverviewState(retry: Int = 0): State<FocusOverviewState> {
    val owner = LocalLifecycleOwner.current
    return produceState(FocusOverviewState(), owner, retry) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var date = LocalDate.now()
            var zone = ZoneId.systemDefault()
            while (true) {
                val collector = launch {
                    try {
                        KmpV6Runtime.database.focusStatistics.observeOverview().collect {
                            value = FocusOverviewState(data = it, isLoading = false)
                        }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        value = value.copy(isLoading = false, hasError = true)
                    }
                }
                // Resubscribe after midnight so date projections also update without a mutation.
                while (LocalDate.now() == date && ZoneId.systemDefault() == zone) delay(30_000)
                collector.cancelAndJoin()
                date = LocalDate.now()
                zone = ZoneId.systemDefault()
            }
        }
    }
}
