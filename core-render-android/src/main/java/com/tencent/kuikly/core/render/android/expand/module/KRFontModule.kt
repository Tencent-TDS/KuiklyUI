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

package com.tencent.kuikly.core.render.android.expand.module

import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback

/**
 * 字体模块，用于宿主自定义字体相关
 */
class KRFontModule: KuiklyRenderBaseModule() {

    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {
        if (method == SCALE_FONT_SIZE_METHOD) {
            return scaleFontSize(params)
        }
        return super.call(method, params, callback)
    }

    fun scaleFontSize(params: Any?): String? {
        val paramsArray = params as? Array<*>
        if (paramsArray?.isNotEmpty() == true) {
            val fontSize = paramsArray[0] as Float
            val resFontSize = KuiklyRenderAdapterManager.krFontAdapter?.scaleFontSize(fontSize) ?: fontSize
            return "${resFontSize}"
        }
        return null
    }

    companion object {
        const val MODULE_NAME = "KRFontModule"
        const val SCALE_FONT_SIZE_METHOD = "scaleFontSize"
    }

}