package com.aritxonly.deadliner.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
internal data class TextFieldContainerColors(
    val textFieldContainer: Color,
    val textFieldSheetContainer: Color,
)

internal val LocalTextFieldContainerColors = staticCompositionLocalOf<TextFieldContainerColors?> { null }

/** Application-owned semantic roles; Material's built-in palette stays unchanged. */
internal val ColorScheme.textFieldContainer: Color
    @Composable get() = LocalTextFieldContainerColors.current?.textFieldContainer ?: secondaryContainer

internal val ColorScheme.textFieldSheetContainer: Color
    @Composable get() = LocalTextFieldContainerColors.current?.textFieldSheetContainer ?: secondaryContainer.copy(alpha = 0.65f)
