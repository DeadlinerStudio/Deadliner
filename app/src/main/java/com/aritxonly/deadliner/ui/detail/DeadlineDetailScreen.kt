package com.aritxonly.deadliner.ui.detail

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.aritxonly.deadliner.ui.base.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.calendar.CalendarHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.deadliner.domain.task.Task
import com.deadliner.domain.task.TaskAction
import com.deadliner.domain.task.TaskState
import com.deadliner.domain.task.TaskSubtask
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.symbolPainterResource
import com.aritxonly.deadliner.ui.category.CategoryPresentation
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeadlineDetailScreen(
    applicationContext: Context,
    activityContext: Context,
    deadline: Task,
    onClose: () -> Unit,
    onToggleStar: (Boolean) -> Unit,
    categories: List<CategoryPresentation>,
    onEdit: () -> Unit,
    onOpenFocusTimer: () -> Unit,
    onApplyTaskAction: (TaskAction) -> Unit,
    onDelete: () -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onUpdateSubtask: (String, String) -> Unit,
    onCalendarEventCreated: (Long) -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    var isStarred by remember(deadline.uid) { mutableStateOf(deadline.isStarred) }
    var showGiveUpDialog by rememberSaveable(deadline.uid) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(deadline.uid) { mutableStateOf(false) }

    LaunchedEffect(deadline.isStarred) {
        isStarred = deadline.isStarred
    }

    val scrollState = rememberScrollState()

    fun executeAction(action: DetailAction) {
        when (action) {
            DetailAction.MARK_COMPLETE -> {
                onApplyTaskAction(TaskAction.MARK_COMPLETE)
            }

            DetailAction.FOCUS -> onOpenFocusTimer()

            DetailAction.GIVE_UP -> {
                showGiveUpDialog = true
            }

            DetailAction.RESTORE_ACTIVE -> {
                onApplyTaskAction(TaskAction.RESTORE_ACTIVE)
            }

            DetailAction.ARCHIVE -> {
                onApplyTaskAction(TaskAction.MARK_ARCHIVE)
            }

            DetailAction.UNARCHIVE -> {
                onApplyTaskAction(TaskAction.UNARCHIVE)
            }

            DetailAction.DELETE -> {
                showDeleteDialog = true
            }

            DetailAction.SAVE_TO_CALENDAR -> {
                val calendarHelper = CalendarHelper(applicationContext)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val eventId = calendarHelper.insertEvent(deadline)
                        onCalendarEventCreated(eventId)
                        Toast.makeText(
                            activityContext,
                            R.string.add_calendar_success,
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            activityContext,
                            activityContext.getString(R.string.add_calendar_failed, e.toString()),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    val sortedSubTasks = remember(deadline.subtasks) {
        deadline.subtasks.sortedWith(
            compareBy<TaskSubtask> { it.isCompleted }
                .thenBy { it.sortOrder }
                .thenBy { it.uid }
        )
    }

    AdaptiveMaterialScaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = deadline.title,
                navigationIcon = {
                    IconButton(modifier = Modifier.miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape), onClick = onClose) {
                        Icon(
                            painter = iconResource(R.drawable.msr_close_wght400),
                            contentDescription = stringResource(R.string.close),
                            modifier = expressiveTypeModifier
                        )
                    }
                },
                titleTextStyle = MaterialTheme.typography.titleLarge,
                mode = TopAppBarStyle.SMALL,
                forceMaterial3 = true,
                actions = {
                    TopBarIconButton(onClick = {
                        isStarred = !isStarred
                        onToggleStar(isStarred)
                    }) {
                        Icon(
                            painter = symbolPainterResource(
                                if (isStarred) R.drawable.msr_star_fill_wght400 else R.drawable.msr_star_wght400
                            ),
                            contentDescription = stringResource(R.string.star),
                            tint = if (isStarred) Color(0xFFFFE819) else MaterialTheme.colorScheme.onSurface,
                            modifier = expressiveTypeModifier
                        )
                    }
                }
            )
        },
        bottomBar = {
            DetailBottomActions(
                state = deadline.state,
                onActionClick = ::executeAction
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .background(MaterialTheme.colorScheme.surface)
                .syncTopBarMaterialScroll(scrollState)
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressCard(deadline = deadline)
            DetailTaskMetaCard(deadline = deadline, categories = categories, onEdit = onEdit)
            NoteCard(note = deadline.note)
            DetailPlanCard(
                taskUid = deadline.uid,
                subTasks = sortedSubTasks,
                onAdd = onAddSubtask,
                onToggle = onToggleSubtask,
                onDelete = onDeleteSubtask,
                onUpdate = onUpdateSubtask,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showGiveUpDialog) {
        AlertDialog(
            show = true,
            onDismissRequest = { showGiveUpDialog = false },
            title = { Text(stringResource(R.string.confirm_give_up_title)) },
            text = { Text(stringResource(R.string.confirm_give_up_message)) },
            miuixTitle = activityContext.getString(R.string.confirm_give_up_title),
            miuixSummary = activityContext.getString(R.string.confirm_give_up_message),
            confirmButton = {
                TextButton(
                    onClick = {
                        showGiveUpDialog = false
                        onApplyTaskAction(TaskAction.MARK_GIVE_UP)
                    }
                ) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGiveUpDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            show = true,
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.alert_delete_title)) },
            text = { Text(stringResource(R.string.alert_delete_message)) },
            miuixTitle = activityContext.getString(R.string.alert_delete_title),
            miuixSummary = activityContext.getString(R.string.alert_delete_message),
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }


}

@Composable
private fun ProgressCard(deadline: Task) {
    val startTime = deadline.startAt?.let(GlobalUtils::safeParseDateTime) ?: LocalDateTime.now()
    val endTime = deadline.dueAt?.let(GlobalUtils::safeParseDateTime) ?: startTime
    val now = LocalDateTime.now()

    val total = Duration.between(startTime, endTime).toMillis().coerceAtLeast(1L)
    val elapsed = Duration.between(startTime, now).toMillis().coerceIn(0L, total)
    val targetProgress = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 650),
        label = "detail_progress"
    )

    val remainingDuration = Duration.between(now, endTime)
    val remainingText = if (remainingDuration.isNegative) {
        stringResource(R.string.overdue)
    } else {
        formatDuration(LocalContext.current, remainingDuration)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_progress_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f)
                )
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

}

@Composable
private fun NoteCard(note: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_note_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            SelectionContainer {
                Text(
                    text = note.ifBlank { stringResource(R.string.detail_note_empty) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(context: Context, duration: Duration): String {
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()

    return if (days > 0) {
        context.getString(R.string.duration_full, days, hours, minutes)
    } else {
        context.getString(R.string.duration_hours_minutes, hours, minutes)
    }
}
