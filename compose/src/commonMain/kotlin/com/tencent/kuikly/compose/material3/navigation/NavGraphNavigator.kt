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
 * A Navigator built specifically for [NavGraph] elements. Handles navigating to the
 * correct destination when the NavGraph is the target of navigation actions.
 *
 * @param navigatorProvider NavigatorProvider used to retrieve the correct
 * [Navigator] to navigate to the start destination
 *
 * @see NavGraph
 * @see Navigator
 */
open class NavGraphNavigator(
    private val navigatorProvider: NavigatorProvider
) : Navigator<NavGraph>() {

    override val name: String = NAME

    private val _state = NavigatorState()

    override val state: NavigatorState
        get() = _state

    /**
     * Creates a new [NavGraph] associated with this navigator.
     *
     * @return The created [NavGraph].
     */
    override fun createDestination(): NavGraph {
        return NavGraph(this)
    }

    companion object {
        /**
         * The name used to register this navigator with [NavigatorProvider].
         */
        const val NAME = "navigation"
    }
}
