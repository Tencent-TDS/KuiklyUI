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
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.appearPercentage
import com.tencent.kuikly.core.base.event.didAppear
import com.tencent.kuikly.core.base.event.didDisappear
import com.tencent.kuikly.core.base.event.willAppear
import com.tencent.kuikly.core.base.event.willDisappear
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.properties.ReadWriteProperty

/**
 * visibleAreaIgnoreTopMargin 和 visibleAreaIgnoreBottomMargin 示例页面
 *
 * 这两个属性用于设置计算子组件可见性面积时忽略的顶部/底部距离。
 * 常用场景：滚动容器顶部或底部有固定的遮挡区域（如悬浮导航栏、底部工具栏等）时，
 * 可以使用这两个属性来调整可见性计算的有效区域。
 */
@Page("visibleArea")
internal class VisibleAreaPage : BasePager() {

    // 顶部忽略区域高度
    private var ignoreTopMargin: Float by observable(60f)
    // 底部忽略区域高度
    private var ignoreBottomMargin: Float by observable(80f)

    // 子项可见性状态
    private var item1State = observable("未触发")
    private var item2State = observable("未触发")
    private var item3State = observable("未触发")
    private var item4State = observable("未触发")
    private var item5State = observable("未触发")

    // 子项可见百分比
    private var item1Percent = observable(0f)
    private var item2Percent = observable(0f)
    private var item3Percent = observable(0f)
    private var item4Percent = observable(0f)
    private var item5Percent = observable(0f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            NavBar {
                attr {
                    title = "VisibleAreaIgnoreMargin示例"
                }
            }

            // 控制区域
            View {
                attr {
                    flexDirectionRow()
                    padding(10f)
                    justifyContentSpaceBetween()
                    zIndex(10)
                }

                // 顶部忽略控制
                View {
                    attr {
                        flex(1f)
                        marginRight(10f)
                    }
                    Text {
                        attr {
                            fontSize(13f)
                            color(Color(0xFF333333))
                            text("忽略顶部高度: ${ctx.ignoreTopMargin.toInt()}dp")
                        }
                    }
                    View {
                        attr {
                            flexDirectionRow()
                            marginTop(5f)
                        }
                        // 减少按钮
                        View {
                            attr {
                                size(40f, 30f)
                                backgroundColor(Color(0xFFE0E0E0))
                                borderRadius(5f)
                                allCenter()
                            }
                            Text {
                                attr {
                                    text("-10")
                                    fontSize(12f)
                                }
                            }
                            event {
                                click {
                                    ctx.ignoreTopMargin = maxOf(0f, ctx.ignoreTopMargin - 10f)
                                }
                            }
                        }
                        // 增加按钮
                        View {
                            attr {
                                size(40f, 30f)
                                backgroundColor(Color(0xFFE0E0E0))
                                borderRadius(5f)
                                allCenter()
                                marginLeft(10f)
                            }
                            Text {
                                attr {
                                    text("+10")
                                    fontSize(12f)
                                }
                            }
                            event {
                                click {
                                    ctx.ignoreTopMargin = minOf(150f, ctx.ignoreTopMargin + 10f)
                                }
                            }
                        }
                    }
                }

                // 底部忽略控制
                View {
                    attr {
                        flex(1f)
                    }
                    Text {
                        attr {
                            fontSize(13f)
                            color(Color(0xFF333333))
                            text("忽略底部高度: ${ctx.ignoreBottomMargin.toInt()}dp")
                        }
                    }
                    View {
                        attr {
                            flexDirectionRow()
                            marginTop(5f)
                        }
                        // 减少按钮
                        View {
                            attr {
                                size(40f, 30f)
                                backgroundColor(Color(0xFFE0E0E0))
                                borderRadius(5f)
                                allCenter()
                            }
                            Text {
                                attr {
                                    text("-10")
                                    fontSize(12f)
                                }
                            }
                            event {
                                click {
                                    ctx.ignoreBottomMargin = maxOf(0f, ctx.ignoreBottomMargin - 10f)
                                }
                            }
                        }
                        // 增加按钮
                        View {
                            attr {
                                size(40f, 30f)
                                backgroundColor(Color(0xFFE0E0E0))
                                borderRadius(5f)
                                allCenter()
                                marginLeft(10f)
                            }
                            Text {
                                attr {
                                    text("+10")
                                    fontSize(12f)
                                }
                            }
                            event {
                                click {
                                    ctx.ignoreBottomMargin = minOf(150f, ctx.ignoreBottomMargin + 10f)
                                }
                            }
                        }
                    }
                }
            }

            // 主体演示区域
            View {
                attr {
                    height(360f)
                    margin(15f)
                    border(Border(2f, BorderStyle.SOLID, Color(0xFF2196F3)))
                    borderRadius(5f)
                }

                // 顶部忽略区域指示
                View {
                    attr {
                        absolutePosition(
                            left = 0f,
                            top = 0f,
                            right = 0f
                        )
                        height(ctx.ignoreTopMargin)
                        backgroundColor(Color(0x40FF5722))
                        allCenter()
                        zIndex(10)
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFFFF5722))
                            text("顶部忽略区域 (${ctx.ignoreTopMargin.toInt()}dp)")
                        }
                    }
                }

                // 可滚动列表
                Scroller {
                    attr {
                        flex(1f)
                        visibleAreaIgnoreTopMargin(ctx.ignoreTopMargin)
                        visibleAreaIgnoreBottomMargin(ctx.ignoreBottomMargin)
                    }

                    // 列表项1
                    VisibilityItem(
                        title = "列表项 1",
                        bgColor = Color(0xFFE3F2FD),
                        percent = ctx.item1Percent,
                        state = ctx.item1State
                    )

                    // 列表项2
                    VisibilityItem(
                        title = "列表项 2",
                        bgColor = Color(0xFFFCE4EC),
                        state = ctx.item2State,
                        percent = ctx.item2Percent
                    )

                    // 列表项3
                    VisibilityItem(
                        title = "列表项 3",
                        bgColor = Color(0xFFE8F5E9),
                        state = ctx.item3State,
                        percent = ctx.item3Percent
                    )

                    // 列表项4 (不监听百分比)
                    VisibilityItem(
                        title = "列表项 4",
                        bgColor = Color(0xFFFFF3E0),
                        state = ctx.item4State,
                        percent = ctx.item4Percent
                    )

                    // 列表项5 (不监听百分比)
                    VisibilityItem(
                        title = "列表项 5",
                        bgColor = Color(0xFFF3E5F5),
                        state = ctx.item5State,
                        percent = ctx.item5Percent
                    )
                }

                // 底部忽略区域指示
                View {
                    attr {
                        absolutePosition(
                            left = 0f,
                            bottom = 0f,
                            right = 0f
                        )
                        height(ctx.ignoreBottomMargin)
                        backgroundColor(Color(0x40FF5722))
                        allCenter()
                        zIndex(10)
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFFFF5722))
                            text("底部忽略区域 (${ctx.ignoreBottomMargin.toInt()}dp)")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 可见性监听列表项组件
 */
