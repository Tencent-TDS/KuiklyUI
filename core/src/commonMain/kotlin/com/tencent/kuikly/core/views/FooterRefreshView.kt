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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.layout.FlexDirection

/** 尾部刷新控件的刷新状态 */
enum class FooterRefreshState {
    /** 普通闲置状态 */
    IDLE,
    /** 正在刷新中的状态 */
    REFRESHING,
    /** 无更多数据状态（后面不会再次触发刷新状态） */
    NONE_MORE_DATA,
    /** 失败状态（一般展示点击重试UI） */
    FAILURE,
}

/** 刷新结束时状态 */
enum class FooterRefreshEndState {
    /** 成功结束状态 */
    SUCCESS,
    /** 失败状态（一般展示点击重试UI） */
    FAILURE,
    /** 无更多数据状态（后面不会再次触发刷新状态） */
    NONE_MORE_DATA,
}

class FooterRefreshView : ViewContainer<FooterRefreshAttr, FooterRefreshEvent>(),
    IScrollerViewEventObserver {
    private var didDragList = false
    private var nextTickSetRefreshStateTaskFlag = 0
    /** 结束刷新（触发刷新中状态后设置该方法） */
    fun endRefresh(endState: FooterRefreshEndState = FooterRefreshEndState.SUCCESS) {
        val flag = ++nextTickSetRefreshStateTaskFlag
        getPager().addNextTickTask {
            if (flag == nextTickSetRefreshStateTaskFlag) {
                when (endState) {
                    FooterRefreshEndState.SUCCESS -> refreshState = FooterRefreshState.IDLE
                    FooterRefreshEndState.FAILURE -> refreshState = FooterRefreshState.FAILURE
                    FooterRefreshEndState.NONE_MORE_DATA -> refreshState = FooterRefreshState.NONE_MORE_DATA
                    else -> {}
                }
            }
        }

    }

    /** 手动开始刷新（一般用于网络加载失败时，点击尾部加载更多重试时，手动触发） */
    fun beginRefresh() {
        refreshState = FooterRefreshState.REFRESHING
    }
    /** 重置刷新状态（一般用于头部刷新成功后来设置尾部刷新组件的刷新状态） */
    fun resetRefreshState(state: FooterRefreshState = FooterRefreshState.IDLE) {
        refreshState = state
    }

    private val scrollerView: ScrollerView<*, *>?
        get() {
            var scrollerView = parent?.parent
            while (scrollerView != null && scrollerView !is ScrollerView<*, *>) {
                scrollerView = scrollerView.parent
            }
            if (scrollerView != null && scrollerView !is ScrollerView<*, *>) {
                throwRuntimeError("FooterRefresh组件需要布局在Scroller容器组件下")
            }
            return scrollerView as? ScrollerView<*, *>
        }

    var refreshState: FooterRefreshState = FooterRefreshState.IDLE
        private set(value) {
            nextTickSetRefreshStateTaskFlag++
            if (value != field) {
                val oldState = field
                field = value
                handleStateDidChange(value, oldState)
            }
        }

    override fun didMoveToParentView() {
        super.didMoveToParentView()
        scrollerView?.addScrollerViewEventObserver(this)
    }

    override fun willRemoveFromParentView() {
        super.willRemoveFromParentView()
        scrollerView?.removeScrollerViewEventObserver(this)
    }

    override fun createAttr(): FooterRefreshAttr {
        return FooterRefreshAttr()
    }

    override fun createEvent(): FooterRefreshEvent {
        return FooterRefreshEvent()
    }

    override fun viewName(): String {
        return ViewConst.TYPE_VIEW
    }

    // IListViewEventObserver
    override fun onContentOffsetDidChanged(
        contentOffsetX: Float,
        contentOffsetY: Float,
        params: ScrollParams
    ) {
        if (refreshState == FooterRefreshState.NONE_MORE_DATA || refreshState == FooterRefreshState.REFRESHING) {
            return
        }
        if (scrollerView == null) {
            return
        }
        if (scrollerView!!.flexNode.layoutFrame.isDefaultValue()) {
            return
        }
        if (!(params.contentHeight > attr.minContentSize.height
                    && params.contentWidth > attr.minContentSize.width)) {
            return
        }
        if (params.isDragging) {
            didDragList = true
        }
        if (!didDragList) { // 需要拖拽过一次才能进行加载更多
            return
        }
        if (isRowDirection()) {
            if (contentOffsetX <= 0) {
                return
            }
        } else if (contentOffsetY <= 0) {
            return
        }
        // 距离底部距离
        val remainBottomDistance = if (isRowDirection()) {
            (params.contentWidth - contentOffsetX) - scrollerView!!.flexNode.layoutFrame.width
        } else {
            (params.contentHeight - contentOffsetY) - scrollerView!!.flexNode.layoutFrame.height
        }

        if (remainBottomDistance < attr.preloadDistance) { // 底部距离不足，转为刷新
            refreshState = FooterRefreshState.REFRESHING
        }
    }

    // IListViewEventObserver
    override fun subViewsDidLayout() {

    }

    private fun isRowDirection(): Boolean {
        return scrollerView?.flexNode?.flexDirection == FlexDirection.ROW || scrollerView?.flexNode?.flexDirection == FlexDirection.ROW_REVERSE
    }

    private fun handleStateDidChange(
        newState: FooterRefreshState,
        oldState: FooterRefreshState
    ) {
        dispatchRefreshStateChanged(newState)
    }

    // 状态变化分发
    private fun dispatchRefreshStateChanged(state: FooterRefreshState) {
        event.refreshStateDidChangeHandlerFn?.invoke(state)
    }
}
class FooterRefreshAttr : ContainerAttr() {
    internal var preloadDistance = 100f
    internal var minContentSize = Size(0f, 0f)

    /**
     * 设置预加载距离，（即列表底部最小剩余空间可以滑动的距离时触发加载更多事件）
     * @param distance， 预加载距离
     */
    fun preloadDistance(distance: Float) {
        this.preloadDistance = distance
    }

    /**
     * 设置触发加载更多的列表最小内容尺寸
     * @param minContentWidth 最小内容宽度
     * @param minContentHeight 最小内容高度
     */
    fun minContentSize(minContentWidth: Float, minContentHeight: Float) {
        minContentSize = Size(minContentWidth, minContentHeight)
    }
}

class FooterRefreshEvent : Event() {
    internal var refreshStateDidChangeHandlerFn: ((state: FooterRefreshState) -> Unit)? = null

    /**
     * 设置刷新状态变化回调。
     * @param handler 一个函数，接收一个 FooterRefreshState 参数，当刷新状态发生变化时调用。
     */
    fun refreshStateDidChange(handler: (state: FooterRefreshState) -> Unit) {
        refreshStateDidChangeHandlerFn = handler
    }
}

/**
 * 创建列表组件尾部刷新组件（注：需要布局于 List 或 WaterfallList 组件下）。
 * @param init 一个 FooterRefreshView.() -> Unit 函数，用于初始化尾部刷新组件的属性和子视图。
 */
fun ViewContainer<*, *>.FooterRefresh(init: FooterRefreshView.() -> Unit) {
    addChild(FooterRefreshView(), init)
}
