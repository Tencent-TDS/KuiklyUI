package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Anchor
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Skew
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("SkewDemo")
internal class SkewDemoPage : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
                backgroundColor(Color.WHITE)
            }
            // 计算布局参数
            val pkTotalWidth = 80f   // PK区域总宽度（包含被遮挡部分）
            val buttonBaseWidth = (ctx.pagerData.pageViewWidth - pkTotalWidth) / 2 // 左右按钮基础宽度

            // PK按钮容器 1 (正确效果版本)
            View {
                attr {
                    width(ctx.pagerData.pageViewWidth)
                    height(48f)
                    flexDirectionRow()
                    overflow(true) // 允许内容溢出（用于实现倾斜重叠效果）
                    justifyContentSpaceBetween()
                    borderRadius(32f)
                    marginTop(12f)
                }

                // 左边按钮（红色）
                View {
                    attr {
                        width(buttonBaseWidth)
                        height(48f)
                        backgroundColor(0xFFE96664)
                        justifyContentCenter()
                        alignItemsCenter()
                        //transform(skew = Skew(-24f, 0f)) // 倾斜效果（平行四边形）
                        border(
                            Border(
                                2f,
                                BorderStyle.SOLID,
                                Color(0xFFE96664)
                            )
                        )
                    }


                    Text {
                        attr {
                            text("111")
                            fontSize(16f)
                            //transform(skew = Skew(24f, 0f)) // 反向倾斜文本（使其正常显示）
                            color(Color.WHITE)
                        }
                    }
                }

                // 红色倾斜层（视觉连接效果，连接红色按钮和中间PK区域）
                View {
                    attr {
                        width(21f)
                        height(50f)
                        backgroundColor(0xFFE96664)
                        transform(skew = Skew(-24f, 0f), anchor = Anchor(0f, 0f))
                        positionAbsolute()
                        top(-1f) // 确保完全覆盖
                        left((ctx.pagerData.pageViewWidth - pkTotalWidth) / 2) // 定位到红色区域右侧
                        zIndex(-1)
                    }
                }

                // 中间PK图标区域
                View {
                    attr {
                        width(pkTotalWidth)
                        height(48f)
                        justifyContentCenter()
                        alignItemsCenter()
                        positionAbsolute()
                        right(buttonBaseWidth)
                        // 上下边框连接左右按钮（红色上边框，蓝色下边框）
                        borderTop(
                            Border(
                                2f,
                                BorderStyle.SOLID,
                                Color(0xFFE96664)
                            )
                        )
                        borderBottom(
                            Border(
                                2f,
                                BorderStyle.SOLID,
                                Color(0xFF5199FF)
                            )
                        )
                    }

                }

                // 蓝色倾斜背景层（连接中间PK区域和蓝色按钮）
                View {
                    attr {
                        width(21f)
                        height(48f)
                        backgroundColor(0xFF5199FF)
                        transform(skew = Skew(-24f, 0f), anchor = Anchor(0f, 0f))
                        positionAbsolute()
                        right(ctx.pagerData.pageViewWidth / 2 - pkTotalWidth / 2 - 21f)
                        zIndex(-1)
                    }
                }

                // 右边按钮（蓝色）
                View {
                    attr {
                        width(buttonBaseWidth)
                        height(48f)
                        justifyContentCenter()
                        alignItemsCenter()
                        borderRadius(0f, 24f, 0f, 24f) // 右侧圆角
                        backgroundColor(0xFF5199FF)
                    }
                    Text {
                        attr {
                            text("2222")
                            fontSize(16f)
                            color(Color.WHITE)
                        }
                    }

                }
            }

            // PK按钮容器2（错误效果版本）
            View {
                attr {
                    width(ctx.pagerData.pageViewWidth)
                    height(48f)
                    flexDirectionRow()
                    overflow(true) // 允许内容溢出（用于实现倾斜重叠效果）
                    justifyContentSpaceBetween()
                    borderRadius(32f)
                    marginTop(12f)
                }

                // 左边按钮（红色）
                View {
                    attr {
                        width(buttonBaseWidth)
                        height(48f)
                        backgroundColor(0xFFE96664)
                        justifyContentCenter()
                        alignItemsCenter()
                        transform(skew = Skew(-24f, 0f)) // 倾斜效果（平行四边形）
                        border(
                            Border(
                                2f,
                                BorderStyle.SOLID,
                                Color(0xFFE96664)
                            )
                        )
                    }


                    Text {
                        attr {
                            text("111")
                            fontSize(16f)
                            transform(skew = Skew(24f, 0f)) // 反向倾斜文本（使其正常显示）
                            color(Color.WHITE)
                        }
                    }
                }

                // 红色倾斜层（视觉连接效果，连接红色按钮和中间PK区域）
                View {
                    attr {
                        width(21f)
                        height(50f)
                        backgroundColor(0xFFE96664)
                        transform(skew = Skew(-24f, 0f))
                        positionAbsolute()
                        top(-1f) // 确保完全覆盖
                        left((ctx.pagerData.pageViewWidth - pkTotalWidth) / 2) // 定位到红色区域右侧
                        zIndex(-1)
                    }
                }

                // 中间PK图标区域
                View {
                    attr {
                        width(pkTotalWidth)
                        height(48f)
                        justifyContentCenter()
                        alignItemsCenter()
                        positionAbsolute()
                        right(buttonBaseWidth)
                        // 上下边框连接左右按钮（红色上边框，蓝色下边框）
                        borderTop(
                            Border(
                                2f,
                                BorderStyle.SOLID,
                                Color(0xFFE96664)
                            )
                        )
                        borderBottom(
                            Border(
                                2f,
                                BorderStyle.SOLID,
                                Color(0xFF5199FF)
                            )
                        )
                    }

                }

                // 蓝色倾斜背景层（连接中间PK区域和蓝色按钮）
                View {
                    attr {
                        width(21f)
                        height(48f)
                        backgroundColor(0xFF5199FF)
                        transform(skew = Skew(-24f, 0f))
                        positionAbsolute()
                        right(ctx.pagerData.pageViewWidth / 2 - pkTotalWidth / 2 - 21f)
                        zIndex(-1)
                    }
                }

                // 右边按钮（蓝色）
                View {
                    attr {
                        width(buttonBaseWidth)
                        height(48f)
                        justifyContentCenter()
                        alignItemsCenter()
                        borderRadius(0f, 24f, 0f, 24f) // 右侧圆角
                        backgroundColor(0xFF5199FF)
                    }
                    Text {
                        attr {
                            text("2222")
                            fontSize(16f)
                            color(Color.WHITE)
                        }
                    }

                }
            }

        }
    }
}