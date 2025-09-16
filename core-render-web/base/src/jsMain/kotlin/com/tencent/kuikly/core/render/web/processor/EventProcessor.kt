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
    val pageX: Int
    val pageY: Int
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
}