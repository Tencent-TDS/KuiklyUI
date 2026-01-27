package com.tencent.kuikly.demo.pages.jank

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.Text

/**
 * Kuikly DSL 版本的 Text 性能测试 Demo
 * 用于测试 Text 组件在 Kuikly DSL 下的渲染性能
 * 
 * 测试内容：
 * - 300 个 Text 组件
 * - 前 100 个：相同文本内容
 * - 后 200 个：不同文本内容
 * - 使用普通纵向布局（不可滚动）
 */
@Page("TextPerfKuiklyDemo")
class TextPerfKuiklyDemo : Pager() {

    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionColumn()
            }

            // 前 100 个相同文本
            repeat(100) { index ->
                Text {
                    attr {
                        text("相同文本内容_${index + 1}")
                        fontSize(14f)
                        color(Color.BLACK)
                        lines(1)
                    }
                }
            }

            // 后 200 个不同文本
            repeat(200) { index ->
                val randomSuffix = "ABC${index}XYZ${index * 2}DEF${index * 3}"
                Text {
                    attr {
                        text("不同文本内容_${index + 101}_$randomSuffix")
                        fontSize(14f)
                        color(Color.BLACK)
                        lines(1)
                    }
                }
            }
        }
    }
}
