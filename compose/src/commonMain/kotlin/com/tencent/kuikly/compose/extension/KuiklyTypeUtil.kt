/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.kuikly.compose.ui.draw.ShadowGraphicsLayerElement
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.core.layout.Frame

/**
 * 判断2个Frame是否相等，精度为int
 * @receiver Frame
 * @param other Frame
 * @return Boolean
 */
internal fun Frame.intEqual(other: Frame): Boolean {
    return this.x.toInt() == other.x.toInt() &&
            y.toInt() == other.y.toInt() &&
            width.toInt() == other.width.toInt() &&
            height.toInt() == other.height.toInt()
}

/**
 * @receiver Frame kuikly的Frame dp单位
 * @param density Float
 * @return IntRect compose的Rect，像素单位
 */
internal fun Frame.toIntRect(density: Float): IntRect {
    return IntRect(
        IntOffset((this.x * density).toInt(), (this.y * density).toInt()),
        IntSize((this.width * density).toInt(), (this.height * density).toInt())
    )
}

fun shouldWrapShadowView(modifier: Modifier): Boolean {
    var hasShadow = false
    modifier.foldIn(Unit) { acc, element ->
        when (element) {
            is SetPropElement -> {
                hasShadow = hasShadow || (element.key == "boxShadow")
            }
            is ShadowGraphicsLayerElement -> {
                hasShadow = true
            }
        }
    }
    return hasShadow
}

