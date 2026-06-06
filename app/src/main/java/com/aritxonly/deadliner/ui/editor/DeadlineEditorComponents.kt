package com.aritxonly.deadliner.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.aritxonly.deadliner.ui.base.Switch
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import java.time.LocalDateTime
import java.time.LocalTime
import top.yukonga.miuix.kmp.blur.BlendColorEntry

data class TaskEditorDraft(
    val name: String = "",
    val note: String = "",
    val startTime: LocalDateTime = LocalDateTime.now(),
    val endTime: LocalDateTime = LocalDateTime.now().plusHours(1),
    val isStarred: Boolean = false,
)

data class HabitEditorDraft(
    val name: String = "",
    val period: EditorHabitPeriod = EditorHabitPeriod.DAILY,
    val goalType: HabitGoalType = HabitGoalType.PER_PERIOD,
    val timesPerPeriod: String = "1",
    val totalTarget: String = "",
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(20, 0),
)

enum class EditorHabitPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    EBBINGHAUS,
}

fun HabitEditorDraft.toDeadlineFrequency(): DeadlineFrequency {
    return when (goalType) {
        HabitGoalType.TOTAL -> DeadlineFrequency.TOTAL
        HabitGoalType.PER_PERIOD -> period.toDeadlineFrequency()
    }
}

fun DeadlineFrequency.toEditorHabitPeriod(default: EditorHabitPeriod = EditorHabitPeriod.DAILY): EditorHabitPeriod {
    return when (this) {
        DeadlineFrequency.DAILY -> EditorHabitPeriod.DAILY
        DeadlineFrequency.WEEKLY -> EditorHabitPeriod.WEEKLY
        DeadlineFrequency.MONTHLY -> EditorHabitPeriod.MONTHLY
        DeadlineFrequency.TOTAL -> default
    }
}

fun EditorHabitPeriod.toModelPeriod(): HabitPeriod {
    return when (this) {
        EditorHabitPeriod.DAILY -> HabitPeriod.DAILY
        EditorHabitPeriod.WEEKLY -> HabitPeriod.WEEKLY
        EditorHabitPeriod.MONTHLY -> HabitPeriod.MONTHLY
        EditorHabitPeriod.EBBINGHAUS -> HabitPeriod.EBBINGHAUS
    }
}

private fun EditorHabitPeriod.toDeadlineFrequency(): DeadlineFrequency {
    return when (this) {
        EditorHabitPeriod.DAILY -> DeadlineFrequency.DAILY
        EditorHabitPeriod.WEEKLY -> DeadlineFrequency.WEEKLY
        EditorHabitPeriod.MONTHLY -> DeadlineFrequency.MONTHLY
        EditorHabitPeriod.EBBINGHAUS -> DeadlineFrequency.DAILY
    }
}

@Composable
fun DeadlineAiQuickAddCard(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldShape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    EditorSectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.ai_quick_add),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.ai_quick_add_placeholder)) },
            miuixLabel = stringResource(R.string.ai_quick_add_placeholder),
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3,
            shape = textFieldShape,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = onSubmit,
                enabled = !isLoading && value.trim().isNotEmpty(),
                forceMaterial3 = true,
            ) {
                Text(
                    if (isLoading) stringResource(R.string.ai_parsing)
                    else stringResource(R.string.ai_parse),
                )
            }
        }
    }
}

@Composable
fun DeadlineNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldShape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.add_ddl_name)) },
        miuixLabel = stringResource(R.string.add_ddl_name),
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = textFieldShape,
    )
}

@Composable
fun DeadlineStarToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    EditorSectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.star),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}

@Composable
fun TaskEditorSection(
    draft: TaskEditorDraft,
    onDraftChange: (TaskEditorDraft) -> Unit,
    formatDateTime: (LocalDateTime) -> String,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldShape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditorDateTimeCard(
            title = stringResource(R.string.start_time),
            value = formatDateTime(draft.startTime),
            onClick = onPickStartTime,
        )
        EditorDateTimeCard(
            title = stringResource(R.string.end_time),
            value = formatDateTime(draft.endTime),
            onClick = onPickEndTime,
        )
        OutlinedTextField(
            value = draft.note,
            onValueChange = { onDraftChange(draft.copy(note = it)) },
            label = { Text(stringResource(R.string.add_ddl_note)) },
            miuixLabel = stringResource(R.string.add_ddl_note),
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            shape = textFieldShape,
        )
    }
}

