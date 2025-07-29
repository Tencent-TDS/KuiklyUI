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

package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.AnimationBall
import com.tencent.kuikly.demo.pages.demo.AnimationBallContainer
import com.tencent.kuikly.demo.pages.demo.AnimationControlButton
import com.tencent.kuikly.demo.pages.demo.AnimationControlButtonState
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

internal class NormalAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var animationIndex by observable(0)
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0x9FFF9900)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    attr {
                        backgroundColor(0x9FFF9900)
                        if (ctx.animationIndex == 0) {
                            absolutePosition(top = 10f, left = 10f)
                            animate(
                                animation = Animation.linear(durationS = 1f),
                                value = ctx.animationIndex
                            )
                        } else {
                            absolutePosition(top = 10f, left = ctx.flexNode.layoutWidth - 118f)
                            animate(
                                animation = Animation.linear(durationS = 0.5f),
                                value = ctx.animationIndex
                            )
                        }
                    }
                    event {
                        animationCompletion {
                            ctx.buttonState.buttonColor = Color(0x9FFF9900)
                            ctx.buttonState.buttonEnable = true
                            ctx.buttonState.buttonText = "播放完成"
                        }
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        ctx.animationIndex = (ctx.animationIndex + 1) % 2
                        ctx.buttonState.buttonColor = Color(0xFFD9D9D9)
                        ctx.buttonState.buttonEnable = false
                        ctx.buttonState.buttonText = "播放中"
                    }
                }
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

internal fun ViewContainer<*, *>.NormalAnimationExampleView(init: NormalAnimationExampleView.() -> Unit) {
    addChild(NormalAnimationExampleView(), init)
}

internal class SpringAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var animationIndex by observable(0)
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCC40E0D0)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    attr {
                        backgroundColor(0xCC40E0D0)
                        if (ctx.animationIndex == 0) {
                            absolutePosition(top = 10f, left = 10f)
                            animate(animation = Animation.springEaseInOut(durationS = 1.0f, damping = 2.5f, velocity = 0.5f), value = ctx.animationIndex)
                        } else {
                            absolutePosition(top = 10f, left = ctx.flexNode.layoutWidth - 118f)
                            animate(animation = Animation.springEaseInOut(durationS = 0.5f, damping = 0.5f, velocity = 0.5f), value = ctx.animationIndex)
                        }
                    }
                    event {
                        animationCompletion {
                            ctx.buttonState.buttonColor = Color(0xCC40E0D0)
                            ctx.buttonState.buttonEnable = true
                            ctx.buttonState.buttonText = "播放完成"
                        }
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        ctx.animationIndex = (ctx.animationIndex + 1) % 2
                        ctx.buttonState.buttonColor = Color(0xFFD9D9D9)
                        ctx.buttonState.buttonEnable = false
                        ctx.buttonState.buttonText = "播放中"
                    }
                }
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

internal fun ViewContainer<*, *>.SpringAnimationExampleView(init: SpringAnimationExampleView.() -> Unit) {
    addChild(SpringAnimationExampleView(), init)
}

internal class ParallelAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var isAnimation by observable(false)
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCCFAF0E6)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    attr {

                        // animation1, 右移100
                        absolutePosition(top = 10f, left = if (ctx.isAnimation)  110f else 10f)
                        animate(Animation.linear(1f), value = ctx.isAnimation)

                        // animation2, 旋转90度
                        transform(Rotate(if (ctx.isAnimation) 90f else 0f))
                        animate(Animation.linear(2f), value = ctx.isAnimation)

                        // animation3, 背景色变换
                        backgroundColor(if (ctx.isAnimation) Color.BLUE else Color.YELLOW)
                        animate(Animation.linear(3f, key = "lastFrame"), value = ctx.isAnimation)
                    }
                    event {
                        animationCompletion {
                            if (it.animationKey == "lastFrame") {
                                ctx.buttonState.buttonText = "播放完成"
                            }
                            KLog.i("ParallelAnimationExampleView", "para animation ${it.animationKey}")
                        }
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        ctx.isAnimation = !ctx.isAnimation
                        ctx.buttonState.buttonColor = Color(0xFFD9D9D9)
                        ctx.buttonState.buttonEnable = false
                        ctx.buttonState.buttonText = "播放中"
                    }
                }
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

internal fun ViewContainer<*, *>.ParallelAnimationExampleView(init: ParallelAnimationExampleView.() -> Unit) {
    addChild(ParallelAnimationExampleView(), init)
}

