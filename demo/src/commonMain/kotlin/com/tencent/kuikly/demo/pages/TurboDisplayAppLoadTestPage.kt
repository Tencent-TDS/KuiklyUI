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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.getFirstVisiblePosition
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.TurboDisplayModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.FooterRefresh
import com.tencent.kuikly.core.views.FooterRefreshEndState
import com.tencent.kuikly.core.views.FooterRefreshState
import com.tencent.kuikly.core.views.FooterRefreshView
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.Refresh
import com.tencent.kuikly.core.views.RefreshView
import com.tencent.kuikly.core.views.RefreshViewState
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.app.feed.AppFeedItem
import com.tencent.kuikly.demo.pages.app.model.AppFeedModel
import com.tencent.kuikly.demo.pages.app.model.AppFeedsManager
import com.tencent.kuikly.demo.pages.app.model.AppFeedsType
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * TurboDisplay 专属测试页面
 *
 * 用途：验证 TurboDisplay AOT 渲染缓存在首次安装（无缓存）和后续打开（有缓存）时的体验差异。
 * 页面结构仿微博：NavBar + 多 Tab（关注/推荐/热搜/附近/榜单）+ Feed 列表。
 *
 * TurboDisplay 缓存策略：
 *   - PageList 缓存当前 pageIndex
 *   - 当前 Tab 的 List 缓存 contentOffset + firstVisibleIndex
 *   - 缓存时机：PageList 翻页、List scrollEnd
 *   - 恢复时机：created() 解析 customFirstScreenTag，
 *               addTaskWhenPagerUpdateLayoutFinish 恢复滚动位置
 */
@Page("TurboDisplayAppLoadTestPage")
internal class TurboDisplayAppLoadTestPage : BasePager() {

    companion object {
        private const val TAG = "TDAppLoadTestPage"
    }

    // Tab 配置：标题 + 对应的 FeedsType
    private val tabs = listOf(
        "关注" to AppFeedsType.Follow,
        "推荐" to AppFeedsType.Recommend,
        "热搜" to AppFeedsType.Star,
        "附近" to AppFeedsType.Nearby,
        "榜单" to AppFeedsType.Top
    )

    private var selectedTab: Int by observable(0)

    // PageList ref
    private var pageListRef: ViewRef<PageListView<*, *>>? = null

    // 每个 Tab 的 List ref（由子 ComposeView 回调注册）
    private val nestedListRefs = mutableMapOf<Int, ViewRef<ListView<*, *>>>()

    // -------- 缓存恢复相关 --------
    private var extraCacheContent = JSONObject()

    // 恢复的 pageIndex
    private var restoredPageIndex = 0

    // 恢复的当前 Tab List 的 offset
    private var listRestoredOffsetX = 0f
    private var listRestoredOffsetY = 0f

    // 当前 Tab List 待缓存的状态
    private var listCurrentOffsetX = 0f
    private var listCurrentOffsetY = 0f
    private var listCurrentFirstVisibleIndex = 0
    private var listCurrentFirstVisibleOffset = 0f

    // -------- 生命周期 --------
    override fun created() {
        super.created()

        // 1. 从 pageData 解析缓存
        restoreFromPageData()

        if (extraCacheContent.keySet().size > 0) {
            val keys = extraCacheContent.keySet().toList()
            val cacheProps = extraCacheContent.toMap()

            // 解析 PageList 缓存（key[0]）
            val pageListPropsValue = cacheProps[keys[0]]
            if (pageListPropsValue != null && pageListPropsValue.toString() != "null") {
                try {
                    val props = JSONObject(pageListPropsValue.toString()).toMap()
                    restoredPageIndex = (props["pageIndex"] as? Number)?.toInt() ?: 0
                } catch (e: Exception) {
                    KLog.e(TAG, "【恢复PageList】解析失败: ${e.message}")
                }
            }
            selectedTab = restoredPageIndex

            // 解析 List 缓存（key[1]，当前 Tab 的 Feed List）
            if (keys.size > 1) {
                val listPropsValue = cacheProps[keys[1]]
                if (listPropsValue != null && listPropsValue.toString() != "null") {
                    try {
                        val listProps = JSONObject(listPropsValue.toString()).toMap()
                        listRestoredOffsetX = (listProps["contentOffsetX"] as? Number)?.toFloat() ?: 0f
                        listRestoredOffsetY = (listProps["contentOffsetY"] as? Number)?.toFloat() ?: 0f
                    } catch (e: Exception) {
                        KLog.e(TAG, "【恢复List】解析失败: ${e.message}")
                    }
                }
            }

            KLog.i(TAG, "【恢复】pageIndex=$restoredPageIndex, listOffsetY=$listRestoredOffsetY")

            // 2. 布局完成后恢复 List 滚动位置
            addTaskWhenPagerUpdateLayoutFinish {
                nestedListRefs[restoredPageIndex]?.view
                    ?.setContentOffset(offsetX = listRestoredOffsetX, offsetY = listRestoredOffsetY)
            }
        }
    }

