package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ImmersiveStyle
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.utils.PlatformUtils.ImmersiveMaterialState
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseTabBarOhos
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseTabBarSelfDrawnOhos
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 「自绘 TabBar」验证页 —— 两种 TabBar **分屏对照**（按钮切换，同时只渲染一版）。
 *
 * 为什么分屏：两版都是全屏 V2 转发组件，同屏叠加时白底互相遮挡、位置互相顶替，
 * 根本无法对照（第一版的教训）。切换按钮在页面顶部。
 *
 * 验证命题：自绘版（原生 Tabs(End) + ArkTS 自绘格 + systemMaterial）在 Tabs scope
 * 内能否出材质。命题成立则「外形自由（圆角/高度）+ 材质」路线打通。
 *
 * 顶部控制项说明：
 * - 版本切换：① HdsTabs 版（官方胶囊，质感基准）/ ② 自绘版（圆角业务可控，验证对象）；
 * - 圆角：只影响 ②（① 圆角官方固定）；
 * - 材质开关：两版共用（开/关对照）。
 */
@Page("12017")
internal class ImmersiveLightSenseTabBarSelfDrawnDemoPage : BasePager() {

    private var selectedIndex by observable(0)
    private var materialEnable by observable(true)

    /** true = 显示 ① HdsTabs 版；false = 显示 ② 自绘版 */
    private var showHds by observable(true)

    /** 整个 bar 的圆角，实时可调（自绘版核心卖点：作用在 bar 底板，非单个格子） */
    private var pillRadius by observable(0f)

    /** 材质档位：0=ULTRA_THIN 1=THIN 2=REGULAR 3=THICK 4=ULTRA_THICK（默认 THIN 更通透） */
    private var materialStyle by observable(1)

    private val tabs = listOf("首页", "发现", "消息", "我的", "设置")

