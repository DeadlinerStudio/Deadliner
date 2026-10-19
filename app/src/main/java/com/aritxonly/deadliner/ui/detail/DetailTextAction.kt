package com.aritxonly.deadliner.ui.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.deadliner.ui.symbolPainterResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Compact ink and padding with a full-size touch target for detail-page inline actions. */
@Composable
internal fun DetailTextAction(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alignStart: Boolean = false,
    color: Color = MiuixTheme.colorScheme.primary,
    iconRotation: Float = 0f,
    iconSize: Dp = 16.dp,
    textStyle: TextStyle = MiuixTheme.textStyles.footnote1.copy(fontSize = 13.sp, lineHeight = 18.sp),
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        minHeight = 48.dp,
        insideMargin = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(color = Color.Transparent, contentColor = color),
    ) {
        Icon(symbolPainterResource(iconRes), contentDescription = null, modifier = Modifier.size(iconSize).rotate(iconRotation))
        Spacer(Modifier.width(6.dp))
        Text(label, style = textStyle)
        if (alignStart) Spacer(Modifier.weight(1f))
    }
}
