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

import com.tencent.kuikly.core.bundle.Bundle
import com.tencent.kuikly.lifecycle.Lifecycle
import com.tencent.kuikly.lifecycle.LifecycleOwner
import com.tencent.kuikly.lifecycle.LifecycleRegistry
import com.tencent.kuikly.lifecycle.ViewModelStore
import com.tencent.kuikly.lifecycle.ViewModelStoreOwner
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A function that provides a [ViewModelStore] for a given back stack entry ID.
 *
 * This follows the official Android Navigation component pattern where the
 * [NavHostController] (via [NavControllerViewModel]) centrally manages all
 * ViewModelStore instances, and each [NavBackStackEntry] retrieves its store
 * through this provider function.
 */
internal typealias ViewModelStoreProvider = (entryId: String) -> ViewModelStore

/**
 * Representation of an entry in the back stack of a [NavHostController]. The [lifecycle] of the
 * entry can be [Lifecycle.State.CREATED], [Lifecycle.State.STARTED], or [Lifecycle.State.RESUMED].
 * This class holds the [NavDestination] and any [arguments] that are applicable when the
 * [NavDestination] is on the back stack.
 *
 * The composable content associated with this entry should appear when this entry's lifecycle is
 * [Lifecycle.State.RESUMED], and disappear when this lifecycle is [Lifecycle.State.CREATED].
 *
 * **ViewModelStore ownership**: This entry does NOT directly own its [ViewModelStore].
 * Instead, it retrieves it via a [viewModelStoreProvider] injected by [NavHostController].
 * The [NavControllerViewModel] centrally manages all ViewModelStore instances, ensuring
 * proper cleanup when entries are destroyed. This follows the official Android Navigation
 * component architecture.
 */
