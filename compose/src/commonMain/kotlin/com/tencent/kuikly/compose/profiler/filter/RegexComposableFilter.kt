package com.tencent.kuikly.compose.profiler.filter

/**
 * Filter that excludes Composables whose info string matches any of the specified regex patterns.
 *
 * This is useful for complex filtering logic that can't be expressed with simple prefixes.
 *
 * Example:
 * ```
 * val frameworkFilter = RegexComposableFilter(
 *     patterns = listOf(
 *         Regex("^androidx\\.compose\\..*"),
 *         Regex(".*\\.internal\\..*")
 *     )
 * )
 * ```
 */
class RegexComposableFilter(
    private val patterns: List<Regex>,
    private val enabled: Boolean = true
) : ComposableFilter {
    init {
        require(patterns.isNotEmpty()) { "At least one regex pattern must be specified" }
    }

    override fun shouldFilter(composableName: String, info: String): Boolean {
        if (!enabled) return false
        return patterns.any { pattern -> pattern.containsMatchIn(info) }
    }

    override fun description(): String {
        return "RegexFilter(${patterns.size} patterns: ${patterns.take(2).map { it.pattern }.joinToString(", ")}${if (patterns.size > 2) ", ..." else ""})"
    }

    override fun isEnabled(): Boolean = enabled

    companion object {
        /**
         * Creates a filter for internal/private Composables (containing ".internal." or "._" patterns).
         */
        fun internalComposables(): RegexComposableFilter {
            return RegexComposableFilter(
                patterns = listOf(
                    Regex(".*\\.internal\\..*"),
                    Regex(".*\\._.*")
                )
            )
        }

        /**
         * Creates a filter for generated/synthetic Composables.
         */
        fun generatedComposables(): RegexComposableFilter {
            return RegexComposableFilter(
                patterns = listOf(
                    Regex(".*\\$\\$.*"),     // Synthetic classes
                    Regex(".*Generated.*"),  // Generated classes
                    Regex(".*Composer.*")    // Compiler-generated Composer functions
                )
            )
        }

        /**
         * Creates a filter for debug/test Composables.
         */
        fun debugComposables(): RegexComposableFilter {
            return RegexComposableFilter(
                patterns = listOf(
                    Regex(".*Debug.*"),
                    Regex(".*Test.*"),
                    Regex(".*Preview.*")
                )
            )
        }

        /**
         * Creates a filter from a list of regex pattern strings.
         * Invalid patterns will throw IllegalArgumentException during shouldFilter() call.
         */
        fun fromStrings(patternStrings: List<String>): RegexComposableFilter {
            val patterns = patternStrings.map { Regex(it) }
            return RegexComposableFilter(patterns)
        }

        /**
         * Combines multiple predefined filters.
         */
        fun combining(vararg filters: RegexComposableFilter): RegexComposableFilter {
            val allPatterns = filters.flatMap { it.patterns }
            return RegexComposableFilter(allPatterns)
        }
    }
}
