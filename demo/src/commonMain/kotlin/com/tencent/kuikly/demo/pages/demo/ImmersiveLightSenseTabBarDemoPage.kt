package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseTabBarOhos
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 鸿蒙「沉浸光感」TabBar 验证页。
 *
 * 形态：鸿蒙的 TabBar 本身就是「原生 Tabs + 每页一个自定义 tabBar builder」，即自绘。
 * 所以组件用的是原生 `Tabs`（**滑动切换由原生提供**），页签格由组件自己画，
 * 沉浸光感由系统画在原生页签栏上。
 *
 * 关键：组件必须**全屏** —— 业务形态是「item view 全屏 + 页签栏悬浮底部」。
 * 只给一个贴底的小条时，页签栏被压缩在条内，item view 也全屏不了。
 *
 * 每个页签给了不同底色，滑动时页签栏"背后"的内容随之变化，这是观察材质
 * 折射 / 取色最直观的场景。
 */
@Page("12015")
internal class ImmersiveLightSenseTabBarDemoPage : BasePager() {

    private var materialEnable by observable(true)
    private var selectedIndex by observable(0)

    private val tabs = listOf("首页", "发现", "消息", "我的", "设置")

    override fun body(): ViewBuilder {
        val ctx = this
        val statusBar = pagerData.statusBarHeight
        return {
            // 原生 TabBar 组件：**全屏**。业务形态是「每个页签的 item view 全屏 +
            // 页签栏悬浮在页面底部」，所以组件本身铺满整页：
            // - TabContent（每页底色）= 全屏；
            // - 页签栏由 barFloatingStyle 悬浮，barInsetX 左右内缩、barBottomOffset 抬离页底。
            // 之前给 128 高的底部小条时，整条页签栏只能挤在节点那个小区域里。
            ImmersiveLightSenseTabBarOhos {
                attr {
                    absolutePosition(left = 0f, top = 0f, right = 0f, bottom = 0f)
                    backgroundColor(Color.TRANSPARENT)
                    items(ctx.tabs)
                    selectedIndex(ctx.selectedIndex)
                    barInsetX(16f)
                    barBottomOffset(48f)
                    if (ctx.materialEnable) {
                        immersiveMaterial(style = 2, interactive = true)
                    }
                }
                event {
                    tabChanged { index ->
                        KLog.i("ImmLightTest", "[kotlin] tabChanged index=$index")
                        ctx.selectedIndex = index
                    }
                }
            }

            // 以下控制项浮在 TabBar 之上（DSL 里后写的在上层）
            Text {
                attr {
                    absolutePosition(top = statusBar + 68f, left = 16f)
                    text("设备支持: ${PlatformUtils.isImmersiveMaterialSupported()}  选中: ${ctx.tabs[ctx.selectedIndex]}")
                    fontSize(13f)
                    color(Color(0xFF333333))
                }
            }

            View {
                attr {
                    absolutePosition(top = statusBar + 92f, left = 16f)
                    padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                    borderRadius(12f)
                    backgroundColor(Color(0x66000000))
                    allCenter()
                }
                event {
                    click {
                        ctx.materialEnable = !ctx.materialEnable
                    }
                }
                Text {
                    attr {
                        text("开启沉浸光感（当前：${if (ctx.materialEnable) "开" else "关"}）")
                        fontSize(14f)
                        color(Color.WHITE)
                    }
                }
            }

            Text {
                attr {
                    absolutePosition(top = statusBar + 140f, left = 16f, right = 16f)
                    text("左右滑动内容区可切换页签；页签栏背后内容会随之变化，材质折射最明显")
                    fontSize(12f)
                    color(Color(0x99333333))
                }
            }
        }
    }
}
