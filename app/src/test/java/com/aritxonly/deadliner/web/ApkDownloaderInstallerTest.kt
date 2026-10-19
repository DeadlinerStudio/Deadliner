package com.aritxonly.deadliner.web

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDownloadManager.ShadowRequest
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ApkDownloaderInstallerTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val manager get() = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val apk get() = File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
    private fun request(): ShadowRequest = Shadow.extract(shadowOf(manager).getRequest(0))

    @Test fun successfulDownloadUsesLongCountersAndReturnsOnlyAnExistingNonemptyFile() = runBlocking {
        val downloader = ApkDownloaderInstaller(app, Dispatchers.Unconfined)
        val progress = mutableListOf<Long>()
        val job = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(2500) { downloader.download("https://example.invalid/update.apk") { bytes, _ -> progress += bytes } }
        }
        val shadow = request()
        shadow.setBytesSoFar(3_000_000_000L); shadow.setTotalSize(4_000_000_000L)
        shadow.setStatus(DownloadManager.STATUS_SUCCESSFUL)
        apk.writeText("APK fixture")
        assertEquals(apk, job.await())
        assertTrue(progress.contains(3_000_000_000L))
        assertEquals(1, shadowOf(manager).requestCount)
    }

    @Test fun cancellingRemovesDownloadAndPartialPackage() = runBlocking {
        val downloader = ApkDownloaderInstaller(app, Dispatchers.Unconfined)
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            downloader.download("https://example.invalid/update.apk") { _, _ -> }
        }
        apk.writeText("partial")
        job.cancelAndJoin()
        assertFalse(apk.exists())
        assertNull(shadowOf(manager).getRequest(0))
    }

    @Test fun failedDownloadCannotReachInstallation() = runBlocking {
        val downloader = ApkDownloaderInstaller(app, Dispatchers.Unconfined)
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { withTimeout(2500) { downloader.download("https://example.invalid/update.apk") { _, _ -> } } }
        }
        request().setStatus(DownloadManager.STATUS_FAILED)
        assertTrue(result.await().exceptionOrNull() is IOException)
        assertFalse(apk.exists())
        assertNull(shadowOf(manager).getRequest(0))
    }

    @Test fun successStatusWithEmptyPackageIsRejectedAndCleanedUp() = runBlocking {
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { withTimeout(2500) {
                ApkDownloaderInstaller(app, Dispatchers.Unconfined).download("https://example.invalid/update.apk") { _, _ -> }
            } }
        }
        apk.writeText("")
        request().setStatus(DownloadManager.STATUS_SUCCESSFUL)
        assertTrue(result.await().exceptionOrNull() is IOException)
        assertFalse(apk.exists())
        assertNull(shadowOf(manager).getRequest(0))
    }

    @Test fun externallyRemovedDownloadStopsPollingInsteadOfWaitingForever() = runBlocking {
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { withTimeout(2500) {
                ApkDownloaderInstaller(app, Dispatchers.Unconfined).download("https://example.invalid/update.apk") { _, _ -> }
            } }
        }
        manager.remove(0)
        assertTrue(result.await().exceptionOrNull() is IOException)
        assertFalse(apk.exists())
    }

    @Test fun permittedInstallUsesReadGrantedContentUriForTheSystemInstaller() {
        apk.writeText("APK fixture")
        shadowOf(app.packageManager).setCanRequestPackageInstalls(true)
        assertTrue(ApkDownloaderInstaller(app).install(apk))
        val intent = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertEquals("content", intent.data!!.scheme)
        assertEquals("${app.packageName}.fileprovider", intent.data!!.authority)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test fun missingInstallationPermissionOpensThisAppsSettingsWithoutStartingInstaller() {
        apk.writeText("APK fixture")
        shadowOf(app.packageManager).setCanRequestPackageInstalls(false)
        assertFalse(ApkDownloaderInstaller(app).install(apk))
        val intent = shadowOf(app).nextStartedActivity
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, intent.action)
        assertEquals("package:${app.packageName}", intent.data.toString())
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test fun reenteringPageWaitsForPreviousDownloadCleanupAcrossInstallerInstances() = runBlocking {
        val first = launch(start = CoroutineStart.UNDISPATCHED) {
            ApkDownloaderInstaller(app, Dispatchers.Unconfined).download("https://example.invalid/first.apk") { _, _ -> }
        }
        apk.writeText("old partial")
        val second = launch(start = CoroutineStart.UNDISPATCHED) {
            ApkDownloaderInstaller(app, Dispatchers.Unconfined).download("https://example.invalid/second.apk") { _, _ -> }
        }
        assertNotNull(shadowOf(manager).getRequest(0))
        assertNull(shadowOf(manager).getRequest(1))
        first.cancelAndJoin()
        yield()
        assertNull(shadowOf(manager).getRequest(0))
        assertNotNull(shadowOf(manager).getRequest(1))
        second.cancelAndJoin()
        assertFalse(apk.exists())
    }
}
