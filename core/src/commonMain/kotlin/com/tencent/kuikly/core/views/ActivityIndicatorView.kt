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

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event

/*
 *  @brief 活动指示器（旋转菊花样式）
 *  注：size ios系统限制为默认size(20f, 20f), 所以这里多端统一固定尺寸为20f，
 *  若想修改显示大小，可通过缩放属性实现 如：transform(Scale(1.5f, 1.5f))
 */
class ActivityIndicatorView : DeclarativeBaseView<ActivityIndicatorAttr, ActivityIndicatorEvent>() {

    override fun didInit() {
        super.didInit()
        attr.size(20f, 20f) // iOS限制了20f，故多端统一20f
    }
    override fun createAttr(): ActivityIndicatorAttr {
        return ActivityIndicatorAttr()
    }

    override fun createEvent(): ActivityIndicatorEvent {
        return ActivityIndicatorEvent()
    }

    override fun viewName(): String {
        return ViewConst.TYPE_ACTIVITY_INDICATOR
    }
}

class ActivityIndicatorAttr : Attr() {

    /* 设置灰色样式菊花，注：该属性无法动态修改 */
    fun isGrayStyle(isGrayStyle: Boolean) {
        STYLE with  (if (isGrayStyle) "gray" else "white")
    }

    companion object {
        const val STYLE = "style"
    }

}

class ActivityIndicatorEvent : Event()

/*
 *  @brief 活动指示器（旋转菊花样式）
 *  注：size ios系统限制为默认size(20f, 20f), 所以这里多端统一固定尺寸为20f，
 *  若想修改显示大小，可通过缩放属性实现 如：transform(Scale(1.5f, 1.5f))
 */
fun ViewContainer<*, *>.ActivityIndicator(init: ActivityIndicatorView.() -> Unit) {
    addChild(ActivityIndicatorView(), init)
}