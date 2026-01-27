package com.tencent.kuikly.demo.pages.jank

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.Text

/**
 * Kuikly DSL 版本的 Text 性能测试 Demo
 * 用于测试 Text 组件在 Kuikly DSL 下的渲染性能
 * 
 * 测试内容：
 * - 900 个 Text 组件
 * - 前 300 个：相同文本内容
 * - 后 600 个：不同文本内容
 * - 使用普通纵向布局（不可滚动）
 */
@Page("TextPerfKuiklyDemo")
class TextPerfKuiklyDemo : Pager() {

    // 总数，前 300 + 后 600 = 900
    private val totalCount = 900

    override fun body(): ViewBuilder {
        val pager = this
        return {
            attr {
                flexDirectionColumn()
            }

            // 前 300 个相同文本
            repeat(300) { index ->
                Text {
                    attr {
                        text("相同文本内容_${index + 1}")
                        fontSize(14f)
                        color(Color.BLACK)
                        lines(1)
                    }
                }
            }

            // 后 600 个不同文本
            repeat(600) { index ->
                val isLast = index == 599 // 最后一个
                val randomSuffix = "ABC${index}XYZ${index * 2}DEF${index * 3}"
                Text {
                    attr {
                        text("不同文本内容_${index + 301}_$randomSuffix")
                        fontSize(14f)
                        color(Color.BLACK)
                        lines(1)
                    }
                    if (isLast) {
                        event {
                        layoutFrameDidChange { _ ->
                                // 最后一个 Text 完成布局，打印性能测试结束标记
                                println("[TextPerf][AllTextLayoutComplete] Kuikly DSL - Total: ${pager.totalCount}")
                                // 通过 NotifyModule 通知原生层性能测试结束
                                pager.acquireModule<NotifyModule>(NotifyModule.MODULE_NAME).postNotify(
                                    "TextPerfAllLayoutComplete",
                                    JSONObject().put("type", "Kuikly").put("total", pager.totalCount)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
