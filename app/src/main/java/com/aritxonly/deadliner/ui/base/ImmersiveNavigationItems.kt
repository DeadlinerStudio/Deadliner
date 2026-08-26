package com.aritxonly.deadliner.ui.base

internal data class ImmersiveNavigationPartition(
    val primaryItems: List<AdaptiveNavItem>,
    val detachedItem: AdaptiveNavItem?,
)

internal fun partitionImmersiveNavigationItems(
    items: List<AdaptiveNavItem>,
    detachedItemKey: String?,
): ImmersiveNavigationPartition {
    if (detachedItemKey == null) {
        return ImmersiveNavigationPartition(primaryItems = items, detachedItem = null)
    }
    val detachedItem = items.firstOrNull { it.key == detachedItemKey }
    return ImmersiveNavigationPartition(
        primaryItems = items.filterNot { it.key == detachedItemKey },
        detachedItem = detachedItem,
    )
}

internal fun resolveImmersivePrimarySelectedKey(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
): String = items.firstOrNull { it.key == selectedKey && it.enabled }?.key
    ?: items.firstOrNull { it.enabled }?.key
    ?: items.firstOrNull()?.key
    ?: selectedKey

internal fun isImmersivePrimarySelectionVisible(
    items: List<AdaptiveNavItem>,
    selectedKey: String,
): Boolean = items.any { it.key == selectedKey && it.enabled }

internal fun resolveImmersiveNavigationIcon(item: AdaptiveNavItem) =
    item.immersiveIcon ?: item.unselectedIcon
