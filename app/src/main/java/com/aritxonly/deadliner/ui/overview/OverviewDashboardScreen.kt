@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aritxonly.deadliner.ui.overview

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ArchiveActivity
import com.aritxonly.deadliner.OverviewActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
fun rememberOverviewMonth(): MutableState<String> = rememberSaveable {
    mutableStateOf(java.time.YearMonth.now().toString())
}

@Composable
fun OverviewDashboardTopBar(
    month: java.time.YearMonth,
    onMonthChange: (java.time.YearMonth) -> Unit,
    scrollBehavior: ScrollBehavior? = null,
    forceMaterial3: Boolean = false,
    onClose: (() -> Unit)? = null,
) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val monthLabel = month.format(java.time.format.DateTimeFormatter.ofPattern(
        if (locale.language == "zh") "M月" else "MMMM", locale))
    TopAppBar(title = stringResource(R.string.dashboard_month_title, monthLabel),
            subtitle = stringResource(R.string.dashboard_month_subtitle, month.year,
                stringResource(if (month == java.time.YearMonth.now()) R.string.dashboard_current_month else R.string.dashboard_month_review)), mode = TopAppBarStyle.LARGE,
            isMainTitle = onClose == null, miuixScrollBehavior = scrollBehavior, forceMaterial3 = forceMaterial3,
            navigationIcon = onClose?.let { close -> { com.aritxonly.deadliner.ui.base.TopBarIconButton(close) {
                androidx.compose.material3.Icon(com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                    stringResource(R.string.close))
            } } },
            actions = {
                androidx.compose.material3.IconButton(
                    onClick = { onMonthChange(month.minusMonths(1)) }, enabled = month.year > 1900) {
                    androidx.compose.material3.Icon(com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_chevron_left_wght400),
                        stringResource(R.string.dashboard_previous_month))
                }
                Spacer(Modifier.width(4.dp))
                androidx.compose.material3.IconButton(
                    onClick = { onMonthChange(month.plusMonths(1)) }, enabled = month < java.time.YearMonth.now()) {
                    androidx.compose.material3.Icon(com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_chevron_right_wght400),
                        stringResource(R.string.dashboard_next_month))
                }
            })

}

/** Calendar month is the common context; domain cards own their statistics destinations. */
@Composable
fun OverviewDashboardContent(
    activity: Activity,
    month: java.time.YearMonth,
    modifier: Modifier = Modifier,
    topContentPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 112.dp,
) {
    var taskRetry by remember { mutableIntStateOf(0) }
    var focusRetry by remember { mutableIntStateOf(0) }
    val tasks by rememberMonthTasks(month, taskRetry)
    val focus by rememberMonthFocus(month, focusRetry)
    val focusOverview by rememberFocusOverviewState(focusRetry)
    val previous by rememberMonthTasks(month.minusMonths(1), taskRetry)
    val current by rememberDashboardTasks(taskRetry)
    val reviewMonth = latestCompletedOverviewMonth(java.time.LocalDate.now())
    val reviewSnapshot = remember(current.data) { current.data?.let { KmpOverviewPresenter.present(activity, it, true) } }
    val listState = rememberLazyListState()
    val focusClearance = com.aritxonly.deadliner.ui.base.LocalNavigationOverlayContentPadding.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val period = month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy MMMM", locale))
    fun open(domain: String) { activity.startActivity(OverviewActivity.domainIntent(activity, domain, if (domain == "review") reviewMonth else month)) }
    LazyColumn(state = listState,
        modifier = modifier.fillMaxSize().testTag("overview-dashboard").syncTopBarMaterialScroll(listState),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topContentPadding + 16.dp,
            bottom = bottomContentPadding + focusClearance),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item("calendar") {
            OverviewMonthCalendar(month, tasks, focus) { taskRetry++; focusRetry++ }
        }
        item("domains") {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 280.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.8f
                val focusCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    DashboardDataCard(stringResource(R.string.dashboard_focus_management),
                        focusOverview.data?.let { focusDuration(it.today.totalDurationSeconds) }
                            ?: stringResource(if (focusOverview.isLoading) R.string.dashboard_loading else R.string.dashboard_unavailable),
                        "", { open("focus") },
                        cardModifier.testTag("dashboard-focus"), focusOverview.hasError, { focusRetry++ }, compact = true)
                }
                val archiveCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    DashboardDataCard(stringResource(R.string.archive),
                        tasks.data?.let { stringResource(R.string.dashboard_count, it.archivedCount) }
                            ?: stringResource(if (tasks.loading) R.string.dashboard_loading else R.string.dashboard_unavailable),
                        "",
                        { activity.startActivity(ArchiveActivity.newIntent(activity)) },
                        cardModifier.testTag("dashboard-archive"), tasks.failed, { taskRetry++ }, compact = true)
                }
                if (stacked) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    focusCard(Modifier); archiveCard(Modifier)
                } else Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    focusCard(Modifier.weight(1f).fillMaxHeight()); archiveCard(Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        item("tasks") {
            OverviewTaskResultCard(month, tasks, previous, { open("tasks") }, { taskRetry++ })
        }
        item("trends") {
            DashboardDataCard(stringResource(R.string.dashboard_all_trends),
                "", stringResource(R.string.dashboard_trend_scope),
                { open("trends") }, Modifier.testTag("dashboard-trends"), current.failed || focusOverview.hasError,
                { taskRetry++; focusRetry++ }, compact = true, supportingContent = {
                    OverviewTrendPreviews(current.data?.daily?.takeLast(7)?.map { it.completedCount.toFloat() }.orEmpty(),
                        focusOverview.data?.daily?.takeLast(7)?.map { it.durationSeconds.toFloat() / 60 }.orEmpty())
                })
        }
        item("monthly-review") {
            OverviewReviewSection(reviewSnapshot, current.failed, reviewMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy MMMM", locale)), { open("review") }, { taskRetry++ })
        }

    }
}

@Composable
fun OverviewDashboardScreen(activity: Activity, onClose: () -> Unit) {
    val monthState = rememberOverviewMonth()
    val month = java.time.YearMonth.parse(monthState.value)
    val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior()
    com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        useCurrentTopBarHeightForContentPadding = true,
        topBar = {
            OverviewDashboardTopBar(month, { monthState.value = it.toString() }, scrollBehavior = scrollBehavior, onClose = onClose)
        },
    ) { padding ->
        OverviewDashboardContent(activity, month,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topContentPadding = padding.calculateTopPadding(),
            bottomContentPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp)
    }
}
