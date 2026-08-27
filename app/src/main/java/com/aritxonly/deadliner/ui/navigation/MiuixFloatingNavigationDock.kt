package com.aritxonly.deadliner.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.SoftGlassFloatingActionButton
import com.aritxonly.deadliner.ui.material.glass.SoftGlassIconButton
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val FloatingNavigationDockMaximumWidth = 442.dp
private val FloatingNavigationDockSpacing = 8.dp

data class MiuixFloatingNavigationAction(
    val icon: Painter,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val iconScale: Float = 1f,
    val onClick: () -> Unit,
)

/**
 * Compact immersive navigation dock: primary destinations stay in the left capsule while a
 * single destination or contextual action can live in an independent soft-glass circle on the right.
 */
@Composable
fun MiuixFloatingNavigationDock(
    items: List<MiuixFloatingTabItem>,
    selectedKey: String,
    onItemSelected: (MiuixFloatingTabItem) -> Unit,
    modifier: Modifier = Modifier,
    detachedItem: MiuixFloatingTabItem? = null,
    detachedAction: MiuixFloatingNavigationAction? = null,
    primarySelectionVisible: Boolean = true,
    backdrop: LayerBackdrop? = null,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val themedContentColor = MiuixTheme.colorScheme.onSurfaceContainer
    val isDark = themedContentColor.luminance() > 0.5f
    val contentColor = if (isDark) Color.White else Color.Black
    val primarySelectedKey = items.firstOrNull { it.key == selectedKey && it.enabled }?.key
        ?: items.firstOrNull { it.enabled }?.key
        ?: items.firstOrNull()?.key
        ?: selectedKey

    Row(
        modifier = modifier
            .widthIn(max = FloatingNavigationDockMaximumWidth)
            .fillMaxWidth()
            .height(MiuixFloatingTabBarDefaults.Height),
        horizontalArrangement = Arrangement.spacedBy(FloatingNavigationDockSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixFloatingTabBar(
            items = items,
            selectedKey = primarySelectedKey,
            onItemSelected = onItemSelected,
            modifier = Modifier.weight(1f),
            layout = MiuixFloatingTabLayout.Stacked,
            selectionVisible = primarySelectionVisible,
            backdrop = backdrop,
        )

        if (detachedAction != null) {
            SoftGlassFloatingActionButton(
                onClick = detachedAction.onClick,
                containerColor = detachedAction.containerColor,
                contentColor = detachedAction.contentColor,
                size = MiuixFloatingTabBarDefaults.Height,
                backdrop = backdrop,
            ) {
                Icon(
                    painter = detachedAction.icon,
                    contentDescription = detachedAction.label,
                    modifier = Modifier
                        .size(MiuixFloatingTabBarDefaults.DetachedIconSize)
                        .graphicsLayer {
                            val scale = detachedAction.iconScale.coerceIn(0.75f, 1.25f)
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
        } else {
            detachedItem?.let { item ->
                val selected = item.key == selectedKey
                SoftGlassIconButton(
                    onClick = { onItemSelected(item) },
                    enabled = item.enabled,
                    size = MiuixFloatingTabBarDefaults.Height,
                    recipe = DeadlinerGlassRecipes.floatingNavigation(advancedMaterial.fineTuning),
                    backdrop = backdrop,
                ) {
                    Icon(
                        painter = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = when {
                            !item.enabled -> contentColor.copy(alpha = 0.22f)
                            else -> contentColor
                        },
                        modifier = Modifier.size(MiuixFloatingTabBarDefaults.DetachedIconSize),
                    )
                }
            }
        }
    }
}
