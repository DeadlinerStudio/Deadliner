package com.aritxonly.deadliner.ui.picker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.AlertDialog
import com.aritxonly.deadliner.ui.base.TextButton
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun MiuixDateTimePicker(
    request: PickerRequest,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalTime) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val layout = if (request.kind == PickerKind.DateTime)
        pickerLayout(configuration.screenWidthDp, configuration.screenHeightDp, configuration.fontScale)
        else PickerLayout.Steps
    val combined = layout != PickerLayout.Steps
    var dateEpochDay by rememberSaveable { mutableLongStateOf(request.initialDate.toEpochDay()) }
    var hour by rememberSaveable { mutableIntStateOf(request.initialTime.hour) }
    var minute by rememberSaveable { mutableIntStateOf(request.initialTime.minute) }
    var showingTime by rememberSaveable { mutableStateOf(request.kind == PickerKind.Time) }
    var displayedTime by remember { mutableStateOf(showingTime) }
    var stageChanging by remember { mutableStateOf(false) }
    val stageOpacity = remember { Animatable(1f) }
    LaunchedEffect(showingTime) {
        if (displayedTime != showingTime) {
            stageChanging = true
            try {
                // Keep a faint content layer and avoid inserting an empty frame between stages.
                stageOpacity.animateTo(0.12f, tween(90))
                displayedTime = showingTime
                stageOpacity.animateTo(1f, tween(150))
            } finally { stageChanging = false }
        }
    }
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val selectedTime = LocalTime.of(hour, minute)
    val canConfirm = request.minimum == null || !date.atTime(selectedTime).isBefore(request.minimum)
    val title = stringResource(if (combined) R.string.picker_choose_date_time
        else if (displayedTime) R.string.picker_choose_time else request.titleRes)
    val dateLabel = remember(date) { date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }

    fun updateDateTime(next: LocalDateTime) {
        dateEpochDay = next.toLocalDate().toEpochDay()
        hour = next.hour
        minute = next.minute
    }

    AlertDialog(
        show = true,
        onDismissRequest = onDismiss,
        miuixTitle = title,
        miuixMaxWidth = if (layout == PickerLayout.CombinedRow) 640.dp else 420.dp,
        renderTextContentInMiuix = true,
        confirmButton = {
            TextButton(
                enabled = !stageChanging && (if (combined) canConfirm else !showingTime || canConfirm),
                onClick = {
                    if (!combined && !showingTime && request.kind == PickerKind.DateTime) {
                        val minimum = pickerMinimumTime(date, request.minimum)
                        val time = if (minimum != null && selectedTime.isBefore(minimum)) minimum else selectedTime
                        hour = time.hour
                        minute = time.minute
                        showingTime = true
                    } else onConfirm(date, selectedTime)
                },
            ) {
                Text(stringResource(if (!combined && !displayedTime && request.kind == PickerKind.DateTime)
                    R.string.picker_next else R.string.accept))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = {
            if (combined) {
                val dateContent: @Composable (Modifier) -> Unit = { modifier ->
                    MiuixDatePicker(date, request.bounds, modifier = modifier,
                        onDateChange = { next ->
                            val selected = next.atTime(hour, minute)
                            updateDateTime(if (request.minimum != null && selected.isBefore(request.minimum))
                                request.minimum else selected)
                        })
                }
                val timeContent: @Composable (Modifier) -> Unit = { modifier ->
                    MiuixTimePicker(date.atTime(hour, minute), request.bounds, request.minimum, true,
                        modifier = modifier, onDateTimeChange = ::updateDateTime)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    dateContent(Modifier.weight(3.2f))
                    timeContent(Modifier.weight(2f))
                }
            } else {
                // Fixed glass geometry, one wheel tree at a time: no per-frame glass resize/remeasure.
                Box(
                    Modifier.fillMaxWidth().height(if (request.kind == PickerKind.DateTime) 320.dp else 251.dp)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(Modifier.fillMaxWidth().graphicsLayer {
                        alpha = stageOpacity.value
                        translationX = (1f - stageOpacity.value) * (if (displayedTime) 16.dp.toPx() else -16.dp.toPx())
                    }) {
                        if (displayedTime) {
                            if (request.kind == PickerKind.DateTime) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(dateLabel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { showingTime = false }, useMiuixDialogActionColors = false) {
                                        Text(stringResource(R.string.picker_change_date))
                                    }
                                }
                            }
                            MiuixTimePicker(
                                dateTime = date.atTime(hour, minute), bounds = request.bounds,
                                minimum = request.minimum, carryDate = request.kind == PickerKind.DateTime,
                                onDateTimeChange = { next ->
                                    dateEpochDay = next.toLocalDate().toEpochDay()
                                    hour = next.hour
                                    minute = next.minute
                                },
                            )
                        } else {
                            MiuixDatePicker(
                                date = date, bounds = request.bounds,
                                onDateChange = { dateEpochDay = it.toEpochDay() },
                            )
                        }
                    }
                }
            }
        },
    )
}

