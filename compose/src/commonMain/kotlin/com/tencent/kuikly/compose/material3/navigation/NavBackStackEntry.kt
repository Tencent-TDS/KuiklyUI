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

/**
 * Represents an entry in the navigation back stack.
 *
 * Migrated from official `androidx.navigation.NavBackStackEntry` to a cross-platform version
 * that does not depend on any Android-specific APIs.
 *
 * @param route The route string of this entry (e.g. "detail/123")
 * @param destination The [NavDestination] associated with this entry
 * @param arguments The arguments extracted from the route
 */
class NavBackStackEntry internal constructor(
    val route: String,
    val destination: NavDestination,
    val arguments: Map<String, String> = emptyMap(),
    internal val id: String = generateId()
) {
    /**
     * Get a string argument by key.
     */
    fun getStringArgument(key: String): String? = arguments[key]

    /**
     * Get an int argument by key with a default value.
     */
    fun getIntArgument(key: String, defaultValue: Int = 0): Int =
        arguments[key]?.toIntOrNull() ?: defaultValue

    /**
     * Get a long argument by key with a default value.
     */
    fun getLongArgument(key: String, defaultValue: Long = 0L): Long =
        arguments[key]?.toLongOrNull() ?: defaultValue

    /**
     * Get a boolean argument by key with a default value.
     */
    fun getBooleanArgument(key: String, defaultValue: Boolean = false): Boolean =
        arguments[key]?.toBooleanStrictOrNull() ?: defaultValue

    /**
     * Get a float argument by key with a default value.
     */
    fun getFloatArgument(key: String, defaultValue: Float = 0f): Float =
        arguments[key]?.toFloatOrNull() ?: defaultValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NavBackStackEntry) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "NavBackStackEntry(route=$route, id=$id)"

    companion object {
        private var counter = 0L

        private fun generateId(): String = "entry-${counter++}"
    }
}

/**
 * Represents a navigation destination defined in the navigation graph.
 *
 * @param route The route pattern (e.g. "detail/{id}")
 * @param content The composable content to display for this destination
 */
class NavDestination internal constructor(
    val route: String,
    internal val content: @androidx.compose.runtime.Composable (NavBackStackEntry) -> Unit
) {
    /**
     * The argument placeholders extracted from the route pattern.
     * e.g. "detail/{id}/{name}" -> ["id", "name"]
     */
    internal val argumentNames: List<String> = buildList {
        val regex = Regex("\\{([^}]+)\\}")
        regex.findAll(route).forEach { matchResult ->
            add(matchResult.groupValues[1])
        }
    }

    /**
     * The route pattern converted to a regex for matching actual routes.
     */
    internal val routePattern: Regex by lazy {
        val pattern = route
            .replace(Regex("\\{[^}]+\\}"), "([^/]+)")
            .replace("?", "\\?")
        Regex("^$pattern$")
    }

    /**
     * Try to match an actual route string against this destination's pattern.
     * Returns the extracted arguments if matched, or null if not matched.
     */
    internal fun matchRoute(actualRoute: String): Map<String, String>? {
        val matchResult = routePattern.matchEntire(actualRoute) ?: return null
        val args = mutableMapOf<String, String>()
        argumentNames.forEachIndexed { index, name ->
            args[name] = matchResult.groupValues[index + 1]
        }
        return args
    }
}
