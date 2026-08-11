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

/** runtimeLegacyMain: PausableComposition is NOT available in compose plugin 1.7.3. */
internal actual fun supportsPausableComposition(): Boolean = false

/**
 * Falls back to a plain [ReusableComposition] since [PausableComposition] is unavailable
 * in compose plugin 1.7.3 (Kotlin 1.9.22 build line).
 */
internal actual fun createPausableSubcompositionCompat(
    container: KNode<DeclarativeBaseView<*, *>>,
    parent: CompositionContext,
): ReusableComposition = createSubcomposition(container, parent)

/**
 * Returns null because PausableComposition is unavailable.
 * The caller must fall back to [ReusableComposition.setContent] / [ReusableComposition.setContentWithReuse].
 */
@Suppress("UNUSED_PARAMETER")
internal actual fun beginPausableContent(
    composition: ReusableComposition,
    content: @Composable () -> Unit,
    forceReuse: Boolean,
): PausedCompositionHandle? = null
