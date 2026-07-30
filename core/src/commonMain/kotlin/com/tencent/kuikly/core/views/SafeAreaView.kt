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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.layout.undefined

/**
 * SafeAreaView渲染嵌套内容并自动应用填充来反映视图中未被导航栏、选项卡栏、工具栏和其他祖先视图覆盖的部分。
 * 此外，最重要的是，SafeArea 的填充反映了屏幕的物理限制，例如圆角或相机凹口（即 iPhone 13 上的传感器外壳区域）。
 * (注1：安全边距包含顶部状态栏高度区域)
 * (注2：SafeAreaView使用时，请使用应用了样式的来包裹您的顶级视图flex: 1。您可能还想使用与您的应用程序设计相匹配的背景颜色。)
 *（注3：该组件可当做View容器去使用，唯一差异仅增加了内容边距
 *  padding(top = pagerData.safeAreaInsets.top, left = pagerData.safeAreaInsets.left, bottom = pagerData.safeAreaInsets.bottom, right = pagerData.safeAreaInsets.right)
     内边距）
 */

fun ViewContainer<*, *>.SafeArea(init: SafeAreaView.() -> Unit) {
    addChild(SafeAreaView(), init)
}

class SafeAreaView : ViewContainer<SafeAreaAttr, SafeAreaEvent>() {

    override fun createAttr() = SafeAreaAttr()

    override fun createEvent() = SafeAreaEvent()

    override fun viewName(): String {
        return ViewConst.TYPE_VIEW
    }

    override fun willInit() {
        super.willInit()
        attr {
            padding(top = pagerData.safeAreaInsets.top, left = pagerData.safeAreaInsets.left, bottom = pagerData.safeAreaInsets.bottom, right = pagerData.safeAreaInsets.right)
        }
    }

    override fun didInit() {
        super.didInit()
        if (flexNode.styleHeight != Float.undefined) {
            getViewAttr().flex(1f)
        }
    }

    override fun isRenderView(): Boolean {
        return isRenderViewForFlatLayer()
    }
}

class SafeAreaAttr : ContainerAttr()

open class SafeAreaEvent : Event()
