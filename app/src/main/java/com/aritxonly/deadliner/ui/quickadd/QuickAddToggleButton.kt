package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.material.glass.SoftGlassFloatingActionButton
import com.aritxonly.deadliner.ui.materialSymbolPainter
import com.aritxonly.deadliner.ui.theme.LocalHomeFabColors
import top.yukonga.miuix.kmp.basic.FloatingActionButtonDefaults

@Composable
internal fun QuickAddToggleButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
                                   enabled: Boolean = true, size: Dp = FloatingActionButtonDefaults.MinWidth) {
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, spring(dampingRatio = 0.78f, stiffness = 380f), label = "quickAddToggleRotation")
    SoftGlassFloatingActionButton(onClick, modifier, enabled = enabled, size = size, neutralTint = expanded,
        contentColor = if (expanded) MaterialTheme.colorScheme.onSurface else LocalHomeFabColors.current.content) {
        Icon(com.aritxonly.deadliner.ui.homeAddPainter(), if (expanded) "关闭快速添加" else "添加",
            Modifier.size(com.aritxonly.deadliner.ui.interfaceIconSize(30.dp, 28.dp)).graphicsLayer { rotationZ = rotation })
    }
}
