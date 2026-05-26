/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.compose.foundation.lazy.layout

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
internal object NoOpPrefetchScheduler : PrefetchScheduler, PriorityPrefetchScheduler {
    override fun schedulePrefetch(prefetchRequest: PrefetchRequest) {}

    override fun scheduleLowPriorityPrefetch(prefetchRequest: PrefetchRequest) {}

    override fun scheduleHighPriorityPrefetch(prefetchRequest: PrefetchRequest) {}

    fun cancelAll() {}
}
