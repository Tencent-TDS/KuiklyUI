package com.tencent.kuikly.compose.profiler.filter

/**
 * Manages a chain of filters with result caching for improved performance.
 *
 * This is the entry point for applying filters in the recomposition tracker.
 * It combines multiple independent filters (OR logic) and caches results
 * to avoid redundant filter evaluations.
 *
 * Typical usage:
 * ```
 * val chain = FilterChain(
 *     filters = listOf(
 *         PrefixComposableFilter.jetpackComposeFramework(),
 *         ExclusionComposableFilter.fromList(listOf("TempComposable"))
 *     ),
 *     enableBuiltinFilters = true
 * )
 *
 * // In the main tracking loop:
 * if (chain.shouldFilter(composableName, info)) {
 *     // Skip this Composable
 * }
 * ```
 */
class FilterChain(
    private val filters: List<ComposableFilter> = emptyList(),
    private val enableBuiltinFilters: Boolean = true,
    private val cacheEnabled: Boolean = true
) {
    // cache key = composableName（不含文件位置），命中率更高
    private val filterCache = mutableMapOf<String, Boolean>()
    private val maxCacheSize = 10_000  // Prevent unbounded memory growth

    init {
        // 移除强制 require：filters 为空且 enableBuiltinFilters=false 等同于「不过滤任何东西」，
        // 这是合理的初始/重置状态，不应抛异常。
    }

    /**
     * Determines whether a Composable should be filtered (excluded) from tracking.
     *
     * @param composableName The simple name of the Composable (e.g., "CounterSection")
     * @param info The full info string from traceEventStart (e.g., "CounterSection (Demo.kt:195)")
     * @return true if the Composable should be filtered out, false if it should be tracked
     */
    fun shouldFilter(composableName: String, info: String): Boolean {
        if (filters.isEmpty() && !enableBuiltinFilters) {
            return false  // No filters enabled, track everything
        }

        // Check cache first — key = composableName（不含源码位置，命中率更高）
        if (cacheEnabled) {
            val cachedResult = filterCache[composableName]
            if (cachedResult != null) {
                return cachedResult
            }
        }

        // Evaluate filters
        val result = evaluateFilters(composableName, info)

        // Cache result if enabled and cache isn't too large
        if (cacheEnabled && filterCache.size < maxCacheSize) {
            filterCache[composableName] = result
        }

        return result
    }

    /**
     * Clears the filter result cache.
     * Useful when filter configuration changes at runtime.
     */
    fun clearCache() {
        filterCache.clear()
    }

    /**
     * Returns cache statistics for debugging/monitoring.
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = filterCache.size,
            maxSize = maxCacheSize,
            enabled = cacheEnabled
        )
    }

    /**
     * Evaluates all filters (OR logic) for the given Composable.
     */
    private fun evaluateFilters(composableName: String, info: String): Boolean {
        // Check custom filters first (typically faster)
        for (filter in filters) {
            if (filter.isEnabled() && filter.shouldFilter(composableName, info)) {
                return true  // Any custom filter matches, exclude
            }
        }

        // Check builtin framework filters
        if (enableBuiltinFilters && isBuiltinFrameworkComposable(info)) {
            return true
        }

        return false  // No filter matched, include
    }

    /**
     * Built-in framework Composable detection (from original implementation).
     * This is kept for backward compatibility with existing configuration.
     */
    private fun isBuiltinFrameworkComposable(info: String): Boolean {
        return frameworkPrefixes.any { prefix -> info.startsWith(prefix) }
    }

    data class CacheStats(
        val size: Int,
        val maxSize: Int,
        val enabled: Boolean
    )

    companion object {
        /**
         * Framework package prefixes to filter by default.
         * These match the original hardcoded values from RecompositionTracker.
         */
        private val frameworkPrefixes = listOf(
            "androidx.compose.",
            "androidx.activity.compose.",
            "androidx.navigation.compose.",
            "androidx.lifecycle.compose.",
            "androidx.tv.compose.",
            "androidx.paging.compose.",
            "androidx.constraintlayout.compose.",
            "com.tencent.kuikly.compose.ui.",
            "com.tencent.kuikly.compose.foundation.",
            "com.tencent.kuikly.compose.layout.",
            "com.tencent.kuikly.compose.material.",
            "com.tencent.kuikly.compose.component.",
            "com.tencent.kuikly.compose.theme.",
            "com.tencent.kuikly.compose.model."
        )

        /**
         * Creates a filter chain with default framework filters enabled.
         */
        fun withDefaults(additionalFilters: List<ComposableFilter> = emptyList()): FilterChain {
            return FilterChain(
                filters = additionalFilters,
                enableBuiltinFilters = true,
                cacheEnabled = true
            )
        }

        /**
         * Creates a filter chain that only uses custom filters (no builtin filtering).
         */
        fun withCustomFiltersOnly(filters: List<ComposableFilter>): FilterChain {
            return FilterChain(
                filters = filters,
                enableBuiltinFilters = false,
                cacheEnabled = true
            )
        }

        /**
         * Creates a filter chain with builtin filtering disabled.
         */
        fun withoutBuiltins(filters: List<ComposableFilter> = emptyList()): FilterChain {
            return FilterChain(
                filters = filters,
                enableBuiltinFilters = false,
                cacheEnabled = true
            )
        }
    }
}
