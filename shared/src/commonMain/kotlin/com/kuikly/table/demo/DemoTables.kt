package com.kuikly.table.demo

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.views.*
import com.kuikly.table.dsl.Table
import com.kuikly.table.model.TableTheme
import com.kuikly.table.model.TextAlign

/**
 * 9 个表格示例的 DSL 配置，抽成 ViewContainer 扩展函数，
 * 供独立 Demo 页面（DemoPages.kt）与主列表页（TableDemoPage.kt）复用，避免重复。
 */

// Demo 1: 基础表格
fun ViewContainer<*, *>.demoBasicTable() {
    Table {
        column("name", "姓名", 120f)
        column("age", "年龄", 80f)
        column("city", "城市", 100f)
        column("job", "职业", 120f)

        row("张三", "28", "北京", "工程师")
        row("李四", "32", "上海", "设计师")
        row("王五", "25", "广州", "产品经理")
        row("赵六", "30", "深圳", "数据分析师")
        row("孙七", "27", "杭州", "前端开发")
    }
}

// Demo 2: 蓝色主题
fun ViewContainer<*, *>.demoBlueThemeTable() {
    Table {
        column("product", "产品名称", 150f)
        column("price", "单价", 100f) {
            align(TextAlign.RIGHT)
        }
        column("quantity", "数量", 80f) {
            align(TextAlign.CENTER)
        }
        column("total", "合计", 100f) {
            align(TextAlign.RIGHT)
        }

        row("Kuikly Pro", "99.00", "10", "990.00")
        row("Kuikly Plus", "199.00", "5", "995.00")
        row("Kuikly Enterprise", "499.00", "2", "998.00")

        theme(TableTheme.BLUE)
    }
}

// Demo 3: 深色主题
fun ViewContainer<*, *>.demoDarkThemeTable() {
    Table {
        column("name", "Name", 120f)
        column("role", "Role", 100f)
        column("status", "Status", 80f)

        row("Alice", "Admin", "Active")
        row("Bob", "Editor", "Inactive")
        row("Carol", "Viewer", "Active")

        theme(TableTheme.DARK)
    }
}

// Demo 4: 大数据量横纵滚动
fun ViewContainer<*, *>.demoLargeDataTable() {
    Table {
        for (i in 1..10) {
            column("col$i", "列$i", 100f)
        }

        for (r in 1..50) {
            row(*(1..10).map { c -> "R${r}C${c}" }.toTypedArray())
        }

        maxHeight(250f)
        scrollEnabled(true)
    }
}

// Demo 5: 固定列
fun ViewContainer<*, *>.demoFixedColumnTable() {
    Table {
        column("id", "ID", 60f)
        column("name", "姓名", 120f)
        column("email", "邮箱", 200f)
        column("phone", "电话", 150f)
        column("address", "地址", 200f)
        column("company", "公司", 150f)

        row("001", "张三", "zhangsan@qq.com", "13800138001", "北京市海淀区", "腾讯")
        row("002", "李四", "lisi@qq.com", "13800138002", "上海市浦东新区", "阿里巴巴")
        row("003", "王五", "wangwu@qq.com", "13800138003", "广州市天河区", "字节跳动")
        row("004", "赵六", "zhaoliu@qq.com", "13800138004", "深圳市南山区", "华为")

        fixedColumns(1)
    }
}

// Demo 6: 自定义单元格渲染
fun ViewContainer<*, *>.demoCustomRenderTable() {
    Table {
        column("name", "姓名", 100f)
        column("status", "状态", 100f) {
            align(TextAlign.CENTER)
            customRenderer { cellData, _, _ ->
                View {
                    attr {
                        backgroundColor(
                            if (cellData.value == "在线") 0xFF4CAF50 else 0xFFF44336
                        )
                        borderRadius(12f)
                        padding(8f, 2f, 8f, 2f)
                        alignSelf(FlexAlign.CENTER)
                    }
                    Text {
                        attr {
                            text(cellData.value)
                            color(0xFFFFFFFF)
                            fontSize(12f)
                        }
                    }
                }
            }
        }
        column("score", "评分", 80f) {
            align(TextAlign.CENTER)
            customRenderer { cellData, _, _ ->
                val score = cellData.value.toIntOrNull() ?: 0
                val stars = "★".repeat(score) + "☆".repeat(5 - score)
                Text {
                    attr {
                        text(stars)
                        color(0xFFFF9800)
                        fontSize(12f)
                    }
                }
            }
        }

        row("张三", "在线", "5")
        row("李四", "离线", "3")
        row("王五", "在线", "4")
        row("赵六", "离线", "2")
    }
}

// Demo 7: 事件处理
fun ViewContainer<*, *>.demoEventTable() {
    Table {
        column("action", "操作", 80f)
        column("status", "状态", 80f)
        column("detail", "详情", 200f)

        row("查看", "已完成", "Kuikly表格组件开发任务")
        row("编辑", "进行中", "单元格自定义渲染功能")
        row("删除", "待开始", "性能优化与测试")

        onCellClick { rowIndex, colIndex, value ->
            println("单元格点击: row=$rowIndex, col=$colIndex, value=$value")
        }

        onRowClick { rowIndex ->
            println("行点击: row=$rowIndex")
        }

        onHeaderClick { colIndex, key ->
            println("表头点击: col=$colIndex, key=$key")
        }
    }
}

// Demo 8: 紧凑主题 + 无表头
fun ViewContainer<*, *>.demoCompactTable() {
    Table {
        column("c1", "", 150f)
        column("c2", "", 150f)
        column("c3", "", 100f)

        row("Apple", "Red", "Sweet")
        row("Banana", "Yellow", "Sweet")
        row("Lime", "Green", "Sour")
        row("Grape", "Purple", "Sweet")

        headerVisible(false)
        theme(TableTheme.COMPACT)
    }
}

// Demo 9: 自定义主题构建器
fun ViewContainer<*, *>.demoCustomThemeTable() {
    Table {
        column("month", "月份", 80f)
        column("revenue", "收入", 100f) { align(TextAlign.RIGHT) }
        column("cost", "成本", 100f) { align(TextAlign.RIGHT) }
        column("profit", "利润", 100f) { align(TextAlign.RIGHT) }

        row("一月", "120,000", "80,000", "40,000")
        row("二月", "135,000", "85,000", "50,000")
        row("三月", "150,000", "90,000", "60,000")

        theme {
            headerBackgroundColor = 0xFF6A1B9A
            headerTextColor = 0xFFFFFFFF
            borderColor = 0xFFCE93D8
            stripedRows = true
            rowAlternateColor = 0xFFF3E5F5
            cellPaddingHorizontal = 12f
        }
    }
}
