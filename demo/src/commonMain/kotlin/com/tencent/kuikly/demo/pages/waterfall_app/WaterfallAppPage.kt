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

package com.tencent.kuikly.demo.pages.waterfall_app

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.waterfall_app.components.BottomNavigationBar
import com.tencent.kuikly.demo.pages.waterfall_app.components.WaterfallEmptyView
import com.tencent.kuikly.demo.pages.waterfall_app.pages.WaterfallHomePage
import com.tencent.kuikly.demo.pages.waterfall_app.pages.WaterfallMessagePage
import com.tencent.kuikly.demo.pages.waterfall_app.pages.WaterfallProfilePage
import com.tencent.kuikly.demo.pages.waterfall_app.pages.CardDetailPage
import com.tencent.kuikly.demo.pages.waterfall_app.models.WaterFallItem
import com.tencent.kuikly.core.log.KLog

/**
 * 页面栈项数据类
 */
internal data class PageStackItem(
    val pageType: String,
    val data: Any? = null
)

/**
 * 瀑布流APP主页面
 */
@Page("waterfallapp")
internal class WaterfallAPP : BasePager() {
    private var currentTabIndex by observable(0) // 当前选中的底部导航栏索引
    private var pageListRef: ViewRef<PageListView<*, *>>? = null
    private val pageNames = listOf("首页", "热门", "消息", "我")

    private var isShowingCardDetail by observable(false)
    private var currentDetailItem: WaterFallItem? by observable(null)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
//                backgroundColor(Color(0xFFF5F5F5))
                backgroundColor(Color.WHITE)
            }

            // 主页面内容 - 使用PageList实现页面切换
            PageList {
                ref {
                    ctx.pageListRef = it
                }
                attr {
                    flex(1f)
                    flexDirectionRow()
                    pageItemWidth(ctx.pagerData.pageViewWidth)
                    defaultPageIndex(0)
                    showScrollerIndicator(false)
                    scrollEnable(false) // 禁用手势滑动，只能通过底部导航切换
                    keepItemAlive(true) // 保持页面状态
                }

                // 首页 - 瀑布流内容
                WaterfallHomePage(ctx.pagerData.pageViewWidth, 2) {
                    event {
                        onTopTabClick = { index, title ->
                            KLog.i("WaterfallApp", "点击顶部导航项: $title")
                        }
                        onCardClick = { item ->
                            KLog.i("WaterfallApp", "点击卡片: ${item.content}")
                            ctx.showCardDetail(item)
                        }
                    }
                }

                // 热门页面 - 空页面
                WaterfallEmptyView("热门页面") {}

                // 消息页面 - 消息列表页面  
                WaterfallMessagePage {}

                // 我的页面 - 个人资料页面
                WaterfallProfilePage {}
            }

            // 底部导航栏
            BottomNavigationBar(ctx.pageNames) {
                attr {
                    currentTabIndex = ctx.currentTabIndex
                }
                event {
                    onTabClick = { index, title ->
                        ctx.pageListRef?.view?.scrollToPageIndex(index)
                        ctx.currentTabIndex = index
                        KLog.i("WaterfallApp", "点击导航项: $title")
                    }
                    onCreateClick = {
                        KLog.i("WaterfallApp", "点击创作按钮")
                    }
                }
            }

            // 卡片详情页面 - 使用vif指令控制显示
            vif({ ctx.isShowingCardDetail }) {
                ctx.currentDetailItem?.let { item ->
                    View {
                        attr {
                            absolutePosition(0f, 0f, 0f, 0f) // 全屏覆盖
                            backgroundColor(Color.WHITE)
                        }
                        
                        CardDetailPage(item, ctx.pagerData.pageViewWidth) {
                            event {
                                onBackClick = {
                                    ctx.hideCardDetail()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 显示卡片详情页面
     */
    private fun showCardDetail(item: WaterFallItem) {
        KLog.i("WaterfallApp", "显示卡片详情: ${item.content}")
        currentDetailItem = item
        isShowingCardDetail = true
    }
    
    /**
     * 隐藏卡片详情页面
     */
    private fun hideCardDetail() {
        isShowingCardDetail = false
        currentDetailItem = null
    }
}
