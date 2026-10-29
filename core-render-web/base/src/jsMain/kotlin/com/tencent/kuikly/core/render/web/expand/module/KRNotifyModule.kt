package com.tencent.kuikly.core.render.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * Event callback wrapper type
 */
private data class KRCallbackWrapper(
    val callbackId: String,
    val callback: KuiklyRenderCallback
)

/**
 * Notifications are shared by live Kuikly pages in the same Web runtime (including cached SPA pages).
 */
open class KRNotifyModule : KuiklyRenderBaseModule() {
    // Notification event Map, cache registered event callbacks
    private val toKRMap: MutableMap<String, MutableList<KRCallbackWrapper>> = mutableMapOf()

    init {
        // register this instance for global notifications
        moduleInstances.add(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // clean up when module is destroyed
        moduleInstances.remove(this)
        toKRMap.clear()
    }

    /**
     * Add event listener
     */
    private fun addNotify(params: String?, callback: KuiklyRenderCallback?) {
        val cb = callback ?: return
        val jsonStr = params ?: return
        val jsonObject = JSONObject(jsonStr)
        // Event name
        val eventName = jsonObject.optString(KEY_EVENT_NAME)
        // Event ID
        val id = jsonObject.optString(KEY_ID)
        // Skip if no event name or event ID
        if (eventName == "" || id == "") {
            return
        }
        // Get callback list
        var callbackList: MutableList<KRCallbackWrapper>? = toKRMap[eventName]
        if (callbackList == null) {
            // Initialize callback list
            callbackList = mutableListOf()
            toKRMap[eventName] = callbackList
        }
        // Add callback
        callbackList.add(KRCallbackWrapper(id, cb))
    }

    /**
     * Remove event listener
     */
    private fun removeNotify(params: String?) {
        val jsonStr = params ?: return
        val jsonObject = JSONObject(jsonStr)
        // Event name
        val eventName = jsonObject.optString(KEY_EVENT_NAME)
        // Event ID
        val id = jsonObject.optString(KEY_ID)
        // Skip if no event name or event ID
        if (eventName == "" || id == "") {
            return
        }
        // Remove specified callback
        toKRMap[eventName]?.also {
            val size = it.size
            for (i in 0 until size) {
                val wrapper = it[i]
                if (wrapper.callbackId == id) {
                    it.removeAt(i)
                    break
                }
            }
            if (it.isEmpty()) {
                // Remove callback list for this event name when no callbacks remain
                toKRMap.remove(eventName)
            }
        }
    }

    /**
     * Trigger event listener callbacks
     */
    private fun postNotify(params: String?) {
        val jsonStr = params ?: return
        val jsonObject = JSONObject(jsonStr)
        // Event name
        val eventName = jsonObject.optString(KEY_EVENT_NAME)
        // Event parameters
        val data = jsonObject.optString(KEY_DATA)
        // Execute callbacks if event name exists
        if (eventName != "") {
            // Include the sender once and notify cached SPA pages before returning.
            dispatchGlobalEventData(eventName, data)
            // Notify the host once per post, rather than once per receiving page.
            dispatchToHost(eventName, data)
        }
    }

    /**
     * Dispatch event to Web host using CustomEvent
     * Web host can listen via: window.addEventListener('kuikly_notify', (e) => { ... })
     */
    private fun dispatchToHost(eventName: String, data: String) {
        val detail = js("{ eventName: eventName, data: data }")
        val eventInit = js("{ detail: detail, bubbles: true, cancelable: true }")
        val customEvent = js("new CustomEvent('kuikly_notify', eventInit)")
        window.dispatchEvent(customEvent.unsafeCast<Event>())
    }

    /**
     * Execute registered event callbacks
     */
    private fun dispatchEvent(eventName: String, data: String) {
        // Callbacks may remove listeners while handling a notification.
        toKRMap[eventName]?.toList()?.forEach {
            it.callback.invoke(data)
        }
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_ADD_NOTIFY -> addNotify(params, callback)
            METHOD_REMOVE_NOTIFY -> removeNotify(params)
            METHOD_POST_NOTIFY -> postNotify(params)
            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRNotifyModule"
        private const val METHOD_ADD_NOTIFY = "addNotify"
        private const val METHOD_REMOVE_NOTIFY = "removeNotify"
        private const val METHOD_POST_NOTIFY = "postNotify"
        const val KEY_EVENT_NAME = "eventName"
        const val KEY_DATA = "data"
        private const val KEY_ID = "id"

        /**
         * Event name for Web host to listen Kuikly events
         * Usage: window.addEventListener('kuikly_notify', (e) => {
         *     const { eventName, data } = e.detail;
         *     console.log('Received Kuikly event:', eventName, data);
         * });
         */
        const val HOST_EVENT_NAME = "kuikly_notify"

        // track all KRNotifyModule instances for global notifications
        private val moduleInstances = mutableSetOf<KRNotifyModule>()

        // dispatch global event to all KRNotifyModule instances
        fun dispatchGlobalEvent(eventName: String, data: JSONObject) {
            dispatchGlobalEventData(eventName, data.toString())
        }

        private fun dispatchGlobalEventData(eventName: String, data: String) {
            // A callback can create or destroy a page during dispatch.
            moduleInstances.toList().forEach { module ->
                if (module in moduleInstances) {
                    module.dispatchEvent(eventName, data)
                }
            }
        }
    }
}
