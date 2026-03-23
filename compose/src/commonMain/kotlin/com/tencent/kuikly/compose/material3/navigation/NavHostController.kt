/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.material3.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.tencent.kuikly.core.bundle.Bundle

/**
 * Controller for the [NavHost] composable. It manages a back stack of [NavBackStackEntry]
 * instances and provides methods for navigating between destinations.
 *
 * The NavHostController maintains the state of the navigation graph and enables forward
 * and backward navigation via [navigate], [popBackStack], and [navigateUp].
 *
 * It also manages the [NavigatorProvider] which holds all registered [Navigator]s,
 * and supports [NavGraph] hierarchies, deep link navigation, and state save/restore.
 *
 * This implementation does not depend on any Android-specific APIs and can run in commonMain.
 *
 * @see NavHost
 * @see NavGraphBuilder
 * @see rememberNavController
 * @see NavigatorProvider
 */
@Stable
class NavHostController internal constructor() {
    /**
     * The [NavigatorProvider] for this controller. Extension functions like
     * [NavigatorProvider.navigation] use this to build navigation graphs.
     *
     * The provider is pre-populated with [ComposeNavigator] and [NavGraphNavigator].
     */
    val navigatorProvider: NavigatorProvider = NavigatorProvider().apply {
        addNavigator(ComposeNavigator())
        addNavigator(NavGraphNavigator(this))
    }

    /**
     * The internal back stack. The last element is the currently visible destination.
     */
    internal val backStack = mutableStateListOf<NavBackStackEntry>()

    /**
     * The root navigation graph. Set by [NavHost] after building.
     */
    var graph: NavGraph? = null
        set(value) {
            field = value
            // Initialize navigator states when graph is set
            initializeNavigatorStates()
        }
    
