/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.ComposeFoundationFlags
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListLayoutInfo
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.enableLazyListPrefetch
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.semantics.testTag
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LOG_TAG = "LazyListPrefetchDemo"
private const val ITEM_COUNT = 100
/** Disable beyond-bounds extra composition so only prefetch drives off-screen compose. */
private const val BEYOND_BOUNDS_ITEM_COUNT = 0

private enum class PrefetchScenario(val label: String) {
    ModifierOptIn("modifier_opt_in"),
    GlobalOnly("global_only"),
    ModifierOverrideOff("modifier_override_off"),
    CacheWindow("cache_window_ahead_1000dp"),
}

@Page("LazyListPrefetchDemo")
@OptIn(ExperimentalFoundationApi::class)
class LazyListPrefetchDemoPage : ComposeContainer() {
    override fun debugUIInspector(): Boolean = true

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyListPrefetchDemoContent()
            }
        }
    }
}

private fun isIndexInViewport(
    layoutInfo: LazyListLayoutInfo,
    index: Int,
): Boolean {
    val item = layoutInfo.visibleItemsInfo.find { it.index == index } ?: return false
    val itemStart = item.offset
    val itemEnd = item.offset + item.size
    return itemEnd > layoutInfo.viewportStartOffset && itemStart < layoutInfo.viewportEndOffset
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyListPrefetchDemoContent() {
    var prefetchEnabled by remember { mutableStateOf(false) }
    var scenario by remember { mutableStateOf(PrefetchScenario.ModifierOptIn) }
    var heavyItems by remember { mutableStateOf(false) }
    val composedIndices = remember { mutableSetOf<Int>() }
    val placedIndices = remember { mutableSetOf<Int>() }
    var composedCount by remember { mutableIntStateOf(0) }
    var placedCount by remember { mutableIntStateOf(0) }
    var maxComposedIndex by remember { mutableIntStateOf(-1) }
    var maxPlacedIndex by remember { mutableIntStateOf(-1) }
    var indexLead by remember { mutableIntStateOf(0) }
    var headGapMetric by remember { mutableIntStateOf(0) }
    val offScreenComposed = remember { mutableSetOf<Int>() }
    val compositionEnterCounts = remember { mutableStateMapOf<Int, Int>() }
    val prefetchPipelineScheduled = remember { mutableSetOf<Int>() }
    val prefetchPipelineComposed = remember { mutableSetOf<Int>() }
    var prefetchTargetIndex by remember { mutableIntStateOf(-1) }
    var prefetchTargetSource by remember { mutableStateOf("none") }
    var prefetchPipelineReentryCount by remember { mutableIntStateOf(0) }
    var compositionReentryTotal by remember { mutableIntStateOf(0) }
    val defaultListState = rememberLazyListState()
    val cacheWindow =
        remember {
            LazyLayoutCacheWindow(ahead = 1000.dp, behind = 1000.dp)
        }
    val cacheWindowListState = rememberLazyListState(cacheWindow = cacheWindow)
    val listState =
        if (scenario == PrefetchScenario.CacheWindow) cacheWindowListState else defaultListState
    val scope = rememberCoroutineScope()
    var resetToken by remember { mutableIntStateOf(0) }

    fun resetCounts() {
        composedIndices.clear()
        placedIndices.clear()
        composedCount = 0
        placedCount = 0
        maxComposedIndex = -1
        maxPlacedIndex = -1
        indexLead = 0
        headGapMetric = 0
        offScreenComposed.clear()
        compositionEnterCounts.clear()
        prefetchPipelineScheduled.clear()
        prefetchPipelineComposed.clear()
        prefetchTargetIndex = -1
        prefetchTargetSource = "none"
        prefetchPipelineReentryCount = 0
        compositionReentryTotal = 0
        if (scenario == PrefetchScenario.ModifierOptIn || scenario == PrefetchScenario.CacheWindow) {
            prefetchEnabled = false
        }
        resetToken++
    }

    fun clearMetricsOnly() {
        composedIndices.clear()
        placedIndices.clear()
        composedCount = 0
        placedCount = 0
        maxComposedIndex = -1
        maxPlacedIndex = -1
        indexLead = 0
        headGapMetric = 0
        offScreenComposed.clear()
        compositionEnterCounts.clear()
        prefetchPipelineScheduled.clear()
        prefetchPipelineComposed.clear()
        prefetchTargetIndex = -1
        prefetchTargetSource = "none"
        prefetchPipelineReentryCount = 0
        compositionReentryTotal = 0
    }

    /** Framework prefetch pipeline events (LazyListPrefetchTrace → ComposeFoundationFlags listener). */
    fun onPrefetchPipelineTrace(message: String) {
        Regex("schedulePremeasure index=(\\d+)").find(message)?.let { match ->
            val index = match.groupValues[1].toInt()
            if (prefetchPipelineScheduled.add(index)) {
                println("$LOG_TAG prefetchPipeline schedule index=$index")
            }
        }
        Regex("executeRequest composed index=(\\d+)").find(message)?.let { match ->
            val index = match.groupValues[1].toInt()
            if (prefetchPipelineComposed.add(index)) {
                println(
                    "$LOG_TAG prefetchPipeline composed index=$index source=prefetch",
                )
                if (prefetchTargetIndex < 0 && index in offScreenComposed) {
                    prefetchTargetIndex = index
                    prefetchTargetSource = "prefetch"
                    println(
                        "$LOG_TAG prefetchTarget index=$index source=prefetch offscreen=1 pipelineComposed=1",
                    )
                }
            }
        }
    }

    fun maybeSetPrefetchTarget(index: Int, inViewport: Boolean) {
        if (prefetchTargetIndex >= 0) return
        if (inViewport) return
        if (index !in prefetchPipelineComposed && index !in offScreenComposed) return
        if (index <= maxPlacedIndex) return
        prefetchTargetIndex = index
        prefetchTargetSource =
            if (index in prefetchPipelineComposed) "prefetch" else "prefetch_offscreen"
        println(
            "$LOG_TAG prefetchTarget index=$index source=$prefetchTargetSource offscreen=1 pipelineComposed=${index in prefetchPipelineComposed}",
        )
    }

    /** Called when item slot enters composition tree (prefetch or visible). Re-entry => prefetch slot discarded. */
    fun onCompositionEnter(index: Int) {
        val inViewport = isIndexInViewport(listState.layoutInfo, index)
        val enterCount = (compositionEnterCounts[index] ?: 0) + 1
        compositionEnterCounts[index] = enterCount
        if (enterCount == 1) {
            composedIndices.add(index)
            composedCount = composedIndices.size
            maxComposedIndex = maxOf(maxComposedIndex, index)
            if (!inViewport) {
                offScreenComposed.add(index)
            }
            maybeSetPrefetchTarget(index, inViewport)
            indexLead = maxComposedIndex - maxPlacedIndex
            val pipelineComposed = index in prefetchPipelineComposed
            println(
                "$LOG_TAG compositionEnter index=$index enterCount=1 inViewport=$inViewport pipelineComposed=$pipelineComposed total=$composedCount indexLead=$indexLead",
            )
            println(
                "$LOG_TAG composed index=$index total=$composedCount indexLead=$indexLead",
            )
        } else {
            compositionReentryTotal++
            val prefetchSlot = index in prefetchPipelineComposed || index in offScreenComposed
            if (prefetchSlot) {
                prefetchPipelineReentryCount++
            }
            println(
                "$LOG_TAG compositionReentry index=$index enterCount=$enterCount inViewport=$inViewport prefetchSlot=$prefetchSlot",
            )
        }
    }

    fun markPlaced(index: Int) {
        if (placedIndices.add(index)) {
            placedCount = placedIndices.size
            maxPlacedIndex = maxOf(maxPlacedIndex, index)
            indexLead = maxComposedIndex - maxPlacedIndex
            println(
                "$LOG_TAG placed(visible) index=$index total=$placedCount indexLead=$indexLead",
            )
        }
    }

    SideEffect {
        ComposeFoundationFlags.isLazyListPrefetchTraceEnabled = true
        ComposeFoundationFlags.lazyListPrefetchTraceListener = { onPrefetchPipelineTrace(it) }
        when (scenario) {
            PrefetchScenario.ModifierOptIn,
            PrefetchScenario.CacheWindow,
            -> {
                ComposeFoundationFlags.isLazyListPrefetchEnabled = false
            }
            PrefetchScenario.GlobalOnly,
            PrefetchScenario.ModifierOverrideOff,
            -> {
                ComposeFoundationFlags.isLazyListPrefetchEnabled = true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "Prefetch: ${if (prefetchEnabled) "ON" else "OFF"}",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { prefetchEnabled = !prefetchEnabled }
                    .background(if (prefetchEnabled) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                    .padding(12.dp)
                    .semantics { testTag = "prefetch_toggle" },
            color = Color.White,
        )

        Text(
            text = "Scenario: ${scenario.label}",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .semantics { testTag = "scenario_label" },
        )

        Text(
            text = "scenario_modifier_opt_in",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        ComposeFoundationFlags.isLazyListPrefetchEnabled = false
                        scenario = PrefetchScenario.ModifierOptIn
                        resetCounts()
                    }
                    .background(Color(0xFFE8EAF6))
                    .padding(8.dp)
                    .semantics { testTag = "scenario_modifier_opt_in" },
        )
        Text(
            text = "scenario_global_only",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        ComposeFoundationFlags.isLazyListPrefetchEnabled = true
                        scenario = PrefetchScenario.GlobalOnly
                        prefetchEnabled = false
                        resetCounts()
                    }
                    .background(Color(0xFFE8EAF6))
                    .padding(8.dp)
                    .semantics { testTag = "scenario_global_only" },
        )
        Text(
            text = "scenario_modifier_override_off",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        ComposeFoundationFlags.isLazyListPrefetchEnabled = true
                        scenario = PrefetchScenario.ModifierOverrideOff
                        prefetchEnabled = false
                        resetCounts()
                    }
                    .background(Color(0xFFE8EAF6))
                    .padding(8.dp)
                    .semantics { testTag = "scenario_modifier_override_off" },
        )

        Text(
            text = "scenario_cache_window",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        ComposeFoundationFlags.isLazyListPrefetchEnabled = false
                        scenario = PrefetchScenario.CacheWindow
                        resetCounts()
                    }
                    .background(Color(0xFFE8EAF6))
                    .padding(8.dp)
                    .semantics { testTag = "scenario_cache_window" },
        )

        Text(
            text = "heavy_items: ${if (heavyItems) "ON" else "OFF"}",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        heavyItems = !heavyItems
                        resetCounts()
                    }
                    .background(if (heavyItems) Color(0xFFFFCC80) else Color(0xFFEEEEEE))
                    .padding(8.dp)
                    .semantics { testTag = "heavy_items_toggle" },
        )

        Text(
            text = "reset_counts",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        resetCounts()
                        scope.launch {
                            listState.scrollToItem(0)
                        }
                    }
                    .background(Color(0xFFFFCDD2))
                    .padding(8.dp)
                    .semantics { testTag = "reset_counts" },
        )

        Text(
            text = "clear_metrics_only",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { clearMetricsOnly() }
                    .background(Color(0xFFFFE0B2))
                    .padding(8.dp)
                    .semantics { testTag = "clear_metrics_only" },
        )

        Text(
            text = "composed=$composedCount placed=$placedCount",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .semantics { testTag = "prefetch_stats" },
        )

        Text(
            text = "composed_count=$composedCount",
            modifier = Modifier.semantics { testTag = "composed_count" },
        )
        Text(
            text = "placed_count=$placedCount",
            modifier = Modifier.semantics { testTag = "placed_count" },
        )
        Text(
            text = "prefetch_lead=$indexLead",
            modifier = Modifier.semantics { testTag = "prefetch_lead" },
        )
        Text(
            text = "prefetch_index_lead=$indexLead",
            modifier = Modifier.semantics { testTag = "prefetch_index_lead" },
        )
        Text(
            text = "prefetch_head_gap=$headGapMetric",
            modifier = Modifier.semantics { testTag = "prefetch_head_gap" },
        )
        Text(
            text = "prefetch_target_index=$prefetchTargetIndex",
            modifier = Modifier.semantics { testTag = "prefetch_target_index" },
        )
        Text(
            text =
                "prefetch_target_enter_count=${if (prefetchTargetIndex >= 0) compositionEnterCounts[prefetchTargetIndex] ?: 0 else 0}",
            modifier = Modifier.semantics { testTag = "prefetch_target_enter_count" },
        )
        Text(
            text = "prefetch_target_source=$prefetchTargetSource",
            modifier = Modifier.semantics { testTag = "prefetch_target_source" },
        )
        Text(
            text =
                "prefetch_target_placed=${if (prefetchTargetIndex >= 0 && prefetchTargetIndex in placedIndices) 1 else 0}",
            modifier = Modifier.semantics { testTag = "prefetch_target_placed" },
        )
        Text(
            text =
                "prefetch_target_pipeline=${if (prefetchTargetIndex >= 0 && prefetchTargetIndex in prefetchPipelineComposed) 1 else 0}",
            modifier = Modifier.semantics { testTag = "prefetch_target_pipeline" },
        )
        Text(
            text = "prefetch_pipeline_reentry_count=$prefetchPipelineReentryCount",
            modifier = Modifier.semantics { testTag = "prefetch_pipeline_reentry_count" },
        )
        Text(
            text = "composition_reentry_total=$compositionReentryTotal",
            modifier = Modifier.semantics { testTag = "composition_reentry_total" },
        )
        Text(
            text = "max_composed_index=$maxComposedIndex",
            modifier = Modifier.semantics { testTag = "max_composed_index" },
        )
        Text(
            text = "max_placed_index=$maxPlacedIndex",
            modifier = Modifier.semantics { testTag = "max_placed_index" },
        )
        Text(
            text =
                "pausable_flag=${ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled}",
            modifier = Modifier.semantics { testTag = "pausable_flag" },
        )

        Text(
            text = "beyond_bounds=$BEYOND_BOUNDS_ITEM_COUNT",
            modifier = Modifier.semantics { testTag = "beyond_bounds_count" },
        )

        val listModifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics { testTag = "lazy_list" }
                .then(
                    when (scenario) {
                        PrefetchScenario.ModifierOptIn,
                        PrefetchScenario.CacheWindow,
                        ->
                            if (prefetchEnabled) {
                                Modifier.enableLazyListPrefetch()
                            } else {
                                Modifier
                            }
                        PrefetchScenario.GlobalOnly -> Modifier
                        PrefetchScenario.ModifierOverrideOff ->
                            Modifier.enableLazyListPrefetch(false)
                    },
                )

        val layoutInfo = listState.layoutInfo
        val layoutVisibleIndices =
            layoutInfo.visibleItemsInfo
                .filter { isIndexInViewport(layoutInfo, it.index) }
                .map { it.index }
        var lastLayoutVisibleLog by remember { mutableStateOf("") }
        SideEffect {
            if (layoutVisibleIndices.isEmpty()) return@SideEffect
            layoutVisibleIndices.forEach { markPlaced(it) }
            val layoutMin = layoutVisibleIndices.min()
            val composedBelowVisible =
                composedIndices.filter { it < layoutMin }.maxOrNull()
            val headGap =
                if (composedBelowVisible != null) layoutMin - composedBelowVisible else 0
            headGapMetric = headGap
            val snapshot =
                "${layoutVisibleIndices.joinToString(",")}|min=$layoutMin|max=${layoutVisibleIndices.max()}|placed=$maxPlacedIndex|headGap=$headGap"
            if (snapshot != lastLayoutVisibleLog) {
                lastLayoutVisibleLog = snapshot
                println(
                    "$LOG_TAG layoutVisible indices=$layoutVisibleIndices min=$layoutMin max=${layoutVisibleIndices.max()} placedMax=$maxPlacedIndex headGap=$headGap",
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = listModifier,
            beyondBoundsItemCount = BEYOND_BOUNDS_ITEM_COUNT,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                count = ITEM_COUNT,
                key = { "$resetToken-$it" },
            ) { index ->
                PrefetchDemoItem(
                    index = index,
                    heavy = heavyItems,
                    onCompositionEnter = { onCompositionEnter(it) },
                )
            }
        }
    }
}

@Composable
private fun PrefetchDemoItem(
    index: Int,
    heavy: Boolean,
    onCompositionEnter: (Int) -> Unit,
) {
    DisposableEffect(index) {
        onCompositionEnter(index)
        onDispose { }
    }

    if (heavy) {
        LaunchedEffect(index) {
            var acc = 0
            repeat(200) { i ->
                acc += i * index
            }
            delay(1)
            if (acc == -1) {
                println("$LOG_TAG unreachable")
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(if (heavy) 120.dp else 72.dp)
                .background(Color(0xFFE3F2FD))
                .padding(horizontal = 12.dp),
    ) {
        Text(
            text = "Item $index",
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
