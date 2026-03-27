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

package com.tencent.kuikly.compose.foundation.text

import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.core.views.LengthLimitType

internal class MaxLengthElement(
    val maxLength: Int,
    val lengthLimitType: LengthLimitType,
) : Modifier.Element {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is MaxLengthElement &&
                maxLength == other.maxLength &&
                lengthLimitType == other.lengthLimitType
    }

    override fun hashCode(): Int = 31 * maxLength.hashCode() + lengthLimitType.hashCode()
}

internal class OnLimitChangeElement(
    val onLimitChange: (length: Int, limit: Boolean) -> Unit,
) : Modifier.Element {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is OnLimitChangeElement && onLimitChange === other.onLimitChange
    }

    override fun hashCode(): Int = onLimitChange.hashCode()
}

/**
 * 设置输入框最大输入长度限制
 * @param length 最大输入长度
 * @param type 长度限制类型，默认按字符计算（CHARACTER）
 * @see LengthLimitType
 */
fun Modifier.maxLength(
    length: Int,
    type: LengthLimitType = LengthLimitType.CHARACTER,
): Modifier = this.then(MaxLengthElement(length, type))

/**
 * 设置输入框长度变化/超限回调
 * 当字数变化或长度超过限制时回调
 * @param onLimitChange 回调函数，length 为当前文本长度，limit 为是否超限
 */
fun Modifier.onLimitChange(
    onLimitChange: (length: Int, limit: Boolean) -> Unit,
): Modifier = this.then(OnLimitChangeElement(onLimitChange))

internal fun Modifier.extractTextFieldMaxLengthElements(): Triple<Modifier, MaxLengthElement?, OnLimitChangeElement?> {
    var remaining: Modifier = Modifier
    var maxLengthElement: MaxLengthElement? = null
    var onLimitChangeElement: OnLimitChangeElement? = null

    foldOut(Unit) { element, _ ->
        when (element) {
            is MaxLengthElement -> {
                maxLengthElement = element
            }
            is OnLimitChangeElement -> {
                onLimitChangeElement = element
            }
            else -> {
                remaining = element.then(remaining)
            }
        }
    }

    return Triple(remaining, maxLengthElement, onLimitChangeElement)
}
