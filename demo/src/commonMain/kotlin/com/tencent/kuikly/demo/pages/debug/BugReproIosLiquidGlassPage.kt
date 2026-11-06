/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.demo.pages.debug

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

private const val TAG = "BD_IOS_LIQUID_GLASS"

private const val BAR_HEIGHT = 62f
private const val BAR_RADIUS = 31f

/** 阴影加深，便于肉眼判断阴影是否存在 */
private val DEMO_SHADOW = BoxShadow(0f, 6f, 16f, Color(0x66000000L))

/** 卡片左右 margin(12) + padding(14)，玻璃条两侧各留出 14 的内边距 */
private const val CARD_HORIZONTAL_INSET = (12f + 14f) * 2

/**
 * iOS 液态玻璃问题回归页（对应《Kuikly-iOS 液态玻璃问题汇总》问题 1~5）。
 *
 * 每个问题一张卡片，每个 case 标注「预期」，任一 case 与预期不符即为回归：
 *   问题 1 —— 渲染器产物是否包含玻璃代码
 *   问题 2 —— View + glassEffectIOS() 的圆角（静态 / 四角不一致 / 动态切换）
 *   问题 3 —— boxShadow 与玻璃包装层争用槽位（含 container、enable=false、挂载卸载压力）
 *   问题 4 —— LiquidGlass 控件的圆角与子内容裁剪
 *   问题 5 —— Scroller / List 上的玻璃（不支持，告警 + 推荐写法）
 *
 * 必须在 iOS 26+ 且用 iOS 26 SDK 编译的包上运行。
 *
 * 注意：本页私有片段方法返回 ViewBuilder，调用处必须再跟一个 ()，例如 ctx.caseTitle("x")()。
 */
@Page("BugReproIosLiquidGlassPage")
internal class BugReproIosLiquidGlassPage : BasePager() {

    /** 问题 2 / 4：圆角开关（0 ↔ 31） */
    private var radiusOn by observable(true)

    /** 问题 3：先玻璃、后叠加阴影 */
    private var shadowAdded by observable(false)

    /** 问题 3：挂载 / 卸载压力 case 的显示状态与已完成轮次 */
    private var stressShow by observable(true)
    private var stressRound by observable(0)

    /** 问题 3：玻璃开关（验证玻璃与 boxShadow 的动态隔离） */
    private var glassOn by observable(true)

    private val glassSupported: Boolean
        get() = PlatformUtils.isLiquidGlassSupported()

