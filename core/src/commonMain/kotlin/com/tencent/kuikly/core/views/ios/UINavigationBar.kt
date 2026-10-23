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
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.directives.BindDirectivesView
import com.tencent.kuikly.core.directives.ConditionView
import com.tencent.kuikly.core.directives.LoopDirectivesView
import com.tencent.kuikly.core.exception.throwRuntimeError

/**
 * 一页导航的**统一 DSL**：`attr`（栏级）+ 可选 `title` + 可选 `items`（**仅 iOS** 生效；其它平台 no-op）。
 *
 * - **attr**：`UINavigationBar` 级配置（显隐、tint、inset、滚动联动等）。与 [UIToolbar] 的 `attr` 对称；
 *   依赖 observable 的字段在 `attr { tintColor(ctx.xxx) }` 中读取即可，页面 `body` 重绘时会同步到 Native。
 * - **title**：中间标题区。`text { attr { … } }` / `titleContent { … }`。
 * - **items**：`navbarItems { navbarItem / navbarItemContent }`；多套按钮用 `vif`/`velse` **整段**换 `navbarItems`。
 *
 * `title` / `items` 下 `vif` 使用 [text]、[titleContent]、[navbarItems] 指令扩展（勿在无关 `vif` import）。
 *
 * Kotlin 树上 chrome 与内容为兄弟节点；DSL 嵌套仅表达归属。每页至多一个 [UINavigationBar]。
 */
fun ViewContainer<*, *>.UINavigationBar(init: UINavigationBarScope.() -> Unit) {
    if (!uiKitChromeSupported()) return
    val scope = UINavigationBarScope().apply(init)
    installUINavigationBar(scope)
}

private fun ViewContainer<*, *>.installUINavigationBar(scope: UINavigationBarScope) {
    lateinit var navigationBar: UINavigationBarView
    mountUINavigationBar {
        navigationBar = this
        scope.barInit?.invoke(this)
    }
    val bar = navigationBar
    scope.titleCreator?.let { titleInit ->
        addChild(UINavigationBarTitleRegionView(bar)) {
            titleInit()
        }
    }
    scope.itemsChromeCreator?.let { chromeInit ->
        addChild(UINavigationBarItemsChromeView(), chromeInit)
    }
}

/**
 * `items { vif { navigationItems } }` 外壳。
 * [insertDomSubView] 对已在树中的子节点幂等，避免 `vif` 同步 DOM 时 `duplicate createFlexNode`。
 */
open class UINavigationBarItemsChromeAttr : ContainerAttr()

open class UINavigationBarItemsChromeView :
    ViewContainer<UINavigationBarItemsChromeAttr, Event>() {
    override fun createAttr(): UINavigationBarItemsChromeAttr = UINavigationBarItemsChromeAttr()
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

    /** 仅在 [UINavigationBarScope.items] 内使用。 */
    fun navbarItems(init: UINavigationBarItemsHostView.() -> Unit) {
        addChild(UINavigationBarItemsHostView(), init)
    }
}

open class UINavigationBarItemsHostAttr : ContainerAttr()

open class UINavigationBarItemsHostView :
    ViewContainer<UINavigationBarItemsHostAttr, Event>() {
    override fun createAttr(): UINavigationBarItemsHostAttr = UINavigationBarItemsHostAttr()
    override fun createEvent(): Event = Event()
    override fun viewName() = ViewConst.TYPE_VIEW

    override fun willInit() {
        super.willInit()
        attr {
            absolutePosition(0f, 0f)
            size(0f, 0f)
        }
    }

    fun navbarItem(init: UIBarButtonItemView.() -> Unit) {
        if (!uiKitChromeSupported()) return
        addChild(UIBarButtonItemView(), init)
    }

    fun navbarItemContent(init: UIBarButtonItemContentCreator) {
        if (!uiKitChromeSupported()) return
        addChild(UIBarButtonItemContentView(), init)
    }
}

/** `items { vif { navbarItems { } } }` 内使用。 */
fun ConditionView.navbarItems(init: UINavigationBarItemsHostView.() -> Unit) {
    addChild(UINavigationBarItemsHostView(), init)
}

