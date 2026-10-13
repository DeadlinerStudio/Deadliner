@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aritxonly.deadliner.ui.overview

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.OverviewSettingsDialog
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.*
import java.time.YearMonth
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

/** Each former tab is a destination with its own scroll state and page chrome. */
@Composable
internal fun OverviewDomainScreen(activity: Activity, domain: String, month: YearMonth, onClose: () -> Unit) {
    var settings by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val scroll = MiuixScrollBehavior()
    val title = stringResource(when (domain) {
        "focus" -> R.string.dashboard_focus_management
        "trends" -> R.string.dashboard_all_trends
        "review" -> R.string.dashboard_month_review
        else -> R.string.dashboard_task_statistics
    })
    val listState = rememberLazyListState()
    AdaptiveMaterialScaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        useCurrentTopBarHeightForContentPadding = true,
        topBarMaterialScrollKey = domain,
        topBar = {
            TopAppBar(title = title, mode = TopAppBarStyle.LARGE, miuixScrollBehavior = scroll,
                navigationIcon = { TopBarIconButton(onClose) {
                    Icon(com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400), stringResource(R.string.close))
                } }, actions = {
                    if (domain == "trends") TopBarIconButton({ settings = true }) {
                        Icon(com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_tune_wght400), stringResource(R.string.settings_more))
                    }
                })
        }) { padding ->
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        // Padding belongs inside the scroll content: the captured page reaches the gesture area.
        val modifier = Modifier.fillMaxSize()
        if (domain == "focus") {
            FocusManagementContent(modifier, padding.calculateTopPadding(), bottom + 32.dp)
        } else {
            val current by rememberDashboardTasks(retry)
            val reviewMonth = latestCompletedOverviewMonth(java.time.LocalDate.now())
            val selected by rememberMonthTasks(if (domain == "review") reviewMonth else month, retry)
            val needsMonth = domain == "review"
            val snapshot = remember(current.data, selected.data, domain) {
                current.data?.takeIf { !needsMonth || selected.data != null }?.let { data ->
                    KmpOverviewPresenter.present(activity,
                        if (needsMonth) data.copy(lastMonth = selected.data!!.review) else data, needsMonth)
                }
            }
            if (snapshot != null) when (domain) {
                "trends" -> {
                    val focus by rememberFocusOverviewState(retry)
                    TrendAnalysisScreen(snapshot, modifier, padding.calculateTopPadding(), listState,
                        focusState = focus, bottomContentPadding = bottom + 32.dp)
                }
                "review" -> DashboardScreen(snapshot, activity, modifier, padding.calculateTopPadding(), listState,
                    transparentBackground = true, includeFocus = false, bottomContentPadding = bottom + 32.dp)
                else -> OverviewStatsScreen(snapshot.activeStats, snapshot.historyStats, snapshot.completionTimeStats,
                    modifier, padding.calculateTopPadding(), listState, includeFocus = false,
                    bottomContentPadding = bottom + 32.dp)
            } else Column(modifier.padding(top = padding.calculateTopPadding() + 24.dp, start = 24.dp, end = 24.dp)) {
                Text(stringResource(if (current.failed || (needsMonth && selected.failed)) R.string.dashboard_load_error else R.string.dashboard_loading))
                if (current.failed || (needsMonth && selected.failed)) TextButton(onClick = { retry++ }) {
                    Text(stringResource(R.string.dashboard_retry))
                }
            }
        }
        OverviewSettingsDialog(settings, { settings = false })
    }
}
