package com.aritxonly.deadliner.ui.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import com.aritxonly.deadliner.ui.category.CategoryPresentation
import com.aritxonly.deadliner.ui.category.CategoryPresentationRegistry
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.materialSymbolPainter

/** A selected category keeps its own glyph, foreground and tinted capsule. */
@Composable
internal fun QuickAddCategoryChip(
    category: CategoryPresentation,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val categoryColor = CategoryPresentationRegistry.color(category.colorToken)
    val foreground = categoryColor.copy(alpha = if (enabled) 1f else 0.38f)
    Row(modifier.clip(CircleShape).background(categoryColor.copy(alpha = 0.15f)), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f, fill = false).clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 40.dp).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(iconResource(CategoryPresentationRegistry.icon(category.iconSemanticId)), null,
                Modifier.size(16.dp).testTag("quick-category-icon-${category.uid}"), tint = foreground)
            Text(category.name, style = MaterialTheme.typography.labelMedium, color = foreground,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(40.dp).miuixPressFeedback(enabled = enabled, overlay = false).clip(CircleShape)
            .clickable(enabled = enabled, onClick = onRemove), contentAlignment = Alignment.Center) {
            Icon(materialSymbolPainter("close"), "移除${category.name}", Modifier.size(16.dp), tint = foreground)
        }
    }
}
