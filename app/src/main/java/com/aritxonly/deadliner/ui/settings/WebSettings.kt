package com.aritxonly.deadliner.ui.settings

import com.aritxonly.deadliner.logging.AppLog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.aritxonly.deadliner.ui.base.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.aritxonly.deadliner.AppSingletons
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.sync.SyncScheduler
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.base.MiuixBottomSheet
import com.aritxonly.deadliner.ui.base.DeadlinerMiuixDefaults
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WebSettingsScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var webEnabled by remember { mutableStateOf(GlobalUtils.cloudSyncEnable) }
    var serverBase by remember { mutableStateOf(GlobalUtils.webDavBaseUrl) }
    var serverUser by remember { mutableStateOf(GlobalUtils.webDavUser) }
    var serverPass by remember { mutableStateOf(GlobalUtils.webDavPass) }
    var showSheet by remember { mutableStateOf(false) }
    var intervalMin by remember { mutableStateOf(GlobalUtils.syncIntervalMinutes.coerceAtLeast(0)) }
    var wifiOnly by remember { mutableStateOf(GlobalUtils.syncWifiOnly) }
    var chargingOnly by remember { mutableStateOf(GlobalUtils.syncChargingOnly) }
    var tombstoneRetentionDays by remember { mutableIntStateOf(GlobalUtils.tombstoneRetentionDays) }

    val hostFaultHint = stringResource(R.string.settings_web_host_fault)
    val hostSuccessHint = stringResource(R.string.settings_web_host_success)
    val hostIncompleteHint = stringResource(R.string.settings_web_host_incomplete)

    val onWebChange: (Boolean) -> Unit = {
        GlobalUtils.cloudSyncEnable = it
        webEnabled = it

        if (GlobalUtils.cloudSyncEnable) {
            KmpV6Runtime.updateAutomaticSync()
            SyncScheduler.enqueuePeriodic(context)
        } else {
            KmpV6Runtime.updateAutomaticSync()
            SyncScheduler.cancelAll(context)
        }
    }
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
            IconButton(onClick = navigateUp, modifier = navIconPaddingModifier) {
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
                SettingsSection(
                    mainContent = true,
                    enabled = webEnabled
                ) {
                    SettingsSwitchItem(
                        label = R.string.settings_enable_webDAV,
                        checked = webEnabled,
                        onCheckedChange = onWebChange,
                        mainSwitch = true
                    )
                }
            }

            item { SvgCard(R.drawable.svg_cloud_sync, modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    stringResource(R.string.settings_webDAV_description),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (webEnabled) {
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

                item {
                    SettingsSection(topLabel = stringResource(R.string.settings_more)) {
                        SettingsDetailTextButtonItem(
                            headline = R.string.settings_web_auto_sync,
                            supporting = R.string.settings_support_web_auto_sync
                        ) {
                            showSheet = true
                        }
                    }
                }

                item {
                    SettingsSection(topLabel = stringResource(R.string.settings_tombstone_retention)) {
                        SettingsSliderItemWithLabel(
                            label = R.string.settings_tombstone_retention,
                            value = tombstoneRetentionDays.toFloat(),
                            valueRange = 0f..90f,
                            steps = 89,
                            onValueChange = {
                                tombstoneRetentionDays = it.toInt()
                                GlobalUtils.tombstoneRetentionDays = it.toInt()
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(
                            R.string.settings_tombstone_retention_description,
                            tombstoneRetentionDays
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }

        SyncIntervalBottomSheet(
            show = showSheet,
            onDismiss = { showSheet = false },
            intervalMin = intervalMin,
            onIntervalChange = { intervalMin = it },
            wifiOnly = wifiOnly,
            onWifiOnlyChange = { wifiOnly = it },
            chargingOnly = chargingOnly,
            onChargingOnlyChange = { chargingOnly = it },
            onSave = {
                GlobalUtils.syncIntervalMinutes = intervalMin
                GlobalUtils.syncWifiOnly = wifiOnly
                GlobalUtils.syncChargingOnly = chargingOnly

                if (GlobalUtils.cloudSyncEnable) {
                    if (intervalMin <= 0) {
                        SyncScheduler.cancelPeriodic(context)
                    } else {
                        SyncScheduler.enqueuePeriodic(context)
                    }
                } else {
                    SyncScheduler.cancelPeriodic(context)
                }
            }
        )
    }
}

@Composable
fun SyncIntervalBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    intervalMin: Int,
    onIntervalChange: (Int) -> Unit,
    wifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    chargingOnly: Boolean,
    onChargingOnlyChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    if (!show) return

    val context = LocalContext.current

    val intervalOptions = remember {
        listOf(
            0    to R.string.sync_interval_manual,
            15   to R.string.sync_interval_15min,
            30   to R.string.sync_interval_30min,
            60   to R.string.sync_interval_1h,
            180  to R.string.sync_interval_3h,
            360  to R.string.sync_interval_6h,
            720  to R.string.sync_interval_12h,
            1440 to R.string.sync_interval_24h,
        )
    }

    MiuixBottomSheet(
        show = true,
        title = stringResource(R.string.settings_web_auto_sync),
        onDismissRequest = onDismiss,
    ) {
        // 用 Column 将主体内容与底部操作区分开：上面滚动、下面固定
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ====== 可滚动的区域 ======
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                // 两个开关在最上面
                item {
                    MiuixSyncSettingRow(
                        title = stringResource(R.string.settings_web_wifi_only),
                        summary = stringResource(R.string.settings_support_web_wifi_only),
                        checked = wifiOnly,
                        onCheckedChange = onWifiOnlyChange,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item {
                    MiuixSyncSettingRow(
                        title = stringResource(R.string.settings_web_charging_only),
                        summary = stringResource(R.string.settings_support_web_charging_only),
                        checked = chargingOnly,
                        onCheckedChange = onChargingOnlyChange,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }

                // 分割线（样式与 SettingsSectionDivider 一致）
                item {
                    SheetDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                // 分组标题
                item {
                    MiuixText(
                        text = stringResource(R.string.settings_web_auto_sync_time),
                        style = MiuixTheme.textStyles.main,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }

                // 间隔单选
                items(
                    items = intervalOptions,
                    key = { it.first }
                ) { opt ->
                    val value = opt.first
                    val label = opt.second
                    RadioRow(
                        text = stringResource(label),
                        selected = (intervalMin == value),
                        onClick = {
                            onIntervalChange(value)
                            GlobalUtils.triggerVibration(context, 10L)
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // ====== 固定在底部的操作按钮 ======
            SheetDivider(modifier = Modifier.fillMaxWidth())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                MiuixTextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                )

                Spacer(Modifier.width(8.dp))

                MiuixButton(
                    onClick = {
                        onSave()
                        onDismiss()
                    },
                    cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
                    colors = MiuixButtonDefaults.buttonColorsPrimary(),
                ) {
                    MiuixText(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun RadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        MiuixText(
            text = text,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.main,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        )
        MiuixRadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun MiuixSyncSettingRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MiuixText(text = title, style = MiuixTheme.textStyles.main)
            MiuixText(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        MiuixSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/** 与 SettingsSectionDivider 一致的样式（厚度 2dp、同色系） */
@Composable
private fun SheetDivider(modifier: Modifier = Modifier, onContainer: Boolean = true) {
    if (!GlobalUtils.hideDividerUi) {
        Spacer(
            modifier = modifier
                .height(1.dp)
                .background(MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.12f)),
        )
    } else {
        Spacer(modifier = Modifier.height(0.dp))
    }
}

suspend fun checkWebDavConnection(): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        val (code, _, _) = AppSingletons.web.head("Deadliner/")
        code in listOf(200, 204, 207, 404)  // 常见“能连通”的状态
    } catch (_: Exception) {
        false
    }
}
