package com.aritxonly.deadliner.platform.kmp

import android.content.Context
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.data.DatabaseHelper
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.Habit
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.HabitRecord
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class V5DataMigratorTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DatabaseHelper.closeInstance()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
        context.deleteDatabase("deadliner_new_era.db")
        context.getSharedPreferences(V5DataMigrator.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        DatabaseHelper.closeInstance()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
        context.deleteDatabase("deadliner_new_era.db")
    }

    @Test
    fun migrateIfNeeded_importsOnceAndLeavesV5DataUntouched() {
        val legacy = DatabaseHelper.getInstance(context)
        val taskId = legacy.insertDDL(
            name = "KMP task",
            startTime = "2026-08-12T09:00:00",
            endTime = "2026-08-12T18:00:00",
            note = "one-way",
        )
        val habitCarrierId = legacy.insertDDL(
            name = "KMP habit",
            startTime = "2026-08-12T09:00:00",
            endTime = "2026-09-12T09:00:00",
            type = DeadlineType.HABIT,
        )
        val habitId = legacy.insertHabit(
            Habit(
                ddlId = habitCarrierId,
                name = "Read",
                period = HabitPeriod.DAILY,
                createdAt = LocalDateTime.parse("2026-08-12T09:00:00"),
                updatedAt = LocalDateTime.parse("2026-08-12T09:00:00"),
            )
        )
        legacy.insertHabitRecord(
            HabitRecord(
                habitId = habitId,
                date = LocalDate.parse("2026-08-12"),
                createdAt = LocalDateTime.parse("2026-08-12T09:30:00"),
            )
        )

        val target = DeadlinerDatabase.create(AndroidDatabaseDriverFactory(context))
        val migrator = V5DataMigrator(
            legacy = legacy,
            target = target,
            preferences = context.getSharedPreferences(
                V5DataMigrator.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            ),
        )

        migrator.migrateIfNeeded()
        migrator.migrateIfNeeded()

        val taskUid = requireNotNull(legacy.getDdlUidById(taskId))
        val habitUid = requireNotNull(legacy.getDdlUidById(habitCarrierId))
        assertEquals("KMP task", target.tasks.find(taskUid)?.title)
        assertEquals(1, target.tasks.list().size)
        assertEquals("Read", target.habits.find(habitUid)?.name)
        assertEquals(1, target.habits.records(habitUid).size)
        assertEquals(2, legacy.getAllDDLs().size)
        assertEquals(1, legacy.getAllHabits().size)
        target.close()
    }
}
