package com.aritxonly.deadliner.ui.main.shared

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.archive.ArchivedHabitCard
import com.aritxonly.deadliner.ui.archive.ArchivedTaskCard
import com.aritxonly.deadliner.ui.archive.toArchiveCardModel
import com.aritxonly.deadliner.ui.main.DDLItemCardSimplified
import com.aritxonly.deadliner.ui.main.simplified.presentationLabel
import com.aritxonly.deadliner.ui.main.simplified.progressText
import com.aritxonly.deadliner.ui.main.simplified.scheduleHintText
import com.deadliner.domain.capture.CaptureItem
import com.deadliner.domain.habit.Habit
import com.deadliner.domain.habit.HabitStatus
import com.deadliner.domain.habit.HabitStatusAction
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskState
import com.deadliner.viewmodel.habit.HabitDayStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@Composable
fun MainSearchResultsContent(
    query: String,
    scope: MainSearchScope,
    onScopeChange: (MainSearchScope) -> Unit,
    activity: MainActivity,
    inspirations: List<CaptureItem> = emptyList(),
) {
    val searchViewModel = KmpV6Runtime.searchViewModel
    val habitViewModel = KmpV6Runtime.habitListViewModel
    val state by searchViewModel.uiState.collectAsState()

    LaunchedEffect(searchViewModel) { searchViewModel.initialize() }
    LaunchedEffect(query) { searchViewModel.search(query) }

    val activeTasks = state.matchedTasks.filter {
        scope != MainSearchScope.ARCHIVE && it.state in setOf(TaskState.ACTIVE, TaskState.COMPLETED, TaskState.ABANDONED)
    }
    val archivedTasks = state.matchedTasks.filter {
        scope != MainSearchScope.ACTIVE && it.state in setOf(TaskState.ARCHIVED, TaskState.ABANDONED_ARCHIVED)
    }
    val activeHabits = state.matchedHabits.filter {
        scope != MainSearchScope.ARCHIVE && it.status == HabitStatus.ACTIVE
    }
    val archivedHabits = state.matchedHabits.filter {
        scope != MainSearchScope.ACTIVE && it.status == HabitStatus.ARCHIVED
    }
    val habitStatusesByUid = remember(state.matchedHabitStatuses) {
        state.matchedHabitStatuses.associateBy { it.habit.uid }
    }
    val captures = buildInspirationSearchResults(query, scope, inspirations)
    val resultCount = activeTasks.size + archivedTasks.size + activeHabits.size + archivedHabits.size + captures.size

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .mainListContainerClip(),
    ) {
        item {
            SearchScopePicker(selectedScope = scope, onScopeChange = onScopeChange)
        }

        when {
            query.isBlank() -> item {
                SearchMessageCard(
                    title = stringResource(R.string.search_empty_query_title),
                    body = stringResource(R.string.search_empty_query_hint),
                )
            }

            resultCount == 0 && !state.isLoading -> item {
                SearchMessageCard(
                    title = stringResource(R.string.search_no_result_title),
                    body = stringResource(R.string.search_no_result_hint),
                )
            }

            else -> {
                if (captures.isNotEmpty()) {
                    item { SearchSectionHeader(stringResource(R.string.search_group_inspiration), R.drawable.msr_format_quote_wght400) }
                    items(captures, key = CaptureItem::uid) { capture ->
                        SearchInspirationCard(
                            item = capture,
                            activity = activity,
                            onDelete = { KmpV6Runtime.captureViewModel.deleteCapture(capture.uid) },
                        )
                    }
                }
                if (activeTasks.isNotEmpty()) {
                    item { SearchSectionHeader(stringResource(R.string.search_group_task), R.drawable.msr_check_circle_wght400) }
                    items(activeTasks, key = Task::uid) { task -> SearchTaskCard(task, activity) }
                }
                if (activeHabits.isNotEmpty()) {
                    item { SearchSectionHeader(stringResource(R.string.search_group_habit), R.drawable.msr_eco_wght400) }
                    items(activeHabits, key = Habit::uid) { habit ->
                        habitStatusesByUid[habit.uid]?.let { status ->
                            SearchHabitCard(
                                status = status,
                                onClick = { habitViewModel.toggleHabitOnSelectedDate(habit.uid) },
                            )
                        }
                    }
                }
                if (archivedTasks.isNotEmpty()) {
                    item { SearchSectionHeader(stringResource(R.string.search_group_archived_task), R.drawable.msr_inbox_wght400) }
                    items(archivedTasks, key = Task::uid) { task ->
                        ArchivedTaskCard(
                            item = task.toArchiveCardModel(),
                            onRestore = {
                                KmpV6Runtime.archiveViewModel.unarchiveTask(task.uid)
                                KmpV6Runtime.taskListViewModel.loadNow()
                                searchViewModel.initialize()
                            },
                        )
                    }
                }
                if (archivedHabits.isNotEmpty()) {
                    item { SearchSectionHeader(stringResource(R.string.search_group_archived_habit), R.drawable.msr_inbox_wght400) }
                    items(archivedHabits, key = Habit::uid) { habit ->
                        ArchivedHabitCard(
                            habit = habit.toArchiveCardModel(),
                            onRestore = {
                                habitViewModel.applyHabitStatusNow(
                                    habit.uid,
                                    HabitStatusAction.RESTORE,
                                    Instant.now().toString(),
                                )
                                searchViewModel.initialize()
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchScopePicker(
    selectedScope: MainSearchScope,
    onScopeChange: (MainSearchScope) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        MainSearchScope.entries.forEachIndexed { index, scope ->
            val selected = scope == selectedScope
            val iconSlotWidth by animateDpAsState(
                if (selected) 20.dp else 0.dp,
                tween(220),
                label = "search-scope-icon-slot",
            )
            val iconAlpha by animateFloatAsState(
                if (selected) 1f else 0f,
                tween(180),
                label = "search-scope-icon-alpha",
            )
            val iconScale by animateFloatAsState(
                if (selected) 1f else 0.82f,
                tween(220),
                label = "search-scope-icon-scale",
            )
            val textShift by animateDpAsState(
                if (selected) 1.5.dp else 0.dp,
                tween(220),
                label = "search-scope-text-shift",
            )
            ToggleButton(
                checked = selected,
                onCheckedChange = { onScopeChange(scope) },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                shapes = searchScopeButtonShapes(index, MainSearchScope.entries.lastIndex),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(width = iconSlotWidth, height = 20.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Icon(
                            painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_check_wght400),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).alpha(iconAlpha).graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                        )
                    }
                    Text(
                        text = stringResource(scope.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(textShift.roundToPx(), 0)
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun searchScopeButtonShapes(index: Int, lastIndex: Int): ToggleButtonShapes = when (index) {
    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}

@Composable
private fun SearchMessageCard(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_search_wght400),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(40.dp).alpha(0.7f),
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun SearchSectionHeader(title: String, iconRes: Int) {
    Row(
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = com.aritxonly.deadliner.ui.symbolPainterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchInspirationCard(item: CaptureItem, activity: MainActivity, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    fun openEditor(typeIndex: Int, autoRunAi: Boolean) {
        activity.startActivity(Intent(activity, AddDDLActivity::class.java).apply {
            putExtra("EXTRA_CURRENT_TYPE", typeIndex)
            putExtra(AddDDLActivity.EXTRA_PREFILL_TEXT, item.text)
            putExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, autoRunAi)
        })
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { openEditor(0, true) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_format_quote_wght400),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp).size(18.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(item.text, style = MaterialTheme.typography.bodyLarge, maxLines = 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_more_vert_wght400), contentDescription = stringResource(R.string.settings_more))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            SearchCaptureMenuItem(R.string.capture_ai_task) { openEditor(0, true); menuExpanded = false }
                            SearchCaptureMenuItem(R.string.capture_ai_habit) { openEditor(1, true); menuExpanded = false }
                            SearchCaptureMenuItem(R.string.capture_direct_task) { openEditor(0, false); menuExpanded = false }
                            SearchCaptureMenuItem(R.string.capture_direct_habit) { openEditor(1, false); menuExpanded = false }
                            SearchCaptureMenuItem(R.string.capture_delete) { onDelete(); menuExpanded = false }
                        }
                    }
                }
                Text(
                    text = formatCaptureRelativeTime(activity, item.updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.search_inspiration_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchCaptureMenuItem(labelRes: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(labelRes)) }, onClick = onClick)
}

@Composable
private fun SearchTaskCard(task: Task, activity: MainActivity) {
    val start = task.startAt?.let(GlobalUtils::parseDateTime)
    val end = task.dueAt?.let(GlobalUtils::parseDateTime)
    val now = LocalDateTime.now()
    val status = if (task.state == TaskState.ACTIVE) {
        DDLStatus.calculateStatus(start, end, now, false)
    } else {
        DDLStatus.COMPLETED
    }
    val remaining = when (task.state) {
        TaskState.ABANDONED, TaskState.ABANDONED_ARCHIVED -> stringResource(R.string.abandoned)
        TaskState.COMPLETED, TaskState.ARCHIVED -> stringResource(R.string.completed)
        else -> GlobalUtils.buildRemainingTime(activity, start, end, true, now)
    }
    DDLItemCardSimplified(
        title = task.title,
        remainingTimeAlt = remaining,
        note = task.note,
        progress = computeProgress(start, end, now),
        isStarred = task.isStarred,
        status = status,
        useDisabledCompletedStyle = task.state in setOf(TaskState.ABANDONED, TaskState.ABANDONED_ARCHIVED),
        onClick = { activity.startActivity(DeadlineDetailActivity.newIntent(activity, task.uid)) },
    )
}

@Composable
private fun SearchHabitCard(status: HabitDayStatus, onClick: () -> Unit) {
    val context = LocalContext.current
    val habit = status.habit
    val statusText = if (status.scheduleState.isDue) {
        status.progressText()
    } else {
        status.scheduleHintText(context) ?: habit.presentationLabel(context)
    }
    val actionEnabled = status.canApplyPrimaryAction
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = actionEnabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_eco_wght400),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                habit.description?.takeIf(String::isNotBlank)?.let { description ->
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                painter = com.aritxonly.deadliner.ui.symbolPainterResource(
                    if (status.isCompleted || actionEnabled) R.drawable.msr_task_alt_wght400 else R.drawable.msr_eco_wght400,
                ),
                contentDescription = null,
                tint = if (actionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatCaptureRelativeTime(context: android.content.Context, timestamp: String): String {
    val updatedAt = runCatching { Instant.parse(timestamp) }.getOrNull() ?: return timestamp
    val diff = Duration.between(updatedAt, Instant.now()).toMillis().absoluteValue
    val minutes = Duration.ofMillis(diff).toMinutes()
    val hours = Duration.ofMillis(diff).toHours()
    val days = Duration.ofMillis(diff).toDays()
    return when {
        minutes < 1 -> context.getString(R.string.capture_relative_just_now)
        minutes < 60 -> context.getString(R.string.capture_relative_minutes_ago, minutes)
        hours < 24 -> context.getString(R.string.capture_relative_hours_ago, hours)
        days < 7 -> context.getString(R.string.capture_relative_days_ago, days)
        else -> DateTimeFormatter.ofPattern(context.getString(R.string.capture_date_short_pattern))
            .format(updatedAt.atZone(ZoneId.systemDefault()))
    }
}
