package com.aritxonly.deadliner.platform.kmp

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class V5DataMigratorTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(V5MigrationReader.DATABASE_NAME)
        context.deleteDatabase("deadliner_new_era.db")
        context.getSharedPreferences(V5DataMigrator.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        context.deleteDatabase(V5MigrationReader.DATABASE_NAME)
        context.deleteDatabase("deadliner_new_era.db")
    }

    @Test
    fun migrateIfNeeded_importsFrozenV5OnceWithoutMutatingSource() {
        createFrozenV5Database()
        val target = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))

        V5MigrationReader.open(context.getDatabasePath(V5MigrationReader.DATABASE_NAME)).use { legacy ->
            val migrator = V5DataMigrator(
                legacy = legacy,
                target = target,
                preferences = context.getSharedPreferences(
                    V5DataMigrator.PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                ),
                legacyCaptureJson =
                    """[{"id":99,"text":"old inspiration","createdAt":1723438800000,"updatedAt":1723438860000}]""",
                legacyProfile = {
                    V5DataMigrator.LegacyProfile(
                        nickname = "KMP user",
                        avatarFileName = "avatar.jpg",
                    )
                },
            )

            migrator.migrateIfNeeded()
            migrator.migrateIfNeeded()

            assertEquals("KMP task", target.tasks.find("v5-task")?.title)
            assertEquals(1, target.tasks.list().size)
            assertEquals("Read", target.habits.find("v5-habit")?.name)
            assertEquals(1, target.habits.records("v5-habit").size)
            assertEquals("old inspiration", target.captures.find("legacy-capture:99")?.text)
            assertEquals(1, target.captures.list().size)
            assertEquals("KMP user", target.profiles.list().single().nickname)
            assertEquals("avatar.jpg", target.profiles.list().single().avatarFileName)
            assertEquals(2, legacy.tasks().size)
            assertEquals(1, legacy.habitCarriers().size)
        }

        target.close()
    }

    private fun createFrozenV5Database() {
        val file = context.getDatabasePath(V5MigrationReader.DATABASE_NAME)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            database.version = 15
            database.execSQL(
                """
                CREATE TABLE ddl_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    is_completed INTEGER,
                    complete_time TEXT NOT NULL,
                    note TEXT NOT NULL,
                    is_archived INTEGER,
                    state TEXT NOT NULL,
                    is_stared INTEGER,
                    type TEXT NOT NULL,
                    habit_count INTEGER,
                    habit_total_count INTEGER,
                    calendar_event INTEGER,
                    timestamp TEXT,
                    sub_tasks_json TEXT NOT NULL,
                    uid TEXT UNIQUE,
                    deleted INTEGER NOT NULL,
                    ver_ts TEXT NOT NULL,
                    ver_ctr INTEGER NOT NULL,
                    ver_dev TEXT NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE habits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ddl_id INTEGER NOT NULL UNIQUE,
                    name TEXT NOT NULL,
                    description TEXT,
                    color INTEGER,
                    icon_key TEXT,
                    period TEXT NOT NULL,
                    times_per_period INTEGER NOT NULL,
                    goal_type TEXT NOT NULL,
                    total_target INTEGER,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    status TEXT NOT NULL,
                    sort_order INTEGER NOT NULL,
                    alarm_time TEXT
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE habit_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    habit_id INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    count INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE TABLE sync_state (id INTEGER PRIMARY KEY, device_id TEXT NOT NULL)",
            )
            database.execSQL("INSERT INTO sync_state(id, device_id) VALUES(1, 'v5-device')")
            insertDdl(database, id = 1, uid = "v5-task", name = "KMP task", type = "task")
            insertDdl(database, id = 2, uid = "v5-habit", name = "KMP habit", type = "habit")
            database.execSQL(
                """
                INSERT INTO habits(
                    id, ddl_id, name, description, color, icon_key, period, times_per_period,
                    goal_type, total_target, created_at, updated_at, status, sort_order, alarm_time
                ) VALUES(1, 2, 'Read', NULL, NULL, NULL, 'DAILY', 1, 'PER_PERIOD', NULL,
                         '2026-08-12T09:00:00', '2026-08-12T09:00:00', 'ACTIVE', 0, NULL)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO habit_records(id, habit_id, date, count, status, created_at)
                VALUES(1, 1, '2026-08-12', 1, 'COMPLETED', '2026-08-12T09:30:00')
                """.trimIndent(),
            )
        }
    }

    private fun insertDdl(
        database: SQLiteDatabase,
        id: Long,
        uid: String,
        name: String,
        type: String,
    ) {
        database.execSQL(
            """
            INSERT INTO ddl_items(
                id, name, start_time, end_time, is_completed, complete_time, note,
                is_archived, state, is_stared, type, habit_count, habit_total_count,
                calendar_event, timestamp, sub_tasks_json, uid, deleted, ver_ts, ver_ctr, ver_dev
            ) VALUES(?, ?, '2026-08-12T09:00:00', '2026-08-12T18:00:00', 0, '',
                     'one-way', 0, 'active', 0, ?, 0, 0, NULL, '2026-08-12T09:00:00',
                     '[]', ?, 0, '2026-08-12T09:00:00Z', 0, 'v5-device')
            """.trimIndent(),
            arrayOf<Any>(id, name, type, uid),
        )
    }
}
