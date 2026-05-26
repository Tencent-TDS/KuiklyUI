/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
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
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.lazy.enableLazyListPrefetch
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.semantics.testTag
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

private const val LOG_TAG = "LazyListPrefetchDemo"
private const val ITEM_COUNT = 100

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyListPrefetchDemoContent() {
    var prefetchEnabled by remember { mutableStateOf(false) }
    val composedIndices = remember { mutableSetOf<Int>() }
    val placedIndices = remember { mutableSetOf<Int>() }
    var composedCount by remember { mutableIntStateOf(0) }
    var placedCount by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    fun markComposed(index: Int) {
        if (composedIndices.add(index)) {
            composedCount = composedIndices.size
            println("$LOG_TAG composed index=$index total=$composedCount")
        }
    }

    fun markPlaced(index: Int) {
        if (placedIndices.add(index)) {
            placedCount = placedIndices.size
            println("$LOG_TAG placed index=$index total=$placedCount")
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
            text = "composed=$composedCount placed=$placedCount",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
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

        val listModifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(if (prefetchEnabled) Modifier.enableLazyListPrefetch() else Modifier)

        LazyColumn(
            state = listState,
            modifier = listModifier,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                count = ITEM_COUNT,
                key = { it },
            ) { index ->
                PrefetchDemoItem(
                    index = index,
                    onComposed = { markComposed(index) },
                    onPlaced = { markPlaced(index) },
                )
            }
        }
    }
}

@Composable
private fun PrefetchDemoItem(
    index: Int,
    onComposed: () -> Unit,
    onPlaced: () -> Unit,
) {
    SideEffect { onComposed() }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Color(0xFFE3F2FD))
                .onGloballyPositioned { onPlaced() }
                .padding(horizontal = 12.dp),
    ) {
        Text(
            text = "Item $index",
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
