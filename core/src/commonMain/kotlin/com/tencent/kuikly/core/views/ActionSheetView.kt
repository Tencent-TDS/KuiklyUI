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

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.VirtualView
import com.tencent.kuikly.core.base.event.ClickParams
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.EventName
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.compose.Button

/*
 * 操作表组件(用于提供一组可供用户选择的操作，以便完成任务)，UI风格对齐iOS UIActionSheet风格, 并支持自定义弹窗UI
 * 用法示例:
 * private var showActionSheet by observable(false) // 定义响应式变量
 *          ActionSheet {
                attr {
                    showActionSheet(ctx.showActionSheet)
                    descriptionOfActions("A short description of the actions")
                    actionButtons("Cancel", "Action0", "Action1", "Action2", "Action3", "Action4")
                }
                event {
                    clickActionButton { index->
                     // 根据index进行确认点击了哪一个button处理对应事件(index值和actionButtons传入button的下标一致)
                        ctx.showActionSheet = false  // 关闭ActionSheet弹框
                    }
                }
            }
 */
fun ViewContainer<*, *>.ActionSheet(init : ActionSheetView.() -> Unit) {
    addChild(ActionSheetView(), init)
}

class ActionSheetAttr : ContainerAttr() {
    internal var showActionSheet by observable(false)
    internal var contentViewCreator: ViewBuilder? = null
    internal var backgroundViewCreator: ViewBuilder? = null
    internal var descriptionOfActions by observable("")
    internal var actionButtonsAttrs by observableList<ActionButtonTitleAttr>()
    internal lateinit var cancelButtonAttr : ActionButtonTitleAttr
    internal var inWindow = false
    /*
     * 控制ActionSheet是否显示，不显示时不占用布局(必须设置该属性)
     * 注: 1.如果要关闭ActionSheet，可以监听Event中的clickActionButton事件进行控制showActionSheet绑定变量
     *     2.安卓中物理back键需要关闭actionSheet，需要业务自己在native监听键盘back事件发送到kotlin侧，业务控制showActionSheet来关闭
     * */
    fun showActionSheet(showAlert: Boolean) {
        this.showActionSheet = showAlert
    }
    /*
     *  A short description of the actions
     */
    fun descriptionOfActions(description: String) {
        this.descriptionOfActions = description
    }
    /*
     * ActionSheet点击的按钮，如取消，Action0, Action1, Action2, ..(必须设置)
     */
    fun actionButtons(cancelButtonTitle: String, vararg buttonTitles: String) {
        this.cancelButtonAttr = { text(cancelButtonTitle) }
        actionButtonsAttrs.clear()
        for (title in buttonTitles) {
            actionButtonsAttrs.add { text(title) }
        }
    }
    /*
    * ActionSheet点击的按钮的自定义按钮文字样式，如Cancel(红色或加粗)，Action0(默认蓝色，加粗)(可选设置)
    * 用法例子:actionButtonsCustomAttr( {text("Cancel").color(Color.RED)}, {text("Action0")})
    */
    fun actionButtonsCustomAttr(cancelButtonTitleAttr: ActionButtonTitleAttr, vararg buttonsAttr: ActionButtonTitleAttr) {
        cancelButtonAttr = cancelButtonTitleAttr
        actionButtonsAttrs.clear()
        for (attr in buttonsAttr) {
            actionButtonsAttrs.add(attr)
        }
    }
    /*
     * 自定义整个前景View UI(代替自带的即整个bottom白色块区域)
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
     * 全屏显示该ActionSheet(默认为false)
     */
    fun inWindow(window: Boolean) {
        this.inWindow = window
    }
}

typealias ActionButtonClickCallback = (buttonTitle: String) -> Unit

class ActionSheetEvent : Event() {
    internal var didClickActionButtonHandlerFn: AlertButtonClickCallback? = null
    internal var clickBackgroundMaskHandlerFn: ((ClickParams) -> Unit)? = null
    internal var actionSheetDidExitHandlerFn: (()->Unit)? = null

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
     * actionSheet完全退出(不显示&动画结束)回调，业务此时可以关闭页面(若有需要)
     */
    fun actionSheetDidExit(handler: () -> Unit) {
        actionSheetDidExitHandlerFn = handler
    }
}

class ActionSheetView : VirtualView<ActionSheetAttr, ActionSheetEvent>() {
    private var showActionSheeting by observable(false)
    override fun createAttr() = ActionSheetAttr()
    override fun createEvent() = ActionSheetEvent()

    override fun didInit() {
        super.didInit()
        showActionSheeting = attr.showActionSheet
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
                    margin(left = 8f, right = 8f, bottom = 8f)
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
                vif({ctx.attr.descriptionOfActions.isNotEmpty()}) {
                    View {
                        attr {
                            margin(all = 12f)
                            allCenter()
                        }
                        Text {
                            attr {
                                fontSize(13f)
                                lineHeight(18f)
                                fontWeightSemisolid()
                                text(ctx.attr.descriptionOfActions)
                                if (getPager().isNightMode()) { color(0xFF89848a) } else { color(0xFF89848a) }
                                textAlignCenter()
                            }
                        }
                    }

                }
                vfor({ctx.attr.actionButtonsAttrs}) { buttonTitleAttr ->
                    View {
                        // line
                        val index = ctx.attr.actionButtonsAttrs.indexOf(buttonTitleAttr)
                        if (!(index== 0 && ctx.attr.descriptionOfActions.isEmpty())) {
                            ctx.createLineView(false).invoke(this)
                        }
                        ctx.createActionButton(buttonTitleAttr, index + 1).invoke(this)
                    }
                }
            }
            View {
                attr {
                    marginBottom(if (pagerData.isIphoneX) 32f else 8f)
                    marginLeft(8f).marginRight(8f)
                    if (getPager().isNightMode()) {
                        backgroundColor(Color.BLACK)
                    } else {
                        backgroundColor(Color.WHITE)
                    }
                    borderRadius(14f)
                }
                ctx.createActionButton(ctx.attr.cancelButtonAttr,0, true).invoke(this)
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
            vif({ctx.showActionSheeting || ctx.attr.showActionSheet}) {
                Modal(ctx.attr.inWindow) {
                    attr {
                        justifyContentFlexEnd() // center all content
                    }
                    // mask
                    TransitionView(type = TransitionType.FADE_IN_OUT) {
                        attr {
                            transitionAppear(ctx.attr.showActionSheet)
                            absolutePositionAllZero()
                            customAnimation(Animation.springEaseInOut(0.3f, 0.9f, 1f))
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
                    TransitionView(type = TransitionType.DIRECTION_FROM_BOTTOM) {
                        attr {
                            transitionAppear(ctx.attr.showActionSheet)
                            customAnimation(Animation.springEaseInOut(0.3f, 0.9f, 1f))
                        }
                        event {
                            transitionFinish { appear ->
                                ctx.showActionSheeting = appear
                                if (!(ctx.showActionSheeting || ctx.attr.showActionSheet)) {
                                    ctx.event.actionSheetDidExitHandlerFn?.invoke()
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
    private fun createActionButton(buttonTitleAttr: ActionButtonTitleAttr, index: Int, isBold: Boolean = false) : ViewBuilder {
        val ctx = this
        return {
            Button {
                attr {
                    height(56f)
                    titleAttr {
                        fontSize(20f)
                        height(24f)
                        if (isBold) fontWeightSemisolid() else fontWeight400()
                        color(Color(0xFF007AFF))
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
