package com.tencent.kuikly.demo.pages.app

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.ActivityIndicator
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TransitionType
import com.tencent.kuikly.core.views.TransitionView
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.app.lang.MultiLingualPager
import com.tencent.kuikly.demo.pages.app.lang.LangManager
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

@Page("AppSettingPage")
internal class AppSettingPage : MultiLingualPager() {

    private var theme by observable(ThemeManager.getTheme())
    private var lang by observable(LangManager.getCurrentLanguage())
    private lateinit var settingLangHint: String
    private var showModal by observable(false)

    private lateinit var spModule: SharedPreferencesModule
    private lateinit var notifyModule: NotifyModule

    override fun created() {
        super.created()
        spModule = acquireModule(SharedPreferencesModule.MODULE_NAME)
        notifyModule = acquireModule(NotifyModule.MODULE_NAME)
    }

    private fun topNavBar(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    paddingTop(ctx.pagerData.statusBarHeight)
                    backgroundColor(ctx.theme.colors.topBarBackground)
                }
                View {
                    attr {
                        height(44f)
                        allCenter()
                    }

                    Text {
                        attr {
                            text(ctx.resStrings.setting)
                            color(ctx.theme.colors.topBarTextFocused)
                            fontSize(17f)
                            fontWeightSemiBold()
                        }
                    }
                }

