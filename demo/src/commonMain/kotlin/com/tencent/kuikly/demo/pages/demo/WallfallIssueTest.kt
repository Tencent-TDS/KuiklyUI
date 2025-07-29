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
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.WaterfallList
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

internal data class TabData(var tabTitle: String = "", var index: Int = 0)

internal data class ItemData(var title: String = "")

@Page("222")
internal class WallfallIssueTest : BasePager() {
    private val tabDataList by observableList<TabData>()
    private val itemsList0 by observableList<ItemData>()
    private val itemsList1 by observableList<ItemData>()

    override fun created() {
        super.created()
        //
        tabDataList.add(TabData("特权tab1", 0))
        tabDataList.add(TabData("特权tab2", 1))
        itemsList0.clear()
        val list = arrayListOf<ItemData>().apply {
            add(ItemData("11我司一个文本表达的打发打发水电费0"))
            add(ItemData("1我司一个文本表达的打发打发水电费1"))
            add(ItemData("1我司一个文本表达的打发打发水电费2"))
            add(ItemData("1我司一个文本表达的打发打发水电费3"))
            add(ItemData("1我司一个文本表达的打发打发水电费4"))
            add(ItemData("1我司一个文本表达的打发打发水电费5"))
        }

        itemsList1.clear()
        val list2 = arrayListOf<ItemData>().apply {
            add(ItemData("2我司一个文本表达的打发打发水电费0"))
            add(ItemData("2我司一个文本表达的打发打发水电费1"))
            add(ItemData("2我司一个文本表达的打发打发水电费2"))
            add(ItemData("2我司一个文本表达的打发打发水电费3"))
            add(ItemData("2我司一个文本表达的打发打发水电费4"))
            add(ItemData("2我司一个文本表达的打发打发水电费5"))
        }
        setTimeout(3000) {
            itemsList0.clear()
            val list = arrayListOf<ItemData>().apply {
                add(ItemData("我司一个文本表达的打发打发水电费0"))
                add(ItemData("我司一个文本表达的打发打发水电费1"))
                add(ItemData("我司一个文本表达的打发打发水电费2"))
                add(ItemData("我司一个文本表达的打发打发水电费3"))
                add(ItemData("我司一个文本表达的打发打发水电费4"))
                add(ItemData("我司一个文本表达的打发打发水电费5"))
            }
            itemsList0.addAll(list)

            itemsList1.clear()
            val list2 = arrayListOf<ItemData>().apply {
                add(ItemData("2我司一个文本表达的打发打发水电费0"))
                add(ItemData("2我司一个文本表达的打发打发水电费1"))
                add(ItemData("2我司一个文本表达的打发打发水电费2"))
                add(ItemData("2我司一个文本表达的打发打发水电费3"))
                add(ItemData("2我司一个文本表达的打发打发水电费4"))
                add(ItemData("2我司一个文本表达的打发打发水电费5"))
            }
            itemsList1.addAll(list2)
        }

    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {

            NavBar {
                attr {
                    title = "WallfallIssueTest"
                }
            }

            PageList {
                attr {
                    flex(1f)
                    flexDirectionRow()
                    marginLeft(16f)
                    marginRight(16f)
                }
                event {
                    contentSizeChanged { width, height ->
                        KLog.i("2", "contentSizeWidth:${width}")
                    }
                }
                vfor({ ctx.tabDataList }) { tab ->
                    WaterfallList {
                        attr {
                            flex(1f)

                            firstContentLoadMaxIndex(1000)
                            listWidth(pagerData.pageViewWidth - 32f)
                            columnCount(4)
                            itemSpacing(20f)
                            lineSpacing(20f)

                        }
                        View {
                            attr {
                                height(40f)
                                width(pagerData.pageViewWidth - 32f)
                                if (tab.index == 0) {
                                    backgroundColor(Color.RED)
                                } else {
                                    backgroundColor(Color.BLUE)
                                }
                            }
                        }

                        vfor({
                            if (tab.index == 0) {
                                ctx.itemsList0
                            } else {
                                ctx.itemsList1
                            }
                        }) {
                            View {
                                Text {
                                    attr {
                                        fontSize(20f)
                                        color(Color.BLACK)
                                        text(it.title)
                                    }
                                }
                            }
                        }

                    }
                }

            }
        }
    }
}