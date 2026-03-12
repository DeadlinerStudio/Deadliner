package com.aritxonly.deadliner.model

enum class UiStyle(val key: String) {
    Classic("classic"),
    Simplified("simplified"),
    Miuix("miuix");

    companion object {
        fun fromKey(k: String?): UiStyle = when (k) {
            Classic.key -> Classic
            Simplified.key -> Simplified
            Miuix.key -> Miuix
            else -> Simplified
        }
    }
}