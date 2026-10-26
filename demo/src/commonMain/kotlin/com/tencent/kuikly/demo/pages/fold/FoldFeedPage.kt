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
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.PageData
import com.tencent.kuikly.core.pager.ReservedRegionKind
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.WaterfallList
import com.tencent.kuikly.core.views.ios.UIBarAxis
import com.tencent.kuikly.core.views.ios.UIBarButtonItemPlacement
import com.tencent.kuikly.core.views.ios.UILargeTitleDisplayMode
import com.tencent.kuikly.core.views.ios.UINavigationBar
import com.tencent.kuikly.demo.pages.base.BasePager
import kotlin.math.max

private class FeedCard(
    val title: String,
    val detail: String,
    val imageHeight: Float,
)

private val feedCards = listOf(
    FeedCard("外滩", "江风贴着栏杆，楼群的灯先亮起来。", 188f),
    FeedCard("豫园", "转弯很多。", 112f),
    FeedCard("武康路", "梧桐和骑楼连成一条，下午的光正好停在墙面上。", 214f),
    FeedCard("苏州河", "桥洞一过，水面就宽了。", 136f),
    FeedCard("静安", "路口人多，往里走就安静。", 96f),
    FeedCard("徐汇", "小店沿着街一间接一间。", 168f),
    FeedCard("南京路", "夜里灯很密。", 124f),
    FeedCard("思南路", "法国梧桐把路盖住一半，适合慢慢走，不用赶。", 196f),
    FeedCard("衡山路", "咖啡馆的灯比路灯先暖。", 108f),
    FeedCard("田子坊", "巷子窄，招牌一层叠一层。", 176f),
    FeedCard("陆家嘴", "江对岸的楼比手边的路更远。", 142f),
    FeedCard("多伦路", "旧房子和新招牌挤在一起。", 118f),
)

/** 大约每 170 一列。手机通常 2 列，展开变宽后自动到 3 列、4 列。 */
private fun feedColumnCount(availableWidth: Float): Int {
    return (availableWidth / 170f).toInt().coerceIn(2, 4)
}

private fun feedRightInset(data: PageData): Float {
    val width = data.pageViewWidth
    val rightOcclusion = data.reservedRegions
        .filter { it.active && it.kind == ReservedRegionKind.OCCLUSION && it.x + it.width / 2f >= width / 2f }
        .maxOfOrNull { (width - it.x).coerceAtLeast(0f) } ?: 0f
    return max(data.safeAreaInsets.right, rightOcclusion)
}

/**
 * 瀑布流不写死卡片宽度。屏幕变宽时按当前宽度增加列数。
 */
@Page("FoldFeed")
internal class FoldFeedPage : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(FoldPalette.bg)
            }
            vbind({
                val insets = ctx.pageData.safeAreaInsets
                val rightInset = feedRightInset(ctx.pageData)
                val available = ctx.pageData.pageViewWidth - insets.left - rightInset - 32f
                "${feedColumnCount(available)}|${available.toInt()}|${rightInset.toInt()}"
            }) {
                WaterfallList {
                    attr {
                        flex(1f)
                        val insets = ctx.pageData.safeAreaInsets
                        val rightInset = feedRightInset(ctx.pageData)
                        val width = ctx.pageData.pageViewWidth
                        val available = (width - insets.left - rightInset - 32f).coerceAtLeast(0f)
                        listWidth(width)
                        contentPadding(
                            top = ctx.pageData.statusBarHeight + 44f + 24f,
                            left = insets.left + 16f,
                            bottom = insets.bottom + 16f,
                            right = rightInset + 16f,
                        )
                        columnCount(feedColumnCount(available))
                        itemSpacing(10f)
                        lineSpacing(10f)
                    }
                    feedCards.forEachIndexed { index, card ->
                        View {
                            attr {
                                borderRadius(12f)
                                backgroundColor(FoldPalette.card)
                                padding(10f)
                            }
                            View {
                                attr {
                                    height(card.imageHeight)
                                    borderRadius(8f)
                                    backgroundColor(foldSwatch(index))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(8f)
                                    text(card.title)
                                    fontSize(15f)
                                    fontWeightSemiBold()
                                    color(FoldPalette.ink)
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    text(card.detail)
                                    fontSize(13f)
                                    lineHeight(18f)
                                    color(FoldPalette.secondary)
                                }
                            }
                        }
                    }
                }
            }
            UINavigationBar {
                attr {
                    tintColor(Color.BLACK)
                }
                title {
                    text {
                        attr {
                            title("城里走走")
                            largeTitle(UILargeTitleDisplayMode.Never)
                        }
                    }
                }
                items {
                    navbarItems {
                        navbarItem {
                            attr {
                                symbol("chevron.backward")
                                placement(UIBarButtonItemPlacement.Leading)
                                index(0)
                                axis(UIBarAxis.VerticalPreferred)
                            }
                            event {
                                click {
                                    ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                                        .closePage()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
