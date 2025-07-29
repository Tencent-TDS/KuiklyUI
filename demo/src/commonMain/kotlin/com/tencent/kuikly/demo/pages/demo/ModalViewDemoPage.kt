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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ModalViewDemoPage")
internal class ModalViewDemoPage : BasePager() {

    var showModal: Boolean  by observable(false)
    var callbackRef: CallbackRef = ""

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun body(): ViewBuilder {
        var ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF3c6cbdL))

            }
            // 背景图
            Image {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                    src("https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg")
                }
            }
            // navBar
            NavBar {
                attr {
                    title = "Modal组件Demo"
                }
            }
            List {
                attr {
                    flex(1f)
                }
                View {
                    attr {
                        height(200f)
                        backgroundColor(Color.RED)
                    }
                }

                View {
                    attr {
                        height(200f)
                        backgroundColor(Color.BLUE)
                    }
                }

                View {
                    attr {
                        height(200f)
                        backgroundColor(Color.BLACK)

                        allCenter()
                    }

                    Button {
                        attr {
                            if (ctx.showModal) {
                                titleAttr {
                                    text("did点击展示modal")
                                }
                            } else {
                                titleAttr {
                                    text("can点击展示modal")
                                }
                            }

                            backgroundColor(Color.BLUE)
                            width(120f)
                            height(50f)
                            borderRadius(10f)
                        }
                        event {
                            click {
                                ctx.showModal = true
                                KLog.i("ModalViewDemoPage", "Button click")
                            }
                        }
                    }

                    vif({ctx.showModal}) {
                        Modal {
                            ActionSheet {
                                event {
                                    close {
                                        KLog.i("ModalViewDemoPage", "ActionSheet close")
                                        ctx.showModal = false
                                    }
                                }
                            }
                        }
                    }
                }

                View {
                    attr {
                        height(200f)
                        backgroundColor(Color.GREEN)
                    }
                }
            }
        }
    }

    override fun created() {
        super.created()
        //
        val module =  getPager().acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
        val eventName = "TestKTVEvent"
        callbackRef =module.addNotify(eventName) {
            KLog.i("ModalViewDemoPage", "收到通知了$it")
        }

        setTimeout(pagerId, 2000) {
            val data = JSONObject()
            data.put("key", "22")
            module.postNotify(eventName, data)
        }

        setTimeout(pagerId, 10 * 1000) {
            module.removeNotify(eventName, callbackRef)
            module.postNotify(eventName, JSONObject())
        }

    }

}

internal class ActionSheetView : ComposeView<ComposeAttr, ActionSheetEvent>() {
    var animated: Boolean  by observable(false)
    var didFirstLayout = false
    override fun body(): ViewBuilder {
        var ctx = this
        return {
            attr {
                absolutePosition(0f, 0f, 0f, 0f)
                justifyContentFlexEnd()
                if (ctx.animated) {
                    backgroundColor(Color(0, 0, 0, 0.5f))
                } else {
                    backgroundColor(Color(0, 0, 0, 0f))
                }
                animation(Animation.springEaseIn(0.5f, 0.92f, 1f), ctx.animated)

                // 三生三世

            }

            event {
                click {
                    ctx.animated = false
                }
                animationCompletion {
                    if (!ctx.animated) {
                        KLog.i("ActionSheetView", "animationCompletion $it")
                        this@ActionSheetView.emit(ActionSheetEvent.CLOSE, it)
                    }
                }
            }

            View {
                attr {
                    backgroundColor(Color.WHITE)
                    paddingBottom(30f)
                    if (ctx.animated) {
                        transform(Translate(0f, 0f))
                    } else {
                        transform(Translate(0f, 1f))
                    }
                    animation(Animation.springEaseIn(0.5f, 0.92f, 1f), ctx.animated)
                }

                Button {
                    attr {
                        margin(20f)
                        marginBottom(0f)
                        backgroundColor(Color.BLUE)
                        height(50f)
                        borderRadius(25f)
                        titleAttr {
                            text("Item0")
                            fontSize(20f)
                            color(Color.WHITE)
                        }
                    }
                }
                Button {
                    attr {
                        margin(20f)
                        marginBottom(0f)
                        backgroundColor(Color.BLUE)
                        height(50f)
                        borderRadius(25f)
                        titleAttr {
                            text("Item1")
                            fontSize(20f)
                            color(Color.WHITE)
                        }
                    }
                }

                Button {
                    attr {
                        margin(20f)
                        marginBottom(0f)
                        backgroundColor(Color.BLUE)
                        height(50f)
                        borderRadius(25f)
                        titleAttr {
                            text("Item2")
                            fontSize(20f)
                            color(Color.WHITE)
                        }
                    }
                }

                Button {
                    attr {
                        margin(20f)
                        marginBottom(0f)
                        backgroundColor(Color.BLUE)
                        height(50f)
                        borderRadius(25f)
                        titleAttr {
                            text("Item3")
                            fontSize(20f)
                            color(Color.WHITE)
                        }
                    }
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ActionSheetEvent {
        return ActionSheetEvent()
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        animated = true
    }

}

internal class ActionSheetEvent : ComposeEvent() {
    fun close(handler: EventHandlerFn) {
        registerEvent(CLOSE, handler)
    }

    companion object {
        const val CLOSE = "close"
    }
}

internal fun ViewContainer<*, *>.ActionSheet(init: ActionSheetView.() -> Unit) {
    addChild(ActionSheetView(), init)
}
