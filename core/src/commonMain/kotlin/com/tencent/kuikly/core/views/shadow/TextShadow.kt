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
import com.tencent.kuikly.core.manager.PagerManager
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
     * OHOS V1：Yoga 最终节点宽若与第一次排版约束宽不同，在 context 线程按节点宽
     * 创建新 Typography。不要改 lastWidth 缓存，以免每帧都重新测自然宽。
     */
    fun relayoutToWidthIfNeeded(constraintWidth: Float, nodeWidth: Float) {
        val pager = PagerManager.getPager(pagerId)
        if (!pager.pageData.isOhOs) {
            return
        }
        if (nodeWidth <= 0f) {
            return
        }
        if (abs(nodeWidth - constraintWidth) <= LAYOUT_WIDTH_EPSILON_VP) {
            return
        }
        callMethod(SHADOW_METHOD_RELAYOUT_TO_WIDTH, nodeWidth.toString())
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
        private const val LAYOUT_WIDTH_EPSILON_VP = 1f
        private const val SHADOW_METHOD_RELAYOUT_TO_WIDTH = "relayoutToWidth"
    }
}