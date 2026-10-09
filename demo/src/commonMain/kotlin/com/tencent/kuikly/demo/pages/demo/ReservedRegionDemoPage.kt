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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.pager.ReservedRegion
import com.tencent.kuikly.core.views.SafeArea
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.core.pager.ReservedRegionKind
import kotlin.math.max

/**
 * 把 [com.tencent.kuikly.core.pager.PageData.reservedRegions] 画成红色。
 * 页面自己也按这些区域避让：顶栏不会压在折缝或遮挡上。
 * 路由页输入 ReservedRegionDemo 打开。
 */
@Page("ReservedRegionDemo")
internal class ReservedRegionDemoPage : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFFF2F2F7))
            }
            // 顶栏跟着避让结果让开，不压在红色区域上
            SafeArea {
                NavBar {
                    attr {
                        title = "避让区域"
                    }
                }
            }
            View {
                attr {
                    flex(1f)
                    paddingLeft(ctx.leadingPad() + 16f)
                    paddingRight(ctx.trailingPad() + 16f)
                    paddingTop(ctx.topPad() + 16f)
                    paddingBottom(ctx.bottomPad() + 16f)
                }
                Text {
                    attr {
                        fontSize(15f)
                        color(Color(0xFF1C1C1E))
                        text(ctx.insetsSummary() + "\n" + ctx.regionSummary())
                    }
                }
            }
            vbind({ ctx.pageData.reservedRegions }) {
                ctx.pageData.reservedRegions.forEach { region ->
                    if (region.width > 0.5f && region.height > 0.5f) {
                        View {
                            attr {
                                absolutePosition(top = region.y, left = region.x)
                                width(region.width)
                                height(region.height)
                                backgroundColor(if (region.active) Color(0x99FF3B30) else Color(0x9930D158))
                                touchEnable(false)
                            }
                            Text {
                                attr {
                                    margin(8f)
                                    text(ctx.regionLabel(region))
                                    fontSize(12f)
                                    color(Color.WHITE)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun regionSummary(): String {
        val regions = pageData.reservedRegions
        if (regions.isEmpty()) {
            return "当前没有避让区域"
        }
        return regions.joinToString("\n") { regionLabel(it) } + "\n" + padSummary()
    }

    private fun insetsSummary(): String {
        val insets = pageData.safeAreaInsets
        return "上报 insets：上 ${insets.top.toInt()} 左 ${insets.left.toInt()} " +
            "右 ${insets.right.toInt()} 下 ${insets.bottom.toInt()}"
    }

    private fun regionLabel(region: ReservedRegion): String {
        val state = if (region.active) "生效" else "未生效"
        return "${region.kind.rawValue} $state  x=${region.x.toInt()} y=${region.y.toInt()} " +
            "${region.width.toInt()}x${region.height.toInt()}"
    }

    private fun padSummary(): String =
        "页面自身避让：左 ${leadingPad().toInt()} 上 ${topInset().toInt()} " +
            "右 ${trailingPad().toInt()} 下 ${bottomPad().toInt()}"

    // region 避让计算

    private fun viewWidth(): Float =
        if (pageData.pageViewWidth > 1f) pageData.pageViewWidth else pageData.deviceWidth

    private fun viewHeight(): Float =
        if (pageData.pageViewHeight > 1f) pageData.pageViewHeight else pageData.deviceHeight

    /** 竖向区域：折缝立着（左右分栏）或贴在侧边的遮挡。 */
    private fun ReservedRegion.isVertical(): Boolean = height >= width

    /** 顶部避让高度，含状态栏。 */
    private fun topInset(): Float {
        var value = max(pageData.safeAreaInsets.top, pageData.statusBarHeight)
        pageData.reservedRegions.forEach { region ->
            if (region.active && region.kind == ReservedRegionKind.OCCLUSION && region.y <= 0.5f) {
                value = max(value, region.y + region.height)
            }
        }
        return value
    }

    /** NavBar 自己已经垫了状态栏高度，这里只补差值。 */
    private fun topPad(): Float = max(0f, topInset() - pageData.statusBarHeight)

    private fun leadingPad(): Float {
        var value = pageData.safeAreaInsets.left
        pageData.reservedRegions.forEach { region ->
            if (!region.active || !region.isVertical()) return@forEach
            if (region.x <= 0.5f) { // 贴左边的遮挡
                value = max(value, region.x + region.width)
            }
        }
        return value
    }

    private fun trailingPad(): Float {
        var value = pageData.safeAreaInsets.right
        val width = viewWidth()
        // 系统已经按折缝算出左右可用宽度时直接用，区域数据异常也能让开。
        val division = pageData.reservedRegions.lastOrNull { region ->
            region.active && region.kind == ReservedRegionKind.DIVISION &&
                region.height >= region.width
        }
        if (division != null && division.x > 1f) {
            value = max(value, width - division.x)
        }
        pageData.reservedRegions.forEach { region ->
            if (!region.active || !region.isVertical()) return@forEach
            val rightEdge = region.x + region.width
            if (rightEdge >= width - 0.5f) { // 贴右边的遮挡
                value = max(value, (width - region.x).coerceAtLeast(0f))
            } else if (region.kind == ReservedRegionKind.DIVISION && region.x > 0.5f) {
                // 生效的中间竖向折缝：整页内容留在折缝左边那块
                value = max(value, (width - region.x).coerceAtLeast(0f))
            }
        }
        return value
    }

    private fun bottomPad(): Float {
        var value = pageData.safeAreaInsets.bottom
        val height = viewHeight()
        pageData.reservedRegions.forEach { region ->
            if (!region.active) return@forEach
            if (region.isVertical()) return@forEach
            val bottomEdge = region.y + region.height
            if (bottomEdge >= height - 0.5f) { // 贴底的遮挡
                value = max(value, (height - region.y).coerceAtLeast(0f))
            } else if (region.kind == ReservedRegionKind.DIVISION && region.y > 0.5f) {
                // 生效的中间横向折缝：内容留在折缝上面那块
                value = max(value, (height - region.y).coerceAtLeast(0f))
            }
        }
        return value
    }

    // endregion
}
