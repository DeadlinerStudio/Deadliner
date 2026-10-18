package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.CloudSyncProvider
import com.aritxonly.deadliner.localutils.CloudSyncPreferences
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import kotlinx.coroutines.launch

@Composable
fun ByosSettingsScreen(navigateUp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled = GlobalUtils.cloudSyncEnable && CloudSyncPreferences.provider == CloudSyncProvider.BYOS
    var baseUrl by remember { mutableStateOf(CloudSyncPreferences.byosBaseUrl) }
    var accessToken by remember { mutableStateOf(CloudSyncPreferences.byosAccessToken) }
    val invalidUrlHint = stringResource(R.string.settings_byos_invalid_url)
    val incompleteHint = stringResource(R.string.settings_byos_incomplete)

    fun saveConfiguration(): Boolean {
        val normalizedUrl = baseUrl.trim()
        if (normalizedUrl.isEmpty() || accessToken.isBlank()) {
            Toast.makeText(context, incompleteHint, Toast.LENGTH_SHORT).show()
            return false
        }
        if (!normalizedUrl.startsWith("https://") && !normalizedUrl.startsWith("http://")) {
            Toast.makeText(context, invalidUrlHint, Toast.LENGTH_SHORT).show()
            return false
        }
        if (normalizedUrl != CloudSyncPreferences.byosBaseUrl || accessToken != CloudSyncPreferences.byosAccessToken) {
            CloudSyncPreferences.resetByosScopeId()
        }
        CloudSyncPreferences.byosBaseUrl = normalizedUrl
        CloudSyncPreferences.byosAccessToken = accessToken
        KmpV6Runtime.updateAutomaticSync()
        Toast.makeText(context, R.string.settings_byos_saved, Toast.LENGTH_SHORT).show()
        return true
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_byos),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = (navIconPaddingModifier).miuixPressFeedback(enabled = true, shape = androidx.compose.material3.IconButtonDefaults.standardShape)) {
                Icon(
                    painter = com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier,
                )
            }
        },
    ) { innerPadding ->
        SettingsLazyColumn(contentPadding = innerPadding) {
            item {
                Text(
                    text = stringResource(R.string.settings_byos_description),
                    modifier = Modifier.padding(horizontal = SettingsItemDefaults.HorizontalPadding),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                SettingsSection(customColor = MaterialTheme.colorScheme.surface, clipContent = false) {
                    RoundedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        hint = stringResource(R.string.settings_byos_server_url),
                    )
                    RoundedTextField(
                        value = accessToken,
                        onValueChange = { accessToken = it },
                        hint = stringResource(R.string.settings_byos_access_token),
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                    )
                    Button(
                        onClick = {
                            if (saveConfiguration() && enabled) {
                                scope.launch {
                                    val outcome = KmpV6Runtime.syncOnce()
                                    val message = if (outcome.succeeded) {
                                        context.getString(R.string.sync_done)
                                    } else {
                                        context.getString(R.string.sync_failed_with_msg, outcome.failureMessage.orEmpty())
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}
