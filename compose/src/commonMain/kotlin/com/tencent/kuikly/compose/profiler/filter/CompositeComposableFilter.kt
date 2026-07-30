package com.tencent.kuikly.compose.profiler.filter

/**
 * Composite filter that combines multiple filters with AND or OR logic.
 *
 * - **AND mode**: All filters must match (Composable is filtered only if ALL filters say to filter it)
 * - **OR mode**: Any filter matches (Composable is filtered if ANY filter says to filter it)
 *
 * The filter automatically reorders filters by expected performance (fast filters first)
 * to optimize execution time.
 *
 * Filter execution order:
 * 1. ExclusionComposableFilter (O(1) set lookup - fastest)
 * 2. PrefixComposableFilter (O(n) linear scan of prefixes)
 * 3. RegexComposableFilter (O(m) regex compilation - slowest)
 *
 * Example:
 * ```
 * val compositeFilter = CompositeComposableFilter(
 *     filters = listOf(
 *         PrefixComposableFilter.jetpackComposeFramework(),
 *         ExclusionComposableFilter.fromList(listOf("MyCustomComposable"))
 *     ),
 *     mode = CompositeMode.OR
 * )
 * ```
 */
enum class CompositeMode {
    /**
     * All filters must match for the Composable to be filtered.
     * Short-circuits on first non-matching filter.
     */
    AND,

    /**
     * Any filter can match for the Composable to be filtered.
     * Short-circuits on first matching filter.
     */
    OR
}

class CompositeComposableFilter(
    private val filters: List<ComposableFilter>,
    private val mode: CompositeMode = CompositeMode.OR,
    private val optimizeOrder: Boolean = true,
    private val enabled: Boolean = true
) : ComposableFilter {
    init {
        require(filters.isNotEmpty()) { "At least one filter must be specified" }
    }

    private val orderedFilters: List<ComposableFilter> = if (optimizeOrder) {
        optimizeFilterOrder(filters)
    } else {
        filters
    }

    override fun shouldFilter(composableName: String, info: String): Boolean {
        if (!enabled) return false

        return when (mode) {
            CompositeMode.AND -> {
                // 所有「已启用」的 filter 都必须命中才过滤；跳过 disabled filter（不参与判断）
                val enabledFilters = orderedFilters.filter { it.isEnabled() }
                if (enabledFilters.isEmpty()) return false
                enabledFilters.all { filter -> filter.shouldFilter(composableName, info) }
            }

            CompositeMode.OR -> {
                // 任意「已启用」的 filter 命中即过滤；跳过 disabled filter
                orderedFilters.any { filter ->
                    filter.isEnabled() && filter.shouldFilter(composableName, info)
                }
            }
        }
    }

    override fun description(): String {
        val modeStr = if (mode == CompositeMode.AND) "AND" else "OR"
        val filterDescs = orderedFilters.map { it.description() }
        return "CompositeFilter($modeStr, ${filters.size} filters: ${filterDescs.take(2).joinToString(", ")}${if (filterDescs.size > 2) ", ..." else ""})"
    }

    override fun isEnabled(): Boolean = enabled

    /**
     * Reorders filters by estimated performance, putting fastest filters first.
     * This allows short-circuit evaluation to skip slower filters when possible.
     */
    private fun optimizeFilterOrder(filters: List<ComposableFilter>): List<ComposableFilter> {
        return filters.sortedWith(compareBy { filter ->
            when (filter) {
                is ExclusionComposableFilter -> 0  // O(1) - fastest
                is PrefixComposableFilter -> 1     // O(n) - medium
                is RegexComposableFilter -> 2      // O(m) - slowest
                else -> 3                          // Unknown - assume slow
            }
        })
    }

    companion object {
        /**
         * Creates an OR-mode composite filter from multiple filters.
         * Short-circuits on first matching filter.
         */
        fun or(vararg filters: ComposableFilter): CompositeComposableFilter {
            return CompositeComposableFilter(filters.toList(), CompositeMode.OR)
        }

        /**
         * Creates an AND-mode composite filter from multiple filters.
         * Short-circuits on first non-matching filter.
         */
        fun and(vararg filters: ComposableFilter): CompositeComposableFilter {
            return CompositeComposableFilter(filters.toList(), CompositeMode.AND)
        }

        /**
         * Creates a composite filter with all standard framework filters.
         * Useful as a "catch-all" for filtering framework Composables.
         */
        fun allFrameworks(): CompositeComposableFilter {
            return CompositeComposableFilter(
                filters = listOf(
                    PrefixComposableFilter.allFrameworks(),
                    RegexComposableFilter.internalComposables(),
                    RegexComposableFilter.generatedComposables()
                ),
                mode = CompositeMode.OR
            )
        }

        /**
         * Creates a composite filter for debugging, excluding internal/generated/debug Composables.
         */
        fun debugMode(): CompositeComposableFilter {
            return CompositeComposableFilter(
                filters = listOf(
                    RegexComposableFilter.internalComposables(),
                    RegexComposableFilter.generatedComposables(),
                    RegexComposableFilter.debugComposables()
                ),
                mode = CompositeMode.OR
            )
        }
    }
}
