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

/**
 * DSL builder for constructing a navigation graph.
 *
 * Migrated from official `androidx.navigation.NavGraphBuilder`.
 * This version is platform-independent and works in commonMain.
 *
 * Usage:
 * ```kotlin
 * NavHost(navController, startDestination = "home") {
 *     composable("home") { HomeScreen() }
 *     composable("detail/{id}") { entry ->
 *         DetailScreen(entry.getStringArgument("id") ?: "")
 *     }
 * }
 * ```
 */
class NavGraphBuilder internal constructor(
    private val navController: NavHostController
) {
    internal val destinations = mutableListOf<NavDestination>()

    /**
     * Register a composable destination with the given [route] pattern.
     *
     * @param route The route pattern for this destination (e.g. "detail/{id}")
     * @param content The composable content for this destination
     */
    fun composable(
        route: String,
        content: @Composable (NavBackStackEntry) -> Unit
    ) {
        val destination = NavDestination(route, content)
        destinations.add(destination)
        navController.addDestination(destination)
    }
}
