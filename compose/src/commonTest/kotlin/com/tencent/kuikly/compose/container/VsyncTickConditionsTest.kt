/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 */

package com.tencent.kuikly.compose.container

import kotlin.test.Test
import kotlin.test.assertEquals

class VsyncTickConditionsTest {

    @Test
    fun derivesDeadlineFromLocalFrameTimestampAndDynamicInterval() {
        val conditions = VsyncTickConditions {}

        conditions.updateFrameTiming(
            frameTimestampMillis = 1_234.5,
            frameIntervalMillis = 8.25,
        )

        assertEquals(1_234.5, conditions.frameTimestampMillis)
        assertEquals(8.25, conditions.frameIntervalMillis)
        assertEquals(1_242.75, conditions.frameDeadlineMillis)
    }
}
