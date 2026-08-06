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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONEngine
import com.tencent.kuikly.core.nvi.serialization.json.JSONException
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 一份待测的解析实现。[name] 同时用作 [selectTokener] 的标识。
 */
class TokenerVariant(val name: String, val parse: (String) -> Any?)

/**
 * 本平台可用的解析实现，index 0 为默认实现。
 * ohosArm64 返回 legacy(`JSONTokener`) + 优化版(`JSONTokenerOhos`) 两份，其余平台只有一份。
 */
expect fun tokenerVariants(): List<TokenerVariant>

/** 当前 `JSONEngine.parse` 实际使用的实现名。 */
expect fun currentTokenerName(): String

/** 把 `JSONEngine.parse` 切到指定实现；名字未知或平台只有一份实现时为空操作。 */
expect fun selectTokener(name: String)

/**
 * 解析 + 序列化 + JSONObject/JSONArray 增删改查的一致性用例集。
 *
 * 对 [tokenerVariants] 里的每一份实现跑同一套用例，做两层校验：
 * 1. **绝对校验**：与手写期望值逐项比对（值 + 运行时类型，`Int` / `Long` / `Double` 不可混淆），
 *    异常用例比对异常消息。单实现平台（iOS / Android / JS）也能靠这层发现问题。
 * 2. **实现间差分**：每份实现产出一条等长的观测序列（label = 指纹），逐行比对。
 *    只要某个用例上两份实现行为不同就会被抓住，哪怕我们没为它写期望值。
 *
 * 用例覆盖 `JSONTokener` 从 org.json 继承的全部历史怪癖：`=` / `=>` 作分隔符、`;` 等价 `,`、
 * `/* */` `//` `#` 注释、单引号与无引号字面量、十六进制 / 八进制数、Int-vs-Long-vs-Double
 * 选型、BOM 剥离、尾部垃圾忽略等。
 */
internal object JsonConformance {

    fun run(): ConformanceReport {
        val variants = tokenerVariants()
        if (variants.isEmpty()) {
            return ConformanceReport(emptyList(), 0, listOf("没有可用的解析实现"), emptyList())
        }
        val savedTokener = currentTokenerName()
        val failures = mutableListOf<String>()
        val summary = mutableListOf<String>()
        val traces = mutableListOf<List<String>>()
        var checks = 0

        try {
            for (variant in variants) {
                // 切换 JSONEngine，让 JSONObject(String) / optJSONObject(String) 这类
                // 经由引擎的间接路径也走当前被测实现。
                selectTokener(variant.name)
                val r = Recorder(variant.name)
                r.checkText("engine/active tokener", currentTokenerName(), variant.name, record = false)
                runParseSection(variant, r)
                runSerializeSection(variant, r)
                runObjectCrudSection(variant, r)
                runArrayCrudSection(variant, r)
                runEngineSection(variant, r)
                checks += r.checks
                failures.addAll(r.failures)
                traces.add(r.trace)
                summary.add(
                    "  ${variant.name}: ${r.checks} checks, ${r.failures.size} failed" +
                            ", ${r.trace.size} observations"
                )
            }
        } finally {
            selectTokener(savedTokener)
        }

        failures.addAll(diffTraces(variants, traces, summary))
        return ConformanceReport(variants.map { it.name }, checks, failures, summary)
    }

    /** 逐行比对各实现的观测序列。 */
    private fun diffTraces(
        variants: List<TokenerVariant>,
        traces: List<List<String>>,
        summary: MutableList<String>
    ): List<String> {
        val failures = mutableListOf<String>()
        if (traces.size < 2) {
            summary.add("  diff: 本平台只有 1 份实现，跳过差分")
            return failures
        }
        val base = traces[0]
        val baseName = variants[0].name
        for (i in 1 until traces.size) {
            val other = traces[i]
            val otherName = variants[i].name
            if (base.size != other.size) {
                failures.add("diff[$baseName vs $otherName]: 观测行数不同 ${base.size} vs ${other.size}")
            }
            val shared = if (base.size < other.size) base.size else other.size
            var diffCount = 0
            for (j in 0 until shared) {
                if (base[j] != other[j]) {
                    diffCount++
                    if (diffCount <= MAX_REPORTED_DIFFS) {
                        failures.add("diff[$baseName vs $otherName]: ${base[j]}  <>  ${other[j]}")
                    }
                }
            }
            if (diffCount > MAX_REPORTED_DIFFS) {
                failures.add("diff[$baseName vs $otherName]: 另有 ${diffCount - MAX_REPORTED_DIFFS} 行不一致未展示")
            }
            summary.add("  diff[$baseName vs $otherName]: $diffCount / $shared 行不一致")
        }
        return failures
    }

    // ============================================================
    // Section 1: 解析
    // ============================================================

