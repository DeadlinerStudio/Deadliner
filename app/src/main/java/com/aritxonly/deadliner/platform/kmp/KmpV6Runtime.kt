package com.aritxonly.deadliner.platform.kmp

import android.content.Context
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.CloudSyncProvider
import com.aritxonly.deadliner.localutils.CloudSyncPreferences
import com.aritxonly.deadliner.notification.AndroidNotificationGateway
import com.deadliner.db.AndroidDatabaseDriverFactory
import com.deadliner.db.DeadlinerDatabase
import com.deadliner.viewmodel.habit.HabitListViewModel
import com.deadliner.viewmodel.habit.HabitEditorViewModel
import com.deadliner.viewmodel.capture.CaptureViewModel
import com.deadliner.viewmodel.overview.OverviewViewModel
import com.deadliner.viewmodel.settings.SettingsViewModel
import com.deadliner.viewmodel.task.ArchiveViewModel
import com.deadliner.viewmodel.task.TaskListViewModel
import com.deadliner.viewmodel.task.TaskDetailViewModel
import com.deadliner.viewmodel.task.TaskEditorViewModel
import com.deadliner.viewmodel.search.SearchViewModel
import com.deadliner.domain.habit.Habit
import com.deadliner.domain.task.Task
import com.deadliner.service.LifiToolExecutor
import com.deadliner.service.NotificationOrchestrator
import com.deadliner.platform.DailySummaryConfig
import com.deadliner.domain.ai.model.AIConfig
import com.deadliner.interfaces.DeadlinerCore
import com.deadliner.lifi.LifiAIComponent
import com.deadliner.lifi.memory.KmpMemoryStoreAdapter
import com.deadliner.sync.WebDavChangeLogSyncOutcome
import com.deadliner.sync.webdav.WebDavCredentials
import com.deadliner.sync.webdav.WebDavTransportConfiguration
import com.deadliner.platform.WidgetUpdateReason
import com.deadliner.platform.updateWidget
import com.deadliner.sync.server.engine.DeadlinerServerSyncConfiguration
import com.deadliner.sync.server.transport.DeadlinerServerTransportConfiguration
import com.deadliner.sync.server.transport.KtorDeadlinerServerTransport
import kotlinx.coroutines.runBlocking

/** Android-private composition root for the v6 shared layer. */
internal object KmpV6Runtime {
    lateinit var database: DeadlinerDatabase
        private set

    private lateinit var notificationGateway: AndroidNotificationGateway

    private val notificationOrchestrator by lazy {
        check(::database.isInitialized && ::notificationGateway.isInitialized) { "KmpV6Runtime is not initialized" }
        NotificationOrchestrator(notificationGateway)
    }

