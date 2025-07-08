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

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.tencent.kuikly.core.render.android.css.ktx.frameHeight
import com.tencent.kuikly.core.render.android.css.ktx.frameWidth
import kotlin.math.abs

/**
 * 分页PageSnapHelp实现
 */

internal class KRPagerSnapHelper(
    private val pageIndexChangeBlock: (index: Int) -> Unit
) : SnapHelper() {

    /**
     * 设置分页的RecyclerView
     */
    private var krRecyclerView: KRRecyclerView? = null

    /**
     * RecyclerView下的第一个孩子
     */
    private val hrContentView: ViewGroup?
        get() {
            return krRecyclerView?.getChildAt(0) as? ViewGroup
        }

    /**
     * 记录滚到到最后位置的数
     */
    private val distanceToFinalSnap = IntArray(2)

    /**
     * 当前的分页index
     */
    private var currentPageIndex = 0

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)
        krRecyclerView = recyclerView as KRRecyclerView
    }

    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray = distanceToFinalSnap(targetView, layoutManager)

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        val centerView = findCenterView(layoutManager)
        centerView?.also {
            onPageIndexChange(it, layoutManager)
        }
        return centerView
    }

    override fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager,
        velocityX: Int,
        velocityY: Int
    ): Int {
        val contentView = hrContentView
        if (contentView == null || contentView.childCount == 0) {
            return RecyclerView.NO_POSITION
        }

        var closestChildBeforeCenter: View? = null
        var distanceBefore = Int.MIN_VALUE
        var closestChildAfterCenter: View? = null
        var distanceAfter = Int.MAX_VALUE
        for (i in 0 until contentView.childCount) {
            val child = contentView.getChildAt(i)
            val distance = distanceToCenter(layoutManager, child)
            if (distance <= 0 && distance > distanceBefore) {
                distanceBefore = distance
                closestChildBeforeCenter = child
            }
            if (distance >= 0 && distance < distanceAfter) {
                distanceAfter = distance
                closestChildAfterCenter = child
            }
        }

        var resultPosition = RecyclerView.NO_POSITION
        val forwardDirection = isForwardFling(layoutManager, velocityX, velocityY)
        if (forwardDirection && closestChildAfterCenter != null) {
            onPageIndexChange(closestChildAfterCenter, layoutManager)
            resultPosition = distanceAfter
        } else if (!forwardDirection && closestChildBeforeCenter != null) {
            onPageIndexChange(closestChildBeforeCenter, layoutManager)
            resultPosition = distanceBefore
        }
        return resultPosition
    }

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        val minFlingVelocity = krRecyclerView?.minFlingVelocity ?: return false
        return if (abs(velocityY) <= minFlingVelocity && abs(velocityX) <= minFlingVelocity) {
            false
        } else {
            snapFromFling(velocityX, velocityY)
        }
    }

    private fun snapFromFling(velocityX: Int, velocityY: Int): Boolean {
        val layoutManager = krRecyclerView?.layoutManager ?: return false
        val distances = distanceToSnapFromFling(layoutManager, velocityX, velocityY)
        if (distances[0] != 0 || distances[1] != 0) {
            krRecyclerView?.smoothScrollBy(distances[0], distances[1])
        } else {
            krRecyclerView?.stopScroll()
        }
        return true
    }

    private fun findCenterView(layoutManager: RecyclerView.LayoutManager): View? {
        val contentView = hrContentView
        if (contentView == null || contentView.childCount == 0) {
            return null
        }

        val center = getContainerCenter(layoutManager)
        var absClosest = Int.MAX_VALUE
        var closestChild: View? = null
        for (i in 0 until contentView.childCount) {
            val child = contentView.getChildAt(i)
            val absDistance = abs(getViewCenter(child, layoutManager) - center)
            if (absDistance < absClosest) {
                absClosest = absDistance
                closestChild = child
            }
        }
        return closestChild
    }

    private fun distanceToSnapFromFling(
        layoutManager: RecyclerView.LayoutManager,
        velocityX: Int,
        velocityY: Int
    ): IntArray {
        val distance = findTargetSnapPosition(layoutManager, velocityX, velocityY)
        if (layoutManager.canScrollVertically()) {
            distanceToFinalSnap[0] = 0
            distanceToFinalSnap[1] = distance
        } else {
            distanceToFinalSnap[0] = distance
            distanceToFinalSnap[1] = 0
        }
        return distanceToFinalSnap
    }

    /**
     * 计算滚到targetView需要的偏移量
     */
    private fun distanceToFinalSnap(
        targetView: View,
        layoutManager: RecyclerView.LayoutManager
    ): IntArray {
        val distanceToCenter = distanceToCenter(layoutManager, targetView)
        if (layoutManager.canScrollVertically()) {
            distanceToFinalSnap[0] = 0 // x
            distanceToFinalSnap[1] = distanceToCenter // y
        } else {
            distanceToFinalSnap[0] = distanceToCenter // x
            distanceToFinalSnap[1] = 0 // y
        }
        return distanceToFinalSnap
    }

    private fun distanceToCenter(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): Int = getViewCenter(targetView, layoutManager) - getContainerCenter(layoutManager)

    private fun isForwardFling(
        layoutManager: RecyclerView.LayoutManager,
        velocityX: Int,
        velocityY: Int
    ): Boolean {
        return if (layoutManager.canScrollHorizontally()) {
            velocityX > 0
        } else {
            velocityY > 0
        }
    }

    private fun onPageIndexChange(targetView: View, layoutManager: RecyclerView.LayoutManager) {
        val isVertical = layoutManager.canScrollVertically()
        val offset = if (isVertical) {
            targetView.top
        } else {
            targetView.left
        }
        val itemSize = if (isVertical) {
            targetView.frameHeight
        } else {
            targetView.frameWidth
        }
        val newPageIndex = offset / itemSize
        if (newPageIndex != currentPageIndex) {
            pageIndexChangeBlock.invoke(newPageIndex)
            currentPageIndex = newPageIndex
        }
    }

    private fun getContainerCenter(layoutManager: RecyclerView.LayoutManager): Int {
        val container = krRecyclerView
        val contentView = hrContentView
        if (container == null || contentView == null) {
            return DEFAULT_CENTER_POSITION
        }
        return if (layoutManager.canScrollVertically()) {
            container.frameHeight / 2 + -contentView.top
        } else {
            container.frameWidth / 2 + -contentView.left
        }
    }

    private fun getViewCenter(view: View, layoutManager: RecyclerView.LayoutManager): Int {
        return if (layoutManager.canScrollVertically()) {
            view.frameHeight / 2 + view.top
        } else {
            view.frameWidth / 2 + view.left
        }
    }

    companion object {
        private const val DEFAULT_CENTER_POSITION = 0
    }
}