    private fun runParseSection(variant: TokenerVariant, r: Recorder) {
        for (case in corpus()) {
            val label = "parse/${case.name}"
            try {
                val value = variant.parse(case.json)
                if (case.expectedError != null) {
                    r.record(label, fingerprint(value))
                    r.fail("$label: 期望抛错 \"${case.expectedError}\"，实际解析出 ${fingerprint(value)}")
                } else {
                    r.check(label, value, case.expected)
                }
            } catch (e: JSONException) {
                val msg = normalizeError(e.message)
                r.record(label, "ERR($msg)")
                r.checks++
                if (case.expectedError == null) {
                    r.fail("$label: 非预期异常 \"$msg\"")
                } else if (msg != case.expectedError) {
                    r.fail("$label: 期望异常 \"${case.expectedError}\"，实际 \"$msg\"")
                }
            } catch (e: Exception) {
                // JSONException 之外的异常一律视为缺陷（例如越界），两份实现同时抛也要报出来。
                val msg = "${e::class.simpleName}: ${normalizeError(e.message)}"
                r.record(label, "EXC($msg)")
                r.checks++
                r.fail("$label: 抛出了非 JSONException 的异常 $msg")
            }
        }
    }

    // ============================================================
    // Section 2: 序列化
    // ============================================================

    private fun runSerializeSection(variant: TokenerVariant, r: Recorder) {
        for (case in corpus()) {
            if (case.expectedError != null) {
                continue
            }
            val tree = try {
                variant.parse(case.json)
            } catch (e: Exception) {
                continue
            }
            if (tree !is JSONObject && tree !is JSONArray) {
                continue // 标量根无法交给 JSONEngine.stringify
            }
            val label = "serialize/${case.name}"
            val text = stringifyTree(tree)
            r.record("$label text", escape(text))

            // 幂等性：序列化 → 解析 → 再序列化，文本必须完全一致。
            val reparsed = try {
                variant.parse(text)
            } catch (e: Exception) {
                r.fail("$label: 序列化结果无法回解 \"${escape(text)}\"：${normalizeError(e.message)}")
                continue
            }
            r.checkText("$label roundtrip", stringifyTree(reparsed), text)

            // 值层面：数字会被 JSON.numberToString 归一（Double 1.0 序列化成 "1"，回解成 Int），
            // 故用把所有数字折叠成 Double 的宽松指纹比对，只校验结构与数值。
            r.checkText("$label value", looseFingerprint(reparsed), looseFingerprint(tree))
        }

        if (!usesCommonSerialization()) {
            r.record("serialize/exact-text", "skipped(platform engine)")
            return
        }
        r.record("serialize/exact-text", "checked")
        r.checkText(
            "serialize/exact flat object",
            jsonObj("i" to 1, "s" to "x", "b" to true, "n" to null).toString(),
            "{\"i\": 1,\"s\": \"x\",\"b\": true,\"n\": null}"
        )
        r.checkText("serialize/exact empty object", JSONObject().toString(), "{}")
        r.checkText("serialize/exact empty array", JSONArray().toString(), "[]")
        r.checkText("serialize/exact array", jsonArr(1, "a", false, null).toString(), "[1,\"a\",false,null]")
        r.checkText(
            "serialize/exact nested",
            jsonObj("a" to jsonArr(jsonObj("b" to 2))).toString(),
            "{\"a\": [{\"b\": 2}]}"
        )
        // Double 整值被写成整数是 JSON.numberToString 的既有行为，锁死避免回归。
        r.checkText("serialize/exact whole double", jsonObj("d" to 1.0).toString(), "{\"d\": 1}")
        r.checkText("serialize/exact fractional", jsonObj("d" to 1.25).toString(), "{\"d\": 1.25}")
        r.checkText("serialize/exact long", jsonObj("l" to 2147483648L).toString(), "{\"l\": 2147483648}")
        // JSONStringer 会额外转义 '/'，控制字符走 \\u00xx。
        r.checkText(
            "serialize/exact escapes",
            jsonObj("s" to "a\"b\\c/d\te\nf\rg\u0001h").toString(),
            "{\"s\": \"a\\\"b\\\\c\\/d\\te\\nf\\rg\\u0001h\"}"
        )
        r.checkText("serialize/quote helper", JSONObject.quote("a\"b/c\n"), "\"a\\\"b\\/c\\n\"")
        r.checkText("serialize/quote null", JSONObject.quote(null), "\"\"")
    }

    // ============================================================
    // Section 3: JSONObject 增删改查
    // ============================================================

