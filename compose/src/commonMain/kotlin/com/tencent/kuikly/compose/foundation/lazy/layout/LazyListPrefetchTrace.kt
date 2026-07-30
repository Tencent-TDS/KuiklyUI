/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.compose.foundation.lazy.layout

import com.tencent.kuikly.compose.foundation.ComposeFoundationFlags
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi

/** Debug trace for LazyList prefetch pipeline (logcat tag: LazyListPrefetchTrace). */
@OptIn(ExperimentalFoundationApi::class)
internal object LazyListPrefetchTrace {
    const val LOG_TAG = "LazyListPrefetchTrace"

    fun log(message: String) {
        if (ComposeFoundationFlags.isLazyListPrefetchTraceEnabled) {
            println("$LOG_TAG $message")
            ComposeFoundationFlags.lazyListPrefetchTraceListener?.invoke(message)
        }
    }
}
