package com.aritxonly.deadliner.ui.navigation

import com.aritxonly.deadliner.ui.base.feedback.DefaultClickFeedback
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.GlassMaterialInteraction
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@Immutable
data class MiuixFloatingTabItem(
    val key: String,
    val label: String,
    val selectedIcon: Painter,
    val unselectedIcon: Painter,
    val enabled: Boolean = true,
    val iconScale: Float = 1f,
)

enum class MiuixFloatingTabLayout {
    Horizontal,
    Stacked,
}

object MiuixFloatingTabBarDefaults {
    val Height = 54.dp
    // 6dp content padding - 3dp indicator overflow = the same 3dp visible edge as top/bottom.
    val HorizontalContentPadding = 6.dp
    val VerticalContentPadding = 3.dp
    val IndicatorHorizontalOverflow = 3.dp
    val MaximumWidth = 380.dp
    // Matches the component used by MyHyperModifier (80dp per stacked tab, 380dp panel cap).
    val MaximumStackedItemWidth = 80.dp
    // 26dp icon + 5dp gap + ~49dp label + 24dp breathing room.
    val MaximumHorizontalItemWidth = 104.dp

    fun maximumWidth(itemCount: Int, layout: MiuixFloatingTabLayout) = when (layout) {
        MiuixFloatingTabLayout.Stacked -> minOf(MaximumWidth, MaximumStackedItemWidth * itemCount)
        MiuixFloatingTabLayout.Horizontal -> MaximumHorizontalItemWidth * itemCount + HorizontalContentPadding * 2
    }
    val TabIconSize = 26.dp
    val DetachedIconSize = 28.dp

    val ContainerShape: Shape = Capsule()
    val IndicatorShape: Shape = Capsule()
}

/**
 * Deadliner's compact floating bottom tab bar for the MIUIX design system. A caller that enables
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
    selectionVisible: Boolean = true,
    backdrop: LayerBackdrop? = LocalAdvancedMaterialBackdrop.current,
) {
    if (items.isEmpty()) return

    val selectedIndex = items.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val themedContentColor = MiuixTheme.colorScheme.onSurfaceContainer
    val isDark = themedContentColor.luminance() > 0.5f
    val contentColor = if (isDark) Color.White else Color.Black
    val motion = remember { FloatingTabMotionState(selectedIndex) }
    val panelReturnOffset = remember { Animatable(0f) }
    var directPanelDragOffset: Float? by remember { mutableStateOf(null) }
    val motionScope = rememberCoroutineScope()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    LaunchedEffect(selectedIndex) {
        motion.syncExternalSelection(selectedIndex)
    }

    val selectionAlpha by animateFloatAsState(
        targetValue = if (selectionVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "floating_tab_selection_visibility",
    )

    BoxWithConstraints(
        modifier = modifier
            .wrapContentWidth()
            .widthIn(max = MiuixFloatingTabBarDefaults.maximumWidth(items.size, layout))
            .fillMaxWidth()
            .height(MiuixFloatingTabBarDefaults.Height),
    ) {
        val density = LocalDensity.current
        val panelDragFraction = floatingTabPanelDragFraction(
            dragOffsetPx = directPanelDragOffset ?: panelReturnOffset.value,
            panelWidthPx = constraints.maxWidth.toFloat(),
        )
        val panelOffsetPx = with(density) {
            FloatingTabMotionDefaults.MaximumPanelOffsetDp.dp.toPx()
        } * sign(panelDragFraction) * EaseOut.transform(abs(panelDragFraction))
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(panelOffsetPx.roundToInt(), 0) }
                .floatingNavigationShadow(
                    shape = MiuixFloatingTabBarDefaults.ContainerShape,
                    isDark = isDark,
                ),
        ) {
            SoftGlassSurface(
                modifier = Modifier.fillMaxSize(),
                shape = MiuixFloatingTabBarDefaults.ContainerShape,
                recipe = DeadlinerGlassRecipes.floatingNavigation(advancedMaterial.fineTuning),
                backdrop = backdrop,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = MiuixFloatingTabBarDefaults.HorizontalContentPadding,
                            vertical = MiuixFloatingTabBarDefaults.VerticalContentPadding,
                        ),
                ) {
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
                            .offset(
                                x = itemWidth * motion.visualPosition -
                                    MiuixFloatingTabBarDefaults.IndicatorHorizontalOverflow,
                            )
                            .width(
                                itemWidth +
                                    MiuixFloatingTabBarDefaults.IndicatorHorizontalOverflow * 2,
                            )
                            .fillMaxHeight()
                            .graphicsLayer {
                                alpha = selectionAlpha
                                scaleX = 1f + stretch
                                scaleY = 1f
                                transformOrigin = TransformOrigin(transformOriginX, 0.5f)
                            }
                            .clip(MiuixFloatingTabBarDefaults.IndicatorShape)
                            .background(indicatorColor),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(items, itemWidthPx, isLtr, selectionVisible) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (!selectionVisible) return@awaitEachGesture
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
                                        if (!dragging && abs(totalDragPx) > viewConfiguration.touchSlop) {
                                            dragging = true
                                            motion.startDrag()
                                        }
                                        if (dragging) {
                                            change.consume()
                                            directPanelDragOffset = totalDragPx
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
                                    val releasedPanelOffset = directPanelDragOffset ?: 0f
                                    motionScope.launch {
                                        panelReturnOffset.snapTo(releasedPanelOffset)
                                        directPanelDragOffset = null
                                        panelReturnOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                                visibilityThreshold = 0.5f,
                                            ),
                                        )
                                    }
                                }
                            }
                            .selectableGroup(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items.forEachIndexed { index, item ->
                            val selected = selectionVisible && index == selectedIndex
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
        else -> contentColor
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
            indication = DefaultClickFeedback,
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
                painter = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = animatedContentColor,
                modifier = Modifier
                    .size(MiuixFloatingTabBarDefaults.TabIconSize)
                    .graphicsLayer {
                        val scale = item.iconScale.coerceIn(0.75f, 1.25f)
                        scaleX = scale
                        scaleY = scale
                    },
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
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
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
