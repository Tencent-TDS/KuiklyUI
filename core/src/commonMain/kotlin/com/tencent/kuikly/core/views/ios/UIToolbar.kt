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

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.directives.BindDirectivesView
import com.tencent.kuikly.core.directives.ConditionView
import com.tencent.kuikly.core.directives.LoopDirectivesView

/**
 * iOS 底部 `UIToolbar`（仅 iOS；其它平台 no-op）。
 *
 * ```
 * UIToolbar {
 *     attr { tintColor(...) }
 *     items {
 *         toolbarItem { attr { symbol("trash"); index(0) }; event { click { } } }
 *         toolbarItemContent { … }
 *     }
 * }
 * ```
 *
 * **每页至多一个** `UIToolbar`；多个会重复栏级逻辑并共用同一 `toolbarItems`。
 * `items { vif { toolbarItem } }` 使用同包 [toolbarItem] / [toolbarItemContent] 扩展。
 */
fun ViewContainer<*, *>.UIToolbar(init: UIToolbarScope.() -> Unit) {
    if (!uiKitChromeSupported()) return
    val scope = UIToolbarScope().apply(init)
    installUIToolbar(scope)
}

private fun ViewContainer<*, *>.installUIToolbar(scope: UIToolbarScope) {
    addChild(UIToolbarView()) {
        scope.barInit?.invoke(this)
    }
    scope.itemsInit?.let { itemsInit ->
        addChild(UIToolbarItemsHostView(), itemsInit)
    }
}

class UIToolbarScope {
    internal var barInit: (UIToolbarView.() -> Unit)? = null
    internal var itemsInit: (UIToolbarItemsHostView.() -> Unit)? = null

    /** 栏级：显隐、tint、leading/trailing inset。 */
    fun attr(init: UIToolbarAttr.() -> Unit) {
        val previous = barInit
        barInit = {
            previous?.invoke(this)
            attr(init)
        }
    }

    fun items(init: UIToolbarItemsHostView.() -> Unit) {
        itemsInit = init
    }
}

open class UIToolbarView : DeclarativeBaseView<UIToolbarAttr, UIToolbarEvent>() {
    override fun createAttr() = UIToolbarAttr()
    override fun createEvent() = UIToolbarEvent()
    override fun viewName() = ViewConst.TYPE_IOS_UI_TOOLBAR

    override fun willInit() {
        super.willInit()
        attr {
            absolutePosition(0f, 0f)
            size(0f, 0f)
        }
    }
}

open class UIToolbarAttr : Attr() {
    fun tintColor(color: Color): UIToolbarAttr {
        "tintColor" with color.toString()
        return this
    }

    override fun visibility(visibility: Boolean): UIToolbarAttr {
        super.visibility(visibility)
        return this
    }

    fun leadingInset(inset: Float): UIToolbarAttr {
        "leadingInset" with inset.coerceAtLeast(0f).toString()
        return this
    }

    fun trailingInset(inset: Float): UIToolbarAttr {
        "trailingInset" with inset.coerceAtLeast(0f).toString()
        return this
    }
}

open class UIToolbarEvent : Event()

open class UIToolbarItemsHostAttr : ContainerAttr()

/**
 * `UIToolbar { items { … } }` 按钮区；[insertDomSubView] 幂等，配合 `vif` 整段切换。
 */
open class UIToolbarItemsHostView :
    ViewContainer<UIToolbarItemsHostAttr, Event>() {
    override fun createAttr(): UIToolbarItemsHostAttr = UIToolbarItemsHostAttr()
    override fun createEvent(): Event = Event()
    override fun viewName() = ViewConst.TYPE_VIEW

    override fun willInit() {
        super.willInit()
        attr {
            absolutePosition(0f, 0f)
            size(0f, 0f)
        }
    }

    override fun insertDomSubView(subView: DeclarativeBaseView<*, *>, index: Int) {
        if (subView.flexNode.parent === flexNode) {
            return
        }
        super.insertDomSubView(subView, index)
    }

    fun toolbarItem(init: UIToolbarItemView.() -> Unit) {
        addChild(UIToolbarItemView(), init)
    }

    fun toolbarItemContent(init: UIToolbarItemContentCreator) {
        addChild(UIToolbarItemContentView(), init)
    }
}

/** `items { vif { toolbarItem { } } }` 等指令分支内使用。 */
fun ConditionView.toolbarItem(init: UIToolbarItemView.() -> Unit) {
    addChild(UIToolbarItemView(), init)
}

fun ConditionView.toolbarItemContent(init: UIToolbarItemContentCreator) {
    addChild(UIToolbarItemContentView(), init)
}

fun <T> LoopDirectivesView<T>.toolbarItem(init: UIToolbarItemView.() -> Unit) {
    addChild(UIToolbarItemView(), init)
}

fun <T> LoopDirectivesView<T>.toolbarItemContent(init: UIToolbarItemContentCreator) {
    addChild(UIToolbarItemContentView(), init)
}

fun BindDirectivesView.toolbarItem(init: UIToolbarItemView.() -> Unit) {
    addChild(UIToolbarItemView(), init)
}

fun BindDirectivesView.toolbarItemContent(init: UIToolbarItemContentCreator) {
    addChild(UIToolbarItemContentView(), init)
}
