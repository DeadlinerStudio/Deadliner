package com.aritxonly.deadliner.model

internal fun resolveImmersiveExperiencePreference(
    currentValue: Boolean?,
    legacyFloatingTabBarValue: Boolean?,
): Boolean = currentValue ?: legacyFloatingTabBarValue ?: false
