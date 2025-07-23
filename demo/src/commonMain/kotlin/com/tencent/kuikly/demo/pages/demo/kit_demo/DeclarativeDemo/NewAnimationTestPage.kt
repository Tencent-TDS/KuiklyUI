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
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Rotate
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("TestNewAnimationPage1")
internal class TestNewAnimationPage1: BasePager() {
    private var isAnimation1 by observable(false)
    private var isAnimation2 by observable(false)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                alignItemsFlexStart()
                justifyContentCenter()
                animate(Animation.linear(1f), value = ctx.isAnimation1)
            }
            View {
                attr {
                    size(100f, 100f)
                    backgroundColor(Color.RED)
                    animate(Animation.linear(2f), value = ctx.isAnimation2)
                    transform(translate = Translate(if (ctx.isAnimation1) 3f else 0f, 0f))
                    animate(Animation.linear(3f), value = ctx.isAnimation1)
                    animate(Animation.linear(2f), value = ctx.isAnimation1)
                }
            }
            View {
                attr {
                    size(100f, 100f)
                    backgroundColor(Color.YELLOW)
                    transform(translate = Translate(if (ctx.isAnimation1) 3f else 0f, 0f))
                }
            }
            event {
                click {
                    ctx.isAnimation1 = !ctx.isAnimation1
                }
            }
        }
    }
}

// 测试作用域
@Page("TestNewAnimationPage2")
internal class TestNewAnimationPage2: BasePager() {
    private var isAnimation1 by observable(false)
    private var isAnimation2 by observable(false)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                alignItemsFlexStart()
                justifyContentCenter()
                animate(Animation.linear(1f), value = ctx.isAnimation1)
            }
            View {
                attr {
                    size(100f, 100f)
                    transform(translate = Translate(if (ctx.isAnimation1) 3f else 0f, 0f))
                    animate(Animation.linear(3f), value = ctx.isAnimation1)
                    animate(Animation.linear(5f), value = ctx.isAnimation2)
                    backgroundColor(if (ctx.isAnimation1) Color.YELLOW else Color.RED)
                    animate(Animation.linear(2f), value = ctx.isAnimation1)
                }
            }
            View {
                attr {
                    size(100f, 100f)
                    backgroundColor(Color.YELLOW)
                    transform(translate = Translate(if (ctx.isAnimation1) 3f else 0f, 0f))
                }
            }
            event {
                click {
                    ctx.isAnimation1 = !ctx.isAnimation1
                }
            }
        }
    }
}

// 测试布局动画能力
@Page("TestNewAnimationPage3")
internal class TestNewAnimationPage3: BasePager() {
    private var isAnimation1 by observable(false)
    private var isAnimation2 by observable(false)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                alignItemsFlexStart()
                justifyContentCenter()
                animate(Animation.linear(1f), value = ctx.isAnimation1)
            }
            View {
                attr {
                    size(100f, 100f)
                    marginLeft(if(ctx.isAnimation1) 300f else 20f)
                    animate(Animation.linear(3f), value = ctx.isAnimation1)
                    animate(Animation.linear(5f), value = ctx.isAnimation2)
                    backgroundColor(if (ctx.isAnimation1) Color.YELLOW else Color.RED)
                    animate(Animation.linear(1f), value = ctx.isAnimation1)
                }
            }
            View {
                attr {
                    size(100f, 100f)
                    animate(Animation.linear(5f), value = ctx.isAnimation2)
                    backgroundColor(if (ctx.isAnimation1) Color.YELLOW else Color.RED)
                    animate(Animation.linear(1f), value = ctx.isAnimation1)
                    marginLeft(if(ctx.isAnimation1) 300f else 20f)
                    animate(Animation.linear(2f), value = ctx.isAnimation1)
                }
            }
            View {
                attr {
                    size(100f, 100f)
                    backgroundColor(Color.YELLOW)
                    marginLeft(if(ctx.isAnimation1) 300f else 20f)
                }
            }
            event {
                click {
                    ctx.isAnimation1 = !ctx.isAnimation1
                }
            }
        }
    }
}

// 测试布局动画的作用域
@Page("TestNewAnimationPage4")
internal class TestNewAnimationPage4: BasePager() {
    private var isAnimation1 by observable(false)
    private var isAnimation2 by observable(false)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                allCenter()
                // 父亲节点有定义动画，第二个子节点的位置和颜色，有动画
                animate(Animation.linear(1f), value = ctx.isAnimation1)
            }
            View {
                attr {
                    size(100f, if(ctx.isAnimation1) 200f else 100f)
                    backgroundColor(if (ctx.isAnimation1) Color.YELLOW else Color.RED)
                    animate(Animation.linear(10f), value = ctx.isAnimation2)
                }
            }
            View {
                attr {
                    size(100f, 100f)
                    animate(Animation.linear(10f), value = ctx.isAnimation2)
                    backgroundColor(if (ctx.isAnimation1) Color.RED else Color.YELLOW)
                }
            }
            event {
                click {
                    ctx.isAnimation1 = !ctx.isAnimation1
                }
            }
        }
    }
}