    /**
     * Initialize states for navigators that need separate state management.
     */
    private fun initializeNavigatorStates() {
        // Initialize DialogNavigator state
        runCatching {
            val dialogNavigator = navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME)
            if (dialogNavigator._state == null) {
                dialogNavigator._state = NavigatorState()
            }
        }
    }

    /**
     * Saved state for destinations that were popped with saveState = true.
     * Keyed by destination route pattern.
     */
    private val savedStates = mutableMapOf<String, List<NavBackStackEntry>>()

    /**
     * The current back stack entry (top of the stack), or null if the back stack is empty.
     */
    val currentBackStackEntry: NavBackStackEntry?
        get() = backStack.lastOrNull()

    /**
     * The previous back stack entry (second from the top), or null if there are fewer
     * than two entries.
     *
     * This is commonly used for passing results back to the previous screen:
     * ```kotlin
     * // In detail screen: set result before navigating back
     * navController.previousBackStackEntry?.savedState?.set("result", "some_value")
     * navController.popBackStack()
     *
     * // In home screen: read the result
     * val result = entry.savedState?.get("result") as? String
     * ```
     */
    val previousBackStackEntry: NavBackStackEntry?
        get() = if (backStack.size >= 2) backStack[backStack.size - 2] else null

    /**
     * The current destination route, or null if the back stack is empty.
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
     * If the route matches a [NavGraph], the navigation will automatically redirect
     * to the graph's start destination.
     *
     * @param route The destination route string (e.g. "detail/123" or "auth")
     * @param builder Optional lambda to configure [NavOptionsBuilder] for this navigation
     *
     * @see NavOptionsBuilder
     * @see NavOptionsBuilder.popUpTo
     * @see NavOptionsBuilder.launchSingleTop
     */
    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
        val options = NavOptionsBuilder().apply(builder).build()

        // Check if route is a nested NavGraph route; if so, resolve to its start destination
        val resolvedRoute = graph?.resolveStartDestination(route) ?: route
        val destination = findDestination(resolvedRoute)

        if (destination == null) {
            // If no destination found, ignore silently
            return
        }

        val args = destination.matchRoute(resolvedRoute) ?: Bundle()

        // Handle popUpTo if specified
        if (options.popUpTo != null) {
            popBackStackInternal(options.popUpTo, options.popUpToInclusive, options.saveState)
        }

        // Handle restoreState: try to restore previously saved state
        if (options.restoreState) {
            val restored = savedStates.remove(destination.route)
            if (restored != null) {
                backStack.addAll(restored)
                return
            }
        }

        val entry = NavBackStackEntry.create(
            destination = destination,
            arguments = args
        )
        entry.route = resolvedRoute

        // Handle launchSingleTop: only deduplicate if the current top is the same destination
        if (options.launchSingleTop) {
            val topEntry = backStack.lastOrNull()
            if (topEntry != null && topEntry.destination.route == destination.route) {
                // Replace the top entry with new arguments (but keep the same sequence number for consistency)
                backStack.removeAt(backStack.size - 1)
                backStack.add(entry)
                return
            }
        }

        backStack.add(entry)
        
        // If this is a dialog destination, also push to DialogNavigator's backStack
        if (destination.navigatorName == DialogNavigator.NAME) {
            runCatching {
                val dialogNavigator = navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME)
                dialogNavigator.dialogState.push(entry)
            }
        }
    }

    /**
     * Attempts to navigate up in the navigation hierarchy.
     *
     * @return true if navigation was handled, false otherwise
     */
    fun navigateUp(): Boolean = popBackStack()

    /**
     * Pop the back stack, removing the top entry.
     *
     * @return true if a back stack entry was popped, false if the back stack was empty or
     *         had only one entry (the start destination).
     */
    fun popBackStack(): Boolean {
        if (backStack.size <= 1) return false
        val poppedEntry = backStack.removeAt(backStack.size - 1)
        
        // If popped entry was a dialog, also pop from DialogNavigator's backStack
        if (poppedEntry.destination.navigatorName == DialogNavigator.NAME) {
            runCatching {
                val dialogNavigator = navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME)
                dialogNavigator.dialogState.pop(poppedEntry, false)
            }
        }
        
        return true
    }

    /**
     * Pop the back stack to the specified [route].
     *
     * @param route The route to pop back to
     * @param inclusive Whether to also pop the specified route itself
     * @param saveState Whether to save the state of the popped destinations for later restoration
     * @return true if the back stack was modified
     */
    fun popBackStack(route: String, inclusive: Boolean = false, saveState: Boolean = false): Boolean {
        return popBackStackInternal(route, inclusive, saveState)
    }

    /**
     * Navigate to a route and clear the entire back stack, then push the new destination.
     * This is a convenience method equivalent to:
     * ```kotlin
     * navigate(route) {
     *     popUpTo("") { inclusive = true }
     * }
     * ```
     *
     * @param route The destination route
     */
    fun navigateAndClearBackStack(route: String) {
        navigate(route) {
            popUpTo("") { inclusive = true }
        }
    }

    /**
     * Gets the topmost [NavBackStackEntry] for a destination matching the given [route].
     *
     * This is useful for accessing the saved state of a specific destination in the back stack,
     * such as passing results between screens.
     *
     * @param route The destination route to search for
     * @return The [NavBackStackEntry] for the given route
     * @throws IllegalArgumentException if no entry matching the route is found in the back stack
     */
    fun getBackStackEntry(route: String): NavBackStackEntry {
        return backStack.lastOrNull { entry ->
            entry.route == route || entry.destination.route == route
        } ?: throw IllegalArgumentException(
            "No destination with route \"$route\" is on the NavController's back stack. " +
                "The current back stack contains routes: ${backStack.map { it.route }}"
        )
    }

    /**
     * Handle a deep link URI. If a matching destination is found, navigates to it.
     *
     * @param uri The deep link URI to handle
     * @return true if the deep link was successfully handled
     */
    fun handleDeepLink(uri: String): Boolean {
        val rootGraph = graph ?: return false
        for (dest in rootGraph) {
            val args = dest.matchDeepLink(uri)
            if (args != null) {
                val entry = NavBackStackEntry.create(
                    destination = dest,
                    arguments = args
                )
                entry.route = dest.route
                backStack.add(entry)
                return true
            }
            // Also check nested graphs
            if (dest is NavGraph) {
                for (nestedDest in dest) {
                val nestedArgs = nestedDest.matchDeepLink(uri)
                    if (nestedArgs != null) {
                        val nestedEntry = NavBackStackEntry.create(
                            destination = nestedDest,
                            arguments = nestedArgs
                        )
                        nestedEntry.route = nestedDest.route
                        backStack.add(nestedEntry)
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Find a destination that matches the given actual route.
     *
     * Searches the root [graph] and all nested graphs.
     *
     * @param actualRoute The actual route string (e.g. "detail/123")
     * @return The matching [NavDestination], or null if not found
     */
    internal fun findDestination(actualRoute: String): NavDestination? {
        val rootGraph = graph ?: return null
        return rootGraph.findDestinationInternal(actualRoute)
    }

    /**
     * Internal pop back stack implementation.
     *
     * @param route The route to pop back to
     * @param inclusive Whether to pop the target route itself
     * @param saveState Whether to save the state of popped entries
     */
    private fun popBackStackInternal(
        route: String,
        inclusive: Boolean,
        saveState: Boolean = false
    ): Boolean {
        if (route.isEmpty()) {
            // Pop to root
            if (inclusive) {
                if (saveState && backStack.isNotEmpty()) {
                    val currentDest = backStack.last().destination.route ?: ""
                    savedStates[currentDest] = backStack.toList()
                }
                backStack.clear()
            } else if (backStack.size > 1) {
                if (saveState) {
                    val currentDest = backStack.last().destination.route ?: ""
                    savedStates[currentDest] = backStack.drop(1).toList()
                }
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

        if (saveState && backStack.size > removeFrom) {
            val topDest = backStack.last().destination.route ?: ""
            savedStates[topDest] = backStack.subList(removeFrom, backStack.size).toList()
        }

        // Pop entries and update DialogNavigator's backStack
        while (backStack.size > removeFrom) {
            val poppedEntry = backStack.removeAt(backStack.size - 1)
            if (poppedEntry.destination.navigatorName == DialogNavigator.NAME) {
                runCatching {
                    val dialogNavigator = navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME)
                    dialogNavigator.dialogState.pop(poppedEntry, false)
                }
            }
        }
        return true
    }
}

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the route for the start destination
 * @param route the route for the graph
 * @param builder the builder used to construct the graph
 */
inline fun NavHostController.createGraph(
    startDestination: String,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(startDestination, route, builder)

/**
 * DSL for constructing navigation options when calling [NavHostController.navigate].
 *
 * Provides configuration for navigation behavior such as single-top launch mode,
 * pop-up-to behavior, and state save/restore.
 *
 * Usage:
 * ```kotlin
 * navController.navigate("detail/123") {
 *     popUpTo("home") {
 *         inclusive = false
 *         saveState = true
 *     }
 *     launchSingleTop = true
 *     restoreState = true
 * }
 * ```
 *
 * @see NavHostController.navigate
 */
class NavOptionsBuilder {
    @PublishedApi internal var popUpTo: String? = null
    @PublishedApi internal var popUpToInclusive: Boolean = false

    /**
     * Whether the new destination should be launched as single top, meaning if an existing
     * instance of this destination is already on top of the back stack, it will be reused
     * rather than creating a new instance.
     */
    var launchSingleTop: Boolean = false

    /**
     * Whether the back stack and the state of all destinations between the current destination
     * and [popUpTo] should be saved for later restoration via [restoreState].
     *
     * This should be used in conjunction with [restoreState] to allow switching between
     * destinations while preserving their state (commonly used with BottomNavigation).
     */
    @PublishedApi internal var saveState: Boolean = false

    /**
     * Whether this navigation action should restore any state previously saved by
     * [PopUpToBuilder.saveState] or [saveState].
     *
     * If no state was previously saved, this has no effect.
     */
    var restoreState: Boolean = false

    /**
     * Pop up to a given destination before navigating. This pops all non-matching destinations
     * from the back stack until the destination with [route] is found.
     *
     * @param route The route to pop up to
     * @param builder Configure the pop-up behavior (inclusive, saveState)
     */
    inline fun popUpTo(route: String, builder: PopUpToBuilder.() -> Unit = {}) {
        popUpTo = route
        PopUpToBuilder().apply(builder).let {
            popUpToInclusive = it.inclusive
            saveState = it.saveState
        }
    }

    internal fun build(): NavOptions = NavOptions(
        popUpTo = popUpTo,
        popUpToInclusive = popUpToInclusive,
        launchSingleTop = launchSingleTop,
        restoreState = restoreState,
        saveState = saveState
    )
}

/**
 * Builder for configuring [NavOptionsBuilder.popUpTo] behavior.
 */
class PopUpToBuilder {
    /**
     * Whether the target destination should also be popped from the back stack.
     */
    var inclusive: Boolean = false

    /**
     * Whether the back stack and the state of all popped destinations should be saved
     * for later restoration via [NavOptionsBuilder.restoreState].
     */
    var saveState: Boolean = false
}

/**
 * Navigation options data class for configuring navigation behavior.
 */
data class NavOptions(
    val popUpTo: String? = null,
    val popUpToInclusive: Boolean = false,
    val launchSingleTop: Boolean = false,
    val restoreState: Boolean = false,
    val saveState: Boolean = false
)

/**
 * Creates a NavHostController that handles the adding of the [ComposeNavigator] and
 * [DialogNavigator]. Additional [Navigator] instances can be passed through [navigators] to
 * be applied to the returned NavController. Note that each [Navigator] must be separately
 * remembered before being passed in here: any changes to those inputs will cause the
 * NavController to be recreated.
 *
 * @param navigators Additional [Navigator] instances to be added to the NavController
 * @return A new or remembered [NavHostController] instance
 *
 * @see NavHost
 * @see NavHostController
 */
@Composable
fun rememberNavController(vararg navigators: Navigator<out NavDestination>): NavHostController {
    return remember { 
        NavHostController().apply {
            // Always register default navigators
            navigatorProvider.addNavigator(ComposeNavigator())
            navigatorProvider.addNavigator(DialogNavigator())
            // Add any additional navigators passed by the user
            navigators.forEach { navigator ->
                navigatorProvider.addNavigator(navigator)
            }
        }
    }
}

/**
 * Gets the current navigation back stack entry as a [State].
 *
 * This function is commonly used with BottomNavigation / NavigationBar to
 * observe the current destination and highlight the active tab:
 *
 * ```kotlin
 * val navBackStackEntry by navController.currentBackStackEntryAsState()
 * val currentRoute = navBackStackEntry?.destination?.route
 *
 * NavigationBar {
 *     items.forEach { screen ->
 *         NavigationBarItem(
 *             selected = currentRoute == screen.route,
 *             onClick = {
 *                 navController.navigate(screen.route) {
 *                     popUpTo(navController.graph?.startDestinationRoute ?: "") {
 *                         saveState = true
 *                     }
 *                     launchSingleTop = true
 *                     restoreState = true
 *                 }
 *             },
 *             icon = { Icon(screen.icon, contentDescription = screen.title) },
 *             label = { Text(screen.title) }
 *         )
 *     }
 * }
 * ```
 *
 * @return A [State] containing the current [NavBackStackEntry], or null if the back stack is empty.
 */
@Composable
fun NavHostController.currentBackStackEntryAsState(): State<NavBackStackEntry?> {
    return remember(this) {
        derivedStateOf { this.currentBackStackEntry }
    }
}
