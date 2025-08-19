package com.tencent.kuikly.core.render.web.runtime.miniapp.processor

import com.tencent.kuikly.core.render.web.processor.IEvent
import com.tencent.kuikly.core.render.web.processor.IEventProcessor
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.MiniEvent
import org.w3c.dom.HTMLElement

/**
 * mini app touch event
 */
data class MiniTouchEvent(
    override val screenX: Int,
    override val screenY: Int,
    override val clientX: Int,
    override val clientY: Int,
    override val pageX: Int,
    override val pageY: Int
) : IEvent

/**
 * mini app common event processor
 */
object EventProcessor : IEventProcessor {
    /**
     * process event callback
     */
    private fun handleEventCallback(event: MiniEvent, callback: (event: IEvent?) -> Unit) {
        val touch = event.unsafeCast<MiniEvent>().touches[0]
        if (jsTypeOf(touch) != "undefined") {
            callback(
                MiniTouchEvent(
                    screenX = touch.screenX,
                    screenY = touch.screenY,
                    clientX = touch.clientX,
                    clientY = touch.clientY,
                    pageX = touch.pageX,
                    pageY = touch.pageY
                )
            )
        }
    }

    /**
     * bind mini app double click event
     */
    override fun doubleClick(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        ele.addEventListener("tap", { event: dynamic ->
            val target = ele.asDynamic()
            val nowTime = js("Date.now()")
            val clickTime = target["clickTime"] as Int? ?: 0
            val dbTime = 500
            if (nowTime - clickTime < dbTime) {
                // double click，trigger event
                target["clickTime"] = 0
                handleEventCallback(event.unsafeCast<MiniEvent>(), callback)
            } else {
                // single tap, record click time
                target["clickTime"] = nowTime
            }
        })
    }

    /**
     * bind mini app long press event
     */
    override fun longPress(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        ele.addEventListener("longpress", { event: dynamic ->
            handleEventCallback(event.unsafeCast<MiniEvent>(), callback)
        })
    }
}