    private fun restoreFromPageData() {
        val raw = getPager().pageData.customFirstScreenTag
        if (raw.isNullOrEmpty()) return
        KLog.i(TAG, "【PageData恢复】$raw")
        try {
            extraCacheContent = JSONObject(raw)
        } catch (e: Exception) {
            KLog.e(TAG, "【PageData恢复】解析失败: ${e.message}")
        }
    }

    // -------- 缓存构建 --------

    /** 更新当前 Tab 的 List 状态（scrollEnd 时调用） */
    internal fun updateCurrentListState(tabIndex: Int, offsetX: Float, offsetY: Float) {
        val listRef = nestedListRefs[tabIndex] ?: return
        val (firstIdx, firstOffset) = listRef.view?.getFirstVisiblePosition() ?: Pair(0, 0f)
        listCurrentOffsetX = offsetX
        listCurrentOffsetY = offsetY
        listCurrentFirstVisibleIndex = firstIdx
        listCurrentFirstVisibleOffset = firstOffset
    }

    private fun buildExtraCacheContent(): String {
        val obj = JSONObject().apply {
            // PageList 缓存
            put(pageListRef?.nativeRef.toString(), JSONObject().apply {
                put("viewName", pageListRef?.view?.viewName())
                put("pageIndex", selectedTab)
            })
            // 当前 Tab 的 List 缓存
            val listRef = nestedListRefs[selectedTab]
            put(listRef?.nativeRef.toString(), JSONObject().apply {
                put("viewName", "KRScrollView")
                put("contentOffsetX", listCurrentOffsetX)
                put("contentOffsetY", listCurrentOffsetY)
                put("firstVisibleIndex", listCurrentFirstVisibleIndex)
                put("firstVisibleOffset", listCurrentFirstVisibleOffset)
            })
        }
        KLog.i(TAG, "【缓存】${obj}")
        return obj.toString()
    }

