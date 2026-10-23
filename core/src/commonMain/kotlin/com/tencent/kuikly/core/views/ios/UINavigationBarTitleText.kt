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
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.views.FontWeight

/** `UINavigationBar { title { text { attr { … } } } }` 配置块。 */
class UINavigationBarTitleTextScope {
    internal var attrBlock: UINavigationBarTitleTextAttr.() -> Unit = {}
    fun attr(init: UINavigationBarTitleTextAttr.() -> Unit) {
        attrBlock = init
    }
}

/**
 * 系统字符串标题（`UINavigationItem.title` / titleTextAttributes）。
 * 属性合并到 [UINavigationBarView] 的 Native 节点；树上用 [UINavigationBarTitleTextSlotView] 管 `vif` 生命周期。
 */
open class UINavigationBarTitleTextAttr internal constructor(
    private val sink: Attr
) {
    private fun prop(key: String, value: Any): UINavigationBarTitleTextAttr {
        with(sink) {
            key with value
        }
        return this
    }

    fun title(title: String): UINavigationBarTitleTextAttr {
        return prop("title", title)
    }

    fun largeTitle(mode: UILargeTitleDisplayMode): UINavigationBarTitleTextAttr {
        return prop("largeTitle", mode.value)
    }

    fun titleFontFamily(family: String): UINavigationBarTitleTextAttr {
        return prop("titleFontFamily", family)
    }

    fun titleFontWeight(weight: String): UINavigationBarTitleTextAttr {
        return prop("titleFontWeight", weight)
    }

    fun titleFontWeight(weight: UINavigationBarTitleFontWeight): UINavigationBarTitleTextAttr {
        return titleFontWeight(weight.value)
    }

    fun titleFontWeight(weight: FontWeight): UINavigationBarTitleTextAttr {
        return titleFontWeight(weight.value)
    }

    fun titleFontSize(size: Float): UINavigationBarTitleTextAttr {
        return prop("titleFontSize", size.toString())
    }

    fun largeTitleFontSize(size: Float): UINavigationBarTitleTextAttr {
        return prop("largeTitleFontSize", size.toString())
    }

    fun titleColor(color: Color): UINavigationBarTitleTextAttr {
        return prop("titleColor", color.toString())
    }

    fun titleKern(kern: Float): UINavigationBarTitleTextAttr {
        return prop("titleKern", kern.toString())
    }

    fun titleShadow(
        color: Color,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        radius: Float = 0f
    ): UINavigationBarTitleTextAttr {
        prop("titleShadowColor", color.toString())
        prop("titleShadowOffsetX", offsetX.toString())
        prop("titleShadowOffsetY", offsetY.toString())
        prop("titleShadowRadius", radius.toString())
        return this
    }

    fun titleOffset(x: Float, y: Float): UINavigationBarTitleTextAttr {
        prop("titleOffsetX", x.toString())
        prop("titleOffsetY", y.toString())
        return this
    }

    fun subtitle(subtitle: String): UINavigationBarTitleTextAttr {
        return prop("subtitle", subtitle)
    }

    fun largeSubtitle(subtitle: String): UINavigationBarTitleTextAttr {
        return prop("largeSubtitle", subtitle)
    }
}

internal fun UINavigationBarAttr.applyTitleText(
    block: UINavigationBarTitleTextAttr.() -> Unit
) {
    UINavigationBarTitleTextAttr(this).block()
}

/** 仅框架内 [UINavigationBarTitleRegionView] / 指令扩展使用。 */
internal fun UINavigationBarView.titleText(init: UINavigationBarTitleTextScope.() -> Unit) {
    val scope = UINavigationBarTitleTextScope().apply(init)
    attr {
        applyTitleText(scope.attrBlock)
    }
}

/**
 * `title { text { } }` / `vif` 分支在树上的生命周期槽：挂载时把配置推到 [UINavigationBarView]，
 * 摘下时清空。无独立 Native 类型（对比 [UINavigationBarTitleView] / [UIBarButtonItemView]）。
 */
internal class UINavigationBarTitleTextSlotView(
    private val navigationBar: UINavigationBarView,
    private val titleTextInit: UINavigationBarTitleTextScope.() -> Unit
) : DeclarativeBaseView<Attr, Event>() {
    override fun createAttr(): Attr = Attr()
    override fun createEvent(): Event = Event()
    override fun viewName(): String = ViewConst.TYPE_VIEW

    override fun willInit() {
        super.willInit()
        attr {
            absolutePosition(0f, 0f)
            size(0f, 0f)
        }
        applyTitleTextToNavigationBar()
    }

    override fun willRemoveFromParentView() {
        navigationBar.attr {
            applyTitleText {
                title("")
                subtitle("")
                largeTitle(UILargeTitleDisplayMode.Never)
            }
        }
        super.willRemoveFromParentView()
    }

    private fun applyTitleTextToNavigationBar() {
        val scope = UINavigationBarTitleTextScope()
        titleTextInit.invoke(scope)
        navigationBar.attr {
            applyTitleText(scope.attrBlock)
        }
    }
}

internal fun ViewContainer<*, *>.mountUINavigationBarTitleText(
    navigationBar: UINavigationBarView,
    init: UINavigationBarTitleTextScope.() -> Unit
) {
    if (!uiKitChromeSupported()) return
    addChild(UINavigationBarTitleTextSlotView(navigationBar, init)) {}
}
