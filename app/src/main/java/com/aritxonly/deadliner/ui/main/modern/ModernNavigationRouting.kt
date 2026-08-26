package com.aritxonly.deadliner.ui.main.modern

import com.aritxonly.deadliner.ui.main.shared.MainSection

internal fun mainSectionNavigationKey(section: MainSection): String = when (section) {
    MainSection.LIST -> "list"
    MainSection.OVERVIEW -> "overview"
    MainSection.CAPTURE -> "capture"
    MainSection.LIFI -> "lifi"
}

internal fun mainSectionForNavigationKey(key: String): MainSection? = when (key) {
    "list" -> MainSection.LIST
    "overview" -> MainSection.OVERVIEW
    "capture" -> MainSection.CAPTURE
    "lifi" -> MainSection.LIFI
    else -> null
}
