package com.aritxonly.deadliner.ui.editor

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.picker.PickerDateBounds
import com.aritxonly.deadliner.ui.picker.PickerKind
import com.aritxonly.deadliner.ui.picker.PickerRequest
import com.aritxonly.deadliner.ui.picker.ceilToPickerMinute
import java.time.LocalDate
import java.time.LocalDateTime

private fun Context.fragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.takeUnless { it === this }?.fragmentActivity()
    else -> null
}

internal fun DeadlineEditorState.pickStartTime() = pickDateTime(null) { selected ->
    taskDraft = taskDraft.copy(startTime = selected,
        endTime = taskDraft.endTime.takeUnless { it.isBefore(selected) } ?: selected.plusHours(1))
}

internal fun DeadlineEditorState.pickEndTime() = pickDateTime(taskDraft.startTime) { selected ->
    taskDraft = taskDraft.copy(endTime = selected)
}

private fun DeadlineEditorState.pickDateTime(minimum: LocalDateTime?, onSelected: (LocalDateTime) -> Unit) {
    context.fragmentActivity()?.let { activity ->
        GlobalUtils.showDateTimePicker(activity.supportFragmentManager, minimum, onDateTimeSelected = onSelected)
        return
    }
    // ComponentActivity-only surfaces (e.g. Capture) can host the same Compose wheel directly.
    val now = LocalDateTime.now().withSecond(0).withNano(0)
    val lowerBound = minimum?.let(::ceilToPickerMinute)
    val initial = if (lowerBound != null && now.isBefore(lowerBound)) lowerBound else now
    datePicker.show(PickerRequest(
        id = System.nanoTime(), kind = PickerKind.DateTime,
        initialDate = initial.toLocalDate(), initialTime = initial.toLocalTime(), minimum = lowerBound,
        bounds = PickerDateBounds(lowerBound?.toLocalDate() ?: LocalDate.of(1900, 1, 1), LocalDate.of(maxOf(2100, initial.year), 12, 31)),
        onSelected = { date, time -> onSelected(date.atTime(time)) },
    ))
}

internal fun DeadlineEditorState.pickReminderTime() {
    val update: (java.time.LocalTime) -> Unit = { time ->
        habitDraft = habitDraft.copy(reminderEnabled = true, reminderTime = time.withSecond(0).withNano(0))
    }
    context.fragmentActivity()?.let { activity ->
        GlobalUtils.showTimePicker(activity.supportFragmentManager, habitDraft.reminderTime, onTimeSelected = update)
        return
    }
    val today = LocalDate.now()
    datePicker.show(PickerRequest(
        id = System.nanoTime(), kind = PickerKind.Time, initialDate = today, initialTime = habitDraft.reminderTime,
        bounds = PickerDateBounds(today, today), onSelected = { _, time -> update(time) },
    ))
}
