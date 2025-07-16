package com.tencent.kuikly.demo.pages.app

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("AppSettingPage")
internal class AppSettingPage : BasePager() {

    private var colorScheme by observable(ThemeManager.colorScheme)
    private var assetScheme by observable(ThemeManager.assetScheme)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // Navigation bar
            View {
                attr {
                    paddingTop(ctx.pagerData.statusBarHeight)
                    backgroundColor(ctx.colorScheme.topBarBackground)
                }
                View {
                    attr {
                        height(44f)
                        allCenter()
                    }

                    Text {
                        attr {
                            text("换肤设置")
                            color(ctx.colorScheme.topBarTextFocused)
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
                        tintColor(ctx.colorScheme.topBarTextFocused)
                    }
                    event {
                        click {
                            getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                        }
                    }
                }
            }

            View {
                attr {
                    flex(1f)
                    backgroundColor(ctx.colorScheme.background)
                    paddingTop(12f)
                    flexDirectionColumn()
                    flexWrapWrap()
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
                                border(Border(1f, BorderStyle.SOLID, ctx.colorScheme.backgroundElement))
                                borderRadius(12f)
                                backgroundColor(theme.primary)
                                allCenter()
                            }
                            vif ({ ctx.colorScheme == theme }) {
                                Text {
                                    attr {
                                        margin(12f)
                                        fontSize(16f)
                                        text("已选中")
                                        color(theme.backgroundElement)
                                    }
                                }
                            }
                            event {
                                click {
                                    if (theme != ThemeManager.colorScheme) {
                                        ThemeManager.changeColorScheme(name)
                                        ctx.colorScheme = ThemeManager.colorScheme
                                        getPager().acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
                                            .postNotify("skinChanged", JSONObject())
                                        getPager().acquireModule<SharedPreferencesModule>(
                                            SharedPreferencesModule.MODULE_NAME)
                                            .setString("colorTheme", name)
                                    }
                                }
                            }
                        }
                    }
                }
                Text {
                    attr {
                        margin(16f)
                        fontSize(20f)
                        text("选择资源主题：" + ctx.assetScheme)
                        color(ctx.colorScheme.backgroundElement)
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
                                    marginTop(12f)
                                    src(ThemeManager.getAssetUri(name, "tabbar_home.png"))
                                    tintColor(ctx.colorScheme.backgroundElement)
                                }
                            }

                            Text {
                                attr {
                                    fontSize(16f)
                                    marginTop(6f)
                                    text(name)
                                    color(ctx.colorScheme.backgroundElement)
                                }
                            }
                            event {
                                click {
                                    if (name != ThemeManager.assetScheme) {
                                        ThemeManager.assetScheme = name
                                        ctx.assetScheme = name
                                        getPager().acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
                                            .postNotify("skinChanged", JSONObject())
                                        getPager().acquireModule<SharedPreferencesModule>(
                                            SharedPreferencesModule.MODULE_NAME)
                                            .setString("assetTheme", name)
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