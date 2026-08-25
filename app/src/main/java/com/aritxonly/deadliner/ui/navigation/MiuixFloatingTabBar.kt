package com.aritxonly.deadliner.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.GlassMaterialInteraction
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.shapes.SmoothCapsuleShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch

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
    val Height = 54.dp
    val ContentPadding = 1.5.dp
    val MaximumWidth = 380.dp
    val IconSize = 28.dp

    val ContainerShape: Shape = SmoothCapsuleShape()
    val IndicatorShape: Shape = SmoothCapsuleShape()
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
    val motion = remember { FloatingTabMotionState(selectedIndex) }
    val motionScope = rememberCoroutineScope()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    LaunchedEffect(selectedIndex) {
        motion.syncExternalSelection(selectedIndex)
    }

    val indicatorPressScale by animateFloatAsState(
        targetValue = if (motion.isPressed) {
            FloatingTabMotionDefaults.PressedScale
        } else {
            1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "floating_tab_indicator_press_scale",
    )

    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .widthIn(max = MiuixFloatingTabBarDefaults.MaximumWidth)
            .fillMaxWidth()
            .height(MiuixFloatingTabBarDefaults.Height)
            .shadow(
                elevation = 12.dp,
                shape = MiuixFloatingTabBarDefaults.ContainerShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.48f else 0.24f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.32f else 0.18f),
            ),
    ) {
        val stretch by animateFloatAsState(
            targetValue = floatingTabStretch(
                velocityPxPerSecond = motion.velocityPxPerSecond,
                tabWidthPx = constraints.maxWidth.toFloat() / items.size,
            ),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh,
            ),
            label = "floating_tab_indicator_velocity_stretch",
        )
        SoftGlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = MiuixFloatingTabBarDefaults.ContainerShape,
            recipe = DeadlinerGlassRecipes.floatingNavigation(advancedMaterial.fineTuning),
            interaction = GlassMaterialInteraction(
                refractionScale = if (motion.isPressed) 1.06f else 1f,
                highlightScale = if (motion.isPressed) 1.04f else 1f,
            ),
            backdrop = backdrop,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MiuixFloatingTabBarDefaults.ContentPadding),
            ) {
                val density = LocalDensity.current
                val itemWidth = maxWidth / items.size
                val itemWidthPx = with(density) { itemWidth.toPx() }
                val indicatorColor = if (isDark) {
                    Color.White.copy(alpha = 0.13f)
                } else {
                    Color.Black.copy(alpha = 0.075f)
                }
                val transformOriginX = floatingTabTransformOriginX(
                    velocityPxPerSecond = motion.velocityPxPerSecond,
                    stretch = stretch,
                )

                Box(
                    modifier = Modifier
                        .offset(x = itemWidth * motion.visualPosition)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                        .graphicsLayer {
                            scaleX = indicatorPressScale * (1f + stretch)
                            scaleY = indicatorPressScale
                            transformOrigin = TransformOrigin(transformOriginX, 0.5f)
                        }
                        .clip(MiuixFloatingTabBarDefaults.IndicatorShape)
                        .background(indicatorColor),
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(items, itemWidthPx, isLtr) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val indicatorStartPx = if (isLtr) {
                                    motion.visualPosition * itemWidthPx
                                } else {
                                    (items.lastIndex - motion.visualPosition) * itemWidthPx
                                }
                                if (down.position.x !in indicatorStartPx..(indicatorStartPx + itemWidthPx)) {
                                    return@awaitEachGesture
                                }

                                motion.press()
                                val downX = down.position.x
                                val startPosition = motion.visualPosition
                                val velocityTracker = VelocityTracker()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)
                                var dragging = false
                                var cancelled = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null) {
                                        cancelled = true
                                        break
                                    }
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    val totalDragPx = change.position.x - downX
                                    if (!dragging && kotlin.math.abs(totalDragPx) > viewConfiguration.touchSlop) {
                                        dragging = true
                                        motion.startDrag()
                                    }
                                    if (dragging) {
                                        change.consume()
                                        val physicalVelocity = velocityTracker.calculateVelocity().x
                                        val logicalDirection = if (isLtr) 1f else -1f
                                        motion.dragTo(
                                            positionInTabs = startPosition +
                                                totalDragPx / itemWidthPx * logicalDirection,
                                            tabCount = items.size,
                                            velocityPxPerSecond = physicalVelocity,
                                        )
                                    }
                                    if (!change.pressed) break
                                }

                                if (dragging && !cancelled) {
                                    val targetIndex = resolveFloatingTabTarget(
                                        position = motion.visualPosition,
                                        tabCount = items.size,
                                        enabledIndices = items.indices.filter { items[it].enabled },
                                    )
                                    motion.finishGesture(targetIndex)
                                    motionScope.launch { motion.settleTo(targetIndex) }
                                    onItemSelected(items[targetIndex])
                                } else {
                                    motion.cancelGesture()
                                    if (cancelled) {
                                        motionScope.launch { motion.settleTo(selectedIndex) }
                                    }
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
                            selectedPressProgress = if (selected && motion.isPressed) 1f else 0f,
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
    }
}

@Composable
private fun FloatingTabItemContent(
    item: MiuixFloatingTabItem,
    selected: Boolean,
    selectedPressProgress: Float,
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
    val animatedContentScale by animateFloatAsState(
        targetValue = 1f + selectedPressProgress * 0.08f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "floating_tab_selected_content_scale",
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
        .graphicsLayer {
            scaleX = animatedContentScale
            scaleY = animatedContentScale
        }
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
