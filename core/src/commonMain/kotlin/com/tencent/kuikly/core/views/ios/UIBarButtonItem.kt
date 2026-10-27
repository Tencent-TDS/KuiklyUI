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
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.base.event.Event

/** 自定义 `UIBarButtonItem.customView` 区域。 */
typealias UIBarButtonItemContentCreator = UIBarButtonItemContentView.() -> Unit

internal fun ViewContainer<*, *>.UIBarButtonItem(init: UIBarButtonItemView.() -> Unit) {
    if (!uiKitChromeSupported()) return
    addChild(UIBarButtonItemView(), init)
}

open class UIBarButtonItemView : DeclarativeBaseView<UIBarButtonItemAttr, UIBarButtonItemEvent>() {
    override fun createAttr() = UIBarButtonItemAttr()
    override fun createEvent() = UIBarButtonItemEvent()
    override fun viewName() = ViewConst.TYPE_IOS_UI_NAVIGATION_ITEM

    override fun willInit() {
        super.willInit()
        attr {
            absolutePosition(0f, 0f)
            size(0f, 0f)
        }
    }

    override fun willRemoveFromParentView() {
        detachUINavigationChromeNativeHost()
        super.willRemoveFromParentView()
    }
}

open class UIBarButtonItemAttr : Attr() {
    fun placement(placement: UIBarButtonItemPlacement): UIBarButtonItemAttr {
        "placement" with placement.value
        return this
    }

    fun symbol(name: String): UIBarButtonItemAttr {
        "symbol" with name
        return this
    }

    fun src(src: String): UIBarButtonItemAttr {
        "src" with src
        return this
    }

    fun src(uri: ImageUri): UIBarButtonItemAttr {
        return src(uri.toUrl(getPager().pageName))
    }

    fun symbolRendering(rendering: UIBarButtonItemSymbolRendering): UIBarButtonItemAttr {
        "template" with rendering.value
        return this
    }

    fun template(template: Boolean): UIBarButtonItemAttr {
        return symbolRendering(
            if (template) {
                UIBarButtonItemSymbolRendering.Template
            } else {
                UIBarButtonItemSymbolRendering.Original
            }
        )
    }

    fun title(title: String): UIBarButtonItemAttr {
        "title" with title
        return this
    }

    fun tintColor(color: Color): UIBarButtonItemAttr {
        "tintColor" with color.toString()
        return this
    }

    fun index(index: Int): UIBarButtonItemAttr {
        "index" with index.toString()
        return this
    }

    fun itemGroup(itemGroup: Int): UIBarButtonItemAttr {
        "itemGroup" with itemGroup.toString()
        return this
    }

    override fun visibility(visibility: Boolean): UIBarButtonItemAttr {
        super.visibility(visibility)
        return this
    }

    fun axis(axis: UIBarAxis): UIBarButtonItemAttr {
        "axis" with axis.value
        return this
    }
}

open class UIBarButtonItemEvent : Event()

internal fun ViewContainer<*, *>.UIBarButtonItemContent(init: UIBarButtonItemContentView.() -> Unit) {
    if (!uiKitChromeSupported()) return
    addChild(UIBarButtonItemContentView(), init)
}

open class UIBarButtonItemContentAttr : ContainerAttr() {
    fun placement(placement: UIBarButtonItemPlacement): UIBarButtonItemContentAttr {
        "placement" with placement.value
        return this
    }

    fun title(title: String): UIBarButtonItemContentAttr {
        "title" with title
        return this
    }

    fun index(index: Int): UIBarButtonItemContentAttr {
        "index" with index.toString()
        return this
    }

    fun itemGroup(itemGroup: Int): UIBarButtonItemContentAttr {
        "itemGroup" with itemGroup.toString()
        return this
    }

    override fun visibility(visibility: Boolean): UIBarButtonItemContentAttr {
        super.visibility(visibility)
        return this
    }

    fun axis(axis: UIBarAxis): UIBarButtonItemContentAttr {
        "axis" with axis.value
        return this
    }

    fun edgeInsetTop(inset: Float): UIBarButtonItemContentAttr {
        "edgeInsetTop" with inset.toString()
        return this
    }

    fun edgeInsetTopAuto(): UIBarButtonItemContentAttr {
        "edgeInsetTop" with "auto"
        return this
    }
}

open class UIBarButtonItemContentView :
    ViewContainer<UIBarButtonItemContentAttr, UIBarButtonItemContentEvent>() {
    override fun createAttr(): UIBarButtonItemContentAttr = UIBarButtonItemContentAttr()
    override fun createEvent(): UIBarButtonItemContentEvent = UIBarButtonItemContentEvent()
    override fun viewName() = ViewConst.TYPE_IOS_UI_NAVIGATION_ITEM_CONTENT

    override fun willInit() {
        super.willInit()
        attr {
            absolutePosition(0f, 0f)
        }
    }

    override fun willRemoveFromParentView() {
        detachUINavigationChromeNativeHost()
        super.willRemoveFromParentView()
    }
}

open class UIBarButtonItemContentEvent : Event()

/** `vif` 等只拆 Kotlin 子树、未走 `removeDomSubView` 时，仍通知 Native 卸掉 UINavigationItem 上的按钮。 */
internal fun DeclarativeBaseView<*, *>.detachUINavigationChromeNativeHost() {
    if (renderView != null) {
        removeRenderView()
    }
}
