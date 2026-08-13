package com.aritxonly.deadliner.platform.kmp

import android.content.SharedPreferences
import android.util.Log
import com.aritxonly.deadliner.model.DDLState
import com.deadliner.db.DeadlinerDatabase
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.deadliner.domain.capture.CaptureItem
import com.deadliner.domain.account.UserProfile
import com.deadliner.domain.account.UserTier
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal class V5DataMigrator(
    private val legacy: V5MigrationReader,
    private val target: DeadlinerDatabase,
    private val preferences: SharedPreferences,
    private val legacyCaptureJson: String? = null,
    private val legacyProfile: () -> LegacyProfile = { LegacyProfile() },
) {
    fun migrateIfNeeded() {
        val currentVersion = preferences.getInt(KEY_DATA_VERSION, 0)
        if (currentVersion >= DATA_VERSION) return

        if (currentVersion < CORE_DATA_VERSION) {
            migrateCoreData()
        }
        if (currentVersion < CAPTURE_DATA_VERSION) {
            migrateCaptures()
        }
        if (currentVersion < PROFILE_DATA_VERSION) {
            migrateProfile()
        }

        check(preferences.edit().putInt(KEY_DATA_VERSION, DATA_VERSION).commit()) {
            "Could not persist v5 -> v6 migration marker"
        }
        Log.i(TAG, "Imported v5 data into KMP at version=$DATA_VERSION")
    }

    private fun migrateCoreData() {
        val result = target.legacyV2Sync.mergeRemotePayloads(
            taskPayload = buildTaskSnapshot().toString(),
            habitPayload = buildCompleteHabitSnapshot().toString(),
            categoryPayload = null,
        )
        check(result.failureMessage == null) { result.failureMessage.orEmpty() }
    }

    private fun migrateCaptures() {
        val raw = legacyCaptureJson ?: return
        val type = object : TypeToken<List<LegacyCapture>>() {}.type
        val captures = Gson().fromJson<List<LegacyCapture>>(raw, type).orEmpty()
        captures.forEach { legacyCapture ->
            val uid = "legacy-capture:${legacyCapture.id}"
            if (target.captures.find(uid) == null) {
                target.captures.create(
                    CaptureItem(
                        uid = uid,
                        text = legacyCapture.text,
                        createdAt = Instant.ofEpochMilli(legacyCapture.createdAt).toString(),
                        updatedAt = Instant.ofEpochMilli(legacyCapture.updatedAt).toString(),
                    )
                )
            }
        }
    }

    private fun migrateProfile() {
        if (target.profiles.list().isNotEmpty()) return
        val now = Instant.now().toString()
        val profile = legacyProfile()
        target.profiles.create(
            UserProfile(
                uid = LEGACY_PROFILE_UID,
                nickname = profile.nickname,
                avatarFileName = profile.avatarFileName,
                tier = UserTier.FREE,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private fun buildTaskSnapshot(): JsonObject {
        val items = JsonArray()
        legacy.tasks().forEach { task ->
                val state = DDLState.fromStoredValue(
                    rawState = task.state,
                    isCompleted = task.isCompleted,
                    isArchived = task.isArchived,
                )
                items.add(JsonObject().apply {
                    addProperty("uid", task.uid)
                    add("ver", JsonObject().apply {
                        addProperty("ts", task.version.timestamp)
                        addProperty("ctr", task.version.counter)
                        addProperty("dev", task.version.deviceId)
                    })
                    addProperty("deleted", task.deleted)
                    add("doc", if (task.deleted) JsonNull.INSTANCE else JsonObject().apply {
                        addProperty("id", task.id)
                        addProperty("name", task.name)
                        addProperty("start_time", canonicalInstantOrBlank(task.startTime))
                        addProperty("end_time", canonicalInstantOrBlank(task.endTime))
                        addProperty("state", state.toWire())
                        addProperty("complete_time", canonicalInstantOrBlank(task.completeTime))
                        addProperty("note", task.note)
                        addProperty("is_stared", task.isStarred)
                        addProperty("type", task.type)
                        addProperty("habit_count", task.habitCount)
                        addProperty("habit_total_count", task.habitTotalCount)
                        addProperty("calendar_event", task.calendarEvent)
                        addProperty("timestamp", canonicalInstant(task.timestamp))
                        add("sub_tasks", canonicalSubtasks(task.subTasksJson))
                    })
                })
        }
        return JsonObject().apply {
            add("version", JsonObject().apply {
                addProperty("ts", Instant.now().toString())
                addProperty("dev", legacy.deviceId())
            })
            add("items", items)
        }
    }

    private fun buildCompleteHabitSnapshot(): JsonObject {
        val items = JsonArray()
        legacy.habitCarriers().forEach { carrier ->
            val habit = carrier.habit
            items.add(JsonObject().apply {
                addProperty("uid", carrier.uid)
                add("ver", JsonObject().apply {
                    addProperty("ts", carrier.version.timestamp)
                    addProperty("ctr", carrier.version.counter)
                    addProperty("dev", carrier.version.deviceId)
                })
                addProperty("deleted", carrier.deleted || habit == null)
                add("doc", if (carrier.deleted || habit == null) {
                    JsonNull.INSTANCE
                } else {
                    habitDocument(carrier.uid, habit)
                })
            })
        }
        return JsonObject().apply {
            add("version", JsonObject().apply {
                addProperty("ts", Instant.now().toString())
                addProperty("dev", legacy.deviceId())
            })
            add("items", items)
        }
    }

    private fun habitDocument(uid: String, habit: V5MigrationReader.Habit): JsonObject = JsonObject().apply {
        addProperty("ddl_uid", uid)
        add("habit", JsonObject().apply {
            addProperty("name", habit.name)
            habit.description?.let { addProperty("description", it) } ?: add("description", JsonNull.INSTANCE)
            habit.color?.let { addProperty("color", it) } ?: add("color", JsonNull.INSTANCE)
            habit.iconKey?.let { addProperty("icon_key", it) } ?: add("icon_key", JsonNull.INSTANCE)
            addProperty("period", habit.period)
            addProperty("times_per_period", habit.timesPerPeriod)
            addProperty("goal_type", habit.goalType)
            habit.totalTarget?.let { addProperty("total_target", it) } ?: add("total_target", JsonNull.INSTANCE)
            addProperty("created_at", canonicalInstant(habit.createdAt.toString()))
            addProperty("updated_at", canonicalInstant(habit.updatedAt.toString()))
            addProperty("status", habit.status)
            addProperty("sort_order", habit.sortOrder)
            habit.alarmTime?.let { addProperty("alarm_time", it) } ?: add("alarm_time", JsonNull.INSTANCE)
        })
        add("records", JsonArray().apply {
            habit.records
                .sortedWith(compareBy<V5MigrationReader.HabitRecord> { it.date }.thenBy { it.createdAt })
                .forEach { record ->
                    add(JsonObject().apply {
                        addProperty("date", record.date)
                        addProperty("count", record.count)
                        addProperty("status", record.status)
                        addProperty("created_at", canonicalInstant(record.createdAt))
                    })
                }
        })
    }

    private fun canonicalSubtasks(raw: String?): JsonArray {
        if (raw.isNullOrBlank()) return JsonArray()
        return JsonParser.parseString(raw).asJsonArray.also { subtasks ->
            subtasks.forEach { element ->
                val subtask = element.asJsonObject
                listOf("created_at", "updated_at", "createdAt", "updatedAt").forEach { key ->
                    val value = subtask[key]?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                    if (value.isNotBlank()) subtask.addProperty(key, canonicalInstant(value))
                }
            }
        }
    }

    private fun canonicalInstantOrBlank(raw: String?): String = raw?.takeIf { it.isNotBlank() }
        ?.let(::canonicalInstant)
        .orEmpty()

    private fun canonicalInstant(raw: String): String = runCatching { Instant.parse(raw).toString() }
        .recoverCatching {
            LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant().toString()
        }
        .getOrElse { throw IllegalArgumentException("Invalid v5 date-time: $raw", it) }

    companion object {
        const val PREFERENCES_NAME = "kmp_v6_migration"
        private const val KEY_DATA_VERSION = "data_version"
        private const val CORE_DATA_VERSION = 1
        private const val CAPTURE_DATA_VERSION = 2
        private const val PROFILE_DATA_VERSION = 3
        private const val DATA_VERSION = PROFILE_DATA_VERSION
        private const val LEGACY_PROFILE_UID = "android-v5-primary-profile"
        private const val TAG = "V5DataMigrator"

        fun isMigrationRequired(preferences: SharedPreferences): Boolean =
            preferences.getInt(KEY_DATA_VERSION, 0) < DATA_VERSION

        fun markMigrationNotApplicable(preferences: SharedPreferences) {
            check(preferences.edit().putInt(KEY_DATA_VERSION, DATA_VERSION).commit()) {
                "Could not persist v5 migration not-applicable marker"
            }
        }
    }

    private data class LegacyCapture(
        val id: Long,
        val text: String,
        val createdAt: Long,
        val updatedAt: Long,
    )

    data class LegacyProfile(
        val nickname: String = "",
        val avatarFileName: String? = null,
    )
}
