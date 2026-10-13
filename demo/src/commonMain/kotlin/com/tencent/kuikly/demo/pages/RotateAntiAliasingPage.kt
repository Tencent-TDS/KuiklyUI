package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("RotateAntiAliasingPage")
internal class RotateAntiAliasingPage : Pager() {

    override fun body(): ViewBuilder {
        return {
            View {
                attr {
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    backgroundColor(Color.WHITE)
                }
                NavBar { attr { title = "旋转抗锯齿测试" } }

                // 说明文字
                Text {
                    attr {
                        marginTop(10f)
                        marginLeft(16f)
                        marginRight(16f)
                        text("以下旋转视图应显示平滑边缘（无锯齿）：")
                        fontSize(14f)
                        color(Color(0xFF666666.toInt()))
                    }
                }

                // 不同角度旋转对比
                View {
                    attr {
                        marginTop(20f)
                        flexDirectionRow()
                        justifyContentSpaceEvenly()
                        alignItemsCenter()
                        size(pagerData.pageViewWidth, 180f)
                    }
                    View {
                        attr {
                            size(60f, 60f)
                            backgroundColor(Color.RED)
                            transform(rotate = Rotate(15f))
                        }
                        Text { attr { text("15°"); fontSize(12f); color(Color.WHITE); marginTop(22f); alignSelfCenter() } }
                    }
                    View {
                        attr {
                            size(60f, 60f)
                            backgroundColor(Color.BLUE)
                            transform(rotate = Rotate(30f))
                        }
                        Text { attr { text("30°"); fontSize(12f); color(Color.WHITE); marginTop(22f); alignSelfCenter() } }
                    }
                    View {
                        attr {
                            size(60f, 60f)
                            backgroundColor(Color(0xFF00AA00.toInt()))
                            transform(rotate = Rotate(45f))
                        }
                        Text { attr { text("45°"); fontSize(12f); color(Color.WHITE); marginTop(22f); alignSelfCenter() } }
                    }
                    View {
                        attr {
                            size(60f, 60f)
                            backgroundColor(Color(0xFFFF8800.toInt()))
                            transform(rotate = Rotate(60f))
                        }
                        Text { attr { text("60°"); fontSize(12f); color(Color.WHITE); marginTop(22f); alignSelfCenter() } }
                    }
                }

                // 大尺寸旋转 - 更容易观察边缘
                Text {
                    attr {
                        marginTop(30f)
                        marginLeft(16f)
                        text("大尺寸旋转（更易观察边缘质量）：")
                        fontSize(14f)
                        color(Color(0xFF666666.toInt()))
                    }
                }
                View {
                    attr {
                        marginTop(20f)
                        flexDirectionRow()
                        justifyContentSpaceEvenly()
                        alignItemsCenter()
                        size(pagerData.pageViewWidth, 200f)
                    }
                    // 大方块旋转
                    View {
                        attr {
                            size(120f, 120f)
                            backgroundColor(Color.RED)
                            borderRadius(4f)
                            transform(rotate = Rotate(30f))
                        }
                        Text {
                            attr {
                                text("旋转30°")
                                fontSize(16f)
                                color(Color.WHITE)
                                marginTop(50f)
                                alignSelfCenter()
                            }
                        }
                    }
                    View {
                        attr {
                            size(120f, 120f)
                            backgroundColor(Color.BLUE)
                            borderRadius(4f)
                            transform(rotate = Rotate(45f))
                        }
                        Text {
                            attr {
                                text("旋转45°")
                                fontSize(16f)
                                color(Color.WHITE)
                                marginTop(50f)
                                alignSelfCenter()
                            }
                        }
                    }
                }

                // 圆角 + 旋转
                Text {
                    attr {
                        marginTop(30f)
                        marginLeft(16f)
                        text("圆角 + 旋转（锯齿最明显的场景）：")
                        fontSize(14f)
                        color(Color(0xFF666666.toInt()))
                    }
                }
                View {
                    attr {
                        marginTop(20f)
                        flexDirectionRow()
                        justifyContentSpaceEvenly()
                        alignItemsCenter()
                        size(pagerData.pageViewWidth, 160f)
                    }
                    View {
                        attr {
                            size(100f, 100f)
                            backgroundColor(Color(0xFFFF5500.toInt()))
                            borderRadius(16f)
                            transform(rotate = Rotate(20f))
                        }
                    }
                    View {
                        attr {
                            size(100f, 100f)
                            backgroundColor(Color(0xFF9900CC.toInt()))
                            borderRadius(50f)
                            transform(rotate = Rotate(45f))
                        }
                    }
                    View {
                        attr {
                            size(100f, 100f)
                            backgroundColor(Color(0xFF009999.toInt()))
                            borderRadius(16f)
                            border(Border(2f, BorderStyle.SOLID, Color.BLACK))
                            transform(rotate = Rotate(33f))
                        }
                    }
                }
            }
        }
    }

}
