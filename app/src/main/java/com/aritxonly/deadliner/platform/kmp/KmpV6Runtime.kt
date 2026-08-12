package com.aritxonly.deadliner.platform.kmp

import android.content.Context
import com.aritxonly.deadliner.data.DatabaseHelper
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import com.deadliner.viewmodel.overview.OverviewViewModel

/** Android-private composition root for the v6 shared layer. */
internal object KmpV6Runtime {
    lateinit var database: DeadlinerDatabase
        private set

    val overviewViewModel: OverviewViewModel by lazy {
        check(::database.isInitialized) { "KmpV6Runtime is not initialized" }
        OverviewViewModel(database.tasks, database.habits)
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
        ).migrateIfNeeded()
    }
}
