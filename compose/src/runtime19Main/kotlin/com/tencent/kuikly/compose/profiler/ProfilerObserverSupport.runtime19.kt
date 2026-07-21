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
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.ObservableComposition
import androidx.compose.runtime.tooling.setObserver

/** runtime19Main: create the real ProfilerCompositionObserver. */
internal actual fun createProfilerObserver(tracker: RecompositionTracker): ProfilerObserverFacade =
    ProfilerCompositionObserver(tracker)

/**
 * Attaches [observer] as a [CompositionObserver] to this [Composition] (cast to
 * [ObservableComposition]) and returns an opaque [KuiklyObserverHandle] for later disposal.
 *
 * Returns null if the composition does not implement [ObservableComposition] or [observer]
 * is not a [CompositionObserver].
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
internal actual fun Composition.kuiklySetObserver(observer: ProfilerObserverFacade): KuiklyObserverHandle? {
    val observable = this as? ObservableComposition ?: return null
    val compObserver = observer as? CompositionObserver ?: return null
    val handle = observable.setObserver(compObserver)
    return KuiklyObserverHandle { handle.dispose() }
}
