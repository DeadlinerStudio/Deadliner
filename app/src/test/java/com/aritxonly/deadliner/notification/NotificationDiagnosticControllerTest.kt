package com.aritxonly.deadliner.notification

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.logging.AppLog
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class NotificationDiagnosticControllerTest {
    private lateinit var context: Application
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        GlobalUtils.init(context)
        AppLog.initialize(context)
        NotificationUtil.createNotificationChannels(context)
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationDiagnosticController.stop(context)
    }
    @After fun cleanup() { NotificationDiagnosticController.stop(context) }

    @Test fun stopRemovesTheActualAlarmAndInvalidatesDelivery() {
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
        NotificationDiagnosticController.start(context, throughAlarm = true)
        val run = NotificationDiagnosticController.currentRun(context)!!
        assertEquals(NotificationTestPhase.SCHEDULED, NotificationDiagnosticController.state.value.phase)
        assertEquals(1, alarms.scheduledAlarms.size)
        NotificationDiagnosticController.stop(context)
        assertTrue(alarms.scheduledAlarms.isEmpty())
        NotificationDiagnosticReceiver().onReceive(context, Intent().putExtra(NotificationDiagnosticController.EXTRA_RUN, run))
        assertNull(shadowOf(context).nextStartedService)
        assertEquals(NotificationTestPhase.IDLE, NotificationDiagnosticController.state.value.phase)
    }

    @Test fun replacingTestDoesNotReviveThePreviousRun() {
        NotificationDiagnosticController.start(context, throughAlarm = true)
        val oldRun = NotificationDiagnosticController.currentRun(context)!!
        NotificationDiagnosticController.start(context, throughAlarm = true)
        val newRun = NotificationDiagnosticController.currentRun(context)!!
        assertNotEquals(oldRun, newRun)
        assertEquals(1, shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.size)
        NotificationDiagnosticController.launch(context, oldRun)
        assertNull(shadowOf(context).nextStartedService)
        assertEquals(newRun, NotificationDiagnosticController.state.value.run)
    }

    @Test fun nativeAndKmpRecordsSurviveRepeatedInitializationAndTextExport() {
        com.deadliner.platform.PlatformLogSink.configure(context.filesDir.absolutePath, 123L)
        NotificationDiagnostics.event("test.native_marker", "uid" to "native-marker")
        com.deadliner.platform.PlatformLogger().i("notification.test.kmp_marker", "uid=core-marker")
        AppLog.initialize(context)
        val text = AppLog.exportFiles().first { it.extension == "log" }.readText()
        assertTrue(text.contains("[Native][notification.test.native_marker] uid=native-marker"))
        assertTrue(text.contains("[KMP][notification.test.kmp_marker] uid=core-marker"))
    }

    @Test fun denialIsRecordedInExportedStandardText() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationDiagnosticController.start(context, throughAlarm = false)
        assertEquals(NotificationTestPhase.BLOCKED, NotificationDiagnosticController.state.value.phase)
        val text = AppLog.exportFiles().first { it.extension == "log" }.readText()
        assertTrue(text.contains("[Native][notification.test.blocked]"))
        assertTrue(text.contains("reason=post_permission_denied"))
        assertTrue(text.contains("run=${NotificationDiagnosticController.currentRun(context)}"))
        assertNull(shadowOf(context).nextStartedService)
    }
}
