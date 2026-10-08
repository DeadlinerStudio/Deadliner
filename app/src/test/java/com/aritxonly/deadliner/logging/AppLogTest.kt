package com.aritxonly.deadliner.logging

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.deadliner.platform.PlatformLogSink
import com.deadliner.platform.PlatformLogger
import com.google.gson.Gson
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppLogTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val directory get() = File(context.filesDir, "Diagnostics")

    @Before fun resetLoggerState() {
        val writer = AppLog::class.java.getDeclaredField("writer").apply { isAccessible = true }
            .get(AppLog) as ExecutorService
        writer.submit {}.get(5, TimeUnit.SECONDS)
        AppLog::class.java.getDeclaredField("directory").apply { isAccessible = true }.set(AppLog, null)
        AppLog::class.java.getDeclaredField("lastPrunedAtMillis").apply { isAccessible = true }.setLong(AppLog, 0L)
        PlatformLogSink.configureFileWriter(null)
        directory.deleteRecursively()
    }

    @Test fun startupAndNotificationLoggingReturnWhileDiskWriterIsBlocked() {
        val writer = AppLog::class.java.getDeclaredField("writer").apply { isAccessible = true }
            .get(AppLog) as ExecutorService
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        writer.execute { entered.countDown(); release.await() }
        assertTrue(entered.await(2, TimeUnit.SECONDS))
        val caller = Executors.newSingleThreadExecutor()
        try {
            caller.submit {
                AppLog.initialize(context)
                PlatformLogSink.configure(context.filesDir.absolutePath, 123)
                repeat(100) {
                    AppLog.event("notification.cancel", "notification", context = mapOf("uid" to "$it"))
                    PlatformLogger().i("notification.kmp", "uid=$it")
                }
            }.get(2, TimeUnit.SECONDS)
            assertFalse(File(directory, "deadliner-diagnostics.jsonl").exists())
        } finally {
            release.countDown()
            caller.shutdownNow()
        }
        val text = AppLog.exportFiles().first { it.extension == "log" }.readText()
        assertEquals(100, text.lineSequence().count { "[Native][notification.cancel]" in it })
        assertEquals(100, text.lineSequence().count { "[KMP][notification.kmp]" in it })
    }

    @Test fun oversizedHistoryLeavesHeadroomAndPreservesNewestValidRecords() {
        directory.mkdirs()
        val json = File(directory, "deadliner-diagnostics.jsonl")
        val gson = Gson()
        val now = System.currentTimeMillis() / 1000.0
        json.bufferedWriter().use { out ->
            out.appendLine("invalid-json")
            repeat(180) { index ->
                out.appendLine(gson.toJson(mapOf(
                    "timestamp" to now, "level" to "info", "category" to "history",
                    "message" to "record-$index", "context" to mapOf("payload" to "x".repeat(60_000)),
                    "source" to if (index % 2 == 0) "KMP" else "Native",
                )))
            }
        }
        assertTrue(json.length() > 8 * 1024 * 1024)
        AppLog.initialize(context)
        AppLog.exportFiles()
        assertTrue(json.length() < 6 * 1024 * 1024 + 2048)
        repeat(500) { AppLog.event("notification.cancel", "notification", context = mapOf("uid" to "$it")) }
        val exported = AppLog.exportFiles()
        val records = exported.first { it.extension == "jsonl" }.readLines()
        assertTrue(records.any { "record-179" in it })
        assertFalse(records.any { "record-0\"" in it || "invalid-json" in it })
        assertEquals(500, records.count { "[notification.cancel]" in it })
        assertTrue(json.length() < 8 * 1024 * 1024)
        val text = exported.first { it.extension == "log" }.readText()
        assertTrue("[KMP][history] record-178" in text)
        assertTrue("[Native][history] record-179" in text)
    }

    @Test fun exportIsAStableSnapshotAndExpiredHistoryIsRemoved() {
        directory.mkdirs()
        File(directory, "deadliner-diagnostics.jsonl").writeText(Gson().toJson(mapOf(
            "timestamp" to (System.currentTimeMillis() / 1000.0 - 15 * 24 * 60 * 60),
            "level" to "info", "category" to "history", "message" to "expired",
        )) + "\n")
        AppLog.initialize(context)
        AppLog.event("before_export", "test")
        val snapshot = AppLog.exportFiles().first { it.extension == "log" }
        val firstText = snapshot.readText()
        AppLog.event("after_export", "test")
        val secondText = AppLog.exportFiles().first { it.extension == "log" }.readText()
        assertFalse(firstText.contains("expired"))
        assertTrue(firstText.contains("[before_export]"))
        assertTrue(secondText.contains("[after_export]"))
        assertEquals(firstText, snapshot.readText())
    }
}
