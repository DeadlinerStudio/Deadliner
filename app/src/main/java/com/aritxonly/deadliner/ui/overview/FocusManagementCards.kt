package com.aritxonly.deadliner.ui.overview

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.focus.*
import com.aritxonly.deadliner.ui.materialSymbolPainter
import com.deadliner.domain.focus.*
import com.deadliner.domain.focus.management.*
import java.time.format.DateTimeFormatter
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FocusManagementToday(data: FocusManagementSnapshot, modifier: Modifier = Modifier) = FocusSummaryTile(
    stringResource(R.string.focus_stats_today), data.today.totalDurationSeconds, data.today.sessionCount,
    data.asOfDate, emphasized = true, modifier = modifier)

@Composable
internal fun FocusManagementTotal(data: FocusManagementSnapshot, modifier: Modifier = Modifier) {
    val split = data.allTime.summary.durationByTarget
    FocusSummaryTile(stringResource(R.string.focus_total), data.allTime.summary.totalDurationSeconds,
        data.allTime.summary.sessionCount,
        stringResource(R.string.focus_hub_split, focusDuration(split?.taskDurationSeconds ?: 0), focusDuration(split?.habitDurationSeconds ?: 0)),
        modifier = modifier)
}

@Composable
private fun FocusSummaryTile(title: String, seconds: Long, count: Int, supporting: String,
    emphasized: Boolean = false, modifier: Modifier = Modifier) = FocusPanel(modifier, emphasized = emphasized) {
    Text(title, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    Text(buildAnnotatedString {
        Regex("[0-9]+|[^0-9]+").findAll(focusDuration(seconds)).forEach { part ->
            withStyle(SpanStyle(fontSize = if (part.value.first().isDigit()) 30.sp else 13.sp)) { append(part.value) }
        }
    }, style = MiuixTheme.textStyles.title2.copy(fontSize = 30.sp,
        fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = if (emphasized) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceContainer)
    Text(stringResource(R.string.focus_stats_session_count_short, count), style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    Spacer(Modifier.height(2.dp))
    Text(supporting, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
}

@Composable
internal fun FocusManagementWeek(data: FocusManagementSnapshot, modifier: Modifier = Modifier) = FocusPanel(modifier) {
    FocusSectionHeading(stringResource(R.string.focus_stats_recent_days), focusDuration(data.recentSevenDays.summary.totalDurationSeconds))
    FocusDurationChart(data.recentSevenDays.daily, FocusChartKind.DAILY, tag = "focus-management-week")
}

@Composable
internal fun FocusManagementAllocation(data: FocusManagementSnapshot, expanded: Boolean, categories: Boolean,
    onExpand: () -> Unit, onCategories: (Boolean) -> Unit, onTarget: (FocusTargetRef) -> Unit) {
    var showAll by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, FocusVisuals.motion(), label = "focus-allocation-chevron")
    val targetLabel = stringResource(R.string.task); val habitLabel = stringResource(R.string.habit)
    val uncategorized = stringResource(R.string.focus_stats_uncategorized)
    val entries = remember(data, categories, targetLabel, habitLabel, uncategorized) { allocationItems(data, categories, targetLabel, habitLabel, uncategorized) }
    FocusPanel {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onExpand).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.focus_stats_time_allocation), style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium))
                Text(stringResource(R.string.focus_hub_allocation_summary, focusDuration(data.allTime.summary.totalDurationSeconds)),
                    style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Icon(materialSymbolPainter("expand_more"), null, Modifier.size(com.aritxonly.deadliner.ui.overviewChevronSize()).rotate(rotation), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        }
        AllocationPreview(data)
        AnimatedVisibility(expanded, enter = expandVertically(FocusVisuals.motion(), expandFrom = Alignment.Top) + fadeIn(FocusVisuals.motion()),
            exit = shrinkVertically(FocusVisuals.motion(), shrinkTowards = Alignment.Top) + fadeOut(FocusVisuals.motion())) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusChoiceRow(listOf(stringResource(R.string.focus_hub_by_target), stringResource(R.string.focus_hub_by_category)),
                    if (categories) 1 else 0, true, { onCategories(it == 1) }, segmented = true)
                AnimatedContent(categories, transitionSpec = {
                    (fadeIn(FocusVisuals.motion()) togetherWith fadeOut(FocusVisuals.motion()))
                        .using(SizeTransform(sizeAnimationSpec = { _, _ -> FocusVisuals.motion() }))
                }, label = "focus-allocation-kind") { kind ->
                    val current = remember(data, kind, targetLabel, habitLabel, uncategorized) { allocationItems(data, kind, targetLabel, habitLabel, uncategorized) }
                    BoxWithConstraints {
                        val columns = focusLayout(maxWidth.value, LocalDensity.current.fontScale).historyColumns
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            current.take(5).chunked(columns).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    row.forEach { entry -> AllocationEntry(entry, data.allTime.summary.totalDurationSeconds, Modifier.weight(1f), onTarget) }
                                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            if (current.isEmpty()) Text(stringResource(R.string.focus_no_sessions), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                }
                if (entries.size > 5) TextButton(onClick = { showAll = true }) { Text(stringResource(R.string.focus_hub_all_allocation, entries.size)) }
            }
        }
    }
    MiuixBottomSheet(show = showAll, title = stringResource(if (categories) R.string.focus_hub_by_category else R.string.focus_hub_by_target),
        onDismissRequest = { showAll = false }) { padding ->
        val state = rememberLazyListState()
        LazyColumn(state = state, modifier = Modifier.syncTopBarMaterialScroll(state).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 20.dp)) {
            items(entries, key = { it.key }) { entry -> AllocationEntry(entry, data.allTime.summary.totalDurationSeconds) { showAll = false; onTarget(it) } }
        }
    }
}

