package com.aritxonly.deadliner.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.aritxonly.deadliner.ui.base.GlassDropdownMotion
import com.aritxonly.deadliner.ui.base.DeadlinerMiuixDefaults
import com.aritxonly.deadliner.ui.base.SoftGlassDropdownPopup
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialBackdrop
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import com.aritxonly.deadliner.ui.theme.advancedTextureBlur
import com.aritxonly.deadliner.ui.theme.rememberBlurColors
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.symbolPainterResource
import com.deadliner.domain.task.TaskState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class DetailAction(val iconRes: Int, val labelRes: Int) {
    MARK_COMPLETE(R.drawable.msr_task_alt_wght400, R.string.mark_complete),
    FOCUS(R.drawable.msr_schedule_wght400, R.string.focus_open),
    POSTPONE(R.drawable.msr_more_time_wght400, R.string.detail_postpone),
    GIVE_UP(R.drawable.msr_flag_wght400, R.string.give_up_task),
    RESTORE_ACTIVE(R.drawable.msr_undo_wght400, R.string.restore_active),
    ARCHIVE(R.drawable.msr_move_to_inbox_wght400, R.string.archive),
    UNARCHIVE(R.drawable.msr_undo_wght400, R.string.unarchive),
    DELETE(R.drawable.msr_delete_wght400, R.string.delete),
    SAVE_TO_CALENDAR(R.drawable.msr_event_wght400, R.string.save_and_add_to_calendar),
}

@Composable
internal fun DetailBottomActions(state: TaskState, onActionClick: (DetailAction) -> Unit) {
    val primary = when (state) {
        TaskState.ACTIVE -> DetailAction.MARK_COMPLETE
        TaskState.COMPLETED, TaskState.ABANDONED -> DetailAction.RESTORE_ACTIVE
        TaskState.ARCHIVED, TaskState.ABANDONED_ARCHIVED -> DetailAction.UNARCHIVE
    }
    val more = when (state) {
        TaskState.ACTIVE -> listOf(DetailAction.POSTPONE, DetailAction.GIVE_UP)
        TaskState.COMPLETED, TaskState.ABANDONED -> listOf(DetailAction.ARCHIVE)
        else -> emptyList()
    } + listOf(DetailAction.DELETE, DetailAction.SAVE_TO_CALENDAR)
    var sourceHidden by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(Rect.Zero) }
    var openingAnchor by remember { mutableStateOf(Rect.Zero) }
    val menuProgress = remember { Animatable(0f) }
    var showMore by remember(state) { mutableStateOf(false) }
    var pendingAction by remember(state) { mutableStateOf<DetailAction?>(null) }

    LaunchedEffect(pendingAction, sourceHidden) {
        if (!sourceHidden) pendingAction?.let { action ->
            pendingAction = null
            onActionClick(action)
        }
    }
    // Preserve the original detail bar's full-width sampled material and opaque fallback.
    val advancedMaterial = LocalAdvancedMaterialSpec.current
    val backdrop = LocalAdvancedMaterialBackdrop.current
    val surfaceTint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = advancedMaterial.navigationTintAlpha * 0.55f)
    val materialColors = advancedMaterial.rememberBlurColors(listOf(BlendColorEntry(surfaceTint)))
    val destructiveColor = MiuixTheme.colorScheme.error
    Box(Modifier.fillMaxWidth().then(
        if (advancedMaterial.enabled && backdrop != null) Modifier.advancedTextureBlur(
            advancedMaterial = advancedMaterial, backdrop = backdrop, shape = RectangleShape, colors = materialColors,
        ) else Modifier.background(MaterialTheme.colorScheme.surface),
    )) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { onActionClick(primary) },
                modifier = Modifier.weight(1f).testTag("detail-primary-action"),
                colors = ButtonDefaults.buttonColorsPrimary(),
                minHeight = 48.dp,
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                cornerRadius = DeadlinerMiuixDefaults.ButtonCornerRadius,
            ) {
                Icon(symbolPainterResource(primary.iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(primary.labelRes), style = MiuixTheme.textStyles.button.copy(fontSize = 14.sp, lineHeight = 20.sp))
            }
            if (state == TaskState.ACTIVE) {
                DetailTextAction(
                    stringResource(R.string.detail_focus), DetailAction.FOCUS.iconRes,
                    onClick = { onActionClick(DetailAction.FOCUS) },
                    color = MiuixTheme.colorScheme.onSurface,
                    iconSize = 22.dp,
                    textStyle = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp, lineHeight = 20.sp),
                )
            }
            IconButton(onClick = {
                if (!sourceHidden) {
                    openingAnchor = anchor
                    sourceHidden = true
                    showMore = true
                }
            }, modifier = Modifier.size(48.dp).onGloballyPositioned {
                anchor = it.boundsInWindow().translate(it.positionOnScreen() - it.positionInWindow())
            }.graphicsLayer { alpha = GlassDropdownMotion.sourceAlpha(menuProgress.value) }
                .then(if (sourceHidden) Modifier.clearAndSetSemantics {} else Modifier)) {
                Icon(symbolPainterResource(R.drawable.msr_more_vert_wght400),
                    contentDescription = stringResource(R.string.detail_more_actions), modifier = Modifier.size(26.dp).rotate(90f))
            }
        }
    }
    SoftGlassDropdownPopup(
        show = showMore,
        title = stringResource(R.string.detail_more_actions),
        anchor = openingAnchor,
        items = more.map { stringResource(it.labelRes) },
        selectedIndex = -1,
        progress = menuProgress,
        itemRole = Role.Button,
        itemLeading = { index ->
            val action = more[index]
            Icon(symbolPainterResource(action.iconRes), contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (action == DetailAction.DELETE) MiuixTheme.colorScheme.error
                    else MiuixTheme.colorScheme.onSurface)
        },
        itemTextColor = { index -> if (more[index] == DetailAction.DELETE) destructiveColor else null },
        onDismiss = { showMore = false },
        onDismissFinished = { sourceHidden = false },
        onSelect = { index ->
            if (showMore) {
                pendingAction = more[index]
                showMore = false
            }
        },
    )
}
