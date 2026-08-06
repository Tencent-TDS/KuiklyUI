/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.nvi.serialization.json

/**
 * OHOS (Kotlin/Native) 专用 tokener。解析语义与 commonMain 的 [JSONTokener] 完全一致
 * （含 org.json 历史怪癖：`=`/`=>` 分隔符、`;` 等价 `,`、`/* */` `//` `#` 注释、
 * 八进制/十六进制字面量、BOM 剥离、Int/Long/Double 类型选择规则），仅按 K/N 的
 * 分配成本重写了实现，不改变对外行为。
 *
 * 单独一份而非改 commonMain，是为了让优化只作用于 ohosArm64，iOS / JS 等共享
 * commonMain 实现的平台不受影响。
 */
class JSONTokenerOhos(json: String) {

    private var jsonStr: String = json

    private var pos = 0

    init {
        if (json.startsWith("\uFEFF")) {
            jsonStr = jsonStr.substring(1)
        }
    }

    @Throws(JSONException::class)
    fun nextValue(): Any? {
        val c = nextCleanInternal()
        return when (c) {
            EOF -> throw syntaxError("End of input")
            LBRACE -> readObject()
            LBRACKET -> readArray()
            QUOTE, APOSTROPHE -> nextString(c.toChar())
            else -> {
                pos--
                readLiteral()
            }
        }
    }

    /**
     * Skip whitespace / comments and return the next significant character's code.
     * Returns [EOF] (-1) at end of input. Int rather than Char so that a literal
     * U+FFFF in the input stays distinguishable from end-of-input, matching
     * [JSONTokener].
     */
    @Throws(JSONException::class)
    private fun nextCleanInternal(): Int {
        loop@ while (pos < jsonStr.length) {
            val c = jsonStr[pos++]
            when (c) {
                '\t', ' ', '\n', '\r' -> continue@loop
                '/' -> {
                    if (pos == jsonStr.length) {
                        return c.code
                    }
                    when (jsonStr[pos]) {
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
                        else -> return c.code
                    }
                }
                '#' -> {
                    /*
                     * Skip a # hash end-of-line comment. The JSON RFC doesn't
                     * specify this behavior, but it's required to parse
                     * existing documents. See http://b/2571423.
                     */
                    skipToEndOfLine()
                    continue@loop
                }
                else -> return c.code
            }
        }
        return EOF
    }

    private fun syntaxError(message: String): JSONException {
        return JSONException(message + this)
    }

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