internal class SerialAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var isAnimation by observable(false)
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCCFAF0E6)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    attr {

                        // animation1, 右移100
                        absolutePosition(top = 10f, left = if (ctx.isAnimation)  110f else 10f)
                        animate(Animation.linear(1f), value = ctx.isAnimation)

                        // animation2, 旋转90度
                        transform(Rotate(if (ctx.isAnimation) 90f else 0f))
                        animate(Animation.linear(1f).delay(1f), value = ctx.isAnimation)

                        // animation3, 背景色变换
                        backgroundColor(if (ctx.isAnimation) Color.BLUE else Color.YELLOW)
                        animate(Animation.linear(1f, key = "lastFrame").delay(2f), value = ctx.isAnimation)
                    }
                    event {
                        animationCompletion {
                            if (it.animationKey == "lastFrame") {
                                ctx.buttonState.buttonText = "播放完成"
                            }
                        }
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        ctx.isAnimation = !ctx.isAnimation
                        ctx.buttonState.buttonColor = Color(0xFFD9D9D9)
                        ctx.buttonState.buttonEnable = false
                        ctx.buttonState.buttonText = "播放中"
                    }
                }
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

internal fun ViewContainer<*, *>.SerialAnimationExampleView(init: SerialAnimationExampleView.() -> Unit) {
    addChild(SerialAnimationExampleView(), init)
}

internal class TransformAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var animationIndex by observable(0)
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCC7CFC00)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    attr {
                        backgroundColor(0xCC7CFC00)
                        if (ctx.animationIndex == 0) {
                            margin(left = 10f, top = 10f)
                            transform(Translate(percentageX = 0f, percentageY = 0f))
                            animate(animation = Animation.linear(durationS = 1f), value = ctx.animationIndex)
                        } else {
                            transform(Translate(percentageX = (ctx.flexNode.layoutWidth - 118f) / 60f, percentageY = 0f))
                            animate(animation = Animation.linear(durationS = 0.5f), value = ctx.animationIndex)
                        }
                    }
                    event {
                        animationCompletion {
                            ctx.buttonState.buttonColor = Color(0xCC7CFC00)
                            ctx.buttonState.buttonEnable = true
                            ctx.buttonState.buttonText = "播放完成"
                        }
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        ctx.animationIndex = (ctx.animationIndex + 1) % 2
                        ctx.buttonState.buttonColor = Color(0xFFD9D9D9)
                        ctx.buttonState.buttonEnable = false
                        ctx.buttonState.buttonText = "播放中"
                    }
                }
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

internal fun ViewContainer<*, *>.TransformAnimationExampleView(init: TransformAnimationExampleView.() -> Unit) {
    addChild(TransformAnimationExampleView(), init)
}

internal class DelayAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var isAnimation by observable(false)
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCC7CFC00)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    attr {
                        backgroundColor(0xCC7CFC00)
                        if (!ctx.isAnimation) {
                            transform(Translate(percentageX = 0f, percentageY = 0f))
                        } else {
                            transform(Translate(percentageX = (ctx.flexNode.layoutWidth - 118f) / 60f, percentageY = 0f))
                        }
                        animate(animation = Animation.linear(durationS = 0.5f).delay(3f), value = ctx.isAnimation)

                    }
                    event {
                        animationCompletion {
                            ctx.buttonState.buttonColor = Color(0xCC7CFC00)
                            ctx.buttonState.buttonEnable = true
                            ctx.buttonState.buttonText = "播放完成"
                        }
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        ctx.isAnimation = !ctx.isAnimation
                        ctx.buttonState.buttonColor = Color(0xFFD9D9D9)
                        ctx.buttonState.buttonEnable = false
                        ctx.buttonState.buttonText = "播放中"
                    }
                }
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

internal fun ViewContainer<*, *>.DelayAnimationExampleView(init: DelayAnimationExampleView.() -> Unit) {
    addChild(DelayAnimationExampleView(), init)
}

internal class RepeatForeverAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var isAnimation by observable(false)
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCC7CFC00)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    attr {
                        backgroundColor(0xCC7CFC00)
                        if (!ctx.isAnimation) {
                            margin(left = 10f, top = 10f)
                            transform(Translate(percentageX = 0f, percentageY = 0f))
                            animate(animation = Animation.linear(durationS = 1f).repeatForever(true), value = ctx.isAnimation)
                        } else {
                            transform(Translate(percentageX = (ctx.flexNode.layoutWidth - 118f) / 60f, percentageY = 0f))
                            animate(animation = Animation.linear(durationS = 0.5f).repeatForever(true), value = ctx.isAnimation)
                        }
                    }
                    event {
                        animationCompletion {
                            ctx.buttonState.buttonColor = Color(0xCC7CFC00)
                            ctx.buttonState.buttonEnable = true
                            ctx.buttonState.buttonText = "播放完成"
                        }
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        ctx.isAnimation = !ctx.isAnimation
                        ctx.buttonState.buttonColor = Color(0xFFD9D9D9)
                        ctx.buttonState.buttonEnable = false
                        ctx.buttonState.buttonText = "播放中"
                    }
                }
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