    private fun runObjectCrudSection(variant: TokenerVariant, r: Recorder) {
        val root = variant.parse(CRUD_JSON) as? JSONObject
        if (root == null) {
            r.fail("objCrud: CRUD_JSON 未解析成 JSONObject")
            return
        }

        // ---- Read ----
        r.check("objCrud/length", root.length(), 7)
        r.check("objCrud/has existing", root.has("user"), true)
        r.check("objCrud/has missing", root.has("missing"), false)
        r.checkText("objCrud/keys", root.keySet().sorted().joinToString(","), "arrStr,big,items,objStr,strBool,strNum,user")
        r.checkText("objCrud/keys iterator", root.keys().asSequence().toList().sorted().joinToString(","), "arrStr,big,items,objStr,strBool,strNum,user")

        val user = root.optJSONObject("user")
        if (user == null) {
            r.fail("objCrud: user 子对象缺失")
            return
        }
        r.check("objCrud/optInt", user.optInt("id"), 42)
        r.check("objCrud/optLong from Int", user.optLong("id"), 42L)
        r.check("objCrud/optDouble from Int", user.optDouble("id"), 42.0)
        r.check("objCrud/optString from Int", user.optString("id"), "42")
        r.check("objCrud/optString", user.optString("name"), "Alice")
        r.check("objCrud/optInt from non-numeric String", user.optInt("name", -1), -1)
        r.check("objCrud/optDouble from non-numeric String", user.optDouble("name", -1.0), -1.0)
        r.check("objCrud/optBoolean from String", user.optBoolean("name"), false)
        r.check("objCrud/optBoolean", user.optBoolean("active"), true)
        r.check("objCrud/optInt from Boolean", user.optInt("active", -1), -1)
        r.check("objCrud/optInt truncates Double", user.optInt("score"), 9)
        r.check("objCrud/optLong truncates Double", user.optLong("score"), 9L)
        r.check("objCrud/optDouble", user.optDouble("score"), 9.5)
        r.check("objCrud/optString from Double", user.optString("score"), "9.5")
        // null 值：key 存在、opt 为 null、optString 走 fallback
        r.check("objCrud/has null value", user.has("nil"), true)
        r.check("objCrud/opt null value", user.opt("nil"), null)
        r.check("objCrud/optString null default", user.optString("nil"), "")
        r.check("objCrud/optString null fallback", user.optString("nil", "fb"), "fb")
        r.check("objCrud/optInt missing", root.optInt("missing", -7), -7)
        r.check("objCrud/optBoolean missing", root.optBoolean("missing"), false)
        // Long 读成 Int 会按 Number.toInt() 截断
        r.check("objCrud/optLong", root.optLong("big"), 2147483648L)
        r.check("objCrud/optInt overflow truncates", root.optInt("big"), -2147483648)
        // 字符串到数字/布尔的强制转换
        r.check("objCrud/optInt from numeric String", root.optInt("strNum"), 42)
        r.check("objCrud/optLong from numeric String", root.optLong("strNum"), 42L)
        r.check("objCrud/optDouble from numeric String", root.optDouble("strNum"), 42.0)
        r.check("objCrud/optBoolean from String ignore case", root.optBoolean("strBool"), true)
        // 字符串字段被 optJSONObject / optJSONArray 当作 JSON 文本二次解析
        r.check("objCrud/optJSONObject from String", root.optJSONObject("objStr")?.optInt("x"), 1)
        r.check("objCrud/optJSONArray from String", root.optJSONArray("arrStr")?.length(), 2)
        r.check("objCrud/optJSONObject on array", root.optJSONObject("items"), null)
        r.check("objCrud/optJSONArray", root.optJSONArray("items")?.length(), 3)
        r.check("objCrud/optJSONObject missing", root.optJSONObject("missing"), null)
        r.check("objCrud/optJSONArray missing", root.optJSONArray("missing"), null)

        // ---- Create ----
        r.check("objCrud/put Int", root.put("cInt", 7).optInt("cInt"), 7)
        r.check("objCrud/put Long keeps type", root.put("cLong", 7L).opt("cLong"), 7L)
        r.check("objCrud/put Double keeps type", root.put("cDouble", 7.5).opt("cDouble"), 7.5)
        r.check("objCrud/put Boolean", root.put("cBool", true).opt("cBool"), true)
        r.check("objCrud/put String", root.put("cStr", "s").opt("cStr"), "s")
        r.check("objCrud/put JSONArray", root.put("cArr", jsonArr(1, 2)).optJSONArray("cArr")?.length(), 2)
        r.check("objCrud/put JSONObject", root.put("cObj", jsonObj("k" to "v")).optJSONObject("cObj")?.optString("k"), "v")
        r.check("objCrud/length after create", root.length(), 14)

        // ---- Update ----
        r.check("objCrud/overwrite same type", root.put("cInt", 8).optInt("cInt"), 8)
        r.check("objCrud/overwrite changes type", root.put("cInt", "eight").opt("cInt"), "eight")
        r.check("objCrud/overwrite does not grow", root.length(), 14)
        // 子对象是引用，改子对象即改父树
        user.put("id", 43)
        r.check("objCrud/nested update visible from root", root.optJSONObject("user")?.optInt("id"), 43)
        val tags = user.optJSONArray("tags")
        tags?.put("c")
        r.check("objCrud/nested array grew", root.optJSONObject("user")?.optJSONArray("tags")?.length(), 3)
        r.check("objCrud/nested array new item", root.optJSONObject("user")?.optJSONArray("tags")?.optString(2), "c")

        // ---- Delete ----
        // JSONObject 没有 remove()：put(key, null) 只是把值置空，key 仍然存在。
        r.check("objCrud/put null keeps key", root.put("cStr", null as Any?).has("cStr"), true)
        r.check("objCrud/put null clears value", root.opt("cStr"), null)
        r.check("objCrud/length after null put", root.length(), 14)
        // 底层 map 上的 remove 才真正删 key（JSONObject 未暴露，这里用 keySet 的视图验证语义）
        r.check("objCrud/keySet is live view", root.keySet().contains("cObj"), true)

        // ---- toMap ----
        val map = user.toMap()
        // toMap 不搬运 null 值（没有 null 分支），所以 nil 会消失
        r.check("objCrud/toMap drops null", map.containsKey("nil"), false)
        r.check("objCrud/toMap size", map.size, 5)
        r.check("objCrud/toMap Int", map["id"], 43)
        r.check("objCrud/toMap String", map["name"], "Alice")
        r.check("objCrud/toMap Double", map["score"], 9.5)
        r.check("objCrud/toMap Boolean", map["active"], true)
        r.check("objCrud/toMap nested list size", (map["tags"] as? List<*>)?.size, 3)
    }

