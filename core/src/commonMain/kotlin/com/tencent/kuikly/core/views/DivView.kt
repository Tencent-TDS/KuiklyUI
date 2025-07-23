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
import com.tencent.kuikly.core.views.internal.GroupAttr
import com.tencent.kuikly.core.views.internal.GroupEvent
import com.tencent.kuikly.core.views.internal.GroupView
import com.tencent.kuikly.core.views.internal.TouchEventHandlerFn

open class DivView : GroupView<DivAttr, DivEvent>() {
    
    override fun createAttr(): DivAttr {
        return DivAttr()
    }

    override fun createEvent(): DivEvent {
        return DivEvent()
    }

    override fun viewName(): String {
        return ViewConst.TYPE_VIEW
    }

    override fun isRenderView(): Boolean {
        return isRenderViewForFlatLayer()
    }
}


open class DivAttr : GroupAttr()

open class DivEvent : GroupEvent()

/**
 * 创建一个类似于 ViewGroup/UIView/Div 的视图容器。
 * @param init 一个 DivView.() -> Unit 函数，用于初始化视图容器的属性和子视图。
 */
fun ViewContainer<*, *>.View(init: DivView.() -> Unit) {
    val viewGroup = createViewFromRegister(ViewConst.TYPE_VIEW_CLASS_NAME) as? DivView
    if (viewGroup != null) { // 存在自定义扩展
        addChild(viewGroup, init)
    } else {
        addChild(DivView(), init)
    }
}