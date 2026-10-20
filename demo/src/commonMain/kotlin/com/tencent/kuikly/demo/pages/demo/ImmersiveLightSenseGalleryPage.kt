package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseDialogOhos
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseOhos
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseSelectOhos
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseSliderOhos
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseSwitchOhos
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseTabBarOhos
import com.tencent.kuikly.core.views.willDismiss
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 沉浸光感组件「总览页」—— 6 个组件一站式对照。
 *
 * 页面骨架本身就是两个组件：
 * - 顶部标题栏 = [ImmersiveLightSenseOhos]，标题文字随页签切换显示当前组件名；
 * - 底部页签栏 = [ImmersiveLightSenseTabBarOhos]，全屏组件（每个 item 对应一屏），
 *   悬浮胶囊的位置（左右内缩 / 离底距离）在「TabBar」页里实时可调。
 *
 * 其余四屏分别展示 Slider / Switch / Select / Dialog。
 * 页签间的空隙是原生 TabContent（彩色底），那里可以直接左右滑动切页；
 * Kuikly 内容覆盖的区域点击/滑动走业务层，切页请点底部页签。
 */
@Page("12016")
internal class ImmersiveLightSenseGalleryPage : BasePager() {

    /** 页签下标（标题栏文字与内容区都由它驱动） */
    private var selectedIndex by observable(0)

    // —— TabBar 位置（业务可实时调整）——
    private var barInset by observable(16f)
    private var barBottom by observable(48f)

    // —— 各组件状态 ——
    private var sliderValue by observable(50f)
    private var switchOn by observable(false)
    private var selectIndex by observable(1)
    private var dialogVisible by observable(false)
    private var dialogStyle by observable(2)
    private var dialogTinted by observable(false)

    private val tabs = listOf("Slider", "Switch", "Select", "Dialog", "TabBar")
    private val selectOptions = listOf("低", "中", "高", "自定义")

