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

/**
 * Opaque handle returned by [Composition.kuiklySetObserver] (defined in source sets).
 * Calling [dispose] removes the observer registration.
 */
internal fun interface KuiklyObserverHandle {
    fun dispose()
}

/**
 * Common interface exposing the composition-scope query methods used by
 * [RecompositionTracker] for precise recomposition reason tracking.
 *
 * On Compose runtime 1.9+, this is implemented by [ProfilerCompositionObserver]
 * (in runtime19Main) which also implements [androidx.compose.runtime.tooling.CompositionObserver].
 *
 * On legacy runtimes (compose plugin 1.7.3 / Kotlin 1.9.22), a no-op implementation
 * is provided by runtimeLegacyMain so that [RecompositionTracker] compiles unchanged.
 */
internal interface ProfilerObserverFacade {
    /** Whether precise scope→state mapping is available for the current composition pass. */
    val hasPreciseMapping: Boolean

    /** Returns the identity hash code of the currently composing scope, or null. */
    fun getCurrentScopeKey(): Int?

    /** Returns the human-readable trigger state strings for the current scope, or null. */
    fun getCurrentScopeTriggerStates(): List<String>?

    /** Returns the raw trigger State objects for the current scope, or null. */
    fun getCurrentScopeTriggerStateObjects(): Set<Any>?
}

// ---------------------------------------------------------------------------
// The two functions below use expect/actual so that:
//   - runtime19Main  provides real CompositionObserver wiring
//   - runtimeLegacyMain provides no-op stubs
// ---------------------------------------------------------------------------

/** Creates the appropriate [ProfilerObserverFacade] for the current runtime. */
internal expect fun createProfilerObserver(tracker: RecompositionTracker): ProfilerObserverFacade

/**
 * Attaches [observer] as a CompositionObserver to this [Composition].
 * Returns null on legacy runtimes where the API is unavailable.
 */
internal expect fun Composition.kuiklySetObserver(observer: ProfilerObserverFacade): KuiklyObserverHandle?