private data class AllocationItem(val key: String, val title: String, val seconds: Long, val count: Int, val target: FocusTargetRef?)
private fun allocationItems(data: FocusManagementSnapshot, categories: Boolean, task: String, habit: String, uncategorized: String): List<AllocationItem> =
    if (categories) data.allTime.categories.map { AllocationItem(it.categoryUid ?: "unclassified", it.categoryTitle ?: uncategorized, it.durationSeconds, it.sessionCount, null) }
    else data.allTime.targets.map { AllocationItem("${it.target.kind}:${it.target.uid}", it.title + " · " + if (it.target.kind == FocusTargetKind.TASK) task else habit, it.durationSeconds, it.sessionCount, it.target) }

@Composable
private fun AllocationPreview(data: FocusManagementSnapshot) {
    val split = data.allTime.summary.durationByTarget
    val task = split?.taskDurationSeconds ?: 0; val habit = split?.habitDurationSeconds ?: 0
    val ratio = (task.toDouble() / (task + habit).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    FocusAllocationProgress(ratio, trackColor = if (task + habit > 0) MiuixTheme.colorScheme.primary.copy(alpha = 0.4f)
        else MiuixTheme.colorScheme.dividerLine)
    Text(stringResource(R.string.focus_hub_split, focusDuration(task), focusDuration(habit)), style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
}

@Composable
private fun AllocationEntry(entry: AllocationItem, total: Long, modifier: Modifier = Modifier, onTarget: (FocusTargetRef) -> Unit) {
    val ratio = (entry.seconds.toDouble() / total.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    Column(modifier.fillMaxWidth().then(if (entry.target != null) Modifier.clickable(role = Role.Button) { onTarget(entry.target) } else Modifier)
        .padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(entry.title, Modifier.weight(1f), maxLines = 2, style = MiuixTheme.textStyles.body2)
            Text(focusDuration(entry.seconds), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.primary)
        }
        Text(stringResource(R.string.focus_hub_allocation_share, ratio * 100f, entry.count), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        FocusAllocationProgress(ratio)
    }
}

/** Match the project's determinate progress component and its animation contract. */
@Composable
private fun FocusAllocationProgress(ratio: Float,
    trackColor: androidx.compose.ui.graphics.Color = MiuixTheme.colorScheme.dividerLine) {
    val progress by animateFloatAsState(ratio, animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "focus-allocation-progress")
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp),
        color = MiuixTheme.colorScheme.primary, trackColor = trackColor, gapSize = 0.dp, drawStopIndicator = {})
}

@Composable
internal fun focusManagedTargetStatus(status: FocusManagedTargetStatus): String = stringResource(when (status) {
    FocusManagedTargetStatus.ACTIVE -> R.string.focus_hub_active
    FocusManagedTargetStatus.COMPLETED -> R.string.completed
    FocusManagedTargetStatus.ARCHIVED -> R.string.focus_hub_archived
    FocusManagedTargetStatus.ABANDONED -> R.string.focus_hub_abandoned
    FocusManagedTargetStatus.DELETED -> R.string.focus_hub_deleted
})

@Composable
internal fun FocusManagementDayHeader(day: FocusManagementDay) = FocusSectionHeading(day.date,
    stringResource(R.string.focus_global_today_value, focusDuration(day.durationSeconds), day.sessionCount), Modifier.padding(horizontal = 4.dp))

@Composable
internal fun FocusManagementRecordRow(record: FocusManagementRecord, framed: Boolean = true, onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 80.dp).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(record.title, style = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.Medium), maxLines = 2)
                val time = runCatching { java.time.Instant.parse(record.session.startedAt).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("—")
                Text(stringResource(if (record.session.boundTarget.kind == FocusTargetKind.TASK) R.string.task else R.string.habit) + " · " + time + " · " +
                    stringResource(if (record.session.mode == FocusTimerMode.STOPWATCH) R.string.focus_stopwatch_mode else R.string.focus_countdown_mode),
                    style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                if (record.targetStatus != FocusManagedTargetStatus.ACTIVE) Text(focusManagedTargetStatus(record.targetStatus),
                    style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Text(focusDuration(record.session.durationSeconds), style = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.Medium))
            Icon(materialSymbolPainter("chevron_right"), null, Modifier.size(com.aritxonly.deadliner.ui.overviewChevronSize(18.dp)), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        }
    }
    if (framed) FocusPanel(padding = 0.dp) { content() } else content()
}
