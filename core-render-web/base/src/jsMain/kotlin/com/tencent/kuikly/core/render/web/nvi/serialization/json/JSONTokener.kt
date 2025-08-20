package com.tencent.kuikly.core.render.web.nvi.serialization.json

/**
 * JSONTokener handling class
 */
class JSONTokener(json: String) {
    private var jsonStr: String = json

    private var pos = 0

    init {
        if (json.startsWith("\uFEFF")) {
            jsonStr = jsonStr.substring(1)
        }
    }

    fun nextValue(): Any? {
        return when (val c: Int = nextCleanInternal()) {
            -1 -> throw syntaxError("End of input")
            '{'.code -> readObject()
            '['.code -> readArray()
            '\''.code, '"'.code -> nextString(c.toChar())
            else -> {
                pos--
                readLiteral()
            }
        }
    }

    private fun nextCleanInternal(): Int {
        loop@ while (pos < jsonStr.length) {
            return when (val c = jsonStr[pos++]) {
                '\t', ' ', '\n', '\r' -> continue@loop
                '/' -> {
                    if (pos == jsonStr.length) {
                        return c.code
                    }
                    val peek = jsonStr[pos]
                    when (peek) {
                        '*' -> {
                            // skip a /* c-style comment */
                            pos++
                            val commentEnd: Int = jsonStr.indexOf("*/", pos)
                            if (commentEnd == -1) {
                                throw syntaxError("Unterminated comment")
                            }
                            pos = commentEnd + 2
                            continue@loop
                        }

                        '/' -> {
                            // skip a // end-of-line comment
                            pos++
                            skipToEndOfLine()
                            continue@loop
                        }

                        else -> c.code
                    }
                }

                '#' -> {
                    /*
                             * Skip a # hash end-of-line comment. The JSON RFC doesn't
                             * specify this behavior, but it's required to parse
                             * existing documents. See http://b/2571423.
                             */skipToEndOfLine()
                    continue@loop
                }

                else -> c.code
            }
        }

        return -1
    }

    private fun syntaxError(message: String): JSONException = JSONException(message + this)

    private fun skipToEndOfLine() {
        while (pos < jsonStr.length) {
            val c: Char = jsonStr[pos]
            if (c == '\r' || c == '\n') {
                pos++
                break
            }
            pos++
        }
    }

    private fun readObject(): JSONObject {
        val result = JSONObject()

        /* Peek to see if this is the empty object. */
        val first = nextCleanInternal()
        if (first == '}'.code) {
            return result
        } else if (first != -1) {
            pos--
        }
        loop@ while (true) {
            val name = nextValue()
            if (name !is String) {
                throw syntaxError(
                    "Names must be strings, but " + name +
                            " is of type " +
                            if (name === null) {
                                "null"
                            } else {
                                name::class.simpleName
                            }
                )
            }

            /*
             * Expect the name/value separator to be either a colon ':', an
             * equals sign '=', or an arrow "=>". The last two are bogus but we
             * include them because that's what the original implementation did.
             */
            val separator = nextCleanInternal()
            if (separator != ':'.code && separator != '='.code) {
                throw syntaxError("Expected ':' after $name")
            }
            if (pos < jsonStr.length && jsonStr[pos] == '>') {
                pos++
            }
            result.put((name as String?)!!, nextValue())
            when (nextCleanInternal()) {
                '}'.code -> return result
                ';'.code, ','.code -> continue@loop
                else -> throw syntaxError("Unterminated object")
            }
        }
    }

    private fun readArray(): JSONArray {
        val result = JSONArray()
        /* to cover input that ends with ",]". */
        loop@ while (true) {
            when (nextCleanInternal()) {
                -1 -> throw syntaxError("Unterminated array")
                ']'.code -> {
                    return result
                }

                ','.code, ';'.code -> continue@loop
                else -> pos--
            }
            result.put(nextValue())
            return when (nextCleanInternal()) {
                ']'.code -> result
                ','.code, ';'.code -> continue@loop
                else -> throw syntaxError("Unterminated array")
            }
        }
    }

    fun nextString(quote: Char): String {
        /*
         * For strings that are free of escape sequences, we can just extract
         * the result as a substring of the input. But if we encounter an escape
         * sequence, we need to use a StringBuilder to compose the result.
         */
        var builder: StringBuilder? = null

        /* the index of the first character not yet appended to the builder. */
        var start = pos
        while (pos < jsonStr.length) {
            val c = jsonStr[pos++]
            if (c.code == quote.code) {
                return if (builder == null) {
                    jsonStr.substring(start, pos - 1) + "" // Ensure creating a string object to avoid memory leaks
                } else {
                    builder.append(jsonStr, start, pos - 1)
                    builder.toString()
                }
            }
            if (c.code == '\\'.code) {
                if (pos == jsonStr.length) {
                    throw syntaxError("Unterminated escape sequence")
                }
                if (builder == null) {
                    builder = StringBuilder()
                }
                builder.append(jsonStr, start, pos - 1)
                builder.append(readEscapeCharacter())
                start = pos
            }
        }
        throw syntaxError("Unterminated string")
    }

    private fun readEscapeCharacter(): Char {
        return when (val escaped = jsonStr[pos++]) {
            'u' -> {
                if (pos + 4 > jsonStr.length) {
                    throw syntaxError("Unterminated escape sequence")
                }
                val hex: String = jsonStr.substring(pos, pos + 4)
                pos += 4
                return try {
                    hex.toInt(16).toChar()
                } catch (nfe: NumberFormatException) {
                    throw syntaxError("Invalid escape sequence: $hex")
                }
            }

            't' -> '\t'
            'b' -> '\b'
            'n' -> '\n'
            'r' -> '\r'
            'f' -> '\u000C' // '\f'
            '\'', '"', '\\' -> escaped
            else -> escaped
        }
    }

    private fun readLiteral(): Any? {
        val literal: String = nextToInternal("{}[]/\\:,=;# \t")
        when {
            literal.isEmpty() -> {
                throw syntaxError("Expected literal value")
            }

            "true" == literal -> {
                return true
            }

            "false" == literal -> {
                return false
            }

            "null" == literal -> {
                return null
            }

            literal.indexOf('.') == -1 -> {
                var base = 10
                var number = literal
                if (number.startsWith("0x") || number.startsWith("0X")) {
                    number = number.substring(2)
                    base = 16
                } else if (number.startsWith("0") && number.length > 1) {
                    number = number.substring(1)
                    base = 8
                }
                try {
                    val longValue = number.toLong(base)
                    return if (longValue <= Int.MAX_VALUE && longValue >= Int.MIN_VALUE) {
                        longValue.toInt()
                    } else {
                        longValue
                    }
                } catch (e: NumberFormatException) {
                    /*
                     * This only happens for integral numbers greater than
                     * Long.MAX_VALUE, numbers in exponential form (5e-10) and
                     * unquoted strings. Fall through to try floating point.
                     */
                }
            }
        }

        try {
            return literal.toDouble()
        } catch (ignored: NumberFormatException) {
        }
        // a new string avoids leaking memory
        return literal + ""
    }

    private fun nextToInternal(excluded: String): String {
        val start = pos
        while (pos < jsonStr.length) {
            val c: Char = jsonStr[pos]
            if (c == '\r' || c == '\n' || excluded.indexOf(c) != -1) {
                return jsonStr.substring(start, pos)
            }
            pos++
        }
        return jsonStr.substring(start)
    }
}
