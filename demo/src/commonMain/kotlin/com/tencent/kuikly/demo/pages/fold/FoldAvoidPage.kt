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
import com.tencent.kuikly.core.pager.ReservedRegionKind
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
internal class GuideRegion(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val title: String,
    val occlusion: Boolean,
    val active: Boolean,
)

@Page("FoldAvoid")
internal class FoldAvoidPage : BasePager() {
    var avoid by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(FoldPalette.bg)
            }
            NavBar {
                attr {
                    title = "预留区域"
                }
            }
            foldChips(listOf(false, true), { if (it) "避让后" else "避让前" }, { ctx.avoid }) {
                ctx.avoid = it
            }
            foldNote { ctx.regionNote() }
            vbind({
                "${ctx.avoid}|${ctx.pageData.pageViewWidth}|${ctx.pageData.pageViewHeight}|${ctx.pageData.reservedRegions}"
            }) {
                View {
                    attr {
                        flex(1f)
                    }
                    ctx.article(this)
                }
            }
            vbind({
                "${ctx.pageData.pageViewWidth}|${ctx.pageData.pageViewHeight}|${ctx.pageData.reservedRegions}"
            }) {
                ctx.regions().forEach { region ->
                    if (!region.occlusion || !region.active) {
                        return@forEach
                    }
                    View {
                        attr {
                            absolutePosition(top = region.y, left = region.x)
                            width(region.width)
                            height(region.height)
                            backgroundColor(Color(0x66FF3B30))
                            touchEnable(false)
                        }
                        if (region.height > 28f && region.width > 40f) {
                            Text {
                                attr {
                                    margin(6f)
                                    text(region.title)
                                    fontSize(11f)
                                    color(Color.WHITE)
                                }
                            }
                        }
                    }
                }
            }
            View {
                attr {
                    val pos = ctx.buttonPos()
                    absolutePosition(top = pos.first, right = pos.second)
                    height(32f)
                    paddingLeft(12f)
                    paddingRight(12f)
                    borderRadius(16f)
                    allCenter()
                    backgroundColor(FoldPalette.accent)
                }
                Text {
                    attr {
                        text("完成")
                        fontSize(13f)
                        fontWeightSemiBold()
                        color(Color.WHITE)
                    }
                }
            }
        }
    }

    private fun regionNote(): String {
        val count = pageData.reservedRegions.size
        val source = if (count == 0) {
            "当前没有系统区域，红块是生效的遮挡，都是示意"
        } else {
            "使用系统上报的 $count 个区域，矩形已经包含预留边距"
        }
        return source
    }

    internal fun regions(): List<GuideRegion> {
        val reported = pageData.reservedRegions.filter { it.width > 1f && it.height > 1f }
        if (reported.isNotEmpty()) {
            return reported.map { region ->
                val kind = when (region.kind) {
                    ReservedRegionKind.OCCLUSION -> "遮挡"
                    ReservedRegionKind.DIVISION -> "折缝"
                    ReservedRegionKind.UNKNOWN -> "区域"
                }
                val state = if (region.active) "生效" else "未生效"
                GuideRegion(
                    region.x,
                    region.y,
                    region.width,
                    region.height,
                    "$kind·$state",
                    region.kind == ReservedRegionKind.OCCLUSION,
                    region.active,
                )
            }
        }
        val width = pageData.pageViewWidth.coerceAtLeast(1f)
        val height = pageData.pageViewHeight.coerceAtLeast(1f)
        val top = pageData.statusBarHeight.coerceAtLeast(28f)
        return listOf(
            GuideRegion(width / 2f - 40f, 0f, 80f, top, "遮挡·示意", true, true),
            GuideRegion(width - 108f, height - 148f, 92f, 72f, "遮挡·示意", true, true),
        )
    }

    /**
     * 返回按钮的 top 和 right。
     * 高度、字号和「避让前」一致，并排在同一行。
     * 避让后只把这颗按钮的 right 让开遮挡，下面的正文不加这块右侧留白。
     */
    private fun buttonPos(): Pair<Float, Float> {
        val pageWidth = pageData.pageViewWidth.coerceAtLeast(1f)
        val top = pageData.statusBarHeight + 44f + 8f
        val target = regions().filter { it.occlusion && it.active }
            .maxByOrNull { it.x + it.width / 2f }
        if (target == null) {
            return top to 12f
        }
        val onRight = target.x + target.width / 2f >= pageWidth / 2f
        val right = if (avoid && onRight) {
            (pageWidth - target.x + 12f).coerceAtLeast(12f)
        } else if (onRight) {
            (pageWidth - (target.x + target.width)).coerceAtLeast(0f)
        } else {
            12f
        }
        return top to right.coerceAtMost((pageWidth - 48f).coerceAtLeast(12f))
    }
}

private fun FoldAvoidPage.article(host: ViewContainer<*, *>) {
    val avoiding = avoid
    host.Scroller {
        attr {
            flex(1f)
        }
        View {
            attr {
                val insets = pagerData.safeAreaInsets
                paddingTop(16f)
                paddingLeft(insets.left + 16f)
                paddingRight(16f)
                paddingBottom(insets.bottom + 24f)
            }
            foldHeading(if (avoiding) "沿江慢慢走" else "按钮还压在遮挡上")
            foldParagraph("从外白渡桥出发，先看见桥洞和来往的船。风贴着栏杆过来，楼群的灯还没全亮。")
            foldParagraph("往南走，海关大楼的钟和沿江的石库门挨在一起。这一段适合看，不适合赶。")
            foldParagraph("走到金陵东路时，天色通常已经暗下来。回头还能看见刚才经过的那一排窗户，灯光贴着江水。")
            View {
                attr {
                    marginTop(16f)
                    padding(16f)
                    borderRadius(16f)
                    backgroundColor(FoldPalette.card)
                }
                Text {
                    attr {
                        text("这一段路")
                        fontSize(17f)
                        fontWeightSemiBold()
                        color(FoldPalette.ink)
                    }
                }
                factLine(this, "起点", "外白渡桥")
                factLine(this, "终点", "金陵东路")
                factLine(this, "时长", "步行大约 40 分钟")
                factLine(this, "光线", "傍晚楼群会先亮起来")
            }
            foldParagraph("同一条路，白天是车和人，傍晚只剩下风，还有远处很轻的钟声。")
            foldParagraph("「完成」只在遮挡生效时让到左边。没生效的摄像头不着色。")
        }
    }
}

private fun factLine(host: ViewContainer<*, *>, label: String, value: String) {
    host.View {
        attr {
            marginTop(10f)
            flexDirectionRow()
        }
        Text {
            attr {
                width(52f)
                text(label)
                fontSize(15f)
                color(FoldPalette.tertiary)
            }
        }
        Text {
            attr {
                flex(1f)
                text(value)
                fontSize(15f)
                color(FoldPalette.ink)
            }
        }
    }
}
