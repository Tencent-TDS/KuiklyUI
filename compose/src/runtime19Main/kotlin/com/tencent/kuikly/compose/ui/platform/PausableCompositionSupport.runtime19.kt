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
import androidx.compose.runtime.PausableComposition
import androidx.compose.runtime.ReusableComposition
import com.tencent.kuikly.compose.KuiklyApplier
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.core.base.DeclarativeBaseView

/** runtime19Main: PausableComposition is available in this runtime. */
internal actual fun supportsPausableComposition(): Boolean = true

/**
 * Creates a [PausableComposition] (which is also a [ReusableComposition]) for the given
 * [container] node. Used when prefetch wants pausable sub-composition.
 */
internal actual fun createPausableSubcompositionCompat(
    container: KNode<DeclarativeBaseView<*, *>>,
    parent: CompositionContext,
): ReusableComposition =
    PausableComposition(
        KuiklyApplier(container) { },
        parent,
    )

/**
 * Starts pausable content in [composition] (which must be a [PausableComposition]).
 * Returns a [PausedCompositionHandle] wrapping the underlying [androidx.compose.runtime.PausedComposition].
 */
internal actual fun beginPausableContent(
    composition: ReusableComposition,
    content: @Composable () -> Unit,
    forceReuse: Boolean,
): PausedCompositionHandle? {
    composition as PausableComposition
    val paused = if (forceReuse) {
        composition.setPausableContentWithReuse(content)
    } else {
        composition.setPausableContent(content)
    }
    return object : PausedCompositionHandle {
        override val isComplete: Boolean get() = paused.isComplete
        override fun resume(shouldPause: PrefetchShouldPause): Boolean = paused.resume(shouldPause)
        override fun apply() = paused.apply()
        override fun cancel() = paused.cancel()
    }
}
