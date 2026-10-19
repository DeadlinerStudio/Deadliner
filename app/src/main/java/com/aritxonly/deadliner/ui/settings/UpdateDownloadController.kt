package com.aritxonly.deadliner.ui.settings

import androidx.compose.runtime.*
import com.aritxonly.deadliner.logging.AppLog
import kotlinx.coroutines.*
import java.io.File

internal sealed interface UpdateDownloadState {
    data class Downloading(val bytes: Long = 0, val total: Long = -1) : UpdateDownloadState
    data class Ready(val file: File, val permissionRequired: Boolean = false, val installFailed: Boolean = false) : UpdateDownloadState
    data object Failed : UpdateDownloadState
}

/** Page-owned download lifecycle. The scope cancels transport and prevents late UI/installation. */
@Stable
internal class UpdateDownloadController(
    private val scope: CoroutineScope,
    private val download: suspend (String, suspend (Long, Long) -> Unit) -> File,
    private val launchInstaller: (File) -> Boolean,
) {
    var state: UpdateDownloadState? by mutableStateOf(null)
        private set
    private var job: Job? = null
    private var requestedUrl: String? = null

    fun start(url: String) {
        if (state is UpdateDownloadState.Downloading) return
        val previous = job
        requestedUrl = url
        state = UpdateDownloadState.Downloading()
        job = scope.launch {
            previous?.cancelAndJoin() // Old cancellation must finish deleting its partial file first.
            val deliveryContext = currentCoroutineContext()
            try {
                val file = download(url) { bytes, total ->
                    withContext(deliveryContext) {
                        ensureActive()
                        state = UpdateDownloadState.Downloading(bytes, total)
                    }
                }
                ensureActive()
                state = UpdateDownloadState.Ready(file)
                install() // One completion path; no competing receiver or progress-triggered install.
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AppLog.e("UpdateDownload", "Update download failed", error)
                state = UpdateDownloadState.Failed
            }
        }
    }

    fun retry() { requestedUrl?.let(::start) }

    fun cancel() {
        job?.cancel()
        state = null
    }

    fun install() {
        val ready = state as? UpdateDownloadState.Ready ?: return
        state = try {
            if (launchInstaller(ready.file)) null else ready.copy(permissionRequired = true, installFailed = false)
        } catch (error: Exception) {
            AppLog.e("UpdateDownload", "Cannot open update installer", error)
            ready.copy(installFailed = true)
        }
    }
}
