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
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

internal data class ImageInfo(val url: String, val width: Float, val height:Float )

internal class DBSubTabItemData {
    var tabTitle by observable("")
    var contentImages by observableList<ImageInfo>()
    var index by observable(0)
}

/*
 * 详情页2层楼交互Demo
 */
@Page("DBExamplePage")
internal class DBExamplePage : BasePager() {
    private var topListContentHeight by observable(0f)
    private var bottomListContentHeight by observable(0f)
    private var topListTranslateY by observable(0f)
    private var bottomListTranslateY by observable(0f)
    private lateinit var topListRef : ViewRef<ListView<*, *>>
    private lateinit var bottomListRef : ViewRef<ListView<*, *>>
    private var lastMainListContentOffsetY = 0f
    private var currentPageIndex = 0
    private val bottomPageListContentHeightMap = hashMapOf<Int, Float>()
    private val bottomPageListRefMap = hashMapOf<Int, ViewRef<ListView<*, *>>>()
    private var mainListHeight : Float = 0f
    private var bottomListMinVisibleHeight = 100f
    private var tabBarHeight = 50f
    private var ignoreMainListScroll = false
    private lateinit var mainListViewRef : ViewRef<ListView<*, *>>
    private var tabDataList by observableList<DBSubTabItemData>()
    private var pageListScrollParams : ScrollParams? by observable(null)
    private var pageListRef : ViewRef<PageListView<*, *>>? = null

