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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.pager.HingeStatus
import com.tencent.kuikly.core.pager.PageData
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.ReservedRegionKind
import kotlin.math.max

@Page("FoldFeedCompose")
internal class FoldFeedComposePage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            val pager = LocalActivity.current
            val pageData = LocalConfiguration.current.pageData
            val flag = pageData.params.optString("iosUIKitNavigationChrome")
            val useSystemBars = pageData.isIOS && (flag.isEmpty() || flag == "1" || flag == "true")
            Box(Modifier.fillMaxSize()) {
                WaterfallSample(useSystemBars = useSystemBars)
                if (useSystemBars) {
                    SystemNavigationBar(
                        title = "城里走走",
                        onBack = {
                            pager.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                        },
                    )
                } else {
                    ComposeDemoNavBar(
                        title = "城里走走",
                        onBack = {
                            pager.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                        },
                    )
                }
            }
        }
    }
}

private class ComposeFeedItem(
    val id: Int,
    val title: String,
    val detail: String,
    val imageHeight: Float,
    val swatch: Color,
)

private fun composeFeedSwatch(index: Int): Color =
    when (index % 4) {
        0 -> Color(0xFF64D2FF)
        1 -> Color(0xFF30D158)
        2 -> Color(0xFFFF9F0A)
        else -> Color(0xFFBF5AF2)
    }

private val feed = listOf(
    Triple("外滩", "江风贴着栏杆，楼群的灯先亮起来。", 188f),
    Triple("豫园", "转弯很多。", 112f),
    Triple("武康路", "梧桐和骑楼连成一条，下午的光正好停在墙面上。", 214f),
    Triple("苏州河", "桥洞一过，水面就宽了。", 136f),
    Triple("静安", "路口人多，往里走就安静。", 96f),
    Triple("徐汇", "小店沿着街一间接一间。", 168f),
    Triple("南京路", "夜里灯很密。", 124f),
    Triple("思南路", "法国梧桐把路盖住一半，适合慢慢走，不用赶。", 196f),
    Triple("衡山路", "咖啡馆的灯比路灯先暖。", 108f),
    Triple("田子坊", "巷子窄，招牌一层叠一层。", 176f),
    Triple("陆家嘴", "江对岸的楼比手边的路更远。", 142f),
    Triple("多伦路", "旧房子和新招牌挤在一起。", 118f),
).mapIndexed { index, (title, detail, height) ->
    ComposeFeedItem(
        id = index,
        title = title,
        detail = detail,
        imageHeight = height,
        swatch = composeFeedSwatch(index),
    )
}

/**
 * 对齐 `FoldFeedPage`（对应 DSL 的 `WaterfallList`）：
 * - `pageWidth >= 600f` 时 4 列，否则 2 列
 * - 卡片不写死宽度，由 `LazyVerticalStaggeredGrid` 按列宽分配
 */
@Composable
internal fun WaterfallSample(useSystemBars: Boolean) {
    rememberFeedReservedRegionsTick()
    val configuration = LocalConfiguration.current
    val pageWidth = configuration.pageViewWidth
    val pageData = configuration.pageData
    val safe = pageData.safeAreaInsets
    val rightInset = feedComposeRightInset(pageData)
    val horizontalStart = if (pageData.hingeStatus == HingeStatus.UNKNOWN) {
        safe.left + 16f
    } else {
        16f
    }
    // 系统 UINavigationBar 叠在内容上：与 FoldAppNav 一致，只留列表与栏之间的间距，勿叠算 safe.top + 44。
    val contentTop = if (useSystemBars) {
        24f
    } else {
        max(safe.top, pageData.statusBarHeight) + 44f + 24f
    }
    val availableWidth = (pageWidth - horizontalStart - (rightInset + 16f)).coerceAtLeast(0f)
    val columnCount = feedColumnCount(availableWidth)

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columnCount),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7)),
        contentPadding = PaddingValues(
            start = horizontalStart.dp,
            top = contentTop.dp,
            end = (rightInset + 16f).dp,
            bottom = (safe.bottom + 16f).dp,
        ),
        verticalItemSpacing = 10.dp,                          // 对应 lineSpacing
        horizontalArrangement = Arrangement.spacedBy(10.dp),  // 对应 itemSpacing
    ) {
        items(feed, key = { it.id }) { item ->
            FeedCard(item, Modifier.fillMaxWidth())           // 卡片不写死宽度，由网格按列宽分配
        }
    }
}

@Composable
private fun FeedCard(item: ComposeFeedItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(item.imageHeight.dp)
                .background(item.swatch, RoundedCornerShape(8.dp)),
        )
        Text(
            text = item.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E),
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = item.detail,
            fontSize = 13.sp,
            color = Color(0xFF636366),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 仅订阅 `reservedRegions`，避免 safeArea 首帧变化导致列表顶距跳变。 */
@Composable
private fun rememberFeedReservedRegionsTick(): Int {
    var tick by remember { mutableStateOf(0) }
    MakeKuiklyComposeNode<DivView>(
        factory = { DivView() },
        modifier = Modifier.size(0.dp),
        viewInit = {
            attr {
                pagerData.reservedRegions
                tick++
            }
        },
    )
    return tick
}

private fun feedComposeRightInset(data: PageData): Float {
    val width = data.pageViewWidth
    val rightOcclusion = data.reservedRegions
        .filter { it.active && it.kind == ReservedRegionKind.OCCLUSION && it.x + it.width / 2f >= width / 2f }
        .maxOfOrNull { (width - it.x).coerceAtLeast(0f) } ?: 0f
    return max(data.safeAreaInsets.right, rightOcclusion)
}

private fun feedColumnCount(availableWidth: Float): Int =
    (availableWidth / 170f).toInt().coerceIn(2, 4)
