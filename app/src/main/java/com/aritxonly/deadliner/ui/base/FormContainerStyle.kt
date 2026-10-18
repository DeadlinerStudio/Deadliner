package com.aritxonly.deadliner.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.aritxonly.deadliner.ui.theme.LocalTextFieldContainerColors
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleSurface

/** Set after the popup boundary so every form in any Sheet gets the Sheet semantic role. */
internal val LocalSheetFormContainers = staticCompositionLocalOf { false }

@Composable
internal fun formContainerColor(fallback: Color): Color {
    val colors = LocalTextFieldContainerColors.current
    return if (LocalSheetFormContainers.current) colors?.textFieldSheetContainer ?: fallback.copy(alpha = 0.65f)
        else colors?.textFieldContainer ?: fallback
}

/** Match MIUIX TextField's translucent fill path, including its continuous rounded corners. */
@Composable
internal fun Modifier.formContainerBackground(cornerRadius: Dp): Modifier = squircleSurface(Color.Transparent, cornerRadius).squircleBackground(
    color = formContainerColor(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.secondaryContainer),
    cornerRadius = cornerRadius,
)

/** These fields paint their shared fill once through formContainerBackground. */
@Composable
internal fun standardMaterialTextFieldColors(): androidx.compose.material3.TextFieldColors =
    androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
    )