// 测试布局动画的作用域
@Page("TestNewAnimationPage5")
internal class TestNewAnimationPage5: BasePager() {
    private var isAnimation1 by observable(false)
    private var isAnimation2 by observable(false)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                allCenter()
                // 父亲节点没有定义动画，第二个view子节点的颜色和布局变化，无动画
//                animationS(Animation.linear(1f), value = ctx.isAnimation1)
            }
            View {
                attr {
                    size(100f, if(ctx.isAnimation1) 200f else 100f)
                    backgroundColor(if (ctx.isAnimation1) Color.YELLOW else Color.RED)
                    animate(Animation.linear(1f), value = ctx.isAnimation1)
                    animate(Animation.linear(10f), value = ctx.isAnimation2)
                }
            }
            View {
                attr {
                    size(100f, 100f)
                    animate(Animation.linear(10f), value = ctx.isAnimation2)
                    backgroundColor(if (ctx.isAnimation1) Color.RED else Color.YELLOW)
                }
            }
            event {
                click {
                    ctx.isAnimation1 = !ctx.isAnimation1
                }
            }
        }
    }
}

// 测试同时有多个property变化的场景（变化过程中，attr里面，又修改了属性)
@Page("TestNewAnimationPage6")
internal class TestNewAnimationPage6: BasePager() {
    private var isAnimation1 by observable(false)
    private var isAnimation2 by observable(false)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                alignItemsFlexStart()
                justifyContentCenter()
            }
            View {
                attr {
                    if (ctx.isAnimation1) {
                        size(300f, 100f)
                        backgroundColor(Color.YELLOW)

                        if (!ctx.isAnimation2) {
                            ctx.isAnimation2 = true
                        }
                    } else {
                        size(100f, 100f)
                        backgroundColor(Color.RED)
                    }
                    animate(Animation.linear(2f), value = ctx.isAnimation1)
                }
            }
            View {
                attr {
                    if (ctx.isAnimation2) {
                        size(300f, 100f)
                        backgroundColor(Color.YELLOW)
                    } else {
                        size(100f, 100f)
                        backgroundColor(Color.RED)
                    }
                    animate(Animation.linear(2f), value = ctx.isAnimation2)
                }
            }
            event {
                click {
                    ctx.isAnimation1 = !ctx.isAnimation1
//                    ctx.isAnimation2 = !ctx.isAnimation2
                }
            }
        }
    }
}

// 测试回调事件
@Page("TestNewAnimationPage7")
internal class TestNewAnimationPage7: BasePager() {
    private var isAnimation1 by observable(false)
    private var isAnimation2 by observable(false)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                alignItemsFlexStart()
                justifyContentCenter()
                animate(Animation.linear(1f, "abcAnim"), value = ctx.isAnimation1)
            }
            View {
                attr {
                    if (ctx.isAnimation1) {
                        size(300f, 100f)
                        transform(Scale(1.5f, 1.5f))
                        backgroundColor(Color.YELLOW)
                    } else {
                        size(100f, 100f)
                        transform(Scale(1f, 1f))
                        backgroundColor(Color.RED)
                    }
                }
                event {
                    animationCompletion {
                        KLog.i("xxxx", "end animation inner. attr: ${it.attr}, key: ${it.animationKey}")
                    }
                }
            }
            event {
                click {
                    ctx.isAnimation1 = !ctx.isAnimation1
                }
                animationCompletion {
                    KLog.i("xxxx", "end animation outer. attr: ${it.attr}, key: ${it.animationKey}")
                }
            }
        }
    }
}

// 测试iOS旋转动画的影响
@Page("TestNewAnimationPage8")
internal class TestNewAnimationPage8: BasePager() {
    private var animationIndex by observable(0)
    private var isAnimation2 by observable(false)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }
            View {
                attr {
                    alignItemsCenter()
                    justifyContentFlexStart()
                    backgroundColor(Color.YELLOW)
                    size(100f, 100f)
                    if (ctx.animationIndex%2 == 1) {
                        transform(rotate = Rotate(200f))
                    } else {
                        transform(rotate = Rotate(0f))
//                        animationS(animation = Animation.springEaseInOut(durationS = 1.0f, damping = 2.5f, velocity = 0.5f), value = ctx.animationIndex)
                        animate(animation = Animation.springEaseInOut(durationS = 0.5f, damping = 0.5f, velocity = 0.5f), value = ctx.animationIndex)
                    }
                }
                event {
                    animationCompletion {
                        KLog.i("xxxx", "end animation inner. attr: ${it.attr}, key: ${it.animationKey}")
                    }
                }
                View {
                    attr {
                        size(10f, 10f)
                        backgroundColor(Color.BLUE)
                    }
                }
            }
            event {
                click {
                    ctx.animationIndex ++
                }
                animationCompletion {
                    KLog.i("xxxx", "end animation outer. attr: ${it.attr}, key: ${it.animationKey}")
                }
            }
        }
    }
}