    override fun body(): ViewBuilder {
        val ctx = this
        val statusBar = pagerData.statusBarHeight
        return {
            // ── 底部页签栏（全屏组件，最先声明 = 最底层；原生 TabContent 提供彩色底与滑动切换） ──
            ImmersiveLightSenseTabBarOhos {
                attr {
                    absolutePosition(left = 0f, top = 0f, right = 0f, bottom = 0f)
                    backgroundColor(Color.TRANSPARENT)
                    items(ctx.tabs)
                    selectedIndex(ctx.selectedIndex)
                    barInsetX(ctx.barInset)
                    barBottomOffset(ctx.barBottom)
                    immersiveMaterial(style = 2)
                }
                event {
                    tabChanged { index ->
                        ctx.selectedIndex = index
                    }
                }
            }

            // Kuikly 侧透明点击条：接管悬浮胶囊的点击（HDS 材质层渲染的页签项
            // 命中不可达，ArkTS 宿主内 onClick 也不触发——详见 12015 注释）。
            // 几何与胶囊参数联动：高度 = barBottom + 胶囊高(64)。
            View {
                attr {
                    absolutePosition(left = 0f, right = 0f, bottom = 0f)
                    height(ctx.barBottom + 64f)
                    flexDirectionRow()
                    backgroundColor(Color.TRANSPARENT)
                }
                ctx.tabs.forEachIndexed { idx, _ ->
                    View {
                        attr {
                            flex(1f)
                            alignSelfStretch()
                            backgroundColor(Color.TRANSPARENT)
                        }
                        event {
                            click {
                                ctx.selectedIndex = idx
                            }
                        }
                    }
                }
            }

            // ── 标题栏（共享，标题 = 当前组件名） ──
            ImmersiveLightSenseOhos {
                attr {
                    height(statusBar + 56f)
                    padding(top = statusBar, left = 12f, right = 12f)
                    flexDirectionRow()
                    alignItemsCenter()
                    backgroundColor(Color.TRANSPARENT)
                    immersiveMaterial(style = 2, interactive = true)
                }
                // 返回
                ctx.pill(36f, 36f) {
                    Text { attr { text("←"); fontSize(18f); color(Color(0xFF333333)) } }
                    event { click { ctx.closePage() } }
                }()
                // 标题：跟随页签。本页背景是浅色 TabContent，玻璃不自动反色，
                // 文字必须用深色（白字会没入浅玻璃，12016 第一版踩过）
                View {
                    attr { flex(1f); allCenter() }
                    Text {
                        attr {
                            text("${ctx.tabs[ctx.selectedIndex]} · 沉浸光感")
                            fontSize(17f)
                            color(Color(0xFF333333))
                        }
                    }
                }
                // 右侧：设备能力
                ctx.pill(72f, 36f) {
                    Text {
                        attr {
                            text(if (PlatformUtils.isImmersiveMaterialSupported()) "支持" else "不支持")
                            fontSize(13f)
                            color(Color(0xFF333333))
                        }
                    }
                }()
            }

            // ── 内容区：flex 填充标题栏以下的剩余空间，底部 margin 让位给悬浮胶囊。
            // 不用 absolutePosition+bottom 锚定：让位量由 flex 算术保证，
            // 页签栏无论如何都留在内容区之外、可点击。 ──
            Scroller {
                attr {
                    flex(1f)
                    marginBottom(ctx.barBottom + 88f)
                }

                // ════════ 屏 1：Slider ════════
                vif({ ctx.selectedIndex == 0 }) {
                    ctx.panel("Slider · REGULAR + 交互形变") {
                        ImmersiveLightSenseSliderOhos {
                            attr {
                                margin(top = 10f)
                                alignSelfStretch()
                                height(40f)
                                value(ctx.sliderValue)
                                min(0f)
                                max(100f)
                                progressColor(Color(0xFF007DFF))
                                trackColor(Color(0x33808080))
                                thumbColor(Color(0xFFFFFFFF))
                                trackThickness(6f)
                                immersiveMaterial(style = 2, interactive = true)
                            }
                            event {
                                valueChanged { v -> ctx.sliderValue = v }
                            }
                        }
                        Text {
                            attr {
                                margin(top = 8f)
                                text("当前值：${ctx.sliderValue}")
                                fontSize(13f)
                                color(Color(0xFFEEEEEE))
                            }
                        }
                    }()
                    ctx.panel("Slider · THICK + 材质赋色") {
                        ImmersiveLightSenseSliderOhos {
                            attr {
                                margin(top = 10f)
                                alignSelfStretch()
                                height(40f)
                                value(70f)
                                min(0f)
                                max(100f)
                                immersiveMaterial(style = 4, materialColor = Color(0x33007DFF))
                            }
                        }
                    }()
                }

                // ════════ 屏 2：Switch ════════
                vif({ ctx.selectedIndex == 1 }) {
                    ctx.panel("Switch · 交互形变") {
                        ImmersiveLightSenseSwitchOhos {
                            attr {
                                margin(top = 10f)
                                size(51f, 31f)
                                isOn(ctx.switchOn)
                                onColor(Color(0xFF007DFF))
                                thumbColor(Color(0xFFFFFFFF))
                                immersiveMaterial(style = 2, interactive = true)
                            }
                            event {
                                switchChanged { on -> ctx.switchOn = on }
                            }
                        }
                        Text {
                            attr {
                                margin(top = 8f)
                                text("开关：${if (ctx.switchOn) "开" else "关"}")
                                fontSize(13f)
                                color(Color(0xFFEEEEEE))
                            }
                        }
                    }()
                }

                // ════════ 屏 3：Select ════════
                vif({ ctx.selectedIndex == 2 }) {
                    ctx.panel("Select · 点光源") {
                        ImmersiveLightSenseSelectOhos {
                            attr {
                                margin(top = 10f)
                                width(160f)
                                height(40f)
                                options(ctx.selectOptions)
                                selectedIndex(ctx.selectIndex)
                                immersiveMaterial(style = 2, lightEffectColor = Color(0x80FFFFFF))
                            }
                            event {
                                selectChanged { index, _ -> ctx.selectIndex = index }
                            }
                        }
                        Text {
                            attr {
                                margin(top = 8f)
                                text("当前选项：${ctx.selectOptions[ctx.selectIndex]}")
                                fontSize(13f)
                                color(Color(0xFFEEEEEE))
                            }
                        }
                    }()
                }

                // ════════ 屏 4：Dialog ════════
                vif({ ctx.selectedIndex == 3 }) {
                    ctx.panel("Dialog · REGULAR") {
                        ctx.pill(200f, 40f) {
                            Text { attr { text("打开弹窗"); fontSize(14f); color(Color(0xFF333333)) } }
                            event {
                                click {
                                    ctx.dialogStyle = 2
                                    ctx.dialogTinted = false
                                    ctx.dialogVisible = true
                                }
                            }
                        }()
                    }()
                    ctx.panel("Dialog · THIN + 材质赋色") {
                        ctx.pill(200f, 40f) {
                            Text { attr { text("打开弹窗（赋色）"); fontSize(14f); color(Color(0xFF333333)) } }
                            event {
                                click {
                                    ctx.dialogStyle = 1
                                    ctx.dialogTinted = true
                                    ctx.dialogVisible = true
                                }
                            }
                        }()
                    }()
                }

                // ════════ 屏 5：TabBar 位置实调 ════════
                vif({ ctx.selectedIndex == 4 }) {
                    ctx.panel("页签栏位置（实时生效）") {
                        ctx.stepper("左右内缩 barInsetX", ctx.barInset) {
                            ctx.barInset = (ctx.barInset + it).coerceIn(0f, 48f)
                        }()
                        ctx.stepper("离底距离 barBottomOffset", ctx.barBottom) {
                            ctx.barBottom = (ctx.barBottom + it).coerceIn(0f, 96f)
                        }()
                        Text {
                            attr {
                                margin(top = 10f)
                                text("拖动页面空隙或点击底部页签可切页；此页调整的值即业务的 attr 入参")
                                fontSize(12f)
                                color(Color(0x99EEEEEE))
                            }
                        }
                    }()
                }
            }

            // ── 弹窗（组件本体，vif 控制显隐） ──
            vif({ ctx.dialogVisible }) {
                ImmersiveLightSenseDialogOhos {
                    attr {
                        width(300f)
                        height(200f)
                        borderRadius(24f)
                        immersiveMaterial(
                            style = ctx.dialogStyle,
                            applyShadow = true,
                            materialColor = if (ctx.dialogTinted) Color(0x33007DFF) else null
                        )
                    }
                    event {
                        willDismiss { _ -> ctx.dialogVisible = false }
                    }
                    // 内容根：flex 撑满弹窗（矩形与背板重合），文字用深色（材质不自动反色）
                    View {
                        attr {
                            flex(1f)
                            alignSelfStretch()
                            padding(24f)
                            flexDirectionColumn()
                            alignItemsCenter()
                            justifyContentCenter()
                        }
                        Text {
                            attr {
                                text(if (ctx.dialogTinted) "THIN + 赋色弹窗" else "REGULAR 弹窗")
                                fontSize(17f)
                                color(Color(0xFF333333))
                            }
                        }
                        Text {
                            attr {
                                margin(top = 8f)
                                text("背板 = 系统材质；borderRadius 映射背板圆角")
                                fontSize(12f)
                                color(Color(0x99333333))
                            }
                        }
                        ctx.pill(120f, 36f) {
                            Text { attr { text("关闭"); fontSize(14f); color(Color(0xFF333333)) } }
                            event { click { ctx.dialogVisible = false } }
                        }()
                    }
                }
            }
        }
    }