    // ============================================================
    // Section 4: JSONArray 增删改查
    // ============================================================

    private fun runArrayCrudSection(variant: TokenerVariant, r: Recorder) {
        val arr = variant.parse(ARRAY_JSON) as? JSONArray
        if (arr == null) {
            r.fail("arrCrud: ARRAY_JSON 未解析成 JSONArray")
            return
        }

        // ---- Read ----
        r.check("arrCrud/length", arr.length(), 7)
        r.check("arrCrud/opt Int", arr.opt(0), 1)
        r.check("arrCrud/opt String", arr.opt(1), "two")
        r.check("arrCrud/opt Double", arr.opt(2), 3.5)
        r.check("arrCrud/opt Boolean", arr.opt(3), true)
        r.check("arrCrud/opt null", arr.opt(4), null)
        r.check("arrCrud/opt negative index", arr.opt(-1), null)
        r.check("arrCrud/opt out of range", arr.opt(99), null)
        r.check("arrCrud/optInt truncates Double", arr.optInt(2), 3)
        r.check("arrCrud/optLong truncates Double", arr.optLong(2), 3L)
        r.check("arrCrud/optString from Double", arr.optString(2), "3.5")
        r.check("arrCrud/optString null default", arr.optString(4), "")
        r.check("arrCrud/optString null fallback", arr.optString(4, "fb"), "fb")
        r.check("arrCrud/optString out of range", arr.optString(99), "")
        r.check("arrCrud/optDouble non-numeric fallback", arr.optDouble(1, -1.0), -1.0)
        r.check("arrCrud/optInt non-numeric fallback", arr.optInt(1, -1), -1)
        r.check("arrCrud/optBoolean", arr.optBoolean(3), true)
        r.check("arrCrud/optBoolean from non-zero Number", arr.optBoolean(0), true)
        r.check("arrCrud/optBoolean from null", arr.optBoolean(4), false)
        r.check("arrCrud/optJSONArray", arr.optJSONArray(5)?.optInt(0), 9)
        r.check("arrCrud/optJSONObject", arr.optJSONObject(6)?.optString("k"), "v")
        r.check("arrCrud/optJSONArray on scalar", arr.optJSONArray(0), null)
        r.check("arrCrud/optJSONObject on array", arr.optJSONObject(5), null)

        // ---- Create ----
        r.check("arrCrud/put Int", arr.put(42).opt(7), 42)
        r.check("arrCrud/put Long keeps type", arr.put(42L).opt(8), 42L)
        r.check("arrCrud/put Double keeps type", arr.put(4.25).opt(9), 4.25)
        r.check("arrCrud/put Boolean", arr.put(false).opt(10), false)
        r.check("arrCrud/put String", arr.put("added").opt(11), "added")
        r.check("arrCrud/put null", arr.put(null as Any?).opt(12), null)
        r.check("arrCrud/length after create", arr.length(), 13)

        // ---- Update（无 set API，改子容器内容） ----
        arr.optJSONObject(6)?.put("k", "v2")
        r.check("arrCrud/nested object update", arr.optJSONObject(6)?.optString("k"), "v2")
        arr.optJSONArray(5)?.put(10)
        r.check("arrCrud/nested array update", arr.optJSONArray(5)?.length(), 2)

        // ---- Delete ----
        r.check("arrCrud/remove returns value", arr.remove(0), 1)
        r.check("arrCrud/remove shifts left", arr.opt(0), "two")
        r.check("arrCrud/length after remove", arr.length(), 12)
        r.check("arrCrud/remove negative index", arr.remove(-1), null)
        r.check("arrCrud/remove out of range", arr.remove(99), null)
        r.check("arrCrud/length after failed removes", arr.length(), 12)

        // ---- toList ----
        val list = arr.toList()
        // toList 同样跳过 null 元素
        r.check("arrCrud/toList drops nulls", list.size, 10)

        if (usesCommonSerialization()) {
            // JSONArray.equals 比较底层 list；只有使用通用集合实现的平台成立。
            r.check("arrCrud/equals same content", jsonArr(1, "a") == jsonArr(1, "a"), true)
            r.check("arrCrud/equals different content", jsonArr(1, "a") == jsonArr(1, "b"), false)
            r.check("arrCrud/hashCode same content", jsonArr(1, "a").hashCode() == jsonArr(1, "a").hashCode(), true)
        }
    }

    // ============================================================
    // Section 5: 经由 JSONEngine 的入口
    // ============================================================

