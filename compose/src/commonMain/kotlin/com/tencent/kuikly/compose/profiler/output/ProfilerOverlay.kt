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

package com.tencent.kuikly.compose.profiler.output

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectDragGestures
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.BoxWithConstraints
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ExperimentalLayoutApi
import com.tencent.kuikly.compose.foundation.layout.FlowRow
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.wrapContentWidth
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.profiler.RecompositionProfiler
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val FAB_SIZE_DP = 52
private const val FAB_MARGIN_DP = 16

/**
 * Profiler Overlay 主入口 Composable。
 *
 * 数据刷新架构：
 * - OverlayOutputStrategy.onFrameComplete 写普通 Map（apply callback 安全）
 * - OverlayOutputStrategy.flushIfNeeded 由 render() 在 postponeInvalidation 之后调用，
 *   写 dataVersion（mutableIntStateOf），触发本 Composable 重组
 * - 60 帧计数器节流，约 1 秒刷新一次
 */
@Composable
internal fun ProfilerOverlaySlot(strategy: OverlayOutputStrategy) {
    var expanded by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // 读 dataVersion 建立订阅，flushIfNeeded() 写入时触发重组
    @Suppress("UNUSED_VARIABLE")
    val version = strategy.dataVersion
    val count = strategy.totalCount
    val hotspots = strategy.getHotspots()
    val paused = strategy.paused

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000.toInt()))
                    .clickable { expanded = false }
            )
            ProfilerExpandedPanel(
                strategy = strategy,
                hotspots = hotspots,
                paused = paused,
                onClose = { expanded = false }
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset {
                        IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt())
                    }
                    .padding(FAB_MARGIN_DP.dp)
                    .size(FAB_SIZE_DP.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(fabBackground(count, paused))
                    .clickable { expanded = true }
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(-maxWidthPx, 0f)
                            dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-maxHeightPx, 0f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fabText(count, paused),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfilerExpandedPanel(
    strategy: OverlayOutputStrategy,
    hotspots: List<HotspotItem>,
    paused: Boolean,
    onClose: () -> Unit
) {
    // 用一个递增版本号触发过滤状态刷新（点击过滤/取消/清空后 +1）
    var filterVersion by remember { mutableStateOf(0) }

    // 读取 filterVersion 建立依赖，确保过滤操作后触发重组
    @Suppress("UNUSED_VARIABLE")
    val fv = filterVersion
    val activeHotspots = hotspots.filter { !RecompositionProfiler.isNameExcluded(it.name) }
    // 已过滤列表：直接从 RecompositionProfiler 读，不依赖 hotspots（重置后 hotspots 为空但过滤规则仍在）
    val excludedNames = RecompositionProfiler.getExcludedNames()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xF0222222.toInt()))
                .padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "重组热点", fontSize = 16.sp, color = Color.White)
                Text(
                    text = "X",
                    fontSize = 16.sp,
                    color = Color(0xFFAAAAAA.toInt()),
                    modifier = Modifier.clickable { onClose() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 控制按钮行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.Start),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OverlayControlButton(
                    text = if (paused) "继续" else "暂停",
                    onClick = { strategy.togglePause() }
                )
                OverlayControlButton(
                    text = "重置",
                    onClick = {
                        strategy.reset()
                        RecompositionProfiler.reset()
                    }
                )
                OverlayControlButton(
                    text = "报告",
                    onClick = {
                        val report = RecompositionProfiler.getReport()
                        println("[ProfilerOverlay] Report:\n${report.toJson()}")
                    }
                )
                OverlayControlButton(
                    text = "清空过滤",
                    onClick = {
                        RecompositionProfiler.clearCustomFilters()
                        filterVersion++
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // === 活跃热点列表 ===
            if (activeHotspots.isEmpty() && excludedNames.isEmpty()) {
                Text(
                    text = "暂无重组记录",
                    fontSize = 12.sp,
                    color = Color(0xFF888888.toInt()),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                if (activeHotspots.isEmpty() && excludedNames.isNotEmpty()) {
                    Text(
                        text = "所有热点已被过滤",
                        fontSize = 12.sp,
                        color = Color(0xFF888888.toInt()),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    for ((index, item) in activeHotspots.withIndex()) {
                        if (index > 0) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFF444444.toInt()))
                            )
                        }
                        HotspotRow(
                            item = item,
                            onFilter = {
                                RecompositionProfiler.excludeByName(listOf(item.name))
                                filterVersion++
                            }
                        )
                    }
                }
            }

            // === 已过滤区域（紧凑 chip 自动换行） ===
            if (excludedNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF555555.toInt()))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已过滤:",
                    fontSize = 11.sp,
                    color = Color(0xFF888888.toInt())
                )
                Spacer(modifier = Modifier.height(3.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (name in excludedNames) {
                        FilterChip(
                            name = name,
                            onRemove = {
                                RecompositionProfiler.removeExcludedName(name)
                                filterVersion++
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HotspotRow(item: HotspotItem, onFilter: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                fontSize = 13.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.totalCount}x",
                    fontSize = 13.sp,
                    color = countColor(item.totalCount)
                )
                if (onFilter != null) {
                    OverlayControlButton(
                        text = "过滤",
                        onClick = { onFilter() }
                    )
                }
            }
        }
        if (item.sourceLocation != null) {
            Text(
                text = item.sourceLocation,
                fontSize = 10.sp,
                color = Color(0xFF888888.toInt())
            )
        }
    }
}

/**
 * 已过滤项紧凑标签：「名称 ✕」，点击 ✕ 取消过滤。
 */
@Composable
private fun FilterChip(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF333333.toInt()))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = name, fontSize = 10.sp, color = Color(0xFF999999.toInt()))
        Text(
            text = "✕",
            fontSize = 10.sp,
            color = Color(0xFF999999.toInt()),
            modifier = Modifier.clickable { onRemove() }
        )
    }
}

@Composable
private fun OverlayControlButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val bgColor = if (enabled) Color(0xFF444444.toInt()) else Color(0xFF2A2A2A.toInt())
    val textColor = if (enabled) Color.White else Color(0xFF666666.toInt())
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 11.sp, color = textColor)
    }
}

private fun fabBackground(count: Int, paused: Boolean): Color = when {
    paused -> Color(0xFF607D8B.toInt())
    count == 0 -> Color(0xFF4CAF50.toInt())
    count <= 10 -> Color(0xFFFF9800.toInt())
    else -> Color(0xFFF44336.toInt())
}

private fun fabText(count: Int, paused: Boolean): String = when {
    paused -> "||"
    count == 0 -> "OK"
    else -> "$count"
}

private fun countColor(count: Int): Color = when {
    count <= 5 -> Color(0xFF4CAF50.toInt())
    count <= 20 -> Color(0xFFFF9800.toInt())
    else -> Color(0xFFF44336.toInt())
}
