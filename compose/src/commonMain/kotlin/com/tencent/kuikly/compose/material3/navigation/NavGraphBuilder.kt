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
 * Construct a new [NavGraph]
 *
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param builder the builder used to construct the graph
 * @return the newly constructed NavGraph
 */
public inline fun NavigatorProvider.navigation(
    startDestination: String,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = NavGraphBuilder(this, startDestination, route).apply(builder).build()

/**
 * Construct a nested [NavGraph]
 *
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public inline fun NavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    builder: NavGraphBuilder.() -> Unit
): Unit = destination(NavGraphBuilder(provider, startDestination, route).apply(builder))

/**
 * Construct a nested [NavGraph]
 *
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
fun NavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    builder: NavGraphBuilder.() -> Unit
) {
    addDestination(
        NavGraphBuilder(provider, startDestination, route).apply(builder).build().apply {
            arguments.forEach { namedArg ->
                addArgument(namedArg.name, namedArg.argument)
            }
            deepLinks.forEach { deepLink -> addDeepLink(deepLink) }
        }
    )
}

/**
 * DSL for constructing a new [NavGraph].
 *
 * The [NavGraphBuilder] extends [NavDestinationBuilder] and offers the
 * [composable] extension function to add composable destinations, and the [navigation]
 * extension function to add nested navigation graphs.
 *
 * This architecture matches the official Jetpack Compose Navigation pattern where
 * [NavGraphBuilder] extends [NavDestinationBuilder]<[NavGraph]> and exposes
 * [provider] for extension functions to retrieve specific [Navigator]s.
 *
 * Usage:
 * ```kotlin
 * NavHost(navController, startDestination = "home") {
 *     composable("home") { HomeScreen() }
 *     composable("detail/{id}") { entry ->
 *         DetailScreen(entry.arguments?.getString("id") ?: "")
 *     }
 *     navigation(startDestination = "login", route = "auth") {
 *         composable("login") { LoginScreen() }
 *         composable("register") { RegisterScreen() }
 *     }
 * }
 * ```
 *
 * @see NavHost
 * @see NavHostController
 * @see NavGraph
 * @see NavigatorProvider.navigation
 */
@NavDestinationDsl
open class NavGraphBuilder(
    /**
     * The [NavGraphBuilder]'s [NavigatorProvider].
     *
     * This is exposed as a public property (matching the official API) so that extension
     * functions like [composable] and [navigation] can retrieve specific [Navigator]s
     * from it (e.g., `provider[ComposeNavigator::class]`).
     */
    val provider: NavigatorProvider,
    startDestination: String,
    route: String?
) : NavDestinationBuilder<NavGraph>(
    provider.getNavigator(NavGraphNavigator.NAME),
    route
) {
    private val startDestinationRoute = startDestination

    /**
     * Build and add a new destination to the [NavGraphBuilder].
     *
     * This is used internally by extension functions like [composable] and [navigation].
     *
     * @param navDestination the destination builder to add
     */
    fun <D : NavDestination> destination(navDestination: NavDestinationBuilder<D>) {
        addDestination(navDestination.build())
    }

    /** Adds this destination to the [NavGraphBuilder] */
    operator fun NavDestination.unaryPlus() {
        addDestination(this)
    }

    private val destinations = mutableListOf<NavDestination>()

    /**
     * Add a destination to this [NavGraphBuilder].
     *
     * @param destination the [NavDestination] to add
     */
    fun addDestination(destination: NavDestination) {
        destinations.add(destination)
    }

    override fun build(): NavGraph {
        val graph = super.build()
        graph.setStartDestination(startDestinationRoute)
        destinations.forEach { destination ->
            graph.addDestination(destination)
        }
        return graph
    }
}

/**
 * Add a [Composable] destination to the [NavGraphBuilder].
 *
 * This is an extension function on [NavGraphBuilder], following the official Jetpack
 * Compose Navigation pattern where `composable()` is defined as an extension rather than
 * a member function. This allows other modules to define their own destination types
 * (e.g., `dialog()`, `bottomSheet()`) using the same extension mechanism.
 *
 * @param route route for the destination
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param enterTransition callback to define enter transitions for forward navigation.
 *   If not provided, uses the NavHost-level default.
 * @param exitTransition callback to define exit transitions for forward navigation.
 *   If not provided, uses the NavHost-level default.
 * @param popEnterTransition callback to define enter transitions for pop (back) navigation.
 *   If not provided, uses the NavHost-level default.
 * @param popExitTransition callback to define exit transitions for pop (back) navigation.
 *   If not provided, uses the NavHost-level default.
 * @param content composable for the destination
 *
 * @see NamedNavArgument
 * @see navArgument
 * @see NavDeepLink
 * @see navDeepLink
 */
fun NavGraphBuilder.composable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    val navigator: ComposeNavigator = provider.getNavigator(ComposeNavigator.NAME)
    addDestination(
        ComposeNavigator.Destination(navigator, content).apply {
            this.route = route
            arguments.forEach { namedArg ->
                addArgument(namedArg.name, namedArg.argument)
            }
            deepLinks.forEach { deepLink -> addDeepLink(deepLink) }
            // Store transition callbacks in the destination for per-destination customization
            this.enterTransition = enterTransition
            this.exitTransition = exitTransition
            this.popEnterTransition = popEnterTransition
            this.popExitTransition = popExitTransition
        }
    )
}

/**
 * Add a dialog-style destination to the [NavGraphBuilder].
 *
 * Unlike [composable], dialog destinations are displayed as overlays on top of the
 * previous destination, with no transition animations. This is useful for modal dialogs,
 * bottom sheets, and other overlay-style navigation.
 *
 * @param route route for the destination
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param content composable for the destination
 *
 * @see composable
 */
fun NavGraphBuilder.dialog(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit
) {
    // Get or create a dialog navigator
    val navigator: DialogNavigator = provider.getNavigator(DialogNavigator.NAME)
    addDestination(
        DialogNavigator.Destination(navigator, content).apply {
            this.route = route
            arguments.forEach { namedArg ->
                addArgument(namedArg.name, namedArg.argument)
            }
            deepLinks.forEach { deepLink -> addDeepLink(deepLink) }
        }
    )
}

/**
 * Navigator for dialog-style destinations.
 *
 * Unlike [ComposeNavigator], dialog destinations are displayed as overlays
 * without transition animations.
 *
 * The official implementation manages a separate backStack for dialogs,
 * and the NavHost renders dialogs by observing this backStack.
 */
class DialogNavigator : Navigator<DialogNavigator.Destination>() {

    override val name: String = NAME

    // Internal state managed by NavHostController - internal visibility for framework access
    internal var _state: NavigatorState? = null
    
    // Public state for NavHost to observe - throws if not attached
    val dialogState: NavigatorState
        get() = _state ?: throw IllegalStateException("DialogNavigator is not attached")
    
    override val state: NavigatorState
        get() = dialogState

    override fun createDestination(): Destination {
        return Destination(this) {}
    }

    /**
     * NavDestination specific to [DialogNavigator].
     */
    class Destination(
        navigator: DialogNavigator,
        internal val content: @Composable (NavBackStackEntry) -> Unit
    ) : NavDestination(navigator)

    companion object {
        const val NAME = "dialog"
    }
}