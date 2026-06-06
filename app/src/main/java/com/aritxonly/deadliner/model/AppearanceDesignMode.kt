package com.aritxonly.deadliner.model

enum class AppearanceDesignMode(val key: String) {
    Modern("modern"),
    Vivid("vivid");

    companion object {
        fun fromKey(value: String?): AppearanceDesignMode = when (value) {
            Modern.key -> Modern
            Vivid.key -> Vivid
            else -> Modern
        }
    }
}
