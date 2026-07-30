package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage.TOUCH_BEGIN_EVENT
import com.tencent.kuikly.core.render.web.scheduler.KuiklyRenderCoreContextScheduler

/**
 * Mini program view node, which will eventually be rendered as a view in the mini program,
 * determining whether to render as a static view based on whether events have been bound
 */
@JsName("MiniDivElement")
class MiniDivElement(
    nodeName: String = TransformConst.VIEW,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    // Element's actual distance from the left side of the page
    private var eleX = 0f

    // Element's actual distance from the top of the page
    private var eleY = 0f

    // Whether node position acquisition has been completed
    private var hasInitElePosition = false

    // New: Used to save timer handles for cleanup
    private val retryTimers = JsArray<Int>()

    // Whether the touch event has been bind
    private var isBindTouchEvent = false

    /**
     * Get the current node's position
     */
    private fun getElePosition() {
        if (parentElement == null) {
            return
        }
        getBoundingClientRectPromise().then<dynamic> { rect ->
            if (rect != null) {
                // Element's distance from the left side of the page
                eleX = rect.left.unsafeCast<Float>()
                // Element's distance from the top of the page
                eleY = rect.top.unsafeCast<Float>()
                hasInitElePosition = true
            }
        }
    }

    @JsName("getBoundingClientRect")
    fun getBoundingClientRect(): dynamic {
        val clientRect = js("{}")
        clientRect.left = eleX
        clientRect.top = eleY
        clientRect.height = offsetHeight
        clientRect.width = offsetWidth
        return clientRect
    }

    /**
     * Determine whether to use a static view based on whether events are being listened for
     */
    override fun onTransformData(): String {
        if (!hasAddEventListener) {
            return TransformConst.STATIC_VIEW
        }
        return super.onTransformData()
    }

    /**
     * override add event listener for div
     */
    override fun addEventListener(type: String, callback: EventHandler, options: dynamic) {
        if (type == TOUCH_BEGIN_EVENT) {
            // If it is a touch event, should do some special things for mini app
            if (!isBindTouchEvent) {
                // bind touch event schedule handle
                isBindTouchEvent = true
                // After initialization, try to get the node's position as soon as possible, generally
                // won't slide the node right away
                KuiklyRenderCoreContextScheduler.scheduleTask(50) {
                    fun scheduleRetry(attempt: Int = 0, nextDelay: Int = 0) {
                        if (attempt >= 4 || hasInitElePosition) return
                        val handle = MiniGlobal.setTimeout({
                            getElePosition()
                            // From the second time onwards, delay time increases as 500ms -> 1000ms -> 2000ms
                            scheduleRetry(attempt + 1, if (nextDelay == 0) 500 else nextDelay * 2)
                        }, nextDelay)
                        retryTimers.add(handle)
                    }

                    scheduleRetry() // Execute immediately the first time (0ms delay)
                }
            }
            // re calculate the node's position
            getElePosition()
        }

        super.addEventListener(type, callback, options)
    }
}
