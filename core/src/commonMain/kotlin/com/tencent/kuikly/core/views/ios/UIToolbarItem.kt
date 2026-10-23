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
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.base.event.Event

enum class UIToolbarItemKind(val value: String) {
    Item("item"),
    FlexibleSpace("flexibleSpace"),
    FixedSpace("fixedSpace"),
}

internal fun ViewContainer<*, *>.UIToolbarItem(init: UIToolbarItemView.() -> Unit) {
    if (!uiKitChromeSupported()) return
    addChild(UIToolbarItemView(), init)
}

open class UIToolbarItemView : DeclarativeBaseView<UIToolbarItemAttr, UIToolbarItemEvent>() {
    override fun createAttr() = UIToolbarItemAttr()
    override fun createEvent() = UIToolbarItemEvent()
    override fun viewName() = ViewConst.TYPE_IOS_UI_TOOLBAR_ITEM

    override fun willInit() {
        super.willInit()
        attr {
            absolutePosition(0f, 0f)
            size(0f, 0f)
        }
    }

    override fun willRemoveFromParentView() {
        detachUIToolbarNativeHost()
        super.willRemoveFromParentView()
    }
}

open class UIToolbarItemAttr : Attr() {
    fun symbol(name: String): UIToolbarItemAttr {
        "symbol" with name
        return this
    }

    fun src(src: String): UIToolbarItemAttr {
        "src" with src
        return this
    }

    fun src(uri: ImageUri): UIToolbarItemAttr {
        return src(uri.toUrl(getPager().pageName))
    }

    fun symbolRendering(rendering: UIBarButtonItemSymbolRendering): UIToolbarItemAttr {
        "template" with rendering.value
        return this
    }

    fun template(template: Boolean): UIToolbarItemAttr {
        return symbolRendering(
            if (template) {
                UIBarButtonItemSymbolRendering.Template
            } else {
                UIBarButtonItemSymbolRendering.Original
            }
        )
    }

    fun title(title: String): UIToolbarItemAttr {
        "title" with title
        return this
    }

    fun tintColor(color: Color): UIToolbarItemAttr {
        "tintColor" with color.toString()
        return this
    }

    fun kind(kind: UIToolbarItemKind): UIToolbarItemAttr {
        "kind" with kind.value
        return this
    }

    fun spaceWidth(width: Float): UIToolbarItemAttr {
        "spaceWidth" with width.toString()
        return this
    }

    fun index(index: Int): UIToolbarItemAttr {
        "index" with index.toString()
        return this
    }

    override fun visibility(visibility: Boolean): UIToolbarItemAttr {
        super.visibility(visibility)
        return this
    }

    fun axis(axis: UIBarAxis): UIToolbarItemAttr {
        "axis" with axis.value
        return this
    }
}

open class UIToolbarItemEvent : Event()

internal fun DeclarativeBaseView<*, *>.detachUIToolbarNativeHost() {
    if (renderView != null) {
        removeRenderView()
    }
}
