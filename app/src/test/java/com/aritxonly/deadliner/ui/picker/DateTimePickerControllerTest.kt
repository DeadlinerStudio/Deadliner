package com.aritxonly.deadliner.ui.picker

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class DateTimePickerControllerTest {
    private val date = LocalDate.of(2026, 10, 9)
    private fun request(
        id: Long = 1, minimum: LocalDateTime? = null,
        visible: (Boolean) -> Unit = {}, selected: (LocalDate, LocalTime) -> Unit = { _, _ -> },
    ) = PickerRequest(id, PickerKind.DateTime, date, LocalTime.NOON,
        PickerDateBounds(date, date.plusDays(10)), minimum = minimum,
        onVisibilityChanged = visible, onSelected = selected)

    @Test fun cancellationNotifiesOnceAndNeverWritesSelection() {
        val controller = DateTimePickerController()
        val visibility = mutableListOf<Boolean>()
        var selected = false
        controller.show(request(visible = { visibility += it }, selected = { _, _ -> selected = true }))
        controller.dismiss()
        controller.dismiss()
        assertEquals(listOf(true, false), visibility)
        assertFalse(selected)
        assertNull(controller.request)
    }

    @Test fun invalidMinimumKeepsDialogOpenUntilValidConfirmation() {
        val controller = DateTimePickerController()
        val minimum = date.atTime(15, 42)
        var result: LocalDateTime? = null
        controller.show(request(minimum = minimum, selected = { d, t -> result = d.atTime(t) }))
        controller.confirm(date, LocalTime.of(15, 41))
        assertNotNull(controller.request)
        assertNull(result)
        controller.confirm(date, LocalTime.of(15, 42))
        assertEquals(minimum, result)
        assertNull(controller.request)
    }

    @Test fun replacementCancelsThePreviousRequestBeforeShowingTheNext() {
        val controller = DateTimePickerController()
        val events = mutableListOf<String>()
        controller.show(request(id = 1, visible = { events += "first:$it" }))
        controller.show(request(id = 2, visible = { events += "second:$it" }))
        assertEquals(listOf("first:true", "first:false", "second:true"), events)
        assertEquals(2L, controller.request?.id)
    }

    @Test fun datesOutsideTheAllowedRangeNeverWriteSelection() {
        val controller = DateTimePickerController()
        var selected = false
        controller.show(request(selected = { _, _ -> selected = true }))
        controller.confirm(date.minusDays(1), LocalTime.NOON)
        controller.confirm(date.plusDays(11), LocalTime.NOON)
        assertFalse(selected)
        assertNotNull(controller.request)
    }
}
