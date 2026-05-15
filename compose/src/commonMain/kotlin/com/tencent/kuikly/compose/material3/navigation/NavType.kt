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
 * NavType denotes the type that can be used in a [NavArgument].
 *
 * Each type has the ability to parse a value from a string (for route-based navigation)
 * and to put/get values from a [Map].
 *
 * Built-in types include [IntType], [LongType], [FloatType], [BoolType], and [StringType].
 *
 * @param T the type of the data that is supported by this NavType
 * @param isNullableAllowed whether an argument with this type can hold a null value
 *
 * @see NamedNavArgument
 * @see NavGraphBuilder.composable
 */
abstract class NavType<T>(
    /**
     * Check if an argument with this type can hold a null value.
     */
    val isNullableAllowed: Boolean
) {
    /**
     * The name of this type.
     */
    abstract val name: String

    /**
     * Parse a value of this type from a String.
     *
     * @param value string representation of a value of this type
     * @return parsed value of the type represented by this NavType
     * @throws IllegalArgumentException if value cannot be parsed into this type
     */
    abstract fun parseValue(value: String): T

    /**
     * The default value of this type.
     */
    abstract val defaultValue: T

    /**
     * Serialize a value of this type to a String.
     *
     * @param value a value of this type
     * @return string representation of the value
     */
    open fun serializeAsValue(value: T): String = value.toString()

    override fun toString(): String = name

    companion object {
        /**
         * NavType for storing integer values, corresponding to the "integer" type in
         * a Navigation XML file.
         *
         * Null values are not supported.
         */
        val IntType: NavType<Int> = object : NavType<Int>(false) {
            override val name: String get() = "integer"
            override val defaultValue: Int get() = 0
            override fun parseValue(value: String): Int {
                return if (value.startsWith("0x")) {
                    value.substring(2).toInt(16)
                } else {
                    value.toInt()
                }
            }
        }

        /**
         * NavType for storing long values.
         *
         * Null values are not supported.
         * Default value is 0L.
         */
        val LongType: NavType<Long> = object : NavType<Long>(false) {
            override val name: String get() = "long"
            override val defaultValue: Long get() = 0L
            override fun parseValue(value: String): Long {
                // Handle values with trailing "L" suffix
                return value.removeSuffix("L").toLong()
            }
        }

        /**
         * NavType for storing float values.
         *
         * Null values are not supported.
         * Default value is 0.0f.
         */
        val FloatType: NavType<Float> = object : NavType<Float>(false) {
            override val name: String get() = "float"
            override val defaultValue: Float get() = 0f
            override fun parseValue(value: String): Float = value.toFloat()
        }

        /**
         * NavType for storing boolean values, corresponding to the "boolean" type in
         * a Navigation XML file.
         *
         * Null values are not supported.
         * Default value is false.
         */
        val BoolType: NavType<Boolean> = object : NavType<Boolean>(false) {
            override val name: String get() = "boolean"
            override val defaultValue: Boolean get() = false
            override fun parseValue(value: String): Boolean {
                return when (value) {
                    "true" -> true
                    "false" -> false
                    else -> throw IllegalArgumentException("A boolean NavType only accepts \"true\" or \"false\" values.")
                }
            }
        }

        /**
         * NavType for storing String values.
         *
         * Null values are supported. Default value is null.
         */
        val StringType: NavType<String?> = object : NavType<String?>(true) {
            override val name: String get() = "string"
            override val defaultValue: String? get() = null
            override fun parseValue(value: String): String = value
            override fun serializeAsValue(value: String?): String = value ?: ""
        }
    }
}

/**
 * Represents a named navigation argument. It associates a [name] with a [NavArgument].
 *
 * Create instances using the [navArgument] DSL function.
 *
 * @see navArgument
 * @see NavGraphBuilder.composable
 */
class NamedNavArgument @PublishedApi internal constructor(
    /**
     * The name of this argument.
     */
    val name: String,
    /**
     * The [NavArgument] associated with this named argument.
     */
    val argument: NavArgument
)

/**
 * A navigation argument that is associated with a [NavDestination].
 *
 * A NavArgument knows its type ([NavType]), whether it is nullable, and its default value.
 *
 * @see NavType
 * @see NamedNavArgument
 */
class NavArgument internal constructor(
    /**
     * The type of this argument.
     */
    val type: NavType<*>,
    /**
     * Whether this argument is nullable.
     */
    val isNullable: Boolean,
    /**
     * The default value of this argument, or null if there is no default.
     */
    val defaultValue: Any?,
    /**
     * Whether this argument has a default value.
     */
    val isDefaultValuePresent: Boolean
) {
    /**
     * Builder for constructing [NavArgument] instances.
     */
    class Builder {
        private var type: NavType<*> = NavType.StringType
        private var isNullable = false
        private var defaultValue: Any? = null
        private var defaultValuePresent = false

        /**
         * Set the type of the argument.
         *
         * @param type the [NavType] for this argument
         * @return this [Builder]
         */
        fun setType(type: NavType<*>): Builder {
            this.type = type
            return this
        }

        /**
         * Set whether the argument is nullable.
         *
         * @param isNullable true if the argument can hold a null value
         * @return this [Builder]
         */
        fun setIsNullable(isNullable: Boolean): Builder {
            this.isNullable = isNullable
            return this
        }

        /**
         * Set the default value for this argument.
         *
         * @param defaultValue the default value
         * @return this [Builder]
         */
        fun setDefaultValue(defaultValue: Any?): Builder {
            this.defaultValue = defaultValue
            this.defaultValuePresent = true
            return this
        }

        /**
         * Build the [NavArgument].
         */
        fun build(): NavArgument {
            return NavArgument(
                type = type,
                isNullable = isNullable,
                defaultValue = defaultValue,
                isDefaultValuePresent = defaultValuePresent
            )
        }
    }
}

/**
 * DSL for constructing a new [NamedNavArgument].
 *
 * Usage:
 * ```kotlin
 * composable(
 *     "profile/{userId}",
 *     arguments = listOf(
 *         navArgument("userId") {
 *             type = NavType.IntType
 *             defaultValue = 0
 *         }
 *     )
 * ) { backStackEntry ->
 *     val userId = backStackEntry.arguments?.getInt("userId") ?: 0
 *     ProfileScreen(userId)
 * }
 * ```
 *
 * @param name the name of the argument
 * @param builder a lambda that configures the [NavArgumentBuilder]
 * @return a [NamedNavArgument] for use in [NavGraphBuilder.composable]
 */
inline fun navArgument(
    name: String,
    builder: NavArgumentBuilder.() -> Unit
): NamedNavArgument {
    val navArgBuilder = NavArgumentBuilder().apply(builder)
    return NamedNavArgument(name, navArgBuilder.build())
}

/**
 * DSL builder for creating [NavArgument] instances.
 *
 * @see navArgument
 */
class NavArgumentBuilder {
    /**
     * The NavType for this argument.
     */
    var type: NavType<*> = NavType.StringType

    /**
     * Whether the argument is nullable.
     */
    var nullable: Boolean = false

    /**
     * The default value for this argument.
     */
    var defaultValue: Any? = null

    private var defaultValuePresent = false

    @PublishedApi internal fun build(): NavArgument {
        // Auto-detect default value presence if set to non-null
        if (defaultValue != null) {
            defaultValuePresent = true
        }
        return NavArgument(
            type = type,
            isNullable = nullable,
            defaultValue = defaultValue,
            isDefaultValuePresent = defaultValuePresent
        )
    }
}
