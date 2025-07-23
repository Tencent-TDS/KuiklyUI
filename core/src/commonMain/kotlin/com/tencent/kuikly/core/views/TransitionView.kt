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
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IPagerEventObserver
import com.tencent.kuikly.core.reactive.handler.observable
/**
 * 转场类型
 */
enum class TransitionType {
    NONE,
    DIRECTION_FROM_BOTTOM,  // 从底部入场过渡
    DIRECTION_FROM_CENTER,  // 从中间入场过渡
    DIRECTION_FROM_RIGHT,  // 从右入场过渡&从右出场过渡
    DIRECTION_FROM_LEFT,  // 从左入场过渡&从左出场过渡
    FADE_IN_OUT, //淡入入场过渡&淡出出场过渡
    CUSTOM, // 自定义转场动画，务必设置attr中的自定义属性
}
/**
 * 等价 View 的内容视图转场过渡类，如淡入淡出，底部过渡入场等，常用于弹窗出场动画使用。
 * 使用方式:
 * 等价 View 来使用，也需要和 view 一样设置布局，总之当作 View 来使用和布局，只不过多了内容转场过度动画。
 * @param type This 视图转场动画类型。
 */
fun ViewContainer<*, *>.TransitionView(type: TransitionType, init: TransitionView.() -> Unit) {
    addChild(TransitionView()) {
        attr {
            transitionType = type
        }
        init()
    }
}

/**
 * 转场属性类，继承自 ContainerAttr。
 */
class TransitionAttr : ContainerAttr() {
    internal var transitionType = TransitionType.NONE  // 转场动画类型
    internal var transitionAppear by observable(true) // 以入场还是退场动画进行
    internal var beginAnimationAttr: (Attr.() -> Unit)? = null
    internal var endAnimationAttr: (Attr.() -> Unit)? = null
    internal var animationConfig: Animation? = null

    /**
     * 控制动画转场是入场还是退场，默认为入场（可选设置）。
     * 不支持二次修改该属性设置。
     * @param enterOrExit 入场或退场布尔值。
     */
    fun transitionAppear(enterOrExit: Boolean) {
        transitionAppear = enterOrExit
    }

    /**
     * 自定义动画起始状态的属性设置（可选设置）。
     * 注: transitionType 记得设置为 TransitionType.CUSTOM。
     * 不支持二次修改该属性设置。
     * @param beginAttr 起始动画属性设置。
     */
    fun customBeginAnimationAttr(beginAttr: Attr.() -> Unit) {
        beginAnimationAttr = beginAttr
    }

    /**
     * 自定义动画终止状态的属性设置（可选设置）。
     * 注: transitionType 记得设置为 TransitionType.CUSTOM。
     * 不支持二次修改该属性设置。
     * @param endAttr 终止动画属性设置。
     */
    fun customEndAnimationAttr(endAttr: Attr.() -> Unit) {
        endAnimationAttr = endAttr
    }

    /**
     * 配置动画参数，优先使用该设置（可选设置）。
     * 不支持二次修改该属性设置。
     * @param animation 动画配置。
     */
    fun customAnimation(animation: Animation) {
        animationConfig = animation
    }
}

/**
 * 转场事件类，继承自 Event。
 */
class TransitionEvent : Event() {
    internal var transitionFinishHandlerFn: ((transitionAppear: Boolean) -> Unit)? = null

    /**
     * 转场动画结束回调。
     * @param eventHandlerFn 回调函数，参数为转场动画类型（入场或退场）。
     */
    fun transitionFinish(eventHandlerFn: (transitionAppear: Boolean) -> Unit) {
        transitionFinishHandlerFn = eventHandlerFn
    }
}

/**
 * 转场过渡类
 */
class TransitionView : ViewContainer<TransitionAttr, TransitionEvent>() , IPagerEventObserver {
    var didLayout by observable(false)
    override fun createAttr() = TransitionAttr()
    override fun createEvent() = TransitionEvent()
    override fun viewName() = ViewConst.TYPE_VIEW

    override fun didInit() {
        super.didInit()
        val ctx = this
        val animation = ctx.attr.animationConfig ?: Animation.springEaseInOut(0.35f, 0.9f, 1f)
        attr {
            // 绑定动画 从底下往上弹
            if (ctx.attr.transitionType == TransitionType.DIRECTION_FROM_BOTTOM) {
                if (!ctx.didLayout || !ctx.attr.transitionAppear) {
                    transform(Translate(0f, 1f))
                } else {
                    transform(Translate(0f, 0f))
                }
            } else  if (ctx.attr.transitionType == TransitionType.DIRECTION_FROM_RIGHT) {
                if (!ctx.didLayout || !ctx.attr.transitionAppear) {
                    transform(Translate(1f, 0f))
                } else {
                    transform(Translate(0f, 0f))
                }
            } else  if (ctx.attr.transitionType == TransitionType.DIRECTION_FROM_LEFT) {
                if (!ctx.didLayout || !ctx.attr.transitionAppear) {
                    transform(Translate(-1f, 0f))
                } else {
                    transform(Translate(0f, 0f))
                }
            } else if (ctx.attr.transitionType == TransitionType.DIRECTION_FROM_CENTER) {
                if (!ctx.didLayout || !ctx.attr.transitionAppear) {
                    transform(Scale(0f, 0f))
                } else {
                    transform(Scale(1f, 1f))
                }
            } else if (ctx.attr.transitionType == TransitionType.FADE_IN_OUT) {
                if (!ctx.didLayout || !ctx.attr.transitionAppear) {
                    opacity(0f)
                } else {
                    opacity(1f)
                }
            } else if (ctx.attr.transitionType == TransitionType.CUSTOM) {
                if (!ctx.didLayout || !ctx.attr.transitionAppear) {
                    ctx.attr.beginAnimationAttr?.invoke(this)
                } else {
                    ctx.attr.endAnimationAttr?.invoke(this)
                }
            }
            animation(animation, ctx.didLayout)
            animation(animation, ctx.attr.transitionAppear)
        }

        event {
            animationCompletion {
                ctx.event.transitionFinishHandlerFn?.invoke(ctx.attr.transitionAppear)
            }
        }
        getPager().addPagerEventObserver(this)
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        getPager().removePagerEventObserver(this)
    }

    override fun setFrameToRenderView(frame: Frame) {
        super.setFrameToRenderView(frame)
        if (!didLayout) {
            didLayout = true
        }
    }

    override fun onPagerEvent(pagerEvent: String, eventData: JSONObject) {
        when (pagerEvent) {
           // "onModalModeBackPressed" -> attr.transitionAppear = false // 收到back键关闭
        }
    }
}
