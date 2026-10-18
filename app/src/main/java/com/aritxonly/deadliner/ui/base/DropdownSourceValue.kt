package com.aritxonly.deadliner.ui.base

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** The setting retains the same text layout throughout the popup morph. */
@Composable
internal fun DropdownSourceValue(value: String, color: Color, modifier: Modifier = Modifier,
                                 leading: (@Composable () -> Unit)? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        leading?.let { it(); Spacer(Modifier.width(8.dp)) }
        Text(value, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
            fontSize = MiuixTheme.textStyles.body2.fontSize, color = color,
            textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
        DropdownArrowEndAction(actionColor = color)
    }
}
