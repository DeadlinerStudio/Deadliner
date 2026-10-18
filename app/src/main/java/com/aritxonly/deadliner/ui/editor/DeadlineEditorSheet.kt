package com.aritxonly.deadliner.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.base.SheetHeight
import com.aritxonly.deadliner.ui.quickadd.QuickAddEditorEntry
import com.aritxonly.deadliner.ui.base.TabRow
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.category.CategoryDropdownField
import com.aritxonly.deadliner.ui.category.rememberActiveCategories
import com.aritxonly.deadliner.ui.iconResource

@Composable
internal fun DeadlineEditorSheet(
    request: android.content.Intent,
    show: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val saved by rememberUpdatedState(onSaved)
    val state = rememberSaveable(saver = Saver<DeadlineEditorState, String>(
        save = { it.saveSnapshot() },
        restore = { raw -> DeadlineEditorState(context, scope, request, { saved() }).apply { restoreSnapshot(raw) } },
    )) { DeadlineEditorState(context, scope, request, { saved() }) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        state.onCalendarPermissionResult()
    }
    SideEffect { state.requestCalendarPermissions = { permissionLauncher.launch(it) } }
    LaunchedEffect(state.isMissingEntity) { if (state.isMissingEntity) onDismissFinished() }
    val useQuickAdd = remember {
        GlobalUtils.quickAddEnabled || GlobalUtils.deadlinerAIEnable ||
            state.aiInputText.isNotBlank() || state.autoRunAiOnAppear
    }
    val quickAdd = if (useQuickAdd) state.rememberEditorQuickAdd() else null
    DisposableEffect(state) { onDispose { state.datePicker.dismiss() } }
    val editing = state.editingTaskUid != null || state.editingHabitUid != null
    MiuixBottomSheet(
        show = show && !state.isMissingEntity,
        sheetHeight = SheetHeight.FULL,
        contentFadeHeight = 12.dp,
        enterFrom = remember(request) { QuickAddEditorEntry.sourceBounds(request) },
        title = stringResource(if (editing) R.string.edit else R.string.add_task),
        allowDismiss = state.canDismiss,
        onDismissRequest = { if (state.canDismiss) onDismiss() },
        onDismissFinished = onDismissFinished,
        startAction = {
            TopBarIconButton(onClick = onDismiss, enabled = state.canDismiss) {
                Icon(iconResource(R.drawable.msr_close_wght400), stringResource(R.string.close))
            }
        },
        endAction = if (editing) ({
            TopBarIconButton(
                onClick = { state.requestSave(DeadlineEditorState.SaveAction.SAVE) },
                enabled = !state.busy,
            ) {
                Icon(iconResource(R.drawable.msr_check_wght400), stringResource(R.string.save))
            }
        }) else if (state.selectedPage == 0) ({
            TopBarIconButton(onClick = state::onImportFromCalendarClick, enabled = !state.busy) {
                Icon(com.aritxonly.deadliner.ui.calendarImportPainter(), stringResource(R.string.select_calendar_to_import))
            }
        }) else null,
        sampleContentForBottomBar = !editing,
        headerContent = {
            if (!editing) TabRow(
                tabs = listOf(stringResource(R.string.task), stringResource(R.string.habit)),
                selectedTabIndex = state.selectedPage,
                onTabSelected = { if (!state.busy) state.selectedPage = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        },
        bottomBar = {
            if (!editing) DeadlineBottomActions(
                showSaveToCalendar = state.selectedPage == 0,
                onSave = { state.requestSave(DeadlineEditorState.SaveAction.SAVE) },
                onSaveToCalendar = { state.requestSave(DeadlineEditorState.SaveAction.SAVE_TO_CALENDAR) },
                enabled = !state.busy,
            )
        },
    ) { padding ->
        state.Form(padding, quickAdd)
    }
    state.Dialogs()
}

@Composable
private fun DeadlineEditorState.Form(padding: PaddingValues, quickAdd: com.aritxonly.deadliner.ui.quickadd.QuickAddState?) {
    val categories = rememberActiveCategories()
    val taskList = rememberLazyListState()
    val habitList = rememberLazyListState()
    val list = if (selectedPage == 0) taskList else habitList
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().syncTopBarMaterialScroll(list),
        contentPadding = PaddingValues(start = 16.dp, top = padding.calculateTopPadding(),
            end = 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (quickAdd != null) item {
            EditorQuickAddCard(quickAdd)
        }
        item { DeadlineNameField(if (selectedPage == 0) taskDraft.name else habitDraft.name, ::updateSharedName) }
        item {
            CategoryDropdownField(
                categoryUid = if (selectedPage == 0) taskDraft.categoryUid else habitDraft.categoryUid,
                categories = categories,
                onSelected = { uid ->
                    if (selectedPage == 0) taskDraft = taskDraft.copy(categoryUid = uid)
                    else habitDraft = habitDraft.copy(categoryUid = uid)
                },
                onManageCategories = {
                    startActivity(com.aritxonly.deadliner.CategoryManagementActivity.newIntent(context))
                },
            )
        }
        if (selectedPage == 0) {
            item { DeadlineStarToggleCard(taskDraft.isStarred, ::updateSharedStarred) }
            item { TaskEditorSection(taskDraft, { taskDraft = it }, ::formatLocalDateTime, { pickStartTime() }, { pickEndTime() }) }
        } else item {
            HabitEditorSection(draft = habitDraft, onDraftChange = { habitDraft = it }, summaryText = buildHabitSummary(),
                reminderTimeLabel = habitDraft.reminderTime.format(DeadlineEditorState.REMINDER_TIME_FORMATTER),
                onPickReminderTime = { pickReminderTime() }, onClearReminder = { habitDraft = habitDraft.copy(reminderEnabled = false) })
        }
    }
}
