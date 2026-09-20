/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * iOS 边框隐式动画复现页（border + 变更 frame）
 *
 * 现象：同一个 View 实例声明 border 后，只要改变它的 frame（size），iOS 上背景/内容与边框不同帧跳变，
 *      边框会带着一段约 0.25s 的隐式动画"滑"到新位置；胶囊形态下上下边框的粗细会在过程中不一致，
 *      看起来像边框在蠕动（业务截图即该现象）。
 *
 * 框架侧可疑点（core-render-ios/Extension/Category/UIView+CSS.m）：
 * 1. CSSBorderLayer.layoutSublayers(:1576) 中同步 frame(:1593)、重设 path(:1662)、
 *    lineWidth(:1634) 全部没有 CATransaction 保护；同文件的 CSSGradientLayer.layoutSublayers(:1439)
 *    与 CSSBorderLayer 的 macOS 分支(:1582) 都有 [CATransaction setDisableActions:YES]。
 * 2. CSSBorderLayer 是手动 addSublayer 到 view.layer 上的独立 CAShapeLayer(:447)，
 *    不享受 UIView backing layer（backing layer 的隐式动画被 UIKit 屏蔽）的待遇。
 * 3. setCss_frame(:801) 只在「已有 animationKeys 且 transform 非 identity」分支里用
 *    performWithoutAnimation / setDisableActions，普通路径(:834) 直接 setFrameBlock()。
 *
 * 观测方法（本页 4 个 case 共用同一个 expanded 状态，方便一次切换同时对比）：
 * - Case 1 圆角矩形：看边框是否与白色背景同帧跳变（正常应为瞬变，异常为 0.25s 缓动滑移）。
 * - Case 2 胶囊：圆角随高度变化，最接近业务截图，重点看上下边框粗细中途是否不对称。
 * - Case 3 业务同款：胶囊 + 小幅纵向变化 + 1pt 级边框，重点看上下粗细蠕动。
 * - Case 4 对照组：不使用 border 属性，用嵌套 View 模拟描边，应始终与背景同帧跳变。
 *
 * 说明：本页只覆盖「无动画直接改 frame」的隐式动画场景；业务声明 animate 的显式动画场景
 *      属于另一条链路，暂不在本页验证。
 */
@Page("BugReproBorderImplicitAnimationPage")
internal class BugReproBorderImplicitAnimationPage : BasePager() {

    /** false = 收起态，true = 展开态 */
    private var expanded by observable(false)

    /** 自动循环切换，便于肉眼/录屏观察；进入页面即开启，可用「重置」停止 */
    private var autoLoop by observable(true)

    /** 循环是否真的切换过至少一次（用于判断 pageDidAppear 是否需要补调度） */
    private var loopToggled = false

    override fun created() {
        super.created()
        scheduleAutoLoop()
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        // Android 上 created 阶段 setTimeout 尚未生效，页面出现后若循环还没跑起来则补一次调度
        if (autoLoop && !loopToggled) {
            scheduleAutoLoop()
        }
    }