    override fun created() {
        super.created()
        val tabTitles = arrayListOf("综合", "影评", "讨论")
        val images = arrayListOf(ImageInfo("", width = 1271f, height = 1755f),
            ImageInfo("", width = 1275f, height = 2106f),
            ImageInfo("", width = 1271f, height = 2219f)
        )
        for (i in 0..2) {
            val tabItemData = DBSubTabItemData().apply {
                tabTitle = tabTitles[i]
                for (j in 0 .. 2) {
                    contentImages.add(images[i])
                }
                index = i
            }
            tabDataList.add(tabItemData)
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val navigationBarHeight = getPager().pageData.navigationBarHeight
        val mainListHeight = getPager().pageData.pageViewHeight - navigationBarHeight
        this.mainListHeight = mainListHeight
        val bottomListMinVisibleHeight = this.bottomListMinVisibleHeight // 底部list最小可见高度
        val tabBarHeight = this.tabBarHeight

        return {
            attr {
                backgroundColor(Color(0xFF4a4330))
            }
            NavBar {
                attr {
                    title = "电影"
                    zIndex(1) // 视觉层级调高
                }
            }
            // mainList
            List {
                ref {
                    ctx.mainListViewRef = it
                }
                attr {
                    height(mainListHeight) // 高度
                    overflow(false) // 内容可以露出自身rect
                    bouncesEnable(false)
                }
                event {
                    scroll {
                        if (ctx.ignoreMainListScroll) {
                            ctx.ignoreMainListScroll = false
                            ctx.lastMainListContentOffsetY = it.offsetY
                            return@scroll
                        }
                        val curOffsetY = it.offsetY
                        val translationY = curOffsetY - ctx.lastMainListContentOffsetY
                        if (translationY > 0) { // 向上滚动
                            val topListMaxOffset = ctx.topListContentHeight - (ctx.topListRef.view?.flexNode?.layoutFrame?.height ?: 0f)
                            val topListNewOffsetY = (ctx.topListRef.view?.curOffsetY ?: 0f) + translationY
                            ctx.topListRef.view?.setContentOffset(it.offsetX, min(topListNewOffsetY, topListMaxOffset) )
                            var remainOffsetY = topListNewOffsetY - topListMaxOffset
                            if (remainOffsetY > 0) { // 有剩余偏移量未消耗
                                // 先看bottomList 位置是否到顶部
                                val maxBottomListTop = (mainListHeight - bottomListMinVisibleHeight)
                                val curBottomListTop = maxBottomListTop + (ctx.bottomListTranslateY * mainListHeight)
                                val newBottomListTop = curBottomListTop - remainOffsetY
                                ctx.bottomListTranslateY =  -(maxBottomListTop - max(0f, newBottomListTop)) / mainListHeight
                                ctx.topListTranslateY = -(maxBottomListTop - max(0f, newBottomListTop)) / maxBottomListTop
                                if (newBottomListTop < 0) { // 有剩余偏移量未消耗
                                    remainOffsetY = -newBottomListTop
                                    val newBottomListOffsetY = (ctx.bottomListRef.view?.curOffsetY ?: 0f) + remainOffsetY
                                    ctx.bottomListRef.view?.setContentOffset(0f, newBottomListOffsetY)
                                }
                            }
                        } else if (translationY < 0) {
                            val bottomListMinOffset = 0f
                            val bottomListNewOffsetY = (ctx.bottomListRef.view?.curOffsetY ?: 0f) + translationY
                            ctx.bottomListRef.view?.setContentOffset(it.offsetX, max(bottomListNewOffsetY, bottomListMinOffset) )
                            var remainOffsetY =  bottomListMinOffset - bottomListNewOffsetY
                            if (remainOffsetY > 0) { // 有剩余偏移量未消耗
                                // 先看bottomList 位置是否到顶部
                                val maxBottomListTop = (mainListHeight - bottomListMinVisibleHeight)
                                val curBottomListTop = maxBottomListTop + (ctx.bottomListTranslateY * mainListHeight)
                                val newBottomListTop = curBottomListTop + remainOffsetY
                                ctx.bottomListTranslateY =  -(maxBottomListTop - min(maxBottomListTop, newBottomListTop)) / mainListHeight
                                ctx.topListTranslateY =  -(maxBottomListTop - min(maxBottomListTop, newBottomListTop)) / maxBottomListTop
                                if (newBottomListTop - maxBottomListTop > 0) { // 有剩余偏移量未消耗
                                    remainOffsetY = newBottomListTop - maxBottomListTop
                                    var newTopListOffsetY = (ctx.topListRef.view?.curOffsetY ?: 0f) - remainOffsetY
                                    if (it.offsetY == 0f) {
                                        newTopListOffsetY = 0f
                                        ctx.topListTranslateY = 0f
                                        ctx.bottomListTranslateY = 0f
                                        ctx.bottomListRef.view?.setContentOffset(0f,0f)

                                    }
                                    ctx.topListRef.view?.setContentOffset(0f, newTopListOffsetY)
                                }
                            }
                        }
                        ctx.lastMainListContentOffsetY = it.offsetY
                    }
                }
                // 填充空白内容，用于可滚动
                View {
                    attr {
                        height(ctx.mainListContentHeight()) // 填充列表内容高度
                    }
                }
                // 内容多列表
                Hover {// hover为置顶组件
                    attr {
                        absolutePosition(top = 0f, left = 0f, right = 0f) // 用绝对布局不占用内容高度
                        overflow(false)
                    }
                    // topList
                    List {
                        ref {
                            ctx.topListRef = it
                        }
                        attr {
                            scrollEnable(false)
                            showScrollerIndicator(false)
                            height(mainListHeight - bottomListMinVisibleHeight)
                            transform(translate = Translate(0f, ctx.topListTranslateY))
                            overflow(false)
                        }
                        event {
                            contentSizeChanged { width, height ->
                                ctx.topListContentHeight = height
                            }
                        }
                        // 模拟正文
                        Image {
                            attr {
                                size(pagerData.pageViewWidth, pagerData.pageViewWidth * (2247f / 1282f))
                                backgroundColor(ctx.generateRandomColor())
                            }
                        }
                        Image {
                            attr {
                                size(pagerData.pageViewWidth, pagerData.pageViewWidth * (2235f / 1281f)) //1281 × 2235
                                backgroundColor(ctx.generateRandomColor())
                            }
                        }
                        Image {
                            attr {
                                size(pagerData.pageViewWidth, pagerData.pageViewWidth * (1228f / 1279f)) //1279 × 1228
                                backgroundColor(ctx.generateRandomColor())
                            }
                        }
                    }
                    // bottomList
                    View {
                        attr {
                            height(mainListHeight)
                            borderRadius(8f)
                            backgroundColor(Color.WHITE)
                            transform(translate = Translate(0f, ctx.bottomListTranslateY))
                        }
                        // 创建tabBar(综合、评论、讨论）
                        apply(ctx.createTabBarView())
                        // BottomList
                        PageList {
                            ref {
                                ctx.pageListRef = it
                            }
                            attr {
                                flex(1f)
                                flexDirectionRow()
                                showScrollerIndicator(false)
                                pageItemWidth(pagerData.pageViewWidth)
                                pageItemHeight(mainListHeight - tabBarHeight)

                            }
                            event {
                                pageIndexDidChanged {
                                    ctx.currentPageIndex = (it as JSONObject).optInt("index")
                                    ctx.updateCurrentBottomListInfo(true)
                                }
                                scroll {
                                    ctx.pageListScrollParams = it
                                }
                            }
                            vfor({ctx.tabDataList}) { tabItemData ->
                                List {
                                    ref {
                                        ctx.bottomPageListRefMap[tabItemData.index] = it
                                    }
                                    attr {
                                        showScrollerIndicator(false)
                                        scrollEnable(false)
                                    }
                                    event {
                                        contentSizeChanged { width, height ->
                                            ctx.bottomPageListContentHeightMap[tabItemData.index] = height
                                            ctx.updateCurrentBottomListInfo()
                                        }
                                    }
                                    vfor({tabItemData.contentImages}) { imageInfo ->
                                        Image {
                                            attr {
                                                backgroundColor(ctx.generateRandomColor())
                                                size(pagerData.pageViewWidth, pagerData.pageViewWidth * (imageInfo.height / imageInfo.width))
                                            }
                                        }
                                    }
                                    // footer
                                    View {
                                        attr {
                                            allCenter()
                                            height(50f)
                                        }
                                        Text {
                                            attr {
                                                text("没有更多了")
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
    }
    private fun generateRandomColor(): Color {
        val r = Random.nextInt(256)
        val g = Random.nextInt(256)
        val b = Random.nextInt(256)
        return Color(r, g, b, 1f)
    }

    // 更新BottomList信息
    private fun updateCurrentBottomListInfo(fromIndexDidChanged: Boolean = false) {
        if (bottomPageListContentHeightMap.containsKey(this.currentPageIndex)) {
            val currentIndexContentHeight = bottomPageListContentHeightMap[this.currentPageIndex] as Float
            this.bottomListContentHeight = currentIndexContentHeight
            this.bottomListRef = this.bottomPageListRefMap[this.currentPageIndex] as ViewRef<ListView<*, *>>

            if (fromIndexDidChanged) {
                // 更新mainList contentOffset
                val curBottomListOffset = (bottomListRef.view?.curOffsetY ?: 0f)
                val maxBottomListTop = (mainListHeight - bottomListMinVisibleHeight)
                val topListHeight =  (mainListHeight - bottomListMinVisibleHeight)
                val curBottomListTop = maxBottomListTop + (this.bottomListTranslateY * mainListHeight)
                val bottomListHeight = this.bottomListRef.view?.flexNode?.layoutFrame?.height ?: 0f
                val maxBottomListOffset = this.bottomListContentHeight - bottomListHeight
                val topListOffset = (this.topListContentHeight - topListHeight) - (this.topListRef.view?.curOffsetY ?: 0f)
                val maxScrollOffset = curBottomListTop + (maxBottomListOffset - curBottomListOffset) + topListOffset
                val mainListContentHeight = this.mainListContentHeight()
                val offset = mainListContentHeight - mainListHeight - maxScrollOffset
                if (offset != mainListViewRef.view?.curOffsetY) {
                    ignoreMainListScroll = true
                    mainListViewRef.view?.setContentOffset(0f, offset)
                }
            }
        }
    }

    private fun mainListContentHeight() : Float {
        return this.topListContentHeight + this.bottomListContentHeight + this.tabBarHeight

    }

    private fun createTabBarView() : ViewBuilder {
        val ctx = this
        return {
            Tabs {
                attr {
                    height(ctx.tabBarHeight) // 横向布局，务必指定高度
                    indicatorInTabItem {
                        View {
                            attr {
                                absolutePosition(left = 5f, right = 5f, bottom = 5f)
                                height(3f)
                                borderRadius(2f)
                                backgroundColor(Color.BLACK)
                            }
                        }
                    }
                    ctx.pageListScrollParams?.also {
                        scrollParams(it)
                    }
                }

                vfor({ctx.tabDataList}) { tabItem ->
                    TabItem { state ->
                        attr {
                            marginLeft(20f)
                            marginRight(20f)
                            allCenter()
                        }
                        event {
                            click {
                                ctx.pageListRef?.view?.scrollToPageIndex(tabItem.index, true)
                             // //  ctx.pageListRef?.view?.scrollToPageIndex(tabItem.index, true)
                            }
                        }
                        Text {
                            attr {
                                text(tabItem.tabTitle)
                                fontSize(16f)
                                if (state.selected) {
                                    color(Color.BLACK)
                                } else {
                                    color(Color.GRAY)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}