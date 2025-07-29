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

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * RecyclerView事件拦截优化处理
 * RecyclerView内部拦截事件的判断只判断的滑动距离，没有判断角度。当横向和纵向List嵌套时
 * 会导致外层的RecyclerView拦截错不该属于它方向的事件，导致滑动冲突
 *
 * 此类作为为优化RecyclerView内部拦截事件判断的方式，加了角度判断
 */
internal class RVScrollConflictHandler(private val context: Context) {

    private var scrollPointerId = 0
    private var initialTouchX = 0
    private var initialTouchY = 0
    private var touchSlop = 0

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    fun setScrollingTouchSlop(slopConstant: Int) {
        val vc = ViewConfiguration.get(context)
        touchSlop = when (slopConstant) {
            RecyclerView.TOUCH_SLOP_DEFAULT -> vc.scaledTouchSlop
            RecyclerView.TOUCH_SLOP_PAGING -> vc.scaledPagingTouchSlop
            else -> vc.scaledTouchSlop
        }
    }

    fun onInterceptTouchEvent(e: MotionEvent, recyclerView: RecyclerView): Boolean {
        val actionIndex = e.actionIndex
        return when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                processDownEvent(e, actionIndex)
                false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                processDownEvent(e, actionIndex)
                false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                processPointerUp(e, actionIndex)
                false
            }
            MotionEvent.ACTION_MOVE -> {
                processMoveEvent(recyclerView, e)
            }
            else -> false
        }
    }

    private fun processDownEvent(e: MotionEvent, actionIndex: Int) {
        scrollPointerId = e.getPointerId(actionIndex)
        initialTouchX = (e.x + 0.5f).toInt()
        initialTouchY = (e.y + 0.5f).toInt()
    }

    private fun processMoveEvent(recyclerView: RecyclerView, e: MotionEvent): Boolean {
        val index = e.findPointerIndex(scrollPointerId)
        if (index < 0) {
            return false
        }

        val layoutManager = recyclerView.layoutManager ?: return false
        val canScrollHorizontally = layoutManager.canScrollHorizontally()
        val canScrollVertically = layoutManager.canScrollVertically()
        val x = (e.getX(index) + 0.5f).toInt()
        val y = (e.getY(index) + 0.5f).toInt()
        if (recyclerView.scrollState != RecyclerView.SCROLL_STATE_DRAGGING) {
            val dx = x - initialTouchX
            val dy = y - initialTouchY
            var startScroll = false
            if (canScrollHorizontally && abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                startScroll = true
            }
            if (canScrollVertically && abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                startScroll = true
            }

            return !startScroll
        }
        return false
    }

    private fun processPointerUp(event: MotionEvent, activeIndex: Int) {
        if (event.getPointerId(activeIndex) == scrollPointerId) {
            val newIndex = if (activeIndex == 0) 1 else 0
            scrollPointerId = event.getPointerId(newIndex)
            initialTouchX = (event.x + 0.5f).toInt()
            initialTouchY = (event.y + 0.5f).toInt()
        }
    }
}
