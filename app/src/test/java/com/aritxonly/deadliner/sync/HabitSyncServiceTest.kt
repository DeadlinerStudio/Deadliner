package com.aritxonly.deadliner.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.Ver
import com.aritxonly.deadliner.web.WebUtils
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime

@RunWith(RobolectricTestRunner::class)
class HabitSyncServiceTest {
    private lateinit var context: Context
    private lateinit var db: DatabaseHelper
    private lateinit var sync: SyncService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DatabaseHelper.closeInstance()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
        db = DatabaseHelper.getInstance(context)
        sync = SyncService(db, WebUtils("http://127.0.0.1"))
    }

    @After
    fun tearDown() {
        DatabaseHelper.closeInstance()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
    }

    @Test
    fun firstHabitApplyUsesHabitAppliedVersionNotCarrierVersion() {
        val ddlId = db.insertDDL(
            name = "carrier",
            startTime = "2026-03-24T08:00:00",
            endTime = "2026-03-24T18:00:00",
            type = DeadlineType.HABIT
        )
        val uid = db.getDdlUidById(ddlId)!!
        val carrierVer = db.getDdlVersionById(ddlId)!!
        assertNull(db.getHabitAppliedVersionByDdlId(ddlId))

        sync.applyHabitSnapshotToLocal(
            habitSnapshotOf(
                uid = uid,
                ver = carrierVer,
                deleted = false
            )
        )

        assertNotNull(db.getHabitByDdlId(ddlId))
        assertEquals(carrierVer, db.getHabitAppliedVersionByDdlId(ddlId))
    }

    @Test
    fun tombstoneApplyWritesHabitAppliedVersion() {
        val ddlId = db.insertDDL(
            name = "carrier",
            startTime = "2026-03-24T08:00:00",
            endTime = "2026-03-24T18:00:00",
            type = DeadlineType.HABIT
        )
        val repo = HabitRepository(db, sync, autoScheduleSync = false)
        repo.createHabitForDdl(ddlId, "habit", HabitPeriod.DAILY)
        val uid = db.getDdlUidById(ddlId)!!
        val tombstoneVer = db.nextVersionUTC()

        sync.applyHabitSnapshotToLocal(
            habitSnapshotOf(
                uid = uid,
                ver = tombstoneVer,
                deleted = true
            )
        )

        assertNull(db.getHabitByDdlId(ddlId))
        assertEquals(tombstoneVer, db.getHabitAppliedVersionByDdlId(ddlId))
    }

    @Test
    fun localBuildSkipsRemoteCarrierWithoutHabitEntity() {
        val ddlId = db.insertDDL(
            name = "remote carrier",
            startTime = "2026-03-24T08:00:00",
            endTime = "2026-03-24T18:00:00",
            type = DeadlineType.HABIT
        )
        val uid = db.getDdlUidById(ddlId)!!
        val remoteVer = Ver("2026-03-24T12:00:00.123456Z", 0, "REMOTE01")
        db.setDdlVersionById(ddlId, remoteVer)

        val localSnapshot = sync.buildLocalHabitSnapshotV2()
        val uids = localSnapshot.getAsJsonArray("items").map { it.asJsonObject["uid"].asString }

        assertFalse(uids.contains(uid))
    }

    @Test
    fun localPayloadMutationEmitsActiveHabitDocWithFreshVersion() {
        val ddlId = db.insertDDL(
            name = "habit carrier",
            startTime = "2026-03-24T08:00:00",
            endTime = "2026-03-24T18:00:00",
            type = DeadlineType.HABIT
        )
        val repo = HabitRepository(db, sync, autoScheduleSync = false)
        repo.createHabitForDdl(ddlId, "habit", HabitPeriod.DAILY)
        val habit = db.getHabitByDdlId(ddlId)!!
        val habitId = db.getHabitIdByDdlId(ddlId)!!
        val beforeUpdatedAt = habit.updatedAt
        val beforeVer = db.getDdlVersionById(ddlId)!!

        repo.insertRecord(habitId, java.time.LocalDate.parse("2026-03-24"))

        val afterHabit = db.getHabitByDdlId(ddlId)!!
        val afterVer = db.getDdlVersionById(ddlId)!!
        assertTrue(afterHabit.updatedAt.isAfter(beforeUpdatedAt))
        assertNotEquals(beforeVer, afterVer)

        val snapshot = sync.buildLocalHabitSnapshotV2()
        val item = snapshot.getAsJsonArray("items")
            .map { it.asJsonObject }
            .firstOrNull { it["uid"].asString == db.getDdlUidById(ddlId)!! }
        assertNotNull(item)
        val payloadUpdatedAt = LocalDateTime.parse(
            item!!.getAsJsonObject("doc").getAsJsonObject("habit")["updated_at"].asString
        )
        assertEquals(parseVersionTs(afterVer.ts), payloadUpdatedAt)
    }

    @Test
    fun buildSkipsStaleActiveHabitWhenCarrierIsAheadOfPayload() {
        val ddlId = db.insertDDL(
            name = "half synced carrier",
            startTime = "2026-03-24T08:00:00",
            endTime = "2026-03-24T18:00:00",
            type = DeadlineType.HABIT
        )
        val repo = HabitRepository(db, sync, autoScheduleSync = false)
        repo.createHabitForDdl(ddlId, "habit", HabitPeriod.DAILY)
        val uid = db.getDdlUidById(ddlId)!!
        val oldCarrierVer = db.getDdlVersionById(ddlId)!!
        db.setHabitAppliedVersionByDdlId(ddlId, oldCarrierVer)
        val remoteAheadVer = Ver(
            Instant.parse(oldCarrierVer.ts).plusSeconds(3600).toString(),
            0,
            "REMOTE01"
        )
        db.setDdlVersionById(ddlId, remoteAheadVer)

        val localSnapshot = sync.buildLocalHabitSnapshotV2()
        val uids = localSnapshot.getAsJsonArray("items").map { it.asJsonObject["uid"].asString }

        assertFalse(uids.contains(uid))
    }

    @Test
    fun localMissingHabitEmitsTombstoneOnlyForLocalCarrierVersion() {
        val ddlId = db.insertDDL(
            name = "local tombstone carrier",
            startTime = "2026-03-24T08:00:00",
            endTime = "2026-03-24T18:00:00",
            type = DeadlineType.HABIT
        )
        val uid = db.getDdlUidById(ddlId)!!

        val localSnapshot = sync.buildLocalHabitSnapshotV2()
        val item = localSnapshot.getAsJsonArray("items")
            .map { it.asJsonObject }
            .firstOrNull { it["uid"].asString == uid }

        assertNotNull(item)
        assertTrue(item!!["deleted"].asBoolean)
    }

    @Test
    fun habitApplyDecisionAndBuildRulesMatchSpec() {
        val incoming = Ver("2026-03-24T12:00:00.100000Z", 0, "A")
        val applied = Ver("2026-03-24T12:00:00.100000Z", 0, "A")

        assertEquals(
            HabitApplyDecision.APPLY,
            HabitSyncRules.applyDecision(
                carrierExists = true,
                deleted = false,
                appliedVer = null,
                incomingVer = incoming
            )
        )
        assertEquals(
            HabitApplyDecision.SKIP_ALREADY_APPLIED,
            HabitSyncRules.applyDecision(
                carrierExists = true,
                deleted = false,
                appliedVer = applied,
                incomingVer = incoming
            )
        )
        assertEquals(
            LocalHabitBuildDecision.SKIP_MISSING_REMOTE_HABIT,
            HabitSyncRules.localBuildDecision(
                carrierDeleted = false,
                habitExists = false,
                carrierVer = incoming,
                carrierVerDev = "REMOTE",
                localDeviceId = "LOCAL",
                appliedVer = null,
                habitUpdatedAt = null
            )
        )
        assertEquals(
            LocalHabitBuildDecision.SKIP_STALE_ACTIVE,
            HabitSyncRules.localBuildDecision(
                carrierDeleted = false,
                habitExists = true,
                carrierVer = Ver("2026-03-24T12:01:00Z", 0, "REMOTE"),
                carrierVerDev = "REMOTE",
                localDeviceId = "LOCAL",
                appliedVer = Ver("2026-03-24T12:00:00Z", 0, "REMOTE"),
                habitUpdatedAt = LocalDateTime.parse("2026-03-24T12:00:00")
            )
        )
    }

    private fun parseVersionTs(ts: String): LocalDateTime {
        return runCatching { LocalDateTime.parse(ts) }
            .getOrElse { OffsetDateTime.parse(ts).toLocalDateTime() }
    }

    private fun habitSnapshotOf(uid: String, ver: Ver, deleted: Boolean): JsonObject {
        val item = JsonObject().apply {
            addProperty("uid", uid)
            add("ver", JsonObject().apply {
                addProperty("ts", ver.ts)
                addProperty("ctr", ver.ctr)
                addProperty("dev", ver.dev)
            })
            addProperty("deleted", deleted)
        }
        if (deleted) {
            item.add("doc", JsonNull.INSTANCE)
        } else {
            item.add("doc", JsonObject().apply {
                addProperty("ddl_uid", uid)
                add("habit", JsonObject().apply {
                    addProperty("name", "Remote habit")
                    addProperty("description", "")
                    add("color", JsonNull.INSTANCE)
                    add("icon_key", JsonNull.INSTANCE)
                    addProperty("period", "DAILY")
                    addProperty("times_per_period", 1)
                    addProperty("goal_type", "PER_PERIOD")
                    add("total_target", JsonNull.INSTANCE)
                    addProperty("created_at", "2026-03-24T08:00:00Z")
                    addProperty("updated_at", "2026-03-24T08:00:00Z")
                    addProperty("status", "ACTIVE")
                    addProperty("sort_order", 0)
                    add("alarm_time", JsonNull.INSTANCE)
                })
                add("records", JsonArray())
            })
        }
        return JsonObject().apply {
            add("version", JsonObject().apply {
                addProperty("ts", "2026-03-24T12:00:00Z")
                addProperty("dev", "REMOTE")
            })
            add("items", JsonArray().apply { add(item) })
        }
    }
}
