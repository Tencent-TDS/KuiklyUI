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

actual object JSONEngine {

    /**
     * 解析实现开关：true 用 K/N 专项优化的 [JSONTokenerOhos]，false 用 commonMain
     * 的 [JSONTokener]。默认 false（legacy）。两者解析语义一致（由 JsonPerfTestPage
     * 的一致性用例集守护），可随时整体切换：线上疑似解析问题时用它快速排除优化版，
     * 做 A/B 时用它在同一个包里对比两条实现。
     *
     * 非线程安全，且不保证切换瞬间在途的解析用哪一份实现，只应在初始化阶段或测试中改。
     */
    var useOptimizedTokener: Boolean = false

    actual fun parse(jsonStr: String): Any? {
        return if (useOptimizedTokener) {
            JSONTokenerOhos(jsonStr).nextValue()
        } else {
            JSONTokener(jsonStr).nextValue()
        }
    }

    actual fun stringify(jsonObject: JSONObject) = commonStringify(jsonObject)

    actual fun stringify(jsonArray: JSONArray) = commonStringify(jsonArray)

    internal actual fun <K, V> getMutableMap(): MutableMap<K, V> = mutableMapOf()

    internal actual fun <E> getMutableList(): MutableList<E> = mutableListOf()
}