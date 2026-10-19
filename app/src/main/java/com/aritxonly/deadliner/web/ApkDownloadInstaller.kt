package com.aritxonly.deadliner.web

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.aritxonly.deadliner.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException

/** Android transport/installer only. All progress and recovery UI belongs to Compose. */
class ApkDownloaderInstaller(context: Context, private val io: CoroutineDispatcher = Dispatchers.IO) {
    private companion object { val downloads = Mutex() }
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    suspend fun download(apkUrl: String, onProgress: suspend (Long, Long) -> Unit): File = withContext(io) {
        downloads.withLock {
            val uri = Uri.parse(apkUrl)
            require(uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank())
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw IOException("Download directory unavailable")
            val apk = File(directory, "update.apk")
            if (apk.exists() && !apk.delete()) throw IOException("Cannot replace old update")
            var id = -1L
            var completed = false
            try {
                currentCoroutineContext().ensureActive()
                id = manager.enqueue(DownloadManager.Request(uri).apply {
                    setTitle(context.getString(R.string.downloading_update))
                    setDescription(context.getString(R.string.please_wait))
                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, apk.name)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setMimeType("application/vnd.android.package-archive")
                    setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                })
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val status = manager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                        if (!cursor.moveToFirst()) throw IOException("Download no longer exists")
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        onProgress(bytes.coerceAtLeast(0), total)
                        status
                    } ?: throw IOException("Download unavailable")
                    when (status) {
                        DownloadManager.STATUS_FAILED -> throw IOException("Download failed")
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            if (!apk.isFile || apk.length() == 0L) throw IOException("Downloaded package is missing or empty")
                            currentCoroutineContext().ensureActive()
                            completed = true
                            return@withContext apk
                        }
                    }
                    delay(500)
                }
                @Suppress("UNREACHABLE_CODE") apk
            } finally {
                if (!completed) withContext(NonCancellable + io) {
                    try { if (id >= 0) manager.remove(id) } finally { apk.delete() }
                }
            }
        }
    }

    /** False means the app-specific installation permission page was opened. */
    fun install(apk: File): Boolean {
        require(apk.isFile && apk.length() > 0) { "Update package is missing" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        })
        return true
    }
}
