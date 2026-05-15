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
import com.tencent.kuikly.compose.animation.AnimatedContentTransitionScope
import com.tencent.kuikly.compose.animation.EnterTransition
import com.tencent.kuikly.compose.animation.ExitTransition

/**
 * Navigator that navigates through [Composable]s. Every destination using this Navigator must
 * set a valid [Composable] by setting it directly on an instantiated [Destination] or calling
 * [composable].
 *
 * This is the core navigator for Compose-based navigation destinations.
 *
 * @see composable
 * @see NavHost
 */
class ComposeNavigator : Navigator<ComposeNavigator.Destination>() {

    override val name: String = NAME

    private val _state = NavigatorState()

    override val state: NavigatorState
        get() = _state

    /**
     * Creates a new [Destination] associated with this navigator.
     */
    override fun createDestination(): Destination {
        return Destination(this) {}
    }

    /**
     * NavDestination specific to [ComposeNavigator].
     *
     * @param navigator The [ComposeNavigator] associated with this destination
     * @param content The composable content for this destination
     */
    class Destination(
        navigator: ComposeNavigator,
        internal val content: @Composable (NavBackStackEntry) -> Unit
    ) : NavDestination(navigator) {
        // Internal properties for animated transitions (matching official API)
        internal var enterTransition: (
            (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)?
        )? = null
        internal var exitTransition: (
            (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)?
        )? = null
        internal var popEnterTransition: (
            (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)?
        )? = null
        internal var popExitTransition: (
            (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)?
        )? = null
        internal var sizeTransform: (() -> Any?)? = null
    }

    internal companion object {
        /**
         * The name used to register this navigator with [NavigatorProvider].
         */
        const val NAME = "composable"
    }
}
