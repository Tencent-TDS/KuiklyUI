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

package com.tencent.kuikly.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ReusableComposition
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.core.base.DeclarativeBaseView

/**
 * A callback that controls whether a paused composition should pause.
 * Equivalent to [androidx.compose.runtime.ShouldPauseCallback] in runtime 1.9+.
 */
internal typealias PrefetchShouldPause = () -> Boolean

/**
 * Platform-agnostic handle for a paused composition started via PausableComposition
 * (available in Compose runtime 1.9+).
 *
 * On legacy runtimes (< 1.9), [beginPausableContent] always returns null and this
 * interface is never instantiated.
 */
internal interface PausedCompositionHandle {
    val isComplete: Boolean
    fun resume(shouldPause: PrefetchShouldPause): Boolean
    fun apply()
    fun cancel()
}

// ---------------------------------------------------------------------------
// The three functions below use expect/actual so that:
//   - runtime19Main  provides the real PausableComposition wiring (Kotlin 2.1.21+)
//   - runtimeLegacyMain provides no-op stubs (Kotlin 1.9.22 / compose plugin 1.7.3)
//
// Exactly ONE of the two source sets is mounted per build via the Gradle
// source set hierarchy (see build.2.1.21.gradle.kts / build.1.9.22.gradle.kts).
// ---------------------------------------------------------------------------

/** Returns true when the compose runtime supports PausableComposition (1.9+). */
internal expect fun supportsPausableComposition(): Boolean

/**
 * Creates a pausable sub-composition (runtime 1.9+) or a plain [ReusableComposition]
 * on legacy runtimes.
 */
internal expect fun createPausableSubcompositionCompat(
    container: KNode<DeclarativeBaseView<*, *>>,
    parent: CompositionContext,
): ReusableComposition

/**
 * Starts pausable content in [composition].
 * Returns a [PausedCompositionHandle] on 1.9+ runtimes, or null on legacy runtimes
 * (caller must fall back to [ReusableComposition.setContent]/[ReusableComposition.setContentWithReuse]).
 */
internal expect fun beginPausableContent(
    composition: ReusableComposition,
    content: @Composable () -> Unit,
    forceReuse: Boolean,
): PausedCompositionHandle?
