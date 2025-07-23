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

package com.tencent.kuikly.core.base.event

import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.MutableFrame
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.IScrollerViewEventObserver
import com.tencent.kuikly.core.views.ScrollParams
import com.tencent.kuikly.core.views.ScrollerView
import kotlin.math.max
import kotlin.math.min

/**
 * 此事件中心类提供了用于监听view是否可见的事件。
 * 主要是 将要可见、完全可见、将要不可见、完全不可见 四种事件。这些事件一般可以用于曝光等的上报。
 */
class VisibilityEvent : BaseEvent(), IScrollerViewEventObserver {

    // 用于获取对应的listView
    private var listViewNativeRef: Int = 0

    // 内部保存的可见性状态
    private var visibilityState = VisibilityState.DID_DISAPPEAR
    // 内部保存的可见百分比
    private var appearPercentage = 0f

    /**
     * 当ListView滑动时，回调该方法
     * @param contentOffsetX X轴的变化值
     * @param contentOffsetY Y轴的变化值
     */
    override fun onContentOffsetDidChanged(contentOffsetX: Float, contentOffsetY: Float, params: ScrollParams) {
        getView()?.also {
            if (it is DeclarativeBaseView<*, *>) {
                onRelativeCoordinatesDidChanged(it)
            }
        }
    }

    /**
     * 当界面完成布局后回调该方法
     */
    override fun subViewsDidLayout() {
        getView()?.let {
            if (it is DeclarativeBaseView<*, *>) {
                onRelativeCoordinatesDidChanged(it)
            }
        }
    }

    override fun onRenderViewDidRemoved() {
        if (listViewNativeRef > 0) {
            getPager().getViewWithNativeRef(listViewNativeRef)?.let {
                if (it is ScrollerView<*, *>) {
                    (it).removeScrollerViewEventObserver(this)
                }
            }
            listViewNativeRef = 0
        }
    }

    override fun onViewDidRemove() {
        super.onViewDidRemove()

        //做下不可见的状态处理
        if (!isEmpty()) {
            handleLeaving(!getPager().isWillDestroy())
            dispatchAppearPercentageEvent(!getPager().isWillDestroy(), 0f)
        }
    }

    override fun onRenderViewDidCreated() {
        //do nothing
    }

    override fun onRelativeCoordinatesDidChanged(view: DeclarativeBaseView<*, *>) {
        if (isEmpty() || view.nativeRef != viewId || view.parent == null) {
            return
        }
        // 找到最近一个list，或者 pager
        var targetVisibleWindow: DeclarativeBaseView<*, *>? = view.parent
        val viewFrame = view.flexNode.layoutFrame
        val frameInWindow = MutableFrame(viewFrame.x, viewFrame.y, viewFrame.width, viewFrame.height)
        while (targetVisibleWindow != null
            && !(targetVisibleWindow is ScrollerView<*, *> || targetVisibleWindow is Pager)
        ) {
            frameInWindow.x += targetVisibleWindow.flexNode.layoutFrame.x
            frameInWindow.y += targetVisibleWindow.flexNode.layoutFrame.y
            targetVisibleWindow = targetVisibleWindow.parent
        }
        generateVisibilityState(targetVisibleWindow, frameInWindow)
    }

    override fun onViewLayoutFrameDidChanged(view: DeclarativeBaseView<*, *>) {

    }

    // 跟进界面变化来触发状态变化并通知
    private fun generateVisibilityState(
        targetVisibleWindow: DeclarativeBaseView<*, *>?,
        frameInWindow: MutableFrame
    ) {
        if (targetVisibleWindow == null) {
            updateViewVisibility(VisibilityState.DID_DISAPPEAR)
            return
        }
        var windowFrame = targetVisibleWindow.flexNode.layoutFrame
        if (targetVisibleWindow is ScrollerView<*, *>) {
            val marginTop = (targetVisibleWindow as ScrollerView<*, *>).getViewAttr().visibleAreaIgnoreTopMargin
            val marginBottom = (targetVisibleWindow as ScrollerView<*, *>).getViewAttr().visibleAreaIgnoreBottomMargin
            frameInWindow.x -= targetVisibleWindow.curOffsetX
            frameInWindow.y -= (targetVisibleWindow.curOffsetY + marginTop)
            if (listViewNativeRef != targetVisibleWindow.nativeRef) {
                listViewNativeRef = targetVisibleWindow.nativeRef
                targetVisibleWindow.addScrollerViewEventObserver(this)
            }
            if (marginTop > 0f || marginBottom > 0f) {
                val mutableWindowFrame = windowFrame.toMutableFrame()
                mutableWindowFrame.height -= (marginTop + marginBottom)
                windowFrame = mutableWindowFrame.toFrame()
            }
        }
        handleState(windowFrame, frameInWindow, targetVisibleWindow as? ScrollerView<*, *>)
    }

