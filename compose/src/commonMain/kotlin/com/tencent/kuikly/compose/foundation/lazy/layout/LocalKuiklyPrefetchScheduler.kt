/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.compose.foundation.lazy.layout

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.staticCompositionLocalOf

@OptIn(ExperimentalFoundationApi::class)
internal val LocalKuiklyPrefetchScheduler =
    staticCompositionLocalOf<PrefetchScheduler> { NoOpPrefetchScheduler }
