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

package com.tencent.kuikly.core.nvi.serialization.json

/**
 * OHOS 实现：严格 JSON 对象 / 数组优先走 KRJSON（RapidJSON SAX），结果包成惰性
 * [LazyJsonMap] / [LazyJsonList]；注释与无引号键等历史写法回退到 [AbstractJSONTokener]。
 *
 * KRJSON 对象重复 key 是 last-wins（与 org.json 一致），因此不再因重复键回退。
 */
actual class JSONTokener actual constructor(json: String) : AbstractJSONTokener(json) {

    private val source = json
    private var nativeTried = false

    @Throws(JSONException::class)
    actual override fun nextValue(): Any? {
        tryNative()?.let { return it }
        return super.nextValue()
    }

    private fun tryNative(): Any? {
        if (nativeTried) {
            return null
        }
        if (source.isEmpty()) {
            return null
        }
        val rootChar = firstNonWhitespace(source) ?: return null
        if (rootChar != '{' && rootChar != '[') {
            return null
        }
        nativeTried = true
        val owned = JsonNative.ownerFromJson(source)
        if (owned == 0L) {
            return null
        }
        return try {
            when (JsonNative.type(owned)) {
                JSON_KIND_OBJECT -> LazyJsonMap.fromOwner(owned)
                JSON_KIND_ARRAY -> LazyJsonList.fromOwner(owned)
                else -> null
            }
        } finally {
            JsonNative.release(owned)
        }
    }
}

private fun firstNonWhitespace(text: String): Char? {
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
            return c
        }
        i++
    }
    return null
}