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
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

internal class NormalImperativeAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var viewRef: ViewRef<AnimationBall>? = null
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0x9FFF9900)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    ref {
                        ctx.viewRef = it
                    }
                    attr {
                        backgroundColor(0x9FFF9900)
                        absolutePosition(top = 10f, left = 10f)
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        if (ctx.buttonState.buttonText == "播放动画") {
                            ctx.viewRef?.view?.animateToAttr(Animation.linear(durationS = 1f), attrBlock = {
                                transform(Translate(0f, offsetX = ctx.flexNode.layoutWidth - 128f))
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0x9FFF9900)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放完成"
                            })
                        } else if (ctx.buttonState.buttonText == "播放完成") {
                            ctx.viewRef?.view?.animateToAttr(Animation.linear(durationS = 0.5f), attrBlock = {
                                transform(Translate(0f, offsetX = 0f))
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0x9FFF9900)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放动画"
                            })
                        }
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

internal fun ViewContainer<*, *>.NormalImperativeAnimation(init: NormalImperativeAnimationExampleView.() -> Unit) {
    addChild(NormalImperativeAnimationExampleView(), init)
}

internal class SpringImperativeAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var viewRef: ViewRef<AnimationBall>? = null
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCC40E0D0)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    ref {
                        ctx.viewRef = it
                    }
                    attr {
                        backgroundColor(0xCC40E0D0)
                        absolutePosition(top = 10f, left = 10f)
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        if (ctx.buttonState.buttonText == "播放动画") {
                            ctx.viewRef?.view?.animateToAttr(animation = Animation.springEaseInOut(
                                durationS = 1.0f,
                                damping = 2.5f,
                                velocity = 0.5f
                            ), attrBlock = {
                                transform(Translate(0f, offsetX = ctx.flexNode.layoutWidth - 128f))
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0xCC40E0D0)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放完成"
                            })
                        } else if (ctx.buttonState.buttonText == "播放完成") {
                            ctx.viewRef?.view?.animateToAttr(animation = Animation.springEaseInOut(
                                durationS = 0.5f,
                                damping = 0.5f,
                                velocity = 0.5f
                            ), attrBlock = {
                                transform(Translate(0f, offsetX = 0f))
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0xCC40E0D0)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放动画"
                            })
                        }
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

internal fun ViewContainer<*, *>.SpringImperativeAnimation(init: SpringImperativeAnimationExampleView.() -> Unit) {
    addChild(SpringImperativeAnimationExampleView(), init)
}

internal class ParallelImperativeAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var viewRef: ViewRef<AnimationBall>? = null
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCCFAF0E6)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    ref {
                        ctx.viewRef = it
                    }
                    attr {
                        backgroundColor(Color.YELLOW)
                        absolutePosition(top = 10f, left = 10f)
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        if (ctx.buttonState.buttonText == "播放动画") {
                            ctx.viewRef?.view?.animateToAttr(animation = Animation.linear(
                                durationS = 2.0f
                            ), attrBlock = {
                                transform(rotate = Rotate(90f),
                                    translate = Translate(0f, offsetX = 100f))
                            })
                            ctx.viewRef?.view?.animateToAttr(animation = Animation.linear(
                                durationS = 3.0f
                            ), attrBlock = {
                                backgroundColor(Color.BLUE)
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0xCCFAF0E6)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放完成"
                            })
                        } else if (ctx.buttonState.buttonText == "播放完成") {
                            ctx.viewRef?.view?.animateToAttr(animation = Animation.linear(
                                durationS = 0f
                            ), attrBlock = {
                                transform(rotate = Rotate(0f),
                                    translate = Translate(0f, offsetX = 0f))
                                backgroundColor(Color.YELLOW)
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0xCCFAF0E6)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放动画"
                            })
                        }
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

internal fun ViewContainer<*, *>.ParallelImperativeAnimation(init: ParallelImperativeAnimationExampleView.() -> Unit) {
    addChild(ParallelImperativeAnimationExampleView(), init)
}