class NavBackStackEntry(
    /**
     * The destination associated with this entry
     */
    @JvmField
    val destination: NavDestination,
    /**
     * The optional arguments sent to the destination
     */
    @JvmField
    val arguments: Bundle? = null,
    /**
     * The unique ID of this entry
     */
    @JvmField
    val id: String = randomId(),
    /**
     * A monotonically increasing sequence number used to determine navigation direction.
     * Higher numbers indicate more recent entries. Used by NavHost to distinguish
     * forward navigation (push) from backward navigation (pop).
     */
    @JvmField
    internal val sequenceNumber: Long = 0L
) : LifecycleOwner, ViewModelStoreOwner {
    /**
     * The route that was used to navigate to this destination.
     * This may include filled-in arguments, e.g., "detail/123".
     */
    var route: String? = null
        internal set

    /**
     * A [SavedStateHandle] for this back stack entry. This provides access to the saved state
     * for this entry, which will survive configuration changes and process death.
     */
    val savedStateHandle: SavedStateHandle = SavedStateHandle()

    /**
     * The [LifecycleRegistry] for this back stack entry. It manages lifecycle state transitions
     * and notifies registered observers.
     *
     * The lifecycle follows these rules:
     * - CREATED: Entry is on the back stack but not visible
     * - STARTED: Entry is visible but not the topmost (e.g., partially visible during transition)
     * - RESUMED: Entry is the topmost visible destination
     * - DESTROYED: Entry has been popped from the back stack
     */
    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    /**
     * Provider function to retrieve the [ViewModelStore] for this entry.
     *
     * This is injected by [NavHostController] and delegates to [NavControllerViewModel]
     * which centrally manages all ViewModelStore instances. This follows the official
     * Android Navigation component pattern where the NavController owns the stores.
     *
     * If no provider is set (e.g., in tests), a fallback empty ViewModelStore is used.
     */
    internal var viewModelStoreProvider: ViewModelStoreProvider? = null

    override val viewModelStore: ViewModelStore
        get() = viewModelStoreProvider?.invoke(id)
            ?: throw IllegalStateException(
                "You must call setViewModelStoreProvider() on this NavBackStackEntry before " +
                    "accessing its ViewModelStore. This is typically done by NavHostController."
            )

    /**
     * The host lifecycle state, representing the lifecycle state of the host (e.g., NavHost).
     * The actual lifecycle state of this entry will be the minimum of [hostLifecycleState]
     * and [maxLifecycle].
     *
     * This follows the official Android Navigation component pattern where the host lifecycle
     * constrains the entry lifecycle.
     */
    @JvmField
    internal var hostLifecycleState: Lifecycle.State = Lifecycle.State.CREATED

    /**
     * The maximum [Lifecycle.State] that this entry can reach. This is used to cap the
     * lifecycle state, for example when the entry is not the current destination.
     *
     * Setting this property will immediately update the lifecycle state to the minimum of
     * [hostLifecycleState] and the new [maxLifecycle] value.
     *
     * This follows the official Android Navigation component pattern:
     * - RESUMED: Entry is the topmost visible destination
     * - STARTED: Entry is visible but not active (e.g., below a dialog)
     * - CREATED: Entry is on the back stack but not visible
     * - DESTROYED: Entry has been popped from the back stack
     */
    internal var maxLifecycle: Lifecycle.State = Lifecycle.State.INITIALIZED
        set(value) {
            field = value
            updateState()
        }

    /**
     * The pagerId for this entry, used by the Kuikly lifecycle system.
     * Set by [NavHostController] at entry creation time from its captured [NavHostController.hostPageId].
     */
    @JvmField
    internal var hostPageId: String = ""

    override val pagerId: String
        get() = hostPageId

    /**
     * Updates the lifecycle state of this entry based on [hostLifecycleState] and [maxLifecycle].
     *
     * The actual state is the minimum of [hostLifecycleState] and [maxLifecycle].
     * If the entry is still in INITIALIZED state (never been pushed to at least CREATED),
     * the update is deferred until maxLifecycle moves beyond INITIALIZED.
     *
     * This follows the official `NavBackStackEntry.updateState()` implementation from
     * `androidx.navigation:navigation-runtime`.
     */
    internal fun updateState() {
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) {
            // Cannot update state after destruction
            return
        }
        // The actual state is the minimum of hostLifecycleState and maxLifecycle
        val targetState = if (hostLifecycleState.ordinal < maxLifecycle.ordinal) {
            hostLifecycleState
        } else {
            maxLifecycle
        }
        // If still INITIALIZED, defer the update (entry hasn't entered composition yet)
        if (targetState == Lifecycle.State.INITIALIZED) {
            return
        }
        if (targetState != lifecycleRegistry.currentState) {
            // LifecycleRegistry requires at least CREATED before moving to DESTROYED.
            // If still INITIALIZED, push to CREATED first.
            if (targetState == Lifecycle.State.DESTROYED
                && lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED
            ) {
                lifecycleRegistry.currentState = Lifecycle.State.CREATED
            }
            lifecycleRegistry.currentState = targetState
        }
        // Note: ViewModelStore cleanup is NOT done here. It is managed centrally by
        // NavControllerViewModel via NavHostController, which clears the store when
        // the entry is fully destroyed (after transitions complete). This follows the
        // official Android Navigation component architecture.
    }

    /**
     * Handles a lifecycle event from the host (e.g., NavHost).
     *
     * This updates the [hostLifecycleState] to the event's target state and then
     * recalculates the actual lifecycle state via [updateState]. This follows the
     * official Android Navigation component pattern where the host lifecycle
     * constrains all entry lifecycles.
     *
     * For example, when the host moves to STARTED (e.g., goes to background),
     * all entries' lifecycles will be capped at STARTED regardless of their
     * [maxLifecycle] setting.
     *
     * @param event The lifecycle event from the host
     */
    internal fun handleLifecycleEvent(event: Lifecycle.Event) {
        hostLifecycleState = event.targetState
        updateState()
    }

    /**
     * Marks this entry as destroyed by moving the lifecycle to DESTROYED state.
     *
     * Delegates to [updateState] by setting [maxLifecycle] to [Lifecycle.State.DESTROYED].
     *
     * Note: This only handles the lifecycle transition. The associated [ViewModelStore]
     * cleanup is managed by [NavControllerViewModel] via [NavHostController], which
     * ensures cleanup happens after any exit transitions have completed.
     *
     * This follows the official Android Navigation component behavior.
     */
    internal fun destroy() {
        maxLifecycle = Lifecycle.State.DESTROYED
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavBackStackEntry) return false
        return id == other.id && destination == other.destination
    }

    override fun hashCode(): Int {
        return 31 * id.hashCode() + destination.hashCode()
    }

    override fun toString(): String {
        return buildString {
            append("NavBackStackEntry(")
            append(destination.route)
            append(", ")
            append(id)
            append(")")
        }
    }

    companion object {
        /**
         * Monotonically increasing sequence counter for back stack entries.
         * Used to determine navigation direction (push vs pop) by comparing entry ages.
         */
        private var sequenceCounter: Long = 0L

        @JvmStatic
        fun create(
            destination: NavDestination,
            arguments: Bundle? = null
        ): NavBackStackEntry {
            return NavBackStackEntry(
                destination = destination,
                arguments = arguments,
                sequenceNumber = ++sequenceCounter
            )
        }
    }
}

