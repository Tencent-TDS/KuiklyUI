/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 */

package com.tencent.kuikly.compose

/**
 * A non-positive value lets the platform follow the display refresh rate.
 * Kotlin/JS is capped at 60fps to avoid amplifying its per-frame runtime overhead.
 */
internal expect val composeVsyncMaxFramesPerSecond: Int
