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
        val statusBar = pagerData.statusBarHeight
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

                ctx.sectionTitle("一、原生 Slider · 档位矩阵")()
                Text {
                    attr {
                        margin(left = 10f, right = 10f, bottom = 6f)
                        text("官方规则：Slider 的材质参数仅作「开关标记」，档位不改变视觉 —— 下列五档应看起来一致；末行为无材质对照")
                        fontSize(11f)
                        color(Color(0x99EEEEEE))
                    }
                }
                // 档位 0~4 全套（“参数仅作标记”实证：五档视觉应相同）
                ctx.sliderRow("ULTRA_THIN(0)", 0, 30f)()
                ctx.sliderRow("THIN(1)", 1, 40f)()
                ctx.sliderRow("REGULAR(2)", 2, 50f)()
                ctx.sliderRow("THICK(3)", 3, 60f)()
                ctx.sliderRow("ULTRA_THICK(4)", 4, 70f)()
                ctx.sliderRow("无材质对照", null, 50f)()
                Text {
                    attr {
                        margin(left = 10f, top = 6f)
                        text("交互行：拖动验证事件回传（当前值：${ctx.sliderValue}）")
                        fontSize(12f)
                        color(Color(0x99EEEEEE))
                    }
                }
                ImmersiveLightSenseSliderOhos {
                    attr {
                        margin(left = 90f, right = 10f, top = 4f, bottom = 8f)
                        alignSelfStretch()
                        height(40f)
                        value(ctx.sliderValue)
                        min(0f)
                        max(100f)
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

                ctx.sectionTitle("二、原生 Switch · 档位矩阵")()
                Text {
                    attr {
                        margin(left = 10f, right = 10f, bottom = 6f)
                        text("官方规则：Toggle(Switch 型) 同样仅作标记，视觉用系统预设 —— 五档应一致；末行为无材质对照")
                        fontSize(11f)
                        color(Color(0x99EEEEEE))
                    }
                }
                ctx.switchRow("ULTRA_THIN(0)", 0, false)()
                ctx.switchRow("THIN(1)", 1, true)()
                ctx.switchRow("REGULAR(2)", 2, false)()
                ctx.switchRow("THICK(3)", 3, true)()
                ctx.switchRow("ULTRA_THICK(4)", 4, false)()
                ctx.switchRow("无材质对照", null, true)()
                Text {
                    attr {
                        margin(left = 10f, top = 6f)
                        text("交互行：点击验证事件回传（当前：${if (ctx.switchOn) "开" else "关"}）")
                        fontSize(12f)
                        color(Color(0x99EEEEEE))
                    }
                }
                ImmersiveLightSenseSwitchOhos {
                    attr {
                        margin(left = 90f, top = 4f, bottom = 8f)
                        size(51f, 31f)
                        isOn(ctx.switchOn)
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

                ctx.sectionTitle("三、原生 Select")()
                Text {
                    attr {
                        margin(left = 10f)
                        text("Select 在右侧固定位置（V2 转发组件不能放进滚动容器，见下方说明）")
                        fontSize(12f)
                        color(Color(0x99EEEEEE))
                    }
                }
                Text {
                    attr {
                        margin(left = 10f, top = 8f)
                        text("当前选项：${ctx.selectOptions[ctx.selectIndex]}")
                        fontSize(13f)
                        color(Color(0xFFEEEEEE))
                    }
                }
            }

            // 原生 Select：**必须放在滚动容器外**（absolutePosition 固定定位）。
            // V2 转发组件的宿主节点位置是一次性静态下发的（NODE_POSITION），
            // 实测放进 Scroller 后：位置不生效（控件出现在页面顶部），
            // 且即使位置对了，滚动时转发内容也不会跟随 —— 架构性限制，不是可修的 bug。
            // 已验证的 V2 组件形态：标题栏容器/TabBar 均为固定定位或页面顶部。
            ImmersiveLightSenseSelectOhos {
                attr {
                    absolutePosition(left = 10f, top = statusBar + 360f)
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

    /** 矩阵行：标签 + Slider。style 为 null 表示无材质对照行（矩阵的关键对照组）。 */
    private fun sliderRow(label: String, style: Int?, value: Float): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    margin(left = 10f, right = 10f, top = 4f, bottom = 4f)
                    flexDirectionRow()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        width(80f)
                        text(label)
                        fontSize(11f)
                        color(Color(0xCCFFFFFF))
                    }
                }
                ImmersiveLightSenseSliderOhos {
                    attr {
                        flex(1f)
                        height(40f)
                        value(value)
                        min(0f)
                        max(100f)
                        if (style != null && ctx.materialEnable) {
                            immersiveMaterial(style = style)
                        }
                    }
                }
            }
        }
    }

    /** 矩阵行：标签 + Switch。style 为 null 表示无材质对照行。 */
    private fun switchRow(label: String, style: Int?, isOn: Boolean): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    margin(left = 10f, right = 10f, top = 4f, bottom = 4f)
                    flexDirectionRow()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        width(80f)
                        text(label)
                        fontSize(11f)
                        color(Color(0xCCFFFFFF))
                    }
                }
                ImmersiveLightSenseSwitchOhos {
                    attr {
                        size(51f, 31f)
                        isOn(isOn)
                        if (style != null && ctx.materialEnable) {
                            immersiveMaterial(style = style)
                        }
                    }
                }
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
