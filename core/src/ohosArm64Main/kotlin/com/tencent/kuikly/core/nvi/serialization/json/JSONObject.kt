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
 * OHOS 实现：底层容器可以是 Kotlin Map（代码构造 / 字符串解析）或原生 KRJSON 树
 * （统一 KRJSON 桥接入参，见 [LazyJsonMap]）。
 */
actual class JSONObject internal actual constructor(
    nameValuePairs: MutableMap<String, Any?>
) : AbstractJSONObject(nameValuePairs) {

    actual constructor() : this(JSONEngine.getMutableMap())

    @Throws(JSONException::class)
    actual constructor(json: String) : this(requireJSONObjectPairs(JSONEngine.parse(json)))

    @Throws(JSONException::class)
    actual constructor(jsonTokener: JSONTokener) : this(requireJSONObjectPairs(jsonTokener.nextValue()))

    actual override fun keyAt(index: Int): String? {
        val lazy = nameValuePairs as? LazyJsonMap ?: return super.keyAt(index)
        return lazy.keyAt(index)
    }

    actual override fun opt(index: Int): Any? {
        // null is a valid value here; falling back to super would index every key.
        val lazy = nameValuePairs as? LazyJsonMap ?: return super.opt(index)
        return lazy.valueAt(index)
    }

    actual companion object {
        actual fun quote(data: String?): String = quoteJSONString(data)

        /**
         * 包装桥接传入的 KRJSON 位型，读取时按需转换。
         * 调用方仍拥有传入字的所有权；这里会再 retain 一份。
         */
        internal fun fromJsonOwner(ownerPtr: Long): JSONObject = LazyJsonMap.fromOwner(ownerPtr)

        /**
         * 同 [fromJsonOwner]，但根节点是数组时返回 [JSONArray]。
         */
        internal fun fromJsonOwnerAny(ownerPtr: Long): Any? = LazyJsonMap.fromOwnerAny(ownerPtr)
    }
}

/** Returns an owned KRJSON word when this object is still lazy-native-backed. */
internal fun JSONObject.retainNativeBitsOrNull(): Long? {
    return (nameValuePairs as? LazyJsonMap)?.retainNativeOrNull()
}
