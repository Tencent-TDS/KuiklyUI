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
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.absolutePadding
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.ReservedRegionKind

@Page("FoldAvoidCompose")
internal class FoldAvoidComposePage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            val pager = LocalActivity.current
            ReservedRegionAvoidSample(
                onBack = {
                    pager.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                },
            )
        }
    }
}

/**
 * 100% 对齐 `FoldAvoidPage`（`@Page("FoldAvoid")`）：
 * - 顶部 `NavBar(title = "预留区域")` + 白底「避让前 / 避让后」chip + 区域说明
 * - 右上角浮动「完成」按钮避开右侧生效的 `OCCLUSION` 预留区域
 * - 生效的遮挡区域画成半透明红块（无设备数据时用与 `FoldAvoidPage` 完全相同的两块示意区域）
 */
@Composable
internal fun ReservedRegionAvoidSample(onBack: () -> Unit = {}) {
    rememberFoldMetricsTick()
    var avoid by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val pageWidth = configuration.pageViewWidth.coerceAtLeast(1f)
    val pageHeight = configuration.pageViewHeight.coerceAtLeast(1f)
    val pageData = configuration.pageData
    val safe = pageData.safeAreaInsets

    val reported = pageData.reservedRegions.filter { it.width > 1f && it.height > 1f }
    val regions = if (reported.isNotEmpty()) {
        reported.map { region ->
            val kind = when (region.kind) {
                ReservedRegionKind.OCCLUSION -> "遮挡"
                ReservedRegionKind.DIVISION -> "折缝"
                ReservedRegionKind.UNKNOWN -> "区域"
            }
            val state = if (region.active) "生效" else "未生效"
            ComposeAvoidGuideRegion(
                x = region.x,
                y = region.y,
                width = region.width,
                height = region.height,
                title = "$kind·$state",
                occlusion = region.kind == ReservedRegionKind.OCCLUSION,
                active = region.active,
            )
        }
    } else {
        val top = pageData.statusBarHeight.coerceAtLeast(28f)
        listOf(
            ComposeAvoidGuideRegion(pageWidth / 2f - 40f, 0f, 80f, top, "遮挡·示意", true, true),
            ComposeAvoidGuideRegion(pageWidth - 108f, pageHeight - 148f, 92f, 72f, "遮挡·示意", true, true),
        )
    }

    val buttonTop = pageData.statusBarHeight + 44f + 8f
    val target = regions.filter { it.occlusion && it.active }
        .maxByOrNull { it.x + it.width / 2f }
    val rawRight = if (target == null) {
        12f
    } else {
        val onRight = target.x + target.width / 2f >= pageWidth / 2f
        if (avoid && onRight) {
            (pageWidth - target.x + 12f).coerceAtLeast(12f)
        } else if (onRight) {
            (pageWidth - (target.x + target.width)).coerceAtLeast(0f)
        } else {
            12f
        }
    }
    val badgeRight = rawRight.coerceAtMost((pageWidth - 48f).coerceAtLeast(12f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ComposeDemoNavBar(title = "预留区域", onBack = onBack)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AvoidModeChip(text = "避让前", selected = !avoid) { avoid = false }
                AvoidModeChip(text = "避让后", selected = avoid) { avoid = true }
            }
            Text(
                text = if (pageData.reservedRegions.isEmpty()) {
                    "当前没有系统区域，红块是生效的遮挡，都是示意"
                } else {
                    "使用系统上报的 ${pageData.reservedRegions.size} 个区域，矩形已经包含预留边距"
                },
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    start = (safe.left + 16f).dp,
                    end = 16.dp,
                    bottom = (safe.bottom + 24f).dp,
                ),
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (avoid) "沿江慢慢走" else "按钮还压在遮挡上",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E),
                        )
                        ComposeFoldParagraph("从外白渡桥出发，先看见桥洞和来往的船。风贴着栏杆过来，楼群的灯还没全亮。")
                        ComposeFoldParagraph("往南走，海关大楼的钟和沿江的石库门挨在一起。这一段适合看，不适合赶。")
                        ComposeFoldParagraph("走到金陵东路时，天色通常已经暗下来。回头还能看见刚才经过的那一排窗户，灯光贴着江水。")
                        Column(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                        ) {
                            Text(
                                text = "这一段路",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1C1E),
                            )
                            ComposeAvoidFactLine("起点", "外白渡桥")
                            ComposeAvoidFactLine("终点", "金陵东路")
                            ComposeAvoidFactLine("时长", "步行大约 40 分钟")
                            ComposeAvoidFactLine("光线", "傍晚楼群会先亮起来")
                        }
                        ComposeFoldParagraph("同一条路，白天是车和人，傍晚只剩下风，还有远处很轻的钟声。")
                        ComposeFoldParagraph("「完成」只在遮挡生效时让到左边。没生效的摄像头不着色。")
                    }
                }
            }
        }

        regions.forEach { region ->
            if (region.occlusion && region.active) {
                OcclusionOverlayBox(
                    x = region.x,
                    y = region.y,
                    width = region.width,
                    height = region.height,
                    label = region.title,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .absolutePadding(top = buttonTop.dp, right = badgeRight.dp)
                .height(32.dp)
                .background(Color(0xFF007AFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "完成",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private class ComposeAvoidGuideRegion(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val title: String,
    val occlusion: Boolean,
    val active: Boolean,
)

@Composable
private fun ComposeAvoidFactLine(label: String, value: String) {
    Row(modifier = Modifier.padding(top = 10.dp)) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier.width(52.dp),
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = Color(0xFF1C1C1E),
        )
    }
}

@Composable
private fun AvoidModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .background(
                if (selected) Color(0xFF007AFF) else Color(0xFFE5E5EA),
                RoundedCornerShape(16.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else Color(0xFF1C1C1E),
        )
    }
}

@Composable
private fun OcclusionOverlayBox(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    label: String,
) {
    Box(
        modifier = Modifier
            .absolutePadding(left = x.dp, top = y.dp)
            .size(width.dp, height.dp)
            .background(Color(0x66FF3B30)),
    ) {
        if (height > 28f && width > 40f) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White,
                modifier = Modifier.padding(6.dp),
            )
        }
    }
}
