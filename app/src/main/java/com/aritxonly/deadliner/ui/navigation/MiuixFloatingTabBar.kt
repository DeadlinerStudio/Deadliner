package com.aritxonly.deadliner.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

@Immutable
data class MiuixFloatingTabItem(
    val key: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val enabled: Boolean = true,
)

enum class MiuixFloatingTabLayout {
    Horizontal,
    Stacked,
}

object MiuixFloatingTabBarDefaults {
    val Height = 56.dp
    val ContentPadding = 2.dp
    val MaximumWidth = 380.dp
    val IconSize = 28.dp

    val ContainerShape: Shape = RoundedCornerShape(percent = 50)
    val IndicatorShape: Shape = RoundedCornerShape(percent = 50)
}

/**
 * Deadliner's compact floating bottom tab bar for the Modern/MIUIX design system.
 *
 * The component intentionally emits no layout in Material 3 mode. A caller that enables
 * advanced material should register its content with a [LayerBackdrop] and either pass that
 * backdrop here or provide it through [LocalAdvancedMaterialBackdrop].
 */
@Composable
fun MiuixFloatingTabBar(
    items: List<MiuixFloatingTabItem>,
    selectedKey: String,
    onItemSelected: (MiuixFloatingTabItem) -> Unit,
    modifier: Modifier = Modifier,
    layout: MiuixFloatingTabLayout = MiuixFloatingTabLayout.Horizontal,
    backdrop: LayerBackdrop? = LocalAdvancedMaterialBackdrop.current,
) {
    if (LocalAppDesignSystem.current != AppDesignSystem.MIUIX || items.isEmpty()) return

    val selectedIndex = items.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val contentColor = MiuixTheme.colorScheme.onSurfaceContainer
    val isDark = contentColor.luminance() > 0.5f
    val containerBaseColor = if (isDark) {
        lerp(MiuixTheme.colorScheme.surfaceContainer, Color.Black, 0.28f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val containerTint = containerBaseColor.copy(
        alpha = if (advancedMaterial.enabled) advancedMaterial.navigationTintAlpha else 0.92f,
    )
    val materialColors = advancedMaterial.rememberBlurColors(
        additionalBlendColors = listOf(BlendColorEntry(containerTint)),
    )
    val containerEdgeGlow = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.07f),
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.58f),
                Color.White.copy(alpha = 0.38f),
                Color.White.copy(alpha = 0.24f),
            )
        },
    )
    val materialModifier = if (advancedMaterial.enabled && backdrop != null) {
        Modifier.advancedTextureBlur(
            advancedMaterial = advancedMaterial,
            backdrop = backdrop,
            shape = MiuixFloatingTabBarDefaults.ContainerShape,
            colors = materialColors,
        )
    } else {
        Modifier.background(
            color = containerTint,
            shape = MiuixFloatingTabBarDefaults.ContainerShape,
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .widthIn(max = MiuixFloatingTabBarDefaults.MaximumWidth)
            .fillMaxWidth()
            .height(MiuixFloatingTabBarDefaults.Height)
            .shadow(
                elevation = if (isDark) 7.dp else 8.dp,
                shape = MiuixFloatingTabBarDefaults.ContainerShape,
                ambientColor = if (isDark) {
                    Color.White.copy(alpha = 0.12f)
                } else {
                    Color.Black.copy(alpha = 0.11f)
                },
                spotColor = if (isDark) {
                    Color.White.copy(alpha = 0.08f)
                } else {
                    Color.Black.copy(alpha = 0.16f)
                },
            )
            .clip(MiuixFloatingTabBarDefaults.ContainerShape)
            .then(materialModifier)
            .border(
                border = BorderStroke(1.dp, containerEdgeGlow),
                shape = MiuixFloatingTabBarDefaults.ContainerShape,
            )
            .padding(
                MiuixFloatingTabBarDefaults.ContentPadding,
            ),
    ) {
        val density = LocalDensity.current
        val itemWidth = maxWidth / items.size
        val itemWidthPx = with(density) { itemWidth.toPx() }
        val indicatorOffsetPx = remember { Animatable(0f) }
        var indicatorPositioned by remember { mutableStateOf(false) }
        var indicatorDragging by remember { mutableStateOf(false) }
        var draggedSelectionKey by remember { mutableStateOf<String?>(null) }
        var draggedIndicatorOffsetPx by remember { mutableStateOf<Float?>(null) }
        var indicatorPressed by remember { mutableStateOf(false) }

        LaunchedEffect(selectedIndex, selectedKey, itemWidthPx) {
            val targetOffsetPx = selectedIndex * itemWidthPx
            when {
                !indicatorPositioned || draggedSelectionKey == selectedKey -> {
                    indicatorOffsetPx.snapTo(targetOffsetPx)
                    indicatorPositioned = true
                    draggedSelectionKey = null
                    draggedIndicatorOffsetPx = null
                }

                !indicatorDragging -> indicatorOffsetPx.animateTo(
                    targetValue = targetOffsetPx,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
        }

        val indicatorScale by animateFloatAsState(
            targetValue = if (indicatorPressed) 0.94f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "floating_tab_indicator_press_scale",
        )
        val indicatorColor = if (isDark) {
            Color.White.copy(alpha = 0.13f)
        } else {
            Color.Black.copy(alpha = 0.075f)
        }
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) {
                        (draggedIndicatorOffsetPx ?: indicatorOffsetPx.value).toDp()
                    },
                )
                .width(itemWidth)
                .fillMaxHeight()
                .padding(horizontal = 2.dp, vertical = 1.dp)
                .graphicsLayer {
                    scaleX = indicatorScale
                    scaleY = indicatorScale
                }
                .background(indicatorColor, MiuixFloatingTabBarDefaults.IndicatorShape),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(items, selectedIndex, itemWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val selectedStartPx = selectedIndex * itemWidthPx
                        val selectedEndPx = selectedStartPx + itemWidthPx
                        if (down.position.x !in selectedStartPx..selectedEndPx) {
                            return@awaitEachGesture
                        }

                        indicatorPressed = true
                        val downX = down.position.x
                        var releaseX = downX
                        var dragging = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            releaseX = change.position.x.coerceIn(0f, itemWidthPx * items.size)
                            val totalDrag = change.position.x - downX
                            if (!dragging && abs(totalDrag) > viewConfiguration.touchSlop) {
                                dragging = true
                                indicatorDragging = true
                            }
                            if (dragging) {
                                change.consume()
                                draggedIndicatorOffsetPx = (selectedStartPx + totalDrag).coerceIn(
                                    minimumValue = 0f,
                                    maximumValue = itemWidthPx * (items.size - 1),
                                )
                            }
                            if (!change.pressed) break
                        }

                        if (dragging) {
                            val releasedIndex = (releaseX / itemWidthPx)
                                .toInt()
                                .coerceIn(items.indices)
                            val settledIndex = items.indices
                                .filter { index -> items[index].enabled }
                                .minByOrNull { index -> abs(index - releasedIndex) }
                                ?: selectedIndex
                            val settledItem = items[settledIndex]
                            draggedIndicatorOffsetPx = settledIndex * itemWidthPx
                            draggedSelectionKey = settledItem.key.takeIf { it != selectedKey }
                            indicatorDragging = false
                            indicatorPressed = false
                            onItemSelected(settledItem)
                            if (draggedSelectionKey == null) {
                                draggedIndicatorOffsetPx = null
                            }
                        } else {
                            indicatorPressed = false
                        }
                    }
                }
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                FloatingTabItemContent(
                    item = item,
                    selected = selected,
                    contentColor = contentColor,
                    layout = layout,
                    onClick = { onItemSelected(item) },
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun FloatingTabItemContent(
    item: MiuixFloatingTabItem,
    selected: Boolean,
    contentColor: Color,
    layout: MiuixFloatingTabLayout,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetColor = when {
        !item.enabled -> contentColor.copy(alpha = 0.22f)
        selected -> contentColor
        else -> contentColor.copy(alpha = 0.46f)
    }
    val animatedContentColor by animateColorAsState(
        targetValue = targetColor,
        label = "floating_tab_content_color",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val itemModifier = modifier
        .clip(MiuixFloatingTabBarDefaults.IndicatorShape)
        .selectable(
            selected = selected,
            enabled = item.enabled,
            role = Role.Tab,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    val icon: @Composable () -> Unit = {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.88f))
                    .togetherWith(fadeOut() + scaleOut(targetScale = 1.06f))
            },
            label = "floating_tab_icon",
        ) { isSelected ->
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = animatedContentColor,
                modifier = Modifier.size(MiuixFloatingTabBarDefaults.IconSize),
            )
        }
    }
    val label: @Composable () -> Unit = {
        Text(
            text = item.label,
            color = animatedContentColor,
            style = when (layout) {
                MiuixFloatingTabLayout.Horizontal -> MaterialTheme.typography.labelMedium
                MiuixFloatingTabLayout.Stacked -> MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                )
            },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    when (layout) {
        MiuixFloatingTabLayout.Horizontal -> Row(
            modifier = itemModifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(5.dp))
            label()
        }

        MiuixFloatingTabLayout.Stacked -> Column(
            modifier = itemModifier.padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            label()
        }
    }
}
