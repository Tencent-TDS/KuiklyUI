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

package com.tencent.kuikly.core.render.android.adapter

import android.graphics.Typeface
import android.util.DisplayMetrics
import com.tencent.kuikly.core.render.android.KuiklyContextParams

/**
 *字体适配器
 */
interface IKRFontAdapter {

    /**
     * 获取fontFamily对于的Typeface
     * @param fontFamily 字体名字
     * @param result 结果回调
     */
    fun getTypeface(fontFamily: String, result: (Typeface?) -> Unit)

    /**
     * 获取fontFamily对于的Typeface
     * @param fontFamily 字体名字
     * @param contextParams 页面打开时相关参数
     * @param result 结果回调
     */
    fun getTypeface(fontFamily: String, contextParams: KuiklyContextParams?, result: (Typeface?) -> Unit) {
        getTypeface(fontFamily, result)
    }

    /**
     * 根据设置的原字体大小返回合适的最终缩放大小尺寸
     * 注：若要启用该字体缩放，需要实现（override）Kotlin侧Pager#scaleFontSizeEnable接口返回YES
     */
    fun scaleFontSize(fontSize: Float): Float {
        return 1f
    }

    /**
     * 宿主可实现此方法来实现DisplayMetrics自定义
     * @param useHostDisplayMetrics 页面的单元转换是否由宿主外部决定
     */
    fun getDisplayMetrics(useHostDisplayMetrics: Boolean?): DisplayMetrics? {
        return null
    }

}
