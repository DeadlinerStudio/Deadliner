package com.aritxonly.deadliner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Years

/** Optical sizes affect glyphs only; callers retain their existing interaction bounds. */
@Composable
internal fun interfaceIconSize(material: Dp, hybrid: Dp): Dp =
    if (LocalInterfaceIconPack.current == InterfaceIconPack.HybridMiuix) hybrid else material

@Composable
internal fun overviewChevronSize(original: Dp = 24.dp): Dp = interfaceIconSize(original, original * 0.5f)

/** Match HyperModifier's 28dp Regular Add, including its viewport padding. */
@Composable
internal fun homeAddPainter(): androidx.compose.ui.graphics.painter.Painter =
    if (LocalInterfaceIconPack.current == InterfaceIconPack.HybridMiuix)
        androidx.compose.ui.res.painterResource(com.aritxonly.deadliner.R.drawable.ic_miuix_home_add)
    else materialSymbolPainter("add")

/** Main list identity stays MIUIX in both icon packs; category homes use Symbols. */
@Composable
internal fun mainListHomePainter(): androidx.compose.ui.graphics.painter.Painter =
    rememberVectorPainter(MiuixIcons.Home)

@Composable
internal fun overviewMonthChevronPainter(previous: Boolean): androidx.compose.ui.graphics.painter.Painter =
    if (LocalInterfaceIconPack.current == InterfaceIconPack.HybridMiuix)
        androidx.compose.ui.res.painterResource(if (previous) com.aritxonly.deadliner.R.drawable.ic_miuix_overview_previous
            else com.aritxonly.deadliner.R.drawable.ic_miuix_overview_next)
    else materialSymbolPainter(if (previous) "chevron_left" else "chevron_right")

@Composable
internal fun quickAddExpandPainter(): androidx.compose.ui.graphics.painter.Painter =
    if (LocalInterfaceIconPack.current == InterfaceIconPack.HybridMiuix)
        androidx.compose.ui.res.painterResource(com.aritxonly.deadliner.R.drawable.ic_miuix_quick_expand)
    else materialSymbolPainter("open_in_full")

/** Calendar import is a calendar collection, distinct from a task's time attributes. */
@Composable
internal fun calendarImportPainter(): androidx.compose.ui.graphics.painter.Painter =
    if (LocalInterfaceIconPack.current == InterfaceIconPack.HybridMiuix)
        rememberVectorPainter(MiuixIcons.Years)
    else materialSymbolPainter("calendar_month")
