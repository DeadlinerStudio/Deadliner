package com.aritxonly.deadliner.ui.picker

import org.junit.Assert.*
import org.junit.Test

class PickerPresentationTest {
    @Test fun dragFramesPublishImmediatelyBeforeGestureCompletion() {
        val publisher = PickerValuePublisher(30)
        val events = mutableListOf<Int>()
        for (next in listOf(30, 31, 31, 32, 33)) {
            val before = events.size
            val changed = publisher.publish(next) { events += it }
            assertEquals(before + if (changed) 1 else 0, events.size)
            if (changed) assertEquals(next, events.last())
        }
        assertEquals(listOf(31, 32, 33), events)
        publisher.publish(31) { events += it }
        assertEquals(listOf(31, 32, 33, 31), events)
    }

    @Test fun phonesUseStepsAndWideScreensSelectDateAndTimeTogether() {
        assertEquals(PickerLayout.Steps, pickerLayout(393, 852, 1f))
        assertEquals(PickerLayout.Steps, pickerLayout(360, 760, 1.1f))
        assertEquals(PickerLayout.CombinedRow, pickerLayout(1280, 800, 1f))
        assertEquals(PickerLayout.CombinedRow, pickerLayout(673, 841, 1f))
    }

    @Test fun smallScreensAndLargeFontsKeepSteps() {
        assertEquals(PickerLayout.Steps, pickerLayout(280, 600, 1f))
        assertEquals(PickerLayout.Steps, pickerLayout(393, 500, 1f))
        assertEquals(PickerLayout.Steps, pickerLayout(1280, 800, 1.5f))
        assertEquals(PickerLayout.Steps, pickerLayout(600, 360, 1f))
    }
}
