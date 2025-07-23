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

package com.tencent.kuikly.compose.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalMap
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.Updater
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import com.tencent.kuikly.compose.KuiklyApplier
import com.tencent.kuikly.compose.foundation.layout.DefaultColumnMeasurePolicy
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.UiComposable
import com.tencent.kuikly.compose.ui.layout.MeasurePolicy
import com.tencent.kuikly.compose.ui.materialize
import com.tencent.kuikly.compose.ui.node.ComposeUiNode
import com.tencent.kuikly.compose.ui.node.ComposeUiNode.Companion.SetCompositeKeyHash
import com.tencent.kuikly.compose.ui.node.ComposeUiNode.Companion.SetMeasurePolicy
import com.tencent.kuikly.compose.ui.node.ComposeUiNode.Companion.SetModifier
import com.tencent.kuikly.compose.ui.node.ComposeUiNode.Companion.SetResolvedCompositionLocals
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.core.base.DeclarativeBaseView

// 默认布局成最小的
val KuiklyDefaultMeasurePolicy = MeasurePolicy { _, constraints ->
    layout(constraints.maxWidth, constraints.maxHeight) {
    }
}

/**
 * Updates the parameters of the [LayoutNode] in the current [Updater] with the given values.
 * @see [AndroidView.android.kt:updateViewHolderParams]
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun <T : DeclarativeBaseView<*, *>> Updater<ComposeUiNode>.updateParameters(
    compositionLocalMap: CompositionLocalMap,
    modifier: Modifier,
    compositeKeyHash: Int,
    measurePolicy: MeasurePolicy,
) {
    set(compositionLocalMap, SetResolvedCompositionLocals)
    set(modifier, SetModifier)
    set(compositeKeyHash, SetCompositeKeyHash)
    set(measurePolicy, SetMeasurePolicy)
}

@Composable
@UiComposable
fun <T : DeclarativeBaseView<*, *>> MakeKuiklyComposeNode(
    factory: () -> T,
    modifier: Modifier,
    viewInit: T.() -> Unit = {},
    viewUpdate: (T) -> Unit = {},
    measurePolicy: MeasurePolicy = KuiklyDefaultMeasurePolicy
) {
    val compositeKeyHash = currentCompositeKeyHash
    val materializedModifier = currentComposer.materialize(modifier)
    val compositionLocalMap = currentComposer.currentCompositionLocalMap

    ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
        factory = {
            val view = factory.invoke()
            KNode(view, init = viewInit)
        },
        update = {
            updateParameters<T>(
                compositionLocalMap,
                materializedModifier,
                compositeKeyHash,
                measurePolicy
            )
            set(viewUpdate) {
                (this as? KNode<T>)?.run {
                    this.update = {
                        viewUpdate(view)
                    }
                }
            }
        }
    )
}

@Composable
@UiComposable
fun <T : DeclarativeBaseView<*, *>> MakeKuiklyComposeNode(
    factory: () -> T,
    modifier: Modifier,
    content: @Composable () -> Unit,
    viewInit: T.() -> Unit = {},
    viewUpdate: (T) -> Unit = {},
    measurePolicy: MeasurePolicy = DefaultColumnMeasurePolicy
) {
    val compositeKeyHash = currentCompositeKeyHash
    val materializedModifier = currentComposer.materialize(modifier)
    val compositionLocalMap = currentComposer.currentCompositionLocalMap

    ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
        factory = {
            val view = factory.invoke()
            KNode(view, init = viewInit)
        },
        update = {
            updateParameters<T>(
                compositionLocalMap,
                materializedModifier,
                compositeKeyHash,
                measurePolicy
            )
            set(viewUpdate) {
                (this as? KNode<T>)?.run {
                    this.update = {
                        viewUpdate(view)
                    }
                }
            }
        },
        content = content
    )
}