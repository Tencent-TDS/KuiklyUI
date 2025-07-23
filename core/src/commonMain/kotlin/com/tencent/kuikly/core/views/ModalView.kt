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
import com.tencent.kuikly.core.layout.MutableFrame
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

const val MIN_BUILD_VERSION = 2
/**
 * 创建一个 Modal 实例。Modal 是一个自定义的模态窗口组件，用于在当前页面上显示一个浮动窗口。
 * 当模态窗口显示时，用户无法与背景页面进行交互，只能与模态窗口内的内容进行交互。
 * 模态窗口可以用于显示表单、提示信息、详细信息等场景。
 * @param inWindow 布尔值，表示模态窗口的层级是否为窗口顶级，以及是否和屏幕等大（默认为 false，表示和 Pager 一样大）。
 * 注：若不使用默认尺寸，可以在 modal attr 中设置，如 attr { absolutePosition(top = xx, left = xx); size(xx, xx); }。
 * @param init 一个 ModalView.() -> Unit 函数，用于初始化 ModalView 的属性和子视图。
 */
fun ViewContainer<*, *>.Modal(inWindow: Boolean = false, init: ModalView.() -> Unit) {
    ModalView().also {
        it.inWindow = inWindow
        addChild(it, init)
    }
}

class ModalView : ViewContainer<ContainerAttr, Event>() {
    /* 层级是否顶层，和屏幕等大 */
    var inWindow: Boolean = false
        set(value) {
            // native render版本最低要求
            if (PagerManager.getCurrentPager().pageData.nativeBuild >= MIN_BUILD_VERSION) {
                field = value
            }
        }
    private var contentView: ViewContainer<*, *>? = null
    override fun createAttr(): ContainerAttr {
        return ContainerAttr()
    }

    override fun createEvent(): Event {
        return Event()
    }

    override fun frameInParentRenderComponentCoordinate(frame: Frame): MutableFrame {
        if (inWindow) {
            return frame.toMutableFrame()
        }
        return super.frameInParentRenderComponentCoordinate(frame)
    }

    override fun willInit() {
        super.willInit()
        if (inWindow) {
            attr {
                absolutePosition(top = 0f, left = 0f)
                width(if (pagerData.activityWidth > 0f) pagerData.activityWidth else pagerData.deviceWidth)
                height(if (pagerData.activityHeight > 0f) pagerData.activityHeight else pagerData.deviceHeight)
            }
            event {
                click {  } // 避免手势穿透
            }
        } else {
            contentView = DivView()
            contentView?.also {
                currentWindow().addChild(it) {
                    attr {
                        absolutePosition(top = 0f, left = 0f, bottom = 0f, right = 0f)
                    }
                }
            }
        }
    }

    override fun didInit() {
        super.didInit()
        if (!inWindow) {
            contentView?.also {
                val index = currentWindow().domChildren().indexOf(it)
                currentWindow().insertDomSubView(it, index)
            }
        }
    }

    override fun attr(init: ContainerAttr.() -> Unit) {
        if (inWindow) {
            super.attr(init)
        } else {
            contentView?.also {
                it.attr(init)
            }
        }
    }

    override fun event(init: Event.() -> Unit) {
        if (inWindow) {
            super.event(init)
        } else {
            contentView?.also {
                it.event(init)
            }
        }
    }

    override fun <T : DeclarativeBaseView<*, *>> addChild(child: T, init: T.() -> Unit) {
        if (inWindow) {
            super.addChild(child, init)
        } else {
            contentView?.also {
                it.addChild(child, init)
            }
        }
    }
    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        if (!inWindow) {
            if (!getPager().isWillDestroy()) {
                contentView?.also {
                    currentWindow().removeDomSubView(it)
                    currentWindow().removeChild(it)
                }
            }
        }
    }
    override fun viewName(): String {
        return if (inWindow) ViewConst.TYPE_MODAL_VIEW else ViewConst.TYPE_VIEW
    }

    private fun currentWindow(): ViewContainer<*, *> {
        val composeCard = locationPager()
        if (composeCard != null) {
            return composeCard as ViewContainer<*, *>
        }
        return (getPager() as ViewContainer<*, *>)
    }

    private fun locationPager() : DeclarativeBaseView<*, *>? {
        var locationPager : DeclarativeBaseView<*, *>? = this
        while (locationPager != null && !locationPager.isPager()) {
            locationPager = locationPager.parent
            if (locationPager?.isPager() == true) {
                break
            }
        }
        return locationPager
    }

    companion object {
        const val ON_WILL_DISMISS = "onWillDismiss"
    }
}

enum class ModalDismissReason(val value: Int) {
    BackPressed(0);

    companion object {
        fun from(value: Int): ModalDismissReason {
            return ModalDismissReason.values()
                .firstOrNull { it.ordinal == value } ?: ModalDismissReason.BackPressed
        }
    }
}

typealias DismissEventHandlerFn = (ModalDismissReason) -> Unit

/**
 * 设置一个用于监听系统back按钮事件的回调
 */
fun Event.willDismiss(handler: DismissEventHandlerFn) {
    register(ModalView.ON_WILL_DISMISS){
        it as JSONObject
        val reason = it.optInt("reason", 0)
        handler(ModalDismissReason.from(reason))
    }
}
