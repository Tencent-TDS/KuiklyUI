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

package com.tencent.kuikly.demo.pages.app.home

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.FooterRefresh
import com.tencent.kuikly.core.views.FooterRefreshEndState
import com.tencent.kuikly.core.views.FooterRefreshState
import com.tencent.kuikly.core.views.FooterRefreshView
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Refresh
import com.tencent.kuikly.core.views.RefreshView
import com.tencent.kuikly.core.views.RefreshViewState
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.app.feed.AppFeedItem
import com.tencent.kuikly.demo.pages.app.model.AppFeedModel
import com.tencent.kuikly.demo.pages.app.model.AppFeedsManager
import com.tencent.kuikly.demo.pages.app.model.AppFeedsType
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

internal class AppFeedListPageView(
    private val type: AppFeedsType
): ComposeView<AppFeedListPageViewAttr, AppFeedListPageViewEvent>() {

    private var feeds by observableList<AppFeedModel>()
    private lateinit var refreshRef : ViewRef<RefreshView>
    private var refreshText by observable( "下拉刷新")
    private var curPage by observable(0)
    private lateinit var footerRefreshRef : ViewRef<FooterRefreshView>
    private var footerRefreshText by observable( "加载更多")
    private var didLoadFirstFeeds = false
    private var theme by observable(ThemeManager.getTheme())
    private lateinit var eventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        eventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                theme = ThemeManager.getTheme()
            }
    }

    override fun viewDestroyed() {
        super.viewDestroyed()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, eventCallbackRef)
    }

    override fun createEvent(): AppFeedListPageViewEvent {
        return AppFeedListPageViewEvent()
    }

    override fun createAttr(): AppFeedListPageViewAttr {
        return AppFeedListPageViewAttr()
    }
    internal fun loadFirstFeeds() {
        if (didLoadFirstFeeds) {
            return
        }
        didLoadFirstFeeds = true
        requestFeeds(curPage) {}
    }
    private fun requestFeeds(page: Int, complete: () -> Unit) {
        if (page > 9) {
            complete()
            return
        }
        AppFeedsManager.requestFeeds(type, page) { feedList, error ->
            if (error.isEmpty()) {
                if (page == 0) {
                    feeds.clear()
                }
                feeds.addAll(feedList)
            }
            complete()
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flex(1f)
                backgroundColor(ctx.theme.colors.background)
            }
            vif({ ctx.feeds.isEmpty() }) {
                Text {
                    attr {
                        text("loading...")
                        color(ctx.theme.colors.feedContentText)
                    }
                }
            }
            velse {
                List {
                    attr {
                        flex(1f)
                        firstContentLoadMaxIndex(4)
                    }
                    Refresh {
                        ref {
                            ctx.refreshRef = it
                        }
                        attr {
                            // TODO: 旋转动画
                            height(50f)
                            allCenter()
                        }
                        event {
                            refreshStateDidChange {
                                when(it) {
                                    RefreshViewState.REFRESHING -> {
                                        ctx.refreshText = "正在刷新"
                                        ctx.requestFeeds(0) {
                                            ctx.refreshRef.view?.endRefresh()
                                            ctx.refreshText = "刷新成功"
                                            ctx.footerRefreshRef.view?.resetRefreshState()
                                        }
                                    }
                                    RefreshViewState.IDLE -> ctx.refreshText = "下拉刷新"
                                    RefreshViewState.PULLING -> ctx.refreshText = "松手即可刷新"
                                }
                            }
                        }
                        Text {
                            attr {
                                color(ctx.theme.colors.feedContentText)
                                text(ctx.refreshText)
                            }
                        }
                    }
                    vfor({ ctx.feeds }) {
                        AppFeedItem {
                            attr {
                                item = it
                            }
                        }
                    }

                    // footer
                    vif({ ctx.feeds.isNotEmpty() }) {
                        FooterRefresh {
                            ref {
                                ctx.footerRefreshRef = it
                            }
                            attr {
                                preloadDistance(600f)
                                allCenter()
                                height(60f)
                            }
                            event {
                                refreshStateDidChange {
                                    when(it) {
                                        FooterRefreshState.REFRESHING -> {
                                            ctx.footerRefreshText = "加载更多..."
                                            ctx.curPage++
                                            ctx.requestFeeds(ctx.curPage) {
                                                val state = if (ctx.curPage == 9) FooterRefreshEndState.NONE_MORE_DATA else FooterRefreshEndState.SUCCESS
                                                ctx.footerRefreshRef.view?.endRefresh(state)
                                            }
                                        }
                                        FooterRefreshState.IDLE -> ctx.footerRefreshText = "加载更多"
                                        FooterRefreshState.NONE_MORE_DATA -> ctx.footerRefreshText = "无更多数据"
                                        FooterRefreshState.FAILURE -> ctx.footerRefreshText = "点击重试加载更多"
                                        else -> {}
                                    }
                                }
                                click {
                                    // 点击重试
                                    ctx.footerRefreshRef.view?.beginRefresh()
                                }
                            }
                            Text {
                                attr {
                                    color(ctx.theme.colors.feedContentText)
                                    text(ctx.footerRefreshText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class AppFeedListPageViewAttr : ComposeAttr()

internal class AppFeedListPageViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.AppFeedListPage(type: AppFeedsType, init: AppFeedListPageView.() -> Unit) {
    addChild(AppFeedListPageView(type), init)
}