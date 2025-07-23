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

package com.tencent.kuikly.core.base

import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.collection.fastArrayListOf

/**
 * 虚拟标签节点, 一般用于做逻辑节点使用，不参与实际Dom渲染
 */

abstract class VirtualView<A : ContainerAttr, E : Event> :
    ViewContainer<A, E>() {

    val realParent: ViewContainer<*, *>?
        get() {
            var currentParent = parent
            while (isVirtualComponent(currentParent)) {
                currentParent = currentParent?.parent
            }
            return currentParent as? ViewContainer<*, *>
        }

    override fun viewName(): String {
        return ViewConst.TYPE_VIEW
    }

    override fun isRenderView(): Boolean {
        return false
    }

    private fun isVirtualComponent(view: DeclarativeBaseView<*, *>?): Boolean {
        if (view != null
            && view.isVirtualView()
        ) {
            return true
        }
        return false
    }

}

/**
 * 是否为虚拟标签节点
 */
fun DeclarativeBaseView<*, *>.isVirtualView(): Boolean {
    return this is VirtualView<*, *>
}

fun ViewContainer<*, *>.domChildren(): List<DeclarativeBaseView<*, *>> {
    val domChildren = fastArrayListOf<DeclarativeBaseView<*, *>>()
    forEachChild { child ->
        if (child.isVirtualView()) {
            domChildren.addAll((child as ViewContainer).domChildren())
        } else {
            domChildren.add(child)
        }
    }
    return domChildren
}
