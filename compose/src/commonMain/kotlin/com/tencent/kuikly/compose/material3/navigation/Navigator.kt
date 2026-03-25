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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Navigator defines a mechanism for navigating within an app.
 *
 * Each Navigator sets the policy for a specific type of navigation, e.g.,
 * [ComposeNavigator] for Composable destinations or [NavGraphNavigator] for
 * nested navigation graphs.
 *
 * Navigators should be able to manage their own back stack when navigating between two
 * destinations that belong to that navigator. The [NavHostController] manages a back stack of
 * navigators representing the current navigation stack across all navigators.
 *
 * @param D the subclass of [NavDestination] used with this Navigator which can be used
 * to hold any special data that will be needed to navigate to that destination.
 *
 * @see ComposeNavigator
 * @see NavGraphNavigator
 * @see NavigatorProvider
 */
abstract class Navigator<D : NavDestination> {

    /**
     * The name of this navigator, used to register it with a [NavigatorProvider].
     *
     * Subclasses must provide a unique name via [name] that can be used to retrieve
     * this navigator from the [NavigatorProvider].
     */
    abstract val name: String

    /**
     * The state of the Navigator is the communication conduit between the Navigator
     * and the [NavHostController] that has called [onAttach].
     *
     * It is the responsibility of the Navigator to call [NavigatorState.push]
     * and [NavigatorState.pop] to in order to update the [NavigatorState.backStack] at
     * the appropriate times.
     *
     * @throws IllegalStateException if [isAttached] is `false`
     */
    protected abstract val state: NavigatorState

    /**
     * Whether this Navigator is actively being used by a [NavHostController].
     *
     * This is set to `true` when [onAttach] is called.
     */
    var isAttached: Boolean = false
        private set

    /**
     * Indicator that this Navigator is actively being used by a [NavHostController]. This
     * is called when the NavController's state is ready to be restored.
     */
    open fun onAttach(state: NavigatorState) {
        isAttached = true
    }

    /**
     * Construct a new NavDestination associated with this Navigator.
     *
     * Any initialization of the destination should be done in the destination's constructor as
     * it is not guaranteed that every destination will be created through this method.
     *
     * @return a new NavDestination
     */
    abstract fun createDestination(): D

    /**
     * Navigate to a destination.
     *
     * Requests navigation to a given destination associated with this navigator in
     * the navigation graph. This method generally should not be called directly;
     * [NavHostController] will delegate to it when appropriate.
     *
     * @param entries destination(s) to navigate to
     * @param navOptions additional options for navigation
     * @param navigatorExtras extras unique to your Navigator.
     */
    open fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions? = null,
        navigatorExtras: Extras? = null
    ) {
        entries.forEach { entry ->
            state.push(entry)
        }
    }

    /**
     * Informational callback indicating that the given [backStackEntry] has been
     * affected by a NavOptions singletop operation. The entry provided is a new
     * [NavBackStackEntry] instance with all the previous state of the old entry and possibly
     * new arguments.
     */
    open fun onLaunchSingleTop(backStackEntry: NavBackStackEntry) {
        // Default implementation does nothing
    }

    /**
     * Attempt to pop this navigator's back stack, performing the appropriate navigation.
     *
     * All destinations back to [popUpTo] should be popped off the back stack.
     *
     * @param popUpTo the entry that should be popped off the [NavigatorState.backStack]
     * along with all entries above this entry.
     * @param savedState whether any Navigator specific state associated with [popUpTo] should
     * be saved to later be restored by a call to [navigate] with restore state option.
     */
    open fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.pop(popUpTo, savedState)
    }

    /**
     * Attempt to pop this navigator's back stack.
     *
     * Implementations should return `true` if navigation
     * was successful. Implementations should return `false` if navigation could not
     * be performed, for example if the navigator's back stack was empty.
     *
     * @return `true` if pop was successful
     */
    open fun popBackStack(): Boolean {
        val lastEntry = state.backStack.value.lastOrNull() ?: return false
        state.pop(lastEntry, false)
        return true
    }

    /**
     * Called to ask for state representing the Navigator's state.
     */
    open fun onSaveState(): Map<String, Any?>? {
        return null
    }

    /**
     * Restore any state previously saved in [onSaveState].
     *
     * Calls to [createDestination] should not be dependent on any state restored here as
     * [createDestination] can be called before the state is restored.
     *
     * @param savedState The state previously saved
     */
    open fun onRestoreState(savedState: Map<String, Any?>) {
        // Default implementation does nothing
    }

    /**
     * Interface indicating that this class should be passed to its respective
     * [Navigator] to enable Navigator specific behavior.
     */
    interface Extras
}

/**
 * The state of a [Navigator]. This class allows a Navigator to communicate with the
 * [NavHostController] to push and pop entries on the back stack.
 */
open class NavigatorState {
    private val _backStack = MutableStateFlow<List<NavBackStackEntry>>(emptyList())

    /**
     * The back stack of [NavBackStackEntry]s managed by this state.
     */
    val backStack: StateFlow<List<NavBackStackEntry>> = _backStack.asStateFlow()

    /**
     * Push a new [entry] onto the back stack.
     */
    open fun push(entry: NavBackStackEntry) {
        _backStack.value = _backStack.value + entry
    }

    /**
     * Pop the [popUpTo] entry from the back stack along with all entries above it.
     *
     * @param popUpTo the entry to be popped
     * @param saveState whether to save state for later restoration
     */
    open fun pop(popUpTo: NavBackStackEntry, saveState: Boolean) {
        val currentStack = _backStack.value
        val index = currentStack.indexOf(popUpTo)
        if (index >= 0) {
            _backStack.value = currentStack.subList(0, index)
        }
    }

    /**
     * Pop entries above and including the given route.
     */
    open fun popWithRoute(route: String, inclusive: Boolean) {
        val currentStack = _backStack.value
        val index = currentStack.indexOfLast { it.route == route || it.destination.route == route }
        if (index >= 0) {
            val removeFrom = if (inclusive) index else index + 1
            _backStack.value = currentStack.subList(0, removeFrom)
        }
    }
}
