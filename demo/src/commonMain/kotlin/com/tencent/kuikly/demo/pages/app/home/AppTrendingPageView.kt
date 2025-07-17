/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.kuikly.demo.pages.app.home

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.ScrollParams
import com.tencent.kuikly.core.views.TabItem
import com.tencent.kuikly.core.views.Tabs
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.app.AppTabPage
import com.tencent.kuikly.demo.pages.app.model.AppFeedsType
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

internal class AppTrendingPageView: ComposeView<AppTrendingPageViewAttr, AppTrendingPageViewEvent>() {

    private var curIndex: Int by observable(0)
    private var scrollParams: ScrollParams? by observable(null)
    private var pageListRef : ViewRef<PageListView<*, *>>? = null
    private val pageTitles = listOf<String>("推荐", "附近", "榜单", "明星", "搞笑", "社会","测试")
    private val pageTypes = listOf<AppFeedsType>(
        AppFeedsType.Recommend,
        AppFeedsType.Nearby,
        AppFeedsType.Top,
        AppFeedsType.Star,
        AppFeedsType.Laugh,
        AppFeedsType.Society,
        AppFeedsType.Test,
    )
    private var viewRefs: MutableList<ViewRef<AppFeedListPageView>> = mutableListOf()
    private var colorScheme by observable(ThemeManager.colorScheme)
    private lateinit var eventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        eventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                colorScheme = ThemeManager.colorScheme
            }
    }

    override fun viewDestroyed() {
        super.viewDestroyed()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, eventCallbackRef)
    }

    internal fun loadFirstFeeds() {
        this.viewRefs.first().view?.loadFirstFeeds()
    }

    override fun createEvent(): AppTrendingPageViewEvent {
        return AppTrendingPageViewEvent()
    }

    override fun createAttr(): AppTrendingPageViewAttr {
        return AppTrendingPageViewAttr()
    }

    fun tabsHeader(): ViewBuilder {
        val ctx = this
        return {
            Tabs {
                attr {
                    height(TAB_HEADER_HEIGHT)
                    defaultInitIndex(ctx.curIndex)
                    backgroundColor(ctx.colorScheme.topBarNestedBackground)
                    ctx.scrollParams?.also {
                        scrollParams(it)
                    }
                    indicatorInTabItem {
                        View {}
                    }
                }
                for (i in 0 until ctx.pageTitles.size) {
                    TabItem { state ->
                        attr {
                            marginLeft(18f)
                            marginRight(18f)
                            allCenter()
                        }
                        event {
                            click {
                                ctx.pageListRef?.view?.scrollToPageIndex(i, true)
                            }
                        }
                        Text {
                            attr {
                                text(ctx.pageTitles[i])
                                fontSize(17f)
                                color(if (state.selected) ctx.colorScheme.topBarNestedTextFocused
                                      else ctx.colorScheme.topBarNestedTextUnfocused)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            ctx.tabsHeader().invoke(this)
            PageList {
                ref {
                    ctx.pageListRef = it
                }
                attr {
                    flexDirectionRow()
                    pageItemWidth(pagerData.pageViewWidth)
                    pageItemHeight(pagerData.pageViewHeight - pagerData.statusBarHeight - AppHomePageView.TAB_HEADER_HEIGHT - AppTabPage.TAB_BOTTOM_HEIGHT - TAB_HEADER_HEIGHT)
                    defaultPageIndex(ctx.curIndex)
                    showScrollerIndicator(false)
                }
                event {
                    scroll {
                        ctx.scrollParams = it
                    }
                    pageIndexDidChanged {
                        val index = (it as JSONObject).optInt("index")
                        ctx.viewRefs[index].view?.loadFirstFeeds()
                    }
                }
                for (i in 0 until ctx.pageTypes.size) {
                    val type = ctx.pageTypes[i]
                    AppFeedListPage(type) {
                        ref {
                            if (ctx.viewRefs.size == i) {
                                ctx.viewRefs.add(it)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TAB_HEADER_HEIGHT = 40f
    }
}

internal class AppTrendingPageViewAttr : ComposeAttr() {

}

internal class AppTrendingPageViewEvent : ComposeEvent() {
    
}

internal fun ViewContainer<*, *>.AppTrendingPage(init: AppTrendingPageView.() -> Unit) {
    addChild(AppTrendingPageView(), init)
}