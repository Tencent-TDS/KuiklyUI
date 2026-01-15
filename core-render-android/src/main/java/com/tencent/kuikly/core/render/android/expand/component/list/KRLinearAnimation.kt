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

package com.tencent.kuikly.core.render.android.expand.component.list

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

/**
 * 线性动画，用于匀速滚动
 *
 * @param startValue 起始值
 * @param endValue 结束值
 * @param duration 动画时长
 */
internal class KRLinearAnimation(
    private val startValue: Float,
    private val endValue: Float,
    duration: Long
): KRScrollAnimation() {
    private val animator = ValueAnimator.ofFloat(startValue, endValue).apply {
        this.duration = duration
        interpolator = LinearInterpolator()

        addUpdateListener { animation ->
            val currentValue = animation.animatedValue as Float
            onUpdate(currentValue)
        }

        addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationCancel(animation: android.animation.Animator) {
                onEnd()
            }

            override fun onAnimationEnd(animation: android.animation.Animator) {
                onEnd()
            }

            override fun onAnimationRepeat(animation: android.animation.Animator) {}
            override fun onAnimationStart(animation: android.animation.Animator) {}
        })
    }

    override var onUpdate: (Float) -> Unit = {}
    override var onEnd: () -> Unit = {}

    override fun start() {
        animator.start()
    }

    override fun cancel() {
        animator.cancel()
    }
}