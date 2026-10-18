package com.aritxonly.deadliner.ui.lifi

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.material.glass.DeadlinerGlassRecipes
import com.aritxonly.deadliner.ui.material.glass.SoftGlassSurface
import com.aritxonly.deadliner.ui.material.glass.softGlassShadow
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.theme.LocalAdvancedMaterialSpec

/** Quick-add glass container with the independent Lifi prototype content. */
@Composable
internal fun LifiComposer(value: String, onValueChange: (String) -> Unit, enabled: Boolean, running: Boolean, onSend: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    val send = { keyboard?.hide(); onSend() }
    val shape = RoundedCornerShape(DeadlinerGlassRecipes.QuickAddCornerRadius)
    SoftGlassSurface(
        modifier = modifier.fillMaxWidth().softGlassShadow(shape, colors.onSurface.luminance() > .5f).semantics { paneTitle = "对话输入" },
        shape = shape,
        recipe = DeadlinerGlassRecipes.quickAdd(LocalAdvancedMaterialSpec.current.fineTuning),
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).animateContentSize().padding(top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BasicTextField(
                value = value, onValueChange = onValueChange, enabled = enabled && !running,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 44.dp, max = 126.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary), maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (enabled && value.isNotBlank() && !running) send() }),
                decorationBox = { inner -> Box {
                    if (value.isEmpty()) Text(if (running) "Lifi 正在处理…" else "告诉我，你想做什么…", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                    inner()
                } },
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (running) "离开页面也会继续处理" else "任务、习惯，或一个还没理清的想法", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(40.dp).miuixPressFeedback(enabled = running || (enabled && value.isNotBlank()), overlay = false).clip(CircleShape)
                        .background(colors.primary.copy(alpha = .12f))
                        .clickable(enabled = running || (enabled && value.isNotBlank()), role = Role.Button,
                            onClick = if (running) onStop else send),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (running) iconResource(R.drawable.msr_stop_wght400)
                        else com.aritxonly.deadliner.ui.originalMaterialSymbolPainter("arrow_upward"),
                        contentDescription = if (running) "停止" else "发送",
                        modifier = Modifier.size(20.dp),
                        tint = if (running || (enabled && value.isNotBlank())) colors.primary else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
