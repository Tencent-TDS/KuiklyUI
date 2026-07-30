package com.tencent.kuikly.core.nvi.serialization.json

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

    @Throws(JSONException::class)
    fun startObject(): JSONStringer {
        return open(Scope.EMPTY_OBJECT, "{")
    }

    @Throws(JSONException::class)
    fun endObject(): JSONStringer? {
        return close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT, "}")
    }

    @Throws(JSONException::class)
    fun startArray(): JSONStringer {
        return open(Scope.EMPTY_ARRAY, "[")
    }

    @Throws(JSONException::class)
    fun endArray(): JSONStringer {
        return close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY, "]")
    }

    @Throws(JSONException::class)
    fun key(name: String): JSONStringer {
        beforeKey()
        string(name)
        return this
    }

    @Throws(JSONException::class)
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

    @Throws(JSONException::class)
    fun open(empty: Scope, openBracket: String): JSONStringer {
        if (stack.isEmpty() && out.isNotEmpty()) {
            throw JSONException("Nesting problem: multiple top-level roots")
        }
        beforeValue()
        stack.add(empty)
        out.append(openBracket)
        return this
    }

    @Throws(JSONException::class)
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

    @Throws(JSONException::class)
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

    @Throws(JSONException::class)
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
            val c = value[i]
            when (c) {
                '"', '\\', '/' -> out.append('\\').append(c)
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                else -> if (c.toInt() <= 0x1F) {
                    out.append("\\u${c.toInt().toString(16).padStart(4, '0')}")
                } else {
                    out.append(c)
                }
            }
            i++
        }
        out.append("\"")
    }

    @Throws(JSONException::class)
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