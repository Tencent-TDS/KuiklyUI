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
import com.tencent.kuikly.core.base.Size
import com.tencent.kuikly.core.layout.Frame

/*
 * View的布局位置变化时回调（此时已经布局完成）
 */
fun Event.layoutFrameDidChange(handlerFn: (layoutFrame: Frame) -> Unit) {
    getFramePlugin().layoutFrameChangedHandlerFn = handlerFn
}

fun Event.notifyLayoutFrameDidChange(layoutFrame: Frame) {
    getFramePlugin().layoutFrameChangedHandlerFn?.invoke(layoutFrame)
}

fun Event.renderViewDidCreated(handlerFn: EventHandlerFn) {
    getFramePlugin().renderViewDidCreatedHandlerFn = handlerFn
}

fun Event.renderViewDidRemoved(handlerFn: EventHandlerFn) {
    getFramePlugin().renderViewDidRemovedHandlerFn = handlerFn
}

/**
 * 添加 LayoutFrameChange 事件监听，不覆盖原先的监听器
 */
fun Event.addLayoutFrameDidChange(handlerFn: (layoutFrame: Frame) -> Unit) {
    val oldHandlerFn = getFramePlugin().layoutFrameChangedHandlerFn
    if (oldHandlerFn == null) {
        getFramePlugin().layoutFrameChangedHandlerFn = handlerFn
    } else {
        getFramePlugin().layoutFrameChangedHandlerFn = { frame ->
            handlerFn(frame)
            oldHandlerFn(frame)
        }
    }
}

internal class FrameEvent : BaseEvent() {
    internal var layoutFrameChangedHandlerFn: ((Frame) -> Unit)? = null
    internal var renderViewDidCreatedHandlerFn: (EventHandlerFn)? = null
    internal var renderViewDidRemovedHandlerFn: (EventHandlerFn)? = null
    override fun onRenderViewDidCreated() {
        renderViewDidCreatedHandlerFn?.invoke(null)
    }

    override fun onRenderViewDidRemoved() {
        renderViewDidRemovedHandlerFn?.invoke(null)
    }

    override fun onRelativeCoordinatesDidChanged(view: DeclarativeBaseView<*, *>) {

    }

    override fun onViewLayoutFrameDidChanged(view: DeclarativeBaseView<*, *>) {
        if (layoutFrameChangedHandlerFn != null) {
            getPager().addTaskWhenPagerUpdateLayoutFinish {
                layoutFrameChangedHandlerFn?.invoke(view.flexNode.layoutFrame)
            }
        }
    }

    override fun onViewDidRemove() {
        super.onViewDidRemove()
        layoutFrameChangedHandlerFn = null
        renderViewDidCreatedHandlerFn = null
        renderViewDidRemovedHandlerFn = null
    }

    companion object {
        const val PLUGIN_NAME = "FrameEvent"
    }

}

//获取所属的事件中心插件，如果不存在需要创建并put进去
private fun Event.getFramePlugin(): FrameEvent {
    var plugin = this.getPluginEvent(FrameEvent.PLUGIN_NAME)
    if (plugin == null) {
        plugin = FrameEvent().also {
            it.init(this.pagerId, this.viewId)
        }
        this.putPluginEvent(FrameEvent.PLUGIN_NAME, plugin)
    }
    return plugin as FrameEvent
}
