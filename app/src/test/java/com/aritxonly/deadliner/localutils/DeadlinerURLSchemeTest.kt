package com.aritxonly.deadliner.localutils

import android.app.Application
import com.aritxonly.deadliner.ai.GeneratedDDL
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class DeadlinerURLSchemeTest {
    @Test
    fun v6TaskDraft_roundTrips() {
        val expected = GeneratedDDL(
            name = "KMP task",
            dueTime = LocalDateTime.parse("2026-08-13T21:30:00"),
            note = "shared from Lifi",
        )

        val url = DeadlinerURLScheme.encodeWithPassphrase(
            expected,
            "deadliner-2025".toCharArray(),
            iterations = 50_000,
        )

        assertEquals(
            expected,
            DeadlinerURLScheme.decodeWithPassphrase(url, "deadliner-2025".toCharArray()),
        )
    }

    @Test
    fun legacyCustomScheme_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DeadlinerURLScheme.decodeWithPassphrase(
                "deadliner://share?v=1",
                "deadliner-2025".toCharArray(),
            )
        }
    }
}
