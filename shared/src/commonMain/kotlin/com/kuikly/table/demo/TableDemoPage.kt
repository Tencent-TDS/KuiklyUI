package com.kuikly.table.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 表格组件演示入口页（tableDemo）
 *
 * 以可滚动列表的形式展示 9 个表格示例，点击任意一项，
 * 通过 RouterModule.openPage 打开对应的独立 @Page 页面（tableDemo1 ~ tableDemo9），
 * 便于单独验证 / 截图。
 *
 * 注意：根 ScrollerView 必须显式 scrollEnable(true)，否则整页无法滚动
 *（之前的版本遗漏了此项，导致列表滚不动、看起来像"点了没反应"）。
 */
@Page("tableDemo")
class TableDemoPage : Pager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Scroller {
                attr {
                    flexDirectionColumn()
                    flex(1f)
                    scrollEnable(true)
                }

                demoEntry(ctx, "Demo 1: 基础表格", "tableDemo1")
                demoEntry(ctx, "Demo 2: 蓝色主题", "tableDemo2")
                demoEntry(ctx, "Demo 3: 深色主题", "tableDemo3")
                demoEntry(ctx, "Demo 4: 大数据量滚动", "tableDemo4")
                demoEntry(ctx, "Demo 5: 固定首列", "tableDemo5")
                demoEntry(ctx, "Demo 6: 自定义单元格渲染", "tableDemo6")
                demoEntry(ctx, "Demo 7: 事件处理", "tableDemo7")
                demoEntry(ctx, "Demo 8: 紧凑风格无表头", "tableDemo8")
                demoEntry(ctx, "Demo 9: 自定义主题 DSL", "tableDemo9")
            }
        }
    }
}

/**
 * 列表项：一张可点击卡片，点击跳转到对应独立 Demo 页。
 */
private fun ViewContainer<*, *>.demoEntry(ctx: Pager, title: String, pageName: String) {
    View {
        attr {
            flexDirectionRow()
            alignItems(FlexAlign.CENTER)
            height(56f)
            marginTop(8f)
            marginLeft(16f)
            marginRight(16f)
            backgroundColor(0xFFFFFFFF)
            borderRadius(8f)
        }
        event {
            click {
                ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                    .openPage(pageName, JSONObject())
            }
        }
        Text {
            attr {
                text(title)
                fontSize(16f)
                color(0xFF333333)
                marginLeft(16f)
            }
        }
        View { attr { flex(1f) } }
        Text {
            attr {
                text("›")
                fontSize(20f)
                color(0xFF999999)
                marginRight(16f)
            }
        }
    }
}
