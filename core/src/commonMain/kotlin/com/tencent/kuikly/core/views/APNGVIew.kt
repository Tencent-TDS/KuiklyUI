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
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.EventHandlerFn

/*
 * APNG动画组件
 */
fun ViewContainer<*, *>.APNG(init: APNGVView.() -> Unit) {
    addChild(APNGVView(), init)
}

class APNGVView : DeclarativeBaseView<APNGAttr, APNGEvent>() {
    override fun createAttr(): APNGAttr = APNGAttr()
    override fun createEvent(): APNGEvent = APNGEvent()
    override fun viewName(): String = ViewConst.TYPE_APNG_VIEW

    /*
     * 播放动画（在attr.autoPlay为true时不需要手动调用该接口）
     */
    fun play() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("play", "")
        }
    }
    /*
     * 停止动画
     */
    fun stop() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("stop", "")
        }
    }

}

class APNGAttr : Attr() {
    // 设置动画资源（support cdn url or local filePath）
    fun src(src: String) {
        APNGConst.SRC with src
    }

    // 循环播放次数（default is 0 无限次）
    fun repeatCount(repeatCount: Int) {
        APNGConst.REPEAT_COUNT with repeatCount
    }

    // 是否view创建后自动播放（default is true）
    fun autoPlay(play: Boolean) {
        APNGConst.AUTO_PLAY with play.toInt()
    }

}

class APNGEvent : Event() {
    // 资源加载失败回调该事件
    fun loadFailure(handler: EventHandlerFn) {
        register("loadFailure", handler)
    }

    // 动画开始播放时调用该事件
    fun animationStart(handler: EventHandlerFn) {
        register("animationStart", handler)
    }

    // 动画播放结束时调用该事件
    fun animationEnd(handler: EventHandlerFn) {
        register("animationEnd", handler)
    }

}

object APNGConst {
    const val SRC = "src"
    const val REPEAT_COUNT = "repeatCount"
    const val AUTO_PLAY = "autoPlay"
}
