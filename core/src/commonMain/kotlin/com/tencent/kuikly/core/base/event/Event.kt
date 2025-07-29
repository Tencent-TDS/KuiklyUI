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

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.collection.fastHashMapOf
import com.tencent.kuikly.core.collection.fastLinkedMapOf

/**
 *  EventCenter类是整个event模块的核心类，该类包含了所有view都可以监听和处理的事件集合，并且给上层业务侧提供了便捷的事件监听方法。
 *
 *  整个事件中心里包含有两类事件集合：
 * 1、一类是标准的终端响应事件集合，比如click，doubleClick，longPress等，具体这些事件已经默认包含在EventCenter，并且在我们这个跨平台UI框架里，
 * 建立起了跟终端侧native事件的关联，上层业务可以直接使用。具体的事件类型可以参考[EventName]
 * 2、另一类是可以自定义的事件集合，这个主要是通过pluginEventsMap来实现的，通过注册和分发，EventCenter可以在不感知具体自定义事件的前提下，扩展更多的事件集合。
 * 我们已经默认提供[VisibilityEvent]这个扩展能力，具体的事件类型可以参考[VisibilityState]。简单来说，EventCenter自身是个事件集合，而且也能扩展包含其他的事件集合。
 *
 *  业务层使用该类的方式为在写UI时只需要设置event和具体事件就可以实现监听，不需要在业务逻辑代码中感知该类。示例如下：
 * ```
 *   obtainCouponItemView {          //这个是某个业务的view
 *       attr {
 *           couponItem = item
 *       }
 *       event {                     //希望监听该view的一些事件
 *           click {                 //监听这个view的点击事件
 *                       //---如下是click事件的处理代码
 *                       XXXXXXXXXXXXXXXX
 *                       XXXXXXXXXXXXXXXX
 *                       //---事件处理方法END
 *            }
 *            longPress {
 *                       //---如下是longPress事件的处理代码
 *                       XXXXXXXXXXXXXXXX
 *                       XXXXXXXXXXXXXXXX
 *                       //---事件处理方法END
 *            }
 *        }
 *    }
 *
 * ```
 * 整个事件模块也提供了两种扩展能力，分别针对指定类型扩展（指定view可见）和通用扩展（所有view可见）：
 * 1、如果希望特定的view才能看到新的事件，那么就继承EventCenter。
 * 2、如果希望所有view都能看到新的事件，那么通过putPluginEvent来注入新的事件中心。具体可以参考[VisibilityEvent]
 *
 */
open class Event : BaseEvent() {
    private val syncMap = fastHashMapOf<String, Int>()

    private val pluginEventsMap by lazy(LazyThreadSafetyMode.NONE) { fastLinkedMapOf<String, IEvent>() }
    /* 内部监听动画结束事件，用于分发命令式动画使用 */
    internal var internalAnimationCompletion: ((AnimationCompletionParams)-> Unit)? = null
    /**
     * 跟基类相比，增加了一个isSync的参数
     * @param isSync 是否需要实时同步用户操作，比如拖拽手势等。普通事件不需要，所以默认值为false
     */
    fun register(eventName: String, eventHandlerFn: EventHandlerFn, isSync: Boolean = false) {
        super.register(eventName, eventHandlerFn)
        syncMap[eventName] = isSync.toInt()
        getRenderView()?.also {
            it.setEvent(eventName, isSync.toInt())
        }
    }

    override fun register(eventName: String, eventHandlerFn: EventHandlerFn) {
        super.register(eventName, eventHandlerFn)
        register(eventName, eventHandlerFn, false)
    }

    override fun isEmpty(): Boolean {
        return super.isEmpty() && pluginEventsMap.isEmpty()
    }

    /**
     * 获取某个事件名称对应的事件处理函数
     * @param eventName 事件名称
     * @return 对应的事件处理函数
     */
    fun handlerWithEventName(eventName: String): EventHandlerFn? {
        return eventMap[eventName]
    }

    /**
     * 增加扩展的事件中心
     * @param pluginName 扩展的事件中心的名称
     * @param event 扩展的事件中心
     */
    fun putPluginEvent(pluginName: String, event: IEvent) {
        if (pluginName.isNotEmpty()) {
            pluginEventsMap[pluginName] = event
        }
    }

