package com.aritxonly.deadliner.ui.detail

import com.aritxonly.deadliner.ui.base.feedback.miuixPressFeedback
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.iconResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DetailPlanFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.detail_plan_add)
    val resolvedModifier = modifier.semantics { contentDescription = label }
    ExtendedFloatingActionButton(
        modifier = (resolvedModifier).miuixPressFeedback(enabled = true, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        onClick = onClick,
        icon = {
            Icon(
                painter = iconResource(R.drawable.msr_add_wght400),
                contentDescription = null,
            )
        },
        text = { Text(text = label) },
    )
}
