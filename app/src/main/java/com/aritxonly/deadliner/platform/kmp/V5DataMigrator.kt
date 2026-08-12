package com.aritxonly.deadliner.platform.kmp

import android.content.SharedPreferences
import android.util.Log
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.Habit
import com.aritxonly.deadliner.model.HabitRecord
import com.deadliner.db.DeadlinerDatabase
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal class V5DataMigrator(
    private val legacy: DatabaseHelper,
    private val target: DeadlinerDatabase,
    private val preferences: SharedPreferences,
) {
    fun migrateIfNeeded() {
        if (preferences.getInt(KEY_DATA_VERSION, 0) >= DATA_VERSION) return

        val taskPayload = buildTaskSnapshot().toString()
        val habitPayload = buildCompleteHabitSnapshot().toString()
        val result = target.legacyV2Sync.mergeRemotePayloads(
            taskPayload = taskPayload,
            habitPayload = habitPayload,
            categoryPayload = null,
        )
        check(result.failureMessage == null) { result.failureMessage.orEmpty() }

        check(preferences.edit().putInt(KEY_DATA_VERSION, DATA_VERSION).commit()) {
            "Could not persist v5 -> v6 migration marker"
        }
        Log.i(TAG, "Imported v5 data into KMP: applied=${result.remoteApplied}")
    }

    private fun buildTaskSnapshot(): JsonObject {
        val items = JsonArray()
        legacy.readableDatabase.rawQuery(
            """
            SELECT uid, COALESCE(deleted, 0), COALESCE(ver_ts, '1970-01-01T00:00:00Z'),
                   COALESCE(ver_ctr, 0), COALESCE(ver_dev, ''), id, name, start_time,
                   end_time, state, is_completed, is_archived, complete_time, note,
                   is_stared, type, habit_count, habit_total_count, calendar_event,
                   timestamp, sub_tasks_json
              FROM ddl_items
            """.trimIndent(),
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val uid = cursor.getString(0)
                    ?: throw IllegalStateException("v5 DDL id=${cursor.getLong(5)} has no stable uid")
                val deleted = cursor.getInt(1) != 0
                items.add(JsonObject().apply {
                    addProperty("uid", uid)
                    add("ver", JsonObject().apply {
                        addProperty("ts", cursor.getString(2))
                        addProperty("ctr", cursor.getInt(3))
                        addProperty("dev", cursor.getString(4))
                    })
                    addProperty("deleted", deleted)
                    add("doc", if (deleted) JsonNull.INSTANCE else JsonObject().apply {
                        val state = DDLState.fromStoredValue(
                            rawState = cursor.getString(9),
                            isCompleted = cursor.getInt(10) != 0,
                            isArchived = cursor.getInt(11) != 0,
                        )
                        addProperty("id", cursor.getLong(5))
                        addProperty("name", cursor.getString(6))
                        addProperty("start_time", canonicalInstantOrBlank(cursor.getString(7)))
                        addProperty("end_time", canonicalInstantOrBlank(cursor.getString(8)))
                        addProperty("state", state.toWire())
                        addProperty("complete_time", canonicalInstantOrBlank(cursor.getString(12)))
                        addProperty("note", cursor.getString(13))
                        addProperty("is_stared", cursor.getInt(14))
                        addProperty("type", cursor.getString(15))
                        addProperty("habit_count", cursor.getInt(16))
                        addProperty("habit_total_count", cursor.getInt(17))
                        addProperty("calendar_event", cursor.getLong(18))
                        addProperty("timestamp", canonicalInstant(cursor.getString(19)))
                        add("sub_tasks", canonicalSubtasks(cursor.getString(20)))
                    })
                })
            }
        }
        return JsonObject().apply {
            add("version", JsonObject().apply {
                addProperty("ts", Instant.now().toString())
                addProperty("dev", legacy.getDeviceId())
            })
            add("items", items)
        }
    }

    private fun buildCompleteHabitSnapshot(): JsonObject {
        val items = JsonArray()
        legacy.getHabitCarrierSyncRows().forEach { carrier ->
            val habit = legacy.getHabitByDdlId(carrier.ddlId)
            items.add(JsonObject().apply {
                addProperty("uid", carrier.uid)
                add("ver", JsonObject().apply {
                    addProperty("ts", carrier.ver.ts)
                    addProperty("ctr", carrier.ver.ctr)
                    addProperty("dev", carrier.ver.dev)
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
                addProperty("dev", legacy.getDeviceId())
            })
            add("items", items)
        }
    }

    private fun habitDocument(uid: String, habit: Habit): JsonObject = JsonObject().apply {
        addProperty("ddl_uid", uid)
        add("habit", JsonObject().apply {
            addProperty("name", habit.name)
            habit.description?.let { addProperty("description", it) } ?: add("description", JsonNull.INSTANCE)
            habit.color?.let { addProperty("color", it) } ?: add("color", JsonNull.INSTANCE)
            habit.iconKey?.let { addProperty("icon_key", it) } ?: add("icon_key", JsonNull.INSTANCE)
            addProperty("period", habit.period.name)
            addProperty("times_per_period", habit.timesPerPeriod)
            addProperty("goal_type", habit.goalType.name)
            habit.totalTarget?.let { addProperty("total_target", it) } ?: add("total_target", JsonNull.INSTANCE)
            addProperty("created_at", canonicalInstant(habit.createdAt.toString()))
            addProperty("updated_at", canonicalInstant(habit.updatedAt.toString()))
            addProperty("status", habit.status.name)
            addProperty("sort_order", habit.sortOrder)
            habit.alarmTime?.let { addProperty("alarm_time", it) } ?: add("alarm_time", JsonNull.INSTANCE)
        })
        add("records", JsonArray().apply {
            legacy.getAllHabitRecordsForHabit(habit.id)
                .sortedWith(compareBy<HabitRecord> { it.date }.thenBy { it.createdAt })
                .forEach { record ->
                    add(JsonObject().apply {
                        addProperty("date", record.date.toString())
                        addProperty("count", record.count)
                        addProperty("status", record.status.name)
                        addProperty("created_at", canonicalInstant(record.createdAt.toString()))
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
        private const val DATA_VERSION = 1
        private const val TAG = "V5DataMigrator"
    }
}
