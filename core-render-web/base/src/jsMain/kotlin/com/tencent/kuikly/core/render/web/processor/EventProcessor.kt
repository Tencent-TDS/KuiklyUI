package com.tencent.kuikly.core.render.web.processor

import org.w3c.dom.HTMLElement

/**
 * 需要不同平台通用的事件参数，需要在不同的宿主 render 中自行处理
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