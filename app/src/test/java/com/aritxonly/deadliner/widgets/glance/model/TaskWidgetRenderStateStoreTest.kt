package com.aritxonly.deadliner.widgets.glance.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskWidgetRenderStateStoreTest {
    @Test
    fun `publishing data replaces snapshot and advances revision`() {
        val store = TaskWidgetRenderStateStore()
        val snapshot = snapshot(totalCount = 1)

        assertTrue(store.publish(snapshot))

        assertEquals(snapshot, store.state.value.snapshot)
        assertEquals(1L, store.state.value.revision)
    }

    @Test
    fun `configuration-only publication keeps snapshot and advances revision`() {
        val store = TaskWidgetRenderStateStore()
        val snapshot = snapshot(totalCount = 2)
        store.publish(snapshot)

        store.invalidateConfiguration()

        assertEquals(snapshot, store.state.value.snapshot)
        assertEquals(2L, store.state.value.revision)
    }

    @Test
    fun `empty store accepts a configuration-only publication`() {
        val store = TaskWidgetRenderStateStore()

        store.invalidateConfiguration()

        assertNull(store.state.value.snapshot)
        assertEquals(1L, store.state.value.revision)
    }

    @Test
    fun `publishing an identical snapshot does not advance revision`() {
        val store = TaskWidgetRenderStateStore()
        val snapshot = snapshot(totalCount = 3)

        assertTrue(store.publish(snapshot))
        assertFalse(store.publish(snapshot.copy()))

        assertEquals(snapshot, store.state.value.snapshot)
        assertEquals(1L, store.state.value.revision)
    }

    @Test
    fun `forced publication advances revision for appearance refresh`() {
        val store = TaskWidgetRenderStateStore()
        val snapshot = snapshot(totalCount = 4)
        store.publish(snapshot)

        assertTrue(store.publish(snapshot, forceRevision = true))

        assertEquals(2L, store.state.value.revision)
    }

    private fun snapshot(totalCount: Int) = TaskWidgetSnapshot(
        totalCount = totalCount,
        nearCount = 0,
        overdueCount = 0,
        summary = "Summary",
        items = emptyList(),
    )
}
