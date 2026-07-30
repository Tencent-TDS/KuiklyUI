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

package com.tencent.kuikly.compose

import androidx.compose.runtime.AbstractApplier
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.core.base.DeclarativeBaseView

internal class KuiklyApplier(
    val node: KNode<DeclarativeBaseView<*, *>>,
    private val onEndChanges: () -> Unit = {},
    ) : AbstractApplier<KNode<DeclarativeBaseView<*, *>>>(node) {

    override fun onEndChanges() {
        super.onEndChanges()
        root.owner?.onEndApplyChanges()
        onEndChanges.invoke()
    }

    override fun insertBottomUp(index: Int, instance: KNode<DeclarativeBaseView<*, *>>) {
        current.insertAt(index, instance)
    }

    override fun insertTopDown(index: Int, instance: KNode<DeclarativeBaseView<*, *>>) {
        current.insertTopDown(index, instance)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        current.removeAll()
    }

    override fun remove(index: Int, count: Int) {
        current.removeAt(index, count)
    }
}


