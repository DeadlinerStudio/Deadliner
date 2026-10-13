package com.aritxonly.deadliner

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.aritxonly.deadliner.ui.base.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.Switch
import com.aritxonly.deadliner.ui.base.TabRow
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.overview.DashboardScreen
import com.aritxonly.deadliner.ui.overview.OverviewStatsScreen
import com.aritxonly.deadliner.ui.overview.TrendAnalysisScreen
import com.aritxonly.deadliner.ui.overview.KmpOverviewPresenter
import com.aritxonly.deadliner.ui.overview.rememberFocusOverviewState
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import androidx.compose.ui.platform.LocalLayoutDirection
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior

class OverviewActivity : DeadlinerComponentActivity() {

    companion object {
        fun domainIntent(context: Context, domain: String, month: java.time.YearMonth): Intent =
            statisticsIntent(context).putExtra("domain", domain).putExtra("month", month.toString())

        fun statisticsIntent(context: Context): Intent =
            Intent(context, OverviewActivity::class.java).putExtra("full_statistics", true)

        fun newIntent(context: Context): Intent {
            return Intent(context, OverviewActivity::class.java).putExtra("full_statistics", false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)

        setContent {
            DeadlinerTheme {
                if (intent.getBooleanExtra("full_statistics", true)) {
                    val month = runCatching { java.time.YearMonth.parse(intent.getStringExtra("month")) }
                        .getOrDefault(java.time.YearMonth.now())
                        .coerceAtMost(java.time.YearMonth.now())
                    com.aritxonly.deadliner.ui.overview.OverviewDomainScreen(
                        this, intent.getStringExtra("domain") ?: "tasks", month, onClose = { finish() })
                } else {
                    com.aritxonly.deadliner.ui.overview.OverviewDashboardScreen(this, onClose = { finish() })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableEdgeToEdgeForAllDevices()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdgeForAllDevices()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        enableEdgeToEdgeForAllDevices()
    }
}

@Composable
fun hashColor(key: String): Color {
    return when (key) {
        stringResource(R.string.today_completed),
        stringResource(R.string.completed),
        stringResource(R.string.cumulative_completed) -> colorResource(R.color.chart_green)

        stringResource(R.string.pending_tasks),
        stringResource(R.string.current_pending),
        stringResource(R.string.incomplete) -> colorResource(R.color.chart_orange)

        stringResource(R.string.today_overdue),
        stringResource(R.string.overdue),
        stringResource(R.string.cumulative_overdue) -> colorResource(R.color.chart_red)

        else -> colorResource(R.color.chart_blue)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTopBar(
    showNavigationIcon: Boolean = true,
    onClose: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    mode: TopAppBarStyle = TopAppBarStyle.CENTER,
    forceMaterial3: Boolean = false,
    scrollBehavior: ScrollBehavior? = null,
) {
    TopAppBar(
        title = stringResource(R.string.dashboard_statistics_title),
        mode = mode,
        forceMaterial3 = forceMaterial3,
        navigationIcon = if (showNavigationIcon) {
            {
                TopBarIconButton(onClick = onClose) {
                    Icon(
                        com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = expressiveTypeModifier,
                    )
                }
            }
        } else {
            null
        },
        actions = {
            TopBarIconButton(onClick = onShowSettings) {
                Icon(
                    com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_tune_wght400),
                    contentDescription = stringResource(R.string.settings_more),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier,
                )
            }
        },
        isMainTitle = !showNavigationIcon,
        miuixScrollBehavior = scrollBehavior,
    )
}

@Composable
fun OverviewSettingsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    var showOverdueSeries by remember {
        mutableStateOf(GlobalUtils.OverviewSettings.showOverdueInDaily)
    }

    AlertDialog(
        show = true,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accept))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.settings_more),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        miuixTitle = stringResource(R.string.settings_more),
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.overview_settings_show_overdue_series),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.overview_settings_show_overdue_series_support),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = showOverdueSeries,
                    onCheckedChange = { checked ->
                        showOverdueSeries = checked
                        GlobalUtils.OverviewSettings.showOverdueInDaily = checked
                    }
                )
            }
        }
    )
}

