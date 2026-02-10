package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("SafeAreaBottomPage")
internal class SafeAreaBottomPage : BasePager() {
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flex(1f)
                flexDirection(FlexDirection.COLUMN_REVERSE)
                backgroundColor(Color.YELLOW)
            }
            View {
                attr {
                    allCenter()
                    backgroundColor(Color.RED)
                    KLog.d("bottom", "${ctx.pageData.safeAreaInsets.bottom}")
                    marginBottom(ctx.pageData.safeAreaInsets.bottom)
                }
                Text {
                    attr {
                        fontSize(20f)
                        textAlignCenter()
                        text("底部导航栏高度测试")
                    }
                }
            }
        }
    }
}