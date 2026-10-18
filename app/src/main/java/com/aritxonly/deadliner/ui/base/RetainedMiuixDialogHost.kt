package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.material3.LocalContentColor as MaterialContentColor
import androidx.compose.material3.LocalTextStyle as MaterialTextStyle
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.aritxonly.deadliner.model.MiuixModalKind
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.theme.LocalDismissState

internal data class MiuixDialogRequest(
    val title: String?,
    val summary: String?,
    val modifier: Modifier,
    val maxWidth: Dp,
    val locals: CompositionLocalContext,
    val onDismiss: () -> Unit,
    val content: @Composable () -> Unit,
)

@Stable
internal class RetainedMiuixDialog(request: MiuixDialogRequest) {
    var request by mutableStateOf(request)
    var show by mutableStateOf(true)
}

/** Lives with the root scaffold, not with conditional callers such as if (show) AlertDialog. */
@Composable
internal fun RetainedMiuixDialogs(state: AdvancedMaterialDialogBlurHostState) {
    for ((id, dialog) in state.dialogs.toMap()) {
        key(id) {
            val request = dialog.request
            CompositionLocalProvider(request.locals) {
                MiuixDialogSurface(dialog.show, request, onDismissFinished = { state.removeFinished(id) })
            }
        }
    }
}

@Composable
internal fun MiuixDialogSurface(
    show: Boolean,
    request: MiuixDialogRequest,
    onDismissFinished: () -> Unit,
) {
    val advanced = LocalAdvancedMaterialSpec.current
    // A separate window may safely sample the full underlying window, including Sheets.
    val backdrop = LocalWindowGlassBackdrop.current
    DisposableEffect(backdrop, advanced.textureBlurEnabled) {
        val active = backdrop?.takeIf { advanced.textureBlurEnabled }
        active?.let { it.consumers++ }
        onDispose { active?.let { it.consumers-- } }
    }
    val shape = remember { RoundedCornerShape(DeadlinerMiuixDefaults.DialogCornerRadius) }
    val surface = rememberMiuixModalSurface(
        kind = MiuixModalKind.Dialog,
        shape = shape,
        recipe = DeadlinerGlassRecipes.dialog(advanced.fineTuning),
        backdrop = backdrop,
    )
    WindowDialog(
        show = show,
        onDismissRequest = request.onDismiss,
        onDismissFinished = onDismissFinished,
        title = request.title,
        summary = request.summary,
        modifier = request.modifier.then(surface.modifier),
        backgroundColor = surface.backgroundColor,
        cornerRadius = DeadlinerMiuixDefaults.DialogCornerRadius,
        maxWidth = request.maxWidth,
    ) {
        // A detached caller's actions remain painted during exit, but can no longer fire.
        val closing = if (show) Modifier else Modifier.clearAndSetSemantics {}.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
        // The caller scope is restored before creating WindowDialog. Keep its new Android
        // window locals here (especially LocalView), instead of restoring the Sheet's View.
        val contentColor = MiuixTheme.colorScheme.onSurface
        CompositionLocalProvider(
            LocalDismissState provides request.onDismiss,
            MaterialContentColor provides contentColor,
            MiuixContentColor provides contentColor,
            MaterialTextStyle provides MaterialTextStyle.current.copy(color = Color.Unspecified),
        ) {
            Box(Modifier.fillMaxWidth().then(closing)) { request.content() }
        }
    }
}
