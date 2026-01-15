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

import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

/**
 * 动画配置
 */
internal sealed class AnimationConfig {
    data class Spring(
        val duration: Int,
        val damping: Float,
        val velocity: Float,
        val isVertical: Boolean  // 用户滑动的方向，决定 velocity 应用到哪个轴
    ) : AnimationConfig() {
        companion object {
            /**
             * 计算弹簧刚度: stiffness = (2π / T)² * m, 其中 m = 1
             */
            fun calculateStiffness(durationMs: Int): Float {
                val durationSec = durationMs / 1000.0
                return (2 * PI / durationSec).pow(2).toFloat()
            }
        }
    }

    data class Linear(
        val duration: Int
    ) : AnimationConfig()
}

/**
 * 滚动动画管理器
 * 支持的动画类型：
 * - Spring 弹簧动画：基于物理模拟的弹性动画
 * - Linear 线性动画：匀速滚动动画
 */
internal class KRScrollAnimationManager(
    private val recyclerView: RecyclerView
) {
    // 当前运行的动画
    private var currentAnimation: KRScrollAnimation? = null
    
    // 记录当前滚动方向：true=水平，false=垂直
    private var isHorizontal: Boolean = false

    // 动画结束时的回调
    var onAnimationEnd: (() -> Unit)? = null

    /**
     * 检查是否有动画正在运行
     */
    fun hasRunningAnimation(): Boolean = currentAnimation != null

    /**
     * 取消动画
     */
    fun cancel() {
        currentAnimation?.cancel()
        currentAnimation = null
    }

    /**
     * 启动 Spring 弹簧动画
     */
    fun startSpringAnimation(
        dx: Int,
        dy: Int,
        duration: Int,
        damping: Float,
        velocity: Float,
        isVertical: Boolean,
        onScrollStateChange: (Int) -> Unit
    ) {
        startAnimation(
            dx, dy,
            AnimationConfig.Spring(duration, damping, velocity, isVertical),
            onScrollStateChange
        )
    }

    /**
     * 启动 Linear 线性动画
     */
    fun startLinearAnimation(
        dx: Int,
        dy: Int,
        duration: Int,
        onScrollStateChange: (Int) -> Unit
    ) {
        startAnimation(
            dx, dy,
            AnimationConfig.Linear(duration),
            onScrollStateChange
        )
    }

    /**
     * 动画启动入口
     */
    private fun startAnimation(
        dx: Int,
        dy: Int,
        config: AnimationConfig,
        onScrollStateChange: (Int) -> Unit
    ) {
        // 取消之前的动画
        cancel()

        // 边界检查
        if (dx == 0 && dy == 0) return

        val layoutManager = recyclerView.layoutManager ?: return
        
        val actualDx = if (layoutManager.canScrollHorizontally()) dx else 0
        val actualDy = if (layoutManager.canScrollVertically()) dy else 0

        if (actualDx == 0 && actualDy == 0) return

        // 确定滚动方向和距离
        val (distance, horizontal) = if (actualDx != 0) {
            actualDx to true
        } else {
            actualDy to false
        }

        isHorizontal = horizontal
        // 设置滚动状态
        onScrollStateChange(RecyclerView.SCROLL_STATE_SETTLING)
        // 启动嵌套滚动
        startNestedScrollIfNeeded(actualDx, actualDy)
        // 创建并启动动画
        val animation = when (config) {
            is AnimationConfig.Spring -> createSpringAnimation(distance, config)
            is AnimationConfig.Linear -> createLinearAnimation(distance, config)
        }
        
        setupAndStartAnimation(animation)
    }

    /**
     * 创建 Spring 动画实例
     */
    private fun createSpringAnimation(
        distance: Int,
        config: AnimationConfig.Spring
    ): KRSpringAnimation {
        val stiffness = AnimationConfig.Spring.calculateStiffness(config.duration)
        
        // 判断是否应该对当前方向应用速度
        val shouldUseVelocity = (isHorizontal && !config.isVertical) || (!isHorizontal && config.isVertical)
        
        return KRSpringAnimation(
            startValue = 0f,
            endValue = distance.toFloat(),
            velocity = if (shouldUseVelocity) config.velocity else 0f,
            stiffness = stiffness,
            dampingRatio = config.damping
        )
    }

    /**
     * 创建 Linear 动画实例
     */
    private fun createLinearAnimation(
        distance: Int,
        config: AnimationConfig.Linear
    ): KRLinearAnimation {
        return KRLinearAnimation(
            startValue = 0f,
            endValue = distance.toFloat(),
            duration = config.duration.toLong()
        )
    }

    /**
     * 动画设置和启动逻辑
     */
    private fun setupAndStartAnimation(animation: KRScrollAnimation) {
        var consumed = 0f

        animation.onUpdate = { value ->
            if (currentAnimation === animation) {
                val delta = value - consumed
                val intDelta = delta.toInt()
                
                if (intDelta != 0) {
                    scrollRecyclerView(intDelta)
                    consumed += intDelta
                }
            }
        }

        animation.onEnd = {
            if (currentAnimation === animation) {
                currentAnimation = null
                onAnimationEnd?.invoke()
            }
        }

        currentAnimation = animation
        animation.start()
    }

    /**
     * 滚动 RecyclerView
     * 根据当前滚动方向选择 X 或 Y 轴
     */
    private fun scrollRecyclerView(delta: Int) {
        if (isHorizontal) {
            recyclerView.scrollBy(delta, 0)
        } else {
            recyclerView.scrollBy(0, delta)
        }
    }

    /**
     * 启动嵌套滚动（如果需要）
     */
    private fun startNestedScrollIfNeeded(dx: Int, dy: Int) {
        if (recyclerView !is KRRecyclerView || !recyclerView.isNestScrolling()) {
            return
        }
        // 根据滚动方向组合嵌套滚动轴
        var nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE
        if (dx != 0) {
            nestedScrollAxis = nestedScrollAxis or ViewCompat.SCROLL_AXIS_HORIZONTAL
        }
        if (dy != 0) {
            nestedScrollAxis = nestedScrollAxis or ViewCompat.SCROLL_AXIS_VERTICAL
        }
        if (nestedScrollAxis != ViewCompat.SCROLL_AXIS_NONE) {
            recyclerView.startNestedScroll(nestedScrollAxis, ViewCompat.TYPE_NON_TOUCH)
        }
    }
}