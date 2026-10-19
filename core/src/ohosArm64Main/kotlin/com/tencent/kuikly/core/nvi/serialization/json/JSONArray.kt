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
 * OHOS 实现：底层容器可以是 Kotlin List（代码构造）或原生 KRJSON 数组
 * （统一 KRJSON 桥接 / [JSONTokener] 快路径，见 [LazyJsonList]）。
 */
actual class JSONArray internal actual constructor(
    values: MutableList<Any?>
) : AbstractJSONArray(values) {

    actual constructor() : this(JSONEngine.getMutableList())

    @Throws(JSONException::class)
    actual constructor(json: String) : this(requireJSONArrayValues(JSONEngine.parse(json)))

    @Throws(JSONException::class)
    actual constructor(jsonTokener: JSONTokener) : this(requireJSONArrayValues(jsonTokener.nextValue()))

    /** 包装原生 KRJSON 数组，读取时按需转换，不做整树拷贝。 */
    internal constructor(list: LazyJsonList) : this(list as MutableList<Any?>)
}