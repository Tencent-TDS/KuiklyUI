package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseNavigationSelfDrawnOhos
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 「自制标题栏」能力验证页（12019）—— 与 12018（HdsNavigation 版）对照。
 *
 * 组件用**原生 Navigation** 的官方能力（非 HdsNavigation）：
 * - NavigationTitleOptions.systemMaterial：标题栏系统材质（官方示例20 同款路径）；
 * - NavigationTitleOptions.scrollEffectOptions：滚动渐变模糊（API 26）。
 *
 * 控制面板与 12018 完全一致，两页来回切换即可对比：
 * 自制路线能达到 HdsNavigation 的哪些能力、差在哪（无自定义菜单等）。
 */
@Page("12019")
internal class ImmersiveLightSenseNavigationSelfDrawnDemoPage : BasePager() {

    /** 0=Free 1=Full 2=Mini */
    private var titleMode by observable(2)

    /** -1=关 0=COMMON_BLUR 1=GRADIENT_BLUR */
    private var scrollEffect by observable(1)

    private var materialEnable by observable(true)

    private var hideBack by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this
        val statusBar = pagerData.statusBarHeight
        return {
            // ── 自制标题栏组件（声明在最前 = z 最底层，全屏）──
            ImmersiveLightSenseNavigationSelfDrawnOhos {
                attr {
                    absolutePosition(left = 0f, top = 0f, right = 0f, bottom = 0f)
                    title("自制标题栏 演示")
                    titleMode(ctx.titleMode)
                    enableScrollEffect(ctx.scrollEffect >= 0)
                    scrollEffectType(if (ctx.scrollEffect < 0) 1 else ctx.scrollEffect)
                    hideBackButton(ctx.hideBack)
                    // 原生 Navigation 内容区默认白底：设透明对齐 HdsNavigation 观感
                    contentBackgroundColor(Color.TRANSPARENT)
                    if (ctx.materialEnable) {
                        immersiveMaterial(style = 2)
                    }
                }
            }

            // ── 控制面板（浮于组件之上，与 12018 同款）──
            View {
                attr {
                    absolutePosition(top = statusBar + 64f, left = 16f, right = 16f)
                    padding(12f)
                    borderRadius(12f)
                    backgroundColor(Color(0x99000000))
                    flexDirectionColumn()
                }

                ctx.row("标题模式") {
                    listOf(
                        0 to "Free",
                        1 to "Full",
                        2 to "Mini"
                    ).forEach { (mode, label) ->
                        ctx.chip(label, ctx.titleMode == mode) { ctx.titleMode = mode }()
                    }
                }

                ctx.row("滚动效果") {
                    listOf(
                        -1 to "关",
                        0 to "COMMON",
                        1 to "GRADIENT"
                    ).forEach { (type, label) ->
                        ctx.chip(label, ctx.scrollEffect == type) { ctx.scrollEffect = type }()
                    }
                }

                ctx.row("标题栏材质") {
                    ctx.chip(if (ctx.materialEnable) "开" else "关", ctx.materialEnable) {
                        ctx.materialEnable = !ctx.materialEnable
                    }()
                }

                ctx.row("返回按钮") {
                    ctx.chip(if (ctx.hideBack) "隐藏" else "显示", ctx.hideBack) {
                        ctx.hideBack = !ctx.hideBack
                    }()
                }

                Text {
                    attr {
                        margin(top = 8f)
                        text("对照 12018（HdsNavigation 版）：同样上下滚动图片，观察标题栏滚动渐变是否同样生效；返回键背板是否有玻璃质感。差集：本组件无右上自定义菜单（原生 Navigation 标题栏能力子集）。")
                        fontSize(11f)
                        color(Color(0x99EEEEEE))
                    }
                }
            }
        }
    }

    /** 一行控制项：label + chips */
    private fun row(label: String, chips: ViewBuilder): ViewBuilder {
        return {
            View {
                attr {
                    margin(top = 6f)
                    flexDirectionRow()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        text("$label：")
                        fontSize(13f)
                        color(Color.WHITE)
                    }
                }
                chips()
            }
        }
    }

    /** 可选中 chip 按钮 */
    private fun chip(label: String, selected: Boolean, onClick: () -> Unit): ViewBuilder {
        return {
            View {
                attr {
                    margin(left = 6f)
                    padding(left = 10f, right = 10f, top = 6f, bottom = 6f)
                    borderRadius(10f)
                    backgroundColor(if (selected) Color(0x99007DFF) else Color(0x33000000))
                    allCenter()
                }
                event {
                    click {
                        onClick()
                    }
                }
                Text {
                    attr {
                        text(label)
                        fontSize(12f)
                        color(Color.WHITE)
                    }
                }
            }
        }
    }
}
