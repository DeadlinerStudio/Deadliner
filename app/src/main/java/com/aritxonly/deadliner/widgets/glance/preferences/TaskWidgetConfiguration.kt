package com.aritxonly.deadliner.widgets.glance.preferences

internal enum class TaskWidgetKind {
    SQUARE,
    CAPSULE,
}

internal enum class TaskWidgetColorScheme(val storedValue: String) {
    FOLLOW_APP("follow_app"),
    BLUE("blue"),
    PURPLE("purple"),
    GREEN("green"),
    ORANGE("orange"),
    GRAPHITE("graphite");

    companion object {
        fun fromStoredValue(value: String?): TaskWidgetColorScheme =
            entries.firstOrNull { it.storedValue == value } ?: FOLLOW_APP
    }
}

internal enum class TaskWidgetThemeMode(val storedValue: String) {
    FOLLOW_APP("follow_app"),
    LIGHT("light"),
    DARK("dark");

    fun isDark(followAppIsDark: Boolean): Boolean = when (this) {
        FOLLOW_APP -> followAppIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStoredValue(value: String?): TaskWidgetThemeMode =
            entries.firstOrNull { it.storedValue == value } ?: FOLLOW_APP
    }
}

internal data class TaskWidgetConfiguration(
    val cornerRadiusDp: Int,
    val marginPreset: TaskWidgetPaddingPreset,
    val customTopMarginDp: Int,
    val customBottomMarginDp: Int,
    val colorScheme: TaskWidgetColorScheme,
    val isScrollable: Boolean,
    val backgroundTransparencyPercent: Int,
    val themeMode: TaskWidgetThemeMode = TaskWidgetThemeMode.FOLLOW_APP,
) {
    fun backgroundAlpha(): Float = (100 - backgroundTransparencyPercent.coerceIn(0, 100)) / 100f

    fun topMarginDp(): Int = when (marginPreset) {
        TaskWidgetPaddingPreset.Custom -> customTopMarginDp
        else -> marginPreset.cardTopMarginDp
    }

    fun bottomMarginDp(): Int = when (marginPreset) {
        TaskWidgetPaddingPreset.Custom -> customBottomMarginDp
        else -> marginPreset.cardBottomMarginDp
    }

    fun sanitized(kind: TaskWidgetKind): TaskWidgetConfiguration {
        val limits = TaskWidgetConfigurationLimits.forKind(kind)
        val allowedPreset = if (kind == TaskWidgetKind.CAPSULE) {
            TaskWidgetPaddingPreset.Custom
        } else {
            marginPreset
        }
        val top = customTopMarginDp.coerceIn(0, limits.maxSingleMarginDp)
        val bottom = customBottomMarginDp
            .coerceIn(0, limits.maxSingleMarginDp)
            .coerceAtMost((limits.maxCombinedMarginDp - top).coerceAtLeast(0))
        return copy(
            cornerRadiusDp = cornerRadiusDp.coerceIn(limits.minCornerRadiusDp, limits.maxCornerRadiusDp),
            marginPreset = allowedPreset,
            customTopMarginDp = top,
            customBottomMarginDp = bottom,
            isScrollable = kind == TaskWidgetKind.SQUARE && isScrollable,
            backgroundTransparencyPercent = backgroundTransparencyPercent.coerceIn(0, 100),
        )
    }

    companion object {
        fun defaultFor(
            kind: TaskWidgetKind,
            squarePreset: TaskWidgetPaddingPreset = TaskWidgetPaddingPreset.Standard,
        ): TaskWidgetConfiguration = when (kind) {
            TaskWidgetKind.SQUARE -> TaskWidgetConfiguration(
                cornerRadiusDp = 22,
                marginPreset = squarePreset.takeUnless { it == TaskWidgetPaddingPreset.Custom }
                    ?: TaskWidgetPaddingPreset.Standard,
                customTopMarginDp = 0,
                customBottomMarginDp = 0,
                colorScheme = TaskWidgetColorScheme.FOLLOW_APP,
                isScrollable = false,
                backgroundTransparencyPercent = 8,
            )

            TaskWidgetKind.CAPSULE -> TaskWidgetConfiguration(
                cornerRadiusDp = 22,
                marginPreset = TaskWidgetPaddingPreset.Custom,
                customTopMarginDp = 0,
                customBottomMarginDp = 0,
                colorScheme = TaskWidgetColorScheme.FOLLOW_APP,
                isScrollable = false,
                backgroundTransparencyPercent = 0,
            )
        }
    }
}

internal data class TaskWidgetConfigurationLimits(
    val minCornerRadiusDp: Int,
    val maxCornerRadiusDp: Int,
    val maxSingleMarginDp: Int,
    val maxCombinedMarginDp: Int,
) {
    companion object {
        private val SharedLimits = TaskWidgetConfigurationLimits(
            minCornerRadiusDp = 8,
            maxCornerRadiusDp = 32,
            maxSingleMarginDp = 28,
            maxCombinedMarginDp = 40,
        )

        fun forKind(kind: TaskWidgetKind): TaskWidgetConfigurationLimits = when (kind) {
            TaskWidgetKind.SQUARE,
            TaskWidgetKind.CAPSULE,
            -> SharedLimits
        }
    }
}