/** The day and month wheels use chronological indices, carrying into their preceding column. */
@Composable
internal fun MiuixDatePicker(
    date: LocalDate, bounds: PickerDateBounds, onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier, visibleItemCount: Int = 5,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NumberWheel(value = date.year, range = bounds.years,
            visibleItemCount = visibleItemCount, title = stringResource(R.string.picker_year), modifier = Modifier.weight(1.2f),
            onValueChange = { onDateChange(bounds.clamp(it, date.monthValue, date.dayOfMonth)) })
        NumberWheel(value = pickerMonthIndex(date),
            range = pickerMonthIndex(bounds.minimum)..pickerMonthIndex(bounds.maximum),
            visibleItemCount = visibleItemCount, title = stringResource(R.string.picker_month), modifier = Modifier.weight(1f),
            label = { twoDigits(Math.floorMod(it, 12) + 1) },
            onValueChange = { onDateChange(dateAtPickerMonth(it, date, bounds)) })
        NumberWheel(value = date.toEpochDay().toInt(),
            range = bounds.minimum.toEpochDay().toInt()..bounds.maximum.toEpochDay().toInt(),
            visibleItemCount = visibleItemCount, title = stringResource(R.string.picker_day), modifier = Modifier.weight(1f),
            label = { twoDigits(LocalDate.ofEpochDay(it.toLong()).dayOfMonth) },
            onValueChange = { onDateChange(LocalDate.ofEpochDay(it.toLong())) })
    }
}

@Composable
internal fun MiuixTimePicker(
    dateTime: LocalDateTime, bounds: PickerDateBounds, minimum: LocalDateTime?, carryDate: Boolean,
    onDateTimeChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier, visibleItemCount: Int = 5,
) {
    val first = minimum ?: bounds.minimum.atStartOfDay()
    val last = bounds.maximum.atTime(23, 59)
    fun emit(next: LocalDateTime) = onDateTimeChange(next.coerceIn(first, last))
    val minuteIndex = if (carryDate) pickerMinuteIndex(dateTime) else dateTime.hour * 60 + dateTime.minute
    val hourIndex = if (carryDate) Math.floorDiv(pickerMinuteIndex(dateTime), 60) else dateTime.hour
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NumberWheel(hourIndex,
            if (carryDate) Math.floorDiv(pickerMinuteIndex(first), 60)..Math.floorDiv(pickerMinuteIndex(last), 60) else 0..23,
            stringResource(R.string.picker_hour),
            onValueChange = { index ->
                emit(if (carryDate) dateTimeAtPickerHour(index, dateTime.minute)
                    else dateTime.toLocalDate().atTime(index, dateTime.minute))
            },
            modifier = Modifier.weight(1f), visibleItemCount = visibleItemCount, wrapAround = !carryDate,
            label = { twoDigits(Math.floorMod(it, 24)) })
        Text(":", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp))
        NumberWheel(minuteIndex,
            if (carryDate) pickerMinuteIndex(first)..pickerMinuteIndex(last) else 0..1439,
            stringResource(R.string.picker_minute),
            onValueChange = { index ->
                emit(if (carryDate) dateTimeAtPickerMinute(index)
                    else dateTime.toLocalDate().atTime(index / 60, index % 60))
            },
            modifier = Modifier.weight(1f), visibleItemCount = visibleItemCount, wrapAround = !carryDate,
            label = { twoDigits(Math.floorMod(it, 60)) })
    }
}

private fun twoDigits(value: Int) = String.format(Locale.getDefault(), "%02d", value)

@Composable
private fun NumberWheel(
    value: Int, range: IntRange, title: String, onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier, wrapAround: Boolean = false,
    label: (Int) -> String = ::twoDigits, visibleItemCount: Int = 5,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        DeadlinerNumberPicker(
            value = value, onValueChange = onValueChange, range = range,
            label = label,
            visibleItemCount = visibleItemCount, wrapAround = wrapAround,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = title
                stateDescription = label(value)
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), range.first.toFloat()..range.last.toFloat(),
                    (range.last - range.first - 1).coerceAtLeast(0))
                setProgress { target ->
                    onValueChange(target.roundToInt().coerceIn(range))
                    true
                }
            },
        )
    }
}
