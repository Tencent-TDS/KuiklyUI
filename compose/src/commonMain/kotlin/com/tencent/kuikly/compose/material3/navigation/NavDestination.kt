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

/**
 * NavDestination represents one node within an overall navigation graph.
 *
 * Each destination is associated with a [Navigator] which knows how to navigate to this
 * particular destination.
 *
 * Each destination has a set of [arguments] that will be applied when
 * navigating to that destination. Any default values for those arguments can be overridden
 * at the time of navigation.
 *
 * NavDestinations should be created via [Navigator.createDestination].
 *
 * @param navigatorName the name of the [Navigator] associated with this destination
 *
 * @see Navigator
 * @see NavGraph
 * @see NavGraphBuilder
 */
open class NavDestination(
    /**
     * The name associated with this destination's [Navigator].
     */
    val navigatorName: String
) {
    /**
     * Gets the [NavGraph] that contains this destination. This will be set when a
     * destination is added to a NavGraph via [NavGraph.addDestination].
     */
    var parent: NavGraph? = null
        internal set

    /**
     * The descriptive label of this destination.
     */
    var label: CharSequence? = null

    /**
     * The destination's unique route.
     *
     * @throws IllegalArgumentException if the given route is empty
     */
    var route: String? = null
        set(value) {
            if (value != null && value.isEmpty()) {
                throw IllegalArgumentException("Route must not be empty")
            }
            field = value
            // Rebuild lazy caches when route changes
            cachedArgumentNames = null
            cachedRoutePattern = null
        }

    private val argMap = mutableMapOf<String, NavArgument>()

    /**
     * The arguments supported by this destination. Returns a read-only map of argument names
     * to [NavArgument] objects that can be used to check the type, default value
     * and nullability of the argument.
     *
     * To add and remove arguments for this NavDestination
     * use [addArgument] and [removeArgument].
     */
    val arguments: Map<String, NavArgument>
        get() = argMap

    private val deepLinkList = mutableListOf<NavDeepLink>()

    /**
     * The deep links associated with this destination.
     */
    val deepLinks: List<NavDeepLink>
        get() = deepLinkList

    /**
     * NavDestination constructor that takes a [Navigator].
     *
     * @param navigator navigator used for this destination
     */
    constructor(navigator: Navigator<out NavDestination>) : this(navigator.name)

    /**
     * Add a [NavArgument] to this destination.
     *
     * @param argumentName the name of the argument
     * @param argument the [NavArgument] to add
     */
    fun addArgument(argumentName: String, argument: NavArgument) {
        argMap[argumentName] = argument
    }

    /**
     * Remove an argument from this destination.
     *
     * @param argumentName the name of the argument to remove
     */
    fun removeArgument(argumentName: String) {
        argMap.remove(argumentName)
    }

    /**
     * Add a deep link to this destination.
     *
     * @param navDeepLink the [NavDeepLink] to add
     */
    fun addDeepLink(navDeepLink: NavDeepLink) {
        deepLinkList.add(navDeepLink)
    }

    /**
     * Add a deep link to this destination.
     *
     * @param uriPattern the URI pattern for the deep link
     */
    fun addDeepLink(uriPattern: String) {
        deepLinkList.add(NavDeepLink(uriPattern = uriPattern))
    }

    // --- Route matching utilities ---

    private var cachedArgumentNames: List<String>? = null

    /**
     * The argument placeholders extracted from the route pattern.
     * e.g. "detail/{id}/{name}" -> ["id", "name"]
     */
    internal val argumentNames: List<String>
        get() {
            if (cachedArgumentNames == null) {
                cachedArgumentNames = buildList {
                    val r = route ?: return@buildList
                    val regex = Regex("\\{([^}]+)\\}")
                    regex.findAll(r).forEach { matchResult ->
                        add(matchResult.groupValues[1])
                    }
                }
            }
            return cachedArgumentNames!!
        }

    private var cachedRoutePattern: Regex? = null

    /**
     * The route pattern converted to a regex for matching actual routes.
     */
    internal val routePattern: Regex
        get() {
            if (cachedRoutePattern == null) {
                val r = route ?: return Regex("$^") // will never match
                val pattern = r
                    .replace(Regex("\\{[^}]+\\}"), "([^/]+)")
                    .replace("?", "\\?")
                cachedRoutePattern = Regex("^$pattern$")
            }
            return cachedRoutePattern!!
        }

    /**
     * Try to match an actual route string against this destination's pattern.
     *
     * @param actualRoute The actual route string (e.g. "detail/123")
     * @return A Bundle with argument names to values if matched, or null if not matched
     */
    internal fun matchRoute(actualRoute: String): Bundle? {
        val matchResult = routePattern.matchEntire(actualRoute) ?: return null
        val bundle = Bundle()
        argumentNames.forEachIndexed { index, name ->
            bundle.putString(name, matchResult.groupValues[index + 1])
        }
        return bundle
    }

    /**
     * Try to match a deep link URI against this destination's deep links.
     *
     * @param uri The URI to match
     * @return A Bundle with argument names to values if matched, or null if no deep link matches
     */
    internal fun matchDeepLink(uri: String): Bundle? {
        for (deepLink in deepLinkList) {
            deepLink.matchUri(uri)?.let { return it }
        }
        return null
    }

    /**
     * The hierarchy of [NavDestination]s from this destination to the root graph.
     */
    val hierarchy: Sequence<NavDestination>
        get() = generateSequence(this) { it.parent }

    override fun toString(): String =
        "NavDestination(route=$route, navigatorName=$navigatorName)"
}
