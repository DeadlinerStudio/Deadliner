package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.logging.AppLog
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import kotlinx.coroutines.launch

@Composable
fun BackupSettingsScreen(
    handleImport: () -> Unit,
    handleExport: () -> Unit,
    handleWebSettings: () -> Unit,
    navigateUp: () -> Unit,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_backup),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = navIconPaddingModifier
            ) {
                Icon(
                    com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { padding ->
        SettingsScrollColumn(
            contentPadding = padding,
            modifier = Modifier,
        ) {
            SvgCard(R.drawable.svg_backup, modifier = Modifier.padding(vertical = 8.dp))

            SettingsSection {
                SettingsTextButtonItem(
                    text = R.string.settings_import,
                    iconRes = R.drawable.msr_download_wght400
                ) {
                    handleImport()
                }
                SettingsSectionDivider()
                SettingsTextButtonItem(
                    text = R.string.settings_export,
                    iconRes = R.drawable.msr_upload_wght400
                ) {
                    handleExport()
                }
            }

            SettingsSection {
                SettingsTextButtonItem(
                    text = R.string.settings_from_webdav,
                    iconRes = R.drawable.msr_cloud_download_wght400
                ) {
                    if (GlobalUtils.cloudSyncEnable && GlobalUtils.webDavBaseUrl.isNotBlank()
                        && GlobalUtils.webDavUser.isNotBlank() && GlobalUtils.webDavPass.isNotBlank()
                    ) {
                        AppSingletons.updateWeb()

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
                    } else {
                        handleWebSettings()
                    }
                }
            }
        }
    }
}
