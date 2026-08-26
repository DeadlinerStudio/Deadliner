package com.aritxonly.deadliner.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.SoftGlassDefaults
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import com.aritxonly.deadliner.ui.material.glass.softGlassShadow
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

object ImmersiveTextTabRowDefaults {
    val Height = 42.dp
    val MaximumWidth = 420.dp
    val ContentPadding = 3.dp
}

internal fun resolveImmersiveTextTabIndex(
    selectedTabIndex: Int,
    tabCount: Int,
): Int = if (tabCount <= 0) 0 else selectedTabIndex.coerceIn(0, tabCount - 1)

/** Pure-text soft-glass tabs used by MIUIX immersive top bars. */
@Composable
fun ImmersiveTextTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = LocalAdvancedMaterialBackdrop.current,
) {
    if (tabs.isEmpty()) return

    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val contentColor = MiuixTheme.colorScheme.onSurfaceContainer
    val isDark = contentColor.luminance() > 0.5f
    val resolvedSelectedIndex = resolveImmersiveTextTabIndex(selectedTabIndex, tabs.size)
    val selectionPosition by animateFloatAsState(
        targetValue = resolvedSelectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "immersive_text_tab_selection_position",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ImmersiveTextTabRowDefaults.Height),
        contentAlignment = Alignment.Center,
    ) {
        SoftGlassSurface(
            modifier = Modifier
                .widthIn(max = ImmersiveTextTabRowDefaults.MaximumWidth)
                .fillMaxWidth()
                .height(ImmersiveTextTabRowDefaults.Height)
                .softGlassShadow(
                    shape = SoftGlassDefaults.Shape,
                    isDark = isDark,
                ),
            shape = SoftGlassDefaults.Shape,
            recipe = DeadlinerGlassRecipes.floatingNavigation(advancedMaterial.fineTuning),
            backdrop = backdrop,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ImmersiveTextTabRowDefaults.ContentPadding),
            ) {
                val itemWidth = maxWidth / tabs.size
                val indicatorColor = if (isDark) {
                    Color.White.copy(alpha = 0.13f)
                } else {
                    Color.Black.copy(alpha = 0.075f)
                }

                Box(
                    modifier = Modifier
                        .offset(x = itemWidth * selectionPosition)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(SoftGlassDefaults.Shape)
                        .background(indicatorColor),
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = index == resolvedSelectedIndex
                        val textColor by animateColorAsState(
                            targetValue = if (selected) {
                                contentColor
                            } else {
                                contentColor.copy(alpha = 0.50f)
                            },
                            label = "immersive_text_tab_color_$index",
                        )
                        val interactionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .fillMaxHeight()
                                .clip(SoftGlassDefaults.Shape)
                                .selectable(
                                    selected = selected,
                                    role = Role.Tab,
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onTabSelected(index) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = title,
                                color = textColor,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                ),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
