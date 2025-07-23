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

import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.requireLayoutNode
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewRef

typealias RefFunc<V> = V.(viewRef: ViewRef<V>) -> Unit

fun Modifier.nativeRef(
    ref: RefFunc<DeclarativeBaseView<*, *>>? = null,
): Modifier = this.then(NativeRefElement(ref))

// region ------------------------------ 修饰符节点实现 ------------------------------
private class NativeRefElement(
    private val ref: RefFunc<DeclarativeBaseView<*, *>>?
) : ModifierNodeElement<NativeRefNode>() {

    // 创建节点实例
    override fun create() = NativeRefNode(ref)

    // 更新节点参数
    override fun update(node: NativeRefNode) {
        node.updateRef(ref)
    }

    // 标识相同性判断
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is NativeRefElement && ref == other.ref
    }

    // 哈希值生成规则
    override fun hashCode(): Int = ref?.hashCode() ?: 0
}

private class NativeRefNode(
    initialRef: RefFunc<DeclarativeBaseView<*, *>>?
) : Modifier.Node() {
    private var currentRef = initialRef

    // 动态更新回调引用
    fun updateRef(newRef: RefFunc<DeclarativeBaseView<*, *>>?) {
        if (currentRef != newRef) {
            currentRef = newRef
            dispatchNativeRef()
        }
    }

    // 分发原生视图引用
    private fun dispatchNativeRef() {
        val layoutNode = requireLayoutNode()
        val kNode = layoutNode as? KNode<*> ?: return
        val view = kNode.view as? DeclarativeBaseView<*, *> ?: return
        currentRef?.invoke(view, ViewRef(view.pagerId, view.nativeRef))
    }

    // 生命周期绑定
    override fun onAttach() {
        dispatchNativeRef() // 视图挂载时立即触发
    }
}
// endregion