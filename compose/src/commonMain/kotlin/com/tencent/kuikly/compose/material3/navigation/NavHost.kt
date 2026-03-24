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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.collectAsState
import com.tencent.kuikly.compose.BackHandler
import com.tencent.kuikly.compose.animation.AnimatedContent
import com.tencent.kuikly.compose.animation.AnimatedContentTransitionScope
import com.tencent.kuikly.compose.animation.ContentTransform
import com.tencent.kuikly.compose.animation.EnterTransition
import com.tencent.kuikly.compose.animation.ExitTransition
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.togetherWith
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.ui.Modifier

/**
 * Provides a place in the Compose hierarchy for self-contained navigation to occur.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The builder passed into this method is [remember]ed. This means that for this NavHost, the
 * contents of the builder cannot be changed.
 *
 * This implementation integrates with [BackHandler] for system back button support and
 * supports animated transitions between destinations via [AnimatedContent].
 *
 * @param navController the navController for this host
 * @param startDestination the route for the start destination
 * @param modifier the [Modifier] to be applied to the layout
 * @param route the route for the graph
 * @param enterTransition callback to define enter transitions for forward navigation.
 *   Defaults to [fadeIn].
 * @param exitTransition callback to define exit transitions for forward navigation.
 *   Defaults to [fadeOut].
 * @param popEnterTransition callback to define enter transitions for pop (back) navigation.
 *   Defaults to [enterTransition].
 * @param popExitTransition callback to define exit transitions for pop (back) navigation.
 *   Defaults to [exitTransition].
 * @param builder the builder used to construct the graph
 *
 * @see NavGraphBuilder
 * @see NavHostController
 * @see rememberNavController
 */
@Composable
fun NavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    route: String? = null,
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) =
        { fadeIn() },
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) =
        { fadeOut() },
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) =
        enterTransition,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) =
        exitTransition,
    builder: NavGraphBuilder.() -> Unit
) {
    NavHost(
        navController,
        remember(route, startDestination, builder) {
            navController.createGraph(startDestination, route, builder)
        },
        modifier,
        enterTransition,
        exitTransition,
        popEnterTransition,
        popExitTransition
    )
}

/**
 * Provides a place in the Compose hierarchy for self-contained navigation to occur.
 *
 * This overload accepts a pre-built [NavGraph] directly rather than a builder.
 *
 * @param navController the navController for this host
 * @param graph the pre-built navigation graph
 * @param modifier the [Modifier] to be applied to the layout
 * @param enterTransition callback to define enter transitions for forward navigation
 * @param exitTransition callback to define exit transitions for forward navigation
 * @param popEnterTransition callback to define enter transitions for pop (back) navigation
 * @param popExitTransition callback to define exit transitions for pop (back) navigation
 *
 * @see NavGraphBuilder
 * @see NavHostController.createGraph
 */
@Composable
fun NavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier,
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) =
        { fadeIn() },
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) =
        { fadeOut() },
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) =
        enterTransition,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) =
        exitTransition
) {
    // Set the graph on the controller
    navController.graph = graph

    // Navigate to the start destination if back stack is empty
    LaunchedEffect(navController, graph) {
        if (navController.backStack.isEmpty()) {
            val startRoute = graph.startDestinationRoute
            if (startRoute != null) {
                navController.navigate(startRoute)
            }
        }
    }

    // Handle system back press
    if (navController.canNavigateBack) {
        BackHandler {
            navController.popBackStack()
        }
    }

    // SaveableStateHolder preserves the state of each destination when navigating
    val saveableStateHolder = rememberSaveableStateHolder()

    // Get dialog navigator to observe dialog back stack
    val dialogNavigator = remember { 
        runCatching { navController.navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME) }.getOrNull()
    }
    
    // Observe dialog back stack - official implementation uses separate backStack for dialogs
    val dialogBackStack by dialogNavigator?.dialogState?.backStack?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    
    // Display current destination with animated transitions
    val currentEntry = navController.currentBackStackEntry
    if (currentEntry != null) {
        // Find the current non-dialog entry (for background when dialog is shown)
        val currentNonDialogEntry = navController.backStack.lastOrNull { entry ->
            entry.destination.navigatorName != DialogNavigator.NAME
        }
        
        // Check if current entry is a dialog
        val isDialogDestination = currentEntry.destination.navigatorName == DialogNavigator.NAME

        Box(modifier = modifier) {
            // Main content - always show the last non-dialog destination
            if (currentNonDialogEntry != null) {
                AnimatedContent(
                    targetState = currentNonDialogEntry,
                    modifier = Modifier,
                    transitionSpec = {
                        // Use sequenceNumber to determine if this is a pop (back) navigation
                        val isPop = targetState.sequenceNumber < initialState.sequenceNumber
                        if (isPop) {
                            popEnterTransition(this) togetherWith popExitTransition(this)
                        } else {
                            enterTransition(this) togetherWith exitTransition(this)
                        }
                    },
                    contentKey = { it.id }
                ) { entry ->
                    saveableStateHolder.SaveableStateProvider(entry.id) {
                        val destination = entry.destination
                        if (destination is ComposeNavigator.Destination) {
                            destination.content(entry)
                        }
                    }
                }
            }

            // Render dialogs as overlay layer - official implementation
            // Each dialog in the backStack is rendered on top of previous ones
            dialogBackStack.forEach { dialogEntry ->
                key(dialogEntry.id) {
                    saveableStateHolder.SaveableStateProvider(dialogEntry.id) {
                        val destination = dialogEntry.destination
                        if (destination is DialogNavigator.Destination) {
                            // Dialog content is rendered as overlay
                            destination.content(dialogEntry)
                        }
                    }
                }
            }
        }

        // Track all entry IDs we have ever provided state for, and clean up removed ones
        val previousEntryIds = remember { mutableStateOf(emptySet<String>()) }
        val currentEntryIds = navController.backStack.map { it.id }.toSet()
        // Remove saved state for entries that are no longer in the back stack
        val removedIds = previousEntryIds.value - currentEntryIds
        removedIds.forEach { id ->
            saveableStateHolder.removeState(id)
        }
        previousEntryIds.value = currentEntryIds
    }
}

/**
 * Convenience overload of [NavHost] that automatically creates and [remember]s a
 * [NavHostController].
 *
 * @param startDestination the route for the start destination
 * @param modifier the [Modifier] to be applied to the layout
 * @param route the route for the graph
 * @param enterTransition callback to define enter transitions for forward navigation
 * @param exitTransition callback to define exit transitions for forward navigation
 * @param popEnterTransition callback to define enter transitions for pop (back) navigation
 * @param popExitTransition callback to define exit transitions for pop (back) navigation
 * @param builder the builder used to construct the graph
 * @return the [NavHostController] created for this NavHost
 */
@Composable
fun NavHost(
    startDestination: String,
    modifier: Modifier = Modifier,
    route: String? = null,
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) =
        { fadeIn() },
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) =
        { fadeOut() },
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) =
        enterTransition,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) =
        exitTransition,
    builder: NavGraphBuilder.() -> Unit
): NavHostController {
val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        route = route,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
        builder = builder
    )
    return navController
}
