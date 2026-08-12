package com.aritxonly.deadliner.model

enum class UiStyle(val key: String) {
    Simplified("simplified"),
    Miuix("miuix");

    companion object {
        fun fromKey(k: String?): UiStyle = when (k) {
            Simplified.key -> Simplified
            Miuix.key -> Miuix
            else -> Simplified
        }
    }
}
