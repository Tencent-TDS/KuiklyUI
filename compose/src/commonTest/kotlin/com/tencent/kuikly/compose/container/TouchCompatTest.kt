/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 */

package com.tencent.kuikly.compose.container

import com.tencent.kuikly.compose.ui.scaleWithDensity
import com.tencent.kuikly.core.base.event.Touch
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [Touch] 改为普通类后的行为测试，包括为兼容旧代码保留的六参数构造方法、copy 和解构操作。
 */
class TouchCompatTest {

    @Suppress("DEPRECATION")
    @Test
    fun legacyMembersRemainUsable() {
        val legacy = Touch(1f, 2f, 3f, 4f, 5f, 6L)
        assertNull(legacy.pressed)
        val (x, y, pageX, pageY, hash, pointerId) = legacy
        assertEquals(listOf(1f, 2f, 3f, 4f, 5f), listOf(x, y, pageX, pageY, hash))
        assertEquals(6L, pointerId)
        assertEquals(false, Touch(1f, 2f, 3f, 4f, 5f, 6L, false).copy(x = 9f).pressed)
    }

    @Test
    fun pressedIsExplicitAndNullable() {
        assertEquals(true, Touch(1f, 2f, 3f, 4f, 5f, 6L, true).pressed)
        assertEquals(false, Touch(1f, 2f, 3f, 4f, 5f, 6L, false).pressed)
        assertNull(Touch(1f, 2f, 3f, 4f, 5f, 6L, null).pressed)
    }

    @Test
    fun equalityCoversAllFields() {
        val base = Touch(1f, 2f, 3f, 4f, 5f, 6L, true)
        assertEquals(base, Touch(1f, 2f, 3f, 4f, 5f, 6L, true))
        assertEquals(base.hashCode(), Touch(1f, 2f, 3f, 4f, 5f, 6L, true).hashCode())
        assertNotEquals(base, Touch(1f, 2f, 3f, 4f, 5f, 6L, false))
        assertNotEquals(base, Touch(1f, 2f, 3f, 4f, 5f, 6L, null))
        // 与 data class 生成的 equals 语义一致：Float 按 compare 比较
        assertEquals(
            Touch(Float.NaN, 0f, 0f, 0f, 0f, 0L, null),
            Touch(Float.NaN, 0f, 0f, 0f, 0f, 0L, null)
        )
    }

    @Test
    fun decodeDistinguishesMissingAndFalse() {
        assertNull(Touch.decode(JSONObject().put("pointerId", 1)).pressed)
        assertNull(Touch.decode(JSONObject().put("pointerId", 1).put("pressed", null)).pressed)
        assertNull(Touch.decode(JSONObject().put("pointerId", 1).put("pressed", "unknown")).pressed)
        assertEquals(false, Touch.decode(JSONObject().put("pointerId", 1).put("pressed", false)).pressed)
        assertEquals(true, Touch.decode(JSONObject().put("pointerId", 1).put("pressed", true)).pressed)
        assertEquals(false, Touch.decode(JSONObject().put("pointerId", 1).put("pressed", 0)).pressed)
        assertEquals(true, Touch.decode(JSONObject().put("pointerId", 1).put("pressed", 1)).pressed)
    }

    @Test
    fun scaleWithDensityKeepsPressed() {
        assertEquals(false, Touch(1f, 2f, 3f, 4f, 5f, 6L, false).scaleWithDensity(2f).pressed)
        assertEquals(true, Touch(1f, 2f, 3f, 4f, 5f, 6L, true).scaleWithDensity(2f).pressed)
        assertNull(Touch(1f, 2f, 3f, 4f, 5f, 6L, null).scaleWithDensity(2f).pressed)
    }

    @Test
    fun toStringContainsPressed() {
        assertTrue(Touch(1f, 2f, 3f, 4f, 5f, 6L, false).toString().contains("pressed:false"))
    }
}