internal fun ViewContainer<*, *>.RepeatForeverAnimationExampleView(init: RepeatForeverAnimationExampleView.() -> Unit) {
    addChild(RepeatForeverAnimationExampleView(), init)
}

internal class AnimationScopeExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var isAnimation1 by observable(false)
    private var isAnimation2 by observable(false)
    private var buttonState1 by observable(AnimationControlButtonState())
    private var buttonState2 by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState1.buttonColor = Color(0xCC7CFC00)
        buttonState2.buttonColor = Color(0xCC7CFC00)

        val ctx = this
        return {
            attr {
                animate(Animation.linear(1f), value = ctx.isAnimation1)
            }
            AnimationBallContainer {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                }
                event {
                    animationCompletion {
                        ctx.buttonState1.buttonColor = Color(0xCC7CFC00)
                        ctx.buttonState1.buttonEnable = true
                        ctx.buttonState1.buttonText = "播放完成"
                    }
                }
                AnimationBall {
                    attr {
                        backgroundColor(if (ctx.isAnimation1) Color.RED else Color.YELLOW)
                        marginLeft(if (ctx.isAnimation1) 100f else 10f)
                    }
                }
                AnimationBall {
                    attr {
                        backgroundColor(if (ctx.isAnimation1) Color.BLUE else Color.YELLOW)
                        marginLeft(10f)
                    }
                }
            }
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        text("animation 绑定父view")
                        margin(left = 20f, right = 12f, bottom = 12f)
                    }
                }
                AnimationControlButton {
                    attr { state = ctx.buttonState1 }
                    event {
                        click {
                            ctx.isAnimation1 = !ctx.isAnimation1
                            ctx.buttonState1.buttonColor = Color(0xFFD9D9D9)
                            ctx.buttonState1.buttonEnable = false
                            ctx.buttonState1.buttonText = "播放中"
                        }
                    }
                }
            }
            AnimationBallContainer {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                }
                AnimationBall {
                    attr {
                        backgroundColor(if (ctx.isAnimation2) Color.RED else Color.YELLOW)
                        marginLeft(if (ctx.isAnimation2) 100f else 10f)
                        animate(Animation.linear(1f), value = ctx.isAnimation2)
                    }
                    event {
                        animationCompletion {
                            ctx.buttonState2.buttonColor = Color(0xCC7CFC00)
                            ctx.buttonState2.buttonEnable = true
                            ctx.buttonState2.buttonText = "播放完成"
                        }
                    }
                }
                AnimationBall {
                    attr {
                        backgroundColor(if (ctx.isAnimation2) Color.BLUE else Color.YELLOW)
                        marginLeft(10f)
                    }
                }
            }
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        text("animation 绑定子view")
                        margin(left = 20f, right = 12f, bottom = 12f)
                    }
                }
                AnimationControlButton {
                    attr { state = ctx.buttonState2 }
                    event {
                        click {
                            ctx.isAnimation2 = !ctx.isAnimation2
                            ctx.buttonState2.buttonColor = Color(0xFFD9D9D9)
                            ctx.buttonState2.buttonEnable = false
                            ctx.buttonState2.buttonText = "播放中"
                        }
                    }
                }
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

internal fun ViewContainer<*, *>.AnimationScopeExampleView(init: AnimationScopeExampleView.() -> Unit) {
    addChild(AnimationScopeExampleView(), init)
}

@Page("")
internal class AnimationExamplePage: BasePager() {
    override fun body(): ViewBuilder {
        return {
            NavBar { attr { title = "Animation Example" } }
            List {
                attr { flex(1f) }
                ViewExampleSectionHeader { attr { title = "Linear Animation" } }
                NormalAnimationExampleView {  }
                ViewExampleSectionHeader { attr { title = "SpringEaseInOut Animation" } }
                SpringAnimationExampleView {  }
                ViewExampleSectionHeader { attr { title = "Parallel Animation" } }
                ParallelAnimationExampleView {  }
                ViewExampleSectionHeader { attr { title = "Serial Animation" } }
                SerialAnimationExampleView {  }
                ViewExampleSectionHeader { attr { title = "Transform Animation" } }
                TransformAnimationExampleView {  }
                ViewExampleSectionHeader { attr { title = "Delay 3s Animation" } }
                DelayAnimationExampleView {  }
                ViewExampleSectionHeader { attr { title = "RepeatForever Animation" } }
                RepeatForeverAnimationExampleView {  }
                ViewExampleSectionHeader { attr { title = "Animation scope" } }
                AnimationScopeExampleView {  }
                View {
                    attr {
                        height(64f)
                    }
                }
            }
        }
    }
}