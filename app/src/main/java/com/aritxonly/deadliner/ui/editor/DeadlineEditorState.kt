package com.aritxonly.deadliner.ui.editor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ai.GeneratedDDL
import com.aritxonly.deadliner.calendar.CalendarHelper
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.GlobalUtils.toDateTimeString
import com.aritxonly.deadliner.logging.AppLog
import com.aritxonly.deadliner.model.CalendarEvent
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.habit.HabitGoalType
import com.deadliner.domain.habit.HabitPeriod
import com.deadliner.domain.habit.HabitReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** One editor session; Android navigation, permissions and lifetime belong to the Compose host. */
internal class DeadlineEditorState(
    internal val context: Context,
    internal val scope: CoroutineScope,
    internal val intent: Intent,
    internal val onSaved: () -> Unit,
) {
    internal var selectedPage by mutableIntStateOf(0)
    internal var editingTaskUid: String? = null
    internal var editingHabitUid: String? = null

    internal var taskDraft by mutableStateOf(TaskEditorDraft())
    internal var habitDraft by mutableStateOf(HabitEditorDraft())
    internal var aiInputText by mutableStateOf("")
    internal var quickAddState: com.aritxonly.deadliner.ui.quickadd.QuickAddState? by mutableStateOf(null)
    internal val isAiLoading get() = quickAddState?.busy == true
    internal var autoRunAiOnAppear by mutableStateOf(false)
    internal var aiAutoTriggered by mutableStateOf(false)
    internal var showDonatePrompt by mutableStateOf(false)
    internal var showCalendarEmptyDialog by mutableStateOf(false)
    internal var showCalendarImportDialog by mutableStateOf(false)
    internal var calendarImportEvents by mutableStateOf<List<CalendarEvent>>(emptyList())
    internal var calendarImportSearchQuery by mutableStateOf("")
    internal var selectedCalendarEventId by mutableStateOf<Long?>(null)
    internal var showCalendarAccountFilterDialog by mutableStateOf(false)
    internal var calendarAccountOptions by mutableStateOf<List<String>>(emptyList())
    internal var selectedCalendarAccounts by mutableStateOf<Set<String>>(emptySet())

    internal var calendarEventId: Long? = null
    internal var pendingCalendarAction by mutableStateOf<PendingCalendarAction?>(null)
    internal var pendingSaveAction by mutableStateOf<SaveAction?>(null)

    internal enum class PendingCalendarAction {
        IMPORT,
        SAVE_TO_CALENDAR
    }

    internal enum class SaveAction {
        SAVE,
        SAVE_TO_CALENDAR
    }

    companion object {
        internal val REMINDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    internal var isSaving by mutableStateOf(false)
    internal var isMissingEntity by mutableStateOf(false)
    internal val busy get() = isSaving || isAiLoading || pendingCalendarAction != null
    internal val canDismiss get() = !isSaving && pendingCalendarAction == null
    internal var requestCalendarPermissions: (Array<String>) -> Unit = {}
    internal val datePicker = com.aritxonly.deadliner.ui.picker.DateTimePickerController()
    internal val applicationContext get() = context.applicationContext

    init {
        editingTaskUid = intent.getStringExtra(AddDDLActivity.EXTRA_EDIT_TASK_UID)
        editingHabitUid = intent.getStringExtra(AddDDLActivity.EXTRA_EDIT_HABIT_UID)
        selectedPage = when {
            editingHabitUid != null -> 1
            editingTaskUid != null -> 0
            else -> intent.getIntExtra("EXTRA_CURRENT_TYPE", 0).coerceIn(0, 1)
        }
        applyInitialData(intent.getParcelableExtra("EXTRA_GENERATE_DDL"),
            intent.getStringExtra(AddDDLActivity.EXTRA_PREFILL_TEXT),
            intent.getBooleanExtra(AddDDLActivity.EXTRA_AUTO_RUN_AI, false))
        editingTaskUid?.let(::loadTaskForEditing)
        editingHabitUid?.let { uid ->
            val habit = KmpV6Runtime.database.habits.find(uid)
            if (habit == null) isMissingEntity = true else habitDraft = habitDraftForEditing(habit)
        }
        promptDeadlinerDonateIfNeeded()
    }

    internal fun getString(id: Int, vararg args: Any): String = context.getString(id, *args)
    internal fun startActivity(intent: Intent) = context.startActivity(intent)

    internal fun onCalendarPermissionResult() {
        val action = pendingCalendarAction
        pendingCalendarAction = null
        when (action) {
            PendingCalendarAction.IMPORT -> if (hasCalendarReadPermission()) loadCalendarEventsAndShowDialog()
                else Toast.makeText(context, getString(R.string.permission_calendar_error, "Missing calendar permission"), Toast.LENGTH_SHORT).show()
            PendingCalendarAction.SAVE_TO_CALENDAR -> if (hasCalendarWritePermission()) performPendingSaveIfNeeded()
                else { pendingSaveAction = null; Toast.makeText(context, getString(R.string.permission_calendar_error, "Missing calendar permission"), Toast.LENGTH_SHORT).show() }
            null -> Unit
        }
    }

    internal fun applyInitialData(generatedDDL: GeneratedDDL?, prefillText: String?, autoRunAi: Boolean) {
        val initial = com.aritxonly.deadliner.ui.quickadd.AddEntryInitialData.create(generatedDDL, prefillText, autoRunAi, intent)
        calendarEventId = initial.calendarEventId
        taskDraft = initial.task
        habitDraft = initial.habit
        aiInputText = initial.aiText
        autoRunAiOnAppear = initial.autoRunAI
    }

    internal fun updateSharedName(name: String) {
        taskDraft = taskDraft.copy(name = name)
        habitDraft = habitDraft.copy(name = name)
    }

    internal fun loadTaskForEditing(taskUid: String) {
        val task = KmpV6Runtime.database.tasks.find(taskUid)
        if (task == null) {
            Toast.makeText(context, R.string.parse_failed, Toast.LENGTH_SHORT).show()
            isMissingEntity = true
            return
        }
        val startTime = task.startAt?.let(GlobalUtils::parseDateTime) ?: LocalDateTime.now()
        val endTime = task.dueAt?.let(GlobalUtils::parseDateTime)
            ?.takeIf { !it.isBefore(startTime) }
            ?: startTime.plusHours(1)
        taskDraft = TaskEditorDraft(
            name = task.title,
            note = task.note,
            startTime = startTime,
            endTime = endTime,
            isStarred = task.isStarred,
            categoryUid = task.categoryUid,
        )
        calendarEventId = task.calendarEventId
    }

    internal fun updateSharedStarred(isStarred: Boolean) {
        taskDraft = taskDraft.copy(isStarred = isStarred)
    }

    internal fun buildHabitSummary(): String {
        val periodLabel = when (habitDraft.period) {
            HabitPeriod.DAILY, HabitPeriod.ONCE, HabitPeriod.EBBINGHAUS -> getString(R.string.frequency_daily)
            HabitPeriod.WEEKLY -> getString(R.string.frequency_weekly)
            HabitPeriod.MONTHLY -> getString(R.string.frequency_monthly)
        }
        val target = if (habitDraft.goalType == HabitGoalType.TOTAL) {
            habitDraft.totalTarget.toIntOrNull()?.let { getString(R.string.habit_total_count, it) }
                ?: getString(R.string.habit_total_unlimited)
        } else {
            getString(R.string.habit_total_unlimited)
        }
        return getString(
            R.string.habit_checkin,
            periodLabel,
            habitDraft.timesPerPeriod.toIntOrNull() ?: 1,
            target,
        )
    }

    internal fun calendarEventDisplayText(event: CalendarEvent): String =
        com.aritxonly.deadliner.calendar.formatCalendarImportEvent(context, event)

    internal fun applySelectedCalendarEvent() {
        val eventId = selectedCalendarEventId ?: return
        val event = calendarImportEvents.firstOrNull { it.id == eventId } ?: return
        updateSharedName(event.title.orEmpty())
        taskDraft = taskDraft.copy(note = event.description.orEmpty())
        GlobalUtils.parseDateTime(event.startMillis.toDateTimeString())?.let {
            taskDraft = taskDraft.copy(endTime = it)
        }
        calendarEventId = event.id
        showCalendarImportDialog = false
    }

    internal fun saveCalendarAccountFilterSelection() {
        GlobalUtils.filteredCalendars = selectedCalendarAccounts
        showCalendarAccountFilterDialog = false
        Toast.makeText(context, R.string.calendar_filter_saved, Toast.LENGTH_SHORT).show()
        loadCalendarEventsAndShowDialog()
    }

    internal fun promptDeadlinerDonateIfNeeded() {
        if (!GlobalUtils.shouldShowDeadlinerDonatePrompt()) return
        GlobalUtils.markDeadlinerDonatePromptShown()
        showDonatePrompt = true
    }

    internal fun requestSave(action: SaveAction) {
        if (busy) return
        pendingSaveAction = action
        performPendingSaveIfNeeded()
    }

    internal fun performPendingSaveIfNeeded() {
        val action = pendingSaveAction ?: return
        performSave(action)
    }

    internal fun performSave(action: SaveAction) {
        val toCalendar = action == SaveAction.SAVE_TO_CALENDAR
        val name = if (selectedPage == 0) taskDraft.name.trim() else habitDraft.name.trim()
        if (name.isBlank()) {
            pendingSaveAction = null
            Toast.makeText(context, getString(R.string.add_ddl_name), Toast.LENGTH_SHORT).show()
            return
        }

        if (toCalendar && !hasCalendarWritePermission()) {
            pendingCalendarAction = PendingCalendarAction.SAVE_TO_CALENDAR
            requestCalendarPermissions(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
            return
        }

        pendingSaveAction = null
        if (selectedPage == 0) {
            saveTask(name = name, toCalendar = toCalendar)
        } else {
            saveHabit(name = name)
        }
    }

    internal fun saveTask(name: String, toCalendar: Boolean) = save {
        val editor = KmpV6Runtime.createTaskEditorViewModel(editingTaskUid)
        editor.updateTitle(name)
        editor.updateNote(taskDraft.note)
        editor.updateStartAt(taskDraft.startTime.toString())
        editor.updateDueAt(taskDraft.endTime.toString())
        editor.updateIsStarred(taskDraft.isStarred)
        editor.updateCategory(taskDraft.categoryUid)
        editor.updateCalendarEventId(calendarEventId)
        editor.save()
        val result = editor.uiState.first { it.savedTask != null || (!it.isSaving && it.error != null) }
        val task = result.savedTask ?: error(result.error ?: getString(R.string.unknown_error))
        if (toCalendar) {
            try {
                val eventId = CalendarHelper(context).insertEvent(task)
                KmpV6Runtime.taskListViewModel.saveTaskNow(task.copy(
                    calendarEventId = eventId, updatedAt = java.time.Instant.now().toString(),
                ))
                Toast.makeText(context, R.string.add_calendar_success, Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                AppLog.e("Calendar", "Could not create calendar event", error)
                Toast.makeText(context, getString(R.string.add_calendar_failed, error.toString()), Toast.LENGTH_SHORT).show()
            }
        }
        KmpV6Runtime.taskListViewModel.loadNow()
    }

    internal fun saveHabit(name: String) = save {
        val isEbbinghaus = habitDraft.period == HabitPeriod.EBBINGHAUS
        val goalType = if (isEbbinghaus) HabitGoalType.PER_PERIOD else habitDraft.goalType
        val frequency = if (isEbbinghaus) 1 else habitDraft.timesPerPeriod.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val total = habitDraft.totalTarget.toIntOrNull().takeIf { goalType == HabitGoalType.TOTAL }
        val editor = KmpV6Runtime.createHabitEditorViewModel(editingHabitUid)
        editor.updateName(name)
        val existing = editingHabitUid?.let(KmpV6Runtime.database.habits::find)
        if (existing == null || existing.period != habitDraft.period) editor.updatePeriod(habitDraft.period)
        if (!isEbbinghaus && (existing == null || existing.schedule.weekdaysMask != habitDraft.weekdaysMask)) {
            editor.updateWeekdaysMask(habitDraft.weekdaysMask)
        }
        editor.updateTimesPerPeriod(if (goalType == HabitGoalType.TOTAL) 1 else frequency)
        editor.updateGoalType(goalType)
        editor.updateTotalTarget(total)
        editor.updateReminder(if (habitDraft.reminderEnabled)
            HabitReminder(habitDraft.reminderTime.format(REMINDER_TIME_FORMATTER)) else null)
        editor.updateCategory(habitDraft.categoryUid)
        editor.save()
        val result = editor.uiState.first { it.savedHabit != null || (!it.isSaving && it.error != null) }
        if (result.savedHabit == null) error(result.error ?: getString(R.string.unknown_error))
        KmpV6Runtime.habitListViewModel.loadNow()
    }

    private fun save(block: suspend () -> Unit) {
        if (isSaving) return
        isSaving = true
        scope.launch {
            try {
                block()
                onSaved()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                AppLog.e("DeadlineEditor", "Save failed", error)
                Toast.makeText(context, error.message ?: getString(R.string.unknown_error), Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
            }
        }
    }

    internal fun onImportFromCalendarClick() {
        if (!hasCalendarReadPermission()) {
            pendingCalendarAction = PendingCalendarAction.IMPORT
            requestCalendarPermissions(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
            return
        }
        loadCalendarEventsAndShowDialog()
    }

    internal fun hasCalendarReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    internal fun hasCalendarWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    internal fun loadCalendarEventsAndShowDialog() {
        if (!hasCalendarReadPermission()) {
            Toast.makeText(
                context,
                getString(R.string.permission_calendar_error, "Missing calendar permission"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val calendarHelper = CalendarHelper(applicationContext)
        val calendarEvents = try {
            calendarHelper.queryAllCalendarEvents()
        } catch (e: SecurityException) {
            AppLog.e("Calendar", "No calendar permission", e)
            Toast.makeText(
                context,
                getString(R.string.permission_calendar_error, e.message ?: "SecurityException"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (calendarEvents.isEmpty()) {
            Toast.makeText(context, getString(R.string.no_task_add_calendar), Toast.LENGTH_SHORT).show()
            showCalendarImportDialog = false
            showCalendarEmptyDialog = true
            return
        }

        calendarImportEvents = calendarEvents
        calendarImportSearchQuery = ""
        selectedCalendarEventId = null
        showCalendarEmptyDialog = false
        showCalendarImportDialog = true
    }

    internal fun showCalendarFilterDialog() {
        if (!hasCalendarReadPermission()) {
            Toast.makeText(
                context,
                getString(R.string.permission_calendar_error, "Missing calendar permission"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val helper = CalendarHelper(applicationContext)
        val accounts = try {
            helper.getAllCalendarAccounts()
        } catch (e: SecurityException) {
            AppLog.e("Calendar", "No calendar permission", e)
            Toast.makeText(
                context,
                getString(R.string.permission_calendar_error, e.message ?: "SecurityException"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (accounts.isEmpty()) {
            Toast.makeText(context, R.string.no_valid_calendar_account, Toast.LENGTH_SHORT).show()
            return
        }

        calendarAccountOptions = accounts.map { it.accountName }.filter { it.isNotBlank() }
        selectedCalendarAccounts = GlobalUtils.filteredCalendars?.filterNotNull()?.toSet() ?: emptySet()
        showCalendarAccountFilterDialog = true
    }

    internal fun formatLocalDateTime(dateTime: LocalDateTime): String =
        com.aritxonly.deadliner.ui.editor.formatEditorDateTime(dateTime)


}