    //判断最终是什么状态
    private fun handleState(
        targetVisibleWindow: Frame,
        frameInWindow: MutableFrame,
        targetScrollerWindow:ScrollerView<*, *>? = null
    ) {
        val windowWidth = targetVisibleWindow.width
        val windowHeight = targetVisibleWindow.height
        if (eventMap.containsKey(appearPercentageEventName)) {
            handleAppearPercentage(targetVisibleWindow, frameInWindow, targetScrollerWindow)
        }
        if (isLeaving(frameInWindow, windowWidth, windowHeight)
        ) { // 相离时
            handleLeaving(true)
        } else if (isContaining(frameInWindow, windowWidth, windowHeight)
        ) { // 包含时
            handleContaining()
        } else { // 相交时
            handleCrossing()
        }
    }

    private fun handleAppearPercentage(
        targetVisibleWindow: Frame,
        frameInWindow: MutableFrame,
        targetScrollerWindow:ScrollerView<*, *>? = null
    ) {
        val windowWidth = targetVisibleWindow.width
        val windowHeight = targetVisibleWindow.height
        var percentage = 0f
        if (isLeaving(frameInWindow, windowWidth, windowHeight)) {
            percentage = 0f
        } else if (isContaining(frameInWindow, windowWidth, windowHeight)) {
            percentage = 1f
        } else {
            var left = frameInWindow.y
            var right = frameInWindow.y + frameInWindow.height
            var length = windowHeight
            var selfLength = frameInWindow.height
            // 横向列表
            if (targetScrollerWindow?.flexNode?.flexDirection == FlexDirection.ROW || targetScrollerWindow?.flexNode?.flexDirection == FlexDirection.ROW_REVERSE ) {
                left = frameInWindow.x
                right = frameInWindow.x + frameInWindow.width
                length = windowWidth
                selfLength = frameInWindow.width
            }
            if (left <= 0 && right >= 0) {
                percentage = right * 1f / selfLength
            } else if (length in left..right) {
                percentage = (length - max(0f, left) ) / selfLength * 1f
            }  else if (left >= 0 && right <= length) {
                percentage = (right - left) / selfLength * 1f
            }
        }
        percentage = max(min(percentage, 1f), 0f)
        if (appearPercentage != percentage) {
            appearPercentage = percentage
            dispatchAppearPercentageEvent(true, percentage)
        }
    }

    private fun dispatchAppearPercentageEvent(async: Boolean, percentage: Float) {
        if (eventMap.containsKey(appearPercentageEventName)) {
            performTask(async) {
                onFireEvent(appearPercentageEventName, percentage)
            }
        }
    }

    private fun isContaining(
        frameInWindow: MutableFrame,
        windowWidth: Float,
        windowHeight: Float
    ) = (frameInWindow.x >= 0 && frameInWindow.x + frameInWindow.width <= windowWidth
            && frameInWindow.y >= 0 && frameInWindow.y + frameInWindow.height <= windowHeight)

    private fun isLeaving(
        frameInWindow: MutableFrame,
        windowWidth: Float,
        windowHeight: Float
    ) = (frameInWindow.x + frameInWindow.width <= 0
            || frameInWindow.x >= windowWidth
            || frameInWindow.y + frameInWindow.height <= 0
            || frameInWindow.y >= windowHeight)

    private fun handleCrossing() {
        if (visibilityState == VisibilityState.DID_DISAPPEAR) {
            updateViewVisibility(VisibilityState.WILL_APPEAR)
        } else if (visibilityState == VisibilityState.DID_APPEAR) {
            updateViewVisibility(VisibilityState.WILL_DISAPPEAR)
        }
    }

