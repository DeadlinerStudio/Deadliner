package com.aritxonly.deadliner.ui.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.focus.*
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import com.aritxonly.deadliner.ui.material.glass.SoftGlassFloatingActionButton
import com.aritxonly.deadliner.ui.materialSymbolPainter
import com.aritxonly.deadliner.ui.navigation.MiuixFloatingTabBarDefaults
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.deadliner.domain.focus.*
import com.deadliner.domain.focus.management.*
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Content-only adaptation: keeps OverviewDomainScreen and its Scaffold intact. */
@Composable
internal fun FocusManagementContent(modifier: Modifier, top: Dp, bottom: Dp) {
    var retry by remember { mutableIntStateOf(0) }
    val background = MiuixTheme.colorScheme.surface
    val listBackdrop = if (LocalAdvancedMaterialSpec.current.enabled) rememberLayerBackdrop {
        drawRect(background)
        drawContent()
    } else null
    val loaded by rememberFocusManagement(retry)
    val data = loaded.data
    var expanded by rememberSaveable { mutableStateOf(false) }
    var allocation by rememberSaveable { mutableStateOf(false) }
    var visibleCount by rememberSaveable { mutableIntStateOf(50) }
    var selectedUid by rememberSaveable { mutableStateOf<String?>(null) }
    var targetUid by rememberSaveable { mutableStateOf<String?>(null) }
    var targetKind by rememberSaveable { mutableStateOf(FocusTargetKind.TASK.name) }
    val target = targetUid?.let { FocusTargetRef(FocusTargetKind.valueOf(targetKind), it) }
    val page = remember(data, visibleCount) { data?.historyPage(visibleCount) }
    val listState = rememberLazyListState()
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val actionBottom = navigation + FocusActionBottomSpacing
    val contentBottom = maxOf(bottom, actionBottom + MiuixFloatingTabBarDefaults.Height + 24.dp)
    LaunchedEffect(listState, page?.hasMore, visibleCount) {
        if (page?.hasMore != true) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.let { it.totalItemsCount > 0 && (it.visibleItemsInfo.lastOrNull()?.index ?: 0) >= it.totalItemsCount - 5 } }
            .distinctUntilChanged().collect { if (it) visibleCount += 50 }
    }
    FocusManagementStartHost(data?.startTargets.orEmpty(), loaded.loading, loaded.failed, { retry++ }) { timer, busy, open ->
        BoxWithConstraints(modifier.fillMaxSize()) {
            val layout = focusLayout(minOf(maxWidth - FocusVisuals.PageGutter * 2, FocusVisuals.MaximumWidth).value, LocalDensity.current.fontScale)
            val previousColumns = remember { intArrayOf(layout.historyColumns) }
            val currentColumns by rememberUpdatedState(layout.historyColumns)
            var anchor by remember { mutableStateOf<Pair<String, Int>?>(null) }
            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.let { it.key.toString() to listState.firstVisibleItemScrollOffset } }
                    .collect { position ->
                        if (previousColumns[0] == currentColumns) anchor = position?.takeIf { it.first.startsWith("record-") }
                    }
            }
            LaunchedEffect(layout.historyColumns) {
                if (previousColumns[0] != layout.historyColumns) {
                    anchor?.let { (key, offset) ->
                        focusHistoryAnchorIndex(page?.days.orEmpty(), layout.historyColumns, key.removePrefix("record-"),
                            3 + if (loaded.failed) 1 else 0)?.let { listState.scrollToItem(it, offset) }
                    }
                    previousColumns[0] = layout.historyColumns
                }
            }
            val columnModifier = Modifier.widthIn(max = FocusVisuals.MaximumWidth).fillMaxWidth()
            LazyColumn(Modifier.fillMaxSize().then(if (listBackdrop != null) Modifier.layerBackdrop(listBackdrop) else Modifier).syncTopBarMaterialScroll(listState),
                state = listState, horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(start = FocusVisuals.PageGutter, end = FocusVisuals.PageGutter, top = top + 12.dp, bottom = contentBottom + 24.dp)) {
                if (data == null) item("loading") { FocusPanel(columnModifier) {
                    Text(stringResource(if (loaded.failed) R.string.focus_hub_load_failed else R.string.focus_stats_loading))
                    if (loaded.failed) TextButton(onClick = { retry++ }) { Text(stringResource(R.string.dashboard_retry)) }
                } }
                else {
                    if (loaded.failed) item("stale") { FocusPanel(columnModifier.padding(bottom = 12.dp)) {
                        Text(stringResource(R.string.focus_hub_stale))
                        TextButton(onClick = { retry++ }) { Text(stringResource(R.string.dashboard_retry)) }
                    } }
                    item("dashboard") { FocusManagementDashboard(data, layout, columnModifier.padding(bottom = 16.dp)) }
                    item("allocation") { Box(columnModifier.padding(bottom = 24.dp)) {
                        FocusManagementAllocation(data, expanded, allocation, { expanded = !expanded }, { allocation = it },
                            { ref -> targetUid = ref.uid; targetKind = ref.kind.name })
                    } }
                    item("history-title") { FocusSectionHeading(stringResource(R.string.dashboard_focus_records),
                        stringResource(R.string.focus_hub_record_count, page?.totalCount ?: 0), columnModifier.padding(horizontal = 4.dp, vertical = 8.dp)) }
                    if (page?.totalCount == 0) item("empty") { FocusPanel(columnModifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.focus_no_sessions))
                        Text(stringResource(R.string.focus_hub_empty), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    } }
                    page?.days?.forEach { day ->
                        item("day-${day.date}") { Box(columnModifier.padding(top = 16.dp, bottom = 8.dp)) { FocusManagementDayHeader(day) } }
                        val rows = day.records.chunked(layout.historyColumns)
                        itemsIndexed(rows, key = { _, row -> "record-${row.first().session.uid}" }) { index, records ->
                            val first = index == 0; val last = index == rows.lastIndex
                            Column(columnModifier.animateItem().clip(RoundedCornerShape(
                                topStart = if (first) FocusVisuals.Corner else 0.dp, topEnd = if (first) FocusVisuals.Corner else 0.dp,
                                bottomStart = if (last) FocusVisuals.Corner else 0.dp, bottomEnd = if (last) FocusVisuals.Corner else 0.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainer)) {
                                Row {
                                    records.forEach { record -> Box(Modifier.weight(1f)) {
                                        FocusManagementRecordRow(record, framed = false, onClick = { selectedUid = record.session.uid })
                                    } }
                                    repeat(layout.historyColumns - records.size) { Spacer(Modifier.weight(1f)) }
                                }
                                if (!last) Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(0.5.dp).background(MiuixTheme.colorScheme.dividerLine))
                            }
                        }
                    }
                    if (page?.hasMore == true) item("more") { TextButton(onClick = { visibleCount += 50 }, modifier = columnModifier) { Text(stringResource(R.string.focus_hub_more)) } }
                }
            }
            FocusBottomChrome(Modifier.align(Alignment.BottomCenter))
            Row(Modifier.align(Alignment.BottomEnd).padding(end = FocusVisuals.PageGutter, bottom = actionBottom),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(timer != null, enter = fadeIn(FocusVisuals.motion()), exit = fadeOut(FocusVisuals.motion())) {
                    if (timer != null) SoftGlassSurface(shape = RoundedCornerShape(FocusVisuals.Corner), backdrop = listBackdrop,
                        modifier = Modifier.widthIn(max = 210.dp).clip(RoundedCornerShape(FocusVisuals.Corner)).clickable(onClick = open)) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(timer.taskTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MiuixTheme.textStyles.body2)
                            Text(stringResource(if (timer.state == FocusTimerState.PAUSED) R.string.focus_paused else R.string.focus_running) + " · " + formatFocusClock(timer.elapsedSeconds),
                                style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                }
                SoftGlassFloatingActionButton(onClick = open, enabled = !busy, size = MiuixFloatingTabBarDefaults.Height, backdrop = listBackdrop,
                    containerColor = MiuixTheme.colorScheme.primary, contentColor = MiuixTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("focus-management-fab")) {
                    Icon(materialSymbolPainter(when {
                        timer == null -> "timer"
                        timer.state == FocusTimerState.PAUSED -> "timer_pause"
                        else -> "timer_play"
                    }), stringResource(if (timer == null) R.string.focus_hub_start else R.string.focus_hub_return), Modifier.size(MiuixFloatingTabBarDefaults.DetachedIconSize))
                }
            }
        }
    }
    FocusManagementHistorySheet(data, target, onDismiss = { targetUid = null }, onSelectRecord = { selectedUid = it })
    FocusManagementRecordSheet(data?.records?.firstOrNull { it.session.uid == selectedUid }, selectedUid != null,
        onDismiss = { selectedUid = null }, onChanged = { retry++ })
}

