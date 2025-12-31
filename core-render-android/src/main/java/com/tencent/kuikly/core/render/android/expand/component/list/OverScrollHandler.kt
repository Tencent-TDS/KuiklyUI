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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.util.SparseArray
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import com.tencent.kuikly.core.render.android.css.ktx.toNumberFloat
import com.tencent.kuikly.core.render.android.css.ktx.toPxF
import kotlin.math.abs

/**
 * List边缘滚动回弹效果处理类
 * @param recyclerView List
 * @param contentView List下的内容View
 * @param isVertical 是否位横向List
 * @param overScrollEventCallback 边缘滚动offset回调
 */
internal class OverScrollHandler(
    private val recyclerView: KRRecyclerView,
    private val contentView: View,
    private val isVertical: Boolean,
    private val overScrollEventCallback: OverScrollEventCallback
) {

    /**
     * 记录多指触控的数据
     */
    private var pointerDataMap = SparseArray<PointerData>()

    /**
     * 最小滚动阈值
     */
    private var touchSlop = ViewConfiguration.get(recyclerView.context).scaledTouchSlop

    /**
     * 是否正在拖拽
     */
    private var dragging = false

    /**
     * 记录收到move事件之前，是否有收到down事件
     */
    private var downing = false
    private var initX = 0f
    private var initY = 0f

    /**
     * 内容边距inset
     */
    var contentInsetWhenEndDrag: KRRecyclerContentViewContentInset? = null
    var forceOverScroll = false

    var overScrolling = false
    var overScrollX: Float = 0f
    var overScrollY: Float = 0f
    private var hadBeginDrag = false

    private val maxFlingVelocity = ViewConfiguration.get(recyclerView.context).scaledMaximumFlingVelocity
    private var velocityTracker =  VelocityTracker.obtain()
    private var scrollPointerId = -1

    /**
     * 标记上一次是否在边缘，用于检测从中间滑动到边缘的时刻
     */
    private var wasAtEdge = false
    
    fun onTouchEvent(event: MotionEvent): Boolean {
        val atEdge = isInStart() || isInEnd()
        
        // 处理 DOWN 事件时，无论是否在边缘都需要记录状态
        // 这样当用户从中间滑动到边缘时，才能正确触发 overscroll
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            wasAtEdge = atEdge
            return processDownEvent(event.actionIndex, event)
        }
        
        // 检测从中间滑动到边缘的时刻
        val justReachedEdge = atEdge && !wasAtEdge
        wasAtEdge = atEdge
        
        if (!atEdge) {
            // 不在边缘时，需要持续更新 pointer 数据
            // 这样当滑动到边缘时，才能正确计算 offset
            if (event.actionMasked == MotionEvent.ACTION_MOVE && downing) {
                // 更新所有 pointer 的位置，但不触发 overscroll
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    val pointerData = pointerDataMap.get(pointerId)
                    if (pointerData != null) {
                        pointerData.offset = getCurrentOffset(i, event)
                    }
                }
            } else if (event.actionMasked == MotionEvent.ACTION_UP || 
                       event.actionMasked == MotionEvent.ACTION_CANCEL) {
                // 只在 UP/CANCEL 时清空状态
                pointerDataMap.clear()
                downing = false
            }
            if (!forceOverScroll) {
                return false // 没有到达边缘, fast fail
            }
        }
        
        // 刚滑到边缘时，设置 dragging = true
        // 这样后续的小增量也会被处理（跳过 touchSlop 检查）
        // 同时触发 beginOverScroll 回调
        if (justReachedEdge && event.actionMasked == MotionEvent.ACTION_MOVE && downing) {
            dragging = true
            if (!hadBeginDrag) {
                fireBeginOverScrollCallback()
            }
        }

        val activeIndex = event.actionIndex
        return when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> processPointerDownEvent(activeIndex, event)
            MotionEvent.ACTION_MOVE -> processMoveEvent(event)
            MotionEvent.ACTION_POINTER_UP -> processPointerUpEVent(activeIndex, event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                downing = false
                processBounceBack()
            }
            else -> false
        }
    }

    /**
     * 根据内容边距进行OverScroll滚动
     * @param contentInset 内容边距
     */
    fun bounceWithContentInset(contentInset: KRRecyclerContentViewContentInset) {
        if (contentInset.animate) {
            startBounceBack(contentInset)
        } else {
            setFinalTranslation(contentInset)
        }
    }

    private fun processDownEvent(activeIndex: Int, event: MotionEvent): Boolean {
        downing = true
        updatePointerData(activeIndex, event)
        if (forceOverScroll) {
            dragging = true
            fireBeginOverScrollCallback()
        }
        initX = event.x
        initY = event.y
        velocityTracker.addMovement(event)
        return false
    }

    private fun processPointerDownEvent(activeIndex: Int, event: MotionEvent): Boolean {
        downing = true
        scrollPointerId = event.getPointerId(activeIndex)
        updatePointerData(activeIndex, event)
        return false
    }

    private fun processMoveEvent(event: MotionEvent): Boolean {
        if (!downing) { // 收到move事件时，没收到down事件的话，补发down事件
            processDownEvent(event.actionIndex, event)
        }
        if (!acceptEvent(event)) {
            velocityTracker.clear()
            return false
        }

        val currentTranslation = getTranslation()
        val offset = getOverScrollOffset(event)
        if (offset == 0f) {
            velocityTracker.clear()
            return dragging
        }

        var processEvent = false
        if (needTranslate(offset, currentTranslation)) {
            setTranslation(offset)
            dragging = true
            processEvent = true
            if (!forceOverScroll && !hadBeginDrag) {
                fireBeginOverScrollCallback()
            }
            fireOverScrollCallback(contentView.translationX, contentView.translationY)
        }
        if (processEvent) {
            velocityTracker.addMovement(event)
        }
        return processEvent
    }

    private fun acceptEvent(event: MotionEvent): Boolean {
        val dx = event.x - initX
        val dy = event.y - initY
        var startScroll = false
        if (!isVertical && abs(dx) > touchSlop && abs(dx) > abs(dy)) {
            startScroll = true
        }
        if (isVertical && abs(dy) > touchSlop && abs(dy) > abs(dx)) {
            startScroll = true
        }

        return startScroll
    }

    private fun needTranslate(offset: Float, currentTranslation: Float): Boolean =
        needInStartTranslate(offset, currentTranslation) || needInEndTranslate(
            offset,
            currentTranslation
        )

    private fun needInStartTranslate(offset: Float, currentTranslation: Float): Boolean =
        !recyclerView.limitHeaderBounces && isInStart() && (offset > 0 || currentTranslation > 0)

    private fun needInEndTranslate(offset: Float, currentTranslation: Float): Boolean =
        isInEnd() && (offset < 0 || currentTranslation < 0)

    private fun processPointerUpEVent(activeIndex: Int, event: MotionEvent): Boolean {
        pointerDataMap.remove(event.getPointerId(activeIndex))
        return false
    }

    internal fun processBounceBack(): Boolean {
        pointerDataMap.clear()
        dragging = false
        overScrollX = contentView.translationX
        overScrollY = contentView.translationY
        velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
        val velocityX = if (isVertical) 0f else -velocityTracker.getXVelocity(scrollPointerId)
        val velocityY = if (isVertical) -velocityTracker.getYVelocity(scrollPointerId) else 0f
        overScrollEventCallback.onEndDragOverScroll(
            overScrollX,
            overScrollY,
            velocityX,
            velocityY,
            isInStart(),
            dragging
        )
        startBounceBack()
        velocityTracker.clear()
        return false
    }

    private fun resetState() {
        pointerDataMap.clear()
        dragging = false
        velocityTracker.clear()
    }

    private fun startBounceBack(contentInset: KRRecyclerContentViewContentInset? = null) {
        val finalOffset = getFinalOffset(contentInset)
        val startOffset = if (isVertical) {
            contentView.translationY
        } else {
            contentView.translationX
        }
        val propertyName = if (isVertical) {
            View.TRANSLATION_Y
        } else {
            View.TRANSLATION_X
        }

        val animator = ObjectAnimator.ofFloat(contentView, propertyName, startOffset, finalOffset)
        animator.interpolator = DecelerateInterpolator()
        animator.duration = BOUND_BACK_DURATION
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {
                hadBeginDrag = false
                overScrolling = false
                fireOverScrollAnimationCallback(finalOffset)
                tryFireContentInsertFinish()
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                hadBeginDrag = false
                overScrolling = false
                fireOverScrollAnimationCallback(finalOffset)
                tryFireContentInsertFinish()
            }

        })
        animator.addUpdateListener {
            fireOverScrollAnimationCallback(it.animatedValue.toNumberFloat())
        }
        animator.start()
    }

    private fun fireOverScrollAnimationCallback(offset: Float) {
        val offsetX = if (isVertical) {
            0f
        } else {
            offset
        }
        val offsetY = if (isVertical) {
            offset
        } else {
            0f
        }
        fireOverScrollCallback(offsetX, offsetY)
    }

    private fun tryFireContentInsertFinish() {
        contentInsetWhenEndDrag?.finishCallback?.invoke()
    }

    private fun getFinalOffset(viewContentInset: KRRecyclerContentViewContentInset?): Float {
        val ci = viewContentInset ?: contentInsetWhenEndDrag
        val contentInset = ci ?: return 0f
        return if (isVertical) {
            contentInset.top
        } else {
            contentInset.left
        }
    }

    private fun setFinalTranslation(viewContentInset: KRRecyclerContentViewContentInset) {
        val finalOffset = getFinalOffset(viewContentInset)
        if (isVertical) {
            contentView.translationY = finalOffset
        } else {
            contentView.translationX = finalOffset
        }
        fireOverScrollAnimationCallback(finalOffset)
    }

    private fun fireBeginOverScrollCallback() {
        overScrolling = true
        overScrollX = contentView.translationX
        overScrollY = contentView.translationY
        overScrollEventCallback.onBeginDragOverScroll(
            overScrollX,
            overScrollY,
            isInStart(),
            dragging
        )
        hadBeginDrag = true
    }

    private fun fireOverScrollCallback(offsetX: Float, offsetY: Float) {
        overScrollX = offsetX
        overScrollY = offsetY
        overScrollEventCallback.onOverScroll(offsetX, offsetY, isInStart(), dragging)
    }

    fun isInStart(): Boolean {
        return if (isVertical) {
            !recyclerView.canScrollVertically(DIRECTION_SCROLL_UP)
        } else {
            !recyclerView.canScrollHorizontally(DIRECTION_SCROLL_UP)
        }
    }

    private fun isInEnd(): Boolean {
        return if (isVertical) {
            !recyclerView.canScrollVertically(DIRECTION_SCROLL_DOWN)
        } else {
            !recyclerView.canScrollHorizontally(DIRECTION_SCROLL_DOWN)
        }
    }

    private fun getOverScrollOffset(event: MotionEvent): Float {
        var offset = 0f
        for (i in 0 until event.pointerCount) {
            val pointerData = pointerDataMap.get(event.getPointerId(i)) ?: continue
            val currentOffset = getCurrentOffset(i, event)
            val deltaOffset = currentOffset - pointerData.offset
            offset += deltaOffset
            pointerData.offset = currentOffset
        }
        return if (abs(offset) <= touchSlop && !dragging) {
            0f
        } else {
            getNewOffset(getTranslation(), offset)
        }
    }

    /**
     * 处理overScroll的值，随着currentTranslation越来越大, newOffset会越来越小，起到一个阻尼的效果
     */
    private fun getNewOffset(currentTranslation: Float, offset: Float): Float =
        offset / (NEW_OFFSET_ADD_FACTOR + abs(currentTranslation) / recyclerView.kuiklyRenderContext.toPxF(NEW_OFFSET_SCALE_FACTOR))

    private fun getTranslation(): Float {
        return if (isVertical) {
            contentView.translationY
        } else {
            contentView.translationX
        }
    }

    private fun setTranslation(offset: Float) {
        if (isVertical) {
            contentView.translationY += offset
        } else {
            contentView.translationX += offset
        }
    }

    private fun updatePointerData(activeIndex: Int, motionEvent: MotionEvent) {
        val pointerId = motionEvent.getPointerId(activeIndex)
        val currentOffset = getCurrentOffset(activeIndex, motionEvent)
        var pointerData = pointerDataMap.get(pointerId)
        if (pointerData == null) {
            pointerData = PointerData(pointerId, currentOffset)
            pointerDataMap.put(pointerId, pointerData)
        } else {
            pointerData.offset = currentOffset
        }
    }

    private fun getCurrentOffset(activeIndex: Int, motionEvent: MotionEvent): Float {
        return if (isVertical) {
            motionEvent.getY(activeIndex)
        } else {
            motionEvent.getX(activeIndex)
        }
    }

    internal fun setTranslationByNestScrollTouch(parentDy: Float) {
        val newOffset = getNewOffset(getTranslation(), parentDy)
        setTranslation(-newOffset)
        if (!overScrolling) {
            dragging = true
            fireBeginOverScrollCallback()
        } else {
            fireOverScrollCallback(contentView.translationX, contentView.translationY)
        }
    }
    
    /**
     * 处理 fling 到边缘时的弹簧效果
     * @param velocity fling 速度（像素/秒），正值表示向下/向右滚动
     * @param atStart 是否在起始边缘
     */
    internal fun triggerFlingBounce(velocity: Float, atStart: Boolean) {
        // 如果已经在 overscroll 状态，不重复触发
        if (overScrolling) {
            return
        }
        
        // 根据速度计算弹簧位移
        // 速度越大，弹簧位移越大，但有上限
        val maxBounceOffset = recyclerView.kuiklyRenderContext.toPxF(80f) // 最大弹簧位移 80dp
        val velocityFactor = 0.05f // 速度转换因子
        val absVelocity = abs(velocity)
        
        // 计算弹簧位移，使用阻尼公式
        var bounceOffset = (absVelocity * velocityFactor).coerceAtMost(maxBounceOffset)
        
        // 根据边缘位置确定方向
        // 在起始边缘时，向正方向弹（translationY/X > 0）
        // 在结束边缘时，向负方向弹（translationY/X < 0）
        if (!atStart) {
            bounceOffset = -bounceOffset
        }
        
        // 如果弹簧位移太小，不触发
        if (abs(bounceOffset) < 1f) {
            return
        }
        
        // 触发弹簧效果
        overScrolling = true
        dragging = false
        fireBeginOverScrollCallback()
        
        // 设置弹簧位移
        if (isVertical) {
            contentView.translationY = bounceOffset
        } else {
            contentView.translationX = bounceOffset
        }
        fireOverScrollCallback(contentView.translationX, contentView.translationY)
        
        // 触发回弹动画
        overScrollX = contentView.translationX
        overScrollY = contentView.translationY
        overScrollEventCallback.onEndDragOverScroll(
            overScrollX,
            overScrollY,
            0f, // fling 回弹时速度为 0
            0f,
            atStart,
            false
        )
        startBounceBack()
    }

    private data class PointerData(
        val pointerId: Int,
        var offset: Float
    )

    companion object {
        private const val BOUND_BACK_DURATION = 250L
        private const val NEW_OFFSET_ADD_FACTOR = 2
        private const val NEW_OFFSET_SCALE_FACTOR = 500f

        private const val DIRECTION_SCROLL_UP = -1
        private const val DIRECTION_SCROLL_DOWN = 1
    }
}

internal interface OverScrollEventCallback {
    fun onBeginDragOverScroll(
        offsetX: Float,
        offsetY: Float,
        overScrollStart: Boolean,
        isDragging: Boolean
    )

    fun onOverScroll(offsetX: Float, offsetY: Float, overScrollStart: Boolean, isDragging: Boolean)
    fun onEndDragOverScroll(
        offsetX: Float,
        offsetY: Float,
        velocityX: Float,
        velocityY: Float,
        overScrollStart: Boolean,
        isDragging: Boolean
    )
}