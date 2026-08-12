package com.aritxonly.deadliner.ui.main.shared

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.iconResource
import com.deadliner.domain.capture.CaptureItem
import com.deadliner.domain.habit.Habit
import com.deadliner.domain.habit.HabitStatus
import com.deadliner.domain.habit.HabitStatusAction
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskState
import java.time.Instant

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
    val habitState by habitViewModel.uiState.collectAsState()

    LaunchedEffect(query, state.allTasks.size, state.allHabits.size) {
        searchViewModel.search(query)
    }

    val tasks = state.matchedTasks.filter { task ->
        when (scope) {
            MainSearchScope.ALL -> true
            MainSearchScope.ACTIVE -> task.state in setOf(TaskState.ACTIVE, TaskState.COMPLETED, TaskState.ABANDONED)
            MainSearchScope.ARCHIVE -> task.state in setOf(TaskState.ARCHIVED, TaskState.ABANDONED_ARCHIVED)
        }
    }
    val habits = state.matchedHabits.filter { habit ->
        when (scope) {
            MainSearchScope.ALL -> true
            MainSearchScope.ACTIVE -> habit.status == HabitStatus.ACTIVE
            MainSearchScope.ARCHIVE -> habit.status == HabitStatus.ARCHIVED
        }
    }
    val captures = buildInspirationSearchResults(query, scope, inspirations)

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MainSearchScope.entries.forEach { item ->
                FilterChip(
                    selected = scope == item,
                    onClick = { onScopeChange(item) },
                    label = { Text(stringResource(item.labelRes)) },
                )
            }
        }

        if (query.isBlank()) {
            Text(
                text = stringResource(R.string.search_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (tasks.isNotEmpty()) {
                item { SearchSectionHeader(stringResource(R.string.search_group_task)) }
                items(tasks, key = Task::uid) { task ->
                    SearchTaskRow(task, activity)
                }
            }
            if (habits.isNotEmpty()) {
                item { SearchSectionHeader(stringResource(R.string.search_group_habit)) }
                items(habits, key = Habit::uid) { habit ->
                    val status = habitState.visibleHabits.firstOrNull { it.habit.uid == habit.uid }
                    SearchHabitRow(
                        habit = habit,
                        progress = status?.let { "${it.doneCount}/${it.targetCount}" },
                        onClick = {
                            if (habit.status == HabitStatus.ACTIVE) {
                                habitViewModel.toggleHabitOnSelectedDate(habit.uid)
                            } else {
                                habitViewModel.applyHabitStatusNow(
                                    habit.uid,
                                    HabitStatusAction.RESTORE,
                                    Instant.now().toString(),
                                )
                                searchViewModel.initialize()
                            }
                        },
                    )
                }
            }
            if (captures.isNotEmpty()) {
                item { SearchSectionHeader(stringResource(R.string.capture_title)) }
                items(captures, key = CaptureItem::uid) { capture ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            activity.startActivity(Intent(activity, AddDDLActivity::class.java).apply {
                                putExtra(AddDDLActivity.EXTRA_PREFILL_TEXT, capture.text)
                                putExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, true)
                            })
                        },
                    ) {
                        Text(capture.text, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SearchTaskRow(task: Task, activity: MainActivity) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            activity.startActivity(DeadlineDetailActivity.newIntent(activity, task.uid))
        },
    ) {
        ListItem(
            headlineContent = { Text(task.title) },
            supportingContent = { Text(task.note.ifBlank { task.dueAt.orEmpty() }) },
            trailingContent = {
                if (task.state == TaskState.ARCHIVED || task.state == TaskState.ABANDONED_ARCHIVED) {
                    IconButton(onClick = {
                        KmpV6Runtime.archiveViewModel.unarchiveTask(task.uid)
                        KmpV6Runtime.taskListViewModel.loadNow()
                        KmpV6Runtime.searchViewModel.initialize()
                    }) {
                        Icon(iconResource(R.drawable.ic_back), contentDescription = stringResource(R.string.unarchive))
                    }
                }
            },
        )
    }
}

@Composable
private fun SearchHabitRow(habit: Habit, progress: String?, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        ListItem(
            headlineContent = { Text(habit.name) },
            supportingContent = { Text(habit.description ?: progress.orEmpty()) },
            trailingContent = {
                Icon(
                    iconResource(if (habit.status == HabitStatus.ARCHIVED) R.drawable.ic_back else R.drawable.ic_done),
                    contentDescription = null,
                )
            },
        )
    }
}
