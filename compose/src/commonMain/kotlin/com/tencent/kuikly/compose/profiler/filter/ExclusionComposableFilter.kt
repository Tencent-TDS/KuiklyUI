package com.tencent.kuikly.compose.profiler.filter

/**
 * Filter that explicitly excludes specific Composable names from tracking.
 *
 * This is useful for excluding specific Composables by exact name match.
 * The set-based lookup provides O(1) performance.
 *
 * Example:
 * ```
 * val exclusionFilter = ExclusionComposableFilter(
 *     exclusionSet = setOf(
 *         "CounterSection",
 *         "DebugOverlay",
 *         "TempComposable"
 *     )
 * )
 * ```
 */
class ExclusionComposableFilter(
    private val exclusionSet: Set<String> = emptySet(),
    private val enabled: Boolean = true
) : ComposableFilter {

    override fun shouldFilter(composableName: String, info: String): Boolean {
        // isEnabled() 由 FilterChain 在调用前检查，此处无需重复判断
        if (composableName in exclusionSet) return true
        // 支持带源码位置的精确排除（如 "invoke @FeedsDoubleColumnCard.kt:47"）
        val srcLoc = extractSourceLocation(info)
        if (srcLoc != null) {
            val qualifiedKey = "$composableName @$srcLoc"
            if (qualifiedKey in exclusionSet) return true
        }
        return false
    }

    /**
     * 从 CompositionTracer 的 info 字符串中提取源码位置。
     * 例如 "CounterSection (Demo.kt:195)" → "Demo.kt:195"
     */
    private fun extractSourceLocation(info: String): String? {
        val startIndex = info.lastIndexOf('(')
        val endIndex = info.lastIndexOf(')')
        if (startIndex < 0 || endIndex < 0 || endIndex <= startIndex) return null
        return info.substring(startIndex + 1, endIndex).takeIf { it.isNotEmpty() }
    }

    override fun description(): String {
        return "ExclusionFilter(${exclusionSet.size} exclusions: ${exclusionSet.take(3).joinToString(", ")}${if (exclusionSet.size > 3) ", ..." else ""})"
    }

    override fun isEnabled(): Boolean = enabled

    companion object {
        /**
         * Creates an empty exclusion filter (no Composables are excluded).
         */
        fun empty(): ExclusionComposableFilter {
            return ExclusionComposableFilter(emptySet())
        }

        /**
         * Creates a filter from a list of Composable names.
         */
        fun fromList(names: List<String>): ExclusionComposableFilter {
            return ExclusionComposableFilter(names.toSet())
        }

        /**
         * Creates a filter from a text format, one name per line.
         * Empty lines and lines starting with '#' are ignored.
         *
         * Example:
         * ```
         * val text = """
         *     # Exclude these Composables
         *     CounterSection
         *     DebugOverlay
         *     TempComposable
         * """.trimIndent()
         * val filter = ExclusionComposableFilter.fromText(text)
         * ```
         */
        fun fromText(text: String): ExclusionComposableFilter {
            val exclusions = text.split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith('#') }
                .toSet()
            return ExclusionComposableFilter(exclusions)
        }

        /**
         * Creates a filter from comma-separated names.
         * Example: "CounterSection,DebugOverlay,TempComposable"
         */
        fun fromString(namesStr: String, separator: String = ","): ExclusionComposableFilter {
            val names = namesStr.split(separator)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            return ExclusionComposableFilter(names)
        }

        /**
         * Creates a filter combining multiple exclusion sets.
         */
        fun combining(vararg filters: ExclusionComposableFilter): ExclusionComposableFilter {
            val allExclusions = filters.flatMap { it.exclusionSet }.toSet()
            return ExclusionComposableFilter(allExclusions)
        }
    }
}