                Image {
                    attr {
                        absolutePosition(12f + getPager().pageData.statusBarHeight, 12f, 12f, 12f)
                        size(10f, 17f)
                        src("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII=")
                        tintColor(ctx.theme.colors.topBarTextFocused)
                    }
                    event {
                        click {
                            getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                        }
                    }
                }
            }
        }
    }

    private fun skinChooseView(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr {
                    margin(16f)
                    fontSize(20f)
                    text(ctx.resStrings.themeHint)
                    color(ctx.theme.colors.backgroundElement)
                    fontWeightBold()
                }
            }

            View {
                attr {
                    flexDirectionRow()
                }
                for ((name, theme) in ThemeManager.COLOR_SCHEME_MAP) {
                    View {
                        attr {
                            size(100f, 64f)
                            marginLeft(12f)
                            border(Border(1f, BorderStyle.SOLID, ctx.theme.colors.backgroundElement))
                            borderRadius(12f)
                            backgroundColor(theme.primary)
                            allCenter()
                        }
                        vif({ ctx.theme.colors == theme }) {
                            Text {
                                attr {
                                    margin(12f)
                                    fontSize(16f)
                                    text(ctx.resStrings.chosen)
                                    color(theme.topBarTextFocused)
                                }
                            }
                        }
                        event {
                            click {
                                if (theme != ctx.theme.colors) {
                                    // 先修改ThemeManager的配置 再修改页面级配置
                                    ThemeManager.changeColorScheme(name)
                                    ctx.theme = ThemeManager.getTheme()
                                    // 持久化缓存后通知所有页面
                                    ctx.spModule.setString(ThemeManager.PREF_KEY_COLOR, name)
                                    ctx.notifyModule.postNotify(ThemeManager.SKIN_CHANGED_EVENT, JSONObject())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun assetChooseView(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr {
                    margin(16f)
                    fontSize(20f)
                    text(ctx.resStrings.themeHint + ": " + ctx.theme.asset)
                    color(ctx.theme.colors.backgroundElement)
                    fontWeightBold()
                }
            }
            View {
                attr {
                    flexDirectionRow()
                }
                for (name in ThemeManager.ASSET_SCHEME_LIST) {
                    View {
                        attr {
                            marginLeft(16f)
                            allCenter()
                            flexDirectionColumn()
                        }

                        Image {
                            attr {
                                size(40f, 40f)
                                src(ThemeManager.getAssetUri(name, "tabbar_home.png"))
                                tintColor(ctx.theme.colors.backgroundElement)
                            }
                        }

                        Text {
                            attr {
                                fontSize(16f)
                                marginTop(6f)
                                text(name)
                                color(ctx.theme.colors.backgroundElement)
                            }
                        }
                        event {
                            click {
                                if (name != ctx.theme.asset) {
                                    // 先修改ThemeManager的配置 再修改页面级配置
                                    ThemeManager.changeAssetScheme(name)
                                    ctx.theme = ThemeManager.getTheme()
                                    // 持久化缓存后通知所有页面
                                    ctx.spModule.setString(ThemeManager.PREF_KEY_ASSET, name)
                                    ctx.notifyModule.postNotify(ThemeManager.SKIN_CHANGED_EVENT, JSONObject())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun langChooseView(): ViewBuilder {
        val ctx = this
        return {

            Text {
                attr {
                    margin(16f)
                    fontSize(20f)
                    text(ctx.resStrings.themeHint + ": " + ctx.theme.asset)
                    color(ctx.theme.colors.backgroundElement)
                    fontWeightBold()
                }
            }

            List {
                attr {
                    flex(1f)
                    flexDirectionColumn()
                    bouncesEnable(false)
                    scrollEnable(false)
                    justifyContentFlexStart()
                    alignItemsCenter()
                }
                for ((k, v) in LangManager.SUPPORTED_LANGUAGES) {
                    View {
                        attr {
                            height(44f)
                            backgroundColor(ctx.theme.colors.feedBackground)
                            flexDirectionRow()
                            alignItemsCenter()
                        }
                        Text {
                            attr {
                                marginLeft(12f)
                                fontSize(16f)
                                color(ctx.theme.colors.feedContentText)
                                text(k)
                            }
                        }
                        vif ({ ctx.lang == v }) {
                            Image {
                                attr {
                                    absolutePosition(right = 12f, top = 10f, bottom = 10f)
                                    src(ImageUri.commonAssets("ic_chosen.png"))
                                    tintColor(Color.GREEN)
                                    size(24f, 24f)
                                }
                            }
                        }
                        event {
                            click {
                                if (LangManager.getCurrentLanguage() != v) {
                                    LangManager.changeLanguage(v)
                                    ctx.settingLangHint = LangManager.SETTING_HINTS[v] ?: "Setting..."
                                    ctx.showModal = true
                                    setTimeout(500) {
                                        ctx.lang = LangManager.getCurrentLanguage()
                                        // 持久化缓存后通知所有页面
                                        ctx.spModule.setString(LangManager.KEY_PREF_LANGUAGE, v)
                                        ctx.notifyModule.postNotify(LangManager.LANG_CHANGED_EVENT, JSONObject())
                                        ctx.showModal = false
                                    }
                                }
                            }
                        }
                    }
                    View {
                        attr {
                            margin(left = 12.0f)
                            height(0.5f)
                            backgroundColor(ctx.theme.colors.feedContentDivider)
                        }
                    }

                }
            }
        }
    }

    private fun modalView(): ViewBuilder {
        val ctx = this
        return {
            Modal(true) {
                attr {
                    allCenter()
                }
                TransitionView(type = TransitionType.FADE_IN_OUT) {
                    attr {
                        absolutePositionAllZero()
                        backgroundColor(Color(0x88000000))
                    }
                }
                TransitionView(type = TransitionType.CUSTOM) {
                    attr {
                        padding(top = 30f, bottom = 20f)
                        transitionAppear(ctx.showModal)
                        size(160f, 120f)
                        borderRadius(16f)
                        backgroundColor(0xFF404040)
                        flexDirectionColumn()
                        justifyContentFlexStart()
                        alignItemsCenter()
                        customBeginAnimationAttr {
                            opacity(0f)
                            transform(scale = Scale(0.7f, 0.7f))
                        }
                        customEndAnimationAttr {
                            opacity(1f)
                            transform(scale = Scale(1f, 1f))
                        }
                        customAnimation(Animation.springEaseInOut(0.3f, 0.8f, 0.9f))
                    }
                    ActivityIndicator {
                        attr {
                            zIndex(0)
                            isGrayStyle(false)
                            transform(Scale(1.5f, 1.5f))
                        }
                    }
                    Text {
                        attr {
                            marginTop(20f)
                            text(ctx.settingLangHint)
                            fontSize(15f)
                            color(Color.WHITE)
                        }
                    }
                }
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 顶部导航栏
            ctx.topNavBar().invoke(this)

            View {
                attr {
                    flex(1f)
                    backgroundColor(ctx.theme.colors.background)
                    paddingTop(12f)
                    flexDirectionColumn()
                }
                ctx.skinChooseView().invoke(this)
                ctx.assetChooseView().invoke(this)
                ctx.langChooseView().invoke(this)
            }

            vif({ ctx.showModal }) {
                ctx.modalView().invoke(this)
            }
        }
    }
}