package com.aritxonly.deadliner.notification

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

internal object NotificationTiming {
    fun dueInstant(raw: String?): Instant? = raw?.let {
        runCatching { Instant.parse(it) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(it).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()
    }

    fun inLiveWindow(dueAt: String?, advanceMinutes: Int, now: Instant = Instant.now()): Boolean {
        val due = dueInstant(dueAt) ?: return false
        return due.isAfter(now) && !now.isBefore(due.minusSeconds(advanceMinutes.coerceAtLeast(0) * 60L))
    }
}
