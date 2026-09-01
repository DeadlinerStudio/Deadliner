package com.aritxonly.deadliner.ui.settings

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.AddDDLTileService
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.SvgCard
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetPaddingPreset
import com.aritxonly.deadliner.widgets.glance.preferences.TaskWidgetPreferences

@Composable
fun WidgetSettingsScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    var paddingPreset by remember {
        mutableStateOf(TaskWidgetPreferences.readPaddingPreset(context))
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_widget),
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
        SettingsScrollColumn(
            contentPadding = innerPadding,
            modifier = Modifier,
        ) {
            SvgCard(R.drawable.svg_space, modifier = Modifier.padding(vertical = 8.dp))

            SettingsSection(topLabel = stringResource(R.string.settings_widget_padding)) {
                SettingsRadioGroupItem(
                    options = listOf(
                        RadioOption(TaskWidgetPaddingPreset.Standard, R.string.settings_widget_padding_standard),
                        RadioOption(TaskWidgetPaddingPreset.Grid4x6, R.string.settings_widget_padding_4x6),
                        RadioOption(TaskWidgetPaddingPreset.Grid4x7, R.string.settings_widget_padding_4x7),
                    ),
                    selectedKey = paddingPreset,
                    onOptionSelected = { preset ->
                        paddingPreset = preset
                        TaskWidgetPreferences.writePaddingPreset(context, preset)
                    },
                )
            }

            SettingsSection(topLabel = stringResource(R.string.control_center_tile)) {
                SettingsDetailTextButtonItem(
                    headline = R.string.add_ddl_tile,
                    supporting = R.string.add_ddl_tile_supporting,
                ) {
                    val component = ComponentName(context, AddDDLTileService::class.java)
                    requestTile(
                        context,
                        component,
                        R.drawable.msr_alarm_add_wght400,
                        ContextCompat.getString(context, R.string.add_ddl_tile_label)
                    )
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

internal fun requestTile(context: Context, component: ComponentName, @DrawableRes res: Int, appLabel: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val sbm = context.getSystemService(StatusBarManager::class.java)
        val icon = android.graphics.drawable.Icon.createWithResource(context, res)

        sbm.requestAddTileService(
            /* tileServiceComponentName = */ component,
            /* tileLabel = */ appLabel,
            /* icon = */ icon,
            /* resultExecutor = */ (context as? Activity)?.mainExecutor ?: context.mainExecutor
        ) { result ->
            when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                    // 已成功添加
                    Toast.makeText(context, R.string.tile_result_tile_added, Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                    Toast.makeText(context, R.string.tile_result_tile_already_added, Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> {
                    Toast.makeText(context, R.string.tile_result_tile_not_added, Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_ERROR_APP_NOT_IN_FOREGROUND -> {
                    Toast.makeText(context, R.string.tile_error_app_not_in_foreground, Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_ERROR_BAD_COMPONENT -> {
                    Toast.makeText(context, R.string.tile_error_bad_component, Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_ERROR_MISMATCHED_PACKAGE -> {
                    Toast.makeText(context, R.string.tile_error_mismatched_package, Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_ERROR_REQUEST_IN_PROGRESS -> {
                    Toast.makeText(context, R.string.tile_error_request_in_progress, Toast.LENGTH_SHORT).show()
                }
                StatusBarManager.TILE_ADD_REQUEST_ERROR_NO_STATUS_BAR_SERVICE -> {
                    Toast.makeText(context, R.string.tile_error_no_status_bar_service, Toast.LENGTH_SHORT).show()
                }
            }
        }
    } else {
        Toast.makeText(context, R.string.tile_invalid_android, Toast.LENGTH_SHORT).show()
    }
}
