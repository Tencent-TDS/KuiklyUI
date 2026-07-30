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
 * NavDeepLink encapsulates the data associated with a deep link to a specific destination.
 *
 * A NavDeepLink can match by URI pattern, action, or MIME type. This implementation
 * supports URI pattern matching with path and query parameter placeholders.
 *
 * This is aligned with the official `androidx.navigation.NavDeepLink` from
 * Jetpack Compose Navigation 1.7.3, adapted for cross-platform usage.
 *
 * @see navDeepLink
 * @see NavGraphBuilder.composable
 */
class NavDeepLink @PublishedApi internal constructor(
    /**
     * The URI pattern for this deep link.
     *
     * Supports placeholders: `"https://example.com/user/{userId}"`
     */
    val uriPattern: String? = null,
    /**
     * The action for this deep link (e.g. "android.intent.action.VIEW").
     */
    val action: String? = null,
    /**
     * The MIME type for this deep link (e.g. `image/{asterisk}`).
     */
    val mimeType: String? = null
) {
    /**
     * The compiled regex for matching URIs against [uriPattern].
     */
    internal val uriRegex: Regex? by lazy {
        uriPattern?.let { pattern ->
            val regexPattern = pattern
                .replace(Regex("\\{[^}]+\\}"), "([^/]+)")
                .replace("?", "\\?")
                .replace(".", "\\.")
            Regex("^$regexPattern$")
        }
    }

    /**
     * The argument names extracted from the [uriPattern].
     * e.g. "https://example.com/user/{userId}" -> ["userId"]
     */
    internal val argumentNames: List<String> by lazy {
        uriPattern?.let { pattern ->
            val regex = Regex("\\{([^}]+)\\}")
            regex.findAll(pattern).map { it.groupValues[1] }.toList()
        } ?: emptyList()
    }

    /**
     * Try to match a URI against this deep link's pattern.
     *
     * @param uri The URI to match
     * @return A Bundle with argument names to values if matched, or null if not matched
     */
    internal fun matchUri(uri: String): Bundle? {
        val regex = uriRegex ?: return null
        val matchResult = regex.matchEntire(uri) ?: return null
        val bundle = Bundle()
        argumentNames.forEachIndexed { index, name ->
            bundle.putString(name, matchResult.groupValues[index + 1])
        }
        return bundle
    }

    override fun toString(): String =
        "NavDeepLink(uriPattern=$uriPattern, action=$action, mimeType=$mimeType)"
}

/**
 * DSL for constructing a new [NavDeepLink].
 *
 * Usage:
 * ```kotlin
 * composable(
 *     "detail/{id}",
 *     deepLinks = listOf(
 *         navDeepLink {
 *             uriPattern = "https://example.com/detail/{id}"
 *             action = "android.intent.action.VIEW"
 *         }
 *     )
 * ) { backStackEntry ->
 *     DetailScreen(backStackEntry.arguments?.getString("id") ?: "")
 * }
 * ```
 *
 * @param deepLinkBuilder the builder DSL used to construct this DeepLink
 * @return the resulting [NavDeepLink]
 */
inline fun navDeepLink(
    deepLinkBuilder: NavDeepLinkBuilder.() -> Unit
): NavDeepLink {
    return NavDeepLinkBuilder().apply(deepLinkBuilder).build()
}

/**
 * DSL builder for constructing [NavDeepLink] instances.
 *
 * @see navDeepLink
 */
class NavDeepLinkBuilder {
    /**
     * The URI pattern for the deep link.
     *
     * Supports placeholders in the form of `{argName}`:
     * ```kotlin
     * uriPattern = "https://example.com/user/{userId}"
     * ```
     */
    var uriPattern: String? = null

    /**
     * The action for the deep link.
     *
     * ```kotlin
     * action = "android.intent.action.VIEW"
     * ```
     */
    var action: String? = null

    /**
     * The MIME type for the deep link.
     *
     * ```kotlin
     * mimeType = "image/png"
     * ```
     */
    var mimeType: String? = null

    @PublishedApi internal fun build(): NavDeepLink = NavDeepLink(
        uriPattern = uriPattern,
        action = action,
        mimeType = mimeType
    )
}
