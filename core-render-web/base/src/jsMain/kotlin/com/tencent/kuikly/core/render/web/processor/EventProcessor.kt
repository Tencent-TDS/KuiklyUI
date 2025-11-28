package com.tencent.kuikly.core.render.web.processor

import org.w3c.dom.HTMLElement

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
}