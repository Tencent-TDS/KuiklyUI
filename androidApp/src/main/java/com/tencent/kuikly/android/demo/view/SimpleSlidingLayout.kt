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

package com.tencent.kuikly.android.demo.view

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlin.math.abs

class SimpleSlidingLayout(context: Context, attrs: AttributeSet): FrameLayout(context, attrs) {

    // 横滑 offset 相关状态
    private var isHorizontalScrolling = false
    private var startX = 0f
    private var startY = 0f
    private var currentOffsetX = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat() * 2
    private var contentView: View? = null
    private var isDragging = false
    private var moveEventCount = 0 // Move 事件计数器

    override fun addView(child: View?, index: Int) {
        super.addView(child, index)
        // 当添加子 View 时，设置 contentView
        if (childCount > 0 && contentView == null) {
            contentView = getChildAt(0)
        }
    }

    /**
     * 拦截触摸事件，处理横滑逻辑
     */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null || contentView == null) {
            return super.onInterceptTouchEvent(ev)
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                isHorizontalScrolling = false
                isDragging = false
                moveEventCount = 0 // 重置 Move 事件计数器
                // 停止回弹动画
                contentView?.clearAnimation()
                // DOWN 事件不拦截，让子 View 有机会处理
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                moveEventCount++ // 递增 Move 事件计数器

                // 只有第2个Move事件及之后才开始处理横滑
                if (moveEventCount >= 2 && !isHorizontalScrolling && !isDragging) {
                    val deltaX = ev.x - startX
                    val deltaY = ev.y - startY
                    val absDeltaX = abs(deltaX)
                    val absDeltaY = abs(deltaY)

                    // 判断是否为横向滑动
                    if (absDeltaX > touchSlop && absDeltaX > absDeltaY) {
                        // 横向滑动，拦截事件
                        isHorizontalScrolling = true
                        isDragging = true
                        // 重置起始位置，避免跳跃
                        startX = ev.x
                        startY = ev.y
                        return true
                    } else if (absDeltaY > touchSlop && absDeltaY > absDeltaX) {
                        // 纵向滑动，不拦截，交给子 View 处理
                        return false
                    }
                } else if (isHorizontalScrolling) {
                    // 已经在横滑状态，继续拦截
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                moveEventCount = 0 // 重置 Move 事件计数器
                if (isHorizontalScrolling) {
                    // 已经在横滑状态，拦截事件并处理结束
                    return true
                }
            }
        }

        return super.onInterceptTouchEvent(ev)
    }

    /**
     * 处理触摸事件
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || contentView == null) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 如果拦截了 DOWN 事件，需要在这里处理
                startX = event.x
                startY = event.y
                isHorizontalScrolling = false
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isHorizontalScrolling) {
                    handleTouchMove(event.x, event.y)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isHorizontalScrolling) {
                    handleTouchEnd()
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * 处理触摸移动，根据 offset 移动子 View
     */
    private fun handleTouchMove(x: Float, y: Float) {
        val deltaX = x - startX

        if (contentView != null) {
            // 应用阻尼效果，随着 offset 增大，阻力增大
            val dampingFactor = 1.0f / (1.0f + abs(currentOffsetX) / 500.0f)
            val newOffset = currentOffsetX + deltaX * dampingFactor

            // 限制 offset 范围，避免过度滑动
            val maxOffset = 200f
            val minOffset = -200f
            currentOffsetX = maxOf(minOffset, minOf(maxOffset, newOffset))

            // 应用 translationX，移动整个子 View
            contentView!!.translationX = currentOffsetX

            // 更新起始位置，用于下一次计算
            startX = x
            startY = y
        }
    }

    /**
     * 处理触摸结束，回弹动画
     */
    private fun handleTouchEnd() {
        if (contentView != null && currentOffsetX != 0f) {
            // 回弹到原始位置
            animateToOffset(0f)
        }
        isHorizontalScrolling = false
        isDragging = false
    }

    /**
     * 动画回弹到指定 offset
     */
    private fun animateToOffset(targetOffset: Float) {
        if (contentView == null) return

        val animator = ObjectAnimator.ofFloat(contentView, View.TRANSLATION_X, currentOffsetX, targetOffset)
        animator.interpolator = DecelerateInterpolator()
        animator.duration = 300L
        animator.addUpdateListener { animation ->
            currentOffsetX = animation.animatedValue as Float
        }
        animator.start()
    }
}