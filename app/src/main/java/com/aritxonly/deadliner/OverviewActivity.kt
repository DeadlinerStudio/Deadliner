package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import java.time.LocalDateTime
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.overview.DashboardScreen
import com.aritxonly.deadliner.ui.overview.OverviewStatsScreen
import com.aritxonly.deadliner.ui.overview.TrendAnalysisScreen
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.base.TabRow
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import okhttp3.internal.toHexString
import java.time.LocalDate

// 辅助函数：判断任务是否逾期
// 假设 endTime 格式为 "yyyy-MM-dd HH:mm"
fun isOverdue(item: DDLItem): Boolean {
    return try {
        val end = GlobalUtils.safeParseDateTime(item.endTime)
        val now = LocalDateTime.now()
        !item.isCompleted && now.isAfter(end)
    } catch (e: Exception) {
        false
    }
}

// 辅助函数：根据完成时间提取时间段（00-06, 06-12, 12-18, 18-24）
fun extractTimeBucket(context: Context, completeTime: String): String {
    return try {
        val time = GlobalUtils.safeParseDateTime(completeTime) // LocalDateTime
        val hour = time.hour
        when (hour) {
            in 0 until 6  -> context.getString(R.string.time_bucket_late_night)
            in 6 until 12 -> context.getString(R.string.time_bucket_morning)
            in 12 until 18-> context.getString(R.string.time_bucket_afternoon)
            in 18..23     -> context.getString(R.string.time_bucket_evening)
            else          -> context.getString(R.string.time_bucket_unknown)
        }
    } catch (_: Exception) {
        context.getString(R.string.time_bucket_unknown)
    }
}

