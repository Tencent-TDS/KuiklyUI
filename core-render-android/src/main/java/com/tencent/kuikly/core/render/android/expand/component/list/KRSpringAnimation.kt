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

internal class KRSpringAnimation(
    startValue: Float,
    val endValue: Float,
    velocity: Float,
    val stiffness: Float,
    dampingRatio: Float
): KRScrollAnimation() {
    private var currentValue = startValue
    private var currentVelocity = velocity
    private var lastTimeNanos = 0L
    private val mass = 1f
    // c = 2 * m * sqrt(k/m) * zeta = 2 * sqrt(m*k) * zeta
    private val dampingCoefficient = 2f * kotlin.math.sqrt((mass * stiffness).toDouble()).toFloat() * dampingRatio

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 100000L // Long enough
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            // 对齐鸿蒙 SpringNowUs()：使用单调时钟，避免墙钟受系统时间调整影响导致 dt 跳变
            val currentTimeNanos = System.nanoTime()
            if (lastTimeNanos == 0L) {
                lastTimeNanos = currentTimeNanos
                return@addUpdateListener
            }
            var dt = (currentTimeNanos - lastTimeNanos) / 1_000_000_000f
            lastTimeNanos = currentTimeNanos

            // Cap dt to avoid instability in case of long pauses (e.g. backgrounding)
            if (dt > 0.064f) dt = 0.064f

            // Semi-implicit Euler integration
            // F = -k * x - c * v
            val displacement = currentValue - endValue
            val force = -stiffness * displacement - dampingCoefficient * currentVelocity
            val acceleration = force / mass

            currentVelocity += acceleration * dt
            currentValue += currentVelocity * dt

            onUpdate(currentValue)

            // End condition: close enough and slow enough
            if (kotlin.math.abs(currentValue - endValue) < 0.5f && kotlin.math.abs(currentVelocity) < 10f) {
                cancel()
                onUpdate(endValue)
            }
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
        lastTimeNanos = 0L
        animator.start()
    }

    override fun cancel() {
        animator.cancel()
    }
}