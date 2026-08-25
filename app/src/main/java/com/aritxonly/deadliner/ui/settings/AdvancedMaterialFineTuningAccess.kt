package com.aritxonly.deadliner.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class VersionTapUnlockCounter(
    private val requiredTaps: Int = RequiredVersionTaps,
) {
    private var tapCount = 0

    init {
        require(requiredTaps > 0)
    }

    fun registerTap(): Boolean {
        tapCount = (tapCount + 1).coerceAtMost(requiredTaps)
        return tapCount >= requiredTaps
    }
}

internal object AdvancedMaterialFineTuningAccess {
    private val counter = VersionTapUnlockCounter()

    var isUnlocked by mutableStateOf(false)
        private set

    fun registerVersionTap(): Boolean {
        if (!isUnlocked && counter.registerTap()) {
            isUnlocked = true
        }
        return isUnlocked
    }
}

private const val RequiredVersionTaps = 10
