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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.timer.setTimeout

/**
 * 在 Scroller 中添加一个 RefreshView 实例。
 * @param init 一个 RefreshView.() -> Unit 函数，用于初始化 RefreshView 的属性和子视图。
 */
fun ScrollerView<*, *>.Refresh(init: RefreshView.() -> Unit) {
    addChild(RefreshView(), init)
}


class RefreshAttr : ContainerAttr() {
    var refreshEnable = true
}

class RefreshEvent : Event() {
    internal var refreshStateDidChangeHandlerFn: ((state: RefreshViewState) -> Unit)? = null
    internal var pullingPercentageChangedEventHandlerFn: ((Float) -> Unit)? = null

    /**
     * 设置刷新状态变化时的事件处理器。
     * @param handler 一个函数，接收 RefreshViewState 参数，当刷新状态发生变化时调用。
     */
    fun refreshStateDidChange(handler: (state: RefreshViewState) -> Unit) {
        refreshStateDidChangeHandlerFn = handler
    }

    /**
     * 设置下拉百分比实时变化时的事件处理器。
     * @param handler 一个函数，接收 Float 参数，当下拉百分比发生变化时调用。
     */
    fun pullingPercentageChanged(handler: (percentage01: Float) -> Unit) {
        pullingPercentageChangedEventHandlerFn = handler
    }
}

/** 刷新控件的状态 */
enum class RefreshViewState {
    /** 普通闲置状态 */
    IDLE,
    /** 松开就可以进行刷新的状态 */
    PULLING,
    /** 正在刷新中的状态 */
    REFRESHING,
}
class RefreshView : ViewContainer<RefreshAttr, RefreshEvent>(), IListViewEventObserver {

    /** 手动调用刷新 */
    fun beginRefresh(animated: Boolean = true) {
        performTaskWhenRenderViewDidLoad {
            if (refreshState != RefreshViewState.REFRESHING) {
                scrollerView?.setContentOffset(0f, -flexNode.layoutFrame.height, animated = animated)
                refreshState = RefreshViewState.REFRESHING
            }
        }
    }

    /** 结束刷新 */
    fun endRefresh() {
        performTaskWhenRenderViewDidLoad {
            if (refreshState != RefreshViewState.IDLE) {
                refreshState = RefreshViewState.IDLE
            }
        }
    }

    private val scrollerView: ScrollerView<*, *>?
        get() = (parent?.parent as? ScrollerView<*, *>)

    private var contentInsetTop: Float = 0f
        set(value) {
            scrollerView?.setContentInset(top = value, animated = true)
        }

    var contentInsetTopWhenEndDrag: Float = 0f
        set(value) {
            scrollerView?.setContentInsetWhenEndDrag(top = value)
        }

    var refreshState: RefreshViewState = RefreshViewState.IDLE
        set(value) {
            if (value != field) {
                val oldState = field
                field = value
                handleStateDidChange(value, oldState)
            }
        }

    override fun didInit() {
        super.didInit()
        attr.apply {
            positionAbsolute().left(0f).right(0f).top(0f).bottom(Float.undefined)
            transform(translate = Translate(0f, -1f))
            keepAlive = true
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

    override fun createAttr(): RefreshAttr {
        return RefreshAttr()
    }

    override fun createEvent(): RefreshEvent {
        return RefreshEvent()
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
        if (flexNode.layoutFrame.isDefaultValue() || flexNode.layoutFrame.height == 0f) {
            return
        }
        if (!attr.refreshEnable) {
            refreshState = RefreshViewState.IDLE
            return
        }
        val viewHeight = flexNode.layoutFrame.height
        val thresholdY = 0
        if (contentOffsetY > thresholdY) return
        val normalAndPullingOffsetY = thresholdY - viewHeight
        val pullingOffset = (thresholdY - contentOffsetY)
        val pullingPercent = pullingOffset / viewHeight
        if (refreshState == RefreshViewState.REFRESHING) { // 正在刷新中就不做操作了
            pullingPercentDidChanged(pullingPercent)
        } else {
            if (params.isDragging) { // 如果正在拖拽
                if (refreshState == RefreshViewState.IDLE && contentOffsetY < normalAndPullingOffsetY) {
                    refreshState = RefreshViewState.PULLING // 转为即将刷新状态
                } else if (refreshState == RefreshViewState.PULLING && contentOffsetY >= normalAndPullingOffsetY) {
                    refreshState = RefreshViewState.IDLE // 转为普通状态
                }
            } else if (refreshState == RefreshViewState.PULLING) { // 即将刷新 && 手松开
                refreshState = RefreshViewState.REFRESHING // 开始刷新
            }
            pullingPercentDidChanged(pullingPercent)
        }
    }

    // IListViewEventObserver
    override fun subViewsDidLayout() {

    }

    private fun pullingPercentDidChanged(pullingPercent: Float) {
        event.pullingPercentageChangedEventHandlerFn?.invoke(pullingPercent)
    }

    private fun handleStateDidChange(newState: RefreshViewState, oldState: RefreshViewState) {
        // 恢复inset和offset
        val height = flexNode.layoutFrame.height
        if (newState == RefreshViewState.IDLE && oldState == RefreshViewState.REFRESHING) {
            contentInsetTop = 0f
            contentInsetTopWhenEndDrag = 0f
            if ((scrollerView?.curOffsetY ?: 0f) >= 0) { // 分发状态变化
                dispatchRefreshStateChanged(RefreshViewState.IDLE)
            } else {
                setTimeout(200) {
                    if (refreshState == RefreshViewState.IDLE) {
                        dispatchRefreshStateChanged(RefreshViewState.IDLE)
                    }
                }
            }
        } else if (newState == RefreshViewState.REFRESHING) {
            dispatchRefreshStateChanged(RefreshViewState.REFRESHING)
        } else if (newState == RefreshViewState.PULLING) {
            contentInsetTopWhenEndDrag = height
            dispatchRefreshStateChanged(RefreshViewState.PULLING)
        } else if (newState == RefreshViewState.IDLE) {
            contentInsetTop = 0f
            contentInsetTopWhenEndDrag = 0f
            dispatchRefreshStateChanged(RefreshViewState.IDLE)
        }
    }

    // 状态变化分发
    private fun dispatchRefreshStateChanged(state: RefreshViewState) {
        event.refreshStateDidChangeHandlerFn?.invoke(state)
    }
}
