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

package com.tencent.kuikly.demo.pages.demo.TBDemoTest

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.getFirstVisiblePosition
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.TurboDisplayModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * TurboDisplay 测试页面 - PageList 组件
 * 用于验证 PageList 的 offset 和 pageIndex 缓存恢复
 */
@Page("TBPageListTestPage")
internal class TBPageListTestPage : BasePager() {

    companion object {
        private const val TAG = "TBPageListTestPage"
    }

    private var currentPageIndex by observable(0)
    private var tabItems by observableList<TabItemData>()
    private var pageItems by observableList<PageItemData>()
    private var pageListRef: ViewRef<PageListView<*, *>>? = null
    
    // 嵌套 List 的引用（key = pageIndex）
    private var nestedListRefs = mutableMapOf<Int, ViewRef<ListView<*, *>>>()
    // 嵌套 List 的当前状态（key = pageIndex）
    private var nestedListStates = mutableMapOf<Int, NestedListState>()

    // 当前 offset
    private var currentOffsetX by observable(0f)
    private var currentOffsetY by observable(0f)

    // 恢复的数据
    private var restoredOffsetX = 0f
    private var restoredOffsetY = 0f
    private var restoredPageIndex = 0
    // 恢复的嵌套 List 状态
    private var restoredNestedListStates = mutableMapOf<Int, NestedListState>()

    private var extraCacheContent = JSONObject()
    
    // 嵌套 List 状态数据类
    data class NestedListState(
        val nativeRef: Int = 0,  // List 的 nativeRef 作为 tag
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
        val firstVisibleIndex: Int = 0,
        val firstVisibleOffset: Float = 0f
    )

