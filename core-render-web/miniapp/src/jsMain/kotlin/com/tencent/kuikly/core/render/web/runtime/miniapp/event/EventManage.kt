package com.tencent.kuikly.core.render.web.runtime.miniapp.event

import com.tencent.kuikly.core.render.web.collection.set.JsSet
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniDocument
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement

/**
 * Handle mini program events
 */
object EventManage {
    // Drag events
    const val DRAG_BEGIN_EVENT = "dragstart"
    const val DRAG_MOVE_EVENT = "dragging"
    const val DRAG_END_EVENT = "dragend"

    // Touch events
    const val TOUCH_MOVE_EVENT = "touchmove"
    const val TOUCH_BEGIN_EVENT = "touchstart"
    const val TOUCH_END_EVENT = "touchend"

    // Change events, triggered by movableView
    const val CHANGE = "change"

    // Scroll events, triggered by scrollView
    const val SCROLL = "scroll"

    // Load events, triggered by image
    const val LOAD = "load"

    const val TAP = "tap"

    // Input events, triggered by textarea, input
    const val INPUT = "input"

    val firedEventFlagList = JsSet<String>()

    private const val DISPATCH_EVENT_NAME = "dispatchEvent"

    val eventHandler: (dynamic) -> Any? = { args: dynamic ->
        handleEvent(args)
    }

    init {
        // Listen to mini program events, dispatch event objects on the associated MiniElement
        EventHook.tap(
            hookName = DISPATCH_EVENT_NAME,
            callBack = {
                val event = it[0]
                val node = it[1].unsafeCast<MiniElement>()
                node.dispatchEvent(event)
            })

        MiniGlobal.globalThis.eventHandler = eventHandler
    }

    /**
     * Method to handle all mini program events, currently not processing return values, will add later if needed
     */
    private fun handleEvent(mpEvent: dynamic): dynamic {
        mpEvent.currentTarget = mpEvent.currentTarget ?: mpEvent.target

        if (mpEvent.type == TAP) {
            // simple implementation to prevent tap bubbling
            val eventFlag = (mpEvent.type + mpEvent.timeStamp).unsafeCast<String>()
            if (firedEventFlagList.has(eventFlag)) {
                return null
            }
        }

        val currentTarget = mpEvent.currentTarget
        val id = currentTarget?.dataset?.sid ?: currentTarget?.id ?: mpEvent.detail?.id
        val element: MiniElement? =
            MiniDocument.getElementById(id.unsafeCast<String>()).unsafeCast<MiniElement?>()
        if (element != null) {
            // Currently directly passing the mini program's MpEvent, will further abstract if needed later
            EventHook.call(DISPATCH_EVENT_NAME, MiniEvent(mpEvent.unsafeCast<Any>()), element)
        }

        // The content returned here will change the input content, need to return null
        return null
    }
}
