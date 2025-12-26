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

package com.tencent.kuikly.demo.pages.demo.list

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ListLimitBouncesTest")
internal class ListLimitBouncesTest : BasePager() {
    
    private var limitHeaderBounces by observable(false)
    private var limitFooterBounces by observable(false)
    private lateinit var scrollerView: ScrollerView<*, *>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "Limit Bounces Test" } }

            // 控制按钮区域
            View {
                attr {
                    padding(16f)
                    backgroundColor(Color(0xFFF5F5F5))
                }
                
                Text {
                    attr {
                        text("回弹限制设置")
                        fontSize(18f)
                        fontWeight500()
                        marginBottom(12f)
                    }
                }

                // 顶部回弹限制开关
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        justifyContentSpaceBetween()
                        padding(12f)
                        backgroundColor(Color.WHITE)
                        marginBottom(8f)
                        borderRadius(8f)
                    }
                    Text {
                        attr {
                            text("限制顶部回弹 (limitHeaderBounces)")
                            fontSize(16f)
                        }
                    }
                    Button {
                        attr {
                            titleAttr {
                                text(if (ctx.limitHeaderBounces) "已开启" else "已关闭")
                            }
                            backgroundColor(if (ctx.limitHeaderBounces) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                        }
                        event {
                            click {
                                ctx.limitHeaderBounces = !ctx.limitHeaderBounces
                                ctx.updateBouncesSettings()
                            }
                        }
                    }
                }

                // 底部回弹限制开关
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        justifyContentSpaceBetween()
                        padding(12f)
                        backgroundColor(Color.WHITE)
                        marginBottom(8f)
                        borderRadius(8f)
                    }
                    Text {
                        attr {
                            text("限制底部回弹 (limitFooterBounces)")
                            fontSize(16f)
                        }
                    }
                    Button {
                        attr {
                            titleAttr {
                                text(if (ctx.limitFooterBounces) "已开启" else "已关闭")
                            }
                            backgroundColor(if (ctx.limitFooterBounces) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                        }
                        event {
                            click {
                                ctx.limitFooterBounces = !ctx.limitFooterBounces
                                ctx.updateBouncesSettings()
                            }
                        }
                    }
                }

                // 状态显示
                View {
                    attr {
                        padding(12f)
                        backgroundColor(Color(0xFFE3F2FD))
                        borderRadius(8f)
                        marginTop(8f)
                    }
                    Text {
                        attr {
                            text("当前状态：\n" +
                                    "• 顶部回弹限制: ${if (ctx.limitHeaderBounces) "开启" else "关闭"}\n" +
                                    "• 底部回弹限制: ${if (ctx.limitFooterBounces) "开启" else "关闭"}\n\n" +
                                    "测试说明：\n" +
                                    "1. 向上滚动到顶部，测试顶部回弹限制\n" +
                                    "2. 向下滚动到底部，测试底部回弹限制\n" +
                                    "3. 切换开关查看效果变化")
                            fontSize(14f)
                            color(Color(0xFF1976D2))
                            lineHeight(20f)
                        }
                    }
                }
            }

            // 滚动区域
            Scroller {
                ctx.scrollerView = this
                attr {
                    flex(1f)
                    bouncesEnable(true, ctx.limitHeaderBounces, ctx.limitFooterBounces)
                }

                // 顶部标识
                View {
                    attr {
                        height(100f)
                        backgroundColor(Color(0xFFFFEB3B))
                        allCenter()
                    }
                    Text {
                        attr {
                            text("顶部区域 - 向上滚动测试顶部回弹限制")
                            fontSize(16f)
                            fontWeight500()
                            color(Color.BLACK)
                        }
                    }
                }

                // 中间内容区域
                for (i in 1..30) {
                    View {
                        attr {
                            height(80f)
                            backgroundColor(if (i % 2 == 0) Color(0xFFE0E0E0) else Color(0xFFF5F5F5))
                            padding(16f)
                            alignItemsCenter()
                        }
                        Text {
                            attr {
                                text("内容项 $i - 滚动测试")
                                fontSize(16f)
                                color(Color.BLACK)
                            }
                        }
                    }
                }

                // 底部标识
                View {
                    attr {
                        height(100f)
                        backgroundColor(Color(0xFF4CAF50))
                        allCenter()
                    }
                    Text {
                        attr {
                            text("底部区域 - 向下滚动测试底部回弹限制")
                            fontSize(16f)
                            fontWeight500()
                            color(Color.WHITE)
                        }
                    }
                }
            }
        }
    }

    private fun updateBouncesSettings() {
        scrollerView.getViewAttr().bouncesEnable(
            bouncesEnable = true,
            limitHeaderBounces = limitHeaderBounces,
            limitFooterBounces = limitFooterBounces
        )
    }
}