    private fun runEngineSection(variant: TokenerVariant, r: Recorder) {
        // JSONObject(String) / JSONArray(String) 走 JSONEngine.parse，即当前选中的实现
        r.check("engine/JSONObject(String)", JSONObject(CRUD_JSON).optJSONObject("user")?.optInt("id"), 42)
        r.check("engine/JSONArray(String)", JSONArray("[1,2]").length(), 2)
        r.check("engine/JSONObject(tokener-free ctor)", JSONObject().length(), 0)

        // 类型不匹配一定要抛错（异常消息里含容器的 toString，平台引擎格式不同，故分开校验）
        r.check("engine/JSONObject on array throws", captureError { JSONObject("[1]") } != NO_ERROR, true)
        r.check("engine/JSONArray on object throws", captureError { JSONArray("{}") } != NO_ERROR, true)

        if (usesCommonSerialization()) {
            r.checkText(
                "engine/JSONObject on array text",
                captureError { JSONObject("[1]") },
                "Value [1] of type JSONArray cannot be converted to JSONObject"
            )
            r.checkText(
                "engine/JSONArray on object text",
                captureError { JSONArray("{}") },
                "Value {} of type JSONObject cannot be converted to JSONArray"
            )
            r.checkText(
                "engine/JSONObject on scalar text",
                captureError { JSONObject("42") },
                "Value 42 of type Int cannot be converted to JSONObject"
            )
            r.checkText("engine/JSONObject on null literal", captureError { JSONObject("null") }, "Value is null.")
            // JSONEngine.parse 必须与被测实现产出一致的树（验证开关真的切到位了）
            r.checkText(
                "engine/parse matches variant",
                fingerprint(JSONEngine.parse(CRUD_JSON)),
                fingerprint(variant.parse(CRUD_JSON))
            )
        }
    }

    private fun captureError(block: () -> Unit): String {
        return try {
            block()
            NO_ERROR
        } catch (e: JSONException) {
            normalizeError(e.message)
        } catch (e: Exception) {
            "${e::class.simpleName}: ${normalizeError(e.message)}"
        }
    }

    // ============================================================
    // 语料
    // ============================================================

    private fun corpus(): List<JsonCase> = CORPUS

    private val CORPUS: List<JsonCase> by lazy { buildCorpus() }