@Composable
private fun FocusManagementDashboard(data: FocusManagementSnapshot, layout: FocusLayout, modifier: Modifier = Modifier) {
    if (layout.dashboardWide) Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(FocusVisuals.Gap)) {
        FocusManagementToday(data, Modifier.weight(1f).fillMaxHeight())
        FocusManagementTotal(data, Modifier.weight(1f).fillMaxHeight())
        FocusManagementWeek(data, Modifier.weight(2f).fillMaxHeight())
    } else Column(modifier, verticalArrangement = Arrangement.spacedBy(FocusVisuals.Gap)) {
        if (layout.summaryColumns == 2) Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(FocusVisuals.Gap)) {
            FocusManagementToday(data, Modifier.weight(1f).fillMaxHeight())
            FocusManagementTotal(data, Modifier.weight(1f).fillMaxHeight())
        } else { FocusManagementToday(data); FocusManagementTotal(data) }
        FocusManagementWeek(data)
    }
}

/** Locate an existing record after columns reflow, including preceding day headers. */
internal fun focusHistoryAnchorIndex(days: List<FocusManagementDay>, columns: Int, uid: String, prefix: Int): Int? {
    require(columns > 0)
    var index = prefix
    days.forEach { day ->
        val record = day.records.indexOfFirst { it.session.uid == uid }
        if (record >= 0) return index + 1 + record / columns
        index += 1 + (day.records.size + columns - 1) / columns
    }
    return null
}
