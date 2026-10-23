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
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.Event

internal fun ViewContainer<*, *>.mountUINavigationBar(init: UINavigationBarView.() -> Unit) {
    if (!uiKitChromeSupported()) return
    addChild(UINavigationBarView(), init)
}

open class UINavigationBarView : DeclarativeBaseView<UINavigationBarAttr, UINavigationBarEvent>() {
    override fun createAttr() = UINavigationBarAttr()
    override fun createEvent() = UINavigationBarEvent()
    override fun viewName() = ViewConst.TYPE_IOS_UI_NAVIGATION_BAR

    override fun willInit() {
        super.willInit()
        attr {
            absolutePosition(0f, 0f)
            size(0f, 0f)
        }
    }
}

open class UINavigationBarAttr : Attr() {
    fun tintColor(color: Color): UINavigationBarAttr {
        "tintColor" with color.toString()
        return this
    }

    override fun visibility(visibility: Boolean): UINavigationBarAttr {
        super.visibility(visibility)
        return this
    }

    /** leading 按钮组与导航栏起始边缘之间额外避让的距离（pt）。 */
    fun leadingInset(inset: Float): UINavigationBarAttr {
        "leadingInset" with inset.coerceAtLeast(0f).toString()
        return this
    }

    /** trailing 按钮组与导航栏结束边缘之间额外避让的距离（pt）。 */
    fun trailingInset(inset: Float): UINavigationBarAttr {
        "trailingInset" with inset.coerceAtLeast(0f).toString()
        return this
    }

    fun contentScrollNativeRef(nativeRef: Int): UINavigationBarAttr {
        "contentScrollNativeRef" with nativeRef.coerceAtLeast(0).toString()
        return this
    }

    fun contentScroll(ref: ViewRef<*>?): UINavigationBarAttr {
        return contentScrollNativeRef(ref?.nativeRef ?: 0)
    }

    fun navigationBarMinimizationBehavior(
        behavior: UINavigationBarMinimizationBehavior
    ): UINavigationBarAttr {
        "navigationBarMinimizationBehavior" with behavior.value
        return this
    }

    fun navigationBarMinimizationSafeAreaAdjustment(
        adjustment: UINavigationBarMinimizationSafeAreaAdjustment
    ): UINavigationBarAttr {
        "navigationBarMinimizationSafeAreaAdjustment" with adjustment.value
        return this
    }

    fun navigationBarMinimizationRestoration(
        restoration: UINavigationBarMinimizationRestoration
    ): UINavigationBarAttr {
        "navigationBarMinimizationRestoration" with restoration.value
        return this
    }

    fun barBackground(style: UINavigationBarBackgroundStyle): UINavigationBarAttr {
        "barBackground" with style.value
        return this
    }

    fun barBackgroundColor(color: Color): UINavigationBarAttr {
        "barBackgroundColor" with color.toString()
        return this
    }

    fun barScrollEdgeBackground(style: UINavigationBarBackgroundStyle): UINavigationBarAttr {
        "barScrollEdgeBackground" with style.value
        return this
    }

    fun barScrollEdgeBackgroundColor(color: Color): UINavigationBarAttr {
        "barScrollEdgeBackgroundColor" with color.toString()
        return this
    }

    fun barShadowColor(color: Color): UINavigationBarAttr {
        "barShadowColor" with color.toString()
        return this
    }

    fun barStyle(style: UINavigationBarStyle): UINavigationBarAttr {
        "barStyle" with style.value
        return this
    }

    fun barTranslucent(translucent: UINavigationBarTranslucent): UINavigationBarAttr {
        "barTranslucent" with translucent.value
        return this
    }

    fun hideScrollEdgeEffects(hide: Boolean): UINavigationBarAttr {
        "hideScrollEdgeEffects" with hide.toString()
        return this
    }
}

open class UINavigationBarEvent : Event()
