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

package com.tencent.kuikly.core.module

import com.tencent.kuikly.core.manager.PagerManager

class FontModule : Module() {

    private val fontFitResultCacheMap: HashMap<Float, Float> by lazy(LazyThreadSafetyMode.NONE) {
        HashMap<Float, Float>()
    }

    override fun moduleName(): String {
        return MODULE_NAME
    }

    fun scaleFontSize(fontSize: Float): Float {
        var resFontSize = fontFitResultCacheMap[fontSize]
        if (resFontSize != null) {
            return resFontSize
        }
        resFontSize = (syncToNativeMethod(SCALE_FONT_SIZE_METHOD, arrayOf<Any>(fontSize), {}) as? String)?.toFloat()
        resFontSize?.also {
            fontFitResultCacheMap[fontSize] = it
        }
        return resFontSize ?: fontSize
    }

    companion object {
        const val MODULE_NAME = ModuleConst.FONT
        const val SCALE_FONT_SIZE_METHOD = "scaleFontSize"

        fun scaleFontSize(fontSize: Float, scaleFontSizeEnable: Boolean? = null): Float {
            val pager = PagerManager.getCurrentPager()
            var enable = pager.scaleFontSizeEnable()
            if (scaleFontSizeEnable != null) {
                enable = scaleFontSizeEnable
            }
            if (!enable || pager.pageData.nativeBuild < 3) {
                return fontSize
            }
            return pager.acquireModule<FontModule>(FontModule.MODULE_NAME).scaleFontSize(fontSize)
        }

    }
}