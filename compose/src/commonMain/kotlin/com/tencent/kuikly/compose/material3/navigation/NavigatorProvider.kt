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

/**
 * A NavigationProvider stores a set of [Navigator]s that are valid ways to navigate
 * to a destination.
 *
 * Navigators are registered by name, and can be retrieved with [getNavigator].
 *
 * @see Navigator
 * @see NavHostController
 */
open class NavigatorProvider {

    private val _navigators = mutableMapOf<String, Navigator<out NavDestination>>()

    /**
     * The registered navigators.
     */
    val navigators: Map<String, Navigator<out NavDestination>>
        get() = _navigators

    /**
     * Retrieves a registered [Navigator] by name.
     *
     * @param name name of the navigator to return
     * @return the registered navigator with the given name
     *
     * @throws IllegalStateException if the Navigator has not been added
     *
     * @see addNavigator
     */
    @Suppress("UNCHECKED_CAST")
    open fun <T : Navigator<*>> getNavigator(name: String): T {
        val navigator = _navigators[name]
            ?: throw IllegalStateException(
                "Could not find Navigator with name \"$name\". You must call " +
                    "NavigatorProvider.addNavigator() for each navigation type."
            )
        return navigator as T
    }

    /**
     * Register a navigator using its [Navigator.name]. If a navigator by this name is
     * already registered, this new navigator will replace it.
     *
     * @param navigator navigator to add
     * @return the previously added Navigator for its name, if any
     */
    fun addNavigator(
        navigator: Navigator<out NavDestination>
    ): Navigator<out NavDestination>? {
        return addNavigator(navigator.name, navigator)
    }

    /**
     * Register a navigator by name. If a navigator by this name is already
     * registered, this new navigator will replace it.
     *
     * @param name name for this navigator
     * @param navigator navigator to add
     * @return the previously added Navigator for the given name, if any
     */
    open fun addNavigator(
        name: String,
        navigator: Navigator<out NavDestination>
    ): Navigator<out NavDestination>? {
        val previous = _navigators[name]
        _navigators[name] = navigator
        return previous
    }
}

/**
 * Retrieves a registered [Navigator] by name.
 *
 * @throws IllegalStateException if the Navigator has not been added
 */
@Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
inline operator fun <T : Navigator<out NavDestination>> NavigatorProvider.get(
    name: String
): T = getNavigator(name)

/**
 * Register a [Navigator] by name. If a navigator by this name is already registered, this new
 * navigator will replace it.
 *
 * @return the previously added [Navigator] for the given name, if any
 */
@Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
inline operator fun NavigatorProvider.set(
    name: String,
    navigator: Navigator<out NavDestination>
): Navigator<out NavDestination>? = addNavigator(name, navigator)

/**
 * Register a navigator using the name provided by its [Navigator.name].
 */
@Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
inline operator fun NavigatorProvider.plusAssign(navigator: Navigator<out NavDestination>) {
    addNavigator(navigator)
}
