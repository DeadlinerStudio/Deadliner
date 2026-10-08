package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.notification.NotificationPolicyPreferences
import com.aritxonly.deadliner.platform.kmp.KmpV6Runtime
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.deadliner.platform.HabitNotificationMode

@Composable
fun NotificationSettingsScreen(navigateUp: () -> Unit) {
    val context = LocalContext.current
    var policy by remember { mutableStateOf(NotificationPolicyPreferences.read(context)) }
    fun save(value: com.deadliner.platform.NotificationPolicy) {
        policy = value
        NotificationPolicyPreferences.save(context, value)
        KmpV6Runtime.refreshNotifications()
    }
    CollapsingTopBarScaffold(title = stringResource(R.string.settings_notification), navigationIcon = {
        IconButton(onClick = navigateUp, modifier = navIconPaddingModifier) {
            Icon(com.aritxonly.deadliner.ui.symbolPainterResource(R.drawable.msr_arrow_back_wght400),
                contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface,
                modifier = expressiveTypeModifier)
        }
    }) { padding ->
        SettingsScrollColumn(contentPadding = padding, modifier = Modifier) {
            SvgCard(R.drawable.svg_notifications, modifier = Modifier.padding(vertical = 8.dp))
            SettingsSection(topLabel = stringResource(R.string.notification_task_settings)) {
                SettingsDetailSwitchItem(headline = R.string.notification_task_realtime,
                    supportingText = R.string.notification_task_realtime_description, checked = policy.taskRealtime,
                    onCheckedChange = { save(policy.copy(taskRealtime = it)) })
                SettingsSectionDivider()
                SettingsDetailSwitchItem(headline = R.string.notification_task_nearby,
                    supportingText = R.string.notification_task_nearby_description, checked = policy.taskNearby,
                    onCheckedChange = { save(policy.copy(taskNearby = it)) })
                SettingsSectionDivider()
                SettingsDetailSwitchItem(headline = R.string.notification_task_due,
                    supportingText = R.string.notification_task_due_description, checked = policy.taskDue,
                    onCheckedChange = { save(policy.copy(taskDue = it)) })
            }
            SettingsSection(topLabel = stringResource(R.string.notification_habit_settings)) {
                SettingsRadioGroupItem(options = listOf(
                    RadioOption(HabitNotificationMode.REALTIME, R.string.notification_habit_realtime),
                    RadioOption(HabitNotificationMode.DUE, R.string.notification_habit_due)),
                    selectedKey = policy.habitMode, onOptionSelected = { save(policy.copy(habitMode = it)) })
            }
            IslandNotificationSettings()
            NotificationDiagnosticSettings()
        }
    }
}