    internal fun saveCache() {
        acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME)
            .setCurrentUIAsFirstScreenForNextLaunch(buildExtraCacheContent())
    }

    // -------- UI --------

    private fun navBar(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    height(pagerData.statusBarHeight)
                    backgroundColor(Color(0xFFFFFFFFL))
                }
            }
            View {
                attr {
                    height(44f)
                    flexDirectionRow()
                    alignItemsCenter()
                    backgroundColor(Color(0xFFFFFFFFL))
                    paddingLeft(14f)
                    paddingRight(14f)
                }
                View {
                    attr {
                        width(30f); height(30f); borderRadius(6f)
                        backgroundColor(Color(0xFFFF8200L))
                        allCenter(); marginRight(10f)
                    }
                    Text {
                        attr { text("微"); fontSize(17f); fontWeightBold(); color(Color.WHITE) }
                    }
                }
                View {
                    attr {
                        flex(1f); height(28f); borderRadius(14f)
                        backgroundColor(Color(0xFFF5F5F5L))
                        flexDirectionRow(); alignItemsCenter(); paddingLeft(12f)
                    }
                    Text {
                        attr { text("🔍  搜索微博、用户、话题"); fontSize(13f); color(Color(0xFFAAAAAAL)) }
                    }
                }
                View {
                    attr {
                        width(30f); height(30f); borderRadius(15f)
                        backgroundColor(Color(0xFFFF8200L))
                        allCenter(); marginLeft(10f)
                    }
                    Text {
                        attr { text("+"); fontSize(22f); fontWeightBold(); color(Color.WHITE) }
                    }
                }
            }
        }
    }

    private fun tabBar(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    height(40f); flexDirectionRow()
                    backgroundColor(Color(0xFFFFFFFFL))
                }
                for (i in ctx.tabs.indices) {
                    View {
                        attr { flex(1f); allCenter() }
                        event {
                            click {
                                ctx.selectedTab = i
                                ctx.pageListRef?.view?.scrollToPageIndex(i, true)
                            }
                        }
                        Text {
                            attr {
                                text(ctx.tabs[i].first); fontSize(14f)
                                if (i == ctx.selectedTab) { color(Color(0xFFFF8200L)); fontWeightBold() }
                                else color(Color(0xFF555555L))
                            }
                        }
                        View {
                            attr {
                                height(3f)
                                absolutePosition(bottom = 0f, left = 6f, right = 6f)
                                borderRadius(1.5f)
                                backgroundColor(
                                    if (i == ctx.selectedTab) Color(0xFFFF8200L) else Color(0x00000000L)
                                )
                            }
                        }
                    }
                }
            }
            View { attr { height(0.5f); backgroundColor(Color(0xFFE0E0E0L)) } }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color(0xFFF0F0F0L)) }

            ctx.navBar().invoke(this)
            ctx.tabBar().invoke(this)

            PageList {
                ref { ctx.pageListRef = it }
                attr {
                    flexDirectionRow()
                    flex(1f)
                    pageItemWidth(pagerData.pageViewWidth)
                    pageItemHeight(
                        pagerData.pageViewHeight
                            - pagerData.statusBarHeight
                            - 44f - 40f - 0.5f
                            - 49f - pagerData.safeAreaInsets.bottom
                    )
                    defaultPageIndex(ctx.restoredPageIndex)
                    showScrollerIndicator(false)
                    keepItemAlive(true)
                }
                event {
                    pageIndexDidChanged {
                        ctx.selectedTab = (it as JSONObject).optInt("index")
                        // Tab 切换时立即刷新缓存
                        ctx.saveCache()
                    }
                }
                for (i in ctx.tabs.indices) {
                    TDFeedListPage(
                        type = ctx.tabs[i].second,
                        tabIndex = i,
                        onListRef = { tabIndex, listRef ->
                            ctx.nestedListRefs[tabIndex] = listRef
                        }
                    ) {}
                }
            }

            // 底部 Tab 栏
            View {
                attr {
                    height(49f + pagerData.safeAreaInsets.bottom)
                    flexDirectionRow()
                    backgroundColor(Color(0xFFFFFFFFL))
                    paddingBottom(pagerData.safeAreaInsets.bottom)
                }
                for (item in listOf("🏠" to "首页", "🔥" to "热点", "📺" to "视频", "💬" to "消息", "👤" to "我")) {
                    View {
                        attr { flex(1f); allCenter() }
                        Text { attr { text(item.first); fontSize(22f); marginBottom(2f) } }
                        Text { attr { text(item.second); fontSize(10f); color(Color(0xFF888888L)) } }
                    }
                }
            }
        }
    }
}

// ============================================================
// TDFeedListPage：单个 Tab 的 Feed 列表，负责注册自己的 List ref
// ============================================================

/**
 * @param type       Feed 类型
 * @param tabIndex   所属 Tab 下标
 * @param onListRef  List ref 创建后的回调，外部 Pager 通过此回调持有 ref 用于缓存恢复
 */
