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
 * JS 实现：底层容器为 [JSONEngine] 提供的 JS 原生对象包装（`FastMutableMap`），
 * 解析与序列化走 `JSON.parse` / `JSON.stringify`，与重构前完全一致。
 */
actual class JSONObject internal actual constructor(
    nameValuePairs: MutableMap<String, Any?>
) : AbstractJSONObject(nameValuePairs) {

    actual constructor() : this(JSONEngine.getMutableMap())

    actual constructor(json: String) : this(requireJSONObjectPairs(JSONEngine.parse(json)))

    actual constructor(jsonTokener: JSONTokener) : this(requireJSONObjectPairs(jsonTokener.nextValue()))

    actual companion object {
        actual fun quote(data: String?): String = quoteJSONString(data)
    }
}
