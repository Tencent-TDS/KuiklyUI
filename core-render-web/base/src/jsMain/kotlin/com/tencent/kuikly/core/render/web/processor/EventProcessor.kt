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
     * Dispatch mouse event (for the case where event bubbling is prevented in ListView, causing window and ListView elements unable to listen to mouse events)
     * @param type event type
     * @param event event
     * @param ele element, if null, defaults to dispatch to window
     */
    fun dispatchMouseEvent(type:String, event: Event, ele: Element? = null)
}