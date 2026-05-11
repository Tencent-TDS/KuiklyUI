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
 * NavGraph is a collection of [NavDestination] nodes fetchable by route.
 *
 * A NavGraph serves as a 'virtual' destination: while the NavGraph itself will not appear
 * on the back stack, navigating to the NavGraph will cause the
 * [startDestinationRoute] to be added to the back stack.
 *
 * Construct a new NavGraph. This NavGraph is not valid until you
 * [add a destination][addDestination] and [set the starting destination][setStartDestination].
 *
 * @param navGraphNavigator The [NavGraphNavigator] which this destination will be associated
 *                          with. Generally retrieved via a [NavigatorProvider].
 *
 * @see NavGraphBuilder
 * @see NavHostController
 * @see NavGraphNavigator
 */
open class NavGraph(
    navGraphNavigator: Navigator<out NavGraph>
) : NavDestination(navGraphNavigator), Iterable<NavDestination> {

    private val nodes = mutableMapOf<String, NavDestination>()

    /**
     * The route of the start destination for this NavGraph. When navigating to the
     * NavGraph, the destination represented by this route is the one the user will initially see.
     */
    var startDestinationRoute: String? = null
        private set

    /**
     * Sets the starting destination for this NavGraph.
     *
     * @param startDestRoute The route of the destination to be shown when navigating to this
     *                    NavGraph.
     */
    fun setStartDestination(startDestRoute: String) {
        startDestinationRoute = startDestRoute
    }

    /**
     * Adds a destination to this NavGraph. The destination must have a route set.
     *
     * The destination must not have a [parent][NavDestination.parent] set. If
     * the destination is already part of a [NavGraph], call
     * [remove] before calling this method.
     *
     * @param node destination to add
     * @throws IllegalArgumentException if destination does not have a route set
     */
    fun addDestination(node: NavDestination) {
        val route = node.route
            ?: throw IllegalArgumentException(
                "Destinations added to a NavGraph must have a route set. " +
                    "Call setRoute() on the destination before adding it."
            )
        require(node.parent == null) {
            "Destination already has a parent set. Call NavGraph.remove() first."
        }
        node.parent = this
        nodes[route] = node
    }

    /**
     * Adds multiple destinations to this NavGraph. Each destination must have a route set.
     *
     * @param nodes destinations to add
     */
    fun addDestinations(nodes: Collection<NavDestination?>) {
        nodes.filterNotNull().forEach { addDestination(it) }
    }

    /**
     * Adds multiple destinations to this NavGraph. Each destination must have a route set.
     *
     * @param nodes destinations to add
     */
    fun addDestinations(vararg nodes: NavDestination) {
        nodes.forEach { addDestination(it) }
    }

    /**
     * Finds a destination in the collection by route. This will recursively check the
     * [parent] of this navigation graph if node is not found in this navigation graph.
     *
     * @param route Route to locate
     * @return the node with route, or null if not found
     */
    fun findNode(route: String?): NavDestination? {
        if (route == null) return null

        // Check direct children
        nodes[route]?.let { return it }

        // Try pattern matching
        for ((_, dest) in nodes) {
            if (dest is NavGraph) {
                dest.findNode(route)?.let { return it }
            } else if (dest.matchRoute(route) != null) {
                return dest
            }
        }

        // Check parent
        return parent?.findNode(route)
    }

    /**
     * @throws NoSuchElementException if there are no more elements
     */
    override fun iterator(): MutableIterator<NavDestination> {
        return nodes.values.toMutableList().iterator()
    }

    /**
     * Add all destinations from another collection to this one. As each destination has at most
     * one parent, the destinations will be removed from the given NavGraph.
     *
     * @param other collection of destinations to add. All destinations will be removed from this
     * graph after being added to this graph.
     */
    fun addAll(other: NavGraph) {
        val otherNodes = other.nodes.values.toList()
        other.clear()
        otherNodes.forEach { addDestination(it) }
    }

    /**
     * Remove a given destination from this NavGraph.
     *
     * @param node the destination to remove.
     */
    fun remove(node: NavDestination) {
        val route = node.route ?: return
        if (nodes.remove(route) != null) {
            node.parent = null
        }
    }

    /**
     * Clear all destinations from this navigation graph.
     */
    fun clear() {
        nodes.values.forEach { it.parent = null }
        nodes.clear()
    }

    /**
     * Internal helper: find a destination that matches the given route within this graph
     * and all nested graphs. Does NOT check parent.
     */
    internal fun findDestinationInternal(route: String): NavDestination? {
        // Direct match
        nodes[route]?.let { return it }

        // Pattern matching or nested graph search
        for ((_, dest) in nodes) {
            if (dest is NavGraph) {
                // If navigating to a nested graph's route, resolve to its start destination
                if (dest.route == route) {
                    return dest.findDestinationInternal(dest.startDestinationRoute ?: return null)
                }
                dest.findDestinationInternal(route)?.let { return it }
            } else if (dest.matchRoute(route) != null) {
                return dest
            }
        }
        return null
    }

    /**
     * Resolve a route that might be a [NavGraph] route to the actual start destination route.
     */
    internal fun resolveStartDestination(route: String): String {
        val node = nodes[route]
        if (node is NavGraph) {
            return node.resolveStartDestination(node.startDestinationRoute ?: route)
        }
        return route
    }

    override fun toString(): String =
        "NavGraph(route=$route, startDestination=$startDestinationRoute, " +
            "nodes=${nodes.keys})"

    companion object {
        /**
         * Finds the actual start destination of the graph, handling cases where the graph's starting
         * destination is itself a NavGraph.
         *
         * @return the actual startDestination of the given graph.
         */
        fun NavGraph.findStartDestination(): NavDestination {
            var startDest: NavDestination = this
            while (startDest is NavGraph) {
                val startRoute = startDest.startDestinationRoute ?: break
                val next = startDest.findNode(startRoute) ?: break
                if (next == startDest) break
                startDest = next
            }
            return startDest
        }
    }
}

/**
 * Returns the destination with `route`.
 *
 * @throws IllegalArgumentException if no destination is found with that route.
 */
@Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
inline operator fun NavGraph.get(route: String): NavDestination =
    findNode(route)
        ?: throw IllegalArgumentException("No destination for $route was found in $this")

/** Returns `true` if a destination with `route` is found in this navigation graph. */
operator fun NavGraph.contains(route: String): Boolean = findNode(route) != null

/**
 * Adds a destination to this NavGraph.
 */
@Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
inline operator fun NavGraph.plusAssign(node: NavDestination) {
    addDestination(node)
}

/**
 * Add all destinations from another collection to this one.
 */
@Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
inline operator fun NavGraph.plusAssign(other: NavGraph) {
    addAll(other)
}

/** Removes `node` from this navigation graph. */
@Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
inline operator fun NavGraph.minusAssign(node: NavDestination) {
    remove(node)
}