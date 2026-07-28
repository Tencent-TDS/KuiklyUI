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
    fun converts120HzFrameIntervalFromNanosToMillis() {
        assertEquals(8.333333, resolveFrameIntervalMillis(8_333_333))
    }

    @Test
    fun fallsBackTo60HzForInvalidFrameInterval() {
        assertEquals(16.666667, resolveFrameIntervalMillis(0))
    }
}
