package com.aritxonly.deadliner.ui.picker

import androidx.fragment.app.FragmentManager
import com.aritxonly.deadliner.R
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalDateTime
import java.time.LocalTime

internal object MaterialDateTimePickers {
    /**
     * 显示日期和时间选择器
     * @param afterDateTime 若不为 null，则限制只能选择该时间之后（含当天）的日期和时间
     */
    fun showDateTimePicker(
        fragmentManager: FragmentManager,
        afterDateTime: LocalDateTime? = null,
        makeToast: (String) -> Unit = {},
        onDialogVisibilityChanged: (Boolean) -> Unit = {},
        initialDateTime: LocalDateTime? = null,
        onDateTimeSelected: (LocalDateTime) -> Unit,
    ) {
        // 是否需要限制
        val minimum = afterDateTime?.let(::ceilToPickerMinute)
        val minDayStartMillisUtc = minimum?.toLocalDate()?.let(::dateToUtcMillis)

        val initial = pickerInitialDateTime(initialDateTime, minimum)
        val defaultSelection = dateToUtcMillis(initial.toLocalDate())

        val builder = MaterialDatePicker.Builder.datePicker()
            .setSelection(defaultSelection)

        if (minDayStartMillisUtc != null) {
            val constraints = CalendarConstraints.Builder()
                .setStart(minDayStartMillisUtc)
                .setValidator(DateValidatorPointForward.from(minDayStartMillisUtc))
                .build()
            builder.setCalendarConstraints(constraints)
        }

        val datePicker = builder.build()
        var timePickerOpened = false

        datePicker.addOnPositiveButtonClickListener { selectedDateUtcMillis ->
            timePickerOpened = true
            val selectedLocalDate = utcMillisToDate(selectedDateUtcMillis)

            val baseDateTime = selectedLocalDate.atStartOfDay()
            val minAllowedTime: LocalTime? =
                if (minimum != null && selectedLocalDate == minimum.toLocalDate()) {
                    minimum.toLocalTime()
                } else null

            showTimePickerWithGuard(
                fragmentManager = fragmentManager,
                datePart = baseDateTime,
                minAllowedTime = minAllowedTime,
                preferredTime = initial.toLocalTime(),
                makeToast = makeToast,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onDateTimeSelected = onDateTimeSelected
            )
        }

        datePicker.addOnDismissListener {
            if (!timePickerOpened) {
                onDialogVisibilityChanged(false)
            }
        }

        onDialogVisibilityChanged(true)
        datePicker.show(fragmentManager, "datePicker_after_${minDayStartMillisUtc ?: "none"}")
    }

    /**
     * 时间选择器，带可选的最小时间限制
     */
    private fun showTimePickerWithGuard(
        fragmentManager: FragmentManager,
        datePart: LocalDateTime,
        minAllowedTime: LocalTime?,
        preferredTime: LocalTime,
        makeToast: (String) -> Unit = {},
        onDialogVisibilityChanged: (Boolean) -> Unit = {},
        onDateTimeSelected: (LocalDateTime) -> Unit,
    ) {
        val initialTime = if (minAllowedTime != null && preferredTime.isBefore(minAllowedTime))
            minAllowedTime else preferredTime

        fun buildAndShow(hour: Int, minute: Int) {
            onDialogVisibilityChanged(true)
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build()
            var relaunching = false

            timePicker.addOnPositiveButtonClickListener {
                val picked = LocalTime.of(timePicker.hour, timePicker.minute)

                if (minAllowedTime != null && picked.isBefore(minAllowedTime)) {
                    relaunching = true
                    makeToast("${minAllowedTime.hour.toString().padStart(2, '0')}:${minAllowedTime.minute.toString().padStart(2, '0')}")
                    buildAndShow(minAllowedTime.hour, minAllowedTime.minute)
                    return@addOnPositiveButtonClickListener
                }

                onDateTimeSelected(datePart.withHour(picked.hour).withMinute(picked.minute))
            }

            timePicker.addOnDismissListener {
                if (!relaunching) {
                    onDialogVisibilityChanged(false)
                }
            }

            timePicker.show(fragmentManager, "timePicker_${datePart.toLocalDate()}_$hour:$minute")
        }

        buildAndShow(initialTime.hour, initialTime.minute)
    }

    fun showTimePicker(
        fragmentManager: FragmentManager,
        initialTime: LocalTime = LocalTime.now(),
        onDialogVisibilityChanged: (Boolean) -> Unit = {},
        onTimeSelected: (LocalTime) -> Unit,
    ) {
        onDialogVisibilityChanged(true)
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialTime.hour)
            .setMinute(initialTime.minute)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            onTimeSelected(LocalTime.of(timePicker.hour, timePicker.minute))
        }
        timePicker.addOnDismissListener {
            onDialogVisibilityChanged(false)
        }

        timePicker.show(fragmentManager, "timePicker_only_${initialTime.hour}:${initialTime.minute}")
    }

    fun showRetroactiveDatePicker(fragmentManager: FragmentManager, onDatePicked: (Long) -> Unit) {
        // 限制不选未来日期
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.choose_retro_date_title)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            onDatePicked(selection)
        }

        picker.show(fragmentManager, "retro_date_picker")
    }

}