    private fun closePage() {
        getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }

    /** 半透明面板容器 */
    private fun panel(title: String, content: ViewBuilder): ViewBuilder {
        return {
            View {
                attr {
                    margin(left = 16f, right = 16f, top = 12f, bottom = 4f)
                    padding(16f)
                    borderRadius(20f)
                    backgroundColor(Color(0x4D000000))
                    flexDirectionColumn()
                }
                Text {
                    attr {
                        text(title)
                        fontSize(14f)
                        color(Color(0xFFEEEEEE))
                    }
                }
                content.invoke(this)
            }
        }
    }

    /** 胶囊按钮：透明背景 + 沉浸光感材质（全页面生效白名单内不需要容器） */
    private fun pill(width: Float, height: Float, content: ViewBuilder): ViewBuilder {
        return {
            View {
                attr {
                    width(width)
                    height(height)
                    borderRadius(height / 2f)
                    allCenter()
                    margin(top = 8f)
                    backgroundColor(Color.TRANSPARENT)
                    immersiveMaterial(style = 2, interactive = true)
                }
                content.invoke(this)
            }
        }
    }

    /** 一行「标签 −  +」步进器，用于实时调 TabBar 位置参数 */
    private fun stepper(label: String, value: Float, onChange: (Float) -> Unit): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    margin(top = 12f)
                    flexDirectionRow()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        text(label)
                        fontSize(13f)
                        color(Color(0xFFEEEEEE))
                        flex(1f)
                    }
                }
                Text {
                    attr {
                        width(36f); height(32f); borderRadius(16f)
                        text("−")
                        fontSize(16f); color(Color(0xFF333333))
                        textAlignCenter()
                        backgroundColor(Color.TRANSPARENT)
                        immersiveMaterial(style = 2)
                    }
                    event { click { onChange(-8f) } }
                }
                Text {
                    attr {
                        margin(left = 8f)
                        width(52f)
                        text("${value}")
                        fontSize(13f)
                        color(Color.WHITE)
                        textAlignCenter()
                    }
                }
                Text {
                    attr {
                        width(36f); height(32f); borderRadius(16f)
                        text("＋")
                        fontSize(16f); color(Color(0xFF333333))
                        textAlignCenter()
                        backgroundColor(Color.TRANSPARENT)
                        immersiveMaterial(style = 2)
                    }
                    event { click { onChange(8f) } }
                }
            }
        }
    }
}
