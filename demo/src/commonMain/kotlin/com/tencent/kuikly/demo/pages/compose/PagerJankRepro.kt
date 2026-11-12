/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the KuiklyUI license;
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.PagerState
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Tab
import com.tencent.kuikly.compose.material3.TabRow
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.launch

private const val JANK_TAG = "[PAGER_JANK]"

/**
 * 重组诊断开关。置 true 后打印 [FRAME_RECOMP]，用于排查不必要的重组：
 * 静止时若计数持续增长，说明页面被持续重组（会污染性能测量并浪费 CPU）。
 */
private const val RECOMP_DIAG = false

/**
 * 自动回归开关（默认关闭）。置 true 后依次执行 刷新 → 增删tab → 切页 → 回切，
 * 配合 tools/repro/pager-regression-visual.sh 做像素级行为回归：
 * 验证改动未破坏「数据集替换 / pageCount 变化 / 切页」的可见行为。
 */
private const val AUTO_REGRESSION = false

/**
 * 变更监听开关。置 true 后打印 [APPLY_OBSERVE]，
 * 用于在重组发生时定位"哪个 state 变化"触发了它。
 */
private const val APPLY_DIAG = false

/**
 * 自动切页开关（默认关闭）。置 true 后每 2.5 秒自动切到下一个 tab 并打印耗时，
 * 用于 iOS/鸿蒙等无法用 adb 点击的平台做切页耗时采集。
 * 日志前缀 [PAGER_JANK] AUTOSWITCH。
 */
private const val AUTO_SWITCH = false

/**
 * 复现：HorizontalPager 在 beyondViewportPageCount=0 时切页卡顿。
 *
 * 结构贴近真实榜单页：外层 HorizontalPager（5 个 tab），每页内部是一个
 * 较重的 LazyColumn（标题 + 摘要 + 元信息 + 缩略图占位），条目数量足以让
 * 页面组合/测量成本达到几十毫秒量级。
 *
 * 页面顶部显示上一帧耗时与切页耗时，便于在真机上直接观察，无需 profiler。
 */
@Page("PagerJankRepro")
internal class PagerJankRepro : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            PagerJankReproContent()
        }
    }
}

private data class FeedItem(
    val id: Int,
    val title: String,
    val summary: String,
    val source: String,
)

private fun buildFeed(tabIndex: Int, version: Int = 0): List<FeedItem> {
    // 每个 tab 生成一批具备稳定 id 的条目，数量足够形成可观测的组合成本
    return (1..60).map { i ->
        val id = tabIndex * 10000 + i
        FeedItem(
            id = id,
            title = "V$version 榜单 $tabIndex · 条目标题 $i：这是一条用于压测组合与测量的新闻标题",
            summary = "v$version 这里是摘要内容，用于让单个 item 具备足够的布局与文本测量成本，条目编号 $i。",
            source = "来源 $tabIndex-$i · 评论 ${i * 7} · ${i % 24 + 1} 小时前",
        )
    }
}

