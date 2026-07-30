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

    private var tabItems by observableList<TabItemData>()
    private var pageItems by observableList<PageItemData>()
    private lateinit var pageListRef: ViewRef<PageListView<*, *>>
    private lateinit var listViewRef: ViewRef<ListView<*, *>>
    private var nestedListRefs = mutableMapOf<Int, ViewRef<ListView<*, *>>>()

    // 当前 offset
    private var currentOffsetX by observable(0f)
    private var currentOffsetY by observable(0f)
    private var currentPageIndex by observable(0)

    // 恢复的数据
    private var restoredOffsetX = 0f
    private var restoredOffsetY = 0f
    private var restoredPageIndex = 0

    // 缓存中恢复的 index 对应的 list 的状态
    private var listRestoredOffsetX: Float = 0f
    private var listRestoredOffsetY: Float = 0f
    private var listRestoredFirstVisibleIndex: Int = 0
    private var listRestoredFirstVisibleOffset: Float = 0f

    // 当前 index 对应的 List 的 待缓存属性
    private var listCurrentOffsetX: Float = 0f
    private var listCurrentOffsetY: Float = 0f
    private var listCurrentFirstVisibleIndex: Int = 0
    private var listCurrentFirstVisibleOffset: Float = 0f


    private var extraCacheContent = JSONObject()

    override fun created() {
        super.created()
        // 初始化数据 - 每个list 32个item
        for (i in 0 until 7) {
            pageItems.add(PageItemData(this).apply {
                for (j in 0 until 32) {
                    dataList.add(CardItemData(this@TBPageListTestPage).apply {
                        title = "Page $i - Item $j"
                    })
                }
            })
            tabItems.add(TabItemData(i, "Tab $i"))
        }
        // 恢复缓存
        restoreFromPageData()


        if (extraCacheContent.keySet().size > 0) {
            val keys = extraCacheContent.keySet().toList()
            val cacheProps = extraCacheContent.toMap()

            // 解析pagelist的缓存信息
            val pageListPropsValue = cacheProps.get(keys[0])
            if (pageListPropsValue != null && pageListPropsValue.toString() != "null") {
                try {
                    val props = JSONObject(pageListPropsValue.toString()).toMap()
                    restoredOffsetX = (props["contentOffsetX"] as? Number)?.toFloat() ?: 0f
                    restoredOffsetY = (props["contentOffsetY"] as? Number)?.toFloat() ?: 0f
                    restoredPageIndex = (props["pageIndex"] as? Number)?.toInt() ?: 0
                } catch (e: Exception) { }
            }
            // 属性关联
            currentPageIndex = restoredPageIndex

            // 解析list的缓存信息
            val listPropsValue = cacheProps.get(keys[1])
            if (listPropsValue != null && listPropsValue.toString() != "null") {
                try {
                    val listProps = JSONObject(listPropsValue.toString()).toMap()
                    listRestoredOffsetX = (listProps["contentOffsetX"] as? Number)?.toFloat() ?: 0f
                    listRestoredOffsetY = (listProps["contentOffsetY"] as? Number)?.toFloat() ?: 0f
                    listRestoredFirstVisibleIndex = (listProps["firstVisibleIndex"] as? Number)?.toInt() ?: 0
                    listRestoredFirstVisibleOffset = (listProps["firstVisibleOffset"] as? Number)?.toFloat() ?: 0f
                } catch (e: Exception) { }
            }

            KLog.i(TAG, "【恢复PageList】解析结果: " +
                    "pageIndex=$restoredPageIndex, " +
                    "contentOffsetX=$restoredOffsetX, " +
                    "contentOffsetY=$restoredOffsetY, " +
                    "listFirstVisibleIndex=$listRestoredFirstVisibleIndex, " +
                    "listFirstVisibleOffset=$listRestoredFirstVisibleOffset, " +
                    "listContentOffsetX=$listRestoredOffsetX, " +
                    "listContentOffsetY=$listRestoredOffsetY, "
            )

            // 使用 layout 完成回调，确保 LazyLoopDirectivesView 已准备好
            addTaskWhenPagerUpdateLayoutFinish {
//                pageListRef.view?.setContentOffset(offsetX = restoredOffsetX, offsetY = restoredOffsetY)
//                addNextTickTask {
                listViewRef.view?.setContentOffset(offsetX = listRestoredOffsetX, offsetY = listRestoredOffsetY)
//                }
            }


        }
    }

    private fun restoreFromPageData() {
        val extraCacheContent = getPager().pageData.customFirstScreenTag
        if (extraCacheContent.isNullOrEmpty()) {
            return
        }
        KLog.i(TAG, "【PageData恢复】$extraCacheContent")
        try {
            this.extraCacheContent = JSONObject(extraCacheContent)
        } catch (e: Exception) {
            KLog.e(TAG, "【PageData恢复】解析失败: ${e.message}")
        }
    }

    private fun buildExtraCacheContent(): String {
        val obj = JSONObject().apply {
            put(pageListRef?.nativeRef.toString(), JSONObject().apply {
                put("viewName", pageListRef?.view?.viewName())
                put("contentOffsetX", currentOffsetX)
                put("contentOffsetY", currentOffsetY)
                put("pageIndex", currentPageIndex)
            })
            put(nestedListRefs[currentPageIndex]?.nativeRef.toString(), JSONObject().apply {
                put("viewName", listViewRef?.view?.viewName())
                put("contentOffsetX", listCurrentOffsetX)
                put("contentOffsetY", listCurrentOffsetY)
                put("firstVisibleIndex", listCurrentFirstVisibleIndex)
                put("firstVisibleOffset", listCurrentFirstVisibleOffset)
            })
        }
        KLog.i("【强刷的缓存】",obj.toString())
        return obj.toString()
    }

    /**
     * 更新指定页面的嵌套 List 状态
     */
    private fun updateNestedListState(pageIndex: Int, offsetX: Float, offsetY: Float) {
        val listRef = nestedListRefs[pageIndex] ?: return
        val (firstVisibleIndex, firstVisibleOffset) = listRef.view?.getFirstVisiblePosition()
            ?: Pair(0, 0f)
        this.listCurrentOffsetX = offsetX
        this.listCurrentOffsetY = offsetY
        this.listCurrentFirstVisibleIndex = firstVisibleIndex
        this.listCurrentFirstVisibleOffset = firstVisibleOffset
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
                            KLog.i(TAG, "【手动缓存】$extraContent")
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
                    }
                }
            }

            // PageList 区域
            PageList {
                ref {
                    ctx.pageListRef = it
                }

                attr {
                    flex(1f)
                    pageDirection(true)
                    pageItemWidth(pagerData.pageViewWidth)
                    showScrollerIndicator(false)
                    keepItemAlive(true)
                    defaultPageIndex(ctx.currentPageIndex)
                }

                event {
                    pageIndexDidChanged {
                        ctx.currentPageIndex = (it as JSONObject).optInt("index")
                        val extraContent = ctx.buildExtraCacheContent()
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
                            if (pageIndex == ctx.restoredPageIndex) {
                                KLog.i(TAG, "listView 绑定完成")
                                ctx.listViewRef = it
                            }

                        }

                        event {
                            scrollEnd {
                                // 更新List状态，只有list的事件中需要调用
                                ctx.updateNestedListState(pageIndex, it.offsetX, it.offsetY)
                                val extraContent = ctx.buildExtraCacheContent()
                                getPager().acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
                                    .setCurrentUIAsFirstScreenForNextLaunch(extraContent)
                            }
                        }

                        vforIndex ({ pageItem.dataList }) { cardData, index, count ->
                            View {
                                attr {
                                    height(60f)
                                    allCenter()
                                    backgroundColor(ctx.getColorByPageAndIndex(pageIndex, index))
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

    // 7个Tab对应的颜色体系：红橙黄绿蓝靛紫，每个体系4种颜色
    // 32个item分为4组，每组8个item使用同一种颜色
    private val colorSchemes = listOf(
        // Tab 0: 红色系
        listOf(
            Color(0xFFB71C1C), // 深红
            Color(0xFFC62828), // 红
            Color(0xFFD32F2F), // 亮红
            Color(0xFFE57373)  // 浅红
        ),
        // Tab 1: 橙色系
        listOf(
            Color(0xFFE65100), // 深橙
            Color(0xFFF57C00), // 橙
            Color(0xFFFF9800), // 亮橙
            Color(0xFFFFB74D)  // 浅橙
        ),
        // Tab 2: 黄色系
        listOf(
            Color(0xFFF9A825), // 深黄
            Color(0xFFFBC02D), // 黄
            Color(0xFFFFEB3B), // 亮黄
            Color(0xFFFFF176)  // 浅黄
        ),
        // Tab 3: 绿色系
        listOf(
            Color(0xFF1B5E20), // 深绿
            Color(0xFF2E7D32), // 绿
            Color(0xFF4CAF50), // 亮绿
            Color(0xFF81C784)  // 浅绿
        ),
        // Tab 4: 蓝色系
        listOf(
            Color(0xFF0D47A1), // 深蓝
            Color(0xFF1565C0), // 蓝
            Color(0xFF2196F3), // 亮蓝
            Color(0xFF64B5F6)  // 浅蓝
        ),
        // Tab 5: 靛色系
        listOf(
            Color(0xFF1A237E), // 深靛
            Color(0xFF283593), // 靛
            Color(0xFF3F51B5), // 亮靛
            Color(0xFF7986CB)  // 浅靛
        ),
        // Tab 6: 紫色系
        listOf(
            Color(0xFF4A148C), // 深紫
            Color(0xFF6A1B9A), // 紫
            Color(0xFF9C27B0), // 亮紫
            Color(0xFFBA68C8)  // 浅紫
        )
    )

    /**
     * 根据pageIndex和itemIndex获取颜色
     * 每个list 32个item，每8个使用同一颜色（32/8=4组）
     */
    private fun getColorByPageAndIndex(pageIndex: Int, itemIndex: Int): Color {
        val scheme = colorSchemes[pageIndex % colorSchemes.size]
        val colorGroup = (itemIndex / 8) % 4 // 每8个item一组，共4组
        return scheme[colorGroup]
    }

    data class TabItemData(
        val index: Int,
        val title: String
    )

    class PageItemData(scope: PagerScope) {
        var dataList by scope.observableList<CardItemData>()
    }

    class CardItemData(scope: PagerScope) {
        var title by scope.observable("")
    }
}