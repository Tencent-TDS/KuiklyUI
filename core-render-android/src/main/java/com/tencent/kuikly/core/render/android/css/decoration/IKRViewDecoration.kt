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

package com.tencent.kuikly.core.render.android.css.decoration

import android.graphics.Canvas
import androidx.annotation.UiThread

/**
 * 绘制KTV页面View的通用样式
 */
interface IKRViewDecoration {

    /**
     * 绘制KTV页面下的View的通用样式，支持以下样式
     * 1. 圆角
     * 2. 阴影
     * 3. 背景颜色
     * 4. 边框
     * 5. 渐变
     * @param w view的宽度
     * @param h view的高度
     * @param canvas 画布
     */
    @UiThread
    fun drawCommonDecoration(w: Int, h: Int, canvas: Canvas)

    /**
     * 绘制View的前景, 6.0以上系统默认支持，6.0以前使用Drawable绘制模拟
     * @param w view的宽度
     * @param h view的高度
     * @param canvas 画布
     */
    @UiThread
    fun drawCommonForegroundDecoration(w: Int, h: Int, canvas: Canvas)

}
