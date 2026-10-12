package com.aritxonly.deadliner

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import com.aritxonly.deadliner.logging.AppLog
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.aritxonly.deadliner.ui.base.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aritxonly.deadliner.ai.GeneratedDDL
import com.aritxonly.deadliner.calendar.CalendarHelper
import com.aritxonly.deadliner.localutils.DynamicColorsExtension
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.GlobalUtils.toDateTimeString
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.CalendarEvent
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.Checkbox
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.base.RadioButton
import com.aritxonly.deadliner.ui.base.TabRow
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.syncTopBarMaterialScroll
import com.aritxonly.deadliner.ui.editor.DeadlineAiQuickAddCard
import com.aritxonly.deadliner.ui.editor.DeadlineBottomActions
import com.aritxonly.deadliner.ui.editor.DeadlineNameField
import com.aritxonly.deadliner.ui.editor.DeadlineStarToggleCard
import com.aritxonly.deadliner.ui.editor.HabitEditorDraft
import com.aritxonly.deadliner.ui.editor.HabitEditorSection
import com.aritxonly.deadliner.ui.editor.TaskEditorDraft
import com.aritxonly.deadliner.ui.editor.TaskEditorSection
import com.aritxonly.deadliner.ui.category.CategoryAssignmentField
import com.aritxonly.deadliner.ui.category.CategoryPickerSheet
import com.aritxonly.deadliner.ui.category.rememberActiveCategories
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.deadliner.domain.habit.HabitGoalType
import com.deadliner.domain.habit.HabitPeriod
import com.deadliner.domain.habit.HabitReminder
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.aritxonly.deadliner.ai.AIUtils

@SuppressLint("SimpleDateFormat")
class AddDDLActivity : DeadlinerAppCompatActivity() {

    private var showQuickAdd by mutableStateOf(false)
    private var selectedPage by mutableIntStateOf(0)
    private var editingTaskUid: String? = null
    private var editingHabitUid: String? = null

    private var taskDraft by mutableStateOf(TaskEditorDraft())
    private var habitDraft by mutableStateOf(HabitEditorDraft())
    private var aiInputText by mutableStateOf("")
    private var isAiLoading by mutableStateOf(false)
    private var autoRunAiOnAppear by mutableStateOf(false)
    private var aiAutoTriggered by mutableStateOf(false)
    private var showDonatePrompt by mutableStateOf(false)
    private var showCalendarEmptyDialog by mutableStateOf(false)
    private var showCalendarImportDialog by mutableStateOf(false)
    private var calendarImportEvents by mutableStateOf<List<CalendarEvent>>(emptyList())
    private var calendarImportSearchQuery by mutableStateOf("")
    private var selectedCalendarEventId by mutableStateOf<Long?>(null)
    private var showCalendarAccountFilterDialog by mutableStateOf(false)
    private var showCategoryPicker by mutableStateOf(false)
    private var calendarAccountOptions by mutableStateOf<List<String>>(emptyList())
    private var selectedCalendarAccounts by mutableStateOf<Set<String>>(emptySet())

    private var calendarEventId: Long? = null
    private var pendingCalendarAction: PendingCalendarAction? = null
    private var pendingSaveAction: SaveAction? = null

    private enum class PendingCalendarAction {
        IMPORT,
        SAVE_TO_CALENDAR
    }

    private enum class SaveAction {
        SAVE,
        SAVE_TO_CALENDAR
    }

    private val calendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val action = pendingCalendarAction
            pendingCalendarAction = null

