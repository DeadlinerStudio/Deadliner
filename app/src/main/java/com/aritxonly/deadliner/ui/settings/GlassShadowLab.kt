package com.aritxonly.deadliner.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.GlassShadowPreset
import com.aritxonly.deadliner.ui.material.glass.SoftGlassShadowProfile
import com.aritxonly.deadliner.ui.material.glass.drawSoftGlassShadow

@Composable
internal fun GlassShadowLab(
    selected: GlassShadowPreset,
    applicable: Boolean,
    onSelected: (GlassShadowPreset) -> Unit,
) {
    SettingsSection(topLabel = stringResource(R.string.settings_glass_shadow_lab)) {
        SettingsDropdownItem(
            title = stringResource(R.string.settings_glass_shadow_preset),
            options = GlassShadowPreset.entries.map { RadioOption(it, it.labelRes) },
            selectedKey = selected,
            onOptionSelected = onSelected,
        )
        Text(stringResource(selected.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SettingsItemDefaults.HorizontalPadding, vertical = 4.dp))
        GlassShadowSample(selected)
        Text(stringResource(if (applicable) R.string.settings_glass_shadow_hint
            else R.string.settings_glass_shadow_inactive_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SettingsItemDefaults.HorizontalPadding)
                .padding(bottom = 20.dp))
    }
}

/** Intentionally isolate the light shadow on white, independent of the active page/theme. */
@Composable
internal fun GlassShadowSample(preset: GlassShadowPreset, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(22.dp)
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
        .background(Color.White, RoundedCornerShape(20.dp))
        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
        .height(120.dp).testTag("shadow-sample"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp)
            .drawSoftGlassShadow(CircleShape, false, 1f, false,
                SoftGlassShadowProfile.TopBarControl, preset)
            .background(Color(0xFFF9F9F9), CircleShape)
            .border(0.6.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Text("‹", color = Color(0xFF171717), style = MaterialTheme.typography.headlineSmall)
        }
        Row(Modifier.width(160.dp).height(44.dp)
            .drawSoftGlassShadow(pill, false, 1f, false, preset = preset)
            .background(Color(0xFFF9F9F9), pill).border(0.6.dp, Color.White, pill),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_glass_shadow_sample_tasks), color = Color(0xFF171717),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.background(Color(0xFFE9E9E9), pill).padding(horizontal = 16.dp, vertical = 8.dp))
            Text(stringResource(R.string.settings_glass_shadow_sample_overview), color = Color(0xFF777777),
                style = MaterialTheme.typography.labelMedium)
        }
    }
}

internal val GlassShadowPreset.labelRes: Int
    @StringRes get() = when (this) {
        GlassShadowPreset.BalancedDiffuse -> R.string.glass_shadow_balanced_diffuse
        GlassShadowPreset.Clean -> R.string.glass_shadow_clean
        GlassShadowPreset.Original -> R.string.glass_shadow_original
        GlassShadowPreset.Diffuse -> R.string.glass_shadow_diffuse
        GlassShadowPreset.Compact -> R.string.glass_shadow_compact
        GlassShadowPreset.Crisp -> R.string.glass_shadow_crisp
        GlassShadowPreset.Airy -> R.string.glass_shadow_airy
        GlassShadowPreset.CloseFloat -> R.string.glass_shadow_close_float
        GlassShadowPreset.Elevated -> R.string.glass_shadow_elevated
        GlassShadowPreset.SoftContact -> R.string.glass_shadow_soft_contact
        GlassShadowPreset.Minimal -> R.string.glass_shadow_minimal
    }

private val GlassShadowPreset.descriptionRes: Int
    @StringRes get() = when (this) {
        GlassShadowPreset.BalancedDiffuse -> R.string.glass_shadow_balanced_diffuse_description
        GlassShadowPreset.Clean -> R.string.glass_shadow_clean_description
        GlassShadowPreset.Original -> R.string.glass_shadow_original_description
        GlassShadowPreset.Diffuse -> R.string.glass_shadow_diffuse_description
        GlassShadowPreset.Compact -> R.string.glass_shadow_compact_description
        GlassShadowPreset.Crisp -> R.string.glass_shadow_crisp_description
        GlassShadowPreset.Airy -> R.string.glass_shadow_airy_description
        GlassShadowPreset.CloseFloat -> R.string.glass_shadow_close_float_description
        GlassShadowPreset.Elevated -> R.string.glass_shadow_elevated_description
        GlassShadowPreset.SoftContact -> R.string.glass_shadow_soft_contact_description
        GlassShadowPreset.Minimal -> R.string.glass_shadow_minimal_description
    }
