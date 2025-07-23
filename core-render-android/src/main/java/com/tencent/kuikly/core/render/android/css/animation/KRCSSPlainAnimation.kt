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

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.Animator
import android.animation.TimeInterpolator
import android.graphics.Rect
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.tencent.kuikly.core.render.android.css.ktx.frame
import com.tencent.kuikly.core.render.android.css.ktx.hrBackgroundColor
import com.tencent.kuikly.core.render.android.css.ktx.toColor
import com.tencent.kuikly.core.render.android.css.ktx.toNumberFloat

/**
 * frame属性动画处理器
 */
class KRCSSPlainFrameAnimationHandler : KRCSSPlainAnimationHandler() {

    override fun createValueAnimator(target: View): ValueAnimator =
        ValueAnimator.ofObject(FrameTypeEvaluator(target), target.frame, finalValue as Rect)

}

/**
 * alpha属性动画处理器
 */
class KRCSSPlainOpacityAnimationHandler : KRCSSPlainAnimationHandler() {

    override fun createValueAnimator(target: View): ValueAnimator = ObjectAnimator.ofFloat(
        target,
        View.ALPHA.name,
        target.alpha,
        finalValue?.toNumberFloat() ?: DEFAULT_ALPHA
    )

    companion object {
        private const val DEFAULT_ALPHA = 1f
    }

}

/**
 * transform动画处理器
 */
class KRCSSPlainTransformAnimationHandler : KRCSSPlainAnimationHandler() {

    override fun createValueAnimator(target: View): ValueAnimator {
        return ValueAnimator.ofObject(
            TransformTypeEvaluator(target),
            target.transform,
            KRCSSTransform(finalValue as String, target))
    }

}

/**
 * 背景颜色属性动画处理器
 */
class KRCSSPlainBackgroundColorAnimationHandler : KRCSSPlainAnimationHandler() {

    override fun createValueAnimator(target: View): ValueAnimator {
        return ValueAnimator.ofObject(
            BackgroundColorTypeEvaluator(target),
            target.hrBackgroundColor,
            (finalValue as String).toColor())
    }
}

/**
 * 属性动画处理器基类
 */
abstract class KRCSSPlainAnimationHandler : KRCSSAnimationHandler() {

    var timingFuncType = TIMING_FUNC_TYPE_LINEAR
    private var objectAnimator: ValueAnimator? = null
    private var isCancel = false

    override fun start(target: View, onAnimationEndBlock: () -> Unit) {
        objectAnimator = createValueAnimator(target).apply {
            interpolator = createInterpolator()
            duration = (durationS * UNIT_S_TO_MS).toLong()
            startDelay = (delay * UNIT_S_TO_MS).toLong()
            if (repeatForever) {
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
            }
        }
        objectAnimator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!isCancel) {
                    onAnimationEndBlock.invoke()
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                isCancel = true
            }

        })
        isCancel = false
        objectAnimator?.start()
    }

    override fun cancel() {
        objectAnimator?.cancel()
    }

    /**
     * 子类创建对应的属性动画类
     * @param target 应用动画的View
     * @return 属性动画
     */
    abstract fun createValueAnimator(target: View): ValueAnimator

    private fun createInterpolator(): TimeInterpolator {
        return when (timingFuncType) {
            TIMING_FUNC_TYPE_ACCELERATE -> AccelerateInterpolator()
            TIMING_FUNC_TYPE_DECELERATE -> DecelerateInterpolator()
            TIMING_FUNC_TYPE_ACCELERATE_DECELERATE -> AccelerateDecelerateInterpolator()
            else -> LinearInterpolator()
        }
    }

    companion object {
        private const val TIMING_FUNC_TYPE_LINEAR = 0
        private const val TIMING_FUNC_TYPE_ACCELERATE = 1
        private const val TIMING_FUNC_TYPE_DECELERATE = 2
        private const val TIMING_FUNC_TYPE_ACCELERATE_DECELERATE = 3

        private const val UNIT_S_TO_MS = 1000 // s->ms单位转换
    }
}
