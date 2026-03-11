/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.material3.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import com.tencent.kuikly.compose.BackHandler
import com.tencent.kuikly.compose.animation.AnimatedContent
import com.tencent.kuikly.compose.animation.AnimatedContentTransitionScope
import com.tencent.kuikly.compose.animation.ContentTransform
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.togetherWith
import com.tencent.kuikly.compose.ui.Modifier

/**
 * CompositionLocal providing the current [NavHostController].
 * Can be used by any composable within the NavHost to access the navigation controller.
 *
 * Usage:
 * ```kotlin
 * val navController = LocalNavHostController.current
 * navController.navigate("detail/123")
 * ```
 */
val LocalNavHostController = staticCompositionLocalOf<NavHostController> {
    error("No NavHostController provided. Make sure you are using NavHost.")
}

/**
 * NavHost - Cross-platform navigation host component.
 *
 * Migrated from official `androidx.navigation.compose.NavHost`.
 * This implementation does not depend on any Android-specific APIs (no `androidx.navigation`)
 * and is fully placed in commonMain for cross-platform usage.
 *
 * NavHost displays the composable content for the current navigation destination,
 * with animated transitions between destinations. It manages a back stack and
 * integrates with [BackHandler] for system back button support.
 *
 * @param navController The [NavHostController] that manages the navigation state
 * @param startDestination The route of the initial destination
 * @param modifier Modifier to be applied to the NavHost container
 * @param transitionSpec Custom transition animation specification.
 *   Defaults to a fade-in/fade-out crossfade animation.
 * @param builder The [NavGraphBuilder] DSL block for defining navigation destinations
 *
 * Usage:
 * ```kotlin
 * val navController = rememberNavHostController()
 *
 * NavHost(
 *     navController = navController,
 *     startDestination = "home"
 * ) {
 *     composable("home") { HomeScreen() }
 *     composable("detail/{id}") { entry ->
 *         DetailScreen(entry.getStringArgument("id") ?: "")
 *     }
 * }
 * ```
 *
 * @see NavHostController
 * @see NavGraphBuilder
 * @see rememberNavHostController
 */
@Composable
fun NavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ContentTransform = {
        fadeIn() togetherWith fadeOut()
    },
    builder: NavGraphBuilder.() -> Unit
) {
    // Build the navigation graph (re-run on every recomposition so dynamic builder changes take effect)
    NavGraphBuilder(navController).apply(builder)

    // Navigate to the start destination if back stack is empty
    LaunchedEffect(navController, startDestination) {
        if (navController.backStack.isEmpty()) {
            navController.navigate(startDestination)
        }
    }

    // Provide the navController via CompositionLocal
    CompositionLocalProvider(LocalNavHostController provides navController) {
        // Handle system back press
        if (navController.canNavigateBack) {
            BackHandler {
                navController.popBackStack()
            }
        }

        // SaveableStateHolder preserves the state of each destination when navigating
        val saveableStateHolder = rememberSaveableStateHolder()

        // Display current destination with animated transitions
        val currentEntry = navController.currentBackStackEntry
        if (currentEntry != null) {
            AnimatedContent(
                targetState = currentEntry,
                modifier = modifier,
                transitionSpec = transitionSpec,
                contentKey = { it.id }
            ) { entry ->
                // Use SaveableStateProvider to save/restore state per destination
                saveableStateHolder.SaveableStateProvider(entry.id) {
                    entry.destination.content(entry)
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
}

/**
 * Convenience overload of [NavHost] that automatically creates and remembers a
 * [NavHostController].
 *
 * @param startDestination The route of the initial destination
 * @param modifier Modifier to be applied to the NavHost container
 * @param transitionSpec Custom transition animation specification
 * @param builder The [NavGraphBuilder] DSL block for defining navigation destinations
 * @return The [NavHostController] created for this NavHost
 *
 * Usage:
 * ```kotlin
 * NavHost(startDestination = "home") {
 *     composable("home") { HomeScreen() }
 *     composable("detail/{id}") { entry ->
 *         DetailScreen(entry.getStringArgument("id") ?: "")
 *     }
 * }
 * ```
 */
@Composable
fun NavHost(
    startDestination: String,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ContentTransform = {
        fadeIn() togetherWith fadeOut()
    },
    builder: NavGraphBuilder.() -> Unit
): NavHostController {
    val navController = rememberNavHostController()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        transitionSpec = transitionSpec,
        builder = builder
    )
    return navController
}
