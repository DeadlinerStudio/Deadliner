package com.aritxonly.deadliner.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import com.aritxonly.deadliner.SettingsRoute
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey

internal data class SettingsPage(val route: String) : NavKey

class SettingsNavigator internal constructor(internal val backStack: NavBackStack) {
    val currentRoute: String get() = (backStack.last() as SettingsPage).route

    fun navigate(route: String) {
        val existing = backStack.indexOfFirst { (it as SettingsPage).route == route }
        if (existing >= 0) {
            while (backStack.lastIndex > existing) backStack.removeAt(backStack.lastIndex)
        } else {
            backStack.add(SettingsPage(route))
        }
    }

    fun navigateUp(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
}

@Composable
internal fun rememberSettingsNavigator(initialRoute: String): SettingsNavigator {
    val stack = rememberSaveable(saver = listSaver<NavBackStack, String>(
        save = { entries -> entries.map { (it as SettingsPage).route } },
        restore = { routes -> routes.map { SettingsPage(it) as NavKey }.toMutableStateList() },
    )) {
        buildList<NavKey> {
            add(SettingsPage(SettingsRoute.Main.route))
            if (initialRoute != SettingsRoute.Main.route) add(SettingsPage(initialRoute))
        }.toMutableStateList()
    }
    return remember(stack) { SettingsNavigator(stack) }
}
