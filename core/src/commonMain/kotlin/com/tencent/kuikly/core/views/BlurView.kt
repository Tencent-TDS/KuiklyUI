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
import kotlin.math.min

/*
 * 实时高斯模糊（毛玻璃）类（用于该view背后的内容区域进行高斯模糊，若背后内容发生变化，该高斯模糊会实时变化）
 */

class BlurView : DeclarativeBaseView<BlurAttr, Event>() {
    override fun createAttr(): BlurAttr = BlurAttr()
    override fun createEvent(): Event = Event()
    override fun viewName(): String {
        return ViewConst.TYPE_BLUR_VIEW
    }

    override fun willInit() {
        super.willInit()
        if (getPager().pageData.isIOS) {
            getViewAttr().backgroundColor(Color(red255 = 0, green255 = 0, blue255 = 0, alpha01 = 0.1f))
        } else {
            getViewAttr().backgroundColor(Color(red255 = 255, green255 = 255, blue255 = 255, alpha01 = 0.1f))
        }
    }

    override fun didInit() {
        super.didInit()
    }
}

class BlurAttr : Attr() {
    /// 高斯模糊半径，最大为12.5f（默认：10f）
    fun blurRadius(radius: Float) {
        "blurRadius" with min(radius, 12.5f)
    }

    /**
     * 想要模糊的View的nativeRef列表，用于提高在Android平台上的模糊性能
     * @param refs 想要模糊的View的nativeRef列表
     */
    fun targetBlurViewNativeRefs(refs: List<Int>) {
        "targetBlurViewNativeRefs" with  refs.joinToString(separator = "|")
    }

    /**
     * 是否模糊其他单独的layer
     * 目前只有 android 使用到，用于开启模糊 TextureView
     * 注意: 如果设置了targetBlurViewNativeRefs属性的话, 此属性无效
     */
    fun blurOtherLayer(blur: Boolean) {
        "blurOtherLayer" with blur.toInt()
    }
}

/*
 * 实时高斯模糊（毛玻璃）类（用于该view背后的内容区域进行高斯模糊，若背后内容发生变化，该高斯模糊会实时变化）
 */

fun ViewContainer<*, *>.Blur(init: BlurView.() -> Unit) {
    addChild(BlurView(), init)
}