package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.web.ApkDownloaderInstaller
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.aritxonly.deadliner.BuildConfig
import com.aritxonly.deadliner.web.UpdateInfo
import com.aritxonly.deadliner.web.UpdateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class UpdateState {
    object Loading : UpdateState()
    data class UpToDate(val current: String) : UpdateState()
    data class Available(val current: String, val latest: String, val notes: String, val downloadUrl: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

internal const val DeadlinerReleasesUrl = "https://github.com/AritxOnly/Deadliner/releases"

@Composable
fun UpdateScreen(navigateUp: () -> Unit) {
    val context = LocalContext.current
    val current = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: BuildConfig.VERSION_NAME
    }
    val scope = rememberCoroutineScope()
    val installer = remember(context) { ApkDownloaderInstaller(context) }
    val download = remember(scope, installer) { UpdateDownloadController(scope, installer::download, installer::install) }
    UpdateRoute(current, navigateUp,
        fetchUpdate = { withContext(Dispatchers.IO) { UpdateManager.fetchUpdateInfo(context) } },
        onInstall = download::start,
        onOpenBrowser = { context.startActivity(Intent(Intent.ACTION_VIEW, it.toUri())) },
        overlay = { UpdateDownloadDialog(download.state, download::cancel, download::retry, download::install) })
}

/** Own the request in the page lifecycle; a new check cancels the old producer, never its successor. */
@Composable
internal fun UpdateRoute(
    currentVersion: String,
    navigateUp: () -> Unit,
    fetchUpdate: suspend () -> UpdateInfo,
    onInstall: (String) -> Unit,
    onOpenBrowser: (String) -> Unit,
    overlay: @Composable () -> Unit = {},
) {
    var request by rememberSaveable { mutableIntStateOf(0) }
    val fetch by rememberUpdatedState(fetchUpdate)
    val state by produceState<UpdateState>(UpdateState.Loading, request) {
        value = UpdateState.Loading
        value = try {
            val info = fetch()
            if (UpdateManager.isNewer(info.currentVersion, info.latestVersion)) {
                UpdateState.Available(info.currentVersion, info.latestVersion, info.releaseNotes, info.downloadUrl)
            } else UpdateState.UpToDate(info.currentVersion)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            UpdateState.Error(error.localizedMessage.orEmpty())
        }
    }
    UpdatePage(currentVersion, state, navigateUp, onCheck = { if (state != UpdateState.Loading) request++ },
        onInstall = onInstall, onOpenBrowser = onOpenBrowser, overlay = overlay)
}
