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
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule

@Page("FoldAppNavCompose")
internal class FoldAppNavComposePage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            val pager = LocalActivity.current
            val pageData = LocalConfiguration.current.pageData
            val flag = pageData.params.optString("iosUIKitNavigationChrome")
            val useSystemBars = pageData.isIOS && (flag.isEmpty() || flag == "1" || flag == "true")
            FoldAppNavComposeContent(
                useSystemBars = useSystemBars,
                onBack = { pager.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage() },
            )
        }
    }
}

/**
 * 100% 对齐 KuiklyDSL 的 `FoldAppNavPage`（`@Page("FoldAppNav")`）。
 */
@Composable
internal fun FoldAppNavComposeContent(
    useSystemBars: Boolean,
    onBack: () -> Unit,
) {
    var note by remember { mutableStateOf("还没点过分享") }
    // 与 FoldAppNavPage 一致：系统栏叠在内容上方时只留 20pt 顶距，勿再加 statusBar+44（会与 safe area 叠算）。
    val scrollInsetTop = if (useSystemBars) 20f else 0f

    AdaptiveNavScaffold(
        title = "自定义Nav",
        useSystemBars = useSystemBars,
        onBack = onBack,
        actions = listOf(
            ComposeNavAction("分享", "square.and.arrow.up") {
                note = "刚把这段路分享出去"
            },
        ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = scrollInsetTop.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "傍晚的江面",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E),
                    )
                    Text(
                        text = "外白渡桥到金陵东路，慢慢走就好",
                        fontSize = 14.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    ComposeFoldParagraph("风从江面贴着栏杆过来。楼群的灯还没全亮，桥洞里先看见一艘很小的船。")
                    ComposeFoldParagraph("再往前，海关大楼的钟和沿江的石库门挨在一起。人不多的时候，这一段适合看，不适合赶。")
                    ComposeFoldParagraph("走到金陵东路，天色通常已经暗下来。回头还能看见刚才那一排窗户，灯光贴着江水，路就短了一截。")
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ComposeFactLine("起点", "外白渡桥")
                        ComposeFactLine("终点", "金陵东路")
                        ComposeFactLine("时长", "四十分钟")
                        ComposeFactLine("光线", "灯先于天黑")
                    }
                    Text(
                        text = note,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}
