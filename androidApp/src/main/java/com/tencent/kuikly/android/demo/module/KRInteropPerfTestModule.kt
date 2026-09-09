/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.android.demo.module

import android.os.SystemClock
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject

class KRInteropPerfTestModule : KuiklyRenderBaseModule() {

    private var statsCallback: KuiklyRenderCallback? = null
    private val plainStringCallbacks = HashMap<String, KuiklyRenderCallback?>()
    private val jsonCallbacks = HashMap<String, KuiklyRenderCallback?>()
    private val plainStringPayloadCaches = HashMap<String, List<String>>()
    private val jsonStringPayloadCaches = HashMap<String, List<String>>()
    private val krRecordPayloadCaches = HashMap<String, List<String>>()

    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {
        when (method) {
            "SetStatsCallback" -> statsCallback = callback
            "SetCallbackWithPlainString" -> plainStringCallbacks[params as? String ?: ""] = callback
            "SetCallbackWithJson" -> jsonCallbacks[params as? String ?: ""] = callback
            "RunPlainString" -> {
                val config = parseConfig(params)
                val payloads = preparePlainStringPayloads(config.caseName, config.count, config.payloadChars)
                runPreparedStrings(config.caseName, payloads, plainStringCallbacks[config.caseName])
            }
            "RunJsonString" -> {
                val config = parseConfig(params)
                val payloads = prepareJsonStringPayloads(config.caseName, config.count, config.payloadChars)
                runPreparedStrings(config.caseName, payloads, jsonCallbacks[config.caseName])
            }
            "RunKRRecord" -> {
                val config = parseConfig(params)
                val payloads = prepareKRRecordPayloads(config.caseName, config.count, config.payloadChars)
                runPreparedStrings(config.caseName, payloads, jsonCallbacks[config.caseName])
            }
            "RunKRJsonValue" -> {
                runPreparedKRJsonValue(params, jsonCallbacks)
            }
        }
        return null
    }

    private data class PerfConfig(val caseName: String, val count: Int, val payloadChars: Int)

    private fun parseConfig(params: Any?): PerfConfig {
        val text = params as? String ?: ""
        val caseName = regexFind(text, "\"case\"\\s*:\\s*\"([^\"]+)\"", "")
        val count = regexFind(text, "\"count\"\\s*:\\s*(\\d+)", DEFAULT_COUNT.toString()).toInt()
        val payloadChars = regexFind(text, "\"payloadChars\"\\s*:\\s*(\\d+)", DEFAULT_PAYLOAD_CHARS.toString()).toInt()
        return PerfConfig(caseName, count, payloadChars)
    }

    private fun regexFind(text: String, pattern: String, fallback: String): String {
        val match = Regex(pattern).find(text) ?: return fallback
        return match.groupValues.getOrElse(1) { fallback }
    }

    private fun makePad(targetLen: Int): String {
        val token = "0123456789abcdef测Abc"
        val builder = StringBuilder(targetLen)
        while (builder.length < targetLen) {
            val remaining = targetLen - builder.length
            if (remaining >= token.length) {
                builder.append(token)
            } else {
                builder.append(token, 0, remaining)
            }
        }
        return builder.toString()
    }

    private fun makeJsonString(i: Int, targetLen: Int): String {
        val json = JSONObject()
            .put("kind", "json_string")
            .put("i", i)
            .put("v", "")
        val padLen = (targetLen - json.toString().length).coerceAtLeast(0)
        json.put("v", makePad(padLen))
        return json.toString()
    }

    private fun makeKRRecord(i: Int, targetLen: Int): String {
        val json = JSONObject()
            .put("kind", "kr_record")
            .put("i", i)
            .put("v", "")
        val padLen = (targetLen - json.toString().length).coerceAtLeast(0)
        json.put("v", makePad(padLen))
        return json.toString()
    }

    private fun preparePlainStringPayloads(caseName: String, count: Int, payloadChars: Int): List<String> {
        val cached = plainStringPayloadCaches[caseName]
        if (cached != null && cached.size == count) return cached
        val out = ArrayList<String>(count)
        repeat(count) { out.add(makePad(payloadChars)) }
        plainStringPayloadCaches[caseName] = out
        return out
    }

    private fun prepareJsonStringPayloads(caseName: String, count: Int, payloadChars: Int): List<String> {
        val cached = jsonStringPayloadCaches[caseName]
        if (cached != null && cached.size == count) return cached
        val out = ArrayList<String>(count)
        repeat(count) { i -> out.add(makeJsonString(i, payloadChars)) }
        jsonStringPayloadCaches[caseName] = out
        return out
    }

    private fun prepareKRRecordPayloads(caseName: String, count: Int, payloadChars: Int): List<String> {
        val cached = krRecordPayloadCaches[caseName]
        if (cached != null && cached.size == count) return cached
        val out = ArrayList<String>(count)
        repeat(count) { i -> out.add(makeKRRecord(i, payloadChars)) }
        krRecordPayloadCaches[caseName] = out
        return out
    }

    private fun runPreparedStrings(caseName: String, payloads: List<String>, callback: KuiklyRenderCallback?) {
        var bytes = 0L
        for (payload in payloads) bytes += payload.length
        val start = SystemClock.elapsedRealtime()
        for (payload in payloads) callback?.invoke(payload)
        report(caseName, payloads.size, SystemClock.elapsedRealtime() - start, bytes)
    }

    private fun runPreparedKRJsonValue(params: Any?, callbacks: Map<String, KuiklyRenderCallback?>) {
        val text = params as? String ?: return
        val request = JSONObject(text)
        val caseName = request.optString("case")
        val count = request.optInt("count")
        val payload = request.optJSONObject("payload")?.toString() ?: return
        val bytes = payload.length.toLong() * count
        val callback = callbacks[caseName]
        val start = SystemClock.elapsedRealtime()
        repeat(count) { callback?.invoke(payload) }
        report(caseName, count, SystemClock.elapsedRealtime() - start, bytes)
    }

    private fun report(caseName: String, count: Int, costMs: Long, bytes: Long) {
        val line = JSONObject()
            .put("case", caseName)
            .put("count", count)
            .put("cost_ms", costMs)
            .put("bytes", bytes)
            .toString()
        statsCallback?.invoke(line)
    }

    companion object {
        const val MODULE_NAME = "KRInteropPerfTestModule"
        private const val DEFAULT_COUNT = 50
        private const val DEFAULT_PAYLOAD_CHARS = 128 * 1024
    }
}
