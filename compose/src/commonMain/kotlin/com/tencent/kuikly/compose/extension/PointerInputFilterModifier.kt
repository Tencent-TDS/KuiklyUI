/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

package com.tencent.kuikly.compose.extension

import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.input.pointer.PointerEvent
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventPass
import com.tencent.kuikly.compose.ui.input.pointer.PointerInputFilter
import com.tencent.kuikly.compose.ui.input.pointer.PointerInputModifier
import com.tencent.kuikly.compose.ui.input.pointer.isOutOfBounds
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.util.fastAll

typealias OnTouchEvent = (PointerEvent) -> Unit
typealias OnTouchCancel = () -> Unit

/**
 * 元素接收未消费事件但不消费事件，且未消费事件分发给兄弟节点。
 *
 * @param pass 监听的派发阶段
 * @param shareWithSiblings 是否允许将未消费事件分发给兄弟节点，而不是直接给父节点
 * @param onTouchEvent 触摸事件回调，可在回调中按需消费事件
 * @param onTouchCancel 触摸取消回调
 */
fun Modifier.pointerInputFilter(
    pass: PointerEventPass = PointerEventPass.Final,
    shareWithSiblings: Boolean = true,
    onTouchEvent: OnTouchEvent,
    onTouchCancel: OnTouchCancel = {}
): Modifier = this.then(UnConsumedPointerInputModifier(pass, shareWithSiblings, onTouchEvent, onTouchCancel))

/**
 * 元素接收未消费事件但不消费事件，且未消费事件分发给兄弟节点。
 */
private class UnConsumedPointerInputModifier(
    private val pass: PointerEventPass = PointerEventPass.Final,
    private val shareWithSiblings: Boolean = true,
    private val onTouchEvent: OnTouchEvent,
    private val onTouchCancel: OnTouchCancel
) : PointerInputModifier {

    override val pointerInputFilter: PointerInputFilter =
        UnConsumedPointerInputFilter(pass, shareWithSiblings, onTouchEvent, onTouchCancel)
}

private class UnConsumedPointerInputFilter(
    private val pass: PointerEventPass = PointerEventPass.Final,
    // 允许将未消费事件分发给兄弟节点，而不是直接给父节点
    override val shareWithSiblings: Boolean = true,
    private val onTouchEvent: OnTouchEvent,
    private val onTouchCancel: OnTouchCancel
) : PointerInputFilter() {

    override fun onCancel() {
        onTouchCancel()
    }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        // 不在当前元素内
        if (pointerEvent.changes.fastAll { it.isOutOfBounds(bounds, Size.Zero) }) {
            return
        }
        if (this.pass != pass) {
            return
        }
        onTouchEvent(pointerEvent)
    }
}
