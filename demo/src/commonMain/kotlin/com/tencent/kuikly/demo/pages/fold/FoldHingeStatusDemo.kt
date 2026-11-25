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

package com.tencent.kuikly.demo.pages.fold

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.pager.HingeStatus
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.max

/**
 * 真机验证 `pageData.hingeStatus`（**KuiklyUI 2.30.0+** 起 Android 同步支持）：
 * - iOS 27.1+ 折叠机：UIKit `UIHingeInteraction`
 * - HarmonyOS 可折叠设备：`display.foldStatusChange` 映射后下发
 * - Android 折叠机：Jetpack `WindowLayoutInfo` / `FoldingFeature`（宿主需依赖 `androidx.window`）
 *
 * 折叠/展开/悬停设备上切换物理姿态，观察状态卡片与推荐布局是否同步更新。
 */
@Page("FoldHingeStatusDemo")
internal class FoldHingeStatusDemo : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(FoldPalette.bg)
            }
            NavBar {
                attr {
                    title = "hingeStatus"
                }
            }
            foldNote { ctx.sourceHint() }
            Scroller {
                attr {
                    flex(1f)
                }
                View {
                    attr {
                        foldContentPadding()
                    }
                    vbind({
                        "${ctx.pageData.hingeStatus}|${ctx.pageData.pageViewWidth}|" +
                            "${ctx.pageData.pageViewHeight}"
                    }) {
                        val hinge = ctx.pageData.hingeStatus
                        val width = ctx.contentWidth()
                        val layout = foldLayout(FoldShot.Auto, width, hinge)
                        statusCard(this, hinge, layout)
                        layoutPreview(this, layout)
                        metricRows(this, ctx, hinge, width, layout)
                        usageBlock(this)
                    }
                }
            }
        }
    }

    private fun contentWidth(): Float {
        val insets = pageData.safeAreaInsets
        return max(0f, pageData.pageViewWidth - insets.left - insets.right)
    }

    private fun sourceHint(): String {
        return when {
            pageData.isOhOs ->
                "HarmonyOS（2.31.0+）：Native 监听 display.foldStatusChange，映射 hingeStatus 后随 rootViewSizeDidChanged 更新。"
            pageData.isIOS ->
                "iOS（2.29.0+）：UIKit UIHingeInteraction；27.1+ 折叠设备才有非 UNKNOWN 数据。"
            pageData.isAndroid ->
                "Android（2.30.0+）：Window HALF_OPENED→2、FLAT→3；无折痕时折叠屏→1，否则→0。" +
                    "宿主需 implementation androidx.window:window 与 window-java。"
            else ->
                "当前平台无 hingeStatus Native 下发，保持 UNKNOWN(0)；可用宽度断点做响应式布局。"
        }
    }
}

private fun statusCard(host: ViewContainer<*, *>, hinge: HingeStatus, layout: FoldLayout) {
    with(host) {
        View {
            attr {
                padding(16f)
                borderRadius(16f)
                backgroundColor(statusTint(hinge))
            }
            foldHeading(foldHingeLabel(hinge))
            Text {
                attr {
                    marginTop(6f)
                    text("rawValue = ${hinge.rawValue} · 推荐布局：${foldLayoutLabel(layout)}")
                    fontSize(14f)
                    color(Color.WHITE)
                }
            }
            Text {
                attr {
                    marginTop(4f)
                    text(hingeBehaviorHint(hinge))
                    fontSize(13f)
                    lineHeight(18f)
                    color(Color(0xE6FFFFFF))
                }
            }
        }
    }
}

