/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.compose.foundation.lazy.layout

internal actual fun createDefaultKuiklyPrefetchScheduler(): PrefetchScheduler =
    KuiklyPrefetchScheduler()

internal actual val isPrefetchSupported: Boolean = true