internal class TDFeedListPageView(
    private val type: AppFeedsType,
    private val tabIndex: Int,
    private val onListRef: (tabIndex: Int, ref: ViewRef<ListView<*, *>>) -> Unit
) : ComposeView<TDFeedListPageAttr, TDFeedListPageEvent>() {

    private var feeds by observableList<AppFeedModel>()
    private var curPage by observable(0)
    private var didLoadFirst = false
    private lateinit var listRef: ViewRef<ListView<*, *>>
    private lateinit var refreshRef: ViewRef<RefreshView>
    private lateinit var footerRef: ViewRef<FooterRefreshView>
    private var refreshText by observable("下拉刷新")
    private var footerText by observable("加载更多")

    override fun createAttr() = TDFeedListPageAttr()
    override fun createEvent() = TDFeedListPageEvent()

    override fun viewDidLoad() {
        super.viewDidLoad()
        loadFirst()
    }

    private fun loadFirst() {
        if (didLoadFirst) return
        didLoadFirst = true
        requestFeeds(0) {}
    }

    private fun requestFeeds(page: Int, complete: () -> Unit) {
        AppFeedsManager.requestFeeds(type, page) { list, error ->
            if (error.isEmpty()) {
                if (page == 0) feeds.clear()
                feeds.addAll(list)
            }
            complete()
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { flex(1f); backgroundColor(Color(0xFFF0F0F0L)) }

            vif({ ctx.feeds.isEmpty() }) {
                View {
                    attr { flex(1f); allCenter() }
                    Text { attr { text("加载中..."); fontSize(14f); color(Color(0xFF999999L)) } }
                }
            }
            velse {
                List {
                    ref {
                        ctx.listRef = it
                        // 回调给父 Pager，用于缓存恢复
                        ctx.onListRef(ctx.tabIndex, it)
                    }
                    attr {
                        flex(1f)
                        firstContentLoadMaxIndex(4)
                    }
                    event {
                        scrollEnd {
                            // scrollEnd 时通知父页面更新 List 状态并刷新缓存
                            val pager = getPager() as? TurboDisplayAppLoadTestPage
                            pager?.updateCurrentListState(ctx.tabIndex, it.offsetX, it.offsetY)
                            pager?.saveCache()
                        }
                    }
                    Refresh {
                        ref { ctx.refreshRef = it }
                        attr { height(50f); allCenter() }
                        event {
                            refreshStateDidChange {
                                when (it) {
                                    RefreshViewState.REFRESHING -> {
                                        ctx.refreshText = "刷新中..."
                                        ctx.requestFeeds(0) {
                                            ctx.curPage = 0
                                            ctx.refreshRef.view?.endRefresh()
                                            ctx.refreshText = "刷新完成"
                                            ctx.footerRef.view?.resetRefreshState()
                                        }
                                    }
                                    RefreshViewState.IDLE -> ctx.refreshText = "下拉刷新"
                                    RefreshViewState.PULLING -> ctx.refreshText = "松开刷新"
                                }
                            }
                        }
                        Text { attr { text(ctx.refreshText); fontSize(13f); color(Color(0xFF888888L)) } }
                    }
                    vfor({ ctx.feeds }) { feed ->
                        AppFeedItem { attr { item = feed } }
                    }
                    vif({ ctx.feeds.isNotEmpty() }) {
                        FooterRefresh {
                            ref { ctx.footerRef = it }
                            attr { preloadDistance(600f); allCenter(); height(60f) }
                            event {
                                refreshStateDidChange {
                                    when (it) {
                                        FooterRefreshState.REFRESHING -> {
                                            ctx.footerText = "加载中..."
                                            ctx.curPage++
                                            ctx.requestFeeds(ctx.curPage) {
                                                val state =
                                                    if (ctx.curPage >= 2) FooterRefreshEndState.NONE_MORE_DATA
                                                    else FooterRefreshEndState.SUCCESS
                                                ctx.footerRef.view?.endRefresh(state)
                                            }
                                        }
                                        FooterRefreshState.IDLE -> ctx.footerText = "加载更多"
                                        FooterRefreshState.NONE_MORE_DATA -> ctx.footerText = "没有更多了"
                                        FooterRefreshState.FAILURE -> ctx.footerText = "点击重试"
                                        else -> {}
                                    }
                                }
                                click { ctx.footerRef.view?.beginRefresh() }
                            }
                            Text { attr { text(ctx.footerText); fontSize(13f); color(Color(0xFF888888L)) } }
                        }
                    }
                }
            }
        }
    }
}

internal class TDFeedListPageAttr : ComposeAttr()
internal class TDFeedListPageEvent : ComposeEvent()

internal fun ViewContainer<*, *>.TDFeedListPage(
    type: AppFeedsType,
    tabIndex: Int,
    onListRef: (tabIndex: Int, ref: ViewRef<ListView<*, *>>) -> Unit,
    init: TDFeedListPageView.() -> Unit
) {
    addChild(TDFeedListPageView(type, tabIndex, onListRef), init)
}
