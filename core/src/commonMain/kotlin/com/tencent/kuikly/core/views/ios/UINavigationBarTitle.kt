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
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.layout.Frame

/** 自定义中间区域：在 [UINavigationBarTitle] 上写 `attr` + 子 View。 */
typealias UINavigationBarTitleCreator = UINavigationBarTitleView.() -> Unit

internal fun ViewContainer<*, *>.UINavigationBarTitle(init: UINavigationBarTitleView.() -> Unit) {
    if (!uiKitChromeSupported()) return
    addChild(UINavigationBarTitleView(), init)
}

open class UINavigationBarTitleAttr : ContainerAttr()

open class UINavigationBarTitleView : ViewContainer<UINavigationBarTitleAttr, UINavigationBarTitleEvent>() {
    override fun createAttr(): UINavigationBarTitleAttr = UINavigationBarTitleAttr()
    override fun createEvent(): UINavigationBarTitleEvent = UINavigationBarTitleEvent()
    override fun viewName() = ViewConst.TYPE_IOS_UI_NAVIGATION_TITLE

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

    override fun layoutFrameDidChanged(frame: Frame) {
        super.layoutFrameDidChanged(frame)
        renderView?.callMethod(METHOD_SYNC_TITLE_ATTACHMENT)
    }

    internal companion object {
        const val METHOD_SYNC_TITLE_ATTACHMENT = "syncTitleAttachment"
    }
}

open class UINavigationBarTitleEvent : Event()
