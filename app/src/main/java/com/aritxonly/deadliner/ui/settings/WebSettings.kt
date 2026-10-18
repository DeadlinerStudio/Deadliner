package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.CloudSyncProvider
import com.aritxonly.deadliner.localutils.CloudSyncPreferences
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.logging.AppLog
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.sync.SyncScheduler
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WebSettingsScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val webEnabled = GlobalUtils.cloudSyncEnable && CloudSyncPreferences.provider == CloudSyncProvider.WEB_DAV
    var serverBase by remember { mutableStateOf(GlobalUtils.webDavBaseUrl) }
    var serverUser by remember { mutableStateOf(GlobalUtils.webDavUser) }
    var serverPass by remember { mutableStateOf(GlobalUtils.webDavPass) }
    val hostFaultHint = stringResource(R.string.settings_web_host_fault)
    val hostSuccessHint = stringResource(R.string.settings_web_host_success)
    val hostIncompleteHint = stringResource(R.string.settings_web_host_incomplete)

    val onBaseChange: (String) -> Unit = {
        serverBase = it
    }
    val onUserChange: (String) -> Unit = {
        serverUser = it
    }
    val onPassChange: (String) -> Unit = {
        serverPass = it
    }

    val onSaveButtonClick: () -> Unit = onSaveButtonClick@{
        if (serverBase.isEmpty() || serverUser.isEmpty() || serverPass.isEmpty()) {
            Toast.makeText(context, hostIncompleteHint, Toast.LENGTH_SHORT).show()
            return@onSaveButtonClick
        }
        if (!(serverBase.startsWith("https://") || serverBase.startsWith("http://"))) {
            Toast.makeText(context, hostFaultHint, Toast.LENGTH_SHORT).show()
            return@onSaveButtonClick
        }

        // 1) 本地保存配置
        GlobalUtils.webDavBaseUrl = serverBase
        GlobalUtils.webDavUser = serverUser
        GlobalUtils.webDavPass = serverPass
        AppSingletons.updateWeb()
        KmpV6Runtime.updateAutomaticSync()
        Toast.makeText(context, hostSuccessHint, Toast.LENGTH_SHORT).show()

        if (!webEnabled) return@onSaveButtonClick

        // 2) 异步检测可用性 & 触发一次同步
        scope.launch {
            try {
                Toast.makeText(
                    context,
                    context.getString(R.string.webdav_checking),
                    Toast.LENGTH_SHORT
                ).show()

                // MKCOL Deadliner
                runCatching { AppSingletons.web.mkcol("Deadliner") }

                // 检测：HEAD Deadliner/（404 也视为可用）
                val (code, _, _) = AppSingletons.web.head("Deadliner/")
                val usable = when (code) {
                    200, 204, 207, 404 -> true
                    else -> false
                }
                if (!usable) {
                    AppLog.e("WebDAV", code.toString())
                    Toast.makeText(
                        context,
                        context.getString(R.string.webdav_unusable, code),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                Toast.makeText(
                    context,
                    context.getString(R.string.webdav_available),
                    Toast.LENGTH_SHORT
                ).show()

                val outcome = KmpV6Runtime.syncOnce()
                if (outcome.succeeded) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.sync_done),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.sync_conflict),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                val msg = e.message ?: context.getString(R.string.unknown_error)
                Toast.makeText(
                    context,
                    context.getString(R.string.sync_failed_with_msg, msg),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (GlobalUtils.cloudSyncEnable) {
            SyncScheduler.enqueuePeriodic(context)
        } else {
            SyncScheduler.cancelAll(context)
        }
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_webdav),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = (navIconPaddingModifier).miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape)) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { innerPadding ->
        SettingsLazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
        ) {
            item {
                Text(
                    stringResource(R.string.settings_webDAV_description),
                    modifier = Modifier.padding(horizontal = SettingsItemDefaults.HorizontalPadding),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                SettingsSection(
                    customColor = MaterialTheme.colorScheme.surface,
                    clipContent = false
                ) {
                    RoundedTextField(
                        value = serverBase,
                        onValueChange = onBaseChange,
                        hint = stringResource(R.string.settings_web_host)
                    )

                    RoundedTextField(
                        value = serverUser.toString(),
                        onValueChange = onUserChange,
                        hint = stringResource(R.string.settings_web_user)
                    )

                    RoundedTextField(
                        value = serverPass,
                        onValueChange = onPassChange,
                        hint = stringResource(R.string.settings_web_pass),
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )

                    Button(
                        onClick = onSaveButtonClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }

            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }

    }
}
