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
 * JS 实现：底层容器为 [JSONEngine] 提供的 JS 原生数组包装（`FastMutableList`），
 * 解析与序列化走 `JSON.parse` / `JSON.stringify`，与重构前完全一致。
 */
actual class JSONArray internal actual constructor(
    values: MutableList<Any?>
) : AbstractJSONArray(values) {

    actual constructor() : this(JSONEngine.getMutableList())

    actual constructor(json: String) : this(requireJSONArrayValues(JSONEngine.parse(json)))

    actual constructor(jsonTokener: JSONTokener) : this(requireJSONArrayValues(jsonTokener.nextValue()))
}
