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

package com.tencent.kuikly.demo.pages.app

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.app.home.AppHomePage
import com.tencent.kuikly.demo.pages.app.lang.LangManager
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

@Page("AppTabPage")
internal class AppTabPage : BasePager() {

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
    private var resString by observable(LangManager.getCurrentResString())
    private lateinit var themeEventCallbackRef: CallbackRef
    private lateinit var langEventCallbackRef: CallbackRef

    private val PageTitleList = listOf(
        resString.btmBarHome,
        resString.btmBarVideo,
        resString.btmBarDiscover,
        resString.btmBarMessage,
        resString.btmBarMe
    )

    private fun updateLangData() {
        resString = LangManager.getCurrentResString()
        pageTitles[0] = resString.btmBarHome
        pageTitles[1] = resString.btmBarVideo
        pageTitles[2] = resString.btmBarDiscover
        pageTitles[3] = resString.btmBarMessage
        pageTitles[4] = resString.btmBarMe
    }

    private fun loadPrefData() {
        val spModule = acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

        val lang = spModule.getString(LangManager.KEY_PREF_LANGUAGE)
            .takeUnless { it.isEmpty() } ?: pagerData.params.optString(LangManager.KEY_PARAM_LANGUAGE)
        val colorTheme = spModule.getString(ThemeManager.PREF_KEY_COLOR)
            .takeUnless { it.isEmpty() } ?: "light"
        val assetTheme = spModule.getString(ThemeManager.PREF_KEY_ASSET)
            .takeUnless { it.isEmpty() } ?: "default"
        val typoTheme = spModule.getString(ThemeManager.PREF_KEY_TYPO)
            .takeUnless { it.isEmpty() } ?: "default"

        LangManager.changeLanguage(lang)
        updateLangData()

        ThemeManager.changeColorScheme(colorTheme)
        ThemeManager.changeAssetScheme(assetTheme)
        ThemeManager.changeTypoScheme(typoTheme)
        theme = ThemeManager.getTheme()
    }

    override fun created() {
        super.created()
        pageTitles.addAll(PageTitleList)
        loadPrefData()

        // 注册换肤和语言相关的事件监听
        themeEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                theme = ThemeManager.getTheme()
            }
        langEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(LangManager.LANG_CHANGED_EVENT) { _ ->
                updateLangData()
            }
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, themeEventCallbackRef)
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(LangManager.LANG_CHANGED_EVENT, langEventCallbackRef)
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
                                val path = if (i == ctx.selectedTabIndex) ctx.pageIconsHighlight[i] else ctx.pageIcons[i]
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

    override fun body(): ViewBuilder {
        val ctx = this
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
                    pageItemHeight(pagerData.pageViewHeight - pagerData.statusBarHeight - TAB_BOTTOM_HEIGHT)
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
            ctx.tabBar().invoke(this)
        }
    }

    companion object {
        const val TAB_BOTTOM_HEIGHT = 80f
    }
}