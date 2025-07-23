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

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.Event

/*
 * 用于列表下的自动悬停（列表滚动可自动悬浮置顶）视图组件
 */
class HoverView : ViewContainer<HoverAttr, Event>() {
    override fun didInit() {
        super.didInit()
        attr.keepAlive = true
    }
    override fun didMoveToParentView() {
        super.didMoveToParentView()
        attr.bringIndex( parent?.domChildren()?.indexOf(this) ?: 0)
    }

    override fun createAttr(): HoverAttr = HoverAttr()

    override fun createEvent(): Event = Event()

    override fun viewName(): String {
        return ViewConst.TYPE_HOVER
    }
}

class HoverAttr : ContainerAttr() {

    /**
     * 设置置顶层级（同一列表多个 HoverView 设置该属性，值越大层级越高）。
     * @param index 置顶层级值。
     */
    fun bringIndex(index: Int) {
        "bringIndex" with index
    }

    /**
     * 设置悬停距离列表顶部距离（默认为 0）。
     * @param offset 悬停距离列表顶部的距离
     */
    fun hoverMarginTop(offset: Float) {
        "hoverMarginTop" with offset
    }
}

/**
 * 创建用于列表下的悬停组件（包括 list，瀑布流，scroll 等）。
 * @param init 一个 HoverView.() -> Unit 函数，用于初始化悬停组件的属性和子视图。
 */
fun ScrollerView<*, *>.Hover(init: HoverView.() -> Unit) {
    addChild(HoverView(), init)
}

/**
 * 创建用于模板指令(vif,vfor等)下的悬停组件。
 * @param init 一个 HoverView.() -> Unit 函数，用于初始化悬停组件的属性和子视图。
 */
fun VirtualView<*, *>.Hover(init: HoverView.() -> Unit) {
    addChild(HoverView(), init)
}