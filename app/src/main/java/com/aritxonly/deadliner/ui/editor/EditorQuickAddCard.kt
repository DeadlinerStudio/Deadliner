package com.aritxonly.deadliner.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import com.aritxonly.deadliner.ui.base.formContainerBackground
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.quickadd.QuickAddAIAction
import com.aritxonly.deadliner.ui.quickadd.QuickAddState

/** The full form owns attributes and saving; this card only recognizes natural-language input. */
@Composable
internal fun EditorQuickAddCard(quick: QuickAddState) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Box(Modifier.formContainerBackground(DeadlinerGlassRecipes.QuickAddCornerRadius)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("快速添加", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
            BasicTextField(quick.text, quick::edit, Modifier.fillMaxWidth().heightIn(min = 64.dp),
                enabled = quick.canClose, maxLines = 5,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() }),
                decorationBox = { inner -> Box {
                    if (quick.text.isEmpty()) Text("明天下午3点交报告 / 每天读书",
                        style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    inner()
                } })
            AnimatedVisibility(quick.text.isNotBlank() && (quick.needsAI || quick.recognizingAI)) {
                QuickAddAIAction(quick)
            }
            val status = if (quick.recognizingAI) "正在生成草案…" else quick.message
            if (status.isNotBlank()) Text(status,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}
