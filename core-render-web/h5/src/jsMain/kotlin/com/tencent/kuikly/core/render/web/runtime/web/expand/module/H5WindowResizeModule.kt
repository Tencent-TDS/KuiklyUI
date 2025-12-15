package com.tencent.kuikly.core.render.web.runtime.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import kotlinx.browser.window
import org.w3c.dom.events.Event
import kotlin.js.Date

class H5WindowResizeModule: KuiklyRenderBaseModule() {
    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_LISTEN_WINDOW_SIZE_CHANGE -> {
                listenWindowSizeChange(callback)
                true
            }
            REMOVE_LISTEN_WINDOW_SIZE_CHANGE -> {
                removeListenWindowSizeChange()
                true
            }

            else -> super.call(method, params, callback)
        }
    }

    /**
     * 监听页面 resize 事件并回调
     */
    private fun listenWindowSizeChange(callback: KuiklyRenderCallback?){
        H5WindowResizeHandler.listenWindowSizeChange(callback)
    }

    private fun removeListenWindowSizeChange(){
        H5WindowResizeHandler.removeListenWindowSizeChange()
    }

    companion object {
        const val MODULE_NAME = "WindowResizeModule"
        private const val METHOD_LISTEN_WINDOW_SIZE_CHANGE = "listenWindowSizeChange"
        private const val REMOVE_LISTEN_WINDOW_SIZE_CHANGE = "removeListenWindowSizeChange"
    }
}

object H5WindowResizeHandler {
    private var callback: KuiklyRenderCallback? = null
    private var isListening = false
    private var timer: Int? = null
    private var lastInvokeTime = 0L
    private var listener: ((Event) -> Unit)? = null
    private const val LISTEN_WINDOW_SIZE_CHANGE_INTERVAL = 100
    fun listenWindowSizeChange(callback: KuiklyRenderCallback?) {
        this.callback = callback
        isListening = true
        listener = {
            val currentTime = Date.now().toLong()
            if (currentTime - lastInvokeTime >= LISTEN_WINDOW_SIZE_CHANGE_INTERVAL) {
                lastInvokeTime = currentTime
                callback?.invoke(
                    mapOf(
                        "width" to window.innerWidth,
                        "height" to window.innerHeight,
                    )
                )
                timer?.let { window.clearTimeout(it) }
                timer = null
            } else {
                timer?.let { window.clearTimeout(it) }
                timer = window.setTimeout({
                    lastInvokeTime = Date.now().toLong()
                    callback?.invoke(
                        mapOf(
                            "width" to window.innerWidth,
                            "height" to window.innerHeight,
                        )
                    )
                    timer = null
                }, LISTEN_WINDOW_SIZE_CHANGE_INTERVAL)
            }
        }
        window.addEventListener("resize", listener!!)
    }

    fun removeListenWindowSizeChange() {
        isListening = false
        listener?.let { window.removeEventListener("resize", it) }
        listener = null
    }
}