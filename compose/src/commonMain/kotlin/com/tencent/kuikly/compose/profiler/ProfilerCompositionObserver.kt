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

import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.ObservableComposition

/**
 * CompositionObserver implementation for precise recomposition reason tracking.
 *
 * Runtime 1.9+ uses [CompositionObserver.onScopeInvalidated] and scope enter/exit callbacks
 * instead of the pre-1.9 invalidationMap + [RecomposeScopeObserver] stack.
 *
 * Data flow:
 * 1. `onScopeInvalidated` → accumulate scope→states mapping between compositions
 * 2. `onBeginComposition` → promote pending invalidations to [scopeToStatesMap]
 * 3. `onScopeEnter` / `onScopeExit` → maintain active scope stack
 * 4. `CompositionTracer.traceEventStart/End` → tracker queries [getCurrentScopeTriggerStates]
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
internal class ProfilerCompositionObserver(
    private val tracker: RecompositionTracker
) : CompositionObserver {

    /**
     * Current frame's precise scope → trigger states mapping.
     * Populated at [onBeginComposition], cleared at [onEndComposition].
     */
    private val scopeToStatesMap = mutableMapOf<RecomposeScope, Set<Any>?>()

    /**
     * Invalidations recorded between composition passes via [onScopeInvalidated].
     */
    private val pendingInvalidations = mutableMapOf<RecomposeScope, MutableSet<Any?>>()

    /**
     * Active scope stack. Maintained by [onScopeEnter] / [onScopeExit].
     */
    private val activeScopeStack = mutableListOf<RecomposeScope>()

    /**
     * Whether precise scope→state mapping is available for the current composition pass.
     */
    internal var hasPreciseMapping: Boolean = false
        private set

    override fun onBeginComposition(composition: ObservableComposition) {
        scopeToStatesMap.clear()
        for ((scope, values) in pendingInvalidations) {
            val nonNullValues = values.filterNotNull().toSet()
            scopeToStatesMap[scope] = when {
                values.contains(null) && nonNullValues.isEmpty() -> null
                nonNullValues.isEmpty() -> null
                else -> nonNullValues
            }
        }
        pendingInvalidations.clear()

        activeScopeStack.clear()
        hasPreciseMapping = scopeToStatesMap.isNotEmpty()

        tracker.onCompositionObserverBegin()
    }

    override fun onScopeEnter(scope: RecomposeScope) {
        activeScopeStack.add(scope)
    }

    override fun onReadInScope(scope: RecomposeScope, value: Any) {
        // Reads are tracked separately by Snapshot observers in RecompositionTracker.
    }

    override fun onScopeExit(scope: RecomposeScope) {
        val idx = activeScopeStack.lastIndexOf(scope)
        if (idx >= 0) {
            activeScopeStack.removeAt(idx)
        }
    }

    override fun onEndComposition(composition: ObservableComposition) {
        tracker.onCompositionObserverEnd()

        activeScopeStack.clear()
        scopeToStatesMap.clear()
        hasPreciseMapping = false
    }

    override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {
        pendingInvalidations.getOrPut(scope) { mutableSetOf() }.add(value)
    }

    override fun onScopeDisposed(scope: RecomposeScope) {
        scopeToStatesMap.remove(scope)
        pendingInvalidations.remove(scope)
    }

    /**
     * Get the currently active scope's identity hash code.
     * Returns null if no active scope (initial composition).
     */
    fun getCurrentScopeKey(): Int? = activeScopeStack.lastOrNull()?.hashCode()

    /**
     * Get the precise trigger states for the currently active scope (top of stack).
     */
    fun getCurrentScopeTriggerStates(): List<String>? {
        val currentScope = activeScopeStack.lastOrNull() ?: return null
        val states = scopeToStatesMap[currentScope]
        return if (states != null) {
            states.map { stateToString(it) }
        } else if (scopeToStatesMap.containsKey(currentScope)) {
            listOf("[forced recomposition]")
        } else {
            null
        }
    }

    /**
     * Get the raw trigger State objects for the currently active scope.
     */
    fun getCurrentScopeTriggerStateObjects(): Set<Any>? {
        val currentScope = activeScopeStack.lastOrNull() ?: return null
        return scopeToStatesMap[currentScope]
    }

    private fun stateToString(state: Any): String {
        return tracker.stateIdentityRegistry.formatState(state)
    }
}
