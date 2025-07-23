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

import android.graphics.Rect
import android.view.MotionEvent
import android.view.ViewConfiguration
import java.lang.ref.WeakReference
import kotlin.math.abs

internal class NestedVerticalChildInterceptor(recyclerView: KRRecyclerView) : INestedChildInterceptor {

    private val recyclerViewWeakRef = WeakReference(recyclerView)
    private var scrollPointerId = 0
    private var initialTouchX = 0
    private var initialTouchY = 0
    private var touchSlop =  ViewConfiguration.get(recyclerView.context).scaledTouchSlop
    private val locationArray = IntArray(2)
    private val hitRect = Rect()
    private var isPointInside = false

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (recyclerViewWeakRef.get()?.isScrollEnabled() == false) {
            return false
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            isPointInside = isPointInside(event)
        }

        if (!isPointInside) {
            return false
        }

        val actionIndex = event.actionIndex
        val result = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                processDownEvent(event, actionIndex)
                false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                processDownEvent(event, actionIndex)
                false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                processPointerUp(event, actionIndex)
                false
            }
            MotionEvent.ACTION_MOVE -> {
                processMoveEvent(event)
            }
            else -> false
        }

        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            isPointInside = false
        }
        return result
    }

    private fun isPointInside(event: MotionEvent): Boolean {
        val recyclerView = recyclerViewWeakRef.get() ?: return false
        recyclerView.getLocationOnScreen(locationArray)
        recyclerView.getHitRect(hitRect)
        val height = hitRect.bottom - hitRect.top
        val width = hitRect.right - hitRect.left
        val rectOnScreen = Rect(locationArray[0], locationArray[1], locationArray[0] + width, locationArray[1] + height)
        return rectOnScreen.contains(event.rawX.toInt(), event.rawY.toInt())
    }

    private fun processDownEvent(e: MotionEvent, actionIndex: Int) {
        scrollPointerId = e.getPointerId(actionIndex)
        initialTouchX = (e.x + 0.5f).toInt()
        initialTouchY = (e.y + 0.5f).toInt()
    }

    private fun processMoveEvent(e: MotionEvent): Boolean {
        val recyclerView = recyclerViewWeakRef.get() ?: return false

        // 如果禁用了父组件滑动联动，则拦截事件，滑动事件完全由子组件处理
        if (!recyclerView.isScrollWithParent()) {
            return true
        }

        val index = e.findPointerIndex(scrollPointerId)
        if (index < 0) {
            return false
        }

        val layoutManager = recyclerView.layoutManager ?: return false
        val canScrollHorizontally = layoutManager.canScrollHorizontally()
        val canScrollVertically = layoutManager.canScrollVertically()
        val x = (e.getX(index) + 0.5f).toInt()
        val y = (e.getY(index) + 0.5f).toInt()
        val dx = x - initialTouchX
        val dy = y - initialTouchY
        if (canScrollHorizontally && abs(dx) > touchSlop && abs(dx) > abs(dy)) {
            return true
        }
        if (canScrollVertically && abs(dy) > touchSlop && abs(dy) > abs(dx)) {
            // View 向上滚动，判断如果当前scroll在顶部，则不拦截事件，让父View继续处理，避免滑动冲突
            if (dy > 0 && recyclerView.contentOffsetY <= 0f) {
                return false
            }

            // View 向下滚动，判断如果当前scroll在底部，则不拦截事件
            if (dy < 0) {
                val reachBottom = recyclerView.computeVerticalScrollExtent() + recyclerView.computeVerticalScrollOffset() >= recyclerView.computeVerticalScrollRange()
                if (reachBottom) {
                    return false
                }
            }
            return true
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