@Composable
private fun PagerJankReproContent() {
    // 诊断：tabs 可变，用于验证 pageCount 动态变化（增删 tab）场景
    var tabs by remember {
        mutableStateOf(listOf("热点", "体育", "娱乐", "较真", "视频"))
    }
    // 诊断：feedVersion 变化即模拟"下拉刷新"替换数据集，用于验证 forceRemeasure=false 下内容是否更新
    var feedVersion by remember { mutableStateOf(0) }
    val feeds = remember(feedVersion, tabs.size) { tabs.indices.map { buildFeed(it, feedVersion) } }
    // 点「刷新」按钮可替换数据集（模拟下拉刷新），标题前缀 V{version} 便于观察刷新是否生效
    // 点「增删tab」按钮可改变 pageCount（5↔4），用于验证动态增删 tab 场景
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    if (AUTO_REGRESSION) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(4000)
            feedVersion++
            println("$JANK_TAG REG refresh feedVersion=$feedVersion")
            kotlinx.coroutines.delay(3000)
            tabs = if (tabs.size > 3) tabs.dropLast(1) else listOf("热点", "体育", "娱乐", "较真", "视频")
            println("$JANK_TAG REG tabchange tabs=${tabs.size}")
            kotlinx.coroutines.delay(3000)
            pagerState.scrollToPage(1)
            println("$JANK_TAG REG scrollToPage1 current=${pagerState.currentPage}")
            kotlinx.coroutines.delay(3000)
            pagerState.scrollToPage(0)
            println("$JANK_TAG REG scrollToPage0 current=${pagerState.currentPage}")
            kotlinx.coroutines.delay(2000)
            println("$JANK_TAG REG_DONE")
        }
    }

    // 自动切页：供 iOS/鸿蒙等无法 adb 点击的平台采集切页耗时
    if (AUTO_SWITCH) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            var target = 1
            while (true) {
                kotlinx.coroutines.delay(2500)
                val from = pagerState.currentPage
                val start = kotlin.time.TimeSource.Monotonic.markNow()
                pagerState.scrollToPage(target % tabs.size)
                val cost = start.elapsedNow().inWholeMilliseconds
                println("$JANK_TAG AUTOSWITCH from=$from to=${target % tabs.size} scrollToPage=${cost}ms")
                target++
            }
        }
    }

    // beyondViewportPageCount 可通过点击顶部按钮在 0 / 1 / 全部保活 之间切换
    var beyondMode by remember { mutableStateOf(1) }
    // 行内容复杂度档位：0=完整(3 Text + 缩略图)，1=精简(1 Text)，2=极简(空 Box)
    var rowMode by remember { mutableStateOf(0) }
    // 每页条目数档位：0=60，1=10（仅首屏量级）
    var rowCountMode by remember { mutableStateOf(1) }


    // 注：框架自带的 RecompositionProfiler 在本工程未产出日志（可能需编译器插桩），
    // 故改用页面内计数法，见 FeedListPage 内的 [FRAME_RECOMP]（由 RECOMP_DIAG 控制）。
    val beyondCount = when (beyondMode) {
        0 -> 0
        1 -> 1
        2 -> 2
        3 -> 3
        else -> tabs.lastIndex
    }
    // 开启预组合时 beyond 固定为 0：让预组合单独承担提前组合的职责，便于 A/B

    var lastFrameMs by remember { mutableStateOf(0L) }
    var maxFrameMs by remember { mutableStateOf(0L) }
    var lastSwitchMs by remember { mutableStateOf(0L) }
    var switchLog by remember { mutableStateOf("尚未切页") }

    // 注意：此处原本有一个每帧更新 lastFrameMs/maxFrameMs 的循环，
    // 它会导致整个页面每帧重组（实测静止 5 秒重组 550 次），
    // 严重污染切页耗时测量。已移除；帧信息改用底部一次性显示。

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F3F5))) {
        // 顶部状态栏：实时帧耗时 + 切页耗时
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "帧 ${lastFrameMs}ms / 峰值 ${maxFrameMs}ms | 切页 ${lastSwitchMs}ms",
                fontSize = 12.sp,
                color = Color(0xFF00E676),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "beyond=$beyondCount  $switchLog",
                fontSize = 11.sp,
                color = Color(0xFFB0BEC5),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeButton("行全", rowMode == 0) { rowMode = 0 }
                ModeButton("无clip", rowMode == 6) { rowMode = 6 }
                ModeButton("空页", rowMode == 5) { rowMode = 5 }
                ModeButton("极简", rowMode == 2) { rowMode = 2 }
                ModeButton("简×3", rowMode == 4) { rowMode = 4 }
                ModeButton("b0", beyondMode == 0) { beyondMode = 0 }
                ModeButton("b1", beyondMode == 1) { beyondMode = 1 }
                ModeButton("b4", beyondMode == 4) { beyondMode = 4 }

                ModeButton("c10", rowCountMode == 1) { rowCountMode = 1 }
                ModeButton("c30", rowCountMode == 2) { rowCountMode = 2 }
                ModeButton("增删tab", tabs.size != 5) {
                    tabs = if (tabs.size > 3) tabs.dropLast(1) else listOf("热点", "体育", "娱乐", "较真", "视频")
                    println("$JANK_TAG tabchange tabs=${tabs.size}")
                }
                ModeButton("刷新", feedVersion > 0) {
                    feedVersion++
                    println("$JANK_TAG refresh feedVersion=$feedVersion tabs=${tabs.size}")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "重置峰值",
                    fontSize = 12.sp,
                    color = Color(0xFFFFCA28),
                    modifier = Modifier.clickable { maxFrameMs = 0L },
                )
            }
        }

        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color(0xFF1976D2),
            contentColor = Color.White,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        val fromPage = pagerState.currentPage
                        scope.launch {
                            val start = kotlin.time.TimeSource.Monotonic.markNow()
                            pagerState.scrollToPage(index)
                            val cost = start.elapsedNow().inWholeMilliseconds
                            lastSwitchMs = cost
                            switchLog = "${tabs[fromPage]}→${tabs[index]} ${cost}ms"
                            val rc = when (rowCountMode) { 1 -> 10; 2 -> 30; else -> 60 }
                            println("$JANK_TAG beyond=$beyondCount rowMode=$rowMode rows=$rc from=$fromPage to=$index scrollToPage=${cost}ms")

                        }
                    },
                    text = { Text(title, fontSize = 15.sp, color = Color.White) },
                )
            }
        }

        HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().bouncesEnable(false),
                beyondViewportPageCount = beyondCount,
                key = { index -> "tab_$index" },
            ) { page ->
                FeedListPage(
                    feed = feeds[page].take(
                        when (rowCountMode) { 1 -> 10; 2 -> 30; else -> 60 }
                    ),
                    tabName = tabs[page],
                    rowMode = rowMode,
                )
        }
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = if (selected) Color.Black else Color.White,
        modifier = Modifier
            .background(
                if (selected) Color(0xFF00E676) else Color(0xFF455A64),
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun FeedListPage(
    feed: List<FeedItem>,
    tabName: String,
    rowMode: Int,
    pinSelf: Boolean = false,
) {
    // 实验：pin 当前页自身，验证 PinnableContainer 能否阻止页面退出窗口时被销毁
    if (pinSelf) {
        val container = com.tencent.kuikly.compose.ui.layout.LocalPinnableContainer.current
        androidx.compose.runtime.DisposableEffect(container) {
            val handle = container?.pin()
            onDispose { handle?.release() }
        }
    }
    // 诊断（默认关闭）：统计本页被组合/重组的次数。
    // 打开后若静止时计数持续增长，即存在不必要的重组（可用于排查框架引发的多余重组）。
    if (RECOMP_DIAG) {
        val __c = remember { intArrayOf(0) }
        __c[0]++
        androidx.compose.runtime.SideEffect {
            println("[FRAME_RECOMP] tab=$tabName count=${__c[0]}")
        }
    }
    // 诊断：监听 snapshot 变更，定位"哪个 state 变化"引发了重组
    if (APPLY_DIAG) {
        androidx.compose.runtime.DisposableEffect(Unit) {
            val observer: (Set<Any>, androidx.compose.runtime.snapshots.Snapshot) -> Unit = { changed, _ ->
                val names = changed.take(8).joinToString(",") { s ->
                    val v = runCatching {
                        (s as? androidx.compose.runtime.State<*>)?.value?.let {
                            it.toString().take(24)
                        }
                    }.getOrNull()
                    // 不用 javaClass：Kotlin/Native（iOS/鸿蒙）没有 javaClass
                    "${s.toString().take(28)}($v)"
                }
                println("[APPLY_OBSERVE] tab=$tabName changed(${changed.size})=$names")
            }
            val handle = androidx.compose.runtime.snapshots.Snapshot.registerApplyObserver(observer)
            onDispose { handle.dispose() }
        }
    }
    if (rowMode == 5) {
        // 空页面：不建 LazyColumn，只留一个撑满的 Box
        Box(modifier = Modifier.fillMaxSize().background(Color.White))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            count = feed.size,
            key = { index -> feed[index].id },
        ) { index ->
            FeedRow(item = feed[index], tabName = tabName, rowMode = rowMode)
        }
    }
}

