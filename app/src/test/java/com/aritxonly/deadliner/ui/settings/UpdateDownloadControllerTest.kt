package com.aritxonly.deadliner.ui.settings

import android.app.Application
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class UpdateDownloadControllerTest {
    @Test fun repeatedStartIsIgnoredAndCompletionInstallsOnlyOnce() = runBlocking {
        val file = File("update.apk")
        val completed = CompletableDeferred<File>()
        var downloads = 0
        var installations = 0
        val controller = UpdateDownloadController(this, { _, progress ->
            downloads++; progress(25, 100); progress(100, 100); completed.await()
        }, { installations++; true })
        controller.start("https://example.invalid/update.apk")
        yield()
        controller.start("https://example.invalid/duplicate.apk")
        assertEquals(1, downloads)
        assertEquals(0, installations)
        completed.complete(file)
        yield()
        assertNull(controller.state)
        assertEquals(1, installations)
        controller.install()
        assertEquals(1, installations)
    }

    @Test fun failureRetryUsesSameUrlAndPermissionRecoveryDoesNotRedownload() = runBlocking {
        val urls = mutableListOf<String>()
        var allowed = false
        var installs = 0
        val controller = UpdateDownloadController(this, { url, _ ->
            urls += url
            if (urls.size == 1) throw IOException("failed")
            File("update.apk")
        }, { installs++; allowed })
        controller.start("https://example.invalid/update.apk")
        yield()
        assertEquals(UpdateDownloadState.Failed, controller.state)
        controller.retry()
        yield()
        assertTrue((controller.state as UpdateDownloadState.Ready).permissionRequired)
        assertEquals(1, installs)
        allowed = true
        controller.install()
        assertNull(controller.state)
        assertEquals(2, installs)
        assertEquals(List(2) { "https://example.invalid/update.apk" }, urls)
    }

    @Test fun cancellationWaitsForCleanupAndRejectsLateProgressAndInstallation() = runBlocking {
        val cleanupStarted = CompletableDeferred<Unit>()
        val releaseCleanup = CompletableDeferred<Unit>()
        val urls = mutableListOf<String>()
        var installs = 0
        val controller = UpdateDownloadController(this, { url, progress ->
            urls += url
            if (urls.size == 1) {
                try { awaitCancellation() } finally {
                    withContext(NonCancellable) { cleanupStarted.complete(Unit); releaseCleanup.await() }
                }
            }
            progress(5, 10)
            awaitCancellation()
        }, { installs++; true })
        controller.start("first")
        yield()
        controller.cancel()
        cleanupStarted.await()
        assertNull(controller.state)
        controller.start("second")
        yield()
        assertEquals(listOf("first"), urls)
        releaseCleanup.complete(Unit)
        yield(); yield()
        assertEquals(listOf("first", "second"), urls)
        controller.cancel()
        yield()
        assertNull(controller.state)
        assertEquals(0, installs)
    }

    @Test fun installerFailureRetainsDownloadedFileAndRetryOnlyReopensInstaller() = runBlocking {
        var downloads = 0
        var calls = 0
        val controller = UpdateDownloadController(this, { _, _ -> downloads++; File("ready.apk") }, {
            if (++calls == 1) throw IllegalStateException("no installer") else true
        })
        controller.start("url")
        yield()
        assertTrue((controller.state as UpdateDownloadState.Ready).installFailed)
        controller.install()
        assertNull(controller.state)
        assertEquals(1, downloads)
        assertEquals(2, calls)
    }

    @Test fun leavingPageCancelsPendingDownloadWithoutInstalling() = runBlocking {
        val scope = CoroutineScope(coroutineContext + Job())
        var cleaned = false
        var installs = 0
        val controller = UpdateDownloadController(scope, { _, _ ->
            try { awaitCancellation() } finally { cleaned = true }
        }, { installs++; true })
        controller.start("url")
        yield()
        scope.cancel()
        yield()
        assertTrue(cleaned)
        assertEquals(0, installs)
    }
}
