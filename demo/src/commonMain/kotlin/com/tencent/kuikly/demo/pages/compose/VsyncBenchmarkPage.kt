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

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.animateColorAsState
import com.tencent.kuikly.compose.animation.core.LinearEasing
import com.tencent.kuikly.compose.animation.core.RepeatMode
import com.tencent.kuikly.compose.animation.core.animateFloat
import com.tencent.kuikly.compose.animation.core.infiniteRepeatable
import com.tencent.kuikly.compose.animation.core.rememberInfiniteTransition
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * Vsync 帧驱动验证页。
 *
 * 验证方式(配合日志过滤 FrameTick / A/B 开关 ComposeContainer.ohosUseNativeVsync):
 * 1. 实时帧率面板: 基于 withFrameNanos 的帧时钟统计(最近 ~2s 窗口)。帧时钟由
 *    ComposeSceneMediator.renderFrame 喂入，直接反映帧驱动源质量:
 *    12ms Timer -> ~83fps 封顶; native vsync -> ≈屏幕刷新率(120Hz 屏 ~120fps)。
 *    页面静止时屏幕 LTPO 降频(如 60Hz)，面板数值随之变化，属正常现象。
 * 2. 连续动画区: 无限往返动画。vsync 驱动下运动连续性肉眼可见地更顺滑，
 *    Timer 驱动下可见周期性顿挫(83Hz 与屏幕节拍失拍)。
 * 3. Fling 压力列表: 快速甩动列表观察滚动流畅度与白屏情况。
 */
@Page("VsyncBenchmarkPage")
class VsyncBenchmarkPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                VsyncBenchmarkContent()
            }
        }
    }

    @Composable
    fun VsyncBenchmarkContent() {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { FrameStatsPanel() }
            item { SmoothAnimationSection() }
            item {
                Text(
                    "Fling 压力列表（快速甩动测试）",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            items(200) { index -> FlingRowItem(index) }
        }
    }

    // ---------- 1. 实时帧率面板 ----------

    @Composable
    fun FrameStatsPanel() {
        var fps by remember { mutableStateOf(0f) }
        var avgMs by remember { mutableStateOf(0f) }
        var maxMs by remember { mutableStateOf(0f) }
        var jank by remember { mutableStateOf(0) }
        var samples by remember { mutableStateOf(0) }

        // 帧时钟探针：每次 frame tick 记录间隔，滑动窗口统计，每 500ms 刷新显示
        LaunchedEffect(Unit) {
            val window = ArrayDeque<Long>()
            var last = 0L
            var lastUiUpdate = 0L
            while (true) {
                val now = withFrameNanos { it }
                if (last != 0L) {
                    val dt = now - last
                    if (dt in 1_000_000L..2_000_000_000L) {
                        window.addLast(dt)
                        if (window.size > 240) {
                            window.removeFirst()
                        }
                    }
                    if (now - lastUiUpdate >= 500_000_000L) {
                        lastUiUpdate = now
                        if (window.isNotEmpty()) {
                            val avg = window.average()
                            fps = (1_000_000_000f / avg.toFloat())
                            avgMs = avg.toFloat() / 1_000_000f
                            maxMs = window.max() / 1_000_000f
                            jank = window.count { it > 12_500_000L }
                            samples = window.size
                        }
                    }
                }
                last = now
            }
        }

        val fpsColor = when {
            fps >= 100f -> Color(0xFF1FA96B) // 绿: 高刷满帧区间
            fps >= 75f -> Color(0xFFE67E22)  // 橙: 12ms Timer 的典型区间
            fps > 0f -> Color(0xFFD64541)    // 红: 明显掉帧
            else -> Color.Gray
        }
        Box(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color.White)
                    .padding(16.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (fps > 0f) fmt1(fps) else "--",
                        fontSize = 44.sp,
                        color = fpsColor,
                    )
                    Text(
                        "  fps",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("帧间隔 avg = ${if (avgMs > 0f) fmt1(avgMs) + "ms" else "--"}", fontSize = 13.sp)
                        Text("帧间隔 max = ${if (maxMs > 0f) fmt1(maxMs) + "ms" else "--"}", fontSize = 13.sp)
                        Text("掉帧(>12.5ms) = $jank / $samples", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "探针基于 Compose 帧时钟(withFrameNanos)：12ms Timer 驱动 ≈83 封顶；vsync 驱动 ≈ 屏幕刷新率。静止时屏幕降频数值下降属正常。",
                    fontSize = 11.sp,
                    color = Color(0xFF999999),
                )
            }
        }
    }

    // ---------- 2. 连续动画区(肉眼验证顺滑度) ----------

    @Composable
    fun SmoothAnimationSection() {
        val transition = rememberInfiniteTransition(label = "vsyncBenchmark")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
            label = "progress",
        )
        val bgColor by animateColorAsState(
            Color(0xFF2196F3).copy(alpha = 0.25f + 0.65f * progress),
        )
        Box(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color.White)
                    .padding(16.dp),
        ) {
            Column {
                Text("连续动画（观察运动连续性）", fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier =
                        Modifier.fillMaxWidth().height(64.dp)
                            .clip(RoundedCornerShape(8.dp)).background(bgColor),
                ) {
                    Box(
                        modifier =
                            Modifier.size(48.dp).offset(x = (progress * 240).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1565C0)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("120", color = Color.White, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 进度条：帧率不足时会出现肉眼可见的阶梯感
                Box(
                    modifier =
                        Modifier.fillMaxWidth().height(10.dp)
                            .clip(RoundedCornerShape(5.dp)).background(Color(0xFFE0E0E0)),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(10.dp)
                                .background(Color(0xFF1FA96B)),
                    )
                }
            }
        }
    }

    // ---------- 3. Fling 压力列表 ----------

    @Composable
    fun FlingRowItem(index: Int) {
        Row(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (index % 2 == 0) Color.White else Color(0xFFECEFF1))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.size(40.dp).clip(RoundedCornerShape(20.dp))
                        .background(Color.hsl((index * 37f) % 360f, 0.55f, 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$index", color = Color.White, fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("列表项 #$index", fontSize = 15.sp)
                Text("快速 fling 观察滚动流畅度", fontSize = 12.sp, color = Color(0xFF888888))
            }
        }
    }

    private fun fmt1(v: Float): String {
        return ((v * 10 + 0.5f).toInt() / 10f).toString()
    }
}
