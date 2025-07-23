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
import com.tencent.kuikly.core.base.event.ClickParams
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.EventName
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.compose.Button
/*
 * 提示对话框组件，UI风格对齐iOS UIAlertController风格, 并支持自定义弹窗UI
 * 用法示例:
 * private var showAlert by observable(false) // 定义响应式变量
 *         AlertDialog {
                attr {
                    showAlert(ctx.showAlert) // 控制Alert是否显示，不显示时不占用布局(必须设置该属性)
                    title("我是Alert标题")
                    message("alert内容")
                    actionButtons("取消"，"确定")
                }
                event {
                    clickActionButton { index ->
                         // 根据index进行确认点击了哪一个button处理对应事件(index值和actionButtons传入button的下标一致)
                        ctx.showAlert = false // 关闭Alert弹框
                    }
                }
            }
 */
fun ViewContainer<*, *>.AlertDialog(init : AlertDialogView.() -> Unit) {
    addChild(AlertDialogView(), init)
}

typealias ActionButtonTitleAttr = TextAttr.() -> Unit

class AlertDialogAttr : ContainerAttr() {
    internal var showAlert by observable(false)
    internal var contentViewCreator: ViewBuilder? = null
    internal var backgroundViewCreator: ViewBuilder? = null
    internal var title by observable("")
    internal var message by observable("")
    internal var actionButtonsAttrs by observableList<ActionButtonTitleAttr>()
    internal var inWindow = false
    /*
     * 控制Alert是否显示，不显示时不占用布局(必须设置该属性)
     * 注:如果要关闭Alert，可以监听Event中的clickActionButton事件进行控制showAlert绑定变量
     */
    fun showAlert(showAlert: Boolean) {
        this.showAlert = showAlert
    }
    /*
     *  Alert标题(当message不为空时可选设置title)
     */
    fun title(title: String) {
        this.title = title
    }
    /*
     *  Alert内容(当标题不为空时为可选设置message)
     */
    fun message(message: String) {
        this.message = message
    }
    /*
     * Alert点击的按钮，如取消，确定(必须设置)
     */
    fun actionButtons(vararg buttonTitles: String) {
        actionButtonsAttrs.clear()
        for (title in buttonTitles) {
            actionButtonsAttrs.add {
                text(title)
            }
        }
    }
    /*
     * Alert点击按钮的自定义按钮文字样式，如取消(红色或加粗)，确定(默认蓝色，加粗)(可选设置)
     * 用法例子:actionButtonsCustomAttr( {text("Cancel").color(Color.RED)}, {text("Confirm")})
     */
    fun actionButtonsCustomAttr(vararg buttonsAttr: ActionButtonTitleAttr) {
        actionButtonsAttrs.clear()
        for (attr in buttonsAttr) {
            actionButtonsAttrs.add(attr)
        }
    }
    /*
     * 自定义整个前景View UI(代替自带的即整个白色块区域，该自定义前景内容UI会被居中显示)
     */
    fun customContentView(viewCreator: ViewBuilder) {
        contentViewCreator = viewCreator
    }

    /*
     * 自定义整个背景View UI(代替自带的即整个背景黑色蒙层，注意需要设置该View布局为全屏尺寸)
     */
    fun customBackgroundView(viewCreator: ViewBuilder) {
        backgroundViewCreator = viewCreator
    }
    /**
     * 全屏显示该Alert(默认为false)
     */
    fun inWindow(window: Boolean) {
        this.inWindow = window
    }
}

typealias AlertButtonClickCallback = (buttonIndex: Int) -> Unit

class AlertDialogEvent : Event() {
    internal var willDismissHandlerFn: DismissEventHandlerFn? = null
    internal var didClickActionButtonHandlerFn: AlertButtonClickCallback? = null
    internal var clickBackgroundMaskHandlerFn: ((ClickParams) -> Unit)? = null
    internal var alertDidExitHandlerFn: (() -> Unit)? = null

    /*
     * 系统返回事件，back按钮或右滑
     */
    fun willDismiss(handler: DismissEventHandlerFn) {
        willDismissHandlerFn = handler
    }
    /*
     * 按钮被点击事件，回参对应被点击的button index(index值和actionButtons传入button的下标一致)
     */
    fun clickActionButton(handler: AlertButtonClickCallback) {
        didClickActionButtonHandlerFn = handler
    }
    /*
     * 背景蒙层点击事件，用于在自定义前景UI场景下，可能会点击背景蒙层关闭弹窗
     */
    fun clickBackgroundMask(handler: (ClickParams) -> Unit) {
        clickBackgroundMaskHandlerFn = handler
    }
    /*
     * alert弹窗完全退出(不显示&动画结束)回调，业务此时可以关闭页面(若有需要)
     */
    fun alertDidExit(handler: () -> Unit) {
        alertDidExitHandlerFn = handler
    }
}

class AlertDialogView : VirtualView<AlertDialogAttr, AlertDialogEvent>() {
    private var showAlerting by observable(false)
    override fun createAttr() = AlertDialogAttr()
    override fun createEvent() = AlertDialogEvent()

    override fun didInit() {
        super.didInit()
        showAlerting = attr.showAlert
        initContentViewCreator()
        initBackgroundViewCreator()
        body()()
    }

