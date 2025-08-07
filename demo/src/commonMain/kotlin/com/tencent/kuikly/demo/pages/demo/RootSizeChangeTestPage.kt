package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.View

@Page("root_size")
internal class RootSizeChangeTestPage : BasePager() {
    
    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(Color.GRAY)
            }
            View {
                attr {
                    absolutePosition(
                        left = 10f,
                        top = 10f,
                        right = 10f,
                        bottom = 10f
                    )
                    backgroundColor(Color.BLUE)
                    allCenter()
                }
                View {
                    attr {
                        backgroundColor(Color.RED)
                        size(100f, 100f)
                    }
                }
            }
        }
    }
}