    /**
     * 获取对应的事件中心扩展
     * @param pluginName 事件中心的名称
     * @return 对应的事件中心
     */
    fun getPluginEvent(pluginName: String): IEvent? {
        return pluginEventsMap[pluginName]
    }

    /**
     * 具体实现上，我们约定一个事件只有一个处理函数，所以当找到并触发处理函数后就返回了
     */
    override fun onFireEvent(eventName: String, data: Any?): Boolean {
        if (super.onFireEvent(eventName, data)) {
            return true
        }
        // 遍历各个插件
        pluginEventsMap.values.forEach {
            if (it.onFireEvent(eventName, data)) {
                return true
            }
        }
        return false
    }

    override fun onViewDidRemove() {
        super.onViewDidRemove()
        pluginEventsMap.values.forEach {
            it.onViewDidRemove()
        }
        pluginEventsMap.clear()
    }

    override fun onRenderViewDidCreated() {
        setEventsToRenderView()
        pluginEventsMap.values.forEach {
            it.onRenderViewDidCreated()
        }
    }

    override fun onRenderViewDidRemoved() {
        pluginEventsMap.values.forEach {
            it.onRenderViewDidRemoved()
        }
    }

    override fun onRelativeCoordinatesDidChanged(view: DeclarativeBaseView<*, *>) {
        if (pluginEventsMap.isEmpty()) return
        pluginEventsMap.values.forEach {
            it.onRelativeCoordinatesDidChanged(view)
        }
    }

    override fun onViewLayoutFrameDidChanged(view: DeclarativeBaseView<*, *>) {
        if (pluginEventsMap.isEmpty()) return
        pluginEventsMap.values.forEach {
            it.onViewLayoutFrameDidChanged(view)
        }
    }

    /**
     * 单击事件的定义
     * @param handler 事件处理函数
     */
    open fun click(handler: (ClickParams) -> Unit) {
        this.register(EventName.CLICK.value) {
            handler(ClickParams.decode(it))
        }
    }

    /**
     * 双击事件的定义
     * @param handler 事件处理函数
     */
    open fun doubleClick(handler: (ClickParams) -> Unit) {
        this.register(EventName.DOUBLE_CLICK.value) {
            handler(ClickParams.decode(it))
        }
    }

    /**
     * 长按事件的定义
     * @param handler 事件处理函数
     */
    open fun longPress(handler: (LongPressParams) -> Unit) {
        this.register(EventName.LONG_PRESS.value) {
            handler(LongPressParams.decode(it))
        }
    }

    /**
     * 滑动事件的定义
     * @param handler 事件处理函数
     */
    open fun pan(handler: (PanGestureParams) -> Unit) {
        this.register(EventName.PAN.value) {
            handler(PanGestureParams.decode(it))
        }
    }

    /**
     * 捏合事件的定义
     * @param handler 事件处理函数
     */
    open fun pinch(handler: (PinchGestureParams) -> Unit) {
        this.register(EventName.PINCH.value) {
            handler(PinchGestureParams.decode(it))
        }
    }

    /**
     * 动画结束事件的扩展定义
     * @param handler 事件处理函数
     */
    open fun animationCompletion(handler: (AnimationCompletionParams) -> Unit) {
        this.register(EventName.ANIMATION_COMPLETE.value) {
            val params = AnimationCompletionParams.decode(it)
            handler(params)
            this.internalAnimationCompletion?.invoke(params)
        }
    }

    internal fun listenInternalAnimationCompletion(handler: (AnimationCompletionParams) -> Unit) {
        this.internalAnimationCompletion = handler
        val eventHandler = handlerWithEventName(EventName.ANIMATION_COMPLETE.value)
        if (eventHandler == null) {
            this.register(EventName.ANIMATION_COMPLETE.value) {
                this.internalAnimationCompletion?.invoke(AnimationCompletionParams.decode(it))
            }
        }
    }

    private fun setEventsToRenderView() {
        getRenderView()?.also {
            eventMap.keys.forEach { key ->
                var sync = 0
                syncMap[key]?.also {
                    sync = it
                }
                it.setEvent(key, sync)
            }
        }
    }

    companion object {
        const val TAG = "EventCenter"
    }
}
