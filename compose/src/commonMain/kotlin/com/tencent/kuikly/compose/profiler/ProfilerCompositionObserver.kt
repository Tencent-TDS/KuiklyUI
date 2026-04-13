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

import androidx.compose.runtime.Composition
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.RecomposeScopeObserver
import androidx.compose.runtime.tooling.observe

/**
 * CompositionObserver implementation for precise recomposition reason tracking.
 *
 * Leverages [CompositionObserver.onBeginComposition]'s `invalidationMap` to determine
 * exactly which State objects triggered each RecomposeScope's invalidation.
 * Combined with [RecomposeScopeObserver] to maintain an active scope stack,
 * this allows [RecompositionTracker] to associate precise trigger states
 * with each Composable (via the CompositionTracer bridge).
 *
 * Data flow:
 * 1. `onBeginComposition(invalidationMap)` → save scope→states mapping
 * 2. `RecomposeScopeObserver.onBeginScopeComposition(scope)` → push to active stack
 * 3. `CompositionTracer.traceEventStart(key, info)` → tracker records composable start
 * 4. `CompositionTracer.traceEventEnd()` → tracker queries [getCurrentScopeTriggerStates]
 * 5. `RecomposeScopeObserver.onEndScopeComposition(scope)` → pop from active stack
 * 6. `onEndComposition()` → cleanup
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
internal class ProfilerCompositionObserver(
    private val tracker: RecompositionTracker
) : CompositionObserver {

    /**
     * Current frame's precise scope → trigger states mapping.
     * Populated by [onBeginComposition], cleared by [onEndComposition].
     */
    private val scopeToStatesMap = mutableMapOf<RecomposeScope, Set<Any>?>()

    /**
     * Active scope stack. Maintained by [RecomposeScopeObserver] callbacks.
     * The top of the stack is the currently executing scope.
     */
    private val activeScopeStack = mutableListOf<RecomposeScope>()

    /**
     * Handles for scope observers registered in the current composition pass.
     * Disposed at the end of composition to avoid leaks.
     */
    private val scopeObserverHandles = mutableListOf<CompositionObserverHandle>()

    /**
     * Whether precise scope→state mapping is available for the current composition pass.
     */
    internal var hasPreciseMapping: Boolean = false
        private set

    override fun onBeginComposition(
        composition: Composition,
        invalidationMap: Map<RecomposeScope, Set<Any>?>
    ) {
        // Clean up previous scope observer handles
        for (handle in scopeObserverHandles) {
            handle.dispose()
        }
        scopeObserverHandles.clear()

        // Save precise scope → states mapping
        scopeToStatesMap.clear()
        scopeToStatesMap.putAll(invalidationMap)

        activeScopeStack.clear()
        hasPreciseMapping = true

        // Register RecomposeScopeObserver for each invalidated scope
        // This is necessary so that onBeginScopeComposition/onEndScopeComposition
        // are called by the runtime when each scope's compose lambda executes.
        val scopeObserver = ScopeObserver()
        for ((scope, _) in invalidationMap) {
            val handle = scope.observe(scopeObserver)
            scopeObserverHandles.add(handle)
        }

        // Notify tracker
        tracker.onCompositionObserverBegin()
    }

    override fun onEndComposition(composition: Composition) {
        // Notify tracker
        tracker.onCompositionObserverEnd()

        // Dispose scope observer handles
        for (handle in scopeObserverHandles) {
            handle.dispose()
        }
        scopeObserverHandles.clear()

        // Cleanup frame-level data
        activeScopeStack.clear()
        scopeToStatesMap.clear()
        hasPreciseMapping = false
    }

    /**
     * Get the currently active scope's identity hash code.
     * Returns null if no active scope (initial composition).
     */
    fun getCurrentScopeKey(): Int? = activeScopeStack.lastOrNull()?.hashCode()

    /**
     * Get the precise trigger states for the currently active scope (top of stack).
     *
     * **语义说明：**
     * 返回的 State 列表含义是"该 scope 依赖的 State 中，本次 Snapshot.apply 批次里发生了值变化的那些"，
     * 即 `invalidationMap[scope]` 的内容。
     *
     * 底层来源（Composition.kt `invalidateChecked`）：每当某个 State 变化时，Runtime 调用
     * `invalidations.add(scope, instance)` 将该 State 记录为触发该 scope 失效的原因。
     * `invalidationMap[scope]` = 本次 apply 批次中，所有「变化了」且「被该 scope 读取」的 State。
     *
     * 常见的"多 State 出现"原因：
     * - 同一个事件 lambda 中同时修改了多个 State（如 `clickCount++; userName = ...`），
     *   它们在同一次 Snapshot.apply 中被 commit，该 scope 依赖的所有这些 State 都会出现；
     * - 子组件 scope 也可能出现在 invalidationMap 中，携带父 scope 的 invalidation 原因。
     *
     * 这是 Compose Runtime API 的设计限制，无法从 `invalidationMap` 进一步细分
     * "是哪个 State 才是真正触发这次重组的那一个"。
     * 如需精确到参数级别，应结合 `paramChanges`（编译器 `$dirty` bitmask）判断。
     *
     * @return List of state identifiers that triggered the current scope's recomposition,
     *         or null if no active scope / scope not in invalidationMap (e.g., initial composition).
     */
    fun getCurrentScopeTriggerStates(): List<String>? {
        val currentScope = activeScopeStack.lastOrNull() ?: return null
        val states = scopeToStatesMap[currentScope]
        // states == null means forced recomposition (not in invalidationMap at all means initial)
        return if (states != null) {
            states.map { stateToString(it) }
        } else if (scopeToStatesMap.containsKey(currentScope)) {
            // Key exists but value is null → forced recomposition
            listOf("[forced recomposition]")
        } else {
            // Key doesn't exist → initial composition or child scope, no trigger info
            null
        }
    }

    /**
     * Get the raw trigger State objects for the currently active scope.
     * Used by [RecompositionTracker] to register reader mappings with
     * the human-readable Composable name from CompositionTracer info.
     *
     * @return Set of State objects that triggered the current scope, or null if unavailable.
     */
    fun getCurrentScopeTriggerStateObjects(): Set<Any>? {
        val currentScope = activeScopeStack.lastOrNull() ?: return null
        return scopeToStatesMap[currentScope]
    }

    /**
     * Convert a State object to a human-readable string identifier.
     * Uses [StateIdentityRegistry] to produce "State(prev=x, now=y), readers: Name" format.
     */
    private fun stateToString(state: Any): String {
        return tracker.stateIdentityRegistry.formatState(state)
    }

    /**
     * Inner RecomposeScopeObserver that tracks scope enter/exit for stack maintenance.
     */
    private inner class ScopeObserver : RecomposeScopeObserver {

        override fun onBeginScopeComposition(scope: RecomposeScope) {
            activeScopeStack.add(scope)
        }

        override fun onEndScopeComposition(scope: RecomposeScope) {
            // Remove from stack (should be the last element, but handle edge cases)
            val idx = activeScopeStack.lastIndexOf(scope)
            if (idx >= 0) {
                activeScopeStack.removeAt(idx)
            }
        }

        override fun onScopeDisposed(scope: RecomposeScope) {
            scopeToStatesMap.remove(scope)
        }
    }
}
