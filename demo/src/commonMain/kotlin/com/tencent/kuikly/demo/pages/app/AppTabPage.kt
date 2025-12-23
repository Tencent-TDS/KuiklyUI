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

package com.tencent.kuikly.demo.pages.app

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.demo.base.TabbarIOS
import com.tencent.kuikly.demo.pages.demo.base.TabbarIOSItem
import com.tencent.kuikly.demo.pages.app.home.AppHomePage
import com.tencent.kuikly.demo.pages.app.lang.MultiLingualPager
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

@Page("AppTabPage")
internal class AppTabPage : MultiLingualPager() {

    private var selectedTabIndex: Int by observable(0)
    private var pageTitles by observableList<String>()
    private val pageIcons = listOf<String>(
        "tabbar_home.png",
        "tabbar_video.png",
        "tabbar_discover.png",
        "tabbar_message_center.png",
        "tabbar_profile.png"
    )
    private val pageIconsHighlight = listOf<String>(
        "tabbar_home_highlighted.png",
        "tabbar_video_highlighted.png",
        "tabbar_discover_highlighted.png",
        "tabbar_message_center_highlighted.png",
        "tabbar_profile_highlighted.png"
    )
    private var pageListRef : ViewRef<PageListView<*, *>>? = null
    private var theme by observable(ThemeManager.getTheme())
    private lateinit var themeEventCallbackRef: CallbackRef

    override fun langEventCallbackFn() {
        super.langEventCallbackFn()
        pageTitles[0] = resStrings.btmBarHome
        pageTitles[1] = resStrings.btmBarVideo
        pageTitles[2] = resStrings.btmBarDiscover
        pageTitles[3] = resStrings.btmBarMessage
        pageTitles[4] = resStrings.btmBarMe
    }

    override fun created() {
        super.created()
        pageTitles.addAll(listOf(
            resStrings.btmBarHome,
            resStrings.btmBarVideo,
            resStrings.btmBarDiscover,
            resStrings.btmBarMessage,
            resStrings.btmBarMe
        ))
        val spModule = acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

        // 获取当前设置的主题
        val colorTheme = spModule.getString(ThemeManager.PREF_KEY_COLOR)
            .takeUnless { it.isEmpty() } ?: "light"
        val assetTheme = spModule.getString(ThemeManager.PREF_KEY_ASSET)
            .takeUnless { it.isEmpty() } ?: "default"
        val typoTheme = spModule.getString(ThemeManager.PREF_KEY_TYPO)
            .takeUnless { it.isEmpty() } ?: "default"

        // 切换主题并更新当前页面的样式
        ThemeManager.changeColorScheme(colorTheme)
        ThemeManager.changeAssetScheme(assetTheme)
        ThemeManager.changeTypoScheme(typoTheme)

        theme = ThemeManager.getTheme()

        // 注册换肤相关的事件监听
        themeEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                theme = ThemeManager.getTheme()
            }
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, themeEventCallbackRef)
    }

    private fun tabBar(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    height(TAB_BOTTOM_HEIGHT)
                    flexDirectionRow()
                    turboDisplayAutoUpdateEnable(false)
                    backgroundColor(ctx.theme.colors.tabBarBackground)
                }
                for (i in 0 until ctx.pageTitles.size) {
                    View {
                        attr {
                            flex(1f)
                            allCenter()
                        }
                        event {
                            click {
                                ctx.selectedTabIndex = i
                                ctx.pageListRef?.view?.scrollToPageIndex(i)
                            }
                        }
                        Image {
                            attr {
                                size(30f, 30f)
                                if (i == ctx.selectedTabIndex) {
                                    src(ThemeManager.getAssetUri(ctx.theme.asset, ctx.pageIconsHighlight[i]))
                                    tintColor(ctx.theme.colors.tabBarIconFocused)
                                } else {
                                    src(ThemeManager.getAssetUri(ctx.theme.asset, ctx.pageIcons[i]))
                                    tintColor(ctx.theme.colors.tabBarIconUnfocused)
                                }
                            }
                        }
                        Text {
                            attr {
                                text(ctx.pageTitles[i])
                                color(if (i == ctx.selectedTabIndex) ctx.theme.colors.tabBarTextFocused
                                else ctx.theme.colors.tabBarTextUnfocused)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun tabBarIOS(): ViewBuilder {
        val ctx = this
        val barItems = mutableListOf<TabbarIOSItem>()
        val pageName = getPager().pageName

        for (i in 0 until ctx.pageTitles.size) {
            barItems.add(
                TabbarIOSItem(
                    pageTitles[i],
                    ThemeManager.getAssetUri(ctx.theme.asset, ctx.pageIcons[i]).toUrl(pageName),
                    ThemeManager.getAssetUri(ctx.theme.asset, ctx.pageIconsHighlight[i]).toUrl(pageName)
                )
            )
        }

        return {
            TabbarIOS {
                attr {
                    height(TAB_BOTTOM_HEIGHT)
                    absolutePosition(bottom = 0f, left = 0f, right = 0f)
                    items(barItems)
                    selectedIndex(ctx.selectedTabIndex)
                }
                event {
                    onTabSelected {
                        // 处理 tab 切换
                        ctx.selectedTabIndex = it.index
                        ctx.pageListRef?.view?.scrollToPageIndex(it.index)
                    }
                }
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val isLiquidGlassSupported = PlatformUtils.isIOS() && PlatformUtils.isLiquidGlassSupported()
        val tabBottomHeight = if (isLiquidGlassSupported) 0f else TAB_BOTTOM_HEIGHT

        return {
            View {
                attr {
                    height(pagerData.statusBarHeight)
                    backgroundColor(ctx.theme.colors.topBarBackground)
                }
            }

            PageList {
                ref {
                    ctx.pageListRef = it
                }
                attr {
                    flexDirectionRow()
                    pageItemWidth(pagerData.pageViewWidth)
                    pageItemHeight(pagerData.pageViewHeight - pagerData.statusBarHeight - tabBottomHeight)
                    defaultPageIndex(0)
                    showScrollerIndicator(false)
                    scrollEnable(false)
                    keepItemAlive(true)
                }
                AppHomePage { }
                for (i in 1 until ctx.pageTitles.size) {
                    AppEmptyPage {
                        attr {
                            title = ctx.pageTitles[i]
                        }
                    }
                }
            }
            if (isLiquidGlassSupported) {
                ctx.tabBarIOS().invoke(this)
            } else {
                ctx.tabBar().invoke(this)
            }
        }
    }

    companion object {
        const val TAB_BOTTOM_HEIGHT = 80f
    }
}