    override fun pageDidDisappear() {
        super.pageDidDisappear()
        autoLoop = false
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(Color(0xFFF2F3F5L))
            }
            NavBar {
                attr {
                    title = "iOS Border 隐式动画复现"
                }
            }
            List {
                attr {
                    flex(1f)
                }

                View {
                    attr {
                        padding(all = 12f)
                        flexDirectionColumn()
                    }
                    title("现象与判定标准")
                    hint("同一个 View 声明 border 后只改 size，背景色块会立刻跳变，而边框会带约 0.25s 缓动滑过去。")
                    hint("判定：切换尺寸时边框与背景「不同帧到位」= 异常；上下边框粗细中途不一致 = 异常。")
                    hint("Android / OHOS 无此现象，可与 iOS 直接对比。")
                }

                View {
                    attr {
                        margin(left = 12f, right = 12f)
                        padding(all = 10f)
                        borderRadius(8f)
                        backgroundColor(Color.WHITE)
                        flexDirectionColumn()
                    }
                    title("操作")
                    hint("当前状态：${if (ctx.expanded) "展开态" else "收起态"}    自动循环：${if (ctx.autoLoop) "开" else "关"}")
                    View {
                        attr {
                            marginTop(8f)
                            flexDirectionRow()
                        }
                        actionButton("切换尺寸", Color(0xFF0F62FEL)) {
                            ctx.expanded = !ctx.expanded
                        }
                        actionButton(if (ctx.autoLoop) "停止循环" else "自动循环", Color(0xFF2E7D32L)) {
                            ctx.autoLoop = !ctx.autoLoop
                            ctx.scheduleAutoLoop()
                        }
                        actionButton("重置", Color(0xFF757575L)) {
                            ctx.autoLoop = false
                            ctx.expanded = false
                        }
                    }
                }

                caseCard(
                    index = "Case 1",
                    desc = "border + 圆角矩形，无动画直接改 size（borderRadius 固定 8f，圆角不随尺寸变，排除圆角干扰）"
                ) {
                    View {
                        attr {
                            size(if (ctx.expanded) 260f else 160f, if (ctx.expanded) 56f else 32f)
                            borderRadius(8f)
                            border(Border(1f, BorderStyle.SOLID, Color(0xFF0F62FEL)))
                            backgroundColor(Color.WHITE)
                            flexDirectionRow()
                            alignItemsCenter()
                            padding(left = 10f, right = 10f)
                        }
                        label(if (ctx.expanded) "展开 260x56" else "收起 160x32")
                    }
                }

                caseCard(
                    index = "Case 2",
                    desc = "border + 胶囊（radius = 高度/2，随 size 一起变），最接近业务截图，重点看上下边框粗细"
                ) {
                    View {
                        attr {
                            size(if (ctx.expanded) 300f else 130f, if (ctx.expanded) 64f else 36f)
                            borderRadius(if (ctx.expanded) 32f else 18f)
                            border(Border(1f, BorderStyle.SOLID, Color(0xFF1F1F1FL)))
                            backgroundColor(Color(0xFFFFFFFFL))
                            flexDirectionRow()
                            alignItemsCenter()
                            padding(left = 12f, right = 12f)
                        }
                        label(if (ctx.expanded) "展开 300x64" else "收起 130x36")
                    }
                }

                caseCard(
                    index = "Case 3",
                    desc = "业务同款：胶囊 + 小幅纵向变化（240x36 → 268x46）+ 2f 边框（业务为 1pt，这里加粗只为肉眼可辨），" +
                        "重点看上/下边框粗细在过程中的差异，即业务截图那种“蠕动”观感"
                ) {
                    View {
                        attr {
                            size(if (ctx.expanded) 268f else 240f, if (ctx.expanded) 46f else 36f)
                            borderRadius(if (ctx.expanded) 23f else 18f)
                            border(Border(2f, BorderStyle.SOLID, Color(0xFFD32F2FL)))
                            backgroundColor(Color(0xFFFFFFFFL))
                            flexDirectionRow()
                            alignItemsCenter()
                            padding(left = 12f, right = 12f)
                        }
                        label(if (ctx.expanded) "展开 268x46" else "收起 240x36")
                    }
                }

                caseCard(
                    index = "Case 4",
                    desc = "对照组：不使用 border 属性，外层 View 当描边底色 + 内层 View 偏移模拟边框，应与背景同帧跳变"
                ) {
                    View {
                        attr {
                            size(if (ctx.expanded) 260f else 160f, if (ctx.expanded) 56f else 32f)
                            borderRadius(8f)
                            backgroundColor(Color(0xFF0F62FEL))
                        }
                        View {
                            attr {
                                absolutePositionAllZero()
                                margin(all = 1f)
                                borderRadius(7f)
                                backgroundColor(Color.WHITE)
                                flexDirectionRow()
                                alignItemsCenter()
                                padding(left = 9f, right = 9f)
                            }
                            label(if (ctx.expanded) "展开对照" else "收起对照")
                        }
                    }
                }

                View {
                    attr {
                        padding(all = 12f)
                    }
                    hint("提示：若 Case 1/2/3 边框和背景不同帧到位、而 Case 4 正常，即为框架 border layer 隐式动画问题。")
                }
            }
        }
    }

    /** 自动循环：每 700ms 切换一次尺寸，便于肉眼/录屏观察 */
    private fun scheduleAutoLoop() {
        if (!autoLoop) {
            return
        }
        setTimeout(700) {
            if (!autoLoop) {
                return@setTimeout
            }
            loopToggled = true
            expanded = !expanded
            scheduleAutoLoop()
        }
    }
}

private fun ViewContainer<*, *>.title(text: String) {
    Text {
        attr {
            fontSize(14f)
            fontWeight600()
            color(Color(0xFF1F1F1FL))
            text(text)
        }
    }
}

private fun ViewContainer<*, *>.hint(text: String) {
    Text {
        attr {
            marginTop(4f)
            fontSize(12f)
            color(Color(0xFF666666L))
            text(text)
        }
    }
}

private fun ViewContainer<*, *>.label(text: String) {
    Text {
        attr {
            fontSize(12f)
            color(Color(0xFF333333L))
            text(text)
        }
    }
}

private fun ViewContainer<*, *>.actionButton(text: String, bgColor: Color, onClick: () -> Unit) {
    View {
        attr {
            margin(right = 8f)
            padding(left = 12f, right = 12f, top = 6f, bottom = 6f)
            borderRadius(4f)
            backgroundColor(bgColor)
        }
        Text {
            attr {
                fontSize(12f)
                color(Color.WHITE)
                text(text)
            }
        }
        event {
            click {
                onClick()
            }
        }
    }
}

private fun ViewContainer<*, *>.caseCard(index: String, desc: String, content: ViewContainer<*, *>.() -> Unit) {
    View {
        attr {
            margin(left = 12f, right = 12f, bottom = 12f)
            padding(all = 10f)
            borderRadius(8f)
            backgroundColor(Color.WHITE)
            flexDirectionColumn()
        }
        title(index)
        hint(desc)
        View {
            attr {
                marginTop(10f)
                padding(all = 10f)
                backgroundColor(Color(0xFFEDEFF2L))
                justifyContentCenter()
                alignItemsCenter()
            }
            content()
        }
    }
}
