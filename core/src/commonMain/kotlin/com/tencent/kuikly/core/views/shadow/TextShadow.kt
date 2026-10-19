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

package com.tencent.kuikly.core.views.shadow

import com.tencent.kuikly.core.base.Shadow
import com.tencent.kuikly.core.base.Size
import kotlin.math.abs

open class TextShadow(pagerId: String, viewRef: Int, viewName: String) : Shadow(
    pagerId, viewRef,
    viewName
) {
    var isDirty = true
        private set
    private var lastWidth: Float? = null
    private var lastHeight: Float? = null
    private var lastSize: Size? = null
    internal var calculateFromCache = false
        private set

    override fun setProp(key: String, value: Any) {
        super.setProp(key, value)
        markDirty()
    }

    override fun calculateRenderViewSize(width: Float, height: Float): Size {
        if (!isDirty
            && lastWidth == width
            && lastHeight == height
            && lastSize != null
        ) {
            calculateFromCache = true
            return lastSize!!
        }
        calculateFromCache = false
        val size = super.calculateRenderViewSize(width, height)
        markNotDirty()
        lastWidth = width
        lastHeight = height
        lastSize = size
        return size
    }

    /**
     * OHOS V1：按布局完成后的节点宽创建新的 Typography，避免在绘制期对同一对象
     * 再次 Layout。返回 null 表示无需重建；非 null 值表示重建后文本高度是否变化。
     */
    fun relayoutToWidthIfNeeded(nodeWidth: Float): Boolean? {
        if (nodeWidth <= 0f) {
            return null
        }
        val sizeParts = callMethod(SHADOW_METHOD_RELAYOUT_TO_WIDTH, nodeWidth.toString()).split('|')
        if (sizeParts.size != 2) {
            return null
        }
        val relayoutWidth = sizeParts[0].toFloatOrNull() ?: return null
        val relayoutHeight = sizeParts[1].toFloatOrNull() ?: return null
        val cachedSize = lastSize
        val heightChanged = cachedSize != null &&
            abs(cachedSize.height - relayoutHeight) > LAYOUT_SIZE_EPSILON
        lastSize = Size(cachedSize?.width ?: relayoutWidth, relayoutHeight)
        return heightChanged
    }

    fun markDirty() {
        if (isDirty) {
            return
        }
        isDirty = true
        lastSize = null
    }

    private fun markNotDirty() {
        isDirty = false
    }

    companion object {
        private const val LAYOUT_SIZE_EPSILON = 0.01f
        private const val SHADOW_METHOD_RELAYOUT_TO_WIDTH = "relayoutToWidth"
    }
}