@Composable
fun HabitEditorSection(
    draft: HabitEditorDraft,
    onDraftChange: (HabitEditorDraft) -> Unit,
    summaryText: String,
    onPickReminderTime: () -> Unit,
    reminderTimeLabel: String,
    onClearReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditorSectionCard {
            Text(
                text = stringResource(R.string.editor_habit_period),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            HabitPeriodRow(
                selected = draft.period,
                onSelect = {
                    val normalizedDraft = if (it == EditorHabitPeriod.EBBINGHAUS) {
                        draft.copy(
                            period = it,
                            goalType = HabitGoalType.PER_PERIOD,
                            timesPerPeriod = "1",
                            totalTarget = "",
                        )
                    } else {
                        draft.copy(period = it)
                    }
                    onDraftChange(normalizedDraft)
                },
            )
        }

        if (draft.period == EditorHabitPeriod.EBBINGHAUS) {
            EditorSectionCard {
                Text(
                    text = stringResource(R.string.editor_habit_ebbinghaus_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.editor_habit_ebbinghaus_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val textFieldShape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
            EditorSectionCard {
                Text(
                    text = stringResource(R.string.editor_habit_goal_type),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                HabitGoalTypeRow(
                    selected = draft.goalType,
                    onSelect = { onDraftChange(draft.copy(goalType = it)) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.timesPerPeriod,
                        onValueChange = { onDraftChange(draft.copy(timesPerPeriod = it.filter(Char::isDigit))) },
                        label = { Text(stringResource(R.string.editor_habit_times_per_period)) },
                        miuixLabel = stringResource(R.string.editor_habit_times_per_period),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = textFieldShape,
                    )
                    if (draft.goalType == HabitGoalType.TOTAL) {
                        OutlinedTextField(
                            value = draft.totalTarget,
                            onValueChange = { onDraftChange(draft.copy(totalTarget = it.filter(Char::isDigit))) },
                            label = { Text(stringResource(R.string.editor_habit_total_target)) },
                            miuixLabel = stringResource(R.string.editor_habit_total_target),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = textFieldShape,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HabitReminderSection(
            enabled = draft.reminderEnabled,
            reminderTimeLabel = reminderTimeLabel,
            onPickTime = onPickReminderTime,
            onClearReminder = onClearReminder,
        )
    }
}

@Composable
fun DeadlineBottomActions(
    showSaveToCalendar: Boolean,
    onSave: () -> Unit,
    onSaveToCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val backdrop = LocalAdvancedMaterialBackdrop.current
    val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = advancedMaterial.navigationTintAlpha * 0.55f)
    val materialColors = advancedMaterial.rememberBlurColors(listOf(BlendColorEntry(surfaceTint)))

    Box(
        modifier = modifier
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
                        shape = RectangleShape,
                    )
                }
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 12.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showSaveToCalendar) {
                Button(
                    onClick = onSaveToCalendar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(text = stringResource(R.string.save_and_add_to_calendar))
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun EditorSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content,
        )
    }
}

@Composable
private fun EditorDateTimeCard(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HabitReminderSection(
    enabled: Boolean,
    reminderTimeLabel: String,
    onPickTime: () -> Unit,
    onClearReminder: () -> Unit,
) {
    EditorSectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPickTime),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.editor_habit_reminder_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (enabled) reminderTimeLabel else stringResource(R.string.editor_habit_no_reminder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (enabled) {
                TextButton(onClick = onClearReminder) {
                    Text(stringResource(R.string.editor_habit_clear_reminder))
                }
            } else {
                Icon(
                    imageVector = iconResource(R.drawable.ic_event),
                    contentDescription = stringResource(R.string.set_time),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HabitPeriodRow(
    selected: EditorHabitPeriod,
    onSelect: (EditorHabitPeriod) -> Unit,
) {
    val options = listOf(
        EditorHabitPeriod.DAILY to stringResource(R.string.frequency_daily),
        EditorHabitPeriod.WEEKLY to stringResource(R.string.frequency_weekly),
        EditorHabitPeriod.MONTHLY to stringResource(R.string.frequency_monthly),
        EditorHabitPeriod.EBBINGHAUS to stringResource(R.string.editor_habit_period_ebbinghaus),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, (type, label) ->
            ToggleButton(
                checked = selected == type,
                onCheckedChange = { onSelect(type) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = connectedButtonShapes(index, options.lastIndex),
                colors = editorConnectedToggleButtonColors(),
                contentPadding = editorConnectedToggleContentPadding(),
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HabitGoalTypeRow(
    selected: HabitGoalType,
    onSelect: (HabitGoalType) -> Unit,
) {
    val options = listOf(
        HabitGoalType.PER_PERIOD to stringResource(R.string.editor_habit_goal_per_period),
        HabitGoalType.TOTAL to stringResource(R.string.editor_habit_goal_total),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, (type, label) ->
            ToggleButton(
                checked = selected == type,
                onCheckedChange = { onSelect(type) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = connectedButtonShapes(index, options.lastIndex),
                colors = editorConnectedToggleButtonColors(),
                contentPadding = editorConnectedToggleContentPadding(),
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun connectedButtonShapes(index: Int, lastIndex: Int): ToggleButtonShapes {
    return when (index) {
        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun editorConnectedToggleButtonColors() = ToggleButtonDefaults.toggleButtonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    checkedContainerColor = MaterialTheme.colorScheme.primary,
    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun editorConnectedToggleContentPadding() = PaddingValues(
    horizontal = 8.dp,
    vertical = 0.dp,
)
