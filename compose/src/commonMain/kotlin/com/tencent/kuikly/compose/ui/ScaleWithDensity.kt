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

package com.tencent.kuikly.compose.ui

import com.tencent.kuikly.core.base.event.Touch
import com.tencent.kuikly.core.views.ScrollParams
import com.tencent.kuikly.core.views.WillEndDragParams

/**
 * 触摸点参数密度缩放扩展
 */
fun Touch.scaleWithDensity(density: Float): Touch {
    return Touch(
        x = x * density,
        y = y * density,
        pageX = pageX * density,
        pageY = pageY * density,
        hash = hash,
        pointerId = pointerId
    )
}

/**
 * 滚动事件参数密度缩放扩展
 */
fun ScrollParams.scaleWithDensity(density: Float): ScrollParams {
    return ScrollParams(
        offsetX = offsetX * density,
        offsetY = offsetY * density,
        contentWidth = contentWidth * density,
        contentHeight = contentHeight * density,
        viewWidth = viewWidth * density,
        viewHeight = viewHeight * density,
        isDragging = isDragging,
        touches = touches.map { it.scaleWithDensity(density) }
    )
}

/**
 * 拖拽结束事件参数密度缩放扩展
 */
fun WillEndDragParams.scaleWithDensity(density: Float): WillEndDragParams {
    return WillEndDragParams(
        offsetX = offsetX * density,
        offsetY = offsetY * density,
        contentWidth = contentWidth * density,
        contentHeight = contentHeight * density,
        viewWidth = viewWidth * density,
        viewHeight = viewHeight * density,
        isDragging = isDragging,
        velocityX = velocityX,
        velocityY = velocityY,
        targetContentOffsetX = targetContentOffsetX * density,
        targetContentOffsetY = targetContentOffsetY * density
    )
}