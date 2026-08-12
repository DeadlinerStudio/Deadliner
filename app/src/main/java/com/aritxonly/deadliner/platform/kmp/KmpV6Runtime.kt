package com.aritxonly.deadliner.platform.kmp

import android.content.Context
import com.aritxonly.deadliner.data.DatabaseHelper
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import com.deadliner.viewmodel.habit.HabitListViewModel
import com.deadliner.viewmodel.capture.CaptureViewModel
import com.deadliner.viewmodel.overview.OverviewViewModel
import com.deadliner.viewmodel.task.ArchiveViewModel

/** Android-private composition root for the v6 shared layer. */
internal object KmpV6Runtime {
    lateinit var database: DeadlinerDatabase
        private set

    val overviewViewModel: OverviewViewModel by lazy {
        check(::database.isInitialized) { "KmpV6Runtime is not initialized" }
        OverviewViewModel(database.tasks, database.habits)
    }

    val captureViewModel: CaptureViewModel by lazy {
        check(::database.isInitialized) { "KmpV6Runtime is not initialized" }
        CaptureViewModel(database.captures)
    }

    val archiveViewModel: ArchiveViewModel by lazy {
        check(::database.isInitialized) { "KmpV6Runtime is not initialized" }
        ArchiveViewModel(database.tasks)
    }

    val habitListViewModel: HabitListViewModel by lazy {
        check(::database.isInitialized) { "KmpV6Runtime is not initialized" }
        HabitListViewModel(database.habits)
    }

    fun initialize(
        context: Context,
        legacyDatabase: DatabaseHelper,
    ) {
        if (::database.isInitialized) return

        database = DeadlinerDatabase.create(
            AndroidDatabaseDriverFactory(context.applicationContext)
        )

        V5DataMigrator(
            legacy = legacyDatabase,
            target = database,
            preferences = context.getSharedPreferences(
                V5DataMigrator.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            ),
            legacyCaptureJson = context.getSharedPreferences(
                "capture_inspiration",
                Context.MODE_PRIVATE,
            ).getString("items_json", null),
        ).migrateIfNeeded()
    }
}
