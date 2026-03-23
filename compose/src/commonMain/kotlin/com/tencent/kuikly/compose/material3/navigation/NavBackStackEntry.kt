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
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * Representation of an entry in the back stack of a [NavHostController]. The [lifecycle] of the
 * entry can be [Lifecycle.State.CREATED], [Lifecycle.State.STARTED], or [Lifecycle.State.RESUMED].
 * This class holds the [NavDestination] and any [arguments] that are applicable when the
 * [NavDestination] is on the back stack.
 *
 * The composable content associated with this entry should appear when this entry's lifecycle is
 * [Lifecycle.State.RESUMED], and disappear when this lifecycle is [Lifecycle.State.CREATED].
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
    val id: String = randomUUID(),
    /**
     * A monotonically increasing sequence number used to determine navigation direction.
     * Higher numbers indicate more recent entries. Used by NavHost to distinguish
     * forward navigation (push) from backward navigation (pop).
     */
    @JvmField
    internal val sequenceNumber: Long = 0L
) {
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
 * Returns a random UUID string.
 */
private fun randomUUID(): String {
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
