package com.aritxonly.deadliner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aritxonly.deadliner.ai.GeneratedDDL
import com.aritxonly.deadliner.calendar.CalendarHelper
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.localutils.DynamicColorsExtension
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.GlobalUtils.toDateTimeString
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.CalendarEvent
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.toJson
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.Checkbox
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.aritxonly.deadliner.ui.base.ProvideAdvancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.base.RadioButton
import com.aritxonly.deadliner.ui.base.RegisterAdvancedMaterialDialogBlur
import com.aritxonly.deadliner.ui.base.TabRow
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
import com.aritxonly.deadliner.ui.base.advancedMaterialDialogBlurHost
import com.aritxonly.deadliner.ui.editor.DeadlineAiQuickAddCard
import com.aritxonly.deadliner.ui.editor.DeadlineBottomActions
import com.aritxonly.deadliner.ui.editor.DeadlineNameField
import com.aritxonly.deadliner.ui.editor.DeadlineStarToggleCard
import com.aritxonly.deadliner.ui.editor.EditorHabitPeriod
import com.aritxonly.deadliner.ui.editor.HabitEditorDraft
import com.aritxonly.deadliner.ui.editor.HabitEditorSection
import com.aritxonly.deadliner.ui.editor.TaskEditorDraft
import com.aritxonly.deadliner.ui.editor.TaskEditorSection
import com.aritxonly.deadliner.ui.editor.toDeadlineFrequency
import com.aritxonly.deadliner.ui.editor.toEditorHabitPeriod
import com.aritxonly.deadliner.ui.editor.toModelPeriod
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.aritxonly.deadliner.ai.AIUtils

@SuppressLint("SimpleDateFormat")
class AddDDLActivity : DeadlinerAppCompatActivity() {

    private val repo = DDLRepository()
    private val habitRepo = HabitRepository()

    private var selectedPage by mutableIntStateOf(0)

    private var taskDraft by mutableStateOf(TaskEditorDraft())
    private var habitDraft by mutableStateOf(HabitEditorDraft())
    private var aiInputText by mutableStateOf("")
    private var isAiLoading by mutableStateOf(false)
    private var autoRunAiOnAppear by mutableStateOf(false)
    private var aiAutoTriggered by mutableStateOf(false)
    private var showDonatePrompt by mutableStateOf(false)
    private var showPickerDialogBlur by mutableStateOf(false)
    private var showCalendarEmptyDialog by mutableStateOf(false)
    private var showCalendarImportDialog by mutableStateOf(false)
    private var calendarImportEvents by mutableStateOf<List<CalendarEvent>>(emptyList())
    private var calendarImportSearchQuery by mutableStateOf("")
    private var selectedCalendarEventId by mutableStateOf<Long?>(null)
    private var showCalendarAccountFilterDialog by mutableStateOf(false)
    private var calendarAccountOptions by mutableStateOf<List<String>>(emptyList())
    private var selectedCalendarAccounts by mutableStateOf<Set<String>>(emptySet())
    private var pendingPickerLaunchJob: Job? = null

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
        private val REMINDER_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val AI_DUE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        Log.d("AddDDLActivity", "available: ${com.google.android.material.color.DynamicColors.isDynamicColorAvailable()}")

        enableEdgeToEdgeForAllDevices()
        super.onCreate(savedInstanceState)

        DynamicColorsExtension.applyApp(this.application, GlobalUtils.seedColor)
        DynamicColorsExtension.apply(this, GlobalUtils.seedColor)
        GlobalUtils.decideHideFromRecent(this, this)

        selectedPage = intent.getIntExtra("EXTRA_CURRENT_TYPE", 0).coerceIn(0, 1)

        val generatedDDL = intent.getParcelableExtra<GeneratedDDL>("EXTRA_GENERATE_DDL")
        val fullDDL = intent.getParcelableExtra<DDLItem>("EXTRA_FULL_DDL")
        val prefillText = intent.getStringExtra(EXTRA_PREFILL_TEXT)
        val autoRunAi = intent.getBooleanExtra(EXTRA_AUTO_RUN_AI, false)

