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

open class TextShadow(private val pagerId: String, viewRef: Int, viewName: String) : Shadow(
    pagerId, viewRef,
    viewName
) {
    var isDirty = true
        private set
    private var lastWidth: Float? = null
    private var lastHeight: Float? = null
    private var lastSize: Size? = null
    private var lastLayoutWidth: Float? = null
    private var lastLayoutSize: Size? = null
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
        lastLayoutWidth = null
        lastLayoutSize = null
        val size = super.calculateRenderViewSize(width, height)
        markNotDirty()
        lastWidth = width
        lastHeight = height
        lastSize = size
        return size
    }

    /**
     * Finish OHOS text layout at the resolved content width before returning from measure.
     * Keep this result separate from the intrinsic measurement cache: changing paragraph
     * width must not replace the height cached for [lastWidth]. Older renderers return null
     * and continue to fit their text when drawing.
     */
    fun layoutAtWidth(width: Float): Size? {
        if (!PagerManager.getPager(pagerId).pageData.isOhOs ||
            !width.isFinite() || width <= 0f || isDirty || lastSize == null
        ) {
            return null
        }
        if (lastLayoutWidth == width && lastLayoutSize != null) {
            return lastLayoutSize
        }
        val parts = callMethod("layoutAtWidth", width.toString()).split('|')
        // Publish after a real measure even if an intrinsic query populated the cache first.
        // This also covers older OHOS renderers and the StyledString path.
        if (parts.size != 3) {
            calculateFromCache = false
            return null
        }
        val layoutHeight = parts[2].toFloatOrNull() ?: return null
        if (!layoutHeight.isFinite() || layoutHeight < 0f) {
            return null
        }
        if (parts[0] == "1") {
            calculateFromCache = false
        }
        // The caller owns the resolved width; do not shrink it again to longestLine.
        val size = Size(width, layoutHeight)
        lastLayoutWidth = width
        lastLayoutSize = size
        return size
    }

    fun markDirty() {
        if (isDirty) {
            return
        }
        isDirty = true
        lastSize = null
        lastLayoutWidth = null
        lastLayoutSize = null
    }

    private fun markNotDirty() {
        isDirty = false
    }
}