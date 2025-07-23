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

package com.tencent.kuikly.core.render.android.css.animation

import android.animation.ArgbEvaluator
import android.animation.RectEvaluator
import android.animation.TypeEvaluator
import android.graphics.Rect
import android.view.View
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.css.ktx.setCommonProp
import com.tencent.kuikly.core.render.android.css.ktx.viewDecorator

/**
 * Frame动画转换器，evaluate方法接收start和end值，并配合[android.animation.RectEvaluator]来进行Frame动画转换
 * @property targetView 应用Frame动画的View
 */
internal class FrameTypeEvaluator(private val targetView: View) : TypeEvaluator<Rect> {

    private val frameEvaluator = RectEvaluator()

    override fun evaluate(fraction: Float, startValue: Rect, endValue: Rect): Rect {
        val newFrame = frameEvaluator.evaluate(fraction, startValue, endValue)
        targetView.setCommonProp(KRCssConst.FRAME, newFrame)
        return newFrame
    }

}

/**
 * 背景颜色动画转换器，evaluate方法接收start和end值，并配合[android.animation.ArgbEvaluator]来进行背景颜色动画转换
 * @property targetView 应用Frame动画的View
 */
internal class BackgroundColorTypeEvaluator(private val targetView: View) : TypeEvaluator<Int> {

    private val argbEvaluator = ArgbEvaluator()

    override fun evaluate(fraction: Float, startValue: Int?, endValue: Int?): Int {
        val newColor = argbEvaluator.evaluate(fraction, startValue, endValue) as Int
        targetView.viewDecorator?.backgroundColor = newColor
        return newColor
    }

}

/**
 * Transform动画转换器，evaluate方法接收start和end值，并配合[KRCSSTransform]来进行背景颜色动画转换
 */
internal class TransformTypeEvaluator(private val targetView: View) : TypeEvaluator<KRCSSTransform> {

    private val reuseTransform = KRCSSTransform(null, targetView)

    override fun evaluate(
        fraction: Float,
        startValue: KRCSSTransform,
        endValue: KRCSSTransform
    ): KRCSSTransform {
        reuseTransform.rotate = startValue.rotate + (endValue.rotate - startValue.rotate) * fraction
        reuseTransform.rotateX = startValue.rotateX + (endValue.rotateX - startValue.rotateX) * fraction
        reuseTransform.rotateY = startValue.rotateY + (endValue.rotateY - startValue.rotateY) * fraction
        reuseTransform.scaleX = startValue.scaleX + (endValue.scaleX - startValue.scaleX) * fraction
        reuseTransform.scaleY = startValue.scaleY + (endValue.scaleY - startValue.scaleY) * fraction
        reuseTransform.translateX = startValue.translateX + (endValue.translateX - startValue.translateX) * fraction
        reuseTransform.translateY = startValue.translateY + (endValue.translateY - startValue.translateY) * fraction
        reuseTransform.pivotX = startValue.pivotX + (endValue.pivotX - startValue.pivotX) * fraction
        reuseTransform.pivotY = startValue.pivotY + (endValue.pivotY - startValue.pivotY) * fraction
        targetView.setCommonProp(KRCssConst.TRANSFORM, reuseTransform)
        return reuseTransform
    }

}
