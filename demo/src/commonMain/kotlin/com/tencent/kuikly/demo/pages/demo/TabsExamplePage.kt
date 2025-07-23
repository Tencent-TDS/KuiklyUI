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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.ScrollParams
import com.tencent.kuikly.core.views.TabItem
import com.tencent.kuikly.core.views.Tabs
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.random.Random

internal class TabItemData {
    var tabTitle by observable("")
    var pageBgColor by observable(Color.WHITE)
    var index by observable(0)
}

@Page("TabsExamplePage")
internal class TabsExamplePage : BasePager() {

    private var pageListRef : ViewRef<PageListView<*, *>>? = null
    private var scrollParams : ScrollParams? by observable(null)

    private var tabDataList by observableList<TabItemData>()

    private var defaultIndex = 2

    override fun created() {
        super.created()
        for (i in 0..20) {
            val tabItemData = TabItemData().apply {
                tabTitle = "tabName$i"
                pageBgColor = generateRandomColor()
                index = i
            }
            tabDataList.add(tabItemData)
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "TabsExamplePage"
                }
            }

            Tabs {
                attr {
                    indicatorAlignCenter()
                    height(50f) // 横向布局，务必指定高度
                    defaultInitIndex(ctx.defaultIndex)
                  //  indicatorAlignCenter()
                    indicatorInTabItem {
                        View {
                            attr {
                                absolutePosition(left = 15f, right = 15f, bottom = 5f)
                                height(6f)
                                borderRadius(3f)
                                backgroundColor(Color.BLUE)
                            }
                        }
                    }
                    ctx.scrollParams?.also {
                        scrollParams(it)
                    }
                }

                vfor({ctx.tabDataList}) { tabItem ->
                    TabItem { state ->
                        attr {
                            marginLeft(10f)
                            marginRight(10f)
                            allCenter()
                        }
                        event {
                            click {
                                ctx.pageListRef?.view?.scrollToPageIndex(tabItem.index, false)
                            }
                        }
                        Text {
                            attr {
                                text(tabItem.tabTitle)
                                fontSize(17f)
                                if (state.selected) {
                                    color(Color.BLUE)
                                } else {
                                    color(Color.BLACK)
                                }
                            }
                        }
                    }
                }
            }

            PageList {
                attr {
                    flexDirectionRow()
                    pageItemWidth(pagerData.pageViewWidth)
                    pageItemHeight(pagerData.pageViewHeight - pagerData.statusBarHeight - 44 - 50)
                    defaultPageIndex(ctx.defaultIndex)
                    offscreenPageLimit(1)
                }
                ref {
                    ctx.pageListRef = it
                }
                event {
                    scroll {
                        ctx.scrollParams = it
                    }
                }
                // create pageItems
                vfor({ctx.tabDataList}) { item ->
                    View {
                        attr {
                            backgroundColor(item.pageBgColor)
                            allCenter()
                        }
                        Text {
                            attr {
                                text("pageIndex" + item.index)
                                fontSize(30f)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun generateRandomColor(): Color {
        val r = Random.nextInt(256)
        val g = Random.nextInt(256)
        val b = Random.nextInt(256)
        return Color(r, g, b, 1f)
    }
}