internal class SerialImperativeAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var viewRef: ViewRef<AnimationBall>? = null
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCCFAF0E6)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    ref {
                        ctx.viewRef = it
                    }
                    attr {
                        backgroundColor(Color.YELLOW)
                        absolutePosition(top = 10f, left = 10f)
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        if (ctx.buttonState.buttonText == "播放动画") {
                            ctx.viewRef?.view?.animateToAttr(animation = Animation.linear(
                                durationS = 1.0f
                            ), attrBlock = {
                                transform(Translate(0f, offsetX = 100f))
                            })
                            ctx.viewRef?.view?.animateToAttr(animation = Animation.linear(
                                durationS = 1.0f
                            ).delay(2f), attrBlock = {
                                backgroundColor(Color.BLUE)
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0xCCFAF0E6)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放完成"
                            })
                        } else if (ctx.buttonState.buttonText == "播放完成") {
                            ctx.viewRef?.view?.animateToAttr(animation = Animation.linear(
                                durationS = 0f
                            ), attrBlock = {
                                transform(Translate(0f))
                                backgroundColor(Color.YELLOW)
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0xCCFAF0E6)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放动画"
                            })
                        }
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

internal fun ViewContainer<*, *>.SerialImperativeAnimation(init: SerialImperativeAnimationExampleView.() -> Unit) {
    addChild(SerialImperativeAnimationExampleView(), init)
}

internal class DelayImperativeAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var viewRef: ViewRef<AnimationBall>? = null
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCC7CFC00)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    ref {
                        ctx.viewRef = it
                    }
                    attr {
                        backgroundColor(0xCC7CFC00)
                        absolutePosition(top = 10f, left = 10f)
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        if (ctx.buttonState.buttonText == "播放动画") {
                            ctx.viewRef?.view?.animateToAttr(Animation.linear(durationS = 1f).delay(3f), attrBlock = {
                                transform(Translate(0f, offsetX = ctx.flexNode.layoutWidth - 128f))
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0xCC7CFC00)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放完成"
                            })
                        } else if (ctx.buttonState.buttonText == "播放完成") {
                            ctx.viewRef?.view?.animateToAttr(Animation.linear(durationS = 0.5f), attrBlock = {
                                transform(Translate(0f, offsetX = 0f))
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0x9FFF9900)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放动画"
                            })
                        }
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

internal fun ViewContainer<*, *>.DelayImperativeAnimation(init: DelayImperativeAnimationExampleView.() -> Unit) {
    addChild(DelayImperativeAnimationExampleView(), init)
}

internal class RepeatForeverImperativeAnimationExampleView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var viewRef: ViewRef<AnimationBall>? = null
    private var buttonState by observable(AnimationControlButtonState())

    override fun body(): ViewBuilder {
        buttonState.buttonColor = Color(0xCC7CFC00)
        val ctx = this
        return {
            AnimationBallContainer {
                AnimationBall {
                    ref {
                        ctx.viewRef = it
                    }
                    attr {
                        backgroundColor(0xCC7CFC00)
                        absolutePosition(top = 10f, left = 10f)
                    }
                }
            }
            AnimationControlButton {
                attr { state = ctx.buttonState }
                event {
                    click {
                        if (ctx.buttonState.buttonText == "播放动画") {
                            ctx.viewRef?.view?.animateToAttr(Animation.linear(durationS = 1f).repeatForever(true), attrBlock = {
                                transform(Translate(0f, offsetX = ctx.flexNode.layoutWidth - 128f))
                            }, completion = {
                                ctx.buttonState.buttonColor = Color(0xCC7CFC00)
                                ctx.buttonState.buttonEnable = true
                                ctx.buttonState.buttonText = "播放完成"
                            })
                        }
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

internal fun ViewContainer<*, *>.RepeatForeverImperativeAnimation(init: RepeatForeverImperativeAnimationExampleView.() -> Unit) {
    addChild(RepeatForeverImperativeAnimationExampleView(), init)
}

@Page("ImperativeAnimationExamplePage")
internal class ImperativeAnimationExamplePage: BasePager() {
    override fun body(): ViewBuilder {
        return {
            NavBar { attr { title = "Imperative Animation Example" } }
            List {
                attr { flex(1f) }
                ViewExampleSectionHeader { attr { title = "Linear Animation" } }
                NormalImperativeAnimation {  }
                ViewExampleSectionHeader { attr { title = "SpringEaseInOut Animation" } }
                SpringImperativeAnimation {  }
                ViewExampleSectionHeader { attr { title = "Parallel Animation" } }
                ParallelImperativeAnimation {  }
                ViewExampleSectionHeader { attr { title = "Serial Animation" } }
                SerialImperativeAnimation {  }
                ViewExampleSectionHeader { attr { title = "Delay 3s Animation" } }
                DelayImperativeAnimation {  }
                ViewExampleSectionHeader { attr { title = "RepeatForever Animation" } }
                RepeatForeverImperativeAnimation {  }
                View {
                    attr {
                        height(64f)
                    }
                }
            }
        }
    }
}
