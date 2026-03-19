package com.tencent.kuikly.core.render.web.runtime.miniapp.event

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.isEmpty
import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.getOrPut

typealias CallBack = (params: Array<out Any>) -> Any?

/**
 * Generic EventEmitter implementation
 */
class EventEmitter {
    private val callbacks: JsMap<String, JsArray<CallBack>> = JsMap()

    /**
     * Add event callback
     */
    fun on(eventName: String, callback: CallBack): EventEmitter {
        val list = callbacks.getOrPut(eventName) { JsArray() }
        list.add(callback)
        return this
    }

    /**
     * Remove event callback
     */
    fun off(eventName: String? = null, callback: CallBack? = null): EventEmitter {
        if (eventName == null) {
            callbacks.clear()
        } else {
            callbacks[eventName]?.let {
                if (it.isEmpty()) {
                    callbacks.delete(eventName)
                }
                val index = it.findIndex { item ->
                    item == callback
                }
                if (index != -1) {
                    it.splice(index, 1)
                }
            }
        }

        return this
    }

    /**
     * Trigger event
     */
    fun trigger(eventName: String, vararg args: Any): EventEmitter {
        callbacks[eventName]?.let {
            it.forEach { callback ->
                callback.invoke(args)
            }
        }
        return this
    }

    /**
     * Trigger event and return the result of the last callback
     */
    fun triggerAndReturn(eventName: String, vararg args: Any): Any? {
        var result: Any? = null
        callbacks[eventName]?.let {
            it.forEach { callback ->
                result = callback.invoke(args)
            }
        }
        return result
    }
}