    override fun created() {
        super.created()
        // 初始化数据
        for (i in 0 until 5) {
            pageItems.add(PageItemData(this).apply {
                for (j in 0 until 30) {
                    dataList.add(CardItemData(this@TBPageListTestPage).apply {
                        title = "Page $i - Item $j"
                    })
                }
            })
            tabItems.add(TabItemData(i, "Tab $i"))
        }
        // 恢复缓存
        restoreFromPageData()
        getPager().acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME).clearCurrentPageCache()
    }

    private fun restoreFromPageData() {
        val extraCacheContent = getPager().pageData.customFirstScreenTag
        if (extraCacheContent.isNullOrEmpty()) {
            KLog.i(TAG, "【PageData恢复】无 extraCacheContent")
            return
        }
        KLog.i(TAG, "【PageData恢复】extraCacheContent=$extraCacheContent")
        try {
            this.extraCacheContent = JSONObject(extraCacheContent)
            // 解析 pageIndex
            restoredPageIndex = this.extraCacheContent.optInt("currentPageIndex", 0)
            
            // 解析嵌套 List 状态
            val nestedListsJson = this.extraCacheContent.optJSONObject("nestedLists")
            if (nestedListsJson != null) {
                for (key in nestedListsJson.keys()) {
                    val pageIndex = key.toIntOrNull() ?: continue
                    val stateJson = nestedListsJson.optJSONObject(key) ?: continue
                    restoredNestedListStates[pageIndex] = NestedListState(
                        nativeRef = stateJson.optInt("nativeRef", 0),
                        offsetX = stateJson.optDouble("offsetX", 0.0).toFloat(),
                        offsetY = stateJson.optDouble("offsetY", 0.0).toFloat(),
                        firstVisibleIndex = stateJson.optInt("firstVisibleIndex", 0),
                        firstVisibleOffset = stateJson.optDouble("firstVisibleOffset", 0.0).toFloat()
                    )
                }
                KLog.i(TAG, "【PageData恢复】嵌套List状态: $restoredNestedListStates")
            }
        } catch (e: Exception) {
            KLog.e(TAG, "【PageData恢复】解析失败: ${e.message}")
        }
    }

    private fun buildExtraCacheContent(): String {
        // 构建嵌套 List 状态 JSON（包含 nativeRef 作为 tag）
        val nestedListsBuilder = StringBuilder()
        nestedListStates.entries.forEachIndexed { index, (pageIndex, state) ->
            if (index > 0) nestedListsBuilder.append(",")
            nestedListsBuilder.append(""""$pageIndex":{"nativeRef":${state.nativeRef},"offsetX":${state.offsetX},"offsetY":${state.offsetY},"firstVisibleIndex":${state.firstVisibleIndex},"firstVisibleOffset":${state.firstVisibleOffset}}""")
        }
        
        // 同时构建 nativeRef -> offset 的映射（用于原生层恢复）
        val nativeRefTagsBuilder = StringBuilder()
        nestedListStates.entries.forEachIndexed { index, (_, state) ->
            if (state.nativeRef > 0) {
                if (nativeRefTagsBuilder.isNotEmpty()) nativeRefTagsBuilder.append(",")
                nativeRefTagsBuilder.append(""""${state.nativeRef}":{"viewName":"KRScrollView","contentOffsetX":${state.offsetX},"contentOffsetY":${state.offsetY}}""")
            }
        }
        
        return """{"${pageListRef?.nativeRef}":{"viewName":"KRScrollView","contentOffsetX":$currentOffsetX,"contentOffsetY":$currentOffsetY},$nativeRefTagsBuilder,"currentPageIndex":$currentPageIndex,"nestedLists":{$nestedListsBuilder}}"""
    }
    
    /**
     * 更新指定页面的嵌套 List 状态
     */
    private fun updateNestedListState(pageIndex: Int, offsetX: Float, offsetY: Float) {
        val listRef = nestedListRefs[pageIndex] ?: return
        val nativeRef = listRef.nativeRef
        val (firstVisibleIndex, firstVisibleOffset) = listRef.view?.getFirstVisiblePosition() ?: Pair(0, 0f)
        nestedListStates[pageIndex] = NestedListState(
            nativeRef = nativeRef,
            offsetX = offsetX,
            offsetY = offsetY,
            firstVisibleIndex = firstVisibleIndex,
            firstVisibleOffset = firstVisibleOffset
        )
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }

            NavBar {
                attr {
                    title = "TB PageList 测试"
                }
            }

            // 按钮区域
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceAround()
                    padding(12f)
                }

                Button {
                    attr {
                        size(90f, 40f)
                        backgroundColor(Color(0xFFFF5722))
                        borderRadius(8f)
                        titleAttr {
                            text("刷新缓存")
                            color(Color.WHITE)
                            fontSize(13f)
                        }
                    }
                    event {
                        click {
                            val extraContent = ctx.buildExtraCacheContent()
                            KLog.i(TAG, "【手动刷新缓存】$extraContent")
                            ctx.acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                                .setCurrentUIAsFirstScreenForNextLaunch(extraContent)
                        }
                    }
                }

                Button {
                    attr {
                        size(90f, 40f)
                        backgroundColor(Color(0xFFE91E63))
                        borderRadius(8f)
                        titleAttr {
                            text("清除缓存")
                            color(Color.WHITE)
                            fontSize(13f)
                        }
                    }
                    event {
                        click {
                            ctx.acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                                .clearCurrentPageCache()
                        }
                    }
                }
            }

            // 状态提示
            Text {
                attr {
                    margin(12f)
                    fontSize(13f)
                    color(Color.GRAY)
                    text("当前页: ${ctx.currentPageIndex}, offset: (${ctx.currentOffsetX.toInt()}, ${ctx.currentOffsetY.toInt()})")
                }
            }

            // Tab 区域
            View {
                attr {
                    flexDirectionRow()
                    height(50f)
                    justifyContentSpaceEvenly()
                    backgroundColor(Color(0xFFF5F5F5))
                }
                vfor({ ctx.tabItems }) { tabItem ->
                    View {
                        attr {
                            allCenter()
                            flex(1f)
                        }
                        Text {
                            attr {
                                color(if (tabItem.index == ctx.currentPageIndex) Color(0xFF007AFF) else Color.BLACK)
                                fontSize(15f)
                                fontWeight500()
                                text(tabItem.title)
                            }
                        }
                        event {
                            click {
                                ctx.pageListRef?.view?.setContentOffset(
                                    tabItem.index * getPager().pageData.pageViewWidth,
                                    0f,
                                    true
                                )
                            }
                        }
                    }
                }
            }

            // PageList 区域
            PageList {
                ref {
                    ctx.pageListRef = it
                    val cacheProps = ctx.extraCacheContent.toMap()
                    val pageListPropsValue = cacheProps[this.nativeRef.toString()]
                    if (pageListPropsValue != null && pageListPropsValue.toString() != "null") {
                        try {
                            val props = JSONObject(pageListPropsValue.toString()).toMap()
                            ctx.restoredOffsetX = (props["contentOffsetX"] as? Number)?.toFloat() ?: 0f
                            ctx.restoredOffsetY = (props["contentOffsetY"] as? Number)?.toFloat() ?: 0f
                            KLog.i(TAG, "【PageData恢复】offset: (${ctx.restoredOffsetX}, ${ctx.restoredOffsetY}), pageIndex: ${ctx.restoredPageIndex}")
                        } catch (e: Exception) {
                            KLog.e(TAG, "【PageData恢复】解析失败: ${e.message}")
                        }
                    }
                    ctx.addTaskWhenPagerUpdateLayoutFinish {
                        if (ctx.restoredPageIndex > 0) {
                            it.view?.scrollToPageIndex(ctx.restoredPageIndex, animated = false)
                            ctx.currentPageIndex = ctx.restoredPageIndex
                        }
                    }
                }

                attr {
                    flex(1f)
                    pageDirection(true)
                    pageItemWidth(pagerData.pageViewWidth)
                    showScrollerIndicator(false)
                    keepItemAlive(true)
                }

                event {
                    pageIndexDidChanged {
                        ctx.currentPageIndex = (it as JSONObject).optInt("index")
                        val extraContent = ctx.buildExtraCacheContent()
                        KLog.i(TAG, "【PageIndexChanged自动缓存】$extraContent")
                        getPager().acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                            .setCurrentUIAsFirstScreenForNextLaunch(extraContent)
                    }
                    scroll {
                        ctx.currentOffsetX = it.offsetX
                        ctx.currentOffsetY = it.offsetY
                    }
                }

                vforIndex({ ctx.pageItems }) { pageItem, pageIndex, _ ->
                    List {
                        ref {
                            ctx.nestedListRefs[pageIndex] = it
                            // 恢复嵌套 List 的状态
                            val restoredState = ctx.restoredNestedListStates[pageIndex]
                            if (restoredState != null) {
                                ctx.addTaskWhenPagerUpdateLayoutFinish {
                                    if (restoredState.firstVisibleIndex > 0) {
                                        KLog.i(TAG, "【恢复嵌套List】page=$pageIndex, scrollToPosition: index=${restoredState.firstVisibleIndex}")
                                        it.view?.scrollToPosition(restoredState.firstVisibleIndex, restoredState.firstVisibleOffset)
                                    } else if (restoredState.offsetY > 0) {
                                        KLog.i(TAG, "【恢复嵌套List】page=$pageIndex, setContentOffset: ${restoredState.offsetY}")
                                        it.view?.setContentOffset(restoredState.offsetX, restoredState.offsetY, animated = false)
                                    }
                                }
                            }
                        }
                        
                        event {
                            scrollEnd {
                                // 更新嵌套 List 状态（包含 nativeRef、offset、index）
                                ctx.updateNestedListState(pageIndex, it.offsetX, it.offsetY)
                                
                                // 自动缓存
                                val extraContent = ctx.buildExtraCacheContent()
                                KLog.i(TAG, "【嵌套List ScrollEnd】page=$pageIndex, $extraContent")
                                getPager().acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                                    .setCurrentUIAsFirstScreenForNextLaunch(extraContent)
                            }
                        }
                        
                        vfor({ pageItem.dataList }) { cardData ->
                            View {
                                attr {
                                    height(60f)
                                    allCenter()
                                    backgroundColor(ctx.randomColor())
                                }
                                Text {
                                    attr {
                                        fontSize(15f)
                                        color(Color.WHITE)
                                        text(cardData.title)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun randomColor(): Color {
        return Color((50..200).random(), (50..200).random(), (50..200).random(), 1.0f)
    }

    data class TabItemData(val index: Int, val title: String)

    class PageItemData(scope: PagerScope) {
        var dataList by scope.observableList<CardItemData>()
    }

    class CardItemData(scope: PagerScope) {
        var title by scope.observable("")
    }
}
