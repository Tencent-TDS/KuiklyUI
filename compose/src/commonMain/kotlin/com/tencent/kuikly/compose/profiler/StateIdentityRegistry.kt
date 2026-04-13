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

package com.tencent.kuikly.compose.profiler

import com.tencent.kuikly.compose.material3.internal.identityHashCode

/**
 * State 身份注册表。
 *
 * 为每个 State 对象记录：
 * - 上一次已知的 value（prevValue），用于输出 "prev=x, now=y" 格式
 * - 读取该 State 的 Composable 名称集合（readers）
 *
 * 本类非线程安全，由 [RecompositionTracker] 负责同步。
 */
internal class StateIdentityRegistry {

    /** identity hash → 上次 apply 时记录的 value 字符串（作为下次的 prev） */
    private val identityToPrevValue = mutableMapOf<Int, String?>()

    /** identity hash → 读取该 State 的 Composable 名称集合 */
    private val identityToReaders = mutableMapOf<Int, MutableSet<String>>()

    /**
     * 在 apply 回调结束后更新某个 State 的 "lastSeen" value。
     * 下次该 State 变更时，此值将作为 prev 输出。
     *
     * @param state State 对象（apply 后已是新值）
     */
    fun updateLastSeenValue(state: Any) {
        val hash = identityHashCode(state)
        identityToPrevValue[hash] = extractValue(state.toString())
    }

    /**
     * 记录一次 State 读取关系：某个 Composable 读取了某个 State。
     *
     * @param state State 对象
     * @param composableName 读取该 State 的 Composable 名称
     */
    fun recordReader(state: Any, composableName: String) {
        val hash = identityHashCode(state)
        identityToReaders.getOrPut(hash) { mutableSetOf() }.add(composableName)
    }

    /**
     * 将 State 对象格式化为可读的身份字符串。
     *
     * 主要在 apply callback 中调用（此时 state 已是新值，prevValue 是上次存的旧值）。
     * 精确路径的 cache miss 场景也会调用（State 未参与本次 apply，降级为 value= 格式）。
     *
     * 输出格式：
     * - `State(prev=0, now=1), readers: CounterSection` — 正常变更
     * - `State(now=1), readers: CounterSection` — 首次变更，无 prev 记录
     * - `State(value=1), readers: CounterSection` — prevValue == nowValue（值未变化）
     */
    fun formatState(state: Any): String {
        val hash = identityHashCode(state)
        val prevValue = identityToPrevValue[hash]
        val nowValue = extractValue(state.toString())
        val readers = identityToReaders[hash]

        return buildString {
            append("State(")
            when {
                prevValue == null -> append("now=$nowValue")
                prevValue == nowValue -> append("value=$nowValue")
                else -> append("prev=$prevValue, now=$nowValue")
            }
            append(")")
            if (!readers.isNullOrEmpty()) {
                append(", readers: ")
                append(readers.joinToString(", "))
            }
        }
    }

    /**
     * 使用 apply callback 缓存的 prev/now 对格式化 State 字符串。
     *
     * 用于精确路径（CompositionObserver）：traceEventEnd 在 apply 之后执行（State 已是新值），
     * 但 registry 里的 prevValue 已被 updateLastSeenValue 覆盖。
     * 因此使用 apply callback 里预先缓存的 (prevValue, nowValue) 来格式化。
     *
     * @param state State 对象
     * @param stateChangeCache apply callback 中缓存的 {identityHashCode → (prevValue, nowValue)}
     */
    fun formatStateFromCache(state: Any, stateChangeCache: Map<Int, Pair<String?, String?>>): String {
        val hash = identityHashCode(state)
        val readers = identityToReaders[hash]
        val cached = stateChangeCache[hash]

        return buildString {
            append("State(")
            if (cached != null) {
                val (prevValue, nowValue) = cached
                when {
                    prevValue == null -> append("now=$nowValue")
                    prevValue == nowValue -> append("value=$nowValue")
                    else -> append("prev=$prevValue, now=$nowValue")
                }
            } else {
                // State 不在 cache 中（apply 时未变化，被其他 scope 关联进来），
                // 降级为 value= 格式
                val nowValue = extractValue(state.toString())
                append("value=$nowValue")
            }
            append(")")
            if (!readers.isNullOrEmpty()) {
                append(", readers: ")
                append(readers.joinToString(", "))
            }
        }
    }

    /**
     * 清除所有注册数据。在 profiler reset 时调用。
     */
    fun clear() {
        identityToPrevValue.clear()
        identityToReaders.clear()
    }

    /**
     * 从 State 的 toString() 中提取 value 部分。
     * 例如 "MutableState(value=3)@128220496" → "3"
     */
    private fun extractValue(stateStr: String): String? {
        val valueStart = stateStr.indexOf("(value=")
        if (valueStart < 0) return null
        val start = valueStart + "(value=".length
        val end = stateStr.indexOf(')', start)
        if (end < 0) return null
        return stateStr.substring(start, end)
    }
}
