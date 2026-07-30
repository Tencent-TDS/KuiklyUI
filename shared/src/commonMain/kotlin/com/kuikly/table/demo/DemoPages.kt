package com.kuikly.table.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.*

/**
 * 9 个表格示例各自独立成一个 @Page 页面（tableDemo1 ~ tableDemo9），
 * 便于单独打开、验证与截图。主列表页 tableDemo 通过 RouterModule.openPage 跳转至此。
 *
 * 每个页面都带一个顶部标题栏与"返回"按钮，"返回"调用 RouterModule.closePage()
 * 回到主列表页。
 */

/**
 * 统一页面外壳：根视图用 View 作为 Flex 容器占满屏幕，内部依次放置
 * 顶部标题栏（含返回按钮）与内容区。
 *
 * 注意：
 * 1. 根 View 的 flex(1f) 在 Pager.body() 返回后由页面容器撑满全屏。
 * 2. 标题栏固定高度 48f，内容区 flex(1f) 占满剩余空间。
 * 3. 表格内部自带滚动（横向/纵向 Scroller），外壳不再包 Scroller，避免
 *    Scroller 不是 Flex 容器导致 flex(1f) 失效、内容高度为 0。
 */
private fun ViewContainer<*, *>.demoPageShell(title: String, pager: Pager, content: ViewContainer<*, *>.() -> Unit) {
    View {
        attr {
            flex(1f)
            flexDirectionColumn()
        }

        // 顶部标题栏
        View {
            attr {
                flexDirectionRow()
                alignItems(FlexAlign.CENTER)
                height(48f)
                backgroundColor(0xFFF5F5F5)
            }
            View {
                attr {
                    padding(12f, 0f, 12f, 0f)
                }
                event {
                    click {
                        pager.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                    }
                }
                Text {
                    attr {
                        text("‹ 返回")
                        fontSize(16f)
                        color(0xFF1976D2)
                    }
                }
            }
            Text {
                attr {
                    text(title)
                    fontSize(16f)
                    fontWeightBold()
                    color(0xFF333333)
                    marginLeft(8f)
                }
            }
        }

        // 内容区：表格放在内部 View 中保证测量正确。
        // 关键：内容区必须拿到确定高度（flex(1f) 占满标题栏以下的空间），
        // 否则内部 KuiklyTableView（ComposeView）没有高度约束，会被 Kuikly
        // 算成高度 0 → 表格渲染了但不可见。
        View {
            attr {
                flex(1f)
                flexDirectionColumn()
                padding(16f, 16f, 16f, 16f)
            }
            content()
        }
    }
}

@Page("tableDemo1")
class Demo1Page : Pager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return { demoPageShell("Demo 1: 基础表格", ctx) { demoBasicTable() } }
    }
}

@Page("tableDemo2")
class Demo2Page : Pager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return { demoPageShell("Demo 2: 蓝色主题", ctx) { demoBlueThemeTable() } }
    }
}

@Page("tableDemo3")
class Demo3Page : Pager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return { demoPageShell("Demo 3: 深色主题", ctx) { demoDarkThemeTable() } }
    }
}

@Page("tableDemo4")
class Demo4Page : Pager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return { demoPageShell("Demo 4: 大数据量滚动", ctx) { demoLargeDataTable() } }
    }
}

@Page("tableDemo5")
class Demo5Page : Pager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return { demoPageShell("Demo 5: 固定首列", ctx) { demoFixedColumnTable() } }
    }
}

@Page("tableDemo6")
class Demo6Page : Pager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return { demoPageShell("Demo 6: 自定义单元格渲染", ctx) { demoCustomRenderTable() } }
    }
}

@Page("tableDemo7")
class Demo7Page : Pager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return { demoPageShell("Demo 7: 事件处理", ctx) { demoEventTable() } }
    }
}

@Page("tableDemo8")
class Demo8Page : Pager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return { demoPageShell("Demo 8: 紧凑风格无表头", ctx) { demoCompactTable() } }
    }
}

@Page("tableDemo9")
class Demo9Page : Pager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return { demoPageShell("Demo 9: 自定义主题 DSL", ctx) { demoCustomThemeTable() } }
    }
}
