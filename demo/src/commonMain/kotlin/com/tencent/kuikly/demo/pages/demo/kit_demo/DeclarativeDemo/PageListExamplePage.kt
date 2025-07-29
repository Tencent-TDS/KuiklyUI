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

package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("PageListExamplePage")
internal class PageListExamplePage : BasePager() {
    var currentIndex by observable(0)
    var tabItems by observableList<TabItem>()
    var pageItems by observableList<PageItem>()
    lateinit var pageListRef : ViewRef<PageListView<*, *>>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {

            }

            NavBar {
                attr {
                    title = "PageList Example"
                }
            }
            View {
                attr {
                    flexDirectionRow()
                    height(60f)
                    justifyContentSpaceEvenly()
                }
                vfor({ctx.tabItems}) { tabItem ->
                    View {
                        attr { allCenter() }
                        Text {
                            attr {
                                color(if (tabItem.index == ctx.currentIndex) Color.RED else Color.BLACK)
                                fontSize(16f)
                                fontWeight500()
                                text("tab" + tabItem.index)
                            }
                        }
                        event {
                            click {
                                ctx.pageListRef.view!!.setContentOffset(tabItem.index * getPager().pageData.pageViewWidth, 0f, true)
                            }
                        }
                    }

                }
            }

            PageList {
                ref {
                    ctx.pageListRef = it
                }
                attr {
                    flex(1f) // 高度撑到底部
                    pageDirection(true)
                    pageItemWidth(pagerData.pageViewWidth)
                    pageItemHeight(pagerData.pageViewHeight - pagerData.navigationBarHeight - 60f)
                    showScrollerIndicator(false)
                    keepItemAlive(true)
                }
                vfor({ctx.pageItems}) { pageItem ->
                    List {
                        vfor({pageItem.dataList}) { cardData ->
                            View {
                                attr {
                                    height(60f)
                                    allCenter()
                                    backgroundColor(Color((0..255).random(), (0..255).random(), (0..255).random(), 1.0f))
                                }
                                Text {
                                    attr {
                                        fontSize(16f)
                                        color(Color.WHITE)
                                        text(cardData.title)
                                    }
                                }
                            }
                        }

                    }
                }

                event {
                    pageIndexDidChanged {
                        ctx.currentIndex = (it as JSONObject).optInt("index")
                    }

                }
            }

        }
    }

    override fun created() {
        super.created()
        // mock数据
        for (i in 0 until 5) {
            pageItems.add(PageItem().apply {
                for (j in 0 until 30) {
                    dataList.add(CardData().apply {
                        title = "pageIndex:" + i + " listIndex:" + j
                    })
                }

            })
            tabItems.add(TabItem().apply {
                index = i
                title = "tab" + i
            })
        }
    }
}

internal class TabItem {
    var index : Int = 0
    var title : String = ""
}

internal class PageItem {
    var dataList by observableList<CardData>()
}

internal class  CardData {
    var title by observable("")
}