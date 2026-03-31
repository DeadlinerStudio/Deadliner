package com.aritxonly.deadliner.sync

import com.aritxonly.deadliner.model.Ver
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

enum class LocalHabitBuildDecision {
    EMIT_TOMBSTONE,
    EMIT_ACTIVE,
    SKIP_MISSING_REMOTE_HABIT,
    SKIP_STALE_ACTIVE
}

enum class HabitApplyDecision {
    APPLY,
    SKIP_ALREADY_APPLIED,
    NOOP_TOMBSTONE_WITHOUT_CARRIER,
    ERROR_MISSING_CARRIER
}

object HabitSyncRules {
    fun compareVer(a: Ver, b: Ver): Int {
        if (a.ts != b.ts) return a.ts.compareTo(b.ts)
        if (a.ctr != b.ctr) return a.ctr.compareTo(b.ctr)
        return a.dev.compareTo(b.dev)
    }

    fun localBuildDecision(
        carrierDeleted: Boolean,
        habitExists: Boolean,
        carrierVer: Ver,
        carrierVerDev: String,
        localDeviceId: String,
        appliedVer: Ver?,
        habitUpdatedAt: LocalDateTime?
    ): LocalHabitBuildDecision {
        return when {
            carrierDeleted -> LocalHabitBuildDecision.EMIT_TOMBSTONE
            !habitExists && carrierVerDev == localDeviceId -> LocalHabitBuildDecision.EMIT_TOMBSTONE
            !habitExists -> LocalHabitBuildDecision.SKIP_MISSING_REMOTE_HABIT
            isHabitPayloadReady(carrierVer, appliedVer, habitUpdatedAt) -> LocalHabitBuildDecision.EMIT_ACTIVE
            else -> LocalHabitBuildDecision.SKIP_STALE_ACTIVE
        }
    }

    fun applyDecision(
        carrierExists: Boolean,
        deleted: Boolean,
        appliedVer: Ver?,
        incomingVer: Ver
    ): HabitApplyDecision {
        if (!carrierExists) {
            return if (deleted) {
                HabitApplyDecision.NOOP_TOMBSTONE_WITHOUT_CARRIER
            } else {
                HabitApplyDecision.ERROR_MISSING_CARRIER
            }
        }
        if (appliedVer != null && compareVer(incomingVer, appliedVer) <= 0) {
            return HabitApplyDecision.SKIP_ALREADY_APPLIED
        }
        return HabitApplyDecision.APPLY
    }

    fun isHabitPayloadReady(
        carrierVer: Ver,
        appliedVer: Ver?,
        habitUpdatedAt: LocalDateTime?
    ): Boolean {
        if (appliedVer != null && compareVer(appliedVer, carrierVer) >= 0) {
            return true
        }
        if (habitUpdatedAt == null) {
            return false
        }
        return habitUpdatedAt == parseVersionTs(carrierVer.ts)
    }

    private fun parseVersionTs(ts: String): LocalDateTime {
        return runCatching { LocalDateTime.parse(ts) }
            .getOrElse { OffsetDateTime.parse(ts).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime() }
    }
}
