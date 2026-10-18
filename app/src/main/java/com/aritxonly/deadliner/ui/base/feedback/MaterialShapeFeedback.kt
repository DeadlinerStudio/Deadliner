package com.aritxonly.deadliner.ui.base.feedback

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Connected toggles own animated per-corner shapes. Let their internal surface clip feedback. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialShapeFeedback(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(), content = content)
}
