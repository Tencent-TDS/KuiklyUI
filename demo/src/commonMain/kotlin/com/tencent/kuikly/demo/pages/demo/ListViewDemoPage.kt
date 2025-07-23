/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.BaseObject
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.base.Skew
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*

internal class ListItem(private val scope: PagerScope) : BaseObject() {
    var title: String by scope.observable("")
    var lifeCycleTitle: String by scope.observable("")
}

@Page("ListViewDemoPage")
internal class ListViewDemoPage : BasePager() {

    private lateinit var refreshRef : ViewRef<RefreshView>
    private lateinit var footerRefreshRef : ViewRef<FooterRefreshView>
    private var dataList: ObservableList<ListItem> by observableList()
    private var refreshText by observable( "下拉刷新")
    private var footerRefreshText by observable( "加载更多")
    private var redBlockStickOriginTop = 300f
    private var redBlockStickTop by observable(redBlockStickOriginTop)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            // navBar
            NavBar {
                attr {
                    title = "List组件Demo"
                }
            }

            List {
                attr {
                    flex(1f)
                }
                event {
                    scroll {

                    }
                }

                Refresh {
                    ref {
                        ctx.refreshRef = it
                    }
                    attr {
                        height(50f)
                        allCenter()
                    }
                    Text {
                        attr {
                            color(Color.BLACK)
                            fontSize(20f)
                            text(ctx.refreshText)
                            transform(Skew(-10f, 0f))
                        }
                    }
                    event {
                        refreshStateDidChange {
                            when(it) {
                                RefreshViewState.REFRESHING -> {
                                    ctx.refreshText = "正在刷新"
                                    setTimeout(2000) {
                                        ctx.dataList.clear()
                                        for (index in 0..10) {
                                            val item = ListItem(ctx)
                                            item.title =  "我是第${ctx.dataList.count()}个卡片"
                                            ctx.dataList.add(item)
                                        }
                                        ctx.refreshRef.view?.endRefresh()
                                        ctx.footerRefreshRef.view?.resetRefreshState() // 刷新成功后，需要重置尾部刷新状态
                                    }
                                }
                                RefreshViewState.IDLE -> ctx.refreshText = "下拉刷新"
                                RefreshViewState.PULLING -> ctx.refreshText =  "松手即可刷新"
                            }
                        }
                    }
                }

                vfor({ ctx.dataList }) { item ->

                    EasyCard {
                        attr {
                            //title = item.title
                            listItem = item
                        }
                        event {
                            titleDidClick {
                                KLog.i("ListViewDemoPage", "did fire titleDidClick")
                            }
                        }
                    }
                }

                Hover {
                    ref {
                      //  ctx.redBlockRef = it
                    }
                    attr {
                        absolutePosition(top = ctx.redBlockStickTop, left =0f, right =0f)
                        height(50f)
                        backgroundColor(Color.RED)
                    }
                }

                Hover {
                    ref {
                        //  ctx.redBlockRef = it
                    }
                    attr {
                        absolutePosition(top = 600f, left =0f, right =0f)
                        height(50f)
                        backgroundColor(Color.BLUE)
                    }
                }

                Hover {
                    ref {
                        //  ctx.redBlockRef = it
                    }
                    attr {
                        absolutePosition(top = 900f, left =0f, right =0f)
                        height(50f)
                        backgroundColor(Color.YELLOW)
                    }
                }

                Hover {
                    ref {
                        //  ctx.redBlockRef = it
                    }
                    attr {
                        absolutePosition(top = 1200f, left =0f, right =0f)
                        height(50f)
                        backgroundColor(Color.BLACK)
                    }
                }

                vif({ctx.dataList.isNotEmpty()}) {
                    FooterRefresh {
                        ref {
                            ctx.footerRefreshRef = it
                        }
                        attr {
                            preloadDistance(600f)
                            allCenter()
                            height(60f)
                        }
                        event {
                            refreshStateDidChange {
                                KLog.i("ListViewDemoPage", "refreshStateDidChange : $it")
                                when(it) {
                                    FooterRefreshState.REFRESHING -> {
                                        ctx.footerRefreshText = "加载更多中.."
                                        setTimeout(1000) {
                                            if (ctx.dataList.count() > 100) {
                                                ctx.footerRefreshRef.view?.endRefresh(FooterRefreshEndState.NONE_MORE_DATA)
                                            } else {
                                                for (index in 0..10) {
                                                    val item = ListItem(ctx)
                                                    item.title =  "我是第${ctx.dataList.count()}个卡片"
                                                    ctx.dataList.add(item)
                                                }
                                                ctx.footerRefreshRef.view?.endRefresh(FooterRefreshEndState.SUCCESS)
                                            }
                                        }
                                    }
                                    FooterRefreshState.IDLE -> ctx.footerRefreshText = "加载更多"
                                    FooterRefreshState.NONE_MORE_DATA -> ctx.footerRefreshText = "无更多数据"
                                    FooterRefreshState.FAILURE -> ctx.footerRefreshText = "点击重试加载更多"
                                    else -> {}
                                }
                            }
                            click {
                                // 点击重试
                                ctx.footerRefreshRef.view?.beginRefresh()
                            }
                        }

                        Text {
                            attr {
                                color(Color.BLACK)
                                fontSize(20f)
                                text(ctx.footerRefreshText)
                            }
                        }

                    }
                }

            }

            Button {
                attr {
                    absolutePosition(bottom = 50f, right = 50f)
                    size(100f, 100f)
                    borderRadius(50f)
                    backgroundColor(Color.BLUE)
                    titleAttr {
                        text("点击刷新")
                        color(Color.WHITE)
                        fontSize(16f)
                    }
                }
                event {
                    click {
                        ctx.refreshRef.view?.beginRefresh()
                    }
                }

            }
        }
    }

    override fun created() {
        super.created()
        for (index in 0..6) {
            val item = ListItem(this)
            item.title =  "我是第${dataList.count()}个卡片"
            dataList.add(item)
        }
    }

}
