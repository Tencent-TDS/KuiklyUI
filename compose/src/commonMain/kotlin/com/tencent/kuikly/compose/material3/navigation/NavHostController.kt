/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.material3.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue

/**
 * NavHostController - Cross-platform navigation controller.
 *
 * Migrated from official `androidx.navigation.NavHostController`.
 * This implementation does not depend on any Android-specific APIs and can run in commonMain.
 *
 * It manages a back stack of [NavBackStackEntry] and provides navigation methods
 * such as [navigate], [popBackStack], and [navigateUp].
 */
@Stable
class NavHostController internal constructor() {

    /**
     * The internal back stack. The last element is the currently visible destination.
     */
    internal val backStack = mutableStateListOf<NavBackStackEntry>()

    /**
     * The registered destinations (route pattern -> NavDestination).
     */
    internal val destinations = mutableMapOf<String, NavDestination>()

    /**
     * The current back stack entry (top of the stack).
     */
    val currentBackStackEntry: NavBackStackEntry?
        get() = backStack.lastOrNull()

    /**
     * The current destination route.
     */
    val currentRoute: String?
        get() = currentBackStackEntry?.route

    /**
     * The number of entries in the back stack.
     */
    val backStackSize: Int
        get() = backStack.size

    /**
     * Whether we can navigate back (i.e., back stack has more than one entry).
     */
    val canNavigateBack: Boolean
        get() = backStack.size > 1

    /**
     * Navigate to the specified [route].
     *
     * @param route The destination route string (e.g. "detail/123")
     * @param builder Optional lambda to configure navigation options
     */
    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
        val options = NavOptionsBuilder().apply(builder).build()
        val destination = findDestination(route)

        if (destination == null) {
            // If no destination found, ignore silently
            return
        }

        val args = destination.matchRoute(route) ?: emptyMap()
        val entry = NavBackStackEntry(
            route = route,
            destination = destination,
            arguments = args
        )

        // Handle popUpTo if specified
        if (options.popUpTo != null) {
            popBackStackInternal(options.popUpTo, options.popUpToInclusive)
        }

        // Handle launchSingleTop: only deduplicate if the current top is the same destination
        if (options.launchSingleTop) {
            val topEntry = backStack.lastOrNull()
            if (topEntry != null && topEntry.destination.route == destination.route) {
                // Replace the top entry with new arguments
                backStack.removeAt(backStack.size - 1)
                backStack.add(entry)
                return
            }
        }

        backStack.add(entry)
    }

    /**
     * Pop the back stack, removing the top entry.
     *
     * @return true if a back stack entry was popped, false if the back stack was empty or
     *         had only one entry (the start destination).
     */
    fun popBackStack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.size - 1)
        return true
    }

    /**
     * Pop the back stack to the specified [route].
     *
     * @param route The route to pop back to
     * @param inclusive Whether to also pop the specified route
     * @return true if the back stack was modified
     */
    fun popBackStack(route: String, inclusive: Boolean = false): Boolean {
        return popBackStackInternal(route, inclusive)
    }

    /**
     * Navigate up (equivalent to popBackStack).
     *
     * @return true if navigation was handled
     */
    fun navigateUp(): Boolean = popBackStack()

    /**
     * Navigate to a route and clear the entire back stack, then push the new destination.
     *
     * @param route The destination route
     */
    fun navigateAndClearBackStack(route: String) {
        navigate(route) {
            popUpTo("") { inclusive = true }
        }
    }

    /**
     * Find a destination that matches the given actual route.
     */
    internal fun findDestination(actualRoute: String): NavDestination? {
        // First try exact match
        destinations[actualRoute]?.let { return it }
        // Then try pattern matching
        for ((_, dest) in destinations) {
            if (dest.matchRoute(actualRoute) != null) {
                return dest
            }
        }
        return null
    }

    /**
     * Register a destination.
     */
    internal fun addDestination(destination: NavDestination) {
        destinations[destination.route] = destination
    }

    /**
     * Internal pop back stack implementation.
     */
    private fun popBackStackInternal(route: String, inclusive: Boolean): Boolean {
        if (route.isEmpty()) {
            // Pop to root
            if (inclusive) {
                backStack.clear()
            } else if (backStack.size > 1) {
                while (backStack.size > 1) {
                    backStack.removeAt(backStack.size - 1)
                }
            }
            return true
        }

        val targetIndex = backStack.indexOfLast { entry ->
            entry.route == route || entry.destination.route == route
        }

        if (targetIndex < 0) return false

        val removeFrom = if (inclusive) targetIndex else targetIndex + 1
        while (backStack.size > removeFrom) {
            backStack.removeAt(backStack.size - 1)
        }
        return true
    }
}

/**
 * Navigation options builder.
 *
 * Mimics the official `NavOptionsBuilder` API for configuring navigation behavior.
 */
class NavOptionsBuilder {
    internal var popUpTo: String? = null
    internal var popUpToInclusive: Boolean = false
    internal var launchSingleTop: Boolean = false

    /**
     * Pop up to a given destination before navigating.
     *
     * @param route The route to pop up to
     * @param builder Configure inclusive behavior
     */
    fun popUpTo(route: String, builder: PopUpToBuilder.() -> Unit = {}) {
        popUpTo = route
        PopUpToBuilder().apply(builder).let {
            popUpToInclusive = it.inclusive
        }
    }

    /**
     * Whether the new destination should be launched as single top
     * (reusing an existing instance at the top of the back stack).
     */
    fun launchSingleTop(value: Boolean = true) {
        launchSingleTop = value
    }

    internal fun build(): NavOptions = NavOptions(
        popUpTo = popUpTo,
        popUpToInclusive = popUpToInclusive,
        launchSingleTop = launchSingleTop
    )
}

/**
 * Builder for popUpTo configuration.
 */
class PopUpToBuilder {
    var inclusive: Boolean = false
}

/**
 * Internal navigation options data.
 */
internal data class NavOptions(
    val popUpTo: String? = null,
    val popUpToInclusive: Boolean = false,
    val launchSingleTop: Boolean = false
)

/**
 * Create and remember a [NavHostController].
 */
@Composable
fun rememberNavHostController(): NavHostController {
    return androidx.compose.runtime.remember { NavHostController() }
}