@Composable
private fun FeedRow(item: FeedItem, tabName: String, rowMode: Int) {
    if (rowMode == 2) {
        // 极简：单个空 Box，用于测量"每个条目"的固定开销
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .background(Color.White),
        )
        return
    }
    if (rowMode == 4) {
        // 极简×3：一行内 3 个空 Box，行高不变、首屏行数不变，
        // 只把节点数翻 3 倍 —— 用于区分成本按"行"计还是按"节点"计
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .background(Color.White),
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(76.dp, 54.dp)
                        .background(Color(0xFFEEEEEE)),
                )
            }
        }
        return
    }
    if (rowMode == 5) {
        // 空页面：完全没有内容。用于测"切一页"本身的固定开销（与列表无关）
        return
    }
    if (rowMode == 1) {
        // 精简：只有一个 Text
        Text(
            text = item.title,
            fontSize = 16.sp,
            color = Color(0xFF212121),
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121),
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.summary,
                fontSize = 13.sp,
                color = Color(0xFF616161),
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$tabName · ${item.source}",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 缩略图占位：制造额外的节点与绘制成本
        Box(
            modifier = Modifier
                .size(76.dp, 54.dp)
                .then(if (rowMode == 6) Modifier else Modifier.clip(RoundedCornerShape(4.dp)))
                .background(Color(0xFFD8D8D8)),
        )
    }
}
