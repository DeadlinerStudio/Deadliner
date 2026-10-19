package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.base.TopBarIconButton
import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.aritxonly.deadliner.ui.symbolPainterResource

@Composable
internal fun UpdatePage(
    currentVersion: String,
    state: UpdateState,
    navigateUp: () -> Unit,
    onCheck: () -> Unit,
    onInstall: (String) -> Unit,
    onOpenBrowser: (String) -> Unit,
    overlay: @Composable () -> Unit = {},
) {
    var showLogFilters by rememberSaveable((state as? UpdateState.Available)?.notes) { mutableStateOf(false) }
    CollapsingTopBarScaffold(
        title = stringResource(R.string.update_page_title),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = navIconPaddingModifier.miuixPressFeedback()) {
                Icon(symbolPainterResource(R.drawable.msr_arrow_back_wght400), stringResource(R.string.back),
                    modifier = expressiveTypeModifier, tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        actions = {
            if (state is UpdateState.Available && state.notes.isNotBlank()) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    TopBarIconButton(onClick = { showLogFilters = true }, modifier = Modifier.testTag("update-log-filters")) {
                        Icon(symbolPainterResource(R.drawable.msr_tune_wght400), stringResource(R.string.update_log_filters),
                            modifier = expressiveTypeModifier, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        bottomBar = { UpdateActions(state, onCheck, onInstall, onOpenBrowser) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            // Let content scroll behind the sampled bottom material. Reserve its measured
            // height inside the scroll content so the final card can still clear both buttons.
            SettingsScrollColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding()),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxHeight().widthIn(max = 600.dp).fillMaxWidth()
                    .testTag("update-content")) {
                UpdateVersionHeader(currentVersion, state)
                when (state) {
                    is UpdateState.Available -> UpdateReleaseNotes(state, showLogFilters) { showLogFilters = false }
                    is UpdateState.UpToDate -> SettingsSection {
                        Text(stringResource(R.string.update_latest_summary),
                            Modifier.padding(SettingsItemDefaults.HorizontalPadding),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is UpdateState.Error -> SettingsSection {
                        Column(Modifier.padding(SettingsItemDefaults.HorizontalPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.update_info_unavailable), style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.update_error_summary), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> Unit
                }
                Spacer(Modifier.height(padding.calculateBottomPadding() + 24.dp))
            }
            overlay()
        }
    }
}
