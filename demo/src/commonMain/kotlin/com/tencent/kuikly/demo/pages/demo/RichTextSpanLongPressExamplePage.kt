package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.ImageSpan
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("RichTextSpanLongPressExamplePage")
internal class RichTextSpanLongPressExamplePage : BasePager() {

    private var statusText by observable("长按蓝色或绿色或图片 span，或长按空白文本区域")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "RichText Span LongPress"
                }
            }

            View {
                attr {
                    flex(1f)
                    padding(all = 16f)
                    backgroundColor(Color(0xFFF5F5F5))
                }

                Text {
                    attr {
                        fontSize(16f)
                        color(Color(0xFF333333))
                        text(ctx.statusText)
                        marginBottom(16f)
                    }
                }

                View {
                    attr {
                        padding(all = 16f)
                        borderRadius(12f)
                        backgroundColor(Color.WHITE)
                    }

                    RichText {
                        attr {
                            fontSize(18f)
                        }
                        event {
                            longPress {
                                ctx.statusText = "RichText longPress state=${it.state}"
                                KLog.i("RichTextSpanLongPress", "richText longPress fallback state=${it.state}, payload=${it.params}")
                            }
                            click {
                                ctx.statusText = "RichText click"
                                KLog.i("RichTextSpanLongPress", "richText click, payload=${it.params}")
                            }
                        }
                        Span {
                            text("普通文本区域，长按这里会回退到 RichText.longPress；")
                            color(Color(0xFF666666))
                        }
                        Span {
                            text("长按我触发蓝色 span")
                            color(Color(0xFF2F80ED))
                            fontWeightBold()
                            longPress {
                                ctx.statusText = "蓝色 span longPress state=${it.state}"
                                KLog.i("RichTextSpanLongPress", "blue span longPress state=${it.state}, indexPayload=${it.params}")
                            }
                            click {
                                ctx.statusText = "蓝色 span click "
                                KLog.i("RichTextSpanLongPress", "blue span click, indexPayload=${it.params}")
                            }
                        }
                        Span {
                            text("，然后轻微移动仍应锁定原 span")
                            color(Color(0xFF666666))
                        }
                        ImageSpan {
                            size(40f, 40f)
                            marginLeft(20f)
                            marginRight(20f)
                            src("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png")
                            longPress {
                                ctx.statusText = "image span longPress state=${it.state}"
                                KLog.i("RichTextSpanLongPress", "image span longPress state=${it.state}, indexPayload=${it.params}")
                            }
                            click {
                                ctx.statusText = "image span click "
                                KLog.i("RichTextSpanLongPress", "image span click, indexPayload=${it.params}")
                            }
                        }
                        Span {
                            text("长按我触发绿色 span")
                            color(Color(0xFF27AE60))
                            fontWeightBold()
                            longPress {
                                ctx.statusText = "绿色 span longPress state=${it.state}"
                                KLog.i("RichTextSpanLongPress", "green span longPress state=${it.state}, indexPayload=${it.params}")
                            }
                            click {
                                ctx.statusText = "绿色 span click "
                                KLog.i("RichTextSpanLongPress", "green span click, indexPayload=${it.params}")
                            }
                        }
                    }

                }
            }
        }
    }
}
