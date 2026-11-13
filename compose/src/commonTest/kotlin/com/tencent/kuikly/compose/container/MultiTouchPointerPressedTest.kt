/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 */

package com.tencent.kuikly.compose.container

import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventType
import com.tencent.kuikly.compose.ui.input.pointer.PointerInputEvent
import com.tencent.kuikly.compose.ui.input.pointer.SyntheticEventSender
import com.tencent.kuikly.core.base.event.Touch
import com.tencent.kuikly.compose.ui.scene.PointerInputEvent as scenePointerInputEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 按以下顺序模拟多指操作：id=0 按下、id=1 按下、id=1 抬起（id=0 仍按下）、id=0 移动、id=0 抬起，
 * 验证渲染层上报每个触点的 `pressed` 状态后 Compose 输入处理的结果，以及缺失该状态时的推断逻辑。
 */
@OptIn(ExperimentalComposeUiApi::class)
class MultiTouchPointerPressedTest {

    @Test
    fun perPointerPressedKeepsRemainingFingerDown() {
        val frames = feed(physicalSequence())

        assertTrue(
            frames.any { it.type == PointerEventType.Release && it.down == mapOf(0L to true, 1L to false) },
            "抬起 id=1 时，id=0 应仍处于按下状态",
        )
        assertTrue(
            frames.any { it.type == PointerEventType.Release && it.down[0L] == false },
            "id=0 最终应收到抬起状态",
        )
    }

    /** 未提供 pressed 状态时，单指点击仍应依据事件类型正确处理。 */
    @Test
    fun singleTapWorksWithoutPressed() {
        val frames = feed(
            listOf(
                PressFrame(PointerEventType.Press, listOf(touch(0, null))),
                PressFrame(PointerEventType.Release, listOf(touch(0, null))),
            )
        )
        assertTrue(frames.any { it.type == PointerEventType.Press && it.down[0L] == true })
        assertTrue(frames.any { it.type == PointerEventType.Release && it.down[0L] == false })
    }

    @Test
    fun touchActivityEndsOnlyAfterLastPointerIsReleased() {
        assertTrue(listOf(touch(0, false), touch(1, true)).hasPressedPointers())
        assertTrue(!listOf(touch(0, false), touch(1, null)).hasPressedPointers())
        assertTrue(!listOf(touch(0, null)).hasPressedPointers())
    }

    /** 同一事件中的部分触点未提供 pressed 状态时，各触点应分别使用上报值或事件类型推断。 */
    @Test
    fun missingPressedIsInferredForEachTouch() {
        val release = listOf(touch(0, true), touch(1, null))
            .toComposeScenePointers(PAGE_DENSITY, PointerEventType.Release)
        assertEquals(listOf(true, false), release.map { it.pressed })

        val move = listOf(touch(0, null), touch(1, false))
            .toComposeScenePointers(PAGE_DENSITY, PointerEventType.Move)
        assertEquals(listOf(true, false), move.map { it.pressed })
    }

    // ---- 测试辅助方法 ----

    private class PressFrame(val type: PointerEventType, val touches: List<Touch>)

    private class SentFrame(val type: PointerEventType, val down: Map<Long, Boolean>)

    private fun touch(pointerId: Long, pressed: Boolean?): Touch = Touch(
        x = pointerId * 100f,
        y = pointerId * 100f,
        pageX = pointerId * 100f,
        pageY = pointerId * 100f,
        hash = pointerId.toFloat(),
        pointerId = pointerId,
        pressed = pressed,
    )

    private fun physicalSequence(): List<PressFrame> = listOf(
        PressFrame(PointerEventType.Press, listOf(touch(0, true))),
        PressFrame(PointerEventType.Press, listOf(touch(0, true), touch(1, true))),
        PressFrame(PointerEventType.Release, listOf(touch(0, true), touch(1, false))),
        PressFrame(PointerEventType.Move, listOf(touch(0, true))),
        PressFrame(PointerEventType.Release, listOf(touch(0, false))),
    )

    private fun feed(frames: List<PressFrame>): List<SentFrame> {
        val sent = mutableListOf<PointerInputEvent>()
        val syntheticEventSender = SyntheticEventSender { sent.add(it) }
        var timeMillis = 1_000L
        frames.forEach { frame ->
            timeMillis += FRAME_INTERVAL_MILLIS
            syntheticEventSender.send(
                scenePointerInputEvent(
                    eventType = frame.type,
                    pointers = frame.touches.toComposeScenePointers(PAGE_DENSITY, frame.type),
                    timeMillis = timeMillis,
                    nativeEvent = null,
                    scrollDelta = Offset.Zero,
                    changedButton = null,
                    rootNode = null,
                )
            )
        }
        return sent.map { event ->
            SentFrame(event.eventType, event.pointers.associate { it.id.value to it.down })
        }
    }

    private companion object {
        const val PAGE_DENSITY = 2f
        const val FRAME_INTERVAL_MILLIS = 16L
    }
}
