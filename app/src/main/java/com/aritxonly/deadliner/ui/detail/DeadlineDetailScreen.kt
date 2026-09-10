package com.aritxonly.deadliner.ui.detail

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.aritxonly.deadliner.ui.base.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
import com.aritxonly.deadliner.ui.base.RadioButton
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.symbolPainterResource
import com.aritxonly.deadliner.ui.category.CategoryAssignmentField
import com.aritxonly.deadliner.ui.category.CategoryPickerSheet
import com.aritxonly.deadliner.ui.category.CategoryPresentation
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.BlendColorEntry

private enum class DetailAction(
    val iconRes: Int,
    val labelRes: Int
) {
    MARK_COMPLETE(R.drawable.msr_task_alt_wght400, R.string.mark_complete),
    GIVE_UP(R.drawable.msr_flag_wght400, R.string.give_up_task),
    RESTORE_ACTIVE(R.drawable.msr_undo_wght400, R.string.restore_active),
    ARCHIVE(R.drawable.msr_move_to_inbox_wght400, R.string.archive),
    UNARCHIVE(R.drawable.msr_undo_wght400, R.string.unarchive),
    DELETE(R.drawable.msr_delete_wght400, R.string.delete),
    SAVE_TO_CALENDAR(R.drawable.msr_event_wght400, R.string.save_and_add_to_calendar)
}

