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

/** runtimeLegacyMain no-op: CompositionObserver / ObservableComposition are not available. */
internal actual fun createProfilerObserver(@Suppress("UNUSED_PARAMETER") tracker: RecompositionTracker): ProfilerObserverFacade =
    object : ProfilerObserverFacade {
        override val hasPreciseMapping: Boolean get() = false
        override fun getCurrentScopeKey(): Int? = null
        override fun getCurrentScopeTriggerStates(): List<String>? = null
        override fun getCurrentScopeTriggerStateObjects(): Set<Any>? = null
    }

/** runtimeLegacyMain no-op: returns null, observer registration is not possible. */
@Suppress("UNUSED_PARAMETER")
internal actual fun Composition.kuiklySetObserver(observer: ProfilerObserverFacade): KuiklyObserverHandle? = null