    private fun initContentViewCreator() {
        if (attr.contentViewCreator != null) {
            return
        }
        val ctx = this
        attr.contentViewCreator = {
            View {
                attr {
                    borderRadius(14f)
                    width(270f)
                    if (getPager().isNightMode()) {
                        backgroundColor(Color(red255 = 0, blue255 = 0, green255 = 0, alpha01 = 0.85f))
                    } else {
                        backgroundColor(Color(red255 = 255, blue255 = 255, green255 = 255, alpha01 = 0.75f))
                    }
                }
                Blur {
                    attr {
                        absolutePositionAllZero()
                    }
                }
                View {
                    attr {
                        margin(top = 20f, left = 16f, right = 16f, bottom = 20f)
                        allCenter()
                    }
                    vif({ctx.attr.title.isNotEmpty()}) {
                        Text {
                            attr {
                                fontSize(17f)
                                lineHeight(22f)
                                text(ctx.attr.title)
                                if (getPager().isNightMode()) { color(Color.WHITE) } else { color(Color.BLACK) }
                                textAlignCenter()
                                fontWeightSemisolid()
                            }
                        }
                    }
                    vif({ctx.attr.message.isNotEmpty()}) {
                        Text {
                            attr {
                                fontSize(13f)
                                lineHeight(18f)
                                text(ctx.attr.message)
                                if (getPager().isNightMode()) { color(Color.WHITE) } else { color(Color.BLACK) }
                                textAlignCenter()
                            }
                        }
                    }
                }
                vif({ctx.attr.actionButtonsAttrs.size == 2}) {
                    // line
                    ctx.createLineView(false).invoke(this)
                    View {
                        attr {
                            flexDirectionRow()
                            height(44f)
                            justifyContentSpaceBetween()
                        }
                        ctx.createActionButton(ctx.attr.actionButtonsAttrs[0], 0).invoke(this)
                        ctx.createLineView(true).invoke(this)
                        ctx.createActionButton(ctx.attr.actionButtonsAttrs[1], 1).invoke(this)
                    }
                }
                velse {
                    vfor({ctx.attr.actionButtonsAttrs}) { buttonTitleAttr ->
                        View {
                            // line
                            ctx.createLineView(false).invoke(this)
                            ctx.createActionButton(buttonTitleAttr, ctx.attr.actionButtonsAttrs.indexOf(buttonTitleAttr)).invoke(this)
                        }

                    }
                }
            }

        }
    }

    private fun initBackgroundViewCreator() {
        if (attr.backgroundViewCreator != null) {
            return
        }
        attr.backgroundViewCreator = {
            View {
                attr {
                    absolutePositionAllZero()
                    backgroundColor(Color(red255 = 0, green255 = 0, blue255 = 0, alpha01 = 0.2f))
                }
            }
        }
    }

    private fun body(): ViewBuilder {
        val ctx = this
        return {
            vif({ctx.showAlerting || ctx.attr.showAlert}) {
                Modal(ctx.attr.inWindow) {
                    attr {
                        allCenter() // center all content
                    }
                    event {
                        if (ctx.event.willDismissHandlerFn != null) {
                            willDismiss {
                                ctx.event.willDismissHandlerFn?.invoke(it)
                            }
                        }
                    }
                    // mask
                    TransitionView(type = TransitionType.FADE_IN_OUT) {
                        attr {
                            transitionAppear(ctx.attr.showAlert)
                            absolutePositionAllZero()
                            customAnimation(Animation.springEaseInOut(0.3f, 0.8f, 0.9f))
                        }
                        event {
                            click {
                                ctx.event.clickBackgroundMaskHandlerFn?.invoke(it)
                                ctx.event.onFireEvent(EventName.CLICK.value, it)
                            }
                        }
                        ctx.attr.backgroundViewCreator?.invoke(this)
                    }
                    // content
                    TransitionView(type = TransitionType.CUSTOM) {
                        attr {
                            transitionAppear(ctx.attr.showAlert)
                            customBeginAnimationAttr {
                                opacity(0f)
                                transform(scale = Scale(0.7f, 0.7f))
                            }
                            customEndAnimationAttr {
                                opacity(1f)
                                transform(scale = Scale(1f, 1f))
                            }
                            customAnimation(Animation.springEaseInOut(0.3f, 0.8f, 0.9f))
                        }
                        event {
                            transitionFinish { appear ->
                                ctx.showAlerting = appear
                                if (!(ctx.showAlerting || ctx.attr.showAlert)) {
                                    ctx.event.alertDidExitHandlerFn?.invoke()
                                }
                            }
                        }
                        View {
                            event {
                                click {  }
                            }
                            ctx.attr.contentViewCreator?.invoke(this)
                        }

                    }

                }
            }
        }
    }
    private fun createActionButton(buttonTitleAttr: ActionButtonTitleAttr, index: Int) : ViewBuilder {
        val ctx = this
        return {
            Button {
                attr {
                    height(44f)
                    flex(1f)
                    titleAttr {
                        fontWeight500()
                        fontSize(17f)
                        height(22f)
                        color(0xFF007AFF)
                        buttonTitleAttr.invoke(this)
                    }
                    highlightBackgroundColor(Color(red255 = 0, green255 = 0, blue255 = 0, alpha01 = 0.1f))
                }
                event {
                    click {
                        ctx.event.didClickActionButtonHandlerFn?.invoke(index)
                    }
                }
            }
        }
    }

    private fun createLineView(isVertical : Boolean) : ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    if (isVertical) width(0.5f) else height(0.5f)
                    if (getPager().isNightMode()) {
                        backgroundColor(Color(red255 = 255, green255 = 255, blue255 = 255, alpha01 = 0.24f))
                    } else {
                        backgroundColor(Color(red255 = 0, green255 = 0, blue255 = 0, alpha01 = 0.24f))
                    }
                }
            }
        }
    }
}
