/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 */

package com.tencent.kuikly.compose

import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeSceneMediatorTest {

    @Test
    fun usesNativeDeadlineWhenClocksShareTimeBase() {
        val deadline = resolveFrameDeadlineMillis(
            localTimestampMillis = 1_002.0,
            frameTimestampMillis = 1_000.0,
            targetTimestampMillis = 1_008.0,
            frameIntervalMillis = 8.0,
        )

        assertEquals(1_008.0, deadline)
    }

    @Test
    fun translatesDeadlineWhenClocksHaveDifferentTimeBases() {
        val localTimestampMillis = 1_750_000_000_000.0
        val deadline = resolveFrameDeadlineMillis(
            localTimestampMillis = localTimestampMillis,
            frameTimestampMillis = 10_000.0,
            targetTimestampMillis = 10_008.0,
            frameIntervalMillis = 8.0,
        )

        assertEquals(localTimestampMillis + 8.0, deadline)
    }

    @Test
    fun fallsBackToFrameIntervalForInvalidTarget() {
        val deadline = resolveFrameDeadlineMillis(
            localTimestampMillis = 1_002.0,
            frameTimestampMillis = 1_000.0,
            targetTimestampMillis = 0.0,
            frameIntervalMillis = 8.0,
        )

        assertEquals(1_010.0, deadline)
    }
}