fun buildTimeBucketOrder(context: Context): Map<String, Int> = mapOf(
    context.getString(R.string.time_bucket_late_night) to 0,
    context.getString(R.string.time_bucket_morning)    to 1,
    context.getString(R.string.time_bucket_afternoon)  to 2,
    context.getString(R.string.time_bucket_evening)    to 3
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class OverviewActivity : ComponentActivity() {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, OverviewActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()

        window.isNavigationBarContrastEnforced = false

        super.onCreate(savedInstanceState)

        setContent {
            DeadlinerTheme {
                val items = DDLRepository().getDDLsByType(DeadlineType.TASK)

                    OverviewScreen(
                        items = items,
                        activity = this
                    ) {
                        finish()
                    }
            }
        }
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
fun hashColor(key: String) : Color {
    val color = when (key) {
        stringResource(R.string.today_completed), stringResource(R.string.completed) -> colorResource(id = R.color.chart_green)
        stringResource(R.string.incomplete) -> colorResource(id = R.color.chart_orange)
        stringResource(R.string.today_overdue), stringResource(R.string.overdue) -> colorResource(id = R.color.chart_red)
        else -> colorResource(id = R.color.chart_blue)
    }
    return color
}

@SuppressLint("SetTextI18n")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    items: List<DDLItem>,
    activity: OverviewActivity,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    // 数据准备
    // 当前任务：未归档
    val activeItems = items.filter { !it.isArchived }
    val completedItems = activeItems.filter {
        val completeTime = GlobalUtils.parseDateTime(it.completeTime) ?: return@filter false
        val completeDateIsToday = (completeTime.toLocalDate() == LocalDate.now())
        it.isCompleted && completeDateIsToday
    }
    val incompleteItems = activeItems.filter { !it.isCompleted && !isOverdue(it) }
    val overdueItems = activeItems.filter {
        Log.d("OverviewDebug", "I reach here with ${it.endTime}")
        val endTime = GlobalUtils.parseDateTime(it.endTime) ?: return@filter false
        val endDateIsToday = (endTime.toLocalDate() == LocalDate.now())
        Log.d("OverviewDebug", endDateIsToday.toString())
        endDateIsToday && !it.isCompleted
    }

    val historyCompleted = items.filter { it.isCompleted }
    val historyIncomplete = items.filter { !it.isCompleted }
    val historyOverdue = activeItems.filter { isOverdue(it) }

    val activeStats = mapOf(
        stringResource(R.string.today_completed) to completedItems.size,
        stringResource(R.string.incomplete) to incompleteItems.size,
        stringResource(R.string.today_overdue) to overdueItems.size
    )
    val historyStats = mapOf(
        stringResource(R.string.completed) to historyCompleted.size,
        stringResource(R.string.incomplete) to historyIncomplete.size,
        stringResource(R.string.overdue) to historyOverdue.size
    )

    // 完成时间段统计：针对所有完成的任务，按时间段统计
    val timeBucketOrder = buildTimeBucketOrder(context)

    val completionTimeStats = historyCompleted.groupBy { extractTimeBucket(context, it.completeTime) }
        .mapValues { it.value.size }
        .toList()
        .sortedBy { timeBucketOrder[it.first] ?: Int.MAX_VALUE }


    // UI准备
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val tabs = listOf(
        stringResource(R.string.tab_overview),
        stringResource(R.string.tab_trend),
        stringResource(R.string.tab_summary)
    )
    val mapIcon = mapOf(
        stringResource(R.string.tab_overview) to painterResource(R.drawable.ic_analytics),
        stringResource(R.string.tab_trend) to painterResource(R.drawable.ic_monitor),
        stringResource(R.string.tab_summary) to painterResource(R.drawable.ic_dashboard)
    )
    var selectedTab by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = contentColorFor(Color.Transparent),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(
                    stringResource(R.string.title_activity_overview),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
                ) },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = expressiveTypeModifier
                        )
                    }
                },
                actions = {
                    Row {
                        IconButton(
                            onClick = { showSettings = true },
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_pref),
                                contentDescription = stringResource(R.string.settings_more),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = expressiveTypeModifier
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val iconsList = tabs.map { title ->
                mapIcon[title] ?: painterResource(R.drawable.ic_info)
            }

            TabRow(
                tabs = tabs,
                selectedTabIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                tabIcons = iconsList,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surface) },
                modifier = Modifier.padding(16.dp)
            )

            when (selectedTab) {
                0 ->
                    OverviewStatsScreen(
                        activeStats,
                        historyStats,
                        completionTimeStats,
                        overdueItems,
                        Modifier
                            .background(MaterialTheme.colorScheme.surface),
                    )
                1 ->
                    TrendAnalysisScreen(
                        items,
                        Modifier
                            .background(MaterialTheme.colorScheme.surface),
                    )
                2 ->
                    DashboardScreen(
                        items,
                        activity,
                        Modifier
                            .background(MaterialTheme.colorScheme.surface)
                    )
            }
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                confirmButton = {
                    TextButton(onClick = { showSettings = false }) {
                        Text(stringResource(R.string.accept))
                    }
                },
                text = {
                    // 用 AndroidView 加载我们刚才写的 XML 布局
                    AndroidView(
                        factory = { context ->
                            LayoutInflater.from(context)
                                .inflate(R.layout.dialog_overview_settings, null)
                                .apply {
                                    val seek = findViewById<Slider>(R.id.seekbar_monthly)
                                    val tv = findViewById<TextView>(R.id.text_monthly_value)
                                    val sw = findViewById<MaterialSwitch>(R.id.switch_overdue)

                                    // 初始化控件状态
                                    seek.value = (GlobalUtils.OverviewSettings.monthlyCount).toFloat()
                                    tv.text = context.getString(R.string.xx_months, GlobalUtils.OverviewSettings.monthlyCount)
                                    sw.isChecked = GlobalUtils.OverviewSettings.showOverdueInDaily

                                    // 监听用户操作，实时更新 SharedPreferences
                                    seek.addOnChangeListener { _, _, _ ->
                                        GlobalUtils.OverviewSettings.monthlyCount = seek.value.toInt()
                                        tv.text = context.getString(R.string.xx_months, GlobalUtils.OverviewSettings.monthlyCount)
                                    }
                                    sw.setOnCheckedChangeListener { _, isChecked ->
                                        GlobalUtils.OverviewSettings.showOverdueInDaily = isChecked
                                    }
                                }
                        },
                        update = { /* nothing to do */ }
                    )
                }
            )
        }
    }
}