private fun layoutPreview(host: ViewContainer<*, *>, layout: FoldLayout) {
    with(host) {
        foldHeading("布局示意（Auto）")
        Text {
            attr {
                marginTop(6f)
                marginBottom(10f)
                text("半开 → 悬停；宽度 ≥ ${FOLD_BREAKPOINT.toInt()} → 展开；否则折叠。")
                fontSize(13f)
                color(FoldPalette.secondary)
            }
        }
        when (layout) {
            FoldLayout.Hover -> {
                foldSwatchBlock(2, "悬停 · 上下分屏", 120f)
            }
            FoldLayout.Wide -> {
                View {
                    attr {
                        flexDirectionRow()
                    }
                    View {
                        attr {
                            flex(1f)
                            marginRight(6f)
                        }
                        foldSwatchBlock(1, "主内容", 120f)
                    }
                    View {
                        attr {
                            flex(1f)
                            marginLeft(6f)
                        }
                        foldSwatchBlock(0, "副栏", 120f)
                    }
                }
            }
            FoldLayout.Narrow -> {
                foldSwatchBlock(3, "单栏 · 外屏", 120f)
            }
        }
    }
}

private fun metricRows(
    host: ViewContainer<*, *>,
    page: FoldHingeStatusDemo,
    hinge: HingeStatus,
    width: Float,
    layout: FoldLayout,
) {
    with(host) {
        View {
            attr {
                marginTop(20f)
                padding(16f)
                borderRadius(16f)
                backgroundColor(FoldPalette.card)
            }
            foldHeading("PagerData 快照")
            metricLine("hingeStatus", foldHingeLabel(hinge))
            metricLine("pageViewWidth", "${page.pageData.pageViewWidth.toInt()} pt")
            metricLine("contentWidth", "${width.toInt()} pt")
            metricLine("foldLayout(Auto)", foldLayoutLabel(layout))
            metricLine(
                "halfFolded",
                if (hinge == HingeStatus.PARTIALLY_OPEN) "true" else "false",
            )
            metricLine("reservedRegions", "${page.pageData.reservedRegions.size} 项")
        }
    }
}

private fun ViewContainer<*, *>.metricLine(title: String, value: String) {
    View {
        attr {
            flexDirectionRow()
            marginTop(8f)
        }
        Text {
            attr {
                width(140f)
                text(title)
                fontSize(13f)
                color(FoldPalette.tertiary)
            }
        }
        Text {
            attr {
                flex(1f)
                text(value)
                fontSize(13f)
                fontWeightSemiBold()
                color(FoldPalette.ink)
            }
        }
    }
}

private fun usageBlock(host: ViewContainer<*, *>) {
    with(host) {
        View {
            attr {
                marginTop(16f)
                marginBottom(24f)
                padding(16f)
                borderRadius(16f)
                backgroundColor(FoldPalette.card)
            }
            foldHeading("业务用法")
            foldParagraph(
                "val half = pageData.hingeStatus == HingeStatus.PARTIALLY_OPEN",
            )
            foldParagraph(
                "val layout = foldLayout(FoldShot.Auto, contentWidth, pageData.hingeStatus)",
            )
            foldNote {
                "折叠时窗口尺寸也会变，hingeStatus 与宽高会一起通过 rootViewSizeDidChanged 刷新，无需手动轮询。"
            }
        }
    }
}

private fun statusTint(hinge: HingeStatus): Color {
    return when (hinge) {
        HingeStatus.UNKNOWN -> Color(0xFF8E8E93)
        HingeStatus.CLOSED -> Color(0xFF636366)
        HingeStatus.PARTIALLY_OPEN -> Color(0xFFFF9F0A)
        HingeStatus.FULLY_OPEN -> Color(0xFF30D158)
    }
}

private fun hingeBehaviorHint(hinge: HingeStatus): String {
    return when (hinge) {
        HingeStatus.UNKNOWN -> "当前无铰链数据；可折叠真机上展开/折叠/悬停后应变为非 0。"
        HingeStatus.CLOSED -> "合上 / 外屏窄态：适合单栏、全屏详情。"
        HingeStatus.PARTIALLY_OPEN -> "半开 / 悬停：内容可分居折缝上下或左右。"
        HingeStatus.FULLY_OPEN -> "完全展开：适合双栏、宽屏信息流。"
    }
}