    private fun buildCorpus(): List<JsonCase> = listOf(
        // ---- 标量根：布尔 / null ----
        JsonCase("true", "true", true),
        JsonCase("false", "false", false),
        JsonCase("null", "null", null),
        JsonCase("TRUE is not a literal", "TRUE", "TRUE"),

        // ---- 标量根：整数与类型选型 ----
        JsonCase("int", "123", 123),
        JsonCase("negative int", "-123", -123),
        JsonCase("minus zero", "-0", 0),
        JsonCase("plus sign", "+5", 5),
        JsonCase("zero", "0", 0),
        JsonCase("double zero is octal zero", "00", 0),
        JsonCase("int max", "2147483647", 2147483647),
        JsonCase("int max + 1 becomes Long", "2147483648", 2147483648L),
        JsonCase("int min", "-2147483648", -2147483648),
        JsonCase("int min - 1 becomes Long", "-2147483649", -2147483649L),
        JsonCase("long max", "9223372036854775807", 9223372036854775807L),
        JsonCase("long min", "-9223372036854775808", Long.MIN_VALUE),
        // 溢出后落到 Double，文本表示交给差分校验
        JsonCase("long max + 1 falls to Double", "9223372036854775808"),
        JsonCase("20 digits falls to Double", "99999999999999999999"),

        // ---- 标量根：十六进制 / 八进制怪癖 ----
        JsonCase("hex lower", "0x1F", 31),
        JsonCase("hex upper prefix", "0X1f", 31),
        JsonCase("octal", "010", 8),
        JsonCase("octal 0777", "0777", 511),
        JsonCase("negative octal stays decimal", "-010", -10),
        JsonCase("octal with invalid digit falls to Double", "08", 8.0),
        JsonCase("sign inside hex", "0x-10", -16),
        JsonCase("plus inside hex", "0x+10", 16),
        JsonCase("bare hex prefix is a String", "0x", "0x"),

        // ---- 标量根：浮点 ----
        JsonCase("double", "1.5", 1.5),
        JsonCase("negative double", "-1.5", -1.5),
        JsonCase("leading dot", ".5", 0.5),
        JsonCase("trailing dot", "5.", 5.0),
        JsonCase("exponent lower", "1e3", 1000.0),
        JsonCase("exponent upper signed", "1E+3", 1000.0),
        JsonCase("negative exponent", "5e-1", 0.5),
        JsonCase("dot with octal prefix", "0.5", 0.5),
        JsonCase("NaN literal", "NaN"),
        JsonCase("Infinity literal", "Infinity"),

        // ---- 标量根：无引号字面量退化成 String ----
        JsonCase("bare word", "hello", "hello"),
        JsonCase("lone minus", "-", "-"),
        JsonCase("version-like literal", "1.2.3", "1.2.3"),

        // ---- 字符串 ----
        JsonCase("double quoted", "\"abc\"", "abc"),
        JsonCase("single quoted", "'abc'", "abc"),
        JsonCase("empty string", "\"\"", ""),
        JsonCase("quote inside single quoted", "'a\"b'", "a\"b"),
        JsonCase("escaped quote", "\"a\\\"b\"", "a\"b"),
        JsonCase("escaped backslash", "\"a\\\\b\"", "a\\b"),
        JsonCase("escaped solidus", "\"a\\/b\"", "a/b"),
        JsonCase("escaped control chars", "\"\\b\\f\\n\\r\\t\"", "\b\u000C\n\r\t"),
        JsonCase("escaped apostrophe", "\"\\'\"", "'"),
        JsonCase("unknown escape keeps char", "\"\\q\"", "q"),
        JsonCase("unicode escape", "\"\\u0041\"", "A"),
        JsonCase("unicode escape cjk", "\"\\u4e2d\\u6587\"", "中文"),
        JsonCase("unicode escape NUL", "\"\\u0000\"", "\u0000"),
        JsonCase("unicode surrogate pair", "\"\\ud83d\\ude00\"", "\uD83D\uDE00"),
        JsonCase("unicode escape uppercase hex", "\"\\u00E9\"", "é"),
        // toInt(16) 接受带符号输入，这条走的是「4 位十六进制解析失败后的兜底路径」
        JsonCase("unicode escape with plus sign", "\"\\u+12a\"", "\u012A"),
        JsonCase("raw newline inside string", "\"a\nb\"", "a\nb"),
        JsonCase("raw tab inside string", "\"a\tb\"", "a\tb"),
        JsonCase("string with trailing garbage", "\"abc\"def", "abc"),

        // ---- 对象 ----
        JsonCase("empty object", "{}", jsonObj()),
        JsonCase("empty object with space", "{ }", jsonObj()),
        JsonCase("flat object", "{\"a\":1,\"b\":\"x\",\"c\":true,\"d\":null}", jsonObj("a" to 1, "b" to "x", "c" to true, "d" to null)),
        JsonCase("nested object", "{\"a\":{\"b\":{\"c\":1}}}", jsonObj("a" to jsonObj("b" to jsonObj("c" to 1)))),
        JsonCase("object with array", "{\"a\":[1,2]}", jsonObj("a" to jsonArr(1, 2))),
        JsonCase("single quoted key", "{'a':1}", jsonObj("a" to 1)),
        JsonCase("unquoted key", "{a:1}", jsonObj("a" to 1)),
        JsonCase("unquoted key and value", "{a:b}", jsonObj("a" to "b")),
        JsonCase("equals separator", "{\"a\"=1}", jsonObj("a" to 1)),
        JsonCase("arrow separator", "{\"a\"=>1}", jsonObj("a" to 1)),
        JsonCase("semicolon separator", "{\"a\":1;\"b\":2}", jsonObj("a" to 1, "b" to 2)),
        JsonCase("duplicate key last wins", "{\"a\":1,\"a\":2}", jsonObj("a" to 2)),
        JsonCase("empty key", "{\"\":1}", jsonObj("" to 1)),
        JsonCase("whitespace everywhere", "\t\n\r {\"a\" : \t1 }", jsonObj("a" to 1)),
        JsonCase("object with trailing garbage", "{\"a\":1} trailing", jsonObj("a" to 1)),
        JsonCase("second root ignored", "{\"a\":1}{\"b\":2}", jsonObj("a" to 1)),
        JsonCase("hash comment ends literal", "{\"a\":1#c\n}", jsonObj("a" to 1)),

        // ---- 数组 ----
        JsonCase("empty array", "[]", jsonArr()),
        JsonCase("flat array", "[1,\"a\",true,null]", jsonArr(1, "a", true, null)),
        JsonCase("nested array", "[[1],[2,[3]]]", jsonArr(jsonArr(1), jsonArr(2, jsonArr(3)))),
        JsonCase("array of objects", "[{\"a\":1},{\"a\":2}]", jsonArr(jsonObj("a" to 1), jsonObj("a" to 2))),
        JsonCase("array semicolon separator", "[1;2]", jsonArr(1, 2)),
        JsonCase("array double comma skips", "[1,,2]", jsonArr(1, 2)),
        JsonCase("array leading comma skips", "[,1]", jsonArr(1)),
        JsonCase("array trailing comma ok", "[1,]", jsonArr(1)),
        JsonCase("array with trailing garbage", "[1] trailing", jsonArr(1)),

        // ---- 注释与 BOM ----
        JsonCase("block comment before root", "/*c*/{\"a\":1}", jsonObj("a" to 1)),
        JsonCase("block comment inside", "{\"a\"/*k*/:/*v*/1}", jsonObj("a" to 1)),
        JsonCase("line comment before root", "//c\n{\"a\":1}", jsonObj("a" to 1)),
        JsonCase("line comment after root", "{\"a\":1}//tail", jsonObj("a" to 1)),
        JsonCase("hash comment before root", "#c\n{\"a\":1}", jsonObj("a" to 1)),
        JsonCase("slash is a literal when not a comment", "{\"a\":1}/x", jsonObj("a" to 1)),
        JsonCase("bom stripped", "\uFEFF{\"a\":1}", jsonObj("a" to 1)),
        JsonCase("bom before scalar", "\uFEFF42", 42),

        // ---- 结构压力 ----
        JsonCase("deep nesting", deepNest(30)),
        JsonCase("wide object", wideObject(60)),
        JsonCase("wide array", wideArray(60)),
        JsonCase("mixed real-world shape", REAL_WORLD_JSON),

        // ---- 异常 ----
        JsonCase("empty input", "", expectedError = "End of input"),
        JsonCase("whitespace only", "   ", expectedError = "End of input"),
        JsonCase("comment only", "//c", expectedError = "End of input"),
        JsonCase("open brace only", "{", expectedError = "End of input"),
        JsonCase("unterminated object", "{\"a\":1", expectedError = "Unterminated object"),
        JsonCase("missing separator", "{\"a\" 1}", expectedError = "Expected ':' after a"),
        JsonCase("bad value separator", "{\"a\":1 \"b\":2}", expectedError = "Unterminated object"),
        JsonCase("object trailing comma", "{\"a\":1,}", expectedError = "Expected literal value"),
        JsonCase("object double comma", "{\"a\":1,,\"b\":2}", expectedError = "Expected literal value"),
        JsonCase("non-string name", "{1:2}", expectedError = "Names must be strings, but 1 is of type Int"),
        JsonCase("null name", "{null:2}", expectedError = "Names must be strings, but null is of type null"),
        JsonCase("open bracket only", "[", expectedError = "Unterminated array"),
        JsonCase("unterminated array", "[1", expectedError = "Unterminated array"),
        JsonCase("array missing separator", "[1 2]", expectedError = "Unterminated array"),
        JsonCase("unterminated string", "\"abc", expectedError = "Unterminated string"),
        JsonCase("unterminated single quoted", "'abc", expectedError = "Unterminated string"),
        JsonCase("dangling escape", "\"abc\\", expectedError = "Unterminated escape sequence"),
        JsonCase("truncated unicode escape", "\"\\u12\"", expectedError = "Unterminated escape sequence"),
        JsonCase("invalid unicode escape", "\"\\uZZZZ\"", expectedError = "Invalid escape sequence: ZZZZ"),
        JsonCase("unterminated block comment", "/*c", expectedError = "Unterminated comment"),
        JsonCase("stop char as value", ",", expectedError = "Expected literal value"),
        JsonCase("close brace as root", "}", expectedError = "Expected literal value")
    )

