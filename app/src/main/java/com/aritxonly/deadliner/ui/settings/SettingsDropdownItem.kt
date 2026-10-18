package com.aritxonly.deadliner.ui.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aritxonly.deadliner.ui.base.SoftGlassDropdownPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsDropdownItem(
    title: String,
    options: List<RadioOption<T>>,
    selectedKey: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    SoftGlassDropdownPreference(
        title = title,
        items = options.map { stringResource(it.labelRes) },
        selectedIndex = options.indexOfFirst { it.key == selectedKey },
        modifier = modifier,
        onSelectedIndexChange = { index ->
            options.getOrNull(index)?.let { onOptionSelected(it.key) }
        },
    )
}
