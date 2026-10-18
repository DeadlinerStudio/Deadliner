package com.aritxonly.deadliner.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.materialSymbolPainter

/** Share category glyphs and colors with the existing badges and icon-pack mappings. */
@Composable
internal fun CategoryDropdownIcon(category: CategoryPresentation?, management: Boolean = false) {
    val color = category?.let { CategoryPresentationRegistry.color(it.colorToken) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant
    Box(Modifier.size(24.dp).testTag("category-icon-${category?.uid ?: if (management) "manage" else "uncategorized"}")
        .background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Icon(if (category != null) iconResource(CategoryPresentationRegistry.icon(category.iconSemanticId))
            else materialSymbolPainter(if (management) "settings" else "folder"), null,
            Modifier.size(16.dp), tint = color)
    }
}
