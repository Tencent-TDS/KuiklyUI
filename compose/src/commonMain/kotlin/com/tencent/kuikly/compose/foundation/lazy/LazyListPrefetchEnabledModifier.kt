/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.compose.foundation.lazy

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.modifier.modifierLocalOf
import com.tencent.kuikly.compose.ui.modifier.modifierLocalProvider

internal val ModifierLocalLazyListPrefetchEnabled = modifierLocalOf<Boolean?> { null }

/**
 * Opt-in prefetch for this LazyList. When omitted, [ComposeFoundationFlags.isLazyListPrefetchEnabled]
 * applies. Explicit false overrides a global true.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
fun Modifier.enableLazyListPrefetch(enabled: Boolean = true): Modifier =
    modifierLocalProvider(ModifierLocalLazyListPrefetchEnabled) { enabled }