    private fun deepNest(depth: Int): String {
        val sb = StringBuilder()
        for (i in 0 until depth) {
            sb.append("{\"l$i\":[")
        }
        sb.append("\"leaf\"")
        for (i in 0 until depth) {
            sb.append("]}")
        }
        return sb.toString()
    }

    private fun wideObject(size: Int): String {
        val sb = StringBuilder("{")
        for (i in 0 until size) {
            if (i > 0) sb.append(',')
            sb.append("\"k$i\":")
            when (i % 5) {
                0 -> sb.append(i)
                1 -> sb.append("\"v$i\"")
                2 -> sb.append(i % 2 == 0)
                3 -> sb.append("null")
                else -> sb.append("$i.5")
            }
        }
        return sb.append('}').toString()
    }

    private fun wideArray(size: Int): String {
        val sb = StringBuilder("[")
        for (i in 0 until size) {
            if (i > 0) sb.append(',')
            when (i % 4) {
                0 -> sb.append(i)
                1 -> sb.append("\"v$i\"")
                2 -> sb.append("{\"i\":$i}")
                else -> sb.append("[$i]")
            }
        }
        return sb.append(']').toString()
    }

    private const val MAX_REPORTED_DIFFS = 10

    private const val NO_ERROR = "<no error>"

    private const val CRUD_JSON =
        """{"user":{"id":42,"name":"Alice","score":9.5,"active":true,"nil":null,"tags":["a","b"]},""" +
                """"items":[1,2,3],"big":2147483648,"strNum":"42","strBool":"TRUE",""" +
                """"objStr":"{\"x\":1}","arrStr":"[1,2]"}"""

    private const val ARRAY_JSON = """[1,"two",3.5,true,null,[9],{"k":"v"}]"""

    private const val REAL_WORLD_JSON =
        """{"code":0,"msg":"ok","data":{"list":[{"id":1,"title":"第一条","tags":["a","b"],""" +
                """"score":9.75,"ok":true,"extra":null},{"id":2,"title":"line\nbreak",""" +
                """"tags":[],"score":0.0,"ok":false,"extra":{"deep":{"deeper":[1,2,3]}}}],""" +
                """"page":{"index":1,"size":20,"total":2147483648},"unicode":"\u4e16\u754c"}}"""
}

// ============================================================
// 报告 / 记录器 / 工具
// ============================================================

internal class ConformanceReport(
    val variantNames: List<String>,
    val checks: Int,
    val failures: List<String>,
    val summary: List<String>
) {
    val passed: Boolean get() = failures.isEmpty()
}

/** 只做实现间差分、不写绝对期望值的哨兵。 */
private val NO_EXPECT = Any()

private class JsonCase(
    val name: String,
    val json: String,
    val expected: Any? = NO_EXPECT,
    val expectedError: String? = null
)

private class Recorder(private val variantName: String) {
    /** 观测序列，用于实现间逐行差分；不含实现名等实现相关内容。 */
    val trace = mutableListOf<String>()
    val failures = mutableListOf<String>()
    var checks = 0

    fun record(label: String, rendered: String) {
        trace.add("$label = $rendered")
    }

    fun check(label: String, actual: Any?, expected: Any?) {
        val actualText = fingerprint(actual)
        trace.add("$label = $actualText")
        if (expected === NO_EXPECT) {
            return
        }
        checks++
        val expectedText = fingerprint(expected)
        if (actualText != expectedText) {
            fail("$label: 期望 $expectedText，实际 $actualText")
        }
    }

    fun checkText(label: String, actual: String, expected: String, record: Boolean = true) {
        if (record) {
            trace.add("$label = ${escape(actual)}")
        }
        checks++
        if (actual != expected) {
            fail("$label: 期望 ${escape(expected)}，实际 ${escape(actual)}")
        }
    }

    fun fail(message: String) {
        failures.add("[$variantName] $message")
    }
}

/**
 * 值指纹：带运行时类型，因此 `Int(1)` / `Long(1)` / `Dbl(1.0)` 互不相等——数字选型
 * 是本次优化最容易踩坏的地方。对象按 key 排序输出，避免不同平台 map 实现的迭代顺序
 * 影响比对（顺序稳定性由序列化幂等性用例覆盖）。
 */
