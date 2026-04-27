package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.CursorType
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("KuiklyDSLHoverDemo")
internal class KuiklyDSLHoverDemoPage : BasePager() {

    private var isHovered1 by observable(false)
    private var isHovered2 by observable(false)
    private var isHovered3 by observable(false)
    private var isHoveredCard by observable(false)
    private var enterCount by observable(0)
    private var exitCount by observable(0)
    private var logText by observable("等待鼠标悬停...")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }

            NavBar {
                attr {
                    title = "KuiklyDSL Hover 测试"
                }
            }

            // 场景：文字悬停选中态 + 手指光标
            View {
                attr {
                    marginTop(16f)
                    marginLeft(16f)
                    marginRight(16f)
                }
                Text {
                    attr {
                        fontSize(18f)
                        fontWeightBold()
                        color(Color.BLACK)
                        text("场景：文字悬停选中态 + 手指光标")
                    }
                }
            }
            View {
                attr {
                    marginTop(4f)
                    marginLeft(16f)
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color.GRAY)
                        text("鼠标移入文字区域，背景变灰 + 光标变手指")
                    }
                }
            }
            View {
                attr {
                    flexDirectionRow()
                    marginTop(12f)
                    marginLeft(16f)
                }
                // 可悬停文字项 1
                View {
                    attr {
                        borderRadius(6f)
                        backgroundColor(if (ctx.isHovered1) Color(0xFFE8E8E8) else Color.TRANSPARENT)
                        cursor(CursorType.POINTER)
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(6f)
                        paddingBottom(6f)
                    }
                    event {
                        mouseEnter { ctx.isHovered1 = true }
                        mouseExit { ctx.isHovered1 = false }
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            color(Color(0xFF333333))
                            text("我的电影清单")
                        }
                    }
                }
                // 可悬停文字项 2
                View {
                    attr {
                        marginLeft(16f)
                        borderRadius(6f)
                        backgroundColor(if (ctx.isHovered2) Color(0xFFE8E8E8) else Color.TRANSPARENT)
                        cursor(CursorType.POINTER)
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(6f)
                        paddingBottom(6f)
                    }
                    event {
                        mouseEnter { ctx.isHovered2 = true }
                        mouseExit { ctx.isHovered2 = false }
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            color(Color(0xFF333333))
                            text("产品设置")
                        }
                    }
                }
                // 可悬停文字项 3
                View {
                    attr {
                        marginLeft(16f)
                        borderRadius(6f)
                        backgroundColor(if (ctx.isHovered3) Color(0xFFE8E8E8) else Color.TRANSPARENT)
                        cursor(CursorType.POINTER)
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(6f)
                        paddingBottom(6f)
                    }
                    event {
                        mouseEnter { ctx.isHovered3 = true }
                        mouseExit { ctx.isHovered3 = false }
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            color(Color(0xFF333333))
                            text("版本管理")
                        }
                    }
                }
            }

            // 测试：hover + 阴影卡片
            View {
                attr {
                    marginTop(32f)
                    marginLeft(16f)
                    marginRight(16f)
                }
                Text {
                    attr {
                        fontSize(18f)
                        fontWeightBold()
                        color(Color.BLACK)
                        text("测试：Hover + 阴影卡片")
                    }
                }
            }
            View {
                attr {
                    marginTop(12f)
                    marginLeft(16f)
                    marginRight(16f)
                    borderRadius(12f)
                    backgroundColor(if (ctx.isHoveredCard) Color(0xFFE3F2FD) else Color.WHITE)
                    if (ctx.isHoveredCard) {
                        boxShadow(com.tencent.kuikly.core.base.BoxShadow(0f, 4f, 12f, Color(0x33000000)))
                    } else {
                        boxShadow(com.tencent.kuikly.core.base.BoxShadow(0f, 1f, 3f, Color(0x1A000000)))
                    }
                    padding(16f)
                    cursor(CursorType.POINTER)
                }
                event {
                    mouseEnter { ctx.isHoveredCard = true }
                    mouseExit { ctx.isHoveredCard = false }
                }
                Text {
                    attr {
                        fontSize(16f)
                        fontWeightMedium()
                        color(Color(0xFF333333))
                        text("可悬停卡片")
                    }
                }
                Text {
                    attr {
                        marginTop(4f)
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text("鼠标移入查看阴影和背景效果变化")
                    }
                }
                Text {
                    attr {
                        marginTop(4f)
                        fontSize(12f)
                        color(if (ctx.isHoveredCard) Color(0xFF4CAF50) else Color(0xFF999999))
                        text(if (ctx.isHoveredCard) "状态: 悬停中" else "状态: 未悬停")
                    }
                }
            }

            // 测试：Hover 日志
            View {
                attr {
                    marginTop(32f)
                    marginLeft(16f)
                    marginRight(16f)
                }
                Text {
                    attr {
                        fontSize(18f)
                        fontWeightBold()
                        color(Color.BLACK)
                        text("测试：Hover 状态日志")
                    }
                }
            }
            View {
                attr {
                    marginTop(12f)
                    marginLeft(16f)
                    marginRight(16f)
                    height(60f)
                    borderRadius(8f)
                    backgroundColor(Color(0x1A9C27B0))
                    allCenter()
                }
                event {
                    mouseEnter {
                        ctx.enterCount++
                        ctx.logText = "mouseEnter #${ctx.enterCount}"
                    }
                    mouseExit {
                        ctx.exitCount++
                        ctx.logText = "mouseExit #${ctx.exitCount}"
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color(0xFF9C27B0))
                        text("Hover 区域（查看计数）")
                    }
                }
            }
            View {
                attr {
                    marginTop(8f)
                    marginLeft(16f)
                }
                Text {
                    attr {
                        fontSize(12f)
                        color(Color(0xFF666666))
                        text("最新事件: ${ctx.logText} | Enter: ${ctx.enterCount} | Exit: ${ctx.exitCount}")
                    }
                }
            }
        }
    }
}
