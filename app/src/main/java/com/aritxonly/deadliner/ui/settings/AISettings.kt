package com.aritxonly.deadliner.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsRoute
import com.aritxonly.deadliner.ai.AIUtils
import com.aritxonly.deadliner.ai.LlmPreset
import com.aritxonly.deadliner.localutils.ApiKeystore
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import kotlinx.coroutines.launch

private const val CUSTOM_BYOK_PRESET_ID = "custom_byok"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AISettingsScreen(
    nav: NavHostController,
    navigateUp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = remember { GlobalUtils.getDeadlinerAIConfig() }
    val initialPreset = remember { config.getPresets().firstOrNull { it.id == CUSTOM_BYOK_PRESET_ID } ?: config.getCurrentPreset() }

    var aiEnabled by remember { mutableStateOf(GlobalUtils.deadlinerAIEnable) }
    var clipboard by remember { mutableStateOf(GlobalUtils.clipboardEnable) }
    var autoApproveReadTasks by remember { mutableStateOf(GlobalUtils.aiAutoApproveReadTasks) }
    var silentTaskAdd by remember { mutableStateOf(GlobalUtils.aiSilentTaskAdd) }
    var hideThinkingProcess by remember { mutableStateOf(GlobalUtils.aiHideThinkingProcess) }

    var apiKey by remember { mutableStateOf(ApiKeystore.retrieveAndDecrypt(context).orEmpty()) }
    var baseUrl by remember { mutableStateOf(initialPreset?.toEditableBaseUrl().orEmpty()) }
    var modelId by remember { mutableStateOf(initialPreset?.model.orEmpty()) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_deadliner_ai),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = navIconPaddingModifier
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
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
            SettingsSection(
                mainContent = true,
                enabled = aiEnabled
            ) {
                SettingsSwitchItem(
                    label = R.string.settings_enable_ai,
                    checked = aiEnabled,
                    onCheckedChange = {
                        GlobalUtils.deadlinerAIEnable = it
                        aiEnabled = it
                    },
                    mainSwitch = true
                )
            }

            SvgCard(
                svgRes = R.drawable.svg_deadliner_ai,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                stringResource(R.string.settings_ai_description),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodySmall
            )

            if (aiEnabled) {
                SettingsSection(topLabel = stringResource(R.string.settings_ai_function)) {
                    SettingsSectionDivider()

                    SettingsDetailSwitchItem(
                        headline = R.string.settings_clipboard,
                        supportingText = R.string.settings_support_clipboard,
                        checked = clipboard,
                        onCheckedChange = {
                            GlobalUtils.clipboardEnable = it
                            clipboard = it
                        }
                    )
                }

//            SettingsSection(topLabel = stringResource(R.string.settings_ai_interaction_preferences)) {
//                SettingsDetailSwitchItem(
//                    headline = R.string.settings_ai_auto_approve_read_tasks,
//                    supportingText = R.string.settings_ai_auto_approve_read_tasks_support,
//                    checked = autoApproveReadTasks,
//                    onCheckedChange = {
//                        GlobalUtils.aiAutoApproveReadTasks = it
//                        autoApproveReadTasks = it
//                    }
//                )
//
//                SettingsSectionDivider()
//
//                SettingsDetailSwitchItem(
//                    headline = R.string.settings_ai_silent_task_add,
//                    supportingText = R.string.settings_ai_silent_task_add_support,
//                    checked = silentTaskAdd,
//                    onCheckedChange = {
//                        GlobalUtils.aiSilentTaskAdd = it
//                        silentTaskAdd = it
//                    }
//                )
//
//                SettingsSectionDivider()
//
//                SettingsDetailSwitchItem(
//                    headline = R.string.settings_ai_hide_thinking_process,
//                    supportingText = R.string.settings_ai_hide_thinking_process_support,
//                    checked = hideThinkingProcess,
//                    onCheckedChange = {
//                        GlobalUtils.aiHideThinkingProcess = it
//                        hideThinkingProcess = it
//                    }
//                )
//            }

                SettingsSection(
                    topLabel = stringResource(R.string.settings_ai_byok_title),
                    customColor = MaterialTheme.colorScheme.surface,
                    clipContent = false
                ) {
                    RoundedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            errorMessage = null
                        },
                        hint = stringResource(R.string.settings_ai_api_key),
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )

                    RoundedTextField(
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it
                            errorMessage = null
                        },
                        hint = stringResource(R.string.settings_ai_base_url),
                        keyboardType = KeyboardType.Uri
                    )

                    RoundedTextField(
                        value = modelId,
                        onValueChange = {
                            modelId = it
                            errorMessage = null
                        },
                        hint = stringResource(R.string.settings_ai_model_id)
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                val trimmedKey = apiKey.trim()
                                val trimmedBaseUrl = baseUrl.trim()
                                val trimmedModelId = modelId.trim()
                                if (trimmedKey.isEmpty() || trimmedBaseUrl.isEmpty() || trimmedModelId.isEmpty()) {
                                    errorMessage =
                                        context.getString(R.string.settings_ai_config_incomplete)
                                    return@launch
                                }

                                isSaving = true
                                errorMessage = null
                                try {
                                    AIUtils.validateDirectConfig(
                                        apiKey = trimmedKey,
                                        baseUrl = trimmedBaseUrl,
                                        modelId = trimmedModelId
                                    )
                                    ApiKeystore.encryptAndStore(context, trimmedKey)
                                    val customPreset = LlmPreset(
                                        id = CUSTOM_BYOK_PRESET_ID,
                                        name = trimmedModelId,
                                        model = trimmedModelId,
                                        endpoint = AIUtils.normalizeDirectEndpoint(trimmedBaseUrl)
                                    )
                                    config.upsertPreset(customPreset)
                                    config.setCurrentPresetId(customPreset.id)
                                    GlobalUtils.advancedAISettings = true
                                    AIUtils.setPreset(customPreset, context)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_ai_saved_and_validated),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    val detail = e.message?.takeIf { it.isNotBlank() }
                                        ?: context.getString(R.string.settings_ai_validation_failed_fallback)
                                    errorMessage = context.getString(
                                        R.string.settings_ai_validation_failed,
                                        detail
                                    )
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            if (isSaving) {
                                stringResource(R.string.settings_ai_validating)
                            } else {
                                stringResource(R.string.settings_ai_save_and_validate)
                            }
                        )
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }

                Text(
                    stringResource(R.string.settings_ai_byok_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

private fun LlmPreset.toEditableBaseUrl(): String {
    return endpoint
        .removeSuffix("/chat/completions")
        .trimEnd('/')
}
