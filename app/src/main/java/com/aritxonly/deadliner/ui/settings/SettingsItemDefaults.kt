package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults

/** Shared content insets for every settings row, including Dropdown. */
internal object SettingsItemDefaults {
    val HorizontalPadding = 20.dp
    val InsideMargin: PaddingValues
        get() = PaddingValues(
            start = HorizontalPadding,
            top = BasicComponentDefaults.InsideMargin.calculateTopPadding(),
            end = HorizontalPadding,
            bottom = BasicComponentDefaults.InsideMargin.calculateBottomPadding(),
        )
}
