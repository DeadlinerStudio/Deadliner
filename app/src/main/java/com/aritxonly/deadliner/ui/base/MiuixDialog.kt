package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * App-level Miuix dialog host.
 *
 * The v5 screens can open dialogs from several different page shells, so the independent
 * Window host is intentional. Keeping it behind one wrapper gives every dialog the same width,
 * 40dp container radius, dismissal lifecycle, and advanced-material background treatment while
 * retaining v5's Android 12 / Miuix 0.9.0 compatibility.
 */
@Composable
fun MiuixDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    maxWidth: Dp = 420.dp,
    content: @Composable () -> Unit,
) {
    RegisterAdvancedMaterialDialogBlur(show = show)
    if (!show) return

    val windowSize = LocalWindowInfo.current.containerDpSize
    val isLargeScreen = windowSize.width >= 840.dp && windowSize.height >= 480.dp
    val contentAlignment = if (isLargeScreen) Alignment.Center else Alignment.BottomCenter
    val shape = RoundedCornerShape(DeadlinerMiuixDefaults.DialogCornerRadius)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onDismissRequest) {
                    detectTapGestures { onDismissRequest() }
                }
                .imePadding()
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(12.dp),
            contentAlignment = contentAlignment,
        ) {
            Column(
                modifier = modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures { /* Consume taps inside the dialog. */ }
                    }
                    .clip(shape)
                    .background(MiuixTheme.colorScheme.background)
                    .padding(24.dp),
            ) {
                title?.let {
                    Text(
                        text = it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        fontSize = MiuixTheme.textStyles.title4.fontSize,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                }
                summary?.let {
                    Text(
                        text = it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        fontSize = MiuixTheme.textStyles.body1.fontSize,
                        textAlign = TextAlign.Center,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                }
                content()
            }
        }
    }
}
