package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.FooterRefresh
import com.tencent.kuikly.core.views.FooterRefreshState
import com.tencent.kuikly.core.views.FooterRefreshView
import com.tencent.kuikly.core.views.Hover
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Refresh
import com.tencent.kuikly.core.views.RefreshView
import com.tencent.kuikly.core.views.RefreshViewState
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.random.Random

@Page("vrefresh")
internal class VforLazyRefreshPage : BasePager() {

    private lateinit var refreshRef: ViewRef<RefreshView>
    private lateinit var footerRef: ViewRef<FooterRefreshView>
    private lateinit var listRef: ViewRef<ListView<*, *>>
    private var state by observable(RefreshViewState.IDLE.toString())
    private var footerState by observable(FooterRefreshState.IDLE.toString())
    private val list by observableList<DemoListItem>()

    override fun willInit() {
        super.willInit()
        list.addAll(generateList())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "VforLazyRefresh"
                }
            }
            List {
                ref { ctx.listRef = it }
                attr {
                    flex(1f)
                }
                Refresh {
                    ref { ctx.refreshRef = it }
                    attr {
                        height(100f)
                        backgroundColor(Color.GRAY)
                        allCenter()
                    }
                    event {
                        refreshStateDidChange {
                            ctx.state = it.toString()
                            if (it == RefreshViewState.REFRESHING) {
                                ctx.refreshList()
                            }
                        }
                    }
                    Text {
                        attr { text(ctx.state) }
                    }
                }
                Hover {
                    attr {
                        backgroundColor(Color.WHITE)
                    }
                    // 滚动按钮
                    View {
                        attr {
                            flexDirectionRow()
                            justifyContentSpaceBetween()
                        }
                        Button {
                            attr {
                                flex(1f)
                                height(36f)
                                borderRadius(6f)
                                margin(4f)
                                backgroundColor(Color(0xFFFF0098))
                                highlightBackgroundColor(Color(0x66FFFFFF))
                                titleAttr {
                                    text("刷新")
                                    color(Color.WHITE)
                                    fontSize(12f)
                                }
                            }
                            event {
                                click {
                                    ctx.refreshList()
                                }
                            }
                        }
                        Button {
                            attr {
                                flex(1f)
                                height(36f)
                                borderRadius(6f)
                                margin(4f)
                                backgroundColor(Color(0xFFFF9800)) // 橙色
                                highlightBackgroundColor(Color(0x66FFFFFF))
                                titleAttr {
                                    text("顶部")
                                    color(Color.WHITE)
                                    fontSize(12f)
                                }
                            }
                            event {
                                click {
                                    ctx.listRef.view?.apply {
                                        val indexOfVforLazy = 2
                                        val offset = (contentView?.getChild(indexOfVforLazy) as? ViewContainer<*, *>)?.getChild(0)?.frame?.y ?: 0f
                                        scrollToPosition(indexOfVforLazy, -offset, true)
                                    }
                                }
                            }
                        }
                        Button {
                            attr {
                                flex(1f)
                                height(36f)
                                borderRadius(6f)
                                margin(4f)
                                backgroundColor(Color(0xFF9C27B0)) // 紫色
                                highlightBackgroundColor(Color(0x66FFFFFF))
                                titleAttr {
                                    text("底部")
                                    color(Color.WHITE)
                                    fontSize(12f)
                                    fontWeight500()
                                }
                            }
                            event {
                                click {
                                    val indexOfVforLazy = 2
                                    ctx.listRef.view?.scrollToPosition(209 + indexOfVforLazy, 0f, true)
                                }
                            }
                        }
                    }
                }
                vforLazy({ ctx.list }) { item, index, count ->
                    View {
                        attr {
                            height(item.height)
                            backgroundColor(Color(item.color))
                            margin(8f)
                            justifyContentCenter()
                            borderBottom(Border(0.5f, BorderStyle.SOLID, Color.GRAY))
                        }
                        Text {
                            attr {
                                text(item.title)
                                fontSize(17f)
                                fontWeight400()
                                color(Color.BLACK)
                                marginBottom(4f)
                            }
                        }
                        Text {
                            attr {
                                text("showIndex: $index, height: ${item.height.toInt()}")
                                fontSize(14f)
                                color(Color(0xFF666666))
                            }
                        }
                    }
                }
                FooterRefresh {
                    ref { ctx.footerRef = it }
                    attr {
                        height(100f)
                        backgroundColor(Color.GRAY)
                        allCenter()
                    }
                    event {
                        refreshStateDidChange {
                            ctx.footerState = it.toString()
                        }
                    }
                    Text {
                        attr { text(ctx.state) }
                    }
                }
            }
        }
    }

    private fun generateList(): List<DemoListItem> {
        val result = mutableListOf<DemoListItem>()
        val random = Random.Default
        for (i in 0..29) {
            //
            val randomHeight = 50f
            val contentType = "卡片高度${randomHeight.toInt()}"

            result.add(
                DemoListItem(
                    i.toString(),
                    "$contentType-数据index:$i",
                    randomHeight,
                    0xFF000000L or random.nextInt(0x000000, 0xFFFFFF).toLong(),
                    contentType
                )
            )
        }
        for (i in 30 until 180) {
            // 生成50-200的随机高度
            val randomHeight = 200f
            val contentType = "卡片高度${randomHeight.toInt()}"

            result.add(
                DemoListItem(
                    i.toString(),
                    "$contentType-数据index:$i",
                    randomHeight,
                    0xFF000000L or random.nextInt(0x000000, 0xFFFFFF).toLong(),
                    contentType
                )
            )
        }
        for (i in 180 until 210) {
            val randomHeight = 50f
            val contentType = "卡片高度${randomHeight.toInt()}"

            result.add(
                DemoListItem(
                    i.toString(),
                    "$contentType-数据index:$i",
                    randomHeight,
                    0xFF000000L or random.nextInt(0x000000, 0xFFFFFF).toLong(),
                    contentType
                )
            )
        }
        return result
    }

    private fun refreshList() {
        setTimeout(1000) {
            list.clear()
            list.addAll(generateList())
            refreshRef.view?.endRefresh()
        }
    }

}

private data class DemoListItem(
    val id: String,
    val title: String,
    val height: Float,
    val color: Long,
    val type: String
)
