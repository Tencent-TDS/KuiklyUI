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

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.reactive.handler.*
internal class EasyCardAttr : ComposeAttr() {
    lateinit var listItem : ListItem
}

internal class EasyCardEvent : ComposeEvent() {

    fun titleDidClick(event: EventHandlerFn) {
        registerEvent(TITLE_DID_CLICKED, event)
    }

    companion object {
        const val TITLE_DID_CLICKED = "titleDidClick"
    }
}

internal class EasyCardView : ComposeView<EasyCardAttr, EasyCardEvent>() {
    var didClick : Boolean by observable(false)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    backgroundColor(Color.WHITE)
                }
                vif({ctx.didClick}) {
                    View {
                        attr {
                            height(100f)
                        }
                    }
                }

                Text {
                    attr {
                        margin(20f)
                        backgroundColor(Color.BLUE)
                        text("title: ${ctx.attr.listItem.title}" )
                        fontSize(20f)
                        color(Color.BLACK)
                    }
                    event {
                        click {
                            // 响应自定义事件
                            ctx.didClick = !ctx.didClick
                            this@EasyCardView.onFireEvent(EasyCardEvent.TITLE_DID_CLICKED, it as JSONObject)
                        }

                        didAppear {
                            ctx.attr.listItem.lifeCycleTitle = "didAppear"
                         //   Utils.logToNative(pagerId, "lifeCycle:didAppear")
                        }
                        willAppear {
                            ctx.attr.listItem.lifeCycleTitle = "willAppear"
                        }
                        willDisappear {
                            ctx.attr.listItem.lifeCycleTitle = "willDisappear"
                        }
                        didDisappear {
                          //  Utils.logToNative(pagerId, "lifeCycle:didDisappear")
                            ctx.attr.listItem.lifeCycleTitle = "didDisappear"
                        }
                    }
                }
                Text {
                    attr {
                        margin(10f)
                        text("蓝色矩形生命周期: ${ctx.attr.listItem.lifeCycleTitle}" )
                        fontSize(20f)
                        color(Color.BLACK)
                    }
                    event {
                        click {
                            // 响应自定义事件
                            this@EasyCardView.onFireEvent(EasyCardEvent.TITLE_DID_CLICKED, JSONObject())
                        }

                    }
                }
                View {
                    attr {
                        height(1f)
                        backgroundColor(Color(0,0, 0, 0.5f))
                    }
                }
            }
        }
    }

    override fun createAttr(): EasyCardAttr {
        return EasyCardAttr()
    }

    override fun createEvent(): EasyCardEvent {
        return EasyCardEvent()
    }
}

internal fun ViewContainer<*, *>.EasyCard(init: EasyCardView.() -> Unit) {
    addChild(EasyCardView(), init)
}