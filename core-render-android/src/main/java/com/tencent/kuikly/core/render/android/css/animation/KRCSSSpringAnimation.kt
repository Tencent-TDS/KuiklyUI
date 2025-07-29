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

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.tencent.kuikly.core.render.android.css.ktx.hrBackgroundColor
import com.tencent.kuikly.core.render.android.css.ktx.frame
import com.tencent.kuikly.core.render.android.css.ktx.toColor
import com.tencent.kuikly.core.render.android.css.ktx.toNumberFloat

/**
 * Frame弹性动画处理器
 */
class KRCSSSpringFrameAnimationHandler : KRCSSSpringAnimationHandler() {

    override fun createSpringProperty(target: View): FloatPropertyCompat<View> {
        return object : FloatPropertyCompat<View>("Frame") {

            private val startFrame = target.frame
            private val endFrame = finalValue as Rect
            private val frameEvaluator = FrameTypeEvaluator(target)

            override fun getValue(target: View): Float {
                return 0f
            }

            override fun setValue(target: View, value: Float) {
                frameEvaluator.evaluate(value, startFrame, endFrame)
            }
        }
    }

    override val finalFloatValue: Float
        get() = 1f

}

/**
 * backgroundColor弹性动画处理器
 */
class KRCSSSpringBackgroundColorAnimationHandler : KRCSSSpringAnimationHandler() {

    override fun createSpringProperty(target: View): FloatPropertyCompat<View> {
        return object : FloatPropertyCompat<View>("hrBackgroundColor") {

            private val startColor = target.hrBackgroundColor
            private val endColor = (finalValue as String).toColor()
            private val argbEvaluator = BackgroundColorTypeEvaluator(target)

            override fun getValue(target: View): Float = 0f

            override fun setValue(target: View, value: Float) {
                argbEvaluator.evaluate(value, startColor, endColor)
            }
        }
    }

    override val finalFloatValue: Float
        get() = 1f

}

/**
 * transform弹性动画处理器
 */
class KRCSSSpringTransformAnimationHandler : KRCSSSpringAnimationHandler() {

    override fun createSpringProperty(target: View): FloatPropertyCompat<View> {
        return object : FloatPropertyCompat<View>("transform") {

            val startTransform = target.transform
            val endTransform = KRCSSTransform(finalValue as String, target)
            val transformTypeEvaluator = TransformTypeEvaluator(target)

            override fun getValue(target: View): Float = 0f

            override fun setValue(target: View, value: Float) {
                transformTypeEvaluator.evaluate(value, startTransform, endTransform)
            }
        }
    }

    override val finalFloatValue: Float
        get() = 1f

}

/**
 * alpha弹性动画处理器
 */
class KRCSSSpringOpacityAnimationHandler : KRCSSSpringAnimationHandler() {

    override fun createSpringProperty(target: View): FloatPropertyCompat<View> =
        SpringAnimation.ALPHA

    override val finalFloatValue: Float
        get() = finalValue?.toNumberFloat() ?: 1f

}

/**
 * 弹性动画处理器基类
 */
abstract class KRCSSSpringAnimationHandler : KRCSSAnimationHandler() {

    var damping = 0f
    var velocity = 0f

    private var springAnimation: SpringAnimation? = null
    private var uiHandler: Handler? = null

    override fun start(target: View, onAnimationEndBlock: () -> Unit) {
        springAnimation = SpringAnimation(target, createSpringProperty(target), finalFloatValue)
            .apply {
                setStartVelocity(velocity)
                spring.dampingRatio =
                    if (damping != 0f) damping else SpringForce.DAMPING_RATIO_NO_BOUNCY
                minimumVisibleChange = DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA
                addEndListener { _, canceled, _, _ ->
                    if (!canceled) {
                        if (repeatForever) {
                            spring.finalPosition = finalFloatValue
                            start()
                        } else {
                            onAnimationEndBlock.invoke()
                        }
                    }
                }
            }
        if (delay > 0) {
            if (uiHandler == null) {
                uiHandler = Handler(Looper.getMainLooper())
            }
            uiHandler?.postDelayed({
                springAnimation?.start()
            }, (delay * UNIT_S_TO_MS).toLong())
        } else {
            springAnimation?.start()
        }
    }

    override fun cancel() {
        uiHandler?.removeCallbacksAndMessages(null)
        springAnimation?.cancel()
    }

    /**
     * 子类实现SpringProperty
     */
    abstract fun createSpringProperty(target: View): FloatPropertyCompat<View>

    /**
     * 子类指定的最后进度值
     */
    abstract val finalFloatValue: Float

    companion object {
        private const val UNIT_S_TO_MS = 1000 // s->ms单位转换
    }

}
