package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.layout.Center
import com.tencent.kuikly.demo.pages.demo.base.NavBar


@Page("borderTestPage")
internal class BorderTestPage : Pager() {

    public val dimens = 0.33333334f

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                }
                NavBar { attr { title = "边框效果测试" } }
                View {
                    attr {
                        size(pagerData.pageViewWidth, 100f)
                        backgroundColor(Color.YELLOW)
                    }
                    Center {
                        Text {
                            attr {
                                text("宽度为1像素边框效果测试")
                            }
                        }
                        View {
                            attr {
                                marginLeft(1.2f)
                                marginTop(2.5f)
                                size(83f, 36f)
                                borderRadius(18f)
                                border(
                                    Border(
                                        1f / pagerData.density,
                                        color = Color.RED,
                                        lineStyle = BorderStyle.SOLID
                                    )
                                )
                            }
                        }
                    }
                }

                View {
                    attr {
                        size(pagerData.pageViewWidth, 100f)
                        backgroundColor(Color.GREEN)
                    }
                    Center {
                        Text {
                            attr {
                                text("宽度为1像素边框效果测试")
                            }
                        }
                        View {
                            attr {
                                size(14f, 3f)
                                borderRadius(8f)
                                backgroundColor(Color(0xFFFB6D0E))
                            }
                        }
                    }
                }
            }
        }


    }
}

