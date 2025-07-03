package com.tencent.kuiklydemo.pages.demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.SafeArea
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.My
import com.tencent.kuikly.demo.pages.demo.MyDemoCustom
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("CustomViewExamplePage")
internal class CustomViewExamplePage : BasePager() {
    var message by observable("0")
    var cnt = 0

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // navBar
            NavBar {
                attr {
                    title = "CustomViewExamplePage"
                }
            }
            View {
                attr {
                    flex(1f)
                    backgroundColor(Color.WHITE)
                }
                My {
                    attr {
                        width(300f)
                        height(300f)
                        backgroundColor(0xff999999)
                        message("hello this is My view")
                    }
                }
                MyDemoCustom {
                    attr {
                        width(300f)
                        height(300f)
                        backgroundColor(Color.YELLOW)
                        message(ctx.message)
                    }
                    event{
                        onMyViewTapped {
                            ctx.message = "${++ctx.cnt}"
                        }
                    }
                    View{
                        attr{
                            width(300f)
                            height(30f)
                            backgroundColor(Color.WHITE)
                            alignItemsCenter()
                        }
                        event{
                            click { println("view clicked") }
                        }
                        Text {
                            attr{
                                text("Static text element from kuikly")
                            }
                        }
                    }
                }
            }
        }
    }
}
