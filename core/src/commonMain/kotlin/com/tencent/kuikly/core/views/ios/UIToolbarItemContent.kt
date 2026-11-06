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

package com.tencent.kuikly.core.views.ios

import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event

/** 自定义 `UIBarButtonItem.customView` 区域（系统底栏）。 */
typealias UIToolbarItemContentCreator = UIToolbarItemContentView.() -> Unit

internal fun ViewContainer<*, *>.UIToolbarItemContent(init: UIToolbarItemContentView.() -> Unit) {
    if (!uiKitChromeSupported()) return
    addChild(UIToolbarItemContentView(), init)
}

open class UIToolbarItemContentAttr : ContainerAttr() {
    fun title(title: String): UIToolbarItemContentAttr {
        "title" with title
        return this
    }

    fun index(index: Int): UIToolbarItemContentAttr {
        "index" with index.toString()
        return this
    }

    override fun visibility(visibility: Boolean): UIToolbarItemContentAttr {
        super.visibility(visibility)
        return this
    }

    fun axis(axis: UIBarAxis): UIToolbarItemContentAttr {
        "axis" with axis.value
        return this
    }
}

open class UIToolbarItemContentView :
    ViewContainer<UIToolbarItemContentAttr, UIToolbarItemContentEvent>() {
    override fun createAttr(): UIToolbarItemContentAttr = UIToolbarItemContentAttr()
    override fun createEvent(): UIToolbarItemContentEvent = UIToolbarItemContentEvent()
    override fun viewName() = ViewConst.TYPE_IOS_UI_TOOLBAR_ITEM_CONTENT

    override fun willInit() {
        super.willInit()
        attr {
            absolutePosition(0f, 0f)
        }
    }

    override fun willRemoveFromParentView() {
        detachUIToolbarNativeHost()
        super.willRemoveFromParentView()
    }
}

open class UIToolbarItemContentEvent : Event()
