package com.tencent.kuikly.demo.pages.video.feed

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IPagerEventObserver
import com.tencent.kuikly.core.reactive.handler.observable

/*
 * 业务基于Kuikly动画能力封装扩展的常用动画转场过渡类
 */

internal enum class TransitionType {
    DIRECTION_FROM_BOTTOM,  // 从底部入场过渡
    DIRECTION_FROM_CENTER,  // 从中间入场过渡
    DIRECTION_FROM_RIGHT,  // 从右入场过渡&从右出场过渡
    DIRECTION_FROM_LEFT,  // 从左入场过渡&从左出场过渡
    FADE_IN_OUT, //淡入入场过渡&淡出出场过渡
}

internal class TransitionAttr : ContainerAttr() {
    internal var transitionType = TransitionType.FADE_IN_OUT  // 弹出方向，默认从底部弹出
    var transitionAppear by observable(true) // 是否过渡出现

}

internal class TransitionEvent : ComposeEvent() {
    // transitionFinish
    fun transitionFinish(eventHandlerFn: (transitionAppear: Boolean) -> Unit) {
        registerEvent(TRANSITION_FINISH) {
            eventHandlerFn((it as TransitionAttr).transitionAppear)
        }
    }

    companion object {
        const val TRANSITION_FINISH = "transitionFinish"
    }
}
/*
 * 淡入淡出过渡类
 */
internal fun ViewContainer<*, *>.TransitionFadeInOutView(init: TransitionView.() -> Unit) {
    addChild(TransitionView()) {
        attr {
            transitionType = TransitionType.FADE_IN_OUT
        }
        init()
    }
}
/*
 * 从底部入场过渡类
 */
internal fun ViewContainer<*, *>.TransitionFromBottomView(init: TransitionView.() -> Unit) {
    addChild(TransitionView()) {
        attr {
            transitionType = TransitionType.DIRECTION_FROM_BOTTOM
        }
        init()
    }
}
// 从右边入场过渡类
internal fun ViewContainer<*, *>.TransitionFromRightView(init: TransitionView.() -> Unit) {
    addChild(TransitionView()) {
        attr {
            transitionType = TransitionType.DIRECTION_FROM_RIGHT
        }
        init()
    }
}
// 从左边入场过渡类
internal fun ViewContainer<*, *>.TransitionFromLeftView(init: TransitionView.() -> Unit) {
    addChild(TransitionView()) {
        attr {
            transitionType = TransitionType.DIRECTION_FROM_LEFT
        }
        init()
    }
}

internal fun ViewContainer<*, *>.TransitionFromCenterView(init: TransitionView.() -> Unit) {
    addChild(TransitionView()) {
        attr {
            transitionType = TransitionType.DIRECTION_FROM_CENTER
        }
        init()
    }
}

internal class TransitionView : ViewContainer<TransitionAttr, TransitionEvent>(), IPagerEventObserver {
    var didLayout by observable(false)
    override fun didInit() {
        super.didInit()
        val ctx = this
        val animation = Animation.springEaseInOut(0.35f, 0.9f, 1f)
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
            }
            animation(animation, ctx.didLayout)
            animation(animation, ctx.attr.transitionAppear)
        }

        event {

            animationCompletion {
                ctx.event.onFireEvent(TransitionEvent.TRANSITION_FINISH, ctx.attr)
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

    override fun createAttr(): TransitionAttr {
        return TransitionAttr()
    }

    override fun createEvent(): TransitionEvent {
        return TransitionEvent()
    }

    override fun viewName(): String {
        return ViewConst.TYPE_VIEW
    }

    override fun onPagerEvent(pagerEvent: String, eventData: JSONObject) {
        when (pagerEvent) {
            "onModalModeBackPressed" -> attr.transitionAppear = false // 收到back键关闭
        }
    }

}