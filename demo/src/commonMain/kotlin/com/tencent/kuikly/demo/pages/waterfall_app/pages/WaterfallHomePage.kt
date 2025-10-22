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

package com.tencent.kuikly.demo.pages.waterfall_app.pages

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Refresh
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.WaterfallList
import com.tencent.kuikly.demo.pages.waterfall_app.data.WaterfallDataProvider
import com.tencent.kuikly.demo.pages.waterfall_app.models.WaterFallItem
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.views.RefreshViewState

/**
 * 瀑布流首页组件
 */
internal class WaterfallHomePage(
    private val pageViewWidth: Float,
    private val columnCount: Int = 2
) : ComposeView<WaterfallHomePageAttr, WaterfallHomePageEvent>() {
    
    private var dataList: ObservableList<WaterFallItem> by observableList<WaterFallItem>()
    private var currentTopTabIndex by observable(1) // 当前选中的顶部导航栏索引
    private var isLoading: Boolean by observable(true)
    private var loadError: String? by observable(null)
    private var currentPage: Int = 1
    private val pageSize: Int = 20
    // 移除coroutineScope，使用Kuikly内建协程
    
    override fun createEvent(): WaterfallHomePageEvent {
        return WaterfallHomePageEvent()
    }

    override fun createAttr(): WaterfallHomePageAttr {
        return WaterfallHomePageAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flex(1f)
                flexDirectionColumn()
                backgroundColor(Color.WHITE)
            }

            // 首页顶部导航栏
            View {
                attr {
                    height(70f)
                    backgroundColor(Color.WHITE)
                    paddingTop(15f)
                }

                // 顶部导航项容器
                View {
                    attr {
                        height(65f)
                        flexDirectionRow()
                        alignItemsCenter()
                        justifyContentCenter()
                        padding(0f, 0f)
                    }

                    // 关注
                    ctx.createTopTabItem("关注", 0).invoke(this)

                    // 发现
                    ctx.createTopTabItem("发现", 1).invoke(this)

                    // 深圳
                    ctx.createTopTabItem("深圳", 2).invoke(this)
                }
            }

            // 瀑布流内容
            if (ctx.isLoading && ctx.dataList.isEmpty()) {
                // 首次加载状态
                View {
                    attr {
                        flex(1f)
                        justifyContentCenter()
                        alignItemsCenter()
                    }
                    Text {
                        attr {
                            text("正在加载内容...")
                            fontSize(16f)
                            color(Color(0xFF999999))
                        }
                    }
                }
            } else if (ctx.loadError != null && ctx.dataList.isEmpty()) {
                // 加载错误状态
                View {
                    attr {
                        flex(1f)
                        justifyContentCenter()
                        alignItemsCenter()
                        flexDirectionColumn()
                    }
                    Text {
                        attr {
                            text("加载失败: ${ctx.loadError}")
                            fontSize(16f)
                            color(Color(0xFFFF4444))
                            marginBottom(10f)
                        }
                    }
                        View {
                            attr {
                                backgroundColor(Color(0xFFFF2442))
                                padding(left = 20f, right = 20f, top = 10f, bottom = 10f)
                                borderRadius(5f)
                            }
                        Text {
                            attr {
                                text("重试")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                KLog.i("WaterfallHomePage", "用户点击重试按钮")
                                ctx.initData()
                            }
                        }
                    }
                }
            } else {
                WaterfallList {
                    attr {
                        flex(1f)
                        columnCount(ctx.columnCount)
                        listWidth(ctx.pageViewWidth)
                        lineSpacing(10f)
                        itemSpacing(10f)
                    }

                    Refresh {
                        attr {
                            height(50f)
                            backgroundColor(Color.WHITE)
                        }
                        event {
                            refreshStateDidChange { state ->
                                if (state == RefreshViewState.REFRESHING) {
                                    KLog.i("WaterfallHomePage", "用户下拉刷新")
                                    ctx.refreshData()
                                }
                            }
                        }
                    }

                    vforIndex({ ctx.dataList }) { item, index, _ ->
                        ctx.createWaterfallCard(item).invoke(this)
                    }
                    
                    // 加载更多指示器
                    if (ctx.isLoading && ctx.dataList.isNotEmpty()) {
                        View {
                            attr {
                                height(50f)
                                justifyContentCenter()
                                alignItemsCenter()
                            }
                            Text {
                                attr {
                                    text("正在加载更多...")
                                    fontSize(14f)
                                    color(Color(0xFF999999))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 创建顶部导航项
     */
    private fun createTopTabItem(title: String, index: Int): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    width(50f)
                    height(40f)
                    allCenter()
                    marginLeft(15f)
                    marginRight(15f)
                }

                Text {
                    attr {
                        text(title)
                        fontSize(16f)
                        color(if (ctx.currentTopTabIndex == index) Color(0xFFFF2442) else Color(0xFF666666))
                        if (ctx.currentTopTabIndex == index) fontWeightBold()
                    }
                }

                event {
                    click {
                        ctx.currentTopTabIndex = index
                        KLog.i("WaterfallHomePage", "点击顶部导航项: $title")
                        ctx.event.onTopTabClick?.invoke(index, title)
                    }
                }
            }
        }
    }
    
    /**
     * 创建瀑布流卡片
     */
    private fun createWaterfallCard(item: WaterFallItem): ViewBuilder {
        val ctx = this
        return {
            // 瀑布流风格卡片
            View {
                attr {
                    backgroundColor(Color.WHITE)
                    borderRadius(4f)
                    flexDirectionColumn()
                }

                // 主图片
                Image {
                    attr {
                        val cardWidth = ctx.pageViewWidth / ctx.columnCount // 计算单个卡片宽度
                        src(item.imageUrl)
                        borderRadius(4f)
                        flex(1f)
                        size(cardWidth, (item.imageHeight / item.imageWidth) * cardWidth) // 按照比例计算高度
                    }
                }

                // 内容区域
                View {
                    attr {
                        flex(1f)
                        backgroundColor(Color.WHITE)
                        borderRadius(0f, 0f, 12f, 12f)
                        padding(8f)
                        flexDirectionColumn()
                        justifyContentSpaceBetween()
                    }

                    // 内容文字
                    Text {
                        attr {
                            text(item.content)
                            fontSize(12f)
                            color(Color.BLACK)
                            lineHeight(16f)
                            marginBottom(8f)
                        }
                    }
                    
                    // 底部用户信息
                    View {
                        attr {
                            height(24f)
                            flexDirectionRow()
                            alignItemsCenter()
                        }
                        
                        // 用户头像
                        Image {
                            attr {
                                width(16f)
                                height(16f)
                                src(item.userAvatar)
                                borderRadius(8f)
                            }
                        }
                        
                        // 用户昵称
                        Text {
                            attr {
                                text(item.userNick)
                                fontSize(10f)
                                color(Color(0xFF666666))
                                marginLeft(4f)
                            }
                        }
                    }
                }
                
                event {
                    click {
                        ctx.event.onCardClick?.invoke(item)
                    }
                }
            }
        }
    }

    override fun created() {
        super.created()
        // 初始化数据
        initData()
    }
    
    private fun initData() {
        currentPage = 1
        isLoading = true
        loadError = null
        
        getPager().lifecycleScope.launch {
            try {
                KLog.i("WaterfallHomePage", "开始异步加载瀑布流数据 - 页码: $currentPage")
                val result = WaterfallDataProvider.createWaterfallItems(currentPage, pageSize)
                
                if (result.isSuccess) {
                    val items = result.getOrNull() ?: emptyList()
                    dataList.clear()
                    dataList.addAll(items)
                    KLog.i("WaterfallHomePage", "瀑布流数据加载成功，共 ${items.size} 条数据")
                } else {
                    val error = result.exceptionOrNull()
                    loadError = error?.message ?: "未知错误"
                    KLog.e("WaterfallHomePage", "瀑布流数据加载失败: ${loadError}")
                }
            } catch (e: Exception) {
                loadError = e.message ?: "加载异常"
                KLog.e("WaterfallHomePage", "瀑布流数据加载异常: ${loadError}")
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * 刷新数据
     */
    private fun refreshData() {
        currentPage = 1
        isLoading = true
        loadError = null
        
        getPager().lifecycleScope.launch {
            try {
                KLog.i("WaterfallHomePage", "开始刷新瀑布流数据")
                val result = WaterfallDataProvider.createWaterfallItems(currentPage, pageSize)
                
                if (result.isSuccess) {
                    val items = result.getOrNull() ?: emptyList()
                    dataList.clear()
                    dataList.addAll(items)
                    KLog.i("WaterfallHomePage", "瀑布流数据刷新成功，共 ${items.size} 条数据")
                } else {
                    val error = result.exceptionOrNull()
                    loadError = error?.message ?: "刷新失败"
                    KLog.e("WaterfallHomePage", "瀑布流数据刷新失败: ${loadError}")
                }
            } catch (e: Exception) {
                loadError = e.message ?: "刷新异常"
                KLog.e("WaterfallHomePage", "瀑布流数据刷新异常: ${loadError}")
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * 加载更多数据
     */
    private fun loadMoreData() {
        if (isLoading) return
        
        currentPage++
        isLoading = true
        
        getPager().lifecycleScope.launch {
            try {
                KLog.i("WaterfallHomePage", "开始加载更多瀑布流数据 - 页码: $currentPage")
                val result = WaterfallDataProvider.createWaterfallItems(currentPage, pageSize)
                
                if (result.isSuccess) {
                    val items = result.getOrNull() ?: emptyList()
                    dataList.addAll(items)
                    KLog.i("WaterfallHomePage", "更多瀑布流数据加载成功，新增 ${items.size} 条数据")
                } else {
                    val error = result.exceptionOrNull()
                    KLog.e("WaterfallHomePage", "更多瀑布流数据加载失败: ${error?.message}")
                    currentPage-- // 回退页码
                }
            } catch (e: Exception) {
                KLog.e("WaterfallHomePage", "更多瀑布流数据加载异常: ${e.message}")
                currentPage-- // 回退页码
            } finally {
                isLoading = false
            }
        }
    }
}

internal class WaterfallHomePageAttr : ComposeAttr()

internal class WaterfallHomePageEvent : ComposeEvent() {
    var onTopTabClick: ((Int, String) -> Unit)? = null
    var onCardClick: ((WaterFallItem) -> Unit)? = null
}

internal fun ViewContainer<*, *>.WaterfallHomePage(
    pageViewWidth: Float,
    columnCount: Int = 2,
    init: WaterfallHomePage.() -> Unit
) {
    addChild(WaterfallHomePage(pageViewWidth, columnCount), init)
}