package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.materialSymbolPainter
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator

@Composable
internal fun UpdateVersionHeader(currentVersion: String, state: UpdateState) {
    val colors = MaterialTheme.colorScheme
    val available = state as? UpdateState.Available
    Column(Modifier.fillMaxWidth().padding(top = if (available == null) 44.dp else 12.dp,
        bottom = if (available == null) 32.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (available != null) {
            Row(Modifier.background(colors.primary.copy(alpha = 0.10f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(materialSymbolPainter("mobile_arrow_down"), null, Modifier.size(14.dp), tint = colors.primary)
                Text(stringResource(R.string.update_available_label), style = MaterialTheme.typography.labelSmall, color = colors.primary)
            }
            Spacer(Modifier.height(10.dp))
            Text(available.latest.removePrefix("v"), style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold, color = colors.onSurface, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            if (state == UpdateState.Loading) {
                CircularProgressIndicator(size = 24.dp, strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                if (state is UpdateState.UpToDate) {
                    Icon(materialSymbolPainter("check_circle"), null, Modifier.size(26.dp), tint = colors.primary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(when (state) {
                    is UpdateState.UpToDate -> R.string.update_latest_label
                    is UpdateState.Error -> R.string.update_error_label
                    else -> R.string.update_checking_label
                }), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(8.dp))
        val current = available?.current ?: (state as? UpdateState.UpToDate)?.current ?: currentVersion
        Text(stringResource(R.string.check_for_updates_current, current),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
