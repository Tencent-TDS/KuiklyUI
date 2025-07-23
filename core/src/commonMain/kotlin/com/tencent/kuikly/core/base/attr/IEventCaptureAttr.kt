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

package com.tencent.kuikly.core.base.attr

import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

interface IEventCaptureAttr {
    companion object {
        const val CAPTURE = "capture"
    }
    fun capture(vararg rule: CaptureRule?)
}

class CaptureRule(
    val type: CaptureRuleType,
    val area: Frame?,
    val direction: Int = CaptureRuleDirection.ALL
) {
    companion object {
        fun click(area: Frame? = null) = CaptureRule(CaptureRuleType.CLICK, area)
        fun doubleClick(area: Frame? = null) = CaptureRule(CaptureRuleType.DOUBLE_CLICK, area)
        fun longPress(area: Frame? = null) = CaptureRule(CaptureRuleType.LONG_PRESS, area)
        fun pan(direction: Int = CaptureRuleDirection.ALL, area: Frame? = null) =
            CaptureRule(CaptureRuleType.PAN, area, direction)
    }

    fun encode(): JSONObject {
        return JSONObject().apply {
            put("type", type.value)
            if (area != null) {
                put("area", JSONObject().apply {
                    put("x", area.x)
                    put("y", area.y)
                    put("width", area.width)
                    put("height", area.height)
                })
            }
            if (direction != CaptureRuleDirection.ALL) {
                put("direction", direction)
            }
        }
    }

}

enum class CaptureRuleType(val value: Int) {
    CLICK(1),
    DOUBLE_CLICK(2),
    LONG_PRESS(3),
    PAN(4)
}

object CaptureRuleDirection {
    const val TO_LEFT = 1
    const val TO_TOP = 1 shl 1
    const val TO_RIGHT = 1 shl 2
    const val TO_BOTTOM = 1 shl 3
    const val HORIZONTAL = TO_LEFT or TO_RIGHT
    const val VERTICAL = TO_TOP or TO_BOTTOM
    const val ALL = HORIZONTAL or VERTICAL
}