@Composable
fun OverviewContent(
    overviewState: com.deadliner.viewmodel.overview.OverviewUiState,
    activity: Activity,
    modifier: Modifier = Modifier,
    flattenedLayout: Boolean = false,
    selectedTab: Int = 0,
    onSelectedTabChange: (Int) -> Unit = {},
    showTabsInContent: Boolean = true,
    topContentPadding: Dp = 0.dp,
    transparentBackground: Boolean = false,
    includeFocus: Boolean = true,
    monthData: com.deadliner.viewmodel.overview.OverviewMonthUiState? = null,
) {
    val focusState = if (includeFocus) rememberFocusOverviewState().value else com.aritxonly.deadliner.ui.overview.FocusOverviewState()
    val snapshot = remember(overviewState, monthData) {
        KmpOverviewPresenter.present(activity, overviewState.copy(lastMonth = monthData?.review ?: overviewState.lastMonth), monthData != null)
    }
    val statsListState = rememberLazyListState()
    val trendListState = rememberLazyListState()
    val dashboardListState = rememberLazyListState()
    val pageBackground = if (transparentBackground) {
        Modifier
    } else {
        Modifier.background(MaterialTheme.colorScheme.surface)
    }

    if (flattenedLayout) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .then(pageBackground)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OverviewStatsScreen(
                focusState = focusState,
                includeFocus = includeFocus,
                monthData = monthData,
                activeStats = snapshot.activeStats,
                historyStats = snapshot.historyStats,
                completionTimeStats = snapshot.completionTimeStats,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(pageBackground),
                topContentPadding = topContentPadding,
                listState = statsListState,
            )
            TrendAnalysisScreen(
                focusState = focusState,
                includeFocus = includeFocus,
                snapshot = snapshot,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(pageBackground),
                topContentPadding = topContentPadding,
                listState = trendListState,
            )
            DashboardScreen(
                focusState = focusState,
                includeFocus = includeFocus,
                snapshot = snapshot,
                activity = activity,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(pageBackground),
                topContentPadding = topContentPadding,
                listState = dashboardListState,
                transparentBackground = transparentBackground,
            )
        }
        return
    }

    val tabs = listOf(
        stringResource(R.string.tab_overview),
        stringResource(R.string.tab_trend),
        stringResource(R.string.tab_summary)
    )
    var internalSelectedTab by rememberSaveable { mutableStateOf(0) }
    val currentTab = if (showTabsInContent) internalSelectedTab else selectedTab

    Column(
        modifier = modifier.then(pageBackground)
    ) {
        if (showTabsInContent) {
            TabRow(
                tabs = tabs,
                selectedTabIndex = currentTab,
                onTabSelected = {
                    internalSelectedTab = it
                    onSelectedTabChange(it)
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surface) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        when (currentTab) {
            0 -> OverviewStatsScreen(
                focusState = focusState,
                includeFocus = includeFocus,
                monthData = monthData,
                activeStats = snapshot.activeStats,
                historyStats = snapshot.historyStats,
                completionTimeStats = snapshot.completionTimeStats,
                modifier = pageBackground,
                topContentPadding = topContentPadding,
                listState = statsListState,
            )

            1 -> TrendAnalysisScreen(
                focusState = focusState,
                includeFocus = includeFocus,
                snapshot = snapshot,
                modifier = pageBackground,
                topContentPadding = topContentPadding,
                listState = trendListState,
            )

            else -> DashboardScreen(
                focusState = focusState,
                includeFocus = includeFocus,
                snapshot = snapshot,
                activity = activity,
                modifier = pageBackground,
                topContentPadding = topContentPadding,
                listState = dashboardListState,
                transparentBackground = transparentBackground,
            )
        }
    }
}


@Composable
fun OverviewTopBarWithTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showNavigationIcon: Boolean = true,
    onClose: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    mode: TopAppBarStyle = TopAppBarStyle.CENTER,
    forceMaterial3: Boolean = false,
    scrollBehavior: ScrollBehavior? = null,
) {
    val tabs = listOf(
        stringResource(R.string.tab_overview),
        stringResource(R.string.tab_trend),
        stringResource(R.string.tab_summary)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
    ) {
        OverviewTopBar(
            showNavigationIcon = showNavigationIcon,
            onClose = onClose,
            onShowSettings = onShowSettings,
            mode = mode,
            forceMaterial3 = forceMaterial3,
            scrollBehavior = scrollBehavior,
        )
        TabRow(
            tabs = tabs,
            selectedTabIndex = selectedTab,
            onTabSelected = onTabSelected,
            divider = { HorizontalDivider(color = Color.Transparent) },
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
        )
    }
}

@Composable
fun OverviewScreen(
    overviewState: com.deadliner.viewmodel.overview.OverviewUiState,
    activity: Activity,
    showTopBar: Boolean = true,
    showNavigationIcon: Boolean = true,
    flattenedLayout: Boolean = false,
    onClose: () -> Unit = {},
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val layoutDirection = LocalLayoutDirection.current
    val topBarScrollBehavior = MiuixScrollBehavior()

    AdaptiveMaterialScaffold(
        containerColor = Color.Transparent,
        contentColor = contentColorFor(Color.Transparent),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBarMaterialScrollKey = selectedTab,
        topBar = {
            if (!showTopBar) return@AdaptiveMaterialScaffold
            if (flattenedLayout) {
                OverviewTopBar(
                    showNavigationIcon = showNavigationIcon,
                    onClose = onClose,
                    onShowSettings = { showSettings = true },
                    mode = TopAppBarStyle.LARGE,
                    scrollBehavior = topBarScrollBehavior,
                )
            } else {
                OverviewTopBarWithTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    showNavigationIcon = showNavigationIcon,
                    onClose = onClose,
                    onShowSettings = { showSettings = true },
                    mode = TopAppBarStyle.LARGE,
                    scrollBehavior = topBarScrollBehavior,
                )
            }
        },
    ) { paddingValues ->
        val topContentPadding = if (showTopBar && !flattenedLayout) {
            paddingValues.calculateTopPadding()
        } else {
            0.dp
        }
        val contentModifier = Modifier
            .then(
                if (showTopBar) {
                    Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection)
                } else {
                    Modifier
                },
            )
            .then(
                if (showTopBar && !flattenedLayout) {
                    Modifier.padding(
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding(),
            )
                } else {
                    Modifier.padding(paddingValues)
                },
            )
        OverviewContent(
            overviewState = overviewState,
            activity = activity,
            modifier = contentModifier,
            flattenedLayout = flattenedLayout,
            selectedTab = selectedTab,
            onSelectedTabChange = { selectedTab = it },
            showTabsInContent = flattenedLayout,
            topContentPadding = topContentPadding,
        )
        OverviewSettingsDialog(
            visible = showSettings,
            onDismiss = { showSettings = false },
        )
    }
}
