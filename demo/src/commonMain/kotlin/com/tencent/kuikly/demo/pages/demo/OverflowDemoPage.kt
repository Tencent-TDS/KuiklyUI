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
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 溢出显示综合示例
 * 展示各种场景下的溢出行为
 */
@Page("OverflowDemoPage")
internal class OverflowDemoPage : BasePager() {

    // 用于控制动画状态
    private var animationIndex by observable(0)
    private var springAnimationIndex by observable(0)
    private var borderRadiusFlag by observable(true)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }

            NavBar {
                attr {
                    title = "溢出显示综合示例"
                }
            }

            List {
                attr {
                    flex(1f)
                }

                // 1. Transform后超出边界
                SectionTitle { attr { text("1. Transform后超出边界") } }
                View {
                    attr {
                        margin(10f)
                        height(150f)
                        backgroundColor(Color(0xFFEEEEEE))
                        borderRadius(10f)
                        allCenter()
                    }
                    View {
                        attr {
                            size(100f, 100f)
                            backgroundColor(Color.GRAY)
                        }
                        View {
                            attr {
                                absolutePositionAllZero()
                                backgroundColor(Color(0xFF4A90E2))
                                borderRadius(10f)
                                transform(Rotate(45f)) // 旋转45度，可能超出边界
                            }
                        }
                    }
                }

                // 2. BoxShadow溢出
                SectionTitle { attr { text("2. BoxShadow溢出") } }
                View {
                    attr {
                        margin(10f)
                        height(150f)
                        backgroundColor(Color(0xFFEEEEEE))
                        borderRadius(10f)
                        flexDirectionRow()
                        justifyContentSpaceEvenly()
                        alignItemsCenter()
                    }
                    View {
                        attr {
                            size(130f, 130f)
                            backgroundColor(Color.GRAY)
                            allCenter()
                        }
                        View {
                            attr {
                                size(120f, 120f)
                                backgroundColor(Color.WHITE)
                                borderRadius(10f)
                                boxShadow(
                                    BoxShadow(
                                        offsetX = 3f,
                                        offsetY = 3f,
                                        shadowRadius = 9f,
                                        shadowColor = Color(0xFF4A90E2)
                                    )
                                )
                            }
                        }
                    }
                    View {
                        attr {
                            size(130f, 130f)
                            backgroundColor(Color.GRAY)
                            allCenter()
                        }
                        View {
                            attr {
                                size(120f, 120f)
                                backgroundColor(Color.WHITE)
                                borderRadius(10f)
                                boxShadow(
                                    BoxShadow(
                                        offsetX = 10f,
                                        offsetY = 10f,
                                        shadowRadius = 30f,
                                        shadowColor = Color(0xFFFF6B6B)
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. 绝对定位超出边界
                SectionTitle { attr { text("3. 绝对定位超出边界") } }
                View {
                    attr {
                        margin(10f)
                        height(150f)
                        backgroundColor(Color(0xFFEEEEEE))
                        borderRadius(10f)
                        allCenter()
                    }
                    View {
                        attr {
                            size(100f, 100f)
                            backgroundColor(Color.GRAY)
                            allCenter()
                        }
                        View {
                            attr {
                                size(80f, 80f)
                                backgroundColor(Color.WHITE)
                            }
                            View {
                                attr {
                                    size(40f, 40f)
                                    backgroundColor(Color(0xFFFF6B6B))
                                    borderRadius(20f)
                                    absolutePosition(top = -20f, right = -20f) // 部分在边界外
                                }
                            }
                        }
                    }
                }

                // 4. 做动画后超出边界
                SectionTitle { attr { text("4. 动画后超出边界") } }
                View {
                    attr {
                        margin(10f)
                        height(150f)
                        backgroundColor(Color(0xFFEEEEEE))
                        borderRadius(10f)
                        allCenter()
                    }
                    View {
                        attr {
                            size(300f, 100f)
                            backgroundColor(Color.GRAY)
                        }
                        View {
                            attr {
                                backgroundColor(Color(0xFFE67E22))
                                if (ctx.animationIndex == 0) {
                                    borderRadius(25f)
                                    size(50f, 50f)
                                    absolutePosition(left = 25f, top = 25f)
                                } else {
                                    borderRadius(75f)
                                    size(150f, 150f)
                                    absolutePosition(left = 250f, top = -25f)
                                }
                                animate(
                                    Animation.linear(durationS = 1f),
                                    value = ctx.animationIndex
                                )
                            }
                        }
                    }
                }
                View {
                    attr {
                        margin(10f)
                        height(150f)
                        backgroundColor(Color(0xFFEEEEEE))
                        borderRadius(10f)
                        allCenter()
                    }
                    View {
                        attr {
                            size(300f, 100f)
                            backgroundColor(Color.GRAY)
                            justifyContentCenter()
                            alignItemsFlexStart()
                        }
                        View {
                            attr {
                                size(50f, 50f)
                                borderRadius(25f)
                                marginLeft(25f)
                                backgroundColor(Color(0xFF27AE60))
                                if (ctx.animationIndex == 0) {
                                    transform(
                                        scale = Scale(0.8f, 0.8f),
                                        translate = Translate(0f, 0f)
                                    )
                                } else {
                                    transform(
                                        scale = Scale(3f, 3f),
                                        translate = Translate(5f, 0f)
                                    )
                                }
                                animate(
                                    Animation.linear(durationS = 1f),
                                    value = ctx.animationIndex
                                )
                            }
                        }
                    }
                }
                View {
                    attr {
                        allCenter()
                        margin(10f)
                    }
                    Button {
                        attr {
                            width(200f)
                            height(40f)
                            backgroundColor(Color(0xFF4A90E2))
                            borderRadius(20f)
                            highlightBackgroundColor(Color(0x33000000))
                            alignSelfCenter()
                            titleAttr {
                                text("点击播放动画")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                ctx.animationIndex = (ctx.animationIndex + 1) % 2
                            }
                        }
                    }
                }

                // 5. 弹性动画的中间状态超出边界
                SectionTitle { attr { text("5. 弹性动画中间状态超出边界") } }
                View {
                    attr {
                        margin(10f)
                        height(150f)
                        backgroundColor(Color(0xFFEEEEEE))
                        borderRadius(10f)
                        allCenter()
                    }
                    View {
                        attr {
                            size(300f, 100f)
                            backgroundColor(Color.GRAY)
                        }
                        View {
                            attr {
                                size(50f, 50f)
                                borderRadius(25f)
                                backgroundColor(Color(0xFFE67E22))
                                if (ctx.springAnimationIndex == 0) {
                                    absolutePosition(left = 25f, top = 25f)
                                } else {
                                    absolutePosition(left = 225f, top = 25f)
                                }
                                animate(
                                    Animation.springEaseInOut(
                                        durationS = 2f,
                                        damping = 0.2f, // 低阻尼
                                        velocity = 0.5f
                                    ),
                                    value = ctx.springAnimationIndex
                                )
                            }
                        }
                    }
                }
                View {
                    attr {
                        margin(10f)
                        height(150f)
                        backgroundColor(Color(0xFFEEEEEE))
                        borderRadius(10f)
                        allCenter()
                    }
                    View {
                        attr {
                            size(300f, 100f)
                            backgroundColor(Color.GRAY)
                            allCenter()
                        }
                        View {
                            attr {
                                size(90f, 90f)
                                borderRadius(45f)
                                backgroundColor(Color(0xFF27AE60))
                                if (ctx.springAnimationIndex == 0) {
                                    transform(Scale(0.2f, 0.2f))
                                } else {
                                    transform(Scale(1f, 1f))
                                }
                                animate(
                                    Animation.springEaseInOut(
                                        durationS = 2f,
                                        damping = 0.2f, // 低阻尼
                                        velocity = 0.5f
                                    ),
                                    value = ctx.springAnimationIndex
                                )
                            }
                        }
                    }
                }
                View {
                    attr {
                        allCenter()
                        margin(10f)
                    }
                    Button {
                        attr {
                            width(200f)
                            height(40f)
                            backgroundColor(Color(0xFF4A90E2))
                            borderRadius(20f)
                            highlightBackgroundColor(Color(0x33000000))
                            titleAttr {
                                text("点击播放弹性动画")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                ctx.springAnimationIndex = (ctx.springAnimationIndex + 1) % 2
                            }
                        }
                    }
                }

                // 6. BorderRadius阻断溢出
                SectionTitle { attr { text("6. BorderRadius阻断溢出") } }
                View {
                    attr {
                        margin(10f)
                        height(150f)
                        backgroundColor(Color(0xFFEEEEEE))
                        borderRadius(10f)
                        allCenter()
                    }
                    View {
                        attr {
                            width(120f)
                            height(80f)
                            backgroundColor(Color(0xFF1ABC9C))
                            if (ctx.borderRadiusFlag) {
                                borderRadius(
                                    BorderRectRadius(
                                        topLeftCornerRadius = 40f,
                                        topRightCornerRadius = 5f,
                                        bottomLeftCornerRadius = 5f,
                                        bottomRightCornerRadius = 40f
                                    )
                                )
                            } else {
                                borderRadius(0f)
                            }
                            overflow(false) // 不同圆角值也会阻断溢出
                            justifyContentCenter()
                            alignItemsCenter()
                        }
                        View {
                            attr {
                                size(40f, 40f)
                                backgroundColor(Color(0xFFE67E22))
                                borderRadius(20f)
                                absolutePosition(top = -10f, left = -10f)
                            }
                        }
                    }
                }
                View {
                    attr {
                        allCenter()
                        margin(10f)
                    }
                    Button {
                        attr {
                            width(200f)
                            height(40f)
                            backgroundColor(Color(0xFF4A90E2))
                            borderRadius(20f)
                            highlightBackgroundColor(Color(0x33000000))
                            titleAttr {
                                text("点击切换BorderRadius")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                ctx.borderRadiusFlag = !ctx.borderRadiusFlag
                            }
                        }
                    }
                }

                // 占位，测试List重建后overflow是否正确
                View {
                    attr {
                        height(ctx.pageData.pageViewHeight * 2)
                        allCenter()
                        overflow(true)
                    }
                    View {
                        attr {
                            flex(1f)
                            width(5f)
                            backgroundColor(Color.BLACK)
                            transform(Rotate(10f))
                        }
                    }
                }
            }
        }
    }
}

// 辅助组件：章节标题
private class SectionTitle : ComposeView<SectionTitleAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr {
                    text(ctx.attr.text)
                    fontSize(16f)
                    color(Color(0xFF333333))
                    marginTop(10f)
                    marginLeft(10f)
                    fontWeight700()
                }
            }
        }
    }

    override fun createAttr(): SectionTitleAttr {
        return SectionTitleAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

private class SectionTitleAttr : ComposeAttr() {
    var text by observable("")
    fun text(text: String) {
        this.text = text
    }
}

private fun ViewContainer<*, *>.SectionTitle(init: SectionTitle.() -> Unit) {
    addChild(SectionTitle(), init)
}