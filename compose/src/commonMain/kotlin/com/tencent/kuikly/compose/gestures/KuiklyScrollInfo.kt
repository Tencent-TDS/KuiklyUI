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

package com.tencent.kuikly.compose.gestures

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.views.ScrollerAttr
import com.tencent.kuikly.core.views.ScrollerEvent
import com.tencent.kuikly.core.views.ScrollerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * 滚动信息管理类，负责处理滚动相关的状态和计算
 */
class KuiklyScrollInfo {
    companion object {
        private const val DEFAULT_CONTENT_SIZE = 3000
        private const val SCROLL_BOTTOM_THRESHOLD = 100
        private const val DEFAULT_DENSITY = 3f
    }

    /**
     * 需要忽略的滚动偏移量
     */
    var ignoreScrollOffset: IntOffset? = null

    /**
     * 滚动视图实例
     */
    var scrollView: ScrollerView<ScrollerAttr, ScrollerEvent>? = null

    /**
     * 滚动方向
     */
    var orientation: Orientation = Orientation.Vertical

    /**
     * Compose侧的偏移量，不会超越边界
     */
    var composeOffset = 0f

    /**
     * 当前contentView的size，用来扩展底部边界
     */
    var currentContentSize by mutableStateOf((DEFAULT_CONTENT_SIZE * getDensity()).toInt())

    /**
     * 滑到底后，真实的contentSize
     */
    var realContentSize: Int? = null

    /**
     * offset是否有偏差
     */
    var offsetDirty = false

    /**
     * Scrollview的滑动偏移量
     */
    var contentOffset: Int by mutableStateOf(0)

    /**
     * 列表的高度缓存
     */
    internal var itemMainSpaceCache = hashMapOf<Any, Int>()

    /**
     * 用于跟踪延迟执行的applyScrollViewOffsetDelta任务
     */
    internal var appleScrollViewOffsetJob: Job? = null

    /**
     * 协程作用域
     */
    internal var scope: CoroutineScope? = null

    /**
     * 更新内容大小到渲染视图
     */
    fun updateContentSizeToRender() {
        val frame = createContentFrame()
        scrollView?.contentView?.setFrameToRenderView(frame)
    }

    /**
     * 创建内容Frame
     */
    private fun createContentFrame(): Frame {
        return if (isVertical()) {
            Frame(
                x = 0f,
                y = 0f,
                width = scrollView?.renderView?.currentFrame?.width ?: 0f,
                height = currentContentSize / getDensity()
            )
        } else {
            Frame(
                x = 0f,
                y = 0f,
                width = currentContentSize / getDensity(),
                height = scrollView?.renderView?.currentFrame?.height ?: 0f
            )
        }
    }

    /**
     * 获取视口大小
     */
    val viewportSize: Int
        get() {
            val size = if (isVertical()) {
                scrollView?.renderView?.currentFrame?.height ?: 0f
            } else {
                scrollView?.renderView?.currentFrame?.width ?: 0f
            }
            return (size * getDensity()).toInt()
        }

    /**
     * 获取密度
     */
    fun getDensity(): Float {
        return scrollView?.getPager()?.pagerDensity() ?: DEFAULT_DENSITY
    }

    /**
     * 判断是否为垂直滚动
     */
    fun isVertical(): Boolean = orientation == Orientation.Vertical

    /**
     * 判断是否接近滚动底部
     */
    fun nearScrollBottom(): Boolean {
        val threshold = SCROLL_BOTTOM_THRESHOLD * getDensity()
        return contentOffset + viewportSize + threshold > currentContentSize
    }
}