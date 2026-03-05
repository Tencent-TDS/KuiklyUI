package com.tencent.kuikly.core.render.web.processor

import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

/**
 * common event, should implement in different host
 */
interface IEvent {
    val screenX: Int
    val screenY: Int
    val clientX: Int
    val clientY: Int
    val offsetX: Int
    val offsetY: Int
    val pageX: Int
    val pageY: Int
}

var IEvent.state: String?
    get() = asDynamic().state as? String
    set(value) {
        asDynamic().state = value
    }

/**
 * common event processor
 */
interface IEventProcessor {
    /**
     * process double click event
     */
    fun doubleClick(ele: HTMLElement, callback: (event: IEvent?) -> Unit)

    /**
     * process long press event
     */
    fun longPress(ele: HTMLElement, callback: (event: IEvent?) -> Unit)

    /**
     * process pan event
     */
    fun pan(ele: HTMLElement, callback: (event: IEvent?) -> Unit)

    /**
     * 派发鼠标事件(针对ListView中由于阻止冒泡导致window、ListView元素 无法监听到鼠标事件的情况)
     * @param type 事件类型
     * @param event 事件
     * @param ele 元素 若为空则默认派发给 window
     */
    fun dispatchMouseEvent(type:String, event: Event, ele: Element? = null)
}