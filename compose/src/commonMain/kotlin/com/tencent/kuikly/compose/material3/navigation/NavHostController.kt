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
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.lifecycle.Lifecycle
import com.tencent.kuikly.lifecycle.LifecycleEventObserver
import com.tencent.kuikly.lifecycle.LifecycleOwner

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
 * **ViewModelStore management**: This controller uses [NavControllerViewModel] to centrally
 * manage all [ViewModelStore] instances for each [NavBackStackEntry]. Each entry retrieves
 * its ViewModelStore via a provider function injected by this controller, following the
 * official Android Navigation component architecture.
 *
 * This implementation does not depend on any Android-specific APIs and can run in commonMain.
 *
 * @see NavHost
 * @see NavGraphBuilder
 * @see rememberNavController
 * @see NavigatorProvider
 * @see NavControllerViewModel
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
        val dialogNavigator = navigatorProvider.navigators[DialogNavigator.NAME] as? DialogNavigator
        if (dialogNavigator != null && dialogNavigator._state == null) {
            dialogNavigator._state = NavigatorState()
        }
    }

    /**
     * Centralized manager for [ViewModelStore] instances associated with [NavBackStackEntry]s.
     *
     * This follows the official Android Navigation component pattern where
     * `NavControllerViewModel` holds all ViewModelStore instances keyed by entry ID.
     * Each [NavBackStackEntry] retrieves its ViewModelStore through a provider function
     * that delegates to this manager.
     *
     * @see NavControllerViewModel
     * @see NavBackStackEntry.viewModelStoreProvider
     */
    internal val viewModelStoreManager = NavControllerViewModel()

    /**
     * The current host lifecycle state. Updated via [setLifecycleOwner].
     *
     * This constrains all entry lifecycles: each entry's actual lifecycle state is
     * `min(hostLifecycleState, maxLifecycle)`. When the host goes to background
     * (STARTED), all entries are capped at STARTED.
     *
     * This follows the official Android Navigation component pattern.
     */
    internal var hostLifecycleState: Lifecycle.State = Lifecycle.State.CREATED
        private set

    /**
     * The lifecycle owner of the host (e.g., NavHost's LocalLifecycleOwner).
     */
    private var lifecycleOwner: LifecycleOwner? = null

    /**
     * Observer that syncs the host lifecycle state to all back stack entries.
     *
     * When the host lifecycle changes (e.g., goes to background), this observer
     * updates [hostLifecycleState] and propagates the event to all entries.
     *
     * This follows the official `NavController.lifecycleObserver` implementation.
     */
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        hostLifecycleState = event.targetState
        if (graph != null) {
            for (entry in backStack) {
                entry.handleLifecycleEvent(event)
            }
            // Also propagate lifecycle events to entries in exit transitions,
            // so they are properly constrained when the host goes to background.
            for (entry in transitionsInProgress) {
                entry.handleLifecycleEvent(event)
            }
        }
    }

    /**
     * Sets the [LifecycleOwner] for this controller.
     *
     * The controller will observe the owner's lifecycle and sync the host lifecycle
     * state to all back stack entries. This ensures entries are properly paused/resumed
     * when the host goes to background/foreground.
     *
     * This follows the official `NavController.setLifecycleOwner()` implementation.
     *
     * @param owner The lifecycle owner of the host (typically from `LocalLifecycleOwner`)
     */
    fun setLifecycleOwner(owner: LifecycleOwner) {
        if (owner == lifecycleOwner) {
            return
        }
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = owner
        owner.lifecycle.addObserver(lifecycleObserver)
    }

    /**
     * Entries that are currently in a transition (exit animation).
     *
     * When an entry is popped, if it's still visible during an exit animation,
     * it's added here instead of being immediately destroyed. Once the transition
     * completes, [markTransitionComplete] is called to finalize the destruction.
     *
     * This follows the official `NavigatorState.transitionsInProgress` pattern.
     */
    internal val transitionsInProgress = mutableSetOf<NavBackStackEntry>()

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
                // Re-inject viewModelStoreProvider and sync hostLifecycleState for restored entries
                restored.forEach { restoredEntry ->
                    if (restoredEntry.viewModelStoreProvider == null) {
                        restoredEntry.viewModelStoreProvider = { entryId ->
                            viewModelStoreManager.getViewModelStore(entryId)
                        }
                    }
                    // Sync the host lifecycle state so restored entries are properly constrained
                    restoredEntry.hostLifecycleState = hostLifecycleState
                }
                backStack.addAll(restored)
                // Update lifecycle states for restored entries (official pattern:
                // dispatchOnDestinationChanged() is always called after restoreState)
                updateBackStackLifecycles()
                return
            }
        }

        val entry = createEntry(destination, args)
        entry.route = resolvedRoute

        // Handle launchSingleTop: only deduplicate if the current top is the same destination
        if (options.launchSingleTop) {
            val topEntry = backStack.lastOrNull()
            if (topEntry != null && topEntry.destination.route == destination.route) {
                // Destroy the old entry before replacing
                val oldEntry = backStack.removeAt(backStack.size - 1)
                destroyEntry(oldEntry)
                backStack.add(entry)
                updateBackStackLifecycles()
                return
            }
        }

        backStack.add(entry)
        updateBackStackLifecycles()
        
        // If this is a dialog destination, also push to DialogNavigator's backStack
        if (destination.navigatorName == DialogNavigator.NAME) {
            val dialogNavigator = navigatorProvider.navigators[DialogNavigator.NAME] as? DialogNavigator
            dialogNavigator?.dialogState?.push(entry)
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
        val isDialog = poppedEntry.destination.navigatorName == DialogNavigator.NAME
        
        if (isDialog) {
            // Dialog entries don't have exit animations via AnimatedContent,
            // so destroy them immediately instead of preparing for transition.
            destroyEntry(poppedEntry)
            // Also pop from DialogNavigator's backStack
            val dialogNavigator = navigatorProvider.navigators[DialogNavigator.NAME] as? DialogNavigator
            dialogNavigator?.dialogState?.pop(poppedEntry, false)
        } else {
            // Non-dialog entries: move to CREATED and prepare for exit transition.
            // The entry will be fully destroyed when markTransitionComplete() is called
            // after the exit animation finishes. This follows the official pattern.
            if (poppedEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                poppedEntry.maxLifecycle = Lifecycle.State.CREATED
            }
            prepareForTransition(poppedEntry)
        }
        
        // Update lifecycle states for remaining entries
        updateBackStackLifecycles()
        
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
                val entry = createEntry(dest, args)
                entry.route = dest.route
                backStack.add(entry)
                updateBackStackLifecycles()
                return true
            }
            // Also check nested graphs
            if (dest is NavGraph) {
                for (nestedDest in dest) {
                    val nestedArgs = nestedDest.matchDeepLink(uri)
                    if (nestedArgs != null) {
                        val nestedEntry = createEntry(nestedDest, nestedArgs)
                        nestedEntry.route = nestedDest.route
                        backStack.add(nestedEntry)
                        updateBackStackLifecycles()
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
                if (saveState) {
                    // Cap lifecycle at CREATED for saved entries
                    backStack.forEach {
                        if (it.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                            it.maxLifecycle = Lifecycle.State.CREATED
                        }
                    }
                } else {
                    backStack.forEach {
                        val isDialog = it.destination.navigatorName == DialogNavigator.NAME
                        if (isDialog) {
                            destroyEntry(it)
                        } else {
                            if (it.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                                it.maxLifecycle = Lifecycle.State.CREATED
                            }
                            prepareForTransition(it)
                        }
                    }
                }
                backStack.clear()
            } else if (backStack.size > 1) {
                if (saveState) {
                    val currentDest = backStack.last().destination.route ?: ""
                    savedStates[currentDest] = backStack.drop(1).toList()
                }
                while (backStack.size > 1) {
                    val poppedEntry = backStack.removeAt(backStack.size - 1)
                    val isDialog = poppedEntry.destination.navigatorName == DialogNavigator.NAME
                    if (saveState) {
                        // Cap lifecycle at CREATED for saved entries
                        if (poppedEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                            poppedEntry.maxLifecycle = Lifecycle.State.CREATED
                        }
                    } else if (isDialog) {
                        destroyEntry(poppedEntry)
                    } else {
                        if (poppedEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                            poppedEntry.maxLifecycle = Lifecycle.State.CREATED
                        }
                        prepareForTransition(poppedEntry)
                    }
                }
            }
            updateBackStackLifecycles()
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
            val isDialog = poppedEntry.destination.navigatorName == DialogNavigator.NAME
            if (saveState) {
                // When saving state, cap lifecycle at CREATED so the entry is no longer active,
                // but don't destroy it since it may be restored later.
                if (poppedEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    poppedEntry.maxLifecycle = Lifecycle.State.CREATED
                }
            } else if (isDialog) {
                // Dialog entries don't have exit animations, destroy immediately
                destroyEntry(poppedEntry)
            } else {
                // Non-dialog entries: move to CREATED and prepare for exit transition
                if (poppedEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    poppedEntry.maxLifecycle = Lifecycle.State.CREATED
                }
                prepareForTransition(poppedEntry)
            }
            if (isDialog) {
                val dialogNavigator = navigatorProvider.navigators[DialogNavigator.NAME] as? DialogNavigator
                dialogNavigator?.dialogState?.pop(poppedEntry, false)
            }
        }
        // Update lifecycle states for remaining entries
        updateBackStackLifecycles()
        return true
    }

    /**
     * Updates the lifecycle states of all entries in the back stack by setting their
     * [NavBackStackEntry.maxLifecycle] property.
     *
     * The rules follow the official Android Navigation component behavior:
     * - The topmost non-dialog entry: RESUMED if no dialog above it, STARTED otherwise
     * - Dialog entries: RESUMED for the topmost, STARTED for others
     * - All other non-dialog entries below the top: CREATED (not visible)
     *
     * Setting [NavBackStackEntry.maxLifecycle] will automatically trigger
     * [NavBackStackEntry.updateState] to compute the actual lifecycle state as
     * `min(hostLifecycleState, maxLifecycle)`.
     */
    internal fun updateBackStackLifecycles() {
        if (backStack.isEmpty()) return

        // Find the topmost non-dialog entry
        val topNonDialogIndex = backStack.indexOfLast { entry ->
            entry.destination.navigatorName != DialogNavigator.NAME
        }

        for (i in backStack.indices) {
            val entry = backStack[i]
            val isDialog = entry.destination.navigatorName == DialogNavigator.NAME

            when {
                // The topmost non-dialog entry: RESUMED if no dialog above it, STARTED otherwise
                i == topNonDialogIndex -> {
                    val hasDialogAbove = backStack.subList(i + 1, backStack.size).any {
                        it.destination.navigatorName == DialogNavigator.NAME
                    }
                    entry.maxLifecycle = if (hasDialogAbove) {
                        Lifecycle.State.STARTED
                    } else {
                        Lifecycle.State.RESUMED
                    }
                }
                // Dialog entries on top: RESUMED for the topmost, STARTED for others
                isDialog && i == backStack.size - 1 -> {
                    entry.maxLifecycle = Lifecycle.State.RESUMED
                }
                isDialog -> {
                    entry.maxLifecycle = Lifecycle.State.STARTED
                }
                // All other non-dialog entries below the top: CREATED (not visible)
                else -> {
                    entry.maxLifecycle = Lifecycle.State.CREATED
                }
            }
        }
    }

    /**
     * Creates a new [NavBackStackEntry] with the [viewModelStoreProvider] injected.
     *
     * This is the single factory method for creating entries, ensuring that every entry
     * has access to the centralized [NavControllerViewModel] for ViewModelStore management.
     * This follows the official Android Navigation component pattern.
     *
     * @param destination The destination for this entry
     * @param arguments The arguments for this entry
     * @return A new [NavBackStackEntry] with the provider injected
     */
    private fun createEntry(
        destination: NavDestination,
        arguments: Bundle? = null
    ): NavBackStackEntry {
        val entry = NavBackStackEntry.create(
            destination = destination,
            arguments = arguments
        )
        entry.viewModelStoreProvider = { entryId -> viewModelStoreManager.getViewModelStore(entryId) }
        // Sync the host lifecycle state so the entry starts with the correct constraint.
        // If the host is already RESUMED, the entry will be able to reach RESUMED;
        // if the host is only STARTED (e.g., in background), the entry will be capped at STARTED.
        entry.hostLifecycleState = hostLifecycleState
        @Suppress("DEPRECATION")
        entry.hostPageId = BridgeManager.currentPageId
        return entry
    }

    /**
     * Destroys a [NavBackStackEntry] by moving its lifecycle to DESTROYED and cleaning up
     * its associated [ViewModelStore] via [NavControllerViewModel].
     *
     * This is the single destruction method for entries, ensuring that both lifecycle
     * and ViewModelStore cleanup happen together. This follows the official Android
     * Navigation component pattern where cleanup is centralized in the controller.
     *
     * @param entry The entry to destroy
     */
    internal fun destroyEntry(entry: NavBackStackEntry) {
        if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            entry.maxLifecycle = Lifecycle.State.DESTROYED
        }
        viewModelStoreManager.clear(entry.id)
    }

    /**
     * Prepares an entry for a transition (exit animation).
     *
     * Instead of immediately destroying the entry, it's moved to CREATED state
     * and added to [transitionsInProgress]. The entry will be fully destroyed
     * when [markTransitionComplete] is called after the animation finishes.
     *
     * This follows the official `NavigatorState.prepareForTransition()` pattern.
     *
     * @param entry The entry that is about to start an exit transition
     */
    internal fun prepareForTransition(entry: NavBackStackEntry) {
        transitionsInProgress.add(entry)
    }

    /**
     * Marks a transition as complete for the given entry.
     *
     * If the entry is no longer in the back stack (i.e., it was popped), this will
     * finalize its destruction by moving its lifecycle to DESTROYED and clearing
     * its ViewModelStore.
     *
     * This follows the official `NavController.markTransitionComplete()` implementation.
     *
     * @param entry The entry whose transition has completed
     */
    internal fun markTransitionComplete(entry: NavBackStackEntry) {
        transitionsInProgress.remove(entry)
        if (!backStack.contains(entry)) {
            // Entry was popped; finalize destruction
            destroyEntry(entry)
        }
        updateBackStackLifecycles()
    }

    /**
     * Clears all [ViewModelStore] instances and destroys all entries.
     *
     * This should be called when the NavHostController itself is being disposed,
     * to ensure all ViewModels are properly cleaned up and prevent memory leaks.
     *
     * This follows the official Android Navigation component pattern where
     * `NavControllerViewModel.onCleared()` cleans up all stores.
     */
    fun clear() {
        // Remove lifecycle observer from host
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = null
        // Destroy all entries in transitions
        transitionsInProgress.forEach { destroyEntry(it) }
        transitionsInProgress.clear()
        // Destroy all entries in back stack
        backStack.forEach { destroyEntry(it) }
        backStack.clear()
        // Destroy all saved state entries to prevent memory leaks
        savedStates.values.flatten().forEach { destroyEntry(it) }
        savedStates.clear()
        viewModelStoreManager.clearAll()
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
            // Register DialogNavigator (ComposeNavigator and NavGraphNavigator are already
            // registered in the NavHostController constructor, so we don't re-register them)
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
