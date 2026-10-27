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
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule

private const val READING_FIGURE_BRIDGE =
    "https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg"
private const val READING_FIGURE_STREET =
    "https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/gjCqDSbr.png"

@Page("FoldReadingCompose")
internal class FoldReadingComposePage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            val pager = LocalActivity.current
            ResponsiveContentByPageWidth(
                onBack = {
                    pager.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                },
            )
        }
    }
}

/**
 * 100% 对齐 `FoldReadingPage`（`@Page("FoldReading")`）：
 * - 顶部使用与 `NavBar(title = "外滩")` 相同的白底导航栏
 * - 滚动容器内边距对齐 `foldContentPadding()`（左/右/下 = safeArea + 16，上 = 16）
 * - 阅读卡片最宽 560dp 并居中，宽屏（available >= 600）双图并排，窄屏上下排列，共 4 段正文
 */
@Composable
internal fun ResponsiveContentByPageWidth(onBack: () -> Unit = {}) {
    rememberFoldMetricsTick()
    val configuration = LocalConfiguration.current
    val pageWidth = configuration.pageViewWidth
    val pageData = configuration.pageData
    val insets = pageData.safeAreaInsets
    val availableWithPad = (pageWidth - insets.left - insets.right - 32f).coerceAtLeast(0f)
    val available = pageWidth - insets.left - insets.right
    val wide = available >= 600f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7)),
    ) {
        ComposeDemoNavBar(title = "外滩", onBack = onBack)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = (insets.left + 16f).dp,
                top = 16.dp,
                end = (insets.right + 16f).dp,
                bottom = (insets.bottom + 16f).dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Box(Modifier.width(minOf(availableWithPad, 560f).dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                    ) {
                        Text(
                            text = "傍晚的江面",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E),
                        )
                        Text(
                            text = "外白渡桥到金陵东路",
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        ComposeFoldParagraph("风从江面贴着栏杆过来。楼群的灯还没全亮，桥洞里先看见一艘很小的船。")
                        ComposeFoldParagraph("再往前，海关大楼的钟和沿江的石库门挨在一起。人不多的时候，这一段适合看，不适合赶。")
                        if (wide) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                ReadingFigureBox(
                                    src = READING_FIGURE_BRIDGE,
                                    caption = "外白渡桥",
                                    modifier = Modifier.weight(1f).height(148.dp),
                                )
                                ReadingFigureBox(
                                    src = READING_FIGURE_STREET,
                                    caption = "金陵东路",
                                    modifier = Modifier.weight(1f).height(148.dp),
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ReadingFigureBox(
                                    src = READING_FIGURE_BRIDGE,
                                    caption = "外白渡桥",
                                    modifier = Modifier.fillMaxWidth().height(168.dp),
                                )
                                ReadingFigureBox(
                                    src = READING_FIGURE_STREET,
                                    caption = "金陵东路",
                                    modifier = Modifier.fillMaxWidth().height(168.dp),
                                )
                            }
                        }
                        ComposeFoldParagraph("走到金陵东路，天色通常已经暗下来。回头还能看见刚才那一排窗户，灯光贴着江水，路就短了一截。")
                        ComposeFoldParagraph("同一条路，白天是车和人，傍晚只剩下风，还有远处很轻的钟声。")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingFigureBox(src: String, caption: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE5E5EA)),
    ) {
        Image(
            painter = rememberAsyncImagePainter(src),
            contentDescription = caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color(0x66000000))
                .padding(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 8.dp),
        ) {
            Text(caption, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}
