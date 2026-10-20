package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseSelectOhos
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseSliderOhos
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseSwitchOhos
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 鸿蒙原生 Slider / Switch（Switch 映射为 ArkUI Toggle）—— 沉浸光感验证页。
 *
 * 背景：官方规定「Slider / Toggle / Select / 弹窗类组件」可**全页面生效**沉浸光感，
 * 不受「必须在 Navigation 标题栏或底部 TabBar 内」的位置限制。
 * 所以这两个控件换成 ArkUI 原生实现后，放在页面任意位置都应有玻璃质感；
 * 而自绘 Slider / Switch 是普通自绘节点，不在白名单内，不会有材质。
 *
 * 页面顶部开关用于对比「开 / 关材质」两种状态，确认差异确实来自沉浸光感。
 */
@Page("12012")
internal class ImmersiveLightSenseNativeControlsDemoPage : BasePager() {

    private var materialEnable by observable(true)
    private var sliderValue by observable(50f)
    private var switchOn by observable(false)
    private var selectIndex by observable(1)
    private val selectOptions = listOf("低", "中", "高", "自定义")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Image {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                    src("https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg")
                }
            }

            NavBar {
                attr {
                    title = "原生 Slider / Switch"
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    margin(15f)
                }

                Text {
                    attr {
                        margin(10f)
                        text("设备支持沉浸光感: ${PlatformUtils.isImmersiveMaterialSupported()}")
                        fontSize(13f)
                        color(Color(0xFFEEEEEE))
                    }
                }

                // 材质总开关：用于对比开 / 关两种状态
                ctx.panelButton("开启沉浸光感（当前：${if (ctx.materialEnable) "开" else "关"}）") {
                    ctx.materialEnable = !ctx.materialEnable
                }()

                ctx.sectionTitle("一、原生 Slider")()
                ImmersiveLightSenseSliderOhos {
                    attr {
                        margin(left = 10f, right = 10f, top = 8f, bottom = 8f)
                        width(300f)
                        height(40f)
                        value(ctx.sliderValue)
                        min(0f)
                        max(100f)
                        // 对齐 iOS 液态玻璃 iOSSlider 的颜色/粗细属性
                        progressColor(Color(0xFF007DFF))
                        trackColor(Color(0x33808080))
                        thumbColor(Color(0xFFFFFFFF))
                        trackThickness(6f)
                        if (ctx.materialEnable) {
                            immersiveMaterial(style = 2)
                        }
                    }
                    event {
                        valueChanged { v ->
                            KLog.i("ImmLightTest", "[kotlin] slider valueChanged v=$v")
                            ctx.sliderValue = v
                        }
                    }
                }
                Text {
                    attr {
                        margin(left = 10f)
                        text("当前值：${ctx.sliderValue}")
                        fontSize(13f)
                        color(Color(0xFFEEEEEE))
                    }
                }

                ctx.sectionTitle("二、原生 Switch")()
                ImmersiveLightSenseSwitchOhos {
                    attr {
                        margin(left = 10f, top = 8f, bottom = 8f)
                        size(51f, 31f)
                        isOn(ctx.switchOn)
                        // 对齐 iOS 液态玻璃 iOSSwitch 的颜色属性
                        onColor(Color(0xFF007DFF))
                        thumbColor(Color(0xFFFFFFFF))
                        if (ctx.materialEnable) {
                            immersiveMaterial(style = 2)
                        }
                    }
                    event {
                        switchChanged { on ->
                            KLog.i("ImmLightTest", "[kotlin] switch switchChanged on=$on")
                            ctx.switchOn = on
                        }
                    }
                }
                Text {
                    attr {
                        margin(left = 10f)
                        text("开关：${if (ctx.switchOn) "开" else "关"}")
                        fontSize(13f)
                        color(Color(0xFFEEEEEE))
                    }
                }

                ctx.sectionTitle("三、原生 Select")()
                ImmersiveLightSenseSelectOhos {
                    attr {
                        margin(left = 10f, top = 8f, bottom = 8f)
                        width(150f)
                        height(40f)
                        options(ctx.selectOptions)
                        selectedIndex(ctx.selectIndex)
                        if (ctx.materialEnable) {
                            immersiveMaterial(style = 2)
                        }
                    }
                    event {
                        selectChanged { index, value ->
                            KLog.i("ImmLightTest", "[kotlin] select changed idx=$index value=$value")
                            ctx.selectIndex = index
                        }
                    }
                }
                Text {
                    attr {
                        margin(left = 10f)
                        text("当前选项：${ctx.selectOptions[ctx.selectIndex]}")
                        fontSize(13f)
                        color(Color(0xFFEEEEEE))
                    }
                }
            }
        }
    }

    private fun sectionTitle(title: String): ViewBuilder = {
        Text {
            attr {
                margin(top = 16f, left = 10f, right = 10f, bottom = 6f)
                text(title)
                fontSize(14f)
                fontWeightSemiBold()
                color(Color(0xCCFFFFFF))
            }
        }
    }

    private fun panelButton(label: String, onClick: () -> Unit): ViewBuilder {
        return {
            View {
                attr {
                    margin(left = 10f, right = 10f, top = 6f, bottom = 6f)
                    padding(12f)
                    borderRadius(12f)
                    backgroundColor(Color(0x66000000))
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
                        fontSize(15f)
                        color(Color.WHITE)
                    }
                }
            }
        }
    }
}
