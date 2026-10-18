package com.aritxonly.deadliner.ui.picker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentManager
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.lang.ref.WeakReference
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong

internal enum class PickerKind { DateTime, Time, Date }

internal data class PickerRequest(
    val id: Long,
    val kind: PickerKind,
    val initialDate: LocalDate,
    val initialTime: LocalTime,
    val bounds: PickerDateBounds,
    val minimum: LocalDateTime? = null,
    val titleRes: Int = R.string.picker_choose_date,
    val onVisibilityChanged: (Boolean) -> Unit = {},
    val onSelected: (LocalDate, LocalTime) -> Unit,
)

internal class DateTimePickerController {
    var request by mutableStateOf<PickerRequest?>(null)
        private set
    fun show(next: PickerRequest) {
        dismiss()
        request = next
        next.onVisibilityChanged(true)
    }
    fun dismiss() {
        val current = request ?: return
        request = null
        current.onVisibilityChanged(false)
    }
    fun confirm(date: LocalDate, time: LocalTime) {
        val current = request ?: return
        if (date.isBefore(current.bounds.minimum) || date.isAfter(current.bounds.maximum)) return
        if (current.minimum != null && date.atTime(time).isBefore(current.minimum)) return
        request = null
        try { current.onSelected(date, time) } finally { current.onVisibilityChanged(false) }
    }
}

/** Legacy callers keep their FragmentManager contract; wheel requests belong to that Activity host. */
object DateTimePickers {
    private val hosts = WeakHashMap<FragmentManager, DateTimePickerController>()
    private val ids = AtomicLong()
    private val windows = WeakHashMap<FragmentManager, WeakReference<Window>>()
    internal fun attach(manager: FragmentManager, controller: DateTimePickerController, window: Window? = null) {
        hosts[manager] = controller
        window?.let { windows[manager] = WeakReference(it) }
    }
    private fun hideIme(manager: FragmentManager) {
        val window = windows[manager]?.get() ?: return
        WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
        window.currentFocus?.clearFocus()
    }
    internal fun detach(manager: FragmentManager, controller: DateTimePickerController) {
        if (hosts[manager] === controller) { hosts.remove(manager); windows.remove(manager); controller.dismiss() }
    }
    private fun wheelHost(manager: FragmentManager): DateTimePickerController? =
        if (GlobalUtils.appearanceFlow.value.useWheelDateTimePicker) hosts[manager] else null

    fun showDateTimePicker(
        fragmentManager: FragmentManager,
        afterDateTime: LocalDateTime? = null,
        makeToast: (String) -> Unit = {},
        onDialogVisibilityChanged: (Boolean) -> Unit = {},
        initialDateTime: LocalDateTime? = null,
        onDateTimeSelected: (LocalDateTime) -> Unit,
    ) {
        hideIme(fragmentManager)
        val host = wheelHost(fragmentManager)
        if (host == null) {
            MaterialDateTimePickers.showDateTimePicker(fragmentManager, afterDateTime, makeToast,
                onDialogVisibilityChanged, initialDateTime, onDateTimeSelected)
            return
        }
        val minimum = afterDateTime?.let(::ceilToPickerMinute)
        val initial = pickerInitialDateTime(initialDateTime, minimum)
        host.show(PickerRequest(
            id = ids.incrementAndGet(), kind = PickerKind.DateTime,
            initialDate = initial.toLocalDate(), initialTime = initial.toLocalTime(), minimum = minimum,
            bounds = PickerDateBounds(minimum?.toLocalDate() ?: LocalDate.of(1900, 1, 1),
                LocalDate.of(maxOf(2100, initial.year), 12, 31)),
            onVisibilityChanged = onDialogVisibilityChanged,
            onSelected = { date, time -> onDateTimeSelected(date.atTime(time)) },
        ))
    }

    fun showTimePicker(
        fragmentManager: FragmentManager,
        initialTime: LocalTime = LocalTime.now(),
        onDialogVisibilityChanged: (Boolean) -> Unit = {},
        onTimeSelected: (LocalTime) -> Unit,
    ) {
        hideIme(fragmentManager)
        val host = wheelHost(fragmentManager)
        if (host == null) {
            MaterialDateTimePickers.showTimePicker(fragmentManager, initialTime, onDialogVisibilityChanged, onTimeSelected)
            return
        }
        val today = LocalDate.now()
        host.show(PickerRequest(
            id = ids.incrementAndGet(), kind = PickerKind.Time,
            initialDate = today, initialTime = initialTime.withSecond(0).withNano(0),
            bounds = PickerDateBounds(today, today), onVisibilityChanged = onDialogVisibilityChanged,
            onSelected = { _, time -> onTimeSelected(time) },
        ))
    }

    fun showRetroactiveDatePicker(fragmentManager: FragmentManager, onDatePicked: (Long) -> Unit) {
        hideIme(fragmentManager)
        val host = wheelHost(fragmentManager)
        if (host == null) {
            MaterialDateTimePickers.showRetroactiveDatePicker(fragmentManager, onDatePicked)
            return
        }
        val today = LocalDate.now()
        host.show(PickerRequest(
            id = ids.incrementAndGet(), kind = PickerKind.Date,
            initialDate = today, initialTime = LocalTime.MIDNIGHT,
            bounds = PickerDateBounds(LocalDate.of(1900, 1, 1), today),
            titleRes = R.string.choose_retro_date_title,
            onSelected = { date, _ -> onDatePicked(dateToUtcMillis(date)) },
        ))
    }
}
