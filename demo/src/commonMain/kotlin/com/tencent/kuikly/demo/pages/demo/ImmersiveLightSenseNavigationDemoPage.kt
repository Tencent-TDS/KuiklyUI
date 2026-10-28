package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseNavigationOhos
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 官方 HdsNavigation 能力验证页 —— 标题栏沉浸光感全能力对比。
 *
 * 组件全屏挂载（z 底层）：内部承载滚动长图 + HdsNavigation 标题栏。
 * 控制面板（Kuikly，浮于其上）实时切换：
 * - 标题模式：FREE(0) / FULL(1) / MINI(2)；
 * - 滚动联动：关 / COMMON_BLUR(0) / GRADIENT_BLUR(1) —— 内容上滚时标题栏
 *   的渐变模糊是沉浸光感核心视效（官方 ScrollEffectType）；
 * - 材质：标题栏按钮沉浸光感开关；
 * - 返回按钮：显/隐。
 */
@Page("12018")
internal class ImmersiveLightSenseNavigationDemoPage : BasePager() {

    /** 0=FREE 1=FULL 2=MINI */
    private var titleMode by observable(2)

    /** -1=关 0=COMMON_BLUR 1=GRADIENT_BLUR */
    private var scrollEffect by observable(1)

    private var materialEnable by observable(true)

    private var hideBack by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this
        val statusBar = pagerData.statusBarHeight
        return {
            // ── HdsNavigation 组件（声明在最前 = z 最底层，全屏）──
            ImmersiveLightSenseNavigationOhos {
                attr {
                    absolutePosition(left = 0f, top = 0f, right = 0f, bottom = 0f)
                    title("HdsNavigation 演示")
                    subTitle("沉浸光感")
                    menuIcons(listOf("square_and_pencil", "star", "more"))
                    menuLabels(listOf("编辑", "收藏", "更多"))
                    titleMode(ctx.titleMode)
                    enableScrollEffect(ctx.scrollEffect >= 0)
                    scrollEffectType(if (ctx.scrollEffect < 0) 1 else ctx.scrollEffect)
                    hideBackButton(ctx.hideBack)
                    immersiveMaterial(enable = ctx.materialEnable, style = 2)
                }
                event {
                    onMenuClick { index ->
                        KLog.i("ImmLightTest", "[kotlin] hdsnav menuClick index=$index")
                    }
                }
            }

            // ── 控制面板（浮于组件之上）──
            View {
                attr {
                    absolutePosition(top = statusBar + 64f, left = 16f, right = 16f)
                    padding(12f)
                    borderRadius(12f)
                    backgroundColor(Color(0x99000000))
                    flexDirectionColumn()
                }

                // 标题模式
                ctx.row("标题模式") {
                    listOf(
                        0 to "FREE",
                        1 to "FULL",
                        2 to "MINI"
                    ).forEach { (mode, label) ->
                        ctx.chip(label, ctx.titleMode == mode) { ctx.titleMode = mode }()
                    }
                }

                // 滚动效果
                ctx.row("滚动效果") {
                    listOf(
                        -1 to "关",
                        0 to "COMMON",
                        1 to "GRADIENT"
                    ).forEach { (type, label) ->
                        ctx.chip(label, ctx.scrollEffect == type) { ctx.scrollEffect = type }()
                    }
                }

                // 材质 + 返回按钮
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
                        text("操作：上下滚动图片，观察标题栏的渐变模糊（GRADIENT）；切换各模式对比。菜单按钮（右上 3 个）与返回按钮带沉浸光感材质。")
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
