/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.PointerEvent
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventType
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.log.KLog

/**
 * 操作步骤：
 * 1. 按住下方区域并轻微移动第一根手指；
 * 2. 按下第二根手指；
 * 3. 抬起第二根手指，同时保持第一根手指按下；
 * 4. 继续移动第一根手指，最后将其抬起。
 *
 * 预期结果：第二根手指抬起时，第一根手指仍处于按下状态；异常计数始终为 0。
 *
 * 异常判定：在非 Press 事件（Move 或 Release）中，某个触点从上一事件的未按下状态变为当前事件的按下状态。
 * 这说明此前事件错误地将仍按下的触点标记为已抬起，导致 Compose 收到不一致的触点状态。
 */
@Page("MultiTouchPointerStateDemo")
class MultiTouchPointerStateDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("多指按压状态诊断") {
                MultiTouchPointerDiagnostics()
            }
        }
    }
}

@Composable
private fun MultiTouchPointerDiagnostics() {
    var eventCount by remember { mutableStateOf(0) }
    var unexpectedPressCount by remember { mutableStateOf(0) }
    val livePointers = remember { mutableStateMapOf<Long, Boolean>() }
    val frames = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "1）按住并移动第一根手指 → 2）按下第二根手指 → 3）抬起第二根手指，第一根保持按下 → 4）移动并抬起第一根手指",
            fontSize = 14.sp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFFE8F1FF))
                .border(2.dp, Color(0xFF3A7BD5))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            eventCount += 1
                            if (event.hasUnexpectedPressTransition()) {
                                unexpectedPressCount += 1
                            }
                            // 已抬起的触点只保留到下一个事件，避免列表持续增长。
                            livePointers.entries.removeAll { !it.value }
                            event.changes.forEach { change ->
                                livePointers[change.id.value] = change.pressed
                            }
                            val summary = event.describe(eventCount)
                            KLog.i(TAG, summary)
                            frames.add(0, summary)
                            while (frames.size > 12) {
                                frames.removeAt(frames.lastIndex)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "多指触摸区域", fontSize = 20.sp, color = Color(0xFF3A7BD5))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "事件数: $eventCount", fontSize = 14.sp)
            Text(
                text = "非按下事件误报按下状态: $unexpectedPressCount",
                fontSize = 14.sp,
                color = if (unexpectedPressCount == 0) Color(0xFF1B8A3A) else Color(0xFFC62828),
            )
        }

        val pointerText = if (livePointers.isEmpty()) {
            "当前触点: 无"
        } else {
            "当前触点: " + livePointers.entries.joinToString { "id=${it.key} ${if (it.value) "按下" else "抬起"}" }
        }
        Text(text = pointerText, fontSize = 14.sp)

        Text(text = "最近事件（新→旧）：", fontSize = 14.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            frames.forEach { frame ->
                Text(text = frame, fontSize = 11.sp, color = Color(0xFF444444))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 生成每个事件中各触点当前及上一事件的按下状态摘要。 */
private fun PointerEvent.describe(index: Int): String = buildString {
    append("事件 ").append(index).append(' ').append(type.toDiagnosticName())
    changes.forEach { change ->
        append(" | 触点 id=").append(change.id.value)
            .append(" 当前按下=").append(change.pressed)
            .append(" 上一事件按下=").append(change.previousPressed)
    }
}

private fun PointerEventType.toDiagnosticName(): String = when (this) {
    PointerEventType.Press -> "按下"
    PointerEventType.Move -> "移动"
    PointerEventType.Release -> "抬起"
    else -> toString()
}

private fun PointerEvent.hasUnexpectedPressTransition(): Boolean =
    type != PointerEventType.Press && changes.any { it.pressed && !it.previousPressed }

private const val TAG = "MultiTouchDemo"
