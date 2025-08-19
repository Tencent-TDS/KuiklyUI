package com.tencent.kuikly.core.render.web.nvi.serialization.json

/**
 * JSONStringer handling class
 */
class JSONStringer {
    private val stack = arrayListOf<Scope>()
    private val out = StringBuilder()

    enum class Scope {
        EMPTY_ARRAY,
        NONEMPTY_ARRAY,
        EMPTY_OBJECT,
        DANGLING_KEY,
        NONEMPTY_OBJECT,
        NULL_OBJ
    }

    fun startObject(): JSONStringer = open(Scope.EMPTY_OBJECT, "{")

    fun endObject(): JSONStringer = close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT, "}")

    fun startArray(): JSONStringer = open(Scope.EMPTY_ARRAY, "[")

    fun endArray(): JSONStringer = close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY, "]")

    fun key(name: String): JSONStringer {
        beforeKey()
        string(name)
        return this
    }

    fun value(value: Any?): JSONStringer {
        if (stack.isEmpty()) {
            throw JSONException("Nesting problem")
        }
        if (value is JSONArray) {
            value.writeTo(this)
            return this
        } else if (value is JSONObject) {
            value.writeTo(this)
            return this
        }
        beforeValue()
        when (value) {
            is Boolean -> {
                out.append(value)
            }

            is Number -> {
                out.append(JSON.numberToString(value))
            }

            else -> {
                if (value == null) {
                    out.append("null")
                } else {
                    string(value.toString())
                }
            }
        }
        return this
    }

    fun open(empty: Scope, openBracket: String): JSONStringer {
        if (stack.isEmpty() && out.isNotEmpty()) {
            throw JSONException("Nesting problem: multiple top-level roots")
        }
        beforeValue()
        stack.add(empty)
        out.append(openBracket)
        return this
    }

    private fun beforeValue() {
        if (stack.isEmpty()) {
            return
        }
        val context = peek()
        when {
            context == Scope.EMPTY_ARRAY -> { // first in array
                replaceTop(Scope.NONEMPTY_ARRAY)
                newline()
            }

            context == Scope.NONEMPTY_ARRAY -> { // another in array
                out.append(',')
                newline()
            }

            context == Scope.DANGLING_KEY -> { // value for key
                out.append(": ")
                replaceTop(Scope.NONEMPTY_OBJECT)
            }

            context != Scope.NULL_OBJ -> {
                throw JSONException("Nesting problem")
            }
        }
    }

    private fun peek(): Scope {
        if (stack.isEmpty()) {
            throw JSONException("Nesting problem")
        }
        return stack[stack.size - 1]
    }

    private fun replaceTop(topOfStack: Scope) {
        stack[stack.size - 1] = topOfStack
    }

    private fun newline() {
    }

    private fun beforeKey() {
        val context = peek()
        if (context == Scope.NONEMPTY_OBJECT) { // first in object
            out.append(',')
        } else if (context != Scope.EMPTY_OBJECT) { // not in an object!
            throw JSONException("Nesting problem")
        }
        newline()
        replaceTop(Scope.DANGLING_KEY)
    }

    private fun string(value: String) {
        out.append("\"")
        var i = 0
        val length = value.length
        while (i < length) {
            when (val c = value[i]) {
                '"', '\\', '/' -> out.append('\\').append(c)
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                else -> if (c.code <= 0x1F) {
                    out.append("\\u${c.code.toString(16).padStart(4, '0')}")
                } else {
                    out.append(c)
                }
            }
            i++
        }
        out.append("\"")
    }

    fun close(
        empty: Scope,
        nonempty: Scope,
        closeBracket: String
    ): JSONStringer {
        val context = peek()
        if (context != nonempty && context != empty) {
            throw JSONException("Nesting problem")
        }
        stack.removeAt(stack.size - 1)
        if (context == nonempty) {
            newline()
        }
        out.append(closeBracket)
        return this
    }

    override fun toString(): String {
        return if (out.isEmpty()) {
            "{}"
        } else {
            out.toString()
        }
    }
}