private fun ViewContainer<*, *>.VisibilityItem(
    title: String,
    bgColor: Color,
    state: ReadWriteProperty<Any, String>,
    percent: ReadWriteProperty<Any, Float>
) {
    val ctx = object {
        var obvState by state
        var obvPercent by percent
    }
    View {
        attr {
            height(120f)
            backgroundColor(bgColor)
            justifyContentCenter()
        }
        Text {
            attr {
                fontSize(16f)
                fontWeightBold()
                color(Color(0xFF333333))
                text(title)
            }
        }
        Text {
            attr {
                fontSize(14f)
                marginTop(8f)
                color(Color(0xFF666666))
                text("状态: ${ctx.obvState}")
            }
        }
        Text {
            attr {
                fontSize(14f)
                marginTop(4f)
                color(Color(0xFF666666))
                text("可见百分比: ${(ctx.obvPercent * 100).toInt()}%")
            }
        }
        event {
            willAppear {
                ctx.obvState = "willAppear"
            }
            didAppear {
                ctx.obvState = "didAppear"
            }
            willDisappear {
                ctx.obvState = "willDisappear"
            }
            didDisappear {
                ctx.obvState = "didDisappear"
            }
            appearPercentage { percentage ->
                ctx.obvPercent = percentage
            }
        }
    }
}