private fun availableActions(state: TaskState): List<DetailAction> {
    val head = when (state) {
        TaskState.ACTIVE -> listOf(DetailAction.MARK_COMPLETE, DetailAction.GIVE_UP)
        TaskState.COMPLETED,
        TaskState.ABANDONED -> listOf(DetailAction.RESTORE_ACTIVE, DetailAction.ARCHIVE)
        TaskState.ARCHIVED,
        TaskState.ABANDONED_ARCHIVED -> listOf(DetailAction.UNARCHIVE)
    }

    return head + listOf(DetailAction.DELETE, DetailAction.SAVE_TO_CALENDAR)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeadlineDetailScreen(
    applicationContext: Context,
    activityContext: Context,
    deadline: Task,
    onClose: () -> Unit,
    onToggleStar: (Boolean) -> Unit,
    categories: List<CategoryPresentation>,
    onSetCategory: (String?) -> Unit,
    onManageCategories: () -> Unit,
    onApplyTaskAction: (TaskAction) -> Unit,
    onDelete: () -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onUpdateSubtask: (String, String) -> Unit,
    onCalendarEventCreated: (Long) -> Unit,
    finishActivity: () -> Unit
) {
    var isStarred by remember(deadline.uid) { mutableStateOf(deadline.isStarred) }
    var showPlanComposer by rememberSaveable(deadline.uid) { mutableStateOf(false) }
    var openComposerSignal by rememberSaveable(deadline.uid) { mutableIntStateOf(0) }
    var draftSubTask by rememberSaveable(deadline.uid) { mutableStateOf("") }
    var editingSubTaskId by rememberSaveable(deadline.uid) { mutableStateOf<String?>(null) }
    var editingSubTaskDraft by rememberSaveable(deadline.uid) { mutableStateOf("") }
    var isMutatingPlan by remember(deadline.uid) { mutableStateOf(false) }
    var showGiveUpDialog by rememberSaveable(deadline.uid) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(deadline.uid) { mutableStateOf(false) }
    var showCategoryPicker by rememberSaveable(deadline.uid) { mutableStateOf(false) }
    val composerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(deadline.isStarred) {
        isStarred = deadline.isStarred
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(showPlanComposer, openComposerSignal) {
        if (!showPlanComposer) return@LaunchedEffect
        scrollState.animateScrollTo(scrollState.maxValue)
        delay(80)
        composerFocusRequester.requestFocus()
    }

    fun submitPlanComposer() {
        val content = draftSubTask.trim()
        if (content.isEmpty()) {
            draftSubTask = ""
            return
        }

        onAddSubtask(content)

        draftSubTask = ""
        showPlanComposer = false
    }

    fun executeAction(action: DetailAction) {
        when (action) {
            DetailAction.MARK_COMPLETE -> {
                onApplyTaskAction(TaskAction.MARK_COMPLETE)
                Toast.makeText(activityContext, R.string.toast_finished, Toast.LENGTH_SHORT).show()
                finishActivity()
            }

            DetailAction.GIVE_UP -> {
                showGiveUpDialog = true
            }

            DetailAction.RESTORE_ACTIVE -> {
                onApplyTaskAction(TaskAction.RESTORE_ACTIVE)
                Toast.makeText(activityContext, R.string.toast_restored_active, Toast.LENGTH_SHORT).show()
                finishActivity()
            }

            DetailAction.ARCHIVE -> {
                onApplyTaskAction(TaskAction.MARK_ARCHIVE)
                Toast.makeText(
                    activityContext,
                    activityContext.getString(R.string.toast_archived, 1),
                    Toast.LENGTH_SHORT
                ).show()
                finishActivity()
            }

            DetailAction.UNARCHIVE -> {
                onApplyTaskAction(TaskAction.UNARCHIVE)
                Toast.makeText(activityContext, R.string.toast_unarchived, Toast.LENGTH_SHORT).show()
                finishActivity()
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
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = deadline.title,
                navigationIcon = {
                    IconButton(onClick = onClose) {
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
                useParentMaterialContainer = true,
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
            val actions = availableActions(deadline.state)
            DetailBottomActions(
                actions = actions,
                onActionClick = ::executeAction
            )
        },
        floatingActionButton = {
            DetailPlanFloatingActionButton(
                onClick = {
                    showPlanComposer = true
                    openComposerSignal++
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .syncTopBarMaterialScroll(scrollState)
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressCard(deadline = deadline)
            TaskMetaCard(deadline = deadline)
            CategoryAssignmentField(
                categoryUid = deadline.categoryUid,
                categories = categories,
                onClick = { showCategoryPicker = true },
            )
            NoteCard(note = deadline.note)
            PlanCard(
                subTasks = sortedSubTasks,
                showComposer = showPlanComposer,
                composerText = draftSubTask,
                isMutating = isMutatingPlan,
                composerFocusRequester = composerFocusRequester,
                onComposerTextChange = { draftSubTask = it },
                onComposerConfirm = {
                    submitPlanComposer()
                },
                onComposerCancel = {
                    draftSubTask = ""
                    showPlanComposer = false
                },
                onToggleSubTask = { subTask ->
                    onToggleSubtask(subTask.uid)
                },
                onDeleteSubTask = { subTask ->
                    onDeleteSubtask(subTask.uid)
                },
                editingSubTaskId = editingSubTaskId,
                editingSubTaskDraft = editingSubTaskDraft,
                onStartEditSubTask = { subTask ->
                    editingSubTaskId = subTask.uid
                    editingSubTaskDraft = subTask.content
                },
                onEditingDraftChange = { editingSubTaskDraft = it },
                onSubmitEditSubTask = { subTask ->
                    val updatedContent = editingSubTaskDraft.trim()
                    if (updatedContent.isNotEmpty() && updatedContent != subTask.content) {
                        onUpdateSubtask(subTask.uid, updatedContent)
                    }
                    editingSubTaskId = null
                    editingSubTaskDraft = ""
                },
                onCancelEditSubTask = {
                    editingSubTaskId = null
                    editingSubTaskDraft = ""
                }
            )
            Spacer(modifier = Modifier.height(88.dp))
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
                        Toast.makeText(activityContext, R.string.toast_give_up, Toast.LENGTH_SHORT)
                            .show()
                        finishActivity()
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
                        Toast.makeText(activityContext, R.string.toast_deletion, Toast.LENGTH_SHORT)
                            .show()
                        finishActivity()
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

    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = categories,
            selectedUid = deadline.categoryUid,
            onSelected = onSetCategory,
            onManageCategories = onManageCategories,
            onDismiss = { showCategoryPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailBottomActions(
    actions: List<DetailAction>,
    onActionClick: (DetailAction) -> Unit,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val backdrop = LocalAdvancedMaterialBackdrop.current
    val surfaceTint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = advancedMaterial.navigationTintAlpha * 0.55f)
    val materialColors = advancedMaterial.rememberBlurColors(listOf(BlendColorEntry(surfaceTint)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (advancedMaterial.enabled && backdrop != null) {
                    Modifier.advancedTextureBlur(
                        advancedMaterial = advancedMaterial,
                        backdrop = backdrop,
                        shape = RectangleShape,
                        colors = materialColors,
                    )
                } else {
                    Modifier.background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RectangleShape
                    )
                }
            )
    ) {
        FlexibleBottomAppBar(
            modifier = Modifier
                .fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            content = {
                actions.forEach { action ->
                    IconButton(onClick = { onActionClick(action) }) {
                        Icon(
                            painter = iconResource(action.iconRes),
                            contentDescription = stringResource(action.labelRes)
                        )
                    }
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
private fun TaskMetaCard(deadline: Task) {
    val startTime = deadline.startAt?.let(GlobalUtils::safeParseDateTime) ?: LocalDateTime.now()
    val endTime = deadline.dueAt?.let(GlobalUtils::safeParseDateTime) ?: startTime
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())

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
                text = stringResource(R.string.detail_task_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.label_start_time, startTime.format(formatter)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.label_end_time, endTime.format(formatter)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StateBadge(deadline.state)
            }
        }
    }
}

@Composable
private fun StateBadge(state: TaskState) {
    val (label, bg, fg) = when (state) {
        TaskState.ACTIVE -> Triple(
            stringResource(R.string.state_active),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )

        TaskState.COMPLETED -> Triple(
            stringResource(R.string.state_completed),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

        TaskState.ARCHIVED -> Triple(
            stringResource(R.string.state_archived),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        TaskState.ABANDONED -> Triple(
            stringResource(R.string.state_abandoned),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )

        TaskState.ABANDONED_ARCHIVED -> Triple(
            stringResource(R.string.state_abandoned_archived),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
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

@Composable
private fun PlanCard(
    subTasks: List<TaskSubtask>,
    showComposer: Boolean,
    composerText: String,
    isMutating: Boolean,
    composerFocusRequester: FocusRequester,
    onComposerTextChange: (String) -> Unit,
    onComposerConfirm: () -> Unit,
    onComposerCancel: () -> Unit,
    onToggleSubTask: (TaskSubtask) -> Unit,
    onDeleteSubTask: (TaskSubtask) -> Unit,
    editingSubTaskId: String?,
    editingSubTaskDraft: String,
    onStartEditSubTask: (TaskSubtask) -> Unit,
    onEditingDraftChange: (String) -> Unit,
    onSubmitEditSubTask: (TaskSubtask) -> Unit,
    onCancelEditSubTask: () -> Unit
) {
    val completedCount = subTasks.count { it.isCompleted }
    val rowSpacing = 3.dp

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.detail_plan_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.detail_plan_progress,
                        completedCount,
                        subTasks.size
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showComposer) {
                PlanComposerRow(
                    value = composerText,
                    focusRequester = composerFocusRequester,
                    enabled = !isMutating,
                    onValueChange = onComposerTextChange,
                    onConfirm = onComposerConfirm,
                    onCancel = onComposerCancel
                )
                if (subTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(rowSpacing))
                }
            }

            if (subTasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.detail_plan_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                subTasks.forEachIndexed { index, subTask ->
                    SubTaskRow(
                        item = subTask,
                        enabled = !isMutating,
                        isEditing = editingSubTaskId == subTask.uid,
                        editingText = editingSubTaskDraft,
                        onEditingTextChange = onEditingDraftChange,
                        onToggle = { onToggleSubTask(subTask) },
                        onDelete = { onDeleteSubTask(subTask) },
                        onLongPressEdit = { onStartEditSubTask(subTask) },
                        onSubmitEdit = { onSubmitEditSubTask(subTask) },
                        onCancelEdit = onCancelEditSubTask
                    )
                    if (index != subTasks.lastIndex) {
                        Spacer(modifier = Modifier.height(rowSpacing))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanComposerRow(
    value: String,
    focusRequester: FocusRequester,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = false,
                onClick = null,
                enabled = false,
                modifier = Modifier.alpha(0.55f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.merge(
                    TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = stringResource(R.string.detail_plan_add_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
            FilledIconButton(
                onClick = onConfirm,
                enabled = enabled,
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
            ) {
                Icon(
                    painter = iconResource(R.drawable.msr_check_wght400),
                    contentDescription = stringResource(R.string.save)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            FilledIconButton(
                onClick = onCancel,
                enabled = enabled,
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
            ) {
                Icon(
                    painter = iconResource(R.drawable.msr_close_wght400),
                    contentDescription = stringResource(R.string.cancel)
                )
            }
        }
    }
}

@Composable
private fun SubTaskRow(
    item: TaskSubtask,
    enabled: Boolean,
    isEditing: Boolean,
    editingText: String,
    onEditingTextChange: (String) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onLongPressEdit: () -> Unit,
    onSubmitEdit: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = enabled && !isEditing,
                    onClick = onToggle,
                    onLongClick = onLongPressEdit
                )
                .padding(end = 12.dp, start = if (GlobalUtils.miuixMode || isEditing) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = item.isCompleted,
                onClick = if (enabled && !isEditing) onToggle else null,
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(10.dp))
            if (isEditing) {
                BasicTextField(
                    value = editingText,
                    onValueChange = onEditingTextChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.merge(
                        TextStyle(color = MaterialTheme.colorScheme.onSurface)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmitEdit() }),
                    decorationBox = { inner ->
                        if (editingText.isBlank()) {
                            Text(
                                text = stringResource(R.string.detail_plan_add_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                )
                FilledIconButton(
                    onClick = onSubmitEdit,
                    enabled = enabled,
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                ) {
                    Icon(
                        painter = iconResource(R.drawable.msr_check_wght400),
                        contentDescription = stringResource(R.string.save)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                FilledIconButton(
                    onClick = onCancelEdit,
                    enabled = enabled,
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                ) {
                    Icon(
                        painter = iconResource(R.drawable.msr_close_wght400),
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            } else {
                Text(
                    text = item.content,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                FilledIconButton(
                    onClick = onDelete,
                    enabled = enabled,
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                ) {
                    Icon(
                        painter = iconResource(R.drawable.msr_delete_wght400),
                        contentDescription = stringResource(R.string.delete)
                    )
                }
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