    private val barWidth: Float
        get() = pagerData.pageViewWidth - CARD_HORIZONTAL_INSET

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 玻璃需要背后有可折射的内容，整页铺一张高饱和渐变底图
            ctx.backgroundLayer()()
            Scroller {
                attr {
                    flex(1f)
                }
                ctx.gap(ctx.pagerData.statusBarHeight)()
                ctx.issue1Section()()
                ctx.issue2Section()()
                ctx.issue3Section()()
                ctx.issue4Section()()
                ctx.issue5Section()()
                ctx.gap(40f)()
            }
        }
    }

    override fun created() {
        super.created()
        KLog.i(
            TAG,
            "env platform=${PlatformUtils.getPlatform()} osVersion=${PlatformUtils.getOSVersion()} " +
                "liquidGlassSupported=${PlatformUtils.isLiquidGlassSupported()}"
        )
    }

    // region 问题 1 —— 渲染器是否包含玻璃代码

    private fun issue1Section(): ViewBuilder {
        val ctx = this
        return {
            ctx.sectionTitle("问题 1 · 渲染器产物是否包含玻璃代码")()
            ctx.card {
                ctx.bodyText(
                    "platform=${PlatformUtils.getPlatform()}  osVersion=${PlatformUtils.getOSVersion()}  " +
                        "isLiquidGlassSupported=${PlatformUtils.isLiquidGlassSupported()}"
                )()
                if (!ctx.glassSupported) {
                    ctx.warnText("当前系统不支持玻璃（需 iOS 26+），本页以降级样式呈现。")()
                }
                ctx.caseTitle("预期：上方玻璃条与下方灰条明显不同；若一致，说明渲染器用低于 iOS 26 的 SDK 编译，玻璃代码已被剔除")()
                ctx.glassBar { borderRadius(22f) }()
                ctx.gap(8f)()
                View {
                    attr {
                        size(ctx.barWidth, BAR_HEIGHT)
                        borderRadius(22f)
                        backgroundColor(Color(0x80000000L))
                    }
                }
            }()
        }
    }

    // endregion

    // region 问题 2 —— View + glassEffectIOS() 圆角

    private fun issue2Section(): ViewBuilder {
        val ctx = this
        return {
            ctx.sectionTitle("问题 2 · View + glassEffectIOS() 圆角")()
            ctx.card {
                ctx.caseTitle("2-1 borderRadius(31)　预期：玻璃外壳为胶囊形，四角不露直角")()
                ctx.glassBar { borderRadius(BAR_RADIUS) }()

                ctx.caseTitle("2-2 四角不一致 (50,10,50,10)　预期：左侧大圆角、右侧小圆角")()
                ctx.glassBar {
                    borderRadius(topLeft = 50f, topRight = 10f, bottomLeft = 50f, bottomRight = 10f)
                }()

                ctx.caseTitle("2-3 动态切换 0 ↔ 31　预期：玻璃与白色描边同步变化，不出现多余矩形")()
                ctx.stateText { "当前 radiusOn=${ctx.radiusOn}" }()
                ctx.glassBar {
                    borderRadius(if (ctx.radiusOn) BAR_RADIUS else 0f)
                    border(Border(2f, BorderStyle.SOLID, Color.WHITE))
                }()
                ctx.gap(10f)()
                ctx.actionButton("切换圆角 0 ↔ 31") {
                    ctx.radiusOn = !ctx.radiusOn
                }()
            }()
        }
    }

    // endregion

    // region 问题 3 —— boxShadow 与玻璃包装层争用槽位

    private fun issue3Section(): ViewBuilder {
        val ctx = this
        return {
            ctx.sectionTitle("问题 3 · boxShadow + borderRadius + 玻璃")()
            ctx.card {
                ctx.caseTitle("3-1 三个属性一次性设置　预期：玻璃正常显示，boxShadow 被忽略（系统玻璃自带阴影）")()
                ctx.glassBar {
                    borderRadius(BAR_RADIUS)
                    boxShadow(DEMO_SHADOW)
                }()

                ctx.caseTitle("3-2 先玻璃、后叠加阴影　预期：点击后玻璃仍在，boxShadow 被忽略")()
                ctx.glassBar {
                    borderRadius(BAR_RADIUS)
                    if (ctx.shadowAdded) {
                        boxShadow(DEMO_SHADOW)
                    }
                }()
                ctx.gap(10f)()
                ctx.actionButton("叠加 boxShadow（单向，撤销需重进页面）") {
                    ctx.shadowAdded = true
                }()

                ctx.caseTitle("3-3 glassEffectContainerIOS + 圆角 + 阴影　预期：容器有阴影，内部两颗玻璃正常")()
                View {
                    attr {
                        size(ctx.barWidth, BAR_HEIGHT)
                        borderRadius(BAR_RADIUS)
                        boxShadow(DEMO_SHADOW)
                        flexDirectionRow()
                        allCenter()
                        if (ctx.glassSupported) {
                            glassEffectContainerIOS(12f)
                        }
                    }
                    for (i in 0 until 2) {
                        View {
                            attr {
                                size(46f, 46f)
                                margin(left = 8f, right = 8f)
                                borderRadius(23f)
                                if (ctx.glassSupported) {
                                    glassEffectIOS()
                                } else {
                                    backgroundColor(Color(0x80000000L))
                                }
                            }
                        }
                    }
                }

                ctx.caseTitle("3-4 glassEffectIOS(enable = false) + 圆角 + 阴影　预期：阴影正常显示")()
                View {
                    attr {
                        size(ctx.barWidth, BAR_HEIGHT)
                        borderRadius(BAR_RADIUS)
                        boxShadow(DEMO_SHADOW)
                        backgroundColor(Color(0x33FFFFFFL))
                        glassEffectIOS(enable = false)
                    }
                }

                ctx.caseTitle("3-5 动态开关玻璃 + 圆角 + boxShadow　预期：玻璃开 → 无额外阴影；玻璃关 → 圆角阴影恢复")()
                ctx.stateText { "当前 glassOn=${ctx.glassOn}" }()
                View {
                    attr {
                        size(ctx.barWidth, BAR_HEIGHT)
                        borderRadius(BAR_RADIUS)
                        boxShadow(DEMO_SHADOW)
                        if (ctx.glassSupported) {
                            glassEffectIOS(enable = ctx.glassOn)
                        }
                        if (!ctx.glassOn || !ctx.glassSupported) {
                            backgroundColor(Color(0x33FFFFFFL))
                        } else {
                            backgroundColor(Color.TRANSPARENT)
                        }
                    }
                }
                ctx.gap(10f)()
                ctx.actionButton("切换玻璃开关") {
                    ctx.glassOn = !ctx.glassOn
                }()

                ctx.caseTitle("3-6 挂载 / 卸载压力（玻璃包装层生命周期）　预期：连续 20 轮不崩溃，结束后玻璃正常")()
                ctx.stateText { "已完成轮次 ${ctx.stressRound} / 20" }()
                vif({ ctx.stressShow }) {
                    ctx.glassBar {
                        borderRadius(BAR_RADIUS)
                        boxShadow(DEMO_SHADOW)
                    }()
                }
                ctx.gap(10f)()
                ctx.actionButton("开始 20 轮挂载 / 卸载") {
                    ctx.startStress()
                }()
            }()
        }
    }

    private fun startStress() {
        stressRound = 0
        for (i in 1..40) {
            setTimeout(pagerId, i * 60) {
                stressShow = !stressShow
                if (stressShow) {
                    stressRound += 1
                }
            }
        }
    }

    // endregion

    // region 问题 4 —— LiquidGlass 控件圆角

    private fun issue4Section(): ViewBuilder {
        val ctx = this
        return {
            ctx.sectionTitle("问题 4 · LiquidGlass 控件圆角")()
            ctx.card {
                ctx.caseTitle("4-1 borderRadius(31) + 子内容　预期：玻璃外壳与橙色子内容都是胶囊形")()
                LiquidGlass {
                    attr {
                        size(ctx.barWidth, BAR_HEIGHT)
                        borderRadius(BAR_RADIUS)
                    }
                    ctx.contentBlock()()
                }

                ctx.caseTitle("4-2 四角不一致 (50,10,50,10) + 子内容　预期：外壳与子内容形状一致")()
                LiquidGlass {
                    attr {
                        size(ctx.barWidth, BAR_HEIGHT)
                        borderRadius(topLeft = 50f, topRight = 10f, bottomLeft = 50f, bottomRight = 10f)
                    }
                    ctx.contentBlock()()
                }

                ctx.caseTitle("4-3 动态切换 0 ↔ 31　预期：玻璃与白色描边同步变化")()
                ctx.stateText { "当前 radiusOn=${ctx.radiusOn}" }()
                LiquidGlass {
                    attr {
                        size(ctx.barWidth, BAR_HEIGHT)
                        borderRadius(if (ctx.radiusOn) BAR_RADIUS else 0f)
                        border(Border(2f, BorderStyle.SOLID, Color.WHITE))
                    }
                }
                ctx.gap(10f)()
                ctx.actionButton("切换圆角 0 ↔ 31") {
                    ctx.radiusOn = !ctx.radiusOn
                }()
            }()
        }
    }

    /** 覆盖左侧 40% 的橙色块，用于观察子内容是否被裁成圆角 */
    private fun contentBlock(): ViewBuilder {
        val blockWidth = barWidth * 0.4f
        return {
            View {
                attr {
                    absolutePosition(top = 0f, left = 0f, bottom = 0f)
                    width(blockWidth)
                    backgroundColor(Color(0xCCFF7043L))
                }
            }
        }
    }

    // endregion

    // region 问题 5 —— Scroller / List 上的玻璃

    private fun issue5Section(): ViewBuilder {
        val ctx = this
        return {
            ctx.sectionTitle("问题 5 · Scroller / List + glassEffectIOS()")()
            ctx.card {
                ctx.caseTitle(
                    "5-1 Scroller + glassEffectIOS()　预期：无玻璃（Apple 规范不在内容层使用玻璃，" +
                        "滚动视图不支持），DEBUG 控制台输出 [Kuikly] 告警"
                )()
                Scroller {
                    attr {
                        size(ctx.barWidth, 100f)
                        borderRadius(16f)
                        if (ctx.glassSupported) {
                            glassEffectIOS()
                        }
                    }
                    ctx.listItems("Scroller")()
                }

                ctx.caseTitle("5-2 推荐写法：玻璃加在外层 View，List 放内部　预期：圆角玻璃面板，列表在其中滚动")()
                View {
                    attr {
                        size(ctx.barWidth, 150f)
                        borderRadius(16f)
                        if (ctx.glassSupported) {
                            glassEffectIOS()
                        } else {
                            backgroundColor(Color(0x80000000L))
                        }
                    }
                    List {
                        attr {
                            flex(1f)
                        }
                        ctx.listItems("List")()
                    }
                }
            }()
        }
    }

    private fun listItems(name: String): ViewBuilder = {
        for (i in 1..8) {
            Text {
                attr {
                    margin(left = 14f, top = 6f, bottom = 6f)
                    text("$name 列表项 $i")
                    fontSize(13f)
                    color(Color.WHITE)
                }
            }
        }
    }

    // endregion

    // region 通用片段（返回值均为 ViewBuilder，调用处必须再跟一个 ()）

    private fun backgroundLayer(): ViewBuilder = {
        View {
            attr {
                absolutePositionAllZero()
                backgroundLinearGradient(
                    Direction.TO_BOTTOM,
                    ColorStop(Color(0xFFFF6B9DL), 0f),
                    ColorStop(Color(0xFF4FC3F7L), 0.35f),
                    ColorStop(Color(0xFFB388FFL), 0.65f),
                    ColorStop(Color(0xFFFFD54FL), 1f)
                )
            }
        }
    }

    /** 统一尺寸的玻璃条；不支持玻璃的系统降级为半透明灰条 */
    private fun glassBar(style: ContainerAttr.() -> Unit): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    size(ctx.barWidth, BAR_HEIGHT)
                    style()
                    if (ctx.glassSupported) {
                        glassEffectIOS()
                    } else {
                        backgroundColor(Color(0x80000000L))
                    }
                }
            }
        }
    }

    private fun sectionTitle(title: String): ViewBuilder = {
        View {
            attr {
                margin(top = 18f, left = 12f, right = 12f, bottom = 6f)
                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                borderRadius(6f)
                backgroundColor(Color(0xCC000000L))
            }
            Text {
                attr {
                    text(title)
                    fontSize(15f)
                    fontWeightBold()
                    color(Color.WHITE)
                }
            }
        }
    }

    private fun card(content: ViewBuilder): ViewBuilder = {
        View {
            attr {
                margin(left = 12f, right = 12f, bottom = 10f)
                padding(left = 14f, right = 14f, top = 4f, bottom = 14f)
                borderRadius(10f)
                border(Border(1f, BorderStyle.SOLID, Color(0x66FFFFFFL)))
                // 低透明度：卡片过黑会让玻璃折射出的全是深色，质感不可见
                backgroundColor(Color(0x4D000000L))
                flexDirectionColumn()
            }
            content()
        }
    }

    /** case 标题：上方留出与上一个 case 的间距 */
    private fun caseTitle(value: String): ViewBuilder = {
        Text {
            attr {
                margin(top = 12f, bottom = 6f)
                text(value)
                fontSize(12f)
                lineHeight(18f)
                color(Color(0xFFE6E6E6L))
            }
        }
    }

    private fun bodyText(value: String): ViewBuilder = {
        Text {
            attr {
                margin(top = 10f)
                text(value)
                fontSize(12f)
                lineHeight(18f)
                color(Color(0xFFE6E6E6L))
            }
        }
    }

    /** 响应式状态文本：provider 在 attr 内求值，状态变化时自动刷新 */
    private fun stateText(provider: () -> String): ViewBuilder = {
        Text {
            attr {
                margin(bottom = 6f)
                text(provider())
                fontSize(12f)
                color(Color(0xFFFFE082L))
            }
        }
    }

    private fun warnText(value: String): ViewBuilder = {
        Text {
            attr {
                margin(top = 6f)
                text(value)
                fontSize(12f)
                lineHeight(18f)
                color(Color(0xFFFFC107L))
            }
        }
    }

    private fun gap(height: Float): ViewBuilder = {
        View {
            attr {
                height(height)
            }
        }
    }

    private fun actionButton(label: String, onClick: () -> Unit): ViewBuilder = {
        View {
            attr {
                alignSelfFlexStart()
                padding(left = 14f, right = 14f, top = 9f, bottom = 9f)
                borderRadius(6f)
                backgroundColor(Color(0xFF1677FFL))
            }
            event {
                click {
                    onClick()
                }
            }
            Text {
                attr {
                    text(label)
                    fontSize(13f)
                    color(Color.WHITE)
                }
            }
        }
    }

    // endregion
}