    private val lifiToolExecutor by lazy {
        LifiToolExecutor(
            tasks = database.tasks,
            habits = database.habits,
            notificationOrchestrator = notificationOrchestrator,
        )
    }

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
        ArchiveViewModel(database.tasks, notificationOrchestrator = notificationOrchestrator)
    }

    val taskListViewModel: TaskListViewModel by lazy {
        check(::database.isInitialized) { "KmpV6Runtime is not initialized" }
        TaskListViewModel(database.tasks, notificationOrchestrator = notificationOrchestrator)
    }

    val habitListViewModel: HabitListViewModel by lazy {
        check(::database.isInitialized) { "KmpV6Runtime is not initialized" }
        HabitListViewModel(database.habits, notificationOrchestrator = notificationOrchestrator)
    }

    val settingsViewModel: SettingsViewModel by lazy {
        check(::database.isInitialized) { "KmpV6Runtime is not initialized" }
        SettingsViewModel(database.profiles)
    }

    val searchViewModel: SearchViewModel by lazy {
        check(::database.isInitialized) { "KmpV6Runtime is not initialized" }
        SearchViewModel(database.tasks, database.habits)
    }

    fun createTaskEditorViewModel(editingUid: String? = null): TaskEditorViewModel =
        TaskEditorViewModel(
            taskRepository = database.tasks,
            categoryRepository = database.categories,
            editingTaskUid = editingUid,
            notificationOrchestrator = notificationOrchestrator,
        )

    fun createHabitEditorViewModel(editingUid: String? = null): HabitEditorViewModel =
        HabitEditorViewModel(
            habitRepository = database.habits,
            categoryRepository = database.categories,
            editingHabitUid = editingUid,
            notificationOrchestrator = notificationOrchestrator,
        )

    fun createTaskDetailViewModel(uid: String): TaskDetailViewModel =
        TaskDetailViewModel(
            taskRepository = database.tasks,
            taskUid = uid,
            notificationOrchestrator = notificationOrchestrator,
        )

    fun updateTaskCategory(task: Task, categoryUid: String?) {
        val updated = task.copy(categoryUid = categoryUid, updatedAt = java.time.Instant.now().toString())
        database.tasks.update(updated)
        notificationOrchestrator.onTaskUpdated(updated)
    }

    fun refreshNotifications() {
        notificationOrchestrator.refreshAll(database.tasks.list(), database.habits.list())
        if (GlobalUtils.dailyStatsNotification) {
            notificationGateway.setDailySummaryNotification(
                DailySummaryConfig(
                    "%02d:%02d".format(GlobalUtils.dailyNotificationHour, GlobalUtils.dailyNotificationMinute),
                ),
            )
        }
    }

    fun cancelNotifications() {
        notificationGateway.cancelAll()
    }

    fun onHabitNotificationDelivered(habit: Habit) {
        notificationOrchestrator.onHabitUpdated(habit)
    }

    fun scheduleDailySummary() {
        notificationGateway.setDailySummaryNotification(
            DailySummaryConfig(
                "%02d:%02d".format(GlobalUtils.dailyNotificationHour, GlobalUtils.dailyNotificationMinute),
            ),
        )
    }

    fun executeLifiTool(toolName: String, argumentsJson: String): String =
        lifiToolExecutor.execute(toolName, argumentsJson)

    fun createLifiCore(config: AIConfig): DeadlinerCore {
        val component = LifiAIComponent(
            config = config,
            memoryStore = KmpMemoryStoreAdapter(database.memory),
        )
        return DeadlinerCore(config, component.orchestrator(), component.toolExecutor())
    }

    fun exportBackup(): String = database.backups.export()

    fun restoreBackup(raw: String) {
        database.disableAutomaticSync()
        try {
            database.backups.restore(raw)
            taskListViewModel.initialize()
            habitListViewModel.initialize()
            captureViewModel.initialize()
            archiveViewModel.initialize()
            overviewViewModel.initialize()
            searchViewModel.initialize()
            settingsViewModel.initialize()
            refreshNotifications()
        } finally {
            updateAutomaticSync()
        }
    }

    suspend fun syncOnce(): WebDavChangeLogSyncOutcome {
        return when (CloudSyncPreferences.provider) {
            CloudSyncProvider.WEB_DAV -> syncWebDavOnce()
            CloudSyncProvider.BYOS -> syncByosOnce()
        }
    }

    fun hasCompleteSyncConfiguration(): Boolean = when (CloudSyncPreferences.provider) {
        CloudSyncProvider.WEB_DAV -> webDavConfiguration() != null
        CloudSyncProvider.BYOS -> byosConfiguration() != null
    }

    private suspend fun syncWebDavOnce(): WebDavChangeLogSyncOutcome {
        val configuration = webDavConfiguration()
            ?: return WebDavChangeLogSyncOutcome(
                succeeded = false,
                failureMessage = "WebDAV configuration is incomplete",
            )
        return database.webDavChangeLogSync.synchronizeSafely(configuration)
    }

    private suspend fun syncByosOnce(): WebDavChangeLogSyncOutcome {
        val configuration = byosConfiguration()
            ?: return WebDavChangeLogSyncOutcome(
                succeeded = false,
                failureMessage = "BYOS configuration is incomplete",
            )
        val transport = KtorDeadlinerServerTransport(configuration)
        val engine = database.createDeadlinerServerSyncEngine(
            configuration = DeadlinerServerSyncConfiguration(
                scopeId = CloudSyncPreferences.byosScopeId,
                deviceUid = CloudSyncPreferences.byosDeviceUid,
            ),
            transport = transport,
        )
        return try {
            val outcome = engine.synchronize()
            WebDavChangeLogSyncOutcome(
                succeeded = outcome.converged,
                remoteApplied = outcome.remoteApplied,
                remoteIgnored = outcome.remoteIgnored,
                failureMessage = if (outcome.converged) null else "BYOS sync requires another retry",
            )
        } catch (error: Throwable) {
            WebDavChangeLogSyncOutcome(
                succeeded = false,
                failureMessage = "BYOS sync failed: " + (error::class.simpleName ?: "UnknownFailure"),
            )
        } finally {
            engine.close()
        }
    }

    /** Pull-to-refresh contract: remote merge first, then publish fresh KMP list snapshots. */
    suspend fun refreshMainLists(): WebDavChangeLogSyncOutcome = try {
        syncOnce()
    } finally {
        reloadMainLists()
        updateWidget(WidgetUpdateReason.USER_REFRESH)
    }

    private fun reloadMainLists() {
        taskListViewModel.loadNow()
        habitListViewModel.loadNow()
    }

    fun updateAutomaticSync() {
        val configuration = webDavConfiguration()
        if (GlobalUtils.cloudSyncEnable && CloudSyncPreferences.provider == CloudSyncProvider.WEB_DAV && configuration != null) {
            database.configureAutomaticWebDavSync(configuration) { outcome ->
                if (outcome.succeeded) reloadMainLists()
            }
        } else {
            database.disableAutomaticSync()
        }
    }

    private fun webDavConfiguration(): WebDavTransportConfiguration? {
        val baseUrl = GlobalUtils.webDavBaseUrl.trim()
        val username = GlobalUtils.webDavUser.trim()
        val password = GlobalUtils.webDavPass
        if (baseUrl.isEmpty() || username.isEmpty() || password.isEmpty()) return null
        return WebDavTransportConfiguration(
            baseUrl = baseUrl,
            credentials = WebDavCredentials(username, password),
        )
    }

    private fun byosConfiguration(): DeadlinerServerTransportConfiguration? {
        val baseUrl = CloudSyncPreferences.byosBaseUrl.trim()
        val accessToken = CloudSyncPreferences.byosAccessToken
        if (baseUrl.isEmpty() || accessToken.isEmpty()) return null
        return runCatching {
            DeadlinerServerTransportConfiguration(
                baseUrl = baseUrl,
                accessToken = accessToken,
                deviceUid = CloudSyncPreferences.byosDeviceUid,
            )
        }.getOrNull()
    }

    fun initialize(
        context: Context,
    ) {
        if (::database.isInitialized) return

        database = DeadlinerDatabase.create(
            AndroidDatabaseDriverFactory(context.applicationContext)
        )
        notificationGateway = AndroidNotificationGateway(context.applicationContext)

        migrateV5IfPresent(context.applicationContext)

        taskListViewModel.initialize()
        habitListViewModel.initialize()
        searchViewModel.initialize()
        settingsViewModel.initialize()
        refreshNotifications()
        updateAutomaticSync()
    }

    private fun migrateV5IfPresent(context: Context) {
        val preferences = context.getSharedPreferences(
            V5DataMigrator.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        if (!V5DataMigrator.isMigrationRequired(preferences)) return
        val v5DatabaseFile = context.getDatabasePath(V5MigrationReader.DATABASE_NAME)
        if (!v5DatabaseFile.exists()) {
            V5DataMigrator.markMigrationNotApplicable(preferences)
            return
        }

        V5MigrationReader.open(v5DatabaseFile).use { legacyDatabase ->
            V5DataMigrator(
                legacy = legacyDatabase,
                target = database,
                preferences = preferences,
                legacyCaptureJson = context.getSharedPreferences(
                    "capture_inspiration",
                    Context.MODE_PRIVATE,
                ).getString("items_json", null),
                legacyProfile = { runBlocking { context.readV5Profile() } },
            ).migrateIfNeeded()
        }
    }
}