/**
 * A handle to saved state passed to the [NavBackStackEntry].
 * This provides access to the saved state, which will survive configuration changes
 * and process death.
 */
class SavedStateHandle {
    @PublishedApi
    internal val _state = mutableMapOf<String, Any?>()

    /**
     * Gets a live data for the value associated with the given key. The value can be observed
     * as it changes.
     *
     * @param key the identifier for the value
     * @return a mutable state associated with this key
     */
    operator fun <T> get(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return _state[key] as? T
    }

    /**
     * Returns a state containing the value associated with the given key.
     *
     * @param key the identifier for the value
     * @return a mutable state associated with this key
     */
    fun <T> getState(key: String): androidx.compose.runtime.MutableState<T?> {
        @Suppress("UNCHECKED_CAST")
        val state = androidx.compose.runtime.mutableStateOf(_state[key] as? T)
        return state
    }

    /**
     * Returns a state containing the value associated with the given key, or [initialValue] if
     * no value is associated with the given key.
     *
     * @param key the identifier for the value
     * @param initialValue the value to set the state to if one is not associated with the key
     * @return a mutable state associated with this key
     */
    fun <T> getState(key: String, initialValue: T): androidx.compose.runtime.MutableState<T> {
        @Suppress("UNCHECKED_CAST")
        val state = androidx.compose.runtime.mutableStateOf(
            (_state[key] as? T) ?: initialValue
        )
        return state
    }

    /**
     * Sets a value associated with the given key.
     *
     * @param key the identifier for the value
     * @param value the value to store
     */
    operator fun <T> set(key: String, value: T) {
        _state[key] = value
    }

    /**
     * Removes a value associated with the given key.
     *
     * @param key the identifier for the value
     * @return the value that was removed.
     */
    fun <T> remove(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return _state.remove(key) as? T
    }

    /**
     * Returns true if there is a value associated with the given key.
     *
     * @param key the identifier for the value
     * @return true if there is a value associated with the key.
     */
    operator fun contains(key: String): Boolean {
        return _state.containsKey(key)
    }

    /**
     * Returns the set of keys in the saved state.
     *
     * @return the set of keys in the saved state.
     */
    fun keys(): Set<String> {
        return _state.keys.toSet()
    }

    /**
     * Performs the given [action] for each element in the saved state.
     *
     * @param action the action to perform on each element
     */
    fun forEach(action: (String, Any?) -> Unit) {
        _state.toList().forEach { (key, value) ->
            action(key, value)
        }
    }

    internal fun internalMap() = _state.toMap()
}

/**
 * Returns a random unique identifier string.
 * Note: This is NOT a standard UUID format (which uses hex chars only).
 * It uses alphanumeric characters (0-9, A-Z, a-z) for higher entropy per character.
 */
private fun randomId(): String {
    val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    val random = kotlin.random.Random
    return buildString {
        repeat(8) { append(chars[random.nextInt(chars.length)]) }
        append("-")
        repeat(4) { append(chars[random.nextInt(chars.length)]) }
        append("-")
        repeat(4) { append(chars[random.nextInt(chars.length)]) }
        append("-")
        repeat(4) { append(chars[random.nextInt(chars.length)]) }
        append("-")
        repeat(12) { append(chars[random.nextInt(chars.length)]) }
    }
}