    private fun handleContaining() {
        if (visibilityState == VisibilityState.WILL_DISAPPEAR
            || visibilityState == VisibilityState.DID_DISAPPEAR
        ) {
            updateViewVisibility(VisibilityState.WILL_APPEAR)
            updateViewVisibility(VisibilityState.DID_APPEAR)
        } else {
            updateViewVisibility(VisibilityState.DID_APPEAR)
        }
    }

    private fun handleLeaving(async: Boolean = true) {
        if (visibilityState == VisibilityState.WILL_APPEAR
            || visibilityState == VisibilityState.DID_APPEAR
        ) {
            updateViewVisibility(VisibilityState.WILL_DISAPPEAR, async)
            updateViewVisibility(VisibilityState.DID_DISAPPEAR, async)
        } else {
            updateViewVisibility(VisibilityState.DID_DISAPPEAR, async)
        }
    }

    //最终设置状态并通知
    private fun updateViewVisibility(newState: VisibilityState, async: Boolean = true) {
        if (visibilityState != newState) {
            visibilityState = newState
            performTask(async) {
                onFireEvent(newState.value, JSONObject())
            }
        }
    }

    private fun performTask(async: Boolean, task: () -> Unit) {
        if (async) {
            getPager().addNextTickTask(task)
        } else {
            task()
        }
    }

    companion object {
        const val PLUGIN_NAME = "VisibilityEvent"
        const val TAG = "VisibilityEventCenter"
    }
}

enum class VisibilityState(val value: String) {
    WILL_APPEAR("willAppear"),  // 将要可见
    DID_APPEAR("didAppear"),   // 完全可见
    WILL_DISAPPEAR("willDisappear"), // 将要不可见
    DID_DISAPPEAR("didDisappear"), // 完全不可见
}

const val appearPercentageEventName = "appearPercentage"

/**
 * view将要可见事件的扩展定义。
 * 在最近的listView中将要出现时回调该事件，若找不到最近的listView，则以为Pager作为可见窗口
 * @param handler 事件处理函数
 */
fun Event.willAppear(handler: EventHandlerFn) {
    getVisibilityPlugin().register(VisibilityState.WILL_APPEAR.value, handler)
}

/**
 * view完全可见事件的扩展定义。
 * 在最近的listView中完全出现时回调该事件，若找不到最近的listView，则以为Pager作为可见窗口
 * @param handler 事件处理函数
 */
fun Event.didAppear(handler: EventHandlerFn) {
    getVisibilityPlugin().register(VisibilityState.DID_APPEAR.value, handler)
}

/**
 * view将要消失事件的扩展定义。
 * 在最近的listView中将要消失时回调该事件，若找不到最近的listView，则以为Pager作为可见窗口
 * @param handler 事件处理函数
 */
fun Event.willDisappear(handler: EventHandlerFn) {
    getVisibilityPlugin().register(VisibilityState.WILL_DISAPPEAR.value, handler)
}

/**
 * view完全不可见事件的扩展定义。
 * 在最近的listView中完全不可见时回调该事件，若找不到最近的listView，则以为Pager作为可见窗口
 * @param handler 事件处理函数
 */
fun Event.didDisappear(handler: EventHandlerFn) {
    getVisibilityPlugin().register(VisibilityState.DID_DISAPPEAR.value, handler)
}
/**
 * view露出可见百分比的扩展定义。
 * 在最近的listView中完全不可见时回调该事件，若找不到最近的listView，则以为Pager作为可见窗口
 * @param handler 事件处理函数 (percentage01为[0,1]的露出百分比，1为100%，0为0%])
 */
fun Event.appearPercentage(handler: (percentage01: Float) -> Unit) {
    getVisibilityPlugin().register(appearPercentageEventName) {
        handler.invoke(it as Float)
    }
}

//获取所属的事件中心插件，如果不存在需要创建并put进去
private fun Event.getVisibilityPlugin(): IEvent {
    var plugin = this.getPluginEvent(VisibilityEvent.PLUGIN_NAME)
    if (plugin == null) {
        plugin = VisibilityEvent().also {
            it.init(this.pagerId, this.viewId)
        }
        this.putPluginEvent(VisibilityEvent.PLUGIN_NAME, plugin)
    }
    return plugin
}
