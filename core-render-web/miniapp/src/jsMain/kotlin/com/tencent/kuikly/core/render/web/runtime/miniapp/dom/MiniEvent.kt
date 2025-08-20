package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.remove
import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.set
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage.TAP

typealias EventHandler = ((dynamic) -> Unit)

/**
 * Abstract event class for simulating DOM events
 */
class MiniEvent {
    private var handlers: JsMap<String, JsArray<EventHandler>?> = JsMap()

    /**
     * Listen for events
     */
    fun addEventListener(type: String, callback: EventHandler, options: dynamic) {
        var isOnce = false
        if (options != null && jsTypeOf(options) == "object") {
            isOnce = options["once"].unsafeCast<Boolean?>() ?: false
        }
        val usedType = type.lowercase()
        val currentHandlers = handlers[usedType]
        var usedHandler = callback

        if (isOnce) {
            usedHandler = {
                callback(it)
                removeEventListener(type, usedHandler)
            }
            addEventListener(type, usedHandler, null)
            return
        }

        if (currentHandlers == null) {
            val newList = JsArray<EventHandler>()
            newList.add(callback)
            handlers[usedType] = newList
        } else {
            currentHandlers.add(callback)
        }
    }

    /**
     * Remove event listener
     */
    fun removeEventListener(type: String, callback: EventHandler?) {
        val usedType = type.lowercase()
        if (callback == null) {
            handlers[usedType] = null
            return
        }
        val currentHandlers = handlers[usedType] ?: return
        currentHandlers.remove(callback)
    }

    /**
     * Dispatch event
     * Here the event is a mini program event object
     */
    fun dispatchEvent(event: dynamic): Boolean {
        val usedType = event.type.toLowerCase().unsafeCast<String>()
        val currentHandlers = handlers[usedType] ?: return false
        if (event.type == TAP) {
            val eventFlag = (event.type + event.timeStamp).unsafeCast<String>()
            EventManage.firedEventFlagList.add(eventFlag)
            MiniGlobal.setTimeout({
                EventManage.firedEventFlagList.clear()
            }, 0)
        }
        currentHandlers.forEach { handler ->
            handler.invoke(event)
        }
        return true
    }

    /**
     * check is hasEventListen
     */
    fun hasEventListener(type: String): Boolean {
        val eventListenerList = handlers[type]
        return eventListenerList != null && eventListenerList.length > 0
    }
}