fun <T> LoopDirectivesView<T>.navbarItems(init: UINavigationBarItemsHostView.() -> Unit) {
    addChild(UINavigationBarItemsHostView(), init)
}

fun BindDirectivesView.navbarItems(init: UINavigationBarItemsHostView.() -> Unit) {
    addChild(UINavigationBarItemsHostView(), init)
}

/** `title { vif { text { attr { … } } } }` 内使用。 */
fun ConditionView.text(init: UINavigationBarTitleTextScope.() -> Unit) {
    mountUINavigationBarTitleText(uiNavigationBarTitleBar(), init)
}

fun ConditionView.titleContent(init: UINavigationBarTitleCreator) {
    if (!uiKitChromeSupported()) return
    addChild(UINavigationBarTitleView(), init)
}

fun <T> LoopDirectivesView<T>.text(init: UINavigationBarTitleTextScope.() -> Unit) {
    mountUINavigationBarTitleText(uiNavigationBarTitleBar(), init)
}

fun <T> LoopDirectivesView<T>.titleContent(init: UINavigationBarTitleCreator) {
    if (!uiKitChromeSupported()) return
    addChild(UINavigationBarTitleView(), init)
}

fun BindDirectivesView.text(init: UINavigationBarTitleTextScope.() -> Unit) {
    mountUINavigationBarTitleText(uiNavigationBarTitleBar(), init)
}

fun BindDirectivesView.titleContent(init: UINavigationBarTitleCreator) {
    if (!uiKitChromeSupported()) return
    addChild(UINavigationBarTitleView(), init)
}

open class UINavigationBarTitleRegionAttr : ContainerAttr()

/**
 * `title { vif { text } velse { titleContent } }` 区域。
 * [insertDomSubView] 幂等，配合 `vif` 切换 text / titleContent。
 */
open class UINavigationBarTitleRegionView internal constructor(
    internal val navigationBar: UINavigationBarView
) : ViewContainer<UINavigationBarTitleRegionAttr, Event>() {
    override fun createAttr(): UINavigationBarTitleRegionAttr = UINavigationBarTitleRegionAttr()
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

    fun text(init: UINavigationBarTitleTextScope.() -> Unit) {
        mountUINavigationBarTitleText(navigationBar, init)
    }

    fun titleContent(init: UINavigationBarTitleCreator) {
        if (!uiKitChromeSupported()) return
        addChild(UINavigationBarTitleView(), init)
    }
}

typealias UINavigationBarTitleScope = UINavigationBarTitleRegionView

/** `title { vif { text } }` 内查找关联的 [UINavigationBarView]。 */
internal fun DeclarativeBaseView<*, *>.uiNavigationBarTitleBar(): UINavigationBarView {
    var current: DeclarativeBaseView<*, *>? = this
    while (current != null) {
        if (current is UINavigationBarTitleRegionView) {
            return current.navigationBar
        }
        current = current.parent
    }
    throwRuntimeError("title { text / titleContent } 须写在 UINavigationBar { title { } } 内")
    error("unreachable")
}

class UINavigationBarScope {
    internal var barInit: (UINavigationBarView.() -> Unit)? = null
    internal var titleCreator: (UINavigationBarTitleRegionView.() -> Unit)? = null
    internal var itemsChromeCreator: (UINavigationBarItemsChromeView.() -> Unit)? = null

    /** 导航栏级属性（与 [UIToolbarScope.attr] 对称）。 */
    fun attr(init: UINavigationBarAttr.() -> Unit) {
        val previous = barInit
        barInit = {
            previous?.invoke(this)
            attr(init)
        }
    }

    fun title(init: UINavigationBarTitleRegionView.() -> Unit) {
        titleCreator = init
    }

    /** 导航按钮区：`vif`/`velse` + [navbarItems] 整段切换。 */
    fun items(init: UINavigationBarItemsChromeView.() -> Unit) {
        itemsChromeCreator = init
    }
}