            when (action) {
                PendingCalendarAction.IMPORT -> {
                    if (hasCalendarReadPermission()) {
                        loadCalendarEventsAndShowDialog()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.permission_calendar_error, "Missing calendar permission"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                PendingCalendarAction.SAVE_TO_CALENDAR -> {
                    if (hasCalendarWritePermission()) {
                        performPendingSaveIfNeeded()
                    } else {
                        pendingSaveAction = null
                        Toast.makeText(
                            this,
                            getString(R.string.permission_calendar_error, "Missing calendar permission"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                null -> Unit
            }
        }

    companion object {
        const val EXTRA_PREFILL_TEXT = "EXTRA_PREFILL_TEXT"
        const val EXTRA_AUTO_RUN_AI = "EXTRA_AUTO_RUN_AI"
        private const val EXTRA_EDIT_HABIT_UID = "EXTRA_EDIT_HABIT_UID"
        private const val EXTRA_EDIT_TASK_UID = "EXTRA_EDIT_TASK_UID"
        private val REMINDER_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val AI_DUE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        fun newEditHabitIntent(context: android.content.Context, habitUid: String): Intent =
            Intent(context, AddDDLActivity::class.java).putExtra(EXTRA_EDIT_HABIT_UID, habitUid)

        fun newEditIntent(context: android.content.Context, taskUid: String): Intent =
            Intent(context, AddDDLActivity::class.java).putExtra(EXTRA_EDIT_TASK_UID, taskUid)
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        AppLog.d("AddDDLActivity", "available: ${com.google.android.material.color.DynamicColors.isDynamicColorAvailable()}")

        enableEdgeToEdgeForAllDevices()
        super.onCreate(savedInstanceState)

        DynamicColorsExtension.applyApp(this.application, GlobalUtils.seedColor)
        DynamicColorsExtension.apply(this, GlobalUtils.seedColor)
        GlobalUtils.decideHideFromRecent(this, this)

        editingHabitUid = intent.getStringExtra(EXTRA_EDIT_HABIT_UID)
        editingTaskUid = intent.getStringExtra(EXTRA_EDIT_TASK_UID)
        selectedPage = if (editingTaskUid == null && editingHabitUid == null) {
            intent.getIntExtra("EXTRA_CURRENT_TYPE", 0).coerceIn(0, 1)
        } else {
            0
        }

        if (editingHabitUid != null) selectedPage = 1

        val generatedDDL = intent.getParcelableExtra<GeneratedDDL>("EXTRA_GENERATE_DDL")
        val prefillText = intent.getStringExtra(EXTRA_PREFILL_TEXT)
        val autoRunAi = intent.getBooleanExtra(EXTRA_AUTO_RUN_AI, false)

        showQuickAdd = savedInstanceState?.getBoolean("quick_add_show") ?: (GlobalUtils.quickAddEnabled &&
            editingTaskUid == null && editingHabitUid == null && generatedDDL == null && prefillText.isNullOrBlank())
        applyInitialData(generatedDDL, prefillText, autoRunAi)
        editingTaskUid?.let(::loadTaskForEditing)
        editingHabitUid?.let { uid ->
            val habit = KmpV6Runtime.database.habits.find(uid)
            if (habit == null) { finish(); return }
            habitDraft = com.aritxonly.deadliner.ui.editor.habitDraftForEditing(habit)
        }
        promptDeadlinerDonateIfNeeded()

        setContent {
            DeadlinerTheme {
                AddDDLScreen()
            }
        }
    }

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("quick_add_show", showQuickAdd)
    }

    private fun applyInitialData(generatedDDL: GeneratedDDL?, prefillText: String?, autoRunAi: Boolean) {
        val initial = com.aritxonly.deadliner.ui.quickadd.AddEntryInitialData.create(generatedDDL, prefillText, autoRunAi, intent)
        calendarEventId = initial.calendarEventId
        taskDraft = initial.task
        habitDraft = initial.habit
        aiInputText = initial.aiText
        autoRunAiOnAppear = initial.autoRunAI
    }

    private fun updateSharedName(name: String) {
        taskDraft = taskDraft.copy(name = name)
        habitDraft = habitDraft.copy(name = name)
    }

    private fun loadTaskForEditing(taskUid: String) {
        val task = KmpV6Runtime.database.tasks.find(taskUid)
        if (task == null) {
            Toast.makeText(this, R.string.parse_failed, Toast.LENGTH_SHORT).show()
            finishAfterTransition()
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

    private fun updateSharedStarred(isStarred: Boolean) {
        taskDraft = taskDraft.copy(isStarred = isStarred)
    }

    private fun parseReminderTime(raw: String?): LocalTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalTime.parse(raw, REMINDER_TIME_FORMATTER) }.getOrNull()
    }

    private fun buildHabitSummary(): String {
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

    private fun calendarEventDisplayText(event: CalendarEvent): String =
        com.aritxonly.deadliner.calendar.formatCalendarImportEvent(this, event)

    private fun applySelectedCalendarEvent() {
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

    private fun saveCalendarAccountFilterSelection() {
        GlobalUtils.filteredCalendars = selectedCalendarAccounts
        showCalendarAccountFilterDialog = false
        Toast.makeText(this, R.string.calendar_filter_saved, Toast.LENGTH_SHORT).show()
        loadCalendarEventsAndShowDialog()
    }

    private fun promptDeadlinerDonateIfNeeded() {
        if (!GlobalUtils.shouldShowDeadlinerDonatePrompt()) return
        GlobalUtils.markDeadlinerDonatePromptShown()
        showDonatePrompt = true
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddDDLScreen() {
        if (showQuickAdd) {
            return com.aritxonly.deadliner.ui.quickadd.QuickAddSheet(
                if (selectedPage == 0) "TASK" else "HABIT", { finishAfterTransition() },
                onExpand = { proposal, raw ->
                    selectedPage = if (proposal.kind == "HABIT") 1 else 0
                    val expanded = com.aritxonly.deadliner.ui.quickadd.QuickAddEditorEntry.initialData(intent)
                    taskDraft = expanded?.task ?: com.aritxonly.deadliner.ui.quickadd.QuickAddPersistence.taskDraft(proposal, raw)
                    habitDraft = expanded?.habit ?: com.aritxonly.deadliner.ui.quickadd.QuickAddPersistence.habitDraft(proposal, raw)
                    calendarEventId = expanded?.calendarEventId
                    aiInputText = raw
                    showQuickAdd = false
                },
                onSaved = { setResult(RESULT_OK); finishAfterTransition() })
        }
        val tabs = listOf(stringResource(R.string.task), stringResource(R.string.habit))
        val pageSurfaceColor = MaterialTheme.colorScheme.surface
        val advancedMaterial = LocalAdvancedMaterialSpec.current
        val layoutDirection = LocalLayoutDirection.current
        val categories = rememberActiveCategories()
        LaunchedEffect(autoRunAiOnAppear, aiInputText, aiAutoTriggered) {
            if (autoRunAiOnAppear && !aiAutoTriggered && aiInputText.isNotBlank()) {
                aiAutoTriggered = true
                parseAiInput()
            }
        }

        ProvideAdvancedMaterialDialogBlurHost {
            Box(modifier = Modifier.fillMaxSize()) {

                val taskEditorListState = rememberLazyListState()
                val habitEditorListState = rememberLazyListState()
                val activeEditorListState = if (selectedPage == 0) {
                    taskEditorListState
                } else {
                    habitEditorListState
                }

                AdaptiveMaterialScaffold(
                    containerColor = Color.Transparent,
                    contentColor = contentColorFor(Color.Transparent),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBarMaterialScrollKey = selectedPage,
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (advancedMaterial.enabled) Color.Transparent else pageSurfaceColor)
                        ) {
                            TopAppBar(
                                title = stringResource(if (editingTaskUid == null && editingHabitUid == null) R.string.add_task else R.string.edit),
                                mode = TopAppBarStyle.CENTER,
                                titleTextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                                navigationIcon = {
                                    IconButton(onClick = { finishAfterTransition() }) {
                                        Icon(
                                            painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                                            contentDescription = stringResource(R.string.close),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = expressiveTypeModifier
                                        )
                                    }
                                },
                                actions = {
                                    if (selectedPage == 0 && editingTaskUid == null) {
                                        TopBarIconButton(onClick = { onImportFromCalendarClick() }) {
                                            Icon(
                                                painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_event_wght400),
                                                contentDescription = stringResource(R.string.select_calendar_to_import),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = expressiveTypeModifier
                                            )
                                        }
                                    }
                                }
                            )
                            if (editingTaskUid == null) {
                                TabRow(
                                    tabs = tabs,
                                    selectedTabIndex = selectedPage,
                                    onTabSelected = { selectedPage = it },
                                    divider = { HorizontalDivider(color = Color.Transparent) },
                                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                                )
                            }
                        }
                    },
                    bottomBar = {
                        DeadlineBottomActions(
                            showSaveToCalendar = selectedPage == 0 && editingTaskUid == null,
                            onSave = { requestSave(SaveAction.SAVE) },
                            onSaveToCalendar = { requestSave(SaveAction.SAVE_TO_CALENDAR) }
                        )
                    }
                ) { padding ->
                    LazyColumn(
                        state = activeEditorListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(pageSurfaceColor)
                            .syncTopBarMaterialScroll(activeEditorListState)
                            .padding(
                                start = padding.calculateStartPadding(layoutDirection),
                                end = padding.calculateEndPadding(layoutDirection),
                            ),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            end = 16.dp,
                            bottom = 160.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (GlobalUtils.deadlinerAIEnable) {
                            item {
                                DeadlineAiQuickAddCard(
                                    value = aiInputText,
                                    onValueChange = { aiInputText = it },
                                    isLoading = isAiLoading,
                                    onSubmit = { parseAiInput() }
                                )
                            }
                        }

                        item {
                            DeadlineNameField(
                                value = taskDraft.name,
                                onValueChange = { updateSharedName(it) }
                            )
                        }

                        item {
                            CategoryAssignmentField(
                                categoryUid = if (selectedPage == 0) taskDraft.categoryUid else habitDraft.categoryUid,
                                categories = categories,
                                onClick = { showCategoryPicker = true },
                            )
                        }

                        if (selectedPage == 0) {
                            item {
                                DeadlineStarToggleCard(
                                    checked = taskDraft.isStarred,
                                    onCheckedChange = { updateSharedStarred(it) }
                                )
                            }

                            item {
                                TaskEditorSection(
                                    draft = taskDraft,
                                    onDraftChange = { taskDraft = it },
                                    formatDateTime = ::formatLocalDateTime,
                                    onPickStartTime = {
                                        launchPicker {
                                            GlobalUtils.showDateTimePicker(
                                                supportFragmentManager,
                                            ) { selected ->
                                                taskDraft = taskDraft.copy(
                                                    startTime = selected,
                                                    endTime = if (taskDraft.endTime.isBefore(selected)) {
                                                        selected.plusHours(1)
                                                    } else {
                                                        taskDraft.endTime
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    onPickEndTime = {
                                        launchPicker {
                                            GlobalUtils.showDateTimePicker(
                                                supportFragmentManager,
                                                taskDraft.startTime,
                                                { chosen ->
                                                    Toast.makeText(
                                                        this@AddDDLActivity,
                                                        getString(R.string.please_choose_the_time_after, chosen),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                            ) { selected ->
                                                taskDraft = taskDraft.copy(endTime = selected)
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            item {
                                HabitEditorSection(
                                    draft = habitDraft,
                                    onDraftChange = { habitDraft = it },
                                    summaryText = buildHabitSummary(),
                                    reminderTimeLabel = habitDraft.reminderTime.format(REMINDER_TIME_FORMATTER),
                                    onPickReminderTime = {
                                        launchPicker {
                                            GlobalUtils.showTimePicker(
                                                supportFragmentManager,
                                                initialTime = habitDraft.reminderTime,
                                            ) { selected ->
                                                habitDraft = habitDraft.copy(
                                                    reminderEnabled = true,
                                                    reminderTime = selected.withSecond(0).withNano(0)
                                                )
                                            }
                                        }
                                    },
                                    onClearReminder = {
                                        habitDraft = habitDraft.copy(reminderEnabled = false)
                                    }
                                )
                            }
                        }
                    }
                }

                if (showCategoryPicker) {
                    CategoryPickerSheet(
                        categories = categories,
                        selectedUid = if (selectedPage == 0) taskDraft.categoryUid else habitDraft.categoryUid,
                        onSelected = { uid ->
                            if (selectedPage == 0) taskDraft = taskDraft.copy(categoryUid = uid)
                            else habitDraft = habitDraft.copy(categoryUid = uid)
                        },
                        onManageCategories = {
                            startActivity(CategoryManagementActivity.newIntent(this@AddDDLActivity))
                        },
                        onDismiss = { showCategoryPicker = false },
                    )
                }

                if (showDonatePrompt) {
                    AlertDialog(
                        show = true,
                        onDismissRequest = { showDonatePrompt = false },
                        title = { Text(stringResource(R.string.deadliner_donate_plan_title)) },
                        text = { Text(stringResource(R.string.deadliner_donate_plan_message)) },
                        miuixTitle = getString(R.string.deadliner_donate_plan_title),
                        miuixSummary = getString(R.string.deadliner_donate_plan_message),
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDonatePrompt = false
                                    startActivity(
                                        Intent(this@AddDDLActivity, SettingsActivity::class.java).apply {
                                            putExtra(SettingsActivity.EXTRA_INITIAL_ROUTE, SettingsRoute.Donate.route)
                                        }
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.deadliner_donate_go))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDonatePrompt = false }) {
                                Text(stringResource(R.string.later))
                            }
                        }
                    )
                }

                if (showCalendarEmptyDialog) {
                    AlertDialog(
                        show = true,
                        onDismissRequest = { showCalendarEmptyDialog = false },
                        title = { Text(stringResource(R.string.select_calendar_to_import)) },
                        text = { Text(stringResource(R.string.no_task_add_calendar)) },
                        miuixTitle = getString(R.string.select_calendar_to_import),
                        miuixSummary = getString(R.string.no_task_add_calendar),
                        confirmButton = {
                            TextButton(onClick = {
                                showCalendarEmptyDialog = false
                                showCalendarFilterDialog()
                            }) {
                                Text(stringResource(R.string.filter_calendar_account))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCalendarEmptyDialog = false }) {
                                Text(stringResource(R.string.close))
                            }
                        }
                    )
                }

                if (showCalendarImportDialog) {
                    com.aritxonly.deadliner.calendar.CalendarImportDialog(
                        events = calendarImportEvents, query = calendarImportSearchQuery,
                        selectedId = selectedCalendarEventId, eventLabel = ::calendarEventDisplayText,
                        onQueryChange = { calendarImportSearchQuery = it },
                        onSelect = { selectedCalendarEventId = it }, onFilter = ::showCalendarFilterDialog,
                        onConfirm = ::applySelectedCalendarEvent,
                        onDismiss = { showCalendarImportDialog = false },
                    )
                }

                if (showCalendarAccountFilterDialog) {
                    com.aritxonly.deadliner.calendar.CalendarAccountFilterDialog(
                        accounts = calendarAccountOptions, selected = selectedCalendarAccounts,
                        onSelectionChange = { selectedCalendarAccounts = it },
                        onConfirm = ::saveCalendarAccountFilterSelection,
                        onDismiss = { showCalendarAccountFilterDialog = false },
                    )
                }
            }
        }
    }

    private fun parseAiInput() {
        val text = aiInputText.trim()
        if (text.isEmpty() || isAiLoading) return

        isAiLoading = true
        lifecycleScope.launch {
            try {
                val json = AIUtils.generateMixed(this@AddDDLActivity, text)
                val mixed = AIUtils.parseMixedResult(json)
                val firstTask = mixed.tasks.firstOrNull()

                when (selectedPage) {
                    0 -> {
                        if (firstTask != null) {
                            updateSharedName(firstTask.name)

                            runCatching {
                                LocalDateTime.parse(firstTask.dueTime, AI_DUE_TIME_FORMATTER)
                            }.onSuccess { parsedDue ->
                                taskDraft = taskDraft.copy(
                                    endTime = parsedDue,
                                    startTime = if (taskDraft.startTime.isAfter(parsedDue)) {
                                        parsedDue.minusHours(1)
                                    } else {
                                        taskDraft.startTime
                                    }
                                )
                            }

                            taskDraft = taskDraft.copy(note = firstTask.note.orEmpty())
                        } else {
                            if (taskDraft.note.isBlank()) {
                                taskDraft = taskDraft.copy(note = text)
                            }
                        }
                    }

                    1 -> {
                        val suggestedName = firstTask?.name?.trim().orEmpty()
                        if (suggestedName.isNotEmpty()) {
                            updateSharedName(suggestedName)
                        } else if (habitDraft.name.isBlank()) {
                            updateSharedName(text.take(20))
                        }
                    }
                }
            } catch (e: Exception) {
                AppLog.e("AddDDLActivity", "AI parse failed", e)
                Toast.makeText(
                    this@AddDDLActivity,
                    getString(R.string.ai_parse_failed),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isAiLoading = false
                autoRunAiOnAppear = false
            }
        }
    }

    private fun requestSave(action: SaveAction) {
        pendingSaveAction = action
        performPendingSaveIfNeeded()
    }

    private fun performPendingSaveIfNeeded() {
        val action = pendingSaveAction ?: return
        performSave(action)
    }

    private fun performSave(action: SaveAction) {
        val toCalendar = action == SaveAction.SAVE_TO_CALENDAR
        val name = if (selectedPage == 0) taskDraft.name.trim() else habitDraft.name.trim()
        if (name.isBlank()) {
            pendingSaveAction = null
            Toast.makeText(this, getString(R.string.add_ddl_name), Toast.LENGTH_SHORT).show()
            return
        }

        if (toCalendar && !hasCalendarWritePermission()) {
            pendingCalendarAction = PendingCalendarAction.SAVE_TO_CALENDAR
            calendarPermissionLauncher.launch(
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

    private fun saveTask(name: String, toCalendar: Boolean) {
        val editor = KmpV6Runtime.createTaskEditorViewModel(editingTaskUid)
        editor.updateTitle(name)
        editor.updateNote(taskDraft.note)
        editor.updateStartAt(taskDraft.startTime.toString())
        editor.updateDueAt(taskDraft.endTime.toString())
        editor.updateIsStarred(taskDraft.isStarred)
        editor.updateCategory(taskDraft.categoryUid)
        editor.updateCalendarEventId(calendarEventId)
        editor.save()
        lifecycleScope.launch {
            editor.uiState.collect { state ->
                state.savedTask?.let { savedTask ->
                    if (toCalendar) {
                        try {
                            val eventId = CalendarHelper(this@AddDDLActivity).insertEvent(savedTask)
                            KmpV6Runtime.taskListViewModel.saveTaskNow(
                                savedTask.copy(
                                    calendarEventId = eventId,
                                    updatedAt = java.time.Instant.now().toString(),
                                )
                            )
                            Toast.makeText(
                                this@AddDDLActivity,
                                getString(R.string.add_calendar_success),
                                Toast.LENGTH_SHORT,
                            ).show()
                        } catch (error: Exception) {
                            AppLog.e("Calendar", "Could not create calendar event", error)
                            Toast.makeText(
                                this@AddDDLActivity,
                                getString(R.string.add_calendar_failed, error.toString()),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    } else {
                        KmpV6Runtime.taskListViewModel.loadNow()
                    }
                    setResult(RESULT_OK)
                    finishAfterTransition()
                    return@collect
                }
                if (!state.isSaving && state.error != null) {
                    Toast.makeText(this@AddDDLActivity, state.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveHabit(name: String) {
        val isEbbinghaus = habitDraft.period == HabitPeriod.EBBINGHAUS
        val goalType = if (isEbbinghaus) HabitGoalType.PER_PERIOD else habitDraft.goalType
        val frequency = if (isEbbinghaus) {
            1
        } else {
            habitDraft.timesPerPeriod.toIntOrNull()?.coerceAtLeast(1) ?: 1
        }
        val total = if (goalType == HabitGoalType.TOTAL) {
            habitDraft.totalTarget.toIntOrNull()
        } else {
            null
        }
        val editor = KmpV6Runtime.createHabitEditorViewModel(editingHabitUid)
        editor.updateName(name)
        val existing = editingHabitUid?.let(KmpV6Runtime.database.habits::find)
        if (existing == null || existing.period != habitDraft.period) editor.updatePeriod(habitDraft.period)
        if (!isEbbinghaus && (existing == null || existing.schedule.weekdaysMask != habitDraft.weekdaysMask)) {
            editor.updateWeekdaysMask(habitDraft.weekdaysMask)
        }
        editor.updateTimesPerPeriod(if (goalType == HabitGoalType.TOTAL) 1 else frequency)
        editor.updateGoalType(goalType)
        editor.updateTotalTarget(if (goalType == HabitGoalType.TOTAL) total else null)
        editor.updateReminder(
            if (habitDraft.reminderEnabled) {
                HabitReminder(habitDraft.reminderTime.format(REMINDER_TIME_FORMATTER))
            } else null
        )
        editor.updateCategory(habitDraft.categoryUid)
        editor.save()
        lifecycleScope.launch {
            editor.uiState.collect { state ->
                if (state.savedHabit != null) {
                    KmpV6Runtime.habitListViewModel.loadNow()
                    setResult(RESULT_OK)
                    finishAfterTransition()
                    return@collect
                }
                if (!state.isSaving && state.error != null) {
                    Toast.makeText(this@AddDDLActivity, state.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onImportFromCalendarClick() {
        if (!hasCalendarReadPermission()) {
            pendingCalendarAction = PendingCalendarAction.IMPORT
            calendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
            return
        }
        loadCalendarEventsAndShowDialog()
    }

    private fun hasCalendarReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCalendarWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadCalendarEventsAndShowDialog() {
        if (!hasCalendarReadPermission()) {
            Toast.makeText(
                this,
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
                this,
                getString(R.string.permission_calendar_error, e.message ?: "SecurityException"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (calendarEvents.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_task_add_calendar), Toast.LENGTH_SHORT).show()
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

    private fun showCalendarFilterDialog() {
        if (!hasCalendarReadPermission()) {
            Toast.makeText(
                this,
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
                this,
                getString(R.string.permission_calendar_error, e.message ?: "SecurityException"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (accounts.isEmpty()) {
            Toast.makeText(this, R.string.no_valid_calendar_account, Toast.LENGTH_SHORT).show()
            return
        }

        calendarAccountOptions = accounts.map { it.accountName }.filter { it.isNotBlank() }
        selectedCalendarAccounts = GlobalUtils.filteredCalendars?.filterNotNull()?.toSet() ?: emptySet()
        showCalendarAccountFilterDialog = true
    }

    private fun formatLocalDateTime(dateTime: LocalDateTime): String =
        com.aritxonly.deadliner.ui.editor.formatEditorDateTime(dateTime)

    private fun launchPicker(showPicker: () -> Unit) = showPicker()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdgeForAllDevices()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        enableEdgeToEdgeForAllDevices()
    }
}