private fun fingerprint(value: Any?): String = when (value) {
    null -> "null"
    is Boolean -> "Bool($value)"
    is Int -> "Int($value)"
    is Long -> "Long($value)"
    is Double -> "Dbl($value)"
    is Float -> "Flt($value)"
    is String -> "Str(${escape(value)})"
    is JSONObject -> {
        val sb = StringBuilder("Obj{")
        var first = true
        for (key in value.keySet().sorted()) {
            if (!first) sb.append(',')
            first = false
            sb.append(escape(key)).append(':').append(fingerprint(value.opt(key)))
        }
        sb.append('}').toString()
    }
    is JSONArray -> {
        val sb = StringBuilder("Arr[")
        for (i in 0 until value.length()) {
            if (i > 0) sb.append(',')
            sb.append(fingerprint(value.opt(i)))
        }
        sb.append(']').toString()
    }
    is Map<*, *> -> {
        val sb = StringBuilder("Map{")
        var first = true
        val rendered = value.entries.map { escape(it.key.toString()) + ":" + fingerprint(it.value) }.sorted()
        for (entry in rendered) {
            if (!first) sb.append(',')
            first = false
            sb.append(entry)
        }
        sb.append('}').toString()
    }
    is List<*> -> {
        val sb = StringBuilder("List[")
        for (i in value.indices) {
            if (i > 0) sb.append(',')
            sb.append(fingerprint(value[i]))
        }
        sb.append(']').toString()
    }
    else -> "${value::class.simpleName}($value)"
}

/**
 * 宽松指纹：所有数字折叠成 Double。用于「序列化再回解」的值比对——
 * `JSON.numberToString` 会把整值 Double 写成整数，回解后类型必然变化。
 */
private fun looseFingerprint(value: Any?): String = when (value) {
    null -> "null"
    is Boolean -> "Bool($value)"
    is Number -> "Num(${value.toDouble()})"
    is String -> "Str(${escape(value)})"
    is JSONObject -> {
        val sb = StringBuilder("Obj{")
        var first = true
        for (key in value.keySet().sorted()) {
            if (!first) sb.append(',')
            first = false
            sb.append(escape(key)).append(':').append(looseFingerprint(value.opt(key)))
        }
        sb.append('}').toString()
    }
    is JSONArray -> {
        val sb = StringBuilder("Arr[")
        for (i in 0 until value.length()) {
            if (i > 0) sb.append(',')
            sb.append(looseFingerprint(value.opt(i)))
        }
        sb.append(']').toString()
    }
    else -> "${value::class.simpleName}($value)"
}

/** 把不可见字符转成 \\uXXXX，保证观测行可读且可逐字符比对。 */
private fun escape(text: String): String {
    val sb = StringBuilder("\"")
    for (c in text) {
        when {
            c == '"' -> sb.append("\\\"")
            c == '\\' -> sb.append("\\\\")
            c.code in 0x20..0x7E -> sb.append(c)
            c.code in 0xA1..0xFFFD && c.code !in 0xD800..0xDFFF -> sb.append(c)
            else -> sb.append("\\u").append(hex4(c.code))
        }
    }
    return sb.append('"').toString()
}

private fun hex4(code: Int): String {
    val digits = "0123456789abcdef"
    val sb = StringBuilder()
    var shift = 12
    while (shift >= 0) {
        sb.append(digits[(code shr shift) and 0xF])
        shift -= 4
    }
    return sb.toString()
}

/**
 * 剥掉异常消息尾部的 tokener 自身 `toString()`。
 *
 * `JSONTokener.syntaxError` 的实现是 `JSONException(message + this)`，两个 tokener 类名
 * 与实例 hash 都不同，不剥掉就没法比对消息。
 */
private fun normalizeError(message: String?): String {
    if (message == null) {
        return "<null>"
    }
    var cut = message
    val pkg = cut.indexOf("com.tencent.kuikly")
    if (pkg >= 0) {
        cut = cut.substring(0, pkg)
    }
    val cls = cut.indexOf("JSONTokener")
    if (cls >= 0) {
        cut = cut.substring(0, cls)
    }
    // 兜底：某些运行时的 Any.toString() 只有 "@hash"
    val at = cut.indexOf('@')
    if (at >= 0) {
        cut = cut.substring(0, at)
    }
    return cut
}

/**
 * 当前平台的 `JSONEngine` 是否使用 commonMain 的 `JSONStringer` 与有序 map。
 * JS 走原生 `JSON.stringify` + 无序 key 集合，精确文本类断言在那边不成立。
 */
private fun usesCommonSerialization(): Boolean =
    JSONObject().put("probe", 1).toString() == "{\"probe\": 1}"

private fun stringifyTree(tree: Any?): String = when (tree) {
    is JSONObject -> tree.toString()
    is JSONArray -> tree.toString()
    else -> "<non-container: ${fingerprint(tree)}>"
}

private fun jsonObj(vararg pairs: Pair<String, Any?>): JSONObject {
    val obj = JSONObject()
    for ((key, value) in pairs) {
        obj.put(key, value)
    }
    return obj
}

private fun jsonArr(vararg values: Any?): JSONArray {
    val arr = JSONArray()
    for (value in values) {
        arr.put(value)
    }
    return arr
}
