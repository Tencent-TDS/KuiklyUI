/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInStart() && !isInEnd()) {
            if (pointerDataMap.size() != 0) {
                // 防止一开始是在start或者end, 然后pointerMap中存在down事件
                // 最后滑到不是在start或者end时松手。此时不会走到clear,这里补一刀clear
                pointerDataMap.clear()
            }
            if (!forceOverScroll) {
                return false // 没有到达边缘, fast fail
            }
        }

        val activeIndex = event.actionIndex
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> processDownEvent(activeIndex, event)
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