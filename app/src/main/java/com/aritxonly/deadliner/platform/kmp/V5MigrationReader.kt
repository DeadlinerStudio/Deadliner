package com.aritxonly.deadliner.platform.kmp

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.Closeable
import java.io.File

/** Read-only, one-shot importer for the frozen Android v5 schema. */
internal class V5MigrationReader private constructor(
    private val database: SQLiteDatabase,
) : Closeable {
    init {
        require(database.version == SCHEMA_VERSION) {
            "Unsupported Android v5 database schema: ${database.version}"
        }
        REQUIRED_COLUMNS.forEach { (table, required) ->
            val actual = database.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
                buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            require(actual.containsAll(required)) {
                "Android v5 database is missing required $table columns: ${required - actual}"
            }
        }
    }

    fun tasks(): List<TaskRow> = database.rawQuery(
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
        buildList {
            while (cursor.moveToNext()) {
                add(
                    TaskRow(
                        uid = cursor.requiredString(0, "DDL id=${cursor.getLong(5)} has no stable uid"),
                        deleted = cursor.getInt(1) != 0,
                        version = Version(cursor.getString(2), cursor.getInt(3), cursor.getString(4)),
                        id = cursor.getLong(5),
                        name = cursor.getString(6),
                        startTime = cursor.getString(7),
                        endTime = cursor.getString(8),
                        state = cursor.getString(9),
                        isCompleted = cursor.getInt(10) != 0,
                        isArchived = cursor.getInt(11) != 0,
                        completeTime = cursor.getString(12),
                        note = cursor.getString(13),
                        isStarred = cursor.getInt(14),
                        type = cursor.getString(15),
                        habitCount = cursor.getInt(16),
                        habitTotalCount = cursor.getInt(17),
                        calendarEvent = cursor.getLong(18),
                        timestamp = cursor.getString(19),
                        subTasksJson = cursor.getString(20),
                    ),
                )
            }
        }
    }

    fun habitCarriers(): List<HabitCarrier> = database.rawQuery(
        """
        SELECT id, uid, COALESCE(deleted, 0),
               COALESCE(ver_ts, '1970-01-01T00:00:00Z'),
               COALESCE(ver_ctr, 0), COALESCE(ver_dev, '')
          FROM ddl_items
         WHERE type = 'habit'
        """.trimIndent(),
        null,
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val ddlId = cursor.getLong(0)
                add(
                    HabitCarrier(
                        uid = cursor.requiredString(1, "Habit carrier id=$ddlId has no stable uid"),
                        deleted = cursor.getInt(2) != 0,
                        version = Version(cursor.getString(3), cursor.getInt(4), cursor.getString(5)),
                        habit = habit(ddlId),
                    ),
                )
            }
        }
    }

    fun deviceId(): String = database.rawQuery(
        "SELECT device_id FROM sync_state WHERE id = 1",
        null,
    ).use { cursor ->
        check(cursor.moveToFirst()) { "Android v5 database has no sync_state row" }
        cursor.requiredString(0, "Android v5 database has no device id")
    }

    private fun habit(ddlId: Long): Habit? = database.rawQuery(
        """
        SELECT id, name, description, color, icon_key, period, times_per_period,
               goal_type, total_target, created_at, updated_at, status, sort_order, alarm_time
          FROM habits
         WHERE ddl_id = ?
         LIMIT 1
        """.trimIndent(),
        arrayOf(ddlId.toString()),
    ).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val habitId = cursor.getLong(0)
        Habit(
            name = cursor.getString(1),
            description = cursor.nullableString(2),
            color = cursor.nullableInt(3),
            iconKey = cursor.nullableString(4),
            period = cursor.getString(5),
            timesPerPeriod = cursor.getInt(6),
            goalType = cursor.getString(7),
            totalTarget = cursor.nullableInt(8),
            createdAt = cursor.getString(9),
            updatedAt = cursor.getString(10),
            status = cursor.getString(11),
            sortOrder = cursor.getInt(12),
            alarmTime = cursor.nullableString(13),
            records = habitRecords(habitId),
        )
    }

    private fun habitRecords(habitId: Long): List<HabitRecord> = database.rawQuery(
        """
        SELECT date, count, status, created_at
          FROM habit_records
         WHERE habit_id = ?
         ORDER BY date ASC, id ASC
        """.trimIndent(),
        arrayOf(habitId.toString()),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    HabitRecord(
                        date = cursor.getString(0),
                        count = cursor.getInt(1),
                        status = cursor.getString(2),
                        createdAt = cursor.getString(3),
                    ),
                )
            }
        }
    }

    override fun close() = database.close()

    data class Version(val timestamp: String, val counter: Int, val deviceId: String)

    data class TaskRow(
        val uid: String,
        val deleted: Boolean,
        val version: Version,
        val id: Long,
        val name: String,
        val startTime: String,
        val endTime: String,
        val state: String,
        val isCompleted: Boolean,
        val isArchived: Boolean,
        val completeTime: String,
        val note: String,
        val isStarred: Int,
        val type: String,
        val habitCount: Int,
        val habitTotalCount: Int,
        val calendarEvent: Long,
        val timestamp: String,
        val subTasksJson: String,
    )

    data class HabitCarrier(
        val uid: String,
        val deleted: Boolean,
        val version: Version,
        val habit: Habit?,
    )

    data class Habit(
        val name: String,
        val description: String?,
        val color: Int?,
        val iconKey: String?,
        val period: String,
        val timesPerPeriod: Int,
        val goalType: String,
        val totalTarget: Int?,
        val createdAt: String,
        val updatedAt: String,
        val status: String,
        val sortOrder: Int,
        val alarmTime: String?,
        val records: List<HabitRecord>,
    )

    data class HabitRecord(
        val date: String,
        val count: Int,
        val status: String,
        val createdAt: String,
    )

    companion object {
        const val DATABASE_NAME = "deadliner.db"
        private const val SCHEMA_VERSION = 15

        private val REQUIRED_COLUMNS = mapOf(
            "ddl_items" to setOf(
                "id", "uid", "deleted", "ver_ts", "ver_ctr", "ver_dev", "name",
                "start_time", "end_time", "state", "is_completed", "is_archived",
                "complete_time", "note", "is_stared", "type", "habit_count",
                "habit_total_count", "calendar_event", "timestamp", "sub_tasks_json",
            ),
            "habits" to setOf(
                "id", "ddl_id", "name", "description", "color", "icon_key", "period",
                "times_per_period", "goal_type", "total_target", "created_at", "updated_at",
                "status", "sort_order", "alarm_time",
            ),
            "habit_records" to setOf("id", "habit_id", "date", "count", "status", "created_at"),
            "sync_state" to setOf("id", "device_id"),
        )

        fun open(file: File): V5MigrationReader = V5MigrationReader(
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY),
        )
    }
}

private fun Cursor.requiredString(index: Int, message: String): String =
    nullableString(index) ?: throw IllegalStateException(message)

private fun Cursor.nullableString(index: Int): String? = if (isNull(index)) null else getString(index)

private fun Cursor.nullableInt(index: Int): Int? = if (isNull(index)) null else getInt(index)
