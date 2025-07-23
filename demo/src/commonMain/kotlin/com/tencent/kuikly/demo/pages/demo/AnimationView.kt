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
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.compose.Button

class AnimationBallContainer: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                margin(left = 24f, top = 12f, right = 24f, bottom = 12f)
                border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                height(80f)
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

fun ViewContainer<*, *>.AnimationBallContainer(init: AnimationBallContainer.() -> Unit) {
    addChild(AnimationBallContainer(), init)
}

class AnimationBall: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                borderRadius(30f)
                size(60f, 60f)
                allCenter()
            }
            Text { attr { text("view") } }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

fun ViewContainer<*, *>.AnimationBall(init: AnimationBall.() -> Unit) {
    addChild(AnimationBall(), init)
}

class AnimationControlButtonState() {
    var buttonEnable by observable(true)
    var buttonColor: Color by observable(Color())
    var buttonText: String by observable("播放动画")
}

class AnimationControlButtonAttr(): ComposeAttr() {
    var state by observable(AnimationControlButtonState())
}

class AnimationControlButton: ComposeView<AnimationControlButtonAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Button {
                attr {
                    titleAttr {
                        text(ctx.attr.state.buttonText)
                    }
                    alignSelfCenter()
                    size(width = 80f, height = 32f)
                    marginBottom(12f)
                    touchEnable(ctx.attr.state.buttonEnable)
                    backgroundColor(ctx.attr.state.buttonColor)
                }
            }
        }
    }

    override fun createAttr(): AnimationControlButtonAttr {
        return AnimationControlButtonAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

fun ViewContainer<*, *>.AnimationControlButton(init: AnimationControlButton.() -> Unit) {
    addChild(AnimationControlButton(), init)
}