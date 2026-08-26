package com.aritxonly.deadliner.ui.detail

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.iconResource
import com.aritxonly.deadliner.ui.material.glass.LocalImmersiveExperienceEnabled
import com.aritxonly.deadliner.ui.material.glass.SoftGlassFloatingActionButton
import com.aritxonly.deadliner.ui.material.glass.SoftGlassFloatingActionButtonDefaults

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DetailPlanFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.detail_plan_add)
    val resolvedModifier = modifier.semantics { contentDescription = label }
    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = iconResource(R.drawable.ic_add),
            contentDescription = null,
            modifier = Modifier.size(SoftGlassFloatingActionButtonDefaults.IconSize),
        )
    }

    if (LocalImmersiveExperienceEnabled.current) {
        SoftGlassFloatingActionButton(
            onClick = onClick,
            modifier = resolvedModifier,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            content = icon,
        )
    } else {
        ExtendedFloatingActionButton(
            modifier = resolvedModifier,
            shape = RoundedCornerShape(20.dp),
            onClick = onClick,
            icon = icon,
            text = { Text(text = label) },
        )
    }
}
