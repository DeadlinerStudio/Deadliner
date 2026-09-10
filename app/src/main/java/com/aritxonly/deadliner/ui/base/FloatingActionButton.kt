package com.aritxonly.deadliner.ui.base

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

import top.yukonga.miuix.kmp.basic.FloatingActionButton as MiuixFab

private val DeadlinerFabShape = RoundedCornerShape(20.dp)

/**
 * 应用级 MIUIX FAB。
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    forceMaterial3: Boolean = false,
    content: @Composable () -> Unit,
) {
    MiuixFab(
        onClick = onClick,
        modifier = modifier.clip(DeadlinerFabShape),
        content = content,
    )
}
