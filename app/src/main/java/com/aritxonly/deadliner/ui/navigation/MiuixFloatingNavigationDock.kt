package com.aritxonly.deadliner.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.SoftGlassFloatingActionButton
import com.aritxonly.deadliner.ui.material.glass.SoftGlassIconButton
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

object MiuixFloatingNavigationDockDefaults {
    // Shared bound for the independent focus timer overlay. Navigation derives its width per tab.
    val MaximumWidth = 442.dp
    val HorizontalPadding = 8.dp
}

private val FloatingNavigationDockSpacing = 8.dp

data class MiuixFloatingNavigationAction(
    @DrawableRes val iconResource: Int,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val iconScale: Float = 1f,
    val onClick: () -> Unit,
    val modifier: Modifier = Modifier,
    val symbolName: String? = null,
    val enabled: Boolean = true,
    val visible: Boolean = true,
    val neutralTint: Boolean = false,
    val iconRotation: Float = 0f,
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
    layout: MiuixFloatingTabLayout = MiuixFloatingTabLayout.Stacked,
) {
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val themedContentColor = MiuixTheme.colorScheme.onSurfaceContainer
    val isDark = themedContentColor.luminance() > 0.5f
    val contentColor = if (isDark) Color.White else Color.Black
    val primarySelectedKey = items.firstOrNull { it.key == selectedKey && it.enabled }?.key
        ?: items.firstOrNull { it.enabled }?.key
        ?: items.firstOrNull()?.key
        ?: selectedKey

    val hasDetachedControl = detachedAction != null || detachedItem != null
    val detachedVisible = detachedAction?.visible ?: (detachedItem != null)
    val detachedSlotWidth = if (hasDetachedControl) {
        MiuixFloatingTabBarDefaults.Height + FloatingNavigationDockSpacing
    } else {
        0.dp
    }
    val detachedFraction by animateFloatAsState(
        targetValue = if (detachedVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f),
        label = "detachedSlotWidth",
    )
    val detachedProgress = detachedFraction.coerceIn(0f, 1f)
    val maximumWidth = MiuixFloatingTabBarDefaults.maximumWidth(items.size, layout) +
        (if (hasDetachedControl) MiuixFloatingTabBarDefaults.Height + FloatingNavigationDockSpacing else 0.dp) +
        MiuixFloatingNavigationDockDefaults.HorizontalPadding * 2

    Row(
        modifier = modifier
            .wrapContentWidth()
            .widthIn(max = maximumWidth)
            .fillMaxWidth()
            .height(MiuixFloatingTabBarDefaults.Height)
            .padding(horizontal = MiuixFloatingNavigationDockDefaults.HorizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixFloatingTabBar(
            items = items,
            selectedKey = primarySelectedKey,
            onItemSelected = onItemSelected,
            // Width expands with the shrinking action slot, always subject to the tab cap.
            modifier = Modifier.weight(1f, fill = false).testTag("floating-navigation-tabs"),
            layout = layout,
            selectionVisible = primarySelectionVisible,
            backdrop = backdrop,
        )

        Box(
            modifier = Modifier.width(detachedSlotWidth * detachedProgress)
                .height(MiuixFloatingTabBarDefaults.Height)
                .graphicsLayer { clip = detachedProgress < 1f; alpha = detachedProgress }
                .then(if (!detachedVisible) Modifier.clearAndSetSemantics { } else Modifier),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (detachedProgress > 0f) {
                if (detachedAction != null) {
                    val rotation by animateFloatAsState(detachedAction.iconRotation, spring(dampingRatio = 0.78f, stiffness = 380f), label = "detachedIconRotation")
                    val isAddAction = detachedAction.symbolName in setOf("add", "add_2")
                    val isHybridAdd = isAddAction && com.aritxonly.deadliner.ui.LocalInterfaceIconPack.current == com.aritxonly.deadliner.ui.InterfaceIconPack.HybridMiuix
                    SoftGlassFloatingActionButton(
                        onClick = detachedAction.onClick,
                        modifier = detachedAction.modifier.requiredSize(MiuixFloatingTabBarDefaults.Height),
                        enabled = detachedAction.enabled && detachedVisible,
                        neutralTint = detachedAction.neutralTint,
                        containerColor = detachedAction.containerColor,
                        contentColor = detachedAction.contentColor,
                        size = MiuixFloatingTabBarDefaults.Height,
                        backdrop = backdrop,
                    ) {
                        Icon(
                            painter = if (isHybridAdd) com.aritxonly.deadliner.ui.homeAddPainter() else detachedAction.symbolName?.let { com.aritxonly.deadliner.ui.materialSymbolPainter(it) }
                                ?: iconResource(detachedAction.iconResource),
                            contentDescription = detachedAction.label,
                            modifier = Modifier
                                .size(if (isAddAction) com.aritxonly.deadliner.ui.interfaceIconSize(
                                    MiuixFloatingTabBarDefaults.DetachedIconSize, 28.dp) else MiuixFloatingTabBarDefaults.DetachedIconSize)
                                .graphicsLayer {
                                    val scale = if (isHybridAdd) 1f else detachedAction.iconScale.coerceIn(0.75f, 1.25f)
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = rotation
                                },
                        )
                    }
                } else {
                    detachedItem?.let { item ->
                        val selected = item.key == selectedKey
                        SoftGlassIconButton(
                            onClick = { onItemSelected(item) },
                            enabled = item.enabled && detachedVisible,
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
    }
}