        applyInitialData(generatedDDL, fullDDL, prefillText, autoRunAi)
        promptDeadlinerDonateIfNeeded()

        setContent {
            DeadlinerTheme {
                AddDDLScreen()
            }
        }
    }

    private fun applyInitialData(
        generatedDDL: GeneratedDDL?,
        fullDDL: DDLItem?,
        prefillText: String?,
        autoRunAi: Boolean
    ) {
        generatedDDL?.let {
            updateSharedName(it.name)
            taskDraft = taskDraft.copy(
                endTime = it.dueTime,
                note = it.note
            )
        }

        fullDDL?.let { item ->
            updateSharedName(item.name)
            taskDraft = taskDraft.copy(isStarred = item.isStared)
            calendarEventId = item.calendarEventId

            val parsedStart = GlobalUtils.parseDateTime(item.startTime)
            val parsedEnd = GlobalUtils.parseDateTime(item.endTime)

            when (item.type) {
                DeadlineType.TASK -> {
                    selectedPage = 0
                    taskDraft = taskDraft.copy(
                        note = item.note,
                        startTime = parsedStart ?: taskDraft.startTime,
                        endTime = parsedEnd ?: taskDraft.endTime
                    )
                }
                DeadlineType.HABIT -> {
                    selectedPage = 1

                    val meta = GlobalUtils.parseHabitMetaData(item.note)
                    val habit = habitRepo.getHabitByDdlId(item.id)
                    val reminderTime = parseReminderTime(habit?.alarmTime)
                    val goalType = habit?.goalType
                        ?: if (meta.frequencyType == DeadlineFrequency.TOTAL) HabitGoalType.TOTAL else HabitGoalType.PER_PERIOD
                    val timesPerPeriod = habit?.timesPerPeriod?.toString()
                        ?: meta.frequency.coerceAtLeast(1).toString()
                    val totalTarget = habit?.totalTarget
                        ?: meta.total.takeIf { it > 0 }

                    habitDraft = habitDraft.copy(
                        period = habit?.period?.let { modelPeriod ->
                            when (modelPeriod) {
                                HabitPeriod.DAILY -> EditorHabitPeriod.DAILY
                                HabitPeriod.WEEKLY -> EditorHabitPeriod.WEEKLY
                                HabitPeriod.MONTHLY -> EditorHabitPeriod.MONTHLY
                                HabitPeriod.EBBINGHAUS -> EditorHabitPeriod.EBBINGHAUS
                            }
                        } ?: meta.frequencyType.toEditorHabitPeriod(),
                        goalType = goalType,
                        timesPerPeriod = timesPerPeriod,
                        totalTarget = totalTarget?.toString().orEmpty(),
                        reminderEnabled = reminderTime != null,
                        reminderTime = reminderTime ?: habitDraft.reminderTime
                    )
                }
            }
        }

        if (fullDDL == null) {
            val safePrefill = prefillText?.trim().orEmpty()
            if (safePrefill.isNotEmpty()) {
                aiInputText = safePrefill
            }
            if (autoRunAi && safePrefill.isNotEmpty()) {
                autoRunAiOnAppear = true
            }
        }
    }

    private fun updateSharedName(name: String) {
        taskDraft = taskDraft.copy(name = name)
        habitDraft = habitDraft.copy(name = name)
    }

    private fun updateSharedStarred(isStarred: Boolean) {
        taskDraft = taskDraft.copy(isStarred = isStarred)
    }

    private fun parseReminderTime(raw: String?): LocalTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalTime.parse(raw, REMINDER_TIME_FORMATTER) }.getOrNull()
    }

    private fun buildHabitSummary(): String {
        return GlobalUtils.generateHabitNote(
            this,
            habitDraft.timesPerPeriod.toIntOrNull(),
            habitDraft.totalTarget.toIntOrNull(),
            habitDraft.toDeadlineFrequency()
        )
    }

    private fun calendarEventDisplayText(event: CalendarEvent): String {
        val time = GlobalUtils.parseDateTime(event.startMillis.toDateTimeString())
        val formattedTime = time?.let(::formatLocalDateTime) ?: getString(R.string.parse_failed)
        val title = event.title?.takeIf { it.isNotBlank() } ?: getString(R.string.untitled)
        return "$title - $formattedTime"
    }

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
        val tabs = listOf(stringResource(R.string.task), stringResource(R.string.habit))
        val pageSurfaceColor = MaterialTheme.colorScheme.surface
        val advancedMaterial = LocalAdvancedMaterialSpec.current
        val layoutDirection = LocalLayoutDirection.current
        val filteredCalendarEvents = calendarImportEvents.filter { event ->
            val query = calendarImportSearchQuery.trim()
            if (query.isBlank()) {
                true
            } else {
                calendarEventDisplayText(event).contains(query, ignoreCase = true)
            }
        }
        LaunchedEffect(autoRunAiOnAppear, aiInputText, aiAutoTriggered) {
            if (autoRunAiOnAppear && !aiAutoTriggered && aiInputText.isNotBlank()) {
                aiAutoTriggered = true
                parseAiInput()
            }
        }

        ProvideAdvancedMaterialDialogBlurHost {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .advancedMaterialDialogBlurHost()
            ) {
                if (showPickerDialogBlur) {
                    RegisterAdvancedMaterialDialogBlur()
                }
                PickerWindowBlurEffect(
                    active = showPickerDialogBlur,
                    surfaceColor = MaterialTheme.colorScheme.surface
                )

                AdaptiveMaterialScaffold(
                    containerColor = Color.Transparent,
                    contentColor = contentColorFor(Color.Transparent),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    advancedMaterialTopBarTintColor = MaterialTheme.colorScheme.surface,
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (advancedMaterial.enabled) Color.Transparent else pageSurfaceColor)
                        ) {
                            TopAppBar(
                                title = stringResource(R.string.add_task),
                                mode = TopAppBarStyle.CENTER,
                                titleTextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                                useParentMaterialContainer = advancedMaterial.enabled,
                                navigationIcon = {
                                    IconButton(onClick = { finishAfterTransition() }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_back),
                                            contentDescription = stringResource(R.string.close),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = expressiveTypeModifier
                                        )
                                    }
                                },
                                actions = {
                                    if (selectedPage == 0) {
                                        IconButton(onClick = { onImportFromCalendarClick() }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_event),
                                                contentDescription = stringResource(R.string.select_calendar_to_import),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = expressiveTypeModifier
                                            )
                                        }
                                    }
                                }
                            )
                            TabRow(
                                tabs = tabs,
                                selectedTabIndex = selectedPage,
                                onTabSelected = { selectedPage = it },
                                divider = { HorizontalDivider(color = Color.Transparent) },
                                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                            )
                        }
                    },
                    bottomBar = {
                        DeadlineBottomActions(
                            showSaveToCalendar = selectedPage == 0,
                            onSave = { requestSave(SaveAction.SAVE) },
                            onSaveToCalendar = { requestSave(SaveAction.SAVE_TO_CALENDAR) }
                        )
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(pageSurfaceColor)
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
                                        launchPickerWithBlur {
                                            GlobalUtils.showDateTimePicker(
                                                supportFragmentManager,
                                                onDialogVisibilityChanged = ::onPickerDialogVisibilityChanged
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
                                        launchPickerWithBlur {
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
                                                onDialogVisibilityChanged = ::onPickerDialogVisibilityChanged
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
                                        launchPickerWithBlur {
                                            GlobalUtils.showTimePicker(
                                                supportFragmentManager,
                                                initialTime = habitDraft.reminderTime,
                                                onDialogVisibilityChanged = ::onPickerDialogVisibilityChanged
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
                    AlertDialog(
                        show = true,
                        onDismissRequest = { showCalendarImportDialog = false },
                        renderTextContentInMiuix = true,
                        title = { Text(stringResource(R.string.select_calendar_to_import)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextButton(
                                    onClick = { showCalendarFilterDialog() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(stringResource(R.string.filter_calendar_account))
                                }
                                OutlinedTextField(
                                    value = calendarImportSearchQuery,
                                    onValueChange = { calendarImportSearchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(stringResource(R.string.search)) },
                                    miuixLabel = getString(R.string.search),
                                    singleLine = true
                                )
                                if (filteredCalendarEvents.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.no_task_add_calendar),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 320.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        filteredCalendarEvents.forEach { event ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedCalendarEventId = event.id }
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = selectedCalendarEventId == event.id,
                                                    onClick = { selectedCalendarEventId = event.id }
                                                )
                                                Text(
                                                    text = calendarEventDisplayText(event),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(start = 8.dp),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        miuixTitle = getString(R.string.select_calendar_to_import),
                        miuixSummary = getString(R.string.settings_import),
                        confirmButton = {
                            TextButton(onClick = {
                                if (selectedCalendarEventId == null) {
                                    Toast.makeText(
                                        this@AddDDLActivity,
                                        getString(R.string.no_event_select),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    applySelectedCalendarEvent()
                                }
                            }) {
                                Text(stringResource(R.string.settings_import))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCalendarImportDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                if (showCalendarAccountFilterDialog) {
                    AlertDialog(
                        show = true,
                        onDismissRequest = { showCalendarAccountFilterDialog = false },
                        renderTextContentInMiuix = true,
                        title = { Text(stringResource(R.string.select_calendar_account_to_hide)) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                calendarAccountOptions.forEach { account ->
                                    val checked = selectedCalendarAccounts.contains(account)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCalendarAccounts = if (checked) {
                                                    selectedCalendarAccounts - account
                                                } else {
                                                    selectedCalendarAccounts + account
                                                }
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { isChecked ->
                                                selectedCalendarAccounts = if (isChecked) {
                                                    selectedCalendarAccounts + account
                                                } else {
                                                    selectedCalendarAccounts - account
                                                }
                                            }
                                        )
                                        Text(
                                            text = account,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 8.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        },
                        miuixTitle = getString(R.string.select_calendar_account_to_hide),
                        miuixSummary = getString(R.string.filter_calendar_account),
                        confirmButton = {
                            TextButton(onClick = { saveCalendarAccountFilterSelection() }) {
                                Text(stringResource(R.string.accept))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCalendarAccountFilterDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
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
                Log.e("AddDDLActivity", "AI parse failed", e)
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
        val ddlId = repo.insertDDL(
            name = name,
            startTime = taskDraft.startTime.toString(),
            endTime = taskDraft.endTime.toString(),
            note = taskDraft.note,
            type = DeadlineType.TASK,
            calendarEventId = calendarEventId
        )

        repo.getDDLById(ddlId)?.let { inserted ->
            val item = if (inserted.isStared != taskDraft.isStarred) {
                inserted.copy(isStared = taskDraft.isStarred)
            } else {
                inserted
            }
            if (item != inserted) {
                repo.updateDDL(item)
            }

            if (GlobalUtils.deadlineNotification) {
                DeadlineAlarmScheduler.scheduleExactAlarm(applicationContext, item)
                DeadlineAlarmScheduler.scheduleUpcomingDDLAlarm(applicationContext, item)
            }

            if (toCalendar) {
                val calendarHelper = CalendarHelper(this)
                lifecycleScope.launch {
                    try {
                        val eventId = calendarHelper.insertEvent(item)
                        item.calendarEventId = eventId
                        repo.updateDDL(item)
                        Toast.makeText(
                            this@AddDDLActivity,
                            getString(R.string.add_calendar_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e("Calendar", e.toString())
                        Toast.makeText(
                            this@AddDDLActivity,
                            getString(R.string.add_calendar_failed, e.toString()),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        setResult(RESULT_OK)
        finishAfterTransition()
    }

    private fun saveHabit(name: String) {
        val isEbbinghaus = habitDraft.period == EditorHabitPeriod.EBBINGHAUS
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
        val frequencyType = if (isEbbinghaus) DeadlineFrequency.DAILY else habitDraft.toDeadlineFrequency()

        val meta = HabitMetaData(
            completedDates = emptySet(),
            frequencyType = frequencyType,
            frequency = if (goalType == HabitGoalType.TOTAL) 1 else frequency,
            total = if (goalType == HabitGoalType.TOTAL) total ?: 0 else 0,
            refreshDate = LocalDate.now().toString()
        )

        val ddlId = repo.insertDDL(
            name = name,
            startTime = "",
            endTime = "",
            note = meta.toJson(),
            type = DeadlineType.HABIT
        )

        habitRepo.createHabitForDdl(
            ddlId = ddlId,
            name = name,
            period = habitDraft.period.toModelPeriod(),
            timesPerPeriod = if (goalType == HabitGoalType.TOTAL) 1 else frequency,
            goalType = goalType,
            totalTarget = if (goalType == HabitGoalType.TOTAL) total else null,
            description = null
        )

        habitRepo.getHabitByDdlId(ddlId)?.let { habit ->
            val updated = habit.copy(
                alarmTime = if (habitDraft.reminderEnabled) {
                    habitDraft.reminderTime.format(REMINDER_TIME_FORMATTER)
                } else {
                    null
                }
            )
            habitRepo.updateHabit(updated)

            if (habitDraft.reminderEnabled) {
                DeadlineAlarmScheduler.scheduleHabitNotifyAlarm(applicationContext, ddlId)
            } else {
                DeadlineAlarmScheduler.cancelHabitNotifyAlarm(applicationContext, ddlId)
            }
        }

        setResult(RESULT_OK)
        finishAfterTransition()
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
            Log.e("Calendar", "No calendar permission", e)
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
            Log.e("Calendar", "No calendar permission", e)
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

    private fun formatLocalDateTime(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
        return dateTime.format(formatter)
    }

    private fun launchPickerWithBlur(showPicker: () -> Unit) {
        pendingPickerLaunchJob?.cancel()
        showPickerDialogBlur = true
        pendingPickerLaunchJob = lifecycleScope.launch {
            delay(PICKER_BLUR_LEAD_IN_MS)
            showPicker()
        }
    }

    private fun onPickerDialogVisibilityChanged(visible: Boolean) {
        if (!visible) {
            showPickerDialogBlur = false
        }
    }

    @Composable
    private fun PickerWindowBlurEffect(active: Boolean, surfaceColor: Color) {
        val blurProgress by animateFloatAsState(
            targetValue = if (active) 1f else 0f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            label = "add-ddl-picker-blur"
        )
        val scale by animateFloatAsState(
            targetValue = if (active) 0.98f else 1f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "add-ddl-picker-scale"
        )
        val blurRadius = (24f * blurProgress).coerceIn(0f, 24f)
        val saturation = 1f - ((1f - 0.5f) * blurProgress)

        val decorView = window?.decorView
        val contentView = decorView?.findViewById<android.view.View>(android.R.id.content) ?: decorView

        DisposableEffect(decorView, contentView) {
            val originalBackground = decorView?.background

            onDispose {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && contentView != null) {
                    contentView.setRenderEffect(null)
                }
                contentView?.scaleX = 1f
                contentView?.scaleY = 1f
                decorView?.background = originalBackground
            }
        }

        SideEffect {
            decorView?.setBackgroundColor(surfaceColor.toArgb())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && contentView != null) {
                val effects = mutableListOf<RenderEffect>()

                if (blurRadius >= 0.5f) {
                    effects += RenderEffect.createBlurEffect(
                        blurRadius,
                        blurRadius,
                        Shader.TileMode.CLAMP
                    )
                }
                if (saturation < 1f - 1e-3f) {
                    val colorMatrix = ColorMatrix().apply { setSaturation(saturation) }
                    effects += RenderEffect.createColorFilterEffect(
                        ColorMatrixColorFilter(colorMatrix)
                    )
                }

                contentView.setRenderEffect(
                    when (effects.size) {
                        0 -> null
                        1 -> effects[0]
                        else -> RenderEffect.createChainEffect(effects[0], effects[1])
                    }
                )
            }

            contentView?.scaleX = scale
            contentView?.scaleY = scale
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

private const val PICKER_BLUR_LEAD_IN_MS = 45L
