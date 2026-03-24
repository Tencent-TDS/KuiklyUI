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
 * Marker annotation for navigation DSL builders.
 */
@DslMarker
annotation class NavDestinationDsl

/**
 * DSL for constructing a new [NavDestination].
 *
 * This is the base class for all destination builders. Subclasses like [NavGraphBuilder]
 * provide additional functionality.
 *
 * @param navigator navigator used to create the destination
 * @param route the destination's unique route
 *
 * @see NavGraphBuilder
 * @see NavDestination
 */
@NavDestinationDsl
open class NavDestinationBuilder<out D : NavDestination>(
    /**
     * The navigator the destination that will be used in [instantiateDestination]
     * to create the destination.
     */
    protected val navigator: Navigator<out D>,
    /**
     * The destination's unique route.
     */
    val route: String?
) {
    /**
     * The descriptive label of the destination.
     */
    var label: CharSequence? = null

    private val arguments = mutableMapOf<String, NavArgument>()
    private val deepLinks = mutableListOf<NavDeepLink>()

    /**
     * Add a [NavArgument] to this destination.
     *
     * @param name the name of the argument
     * @param argumentBuilder lambda to configure the argument
     */
    fun argument(name: String, argumentBuilder: NavArgumentBuilder.() -> Unit) {
        arguments[name] = NavArgumentBuilder().apply(argumentBuilder).build()
    }

    /**
     * Add a [NavArgument] to this destination.
     *
     * @param name the name of the argument
     * @param argument the [NavArgument] to add
     */
    fun argument(name: String, argument: NavArgument) {
        arguments[name] = argument
    }

    /**
     * Add a deep link to this destination.
     *
     * @param uriPattern The uri pattern to add as a deep link
     * @see deepLink
     */
    fun deepLink(uriPattern: String) {
        deepLinks.add(NavDeepLink(uriPattern = uriPattern))
    }

    /**
     * Add a deep link to this destination.
     *
     * @param navDeepLink builder lambda for configuring the [NavDeepLink]
     */
    fun deepLink(navDeepLink: NavDeepLinkDslBuilder.() -> Unit) {
        deepLinks.add(NavDeepLinkDslBuilder().apply(navDeepLink).build())
    }

    /**
     * Add a deep link to this destination.
     *
     * @param navDeepLink the [NavDeepLink] to add
     */
    fun deepLink(navDeepLink: NavDeepLink) {
        deepLinks.add(navDeepLink)
    }

    /**
     * Instantiate a new instance of [D] that will be passed to [build].
     *
     * By default, this calls [Navigator.createDestination] on [navigator], but can
     * be overridden to call a custom constructor, etc.
     */
    protected open fun instantiateDestination(): D {
        return navigator.createDestination()
    }

    /**
     * Build the NavDestination by calling [Navigator.createDestination].
     */
    open fun build(): D {
        val dest = instantiateDestination()
        if (dest is NavDestination) {
            dest.route = route
            dest.label = label
            arguments.forEach { (name, argument) ->
                dest.addArgument(name, argument)
            }
            deepLinks.forEach { deepLink ->
                dest.addDeepLink(deepLink)
            }
        }
        return dest
    }
}

/**
 * DSL builder for [NavDeepLink], used within [NavDestinationBuilder.deepLink].
 *
 * This mirrors the official `NavDeepLinkDslBuilder` used inside `NavDestinationBuilder`.
 */
class NavDeepLinkDslBuilder {
    /**
     * The uri pattern of the deep link.
     */
    var uriPattern: String? = null

    /**
     * The action of the deep link.
     */
    var action: String? = null

    /**
     * The MIME type of the deep link.
     */
    var mimeType: String? = null

    @PublishedApi
    internal fun build(): NavDeepLink = NavDeepLink(
        uriPattern = uriPattern,
        action = action,
        mimeType = mimeType
    )
}
