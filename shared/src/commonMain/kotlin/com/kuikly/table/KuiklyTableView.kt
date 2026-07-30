package com.kuikly.table

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.*
import com.kuikly.table.model.CellData
import com.kuikly.table.model.ColumnDef
import com.kuikly.table.model.TableTheme
import com.kuikly.table.model.TextAlign
import com.kuikly.table.model.toFlexAlign
import com.kuikly.table.log.TableLog

class KuiklyTableView : ComposeView<TableAttr, TableEvent>() {

    override fun createAttr(): TableAttr = TableAttr()
    override fun createEvent(): TableEvent = TableEvent()

    override fun body(): ViewBuilder {
        val tableAttr = attr
        val tableEvent = event
        val theme = tableAttr.theme
        val columns = tableAttr.columns
        val rows = tableAttr.rows
        val fixedCols = tableAttr.fixedColumns

        // 记录渲染开始
        TableLog.render(VIEW_TAG, "开始渲染 | 列=${columns.size} 行=${rows.size} 固定列=${fixedCols}")

        return {
            try {
                View {
                    attr {
                        // flex(1f) 确保 body 根视图拿到 ComposeView 分配的完整高度，
                        // 避免因 ComposeView 内部不自动传播高度而坍缩为 0。
                        flex(1f)
                        flexDirectionColumn()
                        if (theme.showOuterBorder) {
                            border(borderOf(theme.borderWidth, theme.borderColor))
                        }
                        // 不设 overflow(false)：当 ComposeView 高度异常为 0 时，
                        // overflow=false 会裁剪全部内容导致表格完全不可见；
                        // 移除后让内容自然溢出，至少保证可见性。
                        accessibility("数据表格，共${columns.size}列${rows.size}行")
                    }

                    if (fixedCols > 0 && fixedCols < columns.size) {
                        renderFixedColumnsLayout(columns, rows, fixedCols, theme, tableAttr, tableEvent)
                    } else {
                        renderNormalLayout(columns, rows, theme, tableAttr, tableEvent)
                    }
                }
                TableLog.render(VIEW_TAG, "渲染完成")
            } catch (e: Exception) {
                TableLog.exception(VIEW_TAG, e, "表格渲染异常")
                // 异常时显示错误提示
                View {
                    attr {
                        flexDirectionColumn()
                        padding(16f, 16f, 16f, 16f)
                        accessibility("表格渲染失败")
                    }
                    Text {
                        attr {
                            text("表格渲染异常: ${e.message}")
                            fontSize(14f)
                            color(0xFFFF0000)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val VIEW_TAG = "KuiklyTableView"
    }
}

/**
 * 用 Border 对象描述边框
 */
private fun borderOf(width: Float, color: Long) = Border(width, BorderStyle.SOLID, Color(color))

/**
 * 单侧边框辅助方法。
 * GroupView.borderBottom/borderRight 位于 core 的 internal 包，外部模块不可见，
 * 这里用公开的 attr API（absolutePosition + border）以覆盖层方式实现同样的单侧描边效果。
 * 注意：必须显式传入 target 容器，避免与多层 ViewContainer 隐式接收者冲突。
 */
private fun addBorderBottom(target: ViewContainer<*, *>, width: Float, color: Long) {
    val border = Border(width, BorderStyle.SOLID, Color(color))
    target.View {
        attr {
            absolutePosition(bottom = 0f, left = 0f, right = 0f)
                .height(width)
                .border(border)
                .zIndex(999, false)
        }
    }
}

private fun addBorderRight(target: ViewContainer<*, *>, width: Float, color: Long) {
    val border = Border(width, BorderStyle.SOLID, Color(color))
    target.View {
        attr {
            absolutePosition(top = 0f, bottom = 0f, right = 0f)
                .width(width)
                .border(border)
                .zIndex(999, false)
        }
    }
}

// --- Normal layout: all columns scroll together ---

private fun ViewContainer<*, *>.renderNormalLayout(
    columns: List<ColumnDef>,
    rows: List<List<String>>,
    theme: TableTheme,
    tableAttr: TableAttr,
    tableEvent: TableEvent
) {
    Scroller {
        attr {
            // 关键：横向 Scroller 作为 body 根 View（flex column）的子节点，
            // 必须显式 flex(1f) 才能占满父容器高度。缺少它时高度算为 0，
            // 只会画出 body 根 View 的上下边框，中间内容全部空白。
            flex(1f)
            flexDirectionRow()
            scrollEnable(tableAttr.scrollEnabled)
        }
        event {
            scroll { params ->
                tableEvent.onScroll?.invoke(params.offsetX, params.offsetY)
            }
        }

        View {
            attr {
                flexDirectionColumn()
                width(tableAttr.totalWidth)
            }

            if (tableAttr.headerVisible) {
                renderHeaderRow(columns, theme, tableEvent)
            }

            val hasMaxHeight = tableAttr.maxHeightValue != Float.MAX_VALUE
            Scroller {
                attr {
                    // 始终设 flex(1f)：即使没有 maxHeight，也要让纵向 Scroller
                    // 撑满列容器剩余空间（表头以下），确保行内容在 ComposeView
                    // 有高度分配时正确布局。没有 maxHeight 时不限制最大高度，
                    // 表格自然撑开；有 maxHeight 时 maxHeight 起裁剪+滚动作用。
                    flex(1f)
                    flexDirectionColumn()
                    scrollEnable(tableAttr.scrollEnabled)
                    if (hasMaxHeight) {
                        maxHeight(tableAttr.maxHeightValue)
                    }
                }
                event {
                    scroll { params ->
                        tableEvent.onScroll?.invoke(params.offsetX, params.offsetY)
                    }
                }

                rows.forEachIndexed { rowIndex, rowData ->
                    renderBodyRow(rowIndex, rowData, columns, theme, tableEvent)
                }
            }
        }
    }
}

// --- Fixed columns layout: left panel fixed, right panel scrollable ---

private fun ViewContainer<*, *>.renderFixedColumnsLayout(
    columns: List<ColumnDef>,
    rows: List<List<String>>,
    fixedCols: Int,
    theme: TableTheme,
    tableAttr: TableAttr,
    tableEvent: TableEvent
) {
    val fixedColumns = columns.take(fixedCols)
    val scrollableColumns = columns.drop(fixedCols)

    View {
        attr {
            flexDirectionRow()
            flex(1f)
            if (tableAttr.maxHeightValue != Float.MAX_VALUE) {
                maxHeight(tableAttr.maxHeightValue)
            }
        }

        // Left fixed panel
        View {
            attr {
                flexDirectionColumn()
                width(tableAttr.fixedWidth)
            }

            if (tableAttr.headerVisible) {
                renderHeaderRow(fixedColumns, theme, tableEvent)
            }

            Scroller {
                attr {
                    flex(1f)
                    flexDirectionColumn()
                    scrollEnable(tableAttr.scrollEnabled)
                    syncScroll(true)
                }
                event {
                    scroll { params ->
                        tableEvent.onScroll?.invoke(params.offsetX, params.offsetY)
                    }
                }

                rows.forEachIndexed { rowIndex, rowData ->
                    val fixedRowData = rowData.take(fixedCols)
                    renderBodyRow(rowIndex, fixedRowData, fixedColumns, theme, tableEvent)
                }
            }
        }

        // Right scrollable panel
        Scroller {
            attr {
                flex(1f)
                flexDirectionRow()
                scrollEnable(tableAttr.scrollEnabled)
            }
            event {
                scroll { params ->
                    tableEvent.onScroll?.invoke(params.offsetX, params.offsetY)
                }
            }

            View {
                attr {
                    flexDirectionColumn()
                    width(tableAttr.scrollableWidth)
                }

                if (tableAttr.headerVisible) {
                    renderHeaderRow(scrollableColumns, theme, tableEvent, colOffset = fixedCols)
                }

                Scroller {
                    attr {
                        flex(1f)
                        flexDirectionColumn()
                        scrollEnable(tableAttr.scrollEnabled)
                        syncScroll(true)
                    }
                    event {
                        scroll { params ->
                            tableEvent.onScroll?.invoke(params.offsetX, params.offsetY)
                        }
                    }

                    rows.forEachIndexed { rowIndex, rowData ->
                        val scrollableRowData = rowData.drop(fixedCols)
                        renderBodyRow(rowIndex, scrollableRowData, scrollableColumns, theme, tableEvent, colOffset = fixedCols)
                    }
                }
            }
        }
    }
}

// --- Shared rendering helpers ---

private fun ViewContainer<*, *>.renderHeaderRow(
    columns: List<ColumnDef>,
    theme: TableTheme,
    event: TableEvent,
    colOffset: Int = 0
) {
    View {
        attr {
            flexDirectionRow()
            height(theme.headerHeight)
            backgroundColor(theme.headerBackgroundColor)
            accessibility("表头行")
        }
        if (theme.showRowBorder) {
            addBorderBottom(this, theme.borderWidth, theme.borderColor)
        }

        columns.forEachIndexed { index, column ->
            val actualColIndex = index + colOffset

            View {
                attr {
                    width(column.width)
                    height(theme.headerHeight)
                    justifyContent(FlexJustifyContent.CENTER)
                    alignItems(column.headerAlign.toFlexAlign())
                    padding(theme.cellPaddingVertical, theme.cellPaddingHorizontal, theme.cellPaddingVertical, theme.cellPaddingHorizontal)
                    accessibility("第${actualColIndex + 1}列表头：${column.title}")
                }
                if (theme.showColumnBorder && index < columns.size - 1) {
                    addBorderRight(this, theme.borderWidth, theme.borderColor)
                }

                event {
                    click {
                        event.onHeaderClick?.invoke(actualColIndex, column.key)
                    }
                }

                Text {
                    attr {
                        text(column.title)
                        fontSize(theme.headerFontSize)
                        if (theme.headerFontBold) {
                            fontWeightBold()
                        }
                        color(theme.headerTextColor)
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.renderBodyRow(
    rowIndex: Int,
    rowData: List<String>,
    columns: List<ColumnDef>,
    theme: TableTheme,
    event: TableEvent,
    colOffset: Int = 0
) {
    val bgColor = if (theme.stripedRows && rowIndex % 2 == 1) {
        theme.rowAlternateColor
    } else {
        theme.rowBackgroundColor
    }

    View {
        attr {
            flexDirectionRow()
            height(theme.rowHeight)
            backgroundColor(bgColor)
            accessibility("第${rowIndex + 1}行")
        }
        if (theme.showRowBorder) {
            addBorderBottom(this, theme.borderWidth, theme.borderColor)
        }

        event {
            click {
                event.onRowClick?.invoke(rowIndex)
            }
        }

        columns.forEachIndexed { index, column ->
            val actualColIndex = index + colOffset
            val value = rowData.getOrElse(index) { "" }
            val cellData = CellData(
                rowIndex = rowIndex,
                colIndex = actualColIndex,
                value = value,
                columnKey = column.key
            )

            View {
                attr {
                    width(column.width)
                    height(theme.rowHeight)
                    justifyContent(FlexJustifyContent.CENTER)
                    alignItems(column.align.toFlexAlign())
                    padding(theme.cellPaddingVertical, theme.cellPaddingHorizontal, theme.cellPaddingVertical, theme.cellPaddingHorizontal)
                    accessibility("第${rowIndex + 1}行第${actualColIndex + 1}列：$value")
                }
                if (theme.showColumnBorder && index < columns.size - 1) {
                    addBorderRight(this, theme.borderWidth, theme.borderColor)
                }

                event {
                    click {
                        event.onCellClick?.invoke(rowIndex, actualColIndex, value)
                    }
                }

                if (column.customRenderer != null) {
                    column.customRenderer!!.invoke(this, cellData, rowIndex, actualColIndex)
                } else {
                    Text {
                        attr {
                            text(value)
                            fontSize(theme.cellFontSize)
                            color(theme.cellTextColor)
                        }
                    }
                }
            }
        }
    }
}