    override fun body(): ViewBuilder {
        val ctx = this
        val statusBar = pagerData.statusBarHeight
        val stateText = when (PlatformUtils.getImmersiveMaterialState()) {
            ImmersiveMaterialState.ENABLE -> "ENABLE（浮层默认带材质）"
            ImmersiveMaterialState.DISABLE -> "DISABLE（材质全关）"
            else -> "DEFAULT（仅 Dialog/Toast 默认开）"
        }
        return {
            // 全屏背景图：玻璃材质需要背后有内容才能观察
            Image {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                    src("https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg")
                }
            }

            // ── TabBar 组件（声明在最前 = z 序最底层，12016 同款）：
            // NavBar/控制面板浮在它之上，不被 TabContent 底色盖住、可正常点击。 ──

            // ── ① HdsTabs 版（质感基准） ──
            vif({ ctx.showHds }) {
                ImmersiveLightSenseTabBarOhos {
                    attr {
                        // 全屏挂载（12016 验收过的组合）：ArkTS 内 barPosition(End)
                        // 把 bar 放到屏幕最底边。
                        absolutePosition(left = 0f, top = 0f, right = 0f, bottom = 0f)
                        // 全屏挂载必须透明 TabContent：内置逐页换色底色会把整页盖住
                        //（实测淡紫色铺满页面、控制面板被盖点不到）
                        contentTransparent(true)
                        items(ctx.tabs)
                        selectedIndex(ctx.selectedIndex)
                        barInsetX(16f)
                        barBottomOffset(24f)
                        // 无条件下发（enable 参数控制开/关）：if 包裹时"关"不下发任何
                        // prop，ets 侧 materialEnable 永远停在 true，材质关不掉
                        immersiveMaterial(
                            enable = ctx.materialEnable,
                            style = ImmersiveStyle.REGULAR
                        )
                    }
                    event {
                        tabChanged { index ->
                            KLog.i("ImmLightTest", "[kotlin] hdstabs tabChanged index=$index")
                            ctx.selectedIndex = index
                        }
                    }
                }
            }

            // ── ② 自绘版（验证对象：圆角/高度业务可控） ──
            vif({ !ctx.showHds }) {
                ImmersiveLightSenseTabBarSelfDrawnOhos {
                    attr {
                        // 全屏挂载：节点铺满整页，ArkTS 内 Tabs(barPosition=End) 的
                        // bar 才会落在屏幕最底边。
                        absolutePosition(left = 0f, top = 0f, right = 0f, bottom = 0f)
                        items(ctx.tabs)
                        selectedIndex(ctx.selectedIndex)
                        pillRadius(ctx.pillRadius)
                        pillHeight(52f)
                        // 悬浮位置与 HdsTabs 版对齐（barInsetX=16/barBottomOffset=48），
                        // 两版可直观对照官方背板材质效果
                        barInsetX(16f)
                        barBottomOffset(48f)
                        // 无条件下发（enable 参数控制开/关）：if 包裹时"关"不下发任何
                        // prop，ets 侧 materialEnable 永远停在 true，材质关不掉
                        immersiveMaterial(
                            enable = ctx.materialEnable,
                            style = ctx.materialStyle
                        )
                    }
                    event {
                        tabChanged { index ->
                            KLog.i("ImmLightTest", "[kotlin] selfdrawn tabChanged index=$index")
                            ctx.selectedIndex = index
                        }
                    }
                }
            }

            NavBar {
                attr {
                    title = "自绘 TabBar 验证"
                }
            }

            // ── 控制面板 ──
            View {
                attr {
                    margin(top = statusBar + 68f, left = 16f, right = 16f)
                    padding(12f)
                    borderRadius(12f)
                    backgroundColor(Color(0x66000000))
                    flexDirectionColumn()
                }
                // 版本切换
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                    }
                    Text {
                        attr {
                            text("版本：")
                            fontSize(13f)
                            color(Color.WHITE)
                        }
                    }
                    ctx.versionButton("① HdsTabs 版", true)()
                    ctx.versionButton("② 自绘版", false)()
                }
                // 圆角（只影响自绘版）
                View {
                    attr {
                        margin(top = 8f)
                        flexDirectionRow()
                        alignItemsCenter()
                    }
                    Text {
                        attr {
                            text("②bar圆角 ${ctx.pillRadius}vp：")
                            fontSize(13f)
                            color(Color.WHITE)
                        }
                    }
                    listOf(0f, 8f, 20f, 26f).forEach { r ->
                        View {
                            attr {
                                margin(left = 6f)
                                padding(left = 10f, right = 10f, top = 6f, bottom = 6f)
                                borderRadius(10f)
                                backgroundColor(
                                    if (ctx.pillRadius == r) Color(0x66007DFF) else Color(0x33000000)
                                )
                                allCenter()
                            }
                            event {
                                click {
                                    ctx.pillRadius = r
                                }
                            }
                            Text {
                                attr {
                                    text(if (r == 0f) "直角" else "$r")
                                    fontSize(12f)
                                    color(Color.WHITE)
                                }
                            }
                        }
                    }
                }
                // 材质开关（两版共用）
                View {
                    attr {
                        margin(top = 8f)
                        flexDirectionRow()
                        alignItemsCenter()
                    }
                    Text {
                        attr {
                            text("材质：${if (ctx.materialEnable) "开" else "关"}（两版共用）  ")
                            fontSize(13f)
                            color(Color.WHITE)
                        }
                    }
                    View {
                        attr {
                            padding(left = 10f, right = 10f, top = 6f, bottom = 6f)
                            borderRadius(10f)
                            backgroundColor(Color(0x33000000))
                            allCenter()
                        }
                        event {
                            click {
                                ctx.materialEnable = !ctx.materialEnable
                            }
                        }
                        Text {
                            attr {
                                text("切换")
                                fontSize(12f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
                // 材质档位（只影响自绘版格子）
                View {
                    attr {
                        margin(top = 8f)
                        flexDirectionRow()
                        alignItemsCenter()
                    }
                    Text {
                        attr {
                            text("②材质档位：")
                            fontSize(13f)
                            color(Color.WHITE)
                        }
                    }
                    listOf(0 to "极薄", 1 to "薄", 2 to "标准", 3 to "厚", 4 to "极厚").forEach { (s, label) ->
                        View {
                            attr {
                                margin(left = 6f)
                                padding(left = 10f, right = 10f, top = 6f, bottom = 6f)
                                borderRadius(10f)
                                backgroundColor(
                                    if (ctx.materialStyle == s) Color(0x66007DFF) else Color(0x33000000)
                                )
                                allCenter()
                            }
                            event {
                                click {
                                    ctx.materialStyle = s
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
                Text {
                    attr {
                        margin(top = 8f)
                        text("设备支持: ${PlatformUtils.isImmersiveMaterialSupported()}  应用级状态: $stateText")
                        fontSize(11f)
                        color(Color(0x99EEEEEE))
                    }
                }
            }

            Text {
                attr {
                    margin(top = 10f, left = 16f, right = 16f)
                    text("观察点：底部页签栏是否有玻璃质感（背景图透过格子的模糊折射）。" +
                        "② 的圆角随上方按钮实时变化。点击/滑动格子均可切换。")
                    fontSize(11f)
                    color(Color(0x99333333))
                }
            }
        }
    }

    /** 版本切换按钮 */
    private fun versionButton(label: String, hds: Boolean): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    margin(left = 8f)
                    padding(left = 10f, right = 10f, top = 6f, bottom = 6f)
                    borderRadius(10f)
                    backgroundColor(
                        if (ctx.showHds == hds) Color(0x66007DFF) else Color(0x33000000)
                    )
                    allCenter()
                }
                event {
                    click {
                        ctx.showHds = hds
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
