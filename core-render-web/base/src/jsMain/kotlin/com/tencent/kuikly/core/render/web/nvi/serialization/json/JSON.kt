package com.tencent.kuikly.core.render.web.nvi.serialization.json

/**
 * Custom JSON type
 */
object JSON {
    var useNativeMethod: Boolean = false

    fun toBoolean(value: Any?): Boolean? {
        if (value is Boolean) {
            return value
        } else if (value is String) {
            if ("true".equals(value, ignoreCase = true)) {
                return true
            } else if ("false".equals(value, ignoreCase = true)) {
                return false
            }
        } else if (value is Number) {
            return value.toInt() != 0
        }
        return null
    }

    fun toDouble(value: Any?): Double? {
        when (value) {
            is Double -> {
                return value
            }

            is Number -> {
                return value.toDouble()
            }

            is String -> {
                try {
                    return value.toDouble()
                } catch (ignored: NumberFormatException) {
                    // TODO: LOG
                }
            }
        }
        return null
    }

    fun toInteger(value: Any?): Int? {
        when (value) {
            is Int -> {
                return value
            }

            is Number -> {
                return value.toInt()
            }

            is String -> {
                try {
                    return value.toInt()
                } catch (ignored: NumberFormatException) {
                    // TODO LOG
                }
            }
        }
        return null
    }

    fun toLong(value: Any?): Long? {
        when (value) {
            is Long -> {
                return value
            }

            is Number -> {
                return value.toLong()
            }

            is String -> {
                try {
                    return value.toLong()
                } catch (ignored: NumberFormatException) {
                    // TODO: LOG
                }
            }
        }
        return null
    }

    fun toString(value: Any?): String? {
        if (value is String) {
            return value
        } else if (value != null) {
            return value.toString()
        }
        return null
    }

    fun numberToString(number: Number): String {
        val doubleValue = number.toDouble()
        val longValue = number.toLong()
        return if (doubleValue == longValue.toDouble()) {
            longValue.toString()
        } else {
            number.toString()
        }
    }

    fun typeMismatch(actual: Any?, requiredType: String): JSONException {
        if (actual == null) {
            throw JSONException("Value is null.")
        } else {
            throw JSONException(
                "Value " + actual +
                        " of type " + actual::class.simpleName +
                        " cannot be converted to " + requiredType
            )
        }
    }
}