    @Throws(JSONException::class)
    private fun readObject(): JSONObject {
        val result = JSONObject()

        /* Peek to see if this is the empty object. */
        val first = nextCleanInternal()
        if (first == RBRACE) {
            return result
        } else if (first != EOF) {
            pos--
        }
        while (true) {
            val name = nextValue()
            if (name !is String) {
                throw syntaxError(
                    "Names must be strings, but " + name
                            + " is of type "
                            + if (name === null) {
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
            if (separator != COLON && separator != EQUALS) {
                throw syntaxError("Expected ':' after $name")
            }
            if (pos < jsonStr.length && jsonStr[pos] == '>') {
                pos++
            }
            result.put(name, nextValue())
            when (nextCleanInternal()) {
                RBRACE -> return result
                SEMICOLON, COMMA -> { /* continue */ }
                else -> throw syntaxError("Unterminated object")
            }
        }
    }

    @Throws(JSONException::class)
    private fun readArray(): JSONArray {
        val result = JSONArray()

        while (true) {
            when (nextCleanInternal()) {
                EOF -> throw syntaxError("Unterminated array")
                RBRACKET -> return result
                COMMA, SEMICOLON -> continue
                else -> pos--
            }
            result.put(nextValue())
            when (nextCleanInternal()) {
                RBRACKET -> return result
                COMMA, SEMICOLON -> { /* continue */ }
                else -> throw syntaxError("Unterminated array")
            }
        }
    }

    @Throws(JSONException::class)
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
            if (c == quote) {
                return if (builder == null) {
                    // Kotlin/Native 的 substring 本身就 memcpy 出独立字符数组，
                    // 不与源串共享 backing array，无需再拼空串强制拷贝一次。
                    jsonStr.substring(start, pos - 1)
                } else {
                    builder.append(jsonStr, start, pos - 1)
                    builder.toString()
                }
            }
            if (c == '\\') {
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

    @Throws(JSONException::class)
    private fun readEscapeCharacter(): Char {
        val escaped = jsonStr[pos++]
        return when (escaped) {
            'u' -> {
                if (pos + 4 > jsonStr.length) {
                    throw syntaxError("Unterminated escape sequence")
                }
                var code = 0
                var digits = 0
                while (digits < 4) {
                    val digit = hexDigit(jsonStr[pos + digits])
                    if (digit < 0) {
                        break
                    }
                    code = (code shl 4) or digit
                    digits++
                }
                if (digits == 4) {
                    pos += 4
                    return code.toChar()
                }
                // 罕见路径：toInt(16) 还接受带符号的输入（如 "+12a"），
                // 退回原实现以保持行为与异常消息完全一致。
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

    private fun hexDigit(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> -1
    }

    /**
     * Parse an unquoted literal. Type-selection rules match org.json / prior
     * implementation (see design D6): hex/octal quirks, Int-vs-Long, fall
     * through to Double then String. Avoids intermediate String + exception
     * control flow on the integer-success path.
     */
    @Throws(JSONException::class)
    private fun readLiteral(): Any? {
        val start = pos
        while (pos < jsonStr.length) {
            val c = jsonStr[pos]
            if (c == '\r' || c == '\n') {
                break
            }
            val code = c.code
            if (code < 128 && LITERAL_STOP[code]) {
                break
            }
            pos++
        }
        val end = pos
        val len = end - start
        if (len == 0) {
            throw syntaxError("Expected literal value")
        }

        when {
            len == 4 && regionEquals(start, TRUE) -> return true
            len == 5 && regionEquals(start, FALSE) -> return false
            len == 4 && regionEquals(start, NULL_LIT) -> return null
        }

        var hasDot = false
        var i = start
        while (i < end) {
            if (jsonStr[i] == '.') {
                hasDot = true
                break
            }
            i++
        }

        if (!hasDot) {
            var numStart = start
            var base = 10
            if (len >= 2 && jsonStr[start] == '0' &&
                (jsonStr[start + 1] == 'x' || jsonStr[start + 1] == 'X')
            ) {
                numStart = start + 2
                base = 16
            } else if (len > 1 && jsonStr[start] == '0') {
                // leading 0 → octal (org.json quirk). "-010" does NOT match
                // (does not start with '0'), so it stays decimal -10.
                numStart = start + 1
                base = 8
            }
            val longValue = parseLongInRange(numStart, end, base)
            if (longValue != null) {
                return if (longValue <= Int.MAX_VALUE && longValue >= Int.MIN_VALUE) {
                    longValue.toInt()
                } else {
                    longValue
                }
            }
        }

        val literal = jsonStr.substring(start, end)
        try {
            return literal.toDouble()
        } catch (_: NumberFormatException) {
        }
        return literal
    }

    private fun regionEquals(start: Int, expected: String): Boolean {
        for (i in expected.indices) {
            if (jsonStr[start + i] != expected[i]) {
                return false
            }
        }
        return true
    }

    /**
     * Parse signed integer in [start, end) with the given base.
     * Returns null on empty input, invalid digit, or overflow (caller falls
     * through to Double / String), matching prior NumberFormatException paths.
     */
    private fun parseLongInRange(start: Int, end: Int, base: Int): Long? {
        if (start >= end) {
            return null
        }
        var i = start
        var negative = false
        val first = jsonStr[i]
        if (first == '-') {
            negative = true
            i++
        } else if (first == '+') {
            i++
        }
        if (i >= end) {
            return null
        }
        var value = 0L
        val limit = if (negative) Long.MIN_VALUE else -Long.MAX_VALUE
        val multmin = limit / base
        while (i < end) {
            val digit = digitValue(jsonStr[i], base)
            if (digit < 0) {
                return null
            }
            if (value < multmin) {
                return null
            }
            value *= base
            if (value < limit + digit) {
                return null
            }
            value -= digit
            i++
        }
        return if (negative) value else -value
    }

    private fun digitValue(c: Char, base: Int): Int {
        val d = when (c) {
            in '0'..'9' -> c - '0'
            in 'a'..'z' -> c - 'a' + 10
            in 'A'..'Z' -> c - 'A' + 10
            else -> return -1
        }
        return if (d < base) d else -1
    }

    companion object {
        /** Sentinel from [nextCleanInternal] for end-of-input, same as [JSONTokener]. */
        private const val EOF = -1

        private const val LBRACE = '{'.code
        private const val RBRACE = '}'.code
        private const val LBRACKET = '['.code
        private const val RBRACKET = ']'.code
        private const val QUOTE = '"'.code
        private const val APOSTROPHE = '\''.code
        private const val COLON = ':'.code
        private const val EQUALS = '='.code
        private const val COMMA = ','.code
        private const val SEMICOLON = ';'.code

        private const val TRUE = "true"
        private const val FALSE = "false"
        private const val NULL_LIT = "null"

        /**
         * Stop set for unquoted literals: `{}[]/\:,=;#` + space + tab.
         * `\r`/`\n` are checked separately (same as legacy nextToInternal).
         */
        private val LITERAL_STOP = BooleanArray(128).also { table ->
            for (ch in "{}[]/\\:,=;# \t") {
                table[ch.code] = true
            }
        }
    }
}
