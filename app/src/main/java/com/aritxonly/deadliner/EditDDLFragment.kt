package com.aritxonly.deadliner

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.toJson
import com.aritxonly.deadliner.ui.base.AdaptiveMaterialScaffold
import com.aritxonly.deadliner.ui.base.RegisterAdvancedMaterialDialogBlur
import com.aritxonly.deadliner.ui.base.TopAppBar
import com.aritxonly.deadliner.ui.base.TopAppBarStyle
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
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class EditDDLFragment(
    private val ddlItem: DDLItem,
    private val onUpdate: (DDLItem) -> Unit
) : DialogFragment() {

    private val habitRepo by lazy { HabitRepository() }
    private var pendingPickerLaunchJob: Job? = null
    private var showPickerDialogBlur by mutableStateOf(false)

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setWindowAnimations(R.style.DialogAnimation)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DeadlinerTheme {
                    EditDDLScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EditDDLScreen() {
        val context = LocalContext.current
        val initialHabit = remember(ddlItem.id) { habitRepo.getHabitByDdlId(ddlItem.id) }
        val initialTaskDraft = remember(ddlItem.id) { ddlItem.toTaskEditorDraft() }
        val initialHabitDraft = remember(ddlItem.id) { ddlItem.toHabitEditorDraft(initialHabit) }

        var taskDraft by remember(ddlItem.id) { mutableStateOf(initialTaskDraft) }
        var habitDraft by remember(ddlItem.id) { mutableStateOf(initialHabitDraft) }
        val reminderTimeLabel = remember(habitDraft.reminderTime) {
            habitDraft.reminderTime.format(REMINDER_TIME_FORMATTER)
        }

        if (showPickerDialogBlur) {
            RegisterAdvancedMaterialDialogBlur()
        }
        PickerWindowBlurEffect(
            active = showPickerDialogBlur,
            surfaceColor = MaterialTheme.colorScheme.surface
        )

        AdaptiveMaterialScaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            advancedMaterialTopBarTintColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = stringResource(R.string.alert_edit_modify),
                    navigationIcon = {
                        IconButton(onClick = ::dismiss) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    },
                    mode = TopAppBarStyle.SMALL,
                    forceMaterial3 = true,
                    useParentMaterialContainer = true,
                )
            },
            bottomBar = {
                DeadlineBottomActions(
                    showSaveToCalendar = false,
                    onSave = {
                        when (ddlItem.type) {
                            DeadlineType.TASK -> saveTask(taskDraft)
                            DeadlineType.HABIT -> saveHabit(habitDraft)
                        }
                    },
                    onSaveToCalendar = {},
                )
            },
            content = { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        end = 16.dp,
                        bottom = 160.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        DeadlineNameField(
                            value = when (ddlItem.type) {
                                DeadlineType.TASK -> taskDraft.name
                                DeadlineType.HABIT -> habitDraft.name
                            },
                            onValueChange = { value ->
                                taskDraft = taskDraft.copy(name = value)
                                habitDraft = habitDraft.copy(name = value)
                            },
                        )
                    }

                    when (ddlItem.type) {
                        DeadlineType.TASK -> {
                            item {
                                DeadlineStarToggleCard(
                                    checked = taskDraft.isStarred,
                                    onCheckedChange = { taskDraft = taskDraft.copy(isStarred = it) },
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
                                                parentFragmentManager,
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
                                                parentFragmentManager,
                                                taskDraft.startTime,
                                                { chosen ->
                                                    Toast.makeText(
                                                        requireContext(),
                                                        getString(R.string.please_choose_the_time_after, chosen),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                onDialogVisibilityChanged = ::onPickerDialogVisibilityChanged
                                            ) { selected ->
                                                taskDraft = taskDraft.copy(endTime = selected)
                                            }
                                        }
                                    },
                                )
                            }
                        }

                        DeadlineType.HABIT -> {
                            item {
                                HabitEditorSection(
                                    draft = habitDraft,
                                    onDraftChange = { habitDraft = it },
                                    summaryText = buildHabitSummary(context, habitDraft),
                                    onPickReminderTime = {
                                        launchPickerWithBlur {
                                            GlobalUtils.showTimePicker(
                                                parentFragmentManager,
                                                initialTime = habitDraft.reminderTime,
                                                onDialogVisibilityChanged = ::onPickerDialogVisibilityChanged
                                            ) { selected ->
                                                habitDraft = habitDraft.copy(
                                                    reminderEnabled = true,
                                                    reminderTime = selected
                                                )
                                            }
                                        }
                                    },
                                    reminderTimeLabel = reminderTimeLabel,
                                    onClearReminder = {
                                        habitDraft = habitDraft.copy(reminderEnabled = false)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    private fun saveTask(draft: TaskEditorDraft) {
        val name = draft.name.trim()
        if (name.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.add_ddl_name), Toast.LENGTH_SHORT).show()
            return
        }

        val updatedDDL = ddlItem.copy(
            name = name,
            startTime = draft.startTime.toString(),
            endTime = draft.endTime.toString(),
            note = draft.note,
            isStared = draft.isStarred,
            type = DeadlineType.TASK,
        )
        onUpdate(updatedDDL)
        dismiss()
    }

    private fun saveHabit(draft: HabitEditorDraft) {
        val name = draft.name.trim()
        if (name.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.add_ddl_name), Toast.LENGTH_SHORT).show()
            return
        }

        val isEbbinghaus = draft.period == EditorHabitPeriod.EBBINGHAUS
        val goalType = if (isEbbinghaus) HabitGoalType.PER_PERIOD else draft.goalType
        val frequency = if (isEbbinghaus) {
            1
        } else {
            draft.timesPerPeriod.toIntOrNull()?.coerceAtLeast(1) ?: 1
        }
        val total = if (goalType == HabitGoalType.TOTAL) {
            draft.totalTarget.toIntOrNull()
        } else {
            null
        }
        val frequencyType = if (isEbbinghaus) DeadlineFrequency.DAILY else draft.toDeadlineFrequency()

        val meta = HabitMetaData(
            completedDates = emptySet(),
            frequencyType = frequencyType,
            frequency = if (goalType == HabitGoalType.TOTAL) 1 else frequency,
            total = if (goalType == HabitGoalType.TOTAL) total ?: 0 else 0,
            refreshDate = LocalDate.now().toString()
        )

        habitRepo.getHabitByDdlId(ddlItem.id)?.let { habit ->
            val updatedHabit = habit.copy(
                name = name,
                period = draft.period.toModelPeriod(),
                timesPerPeriod = if (goalType == HabitGoalType.TOTAL) 1 else frequency,
                goalType = goalType,
                totalTarget = if (goalType == HabitGoalType.TOTAL) total else null,
                alarmTime = if (draft.reminderEnabled) {
                    draft.reminderTime.format(REMINDER_TIME_FORMATTER)
                } else {
                    null
                }
            )
            habitRepo.updateHabit(updatedHabit)

            if (draft.reminderEnabled) {
                DeadlineAlarmScheduler.scheduleHabitNotifyAlarm(requireContext().applicationContext, ddlItem.id)
            } else {
                DeadlineAlarmScheduler.cancelHabitNotifyAlarm(requireContext().applicationContext, ddlItem.id)
            }
        }

        val updatedDDL = ddlItem.copy(
            name = name,
            startTime = "",
            endTime = "",
            note = meta.toJson(),
            type = DeadlineType.HABIT
        )
        onUpdate(updatedDDL)
        dismiss()
    }

    private fun DDLItem.toTaskEditorDraft(): TaskEditorDraft {
        val parsedStart = GlobalUtils.safeParseDateTime(startTime)
        val parsedEnd = GlobalUtils.parseDateTime(endTime) ?: parsedStart.plusHours(1)
        return TaskEditorDraft(
            name = name,
            note = note,
            startTime = parsedStart,
            endTime = parsedEnd,
            isStarred = isStared,
        )
    }

    private fun DDLItem.toHabitEditorDraft(habit: com.aritxonly.deadliner.model.Habit?): HabitEditorDraft {
        val meta = GlobalUtils.parseHabitMetaData(note)
        val reminderTime = parseReminderTime(habit?.alarmTime)
        val goalType = habit?.goalType
            ?: if (meta.frequencyType == DeadlineFrequency.TOTAL) HabitGoalType.TOTAL else HabitGoalType.PER_PERIOD
        val timesPerPeriod = habit?.timesPerPeriod?.toString()
            ?: meta.frequency.coerceAtLeast(1).toString()
        val totalTarget = habit?.totalTarget
            ?: meta.total.takeIf { it > 0 }

        return HabitEditorDraft(
            name = name,
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
            reminderTime = reminderTime ?: LocalTime.of(20, 0)
        )
    }

    private fun parseReminderTime(raw: String?): LocalTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalTime.parse(raw, REMINDER_TIME_FORMATTER) }.getOrNull()
    }

    private fun buildHabitSummary(context: android.content.Context, draft: HabitEditorDraft): String {
        return GlobalUtils.generateHabitNote(
            context,
            draft.timesPerPeriod.toIntOrNull(),
            draft.totalTarget.toIntOrNull(),
            draft.toDeadlineFrequency()
        )
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
            label = "edit-ddl-picker-blur"
        )
        val scale by animateFloatAsState(
            targetValue = if (active) 0.98f else 1f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "edit-ddl-picker-scale"
        )
        val blurRadius = (24f * blurProgress).coerceIn(0f, 24f)
        val saturation = 1f - ((1f - 0.5f) * blurProgress)

        val decorView = dialog?.window?.decorView
        val contentView = decorView?.findViewById<View>(android.R.id.content) ?: decorView

        DisposableEffect(decorView, contentView) {
            val originalBackground = decorView?.background

            onDispose {
                contentView?.setRenderEffect(null)
                contentView?.scaleX = 1f
                contentView?.scaleY = 1f
                decorView?.background = originalBackground
            }
        }

        SideEffect {
            decorView?.setBackgroundColor(surfaceColor.toArgb())

            if (contentView != null) {
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
                contentView.scaleX = scale
                contentView.scaleY = scale
            }
        }
    }

    override fun getTheme(): Int = android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen
}

private val REMINDER_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private const val PICKER_BLUR_LEAD_IN_MS = 45L
