package com.tencent.kuikly.compose.profiler.filter

/**
 * Interface for filtering Composable functions in recomposition tracking.
 *
 * Implementations should be stateless and thread-safe where possible.
 * Filtering decisions may be cached by the calling context.
 *
 * The filter is applied to both the composable name (e.g., "CounterSection")
 * and the full info string (e.g., "CounterSection (Demo.kt:195)").
 */
interface ComposableFilter {
    /**
     * Determines whether a Composable should be filtered (excluded) from tracking.
     *
     * @param composableName The simple name of the Composable (e.g., "CounterSection")
     * @param info The full info string from traceEventStart (e.g., "CounterSection (Demo.kt:195)")
     * @return true if the Composable should be filtered out, false if it should be tracked
     */
    fun shouldFilter(composableName: String, info: String): Boolean

    /**
     * Optional: Returns a human-readable description of this filter for debugging/logging.
     */
    fun description(): String = this::class.simpleName ?: "ComposableFilter"

    /**
     * Optional: Returns whether this filter is currently enabled.
     * Disabled filters should return false from shouldFilter.
     */
    fun isEnabled(): Boolean = true
}
