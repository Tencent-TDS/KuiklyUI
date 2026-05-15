package com.tencent.kuikly.compose.profiler.filter

/**
 * Filter that excludes Composables whose info string starts with any of the specified prefixes.
 *
 * This is useful for filtering out framework or third-party library Composables.
 *
 * Example:
 * ```
 * val frameworkFilter = PrefixComposableFilter(
 *     prefixes = listOf(
 *         "androidx.compose.",
 *         "com.tencent.kuikly.compose."
 *     )
 * )
 * ```
 */
class PrefixComposableFilter(
    private val prefixes: List<String>,
    private val enabled: Boolean = true
) : ComposableFilter {
    init {
        require(prefixes.isNotEmpty()) { "At least one prefix must be specified" }
    }

    override fun shouldFilter(composableName: String, info: String): Boolean {
        if (!enabled) return false
        // 同时检查两个字段：
        // - info 用于框架内置前缀（如 "androidx.compose."），编译器注入时 info 带全限定名
        // - composableName 用于业务自定义前缀（info 通常只含短名如 "CounterSection"）
        return prefixes.any { prefix -> info.startsWith(prefix) || composableName.startsWith(prefix) }
    }

    override fun description(): String {
        return "PrefixFilter(${prefixes.size} prefixes: ${prefixes.take(2).joinToString(", ")}${if (prefixes.size > 2) ", ..." else ""})"
    }

    override fun isEnabled(): Boolean = enabled

    companion object {
        /**
         * Creates a filter for standard Jetpack Compose framework Composables.
         */
        fun jetpackComposeFramework(): PrefixComposableFilter {
            return PrefixComposableFilter(
                prefixes = listOf(
                    "androidx.compose.",
                    "androidx.activity.compose.",
                    "androidx.navigation.compose.",
                    "androidx.lifecycle.compose.",
                    "androidx.tv.compose.",
                    "androidx.paging.compose."
                )
            )
        }

        /**
         * Creates a filter for KuiklyUI framework Composables.
         */
        fun kuiklyuiFramework(): PrefixComposableFilter {
            return PrefixComposableFilter(
                prefixes = listOf(
                    "com.tencent.kuikly.compose.ui.",
                    "com.tencent.kuikly.compose.foundation.",
                    "com.tencent.kuikly.compose.layout.",
                    "com.tencent.kuikly.compose.material."
                )
            )
        }

        /**
         * Creates a filter for common third-party Compose libraries.
         */
        fun thirdPartyLibraries(): PrefixComposableFilter {
            return PrefixComposableFilter(
                prefixes = listOf(
                    "com.google.accompanist.",
                    "androidx.compose.material3.",
                    "androidx.constraintlayout.compose.",
                    "io.coil."
                )
            )
        }

        /**
         * Creates a filter combining all standard framework prefixes.
         */
        fun allFrameworks(): PrefixComposableFilter {
            return PrefixComposableFilter(
                prefixes = jetpackComposeFramework().prefixes +
                        kuiklyuiFramework().prefixes +
                        thirdPartyLibraries().prefixes
            )
        }

        /**
         * Creates a filter from a comma-separated string of prefixes.
         * Example: "androidx.compose.,com.tencent.kuikly."
         */
        fun fromString(prefixesStr: String, separator: String = ","): PrefixComposableFilter {
            val prefixes = prefixesStr.split(separator)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return PrefixComposableFilter(prefixes)
        }
    }
}
