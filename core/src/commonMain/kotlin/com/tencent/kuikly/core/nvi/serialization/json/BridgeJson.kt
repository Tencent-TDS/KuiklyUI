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
 * 把桥接层送来的数据归一成 [JSONObject]。
 *
 * 平台侧结构化数据（OHOS 的 KRJSON、iOS 的 `NSDictionary`）在各平台
 * callKotlin 入口已经包成惰性 [JSONObject]，直接返回；历史路径送来的是 JSON 字符串，
 * 按原有语义解析（非法文本照旧抛 [JSONException]）。其余类型（含二进制 `ByteArray`）
 * 不是 JSON 对象，返回 `null` 由调用方决定如何处理。
 */
internal fun asBridgeJSONObject(data: Any?): JSONObject? {
    return when (data) {
        null -> null
        is JSONObject -> data
        is String -> JSONObject(data)
        else -> null
    }
}
