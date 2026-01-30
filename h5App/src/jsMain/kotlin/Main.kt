package com.tencent.kuikly.h5app

import com.tencent.kuikly.core.render.web.expand.module.KRNotifyModule
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event
import com.tencent.kuikly.h5app.manager.KuiklyRouter
import com.tencent.kuikly.h5app.processor.CustomImageProcessor

/**
 * WebApp entry, use renderView delegate method to initialize and create renderView
 */
fun main() {
    // Takes over control if "use_spa=1" is present in URL or ENABLE_BY_DEFAULT is true
    if (KuiklyRouter.handleEntry()) {
        return
    }

    console.log("##### Kuikly H5 #####")

    // Create and initialize the page delegator using shared logic
    val delegator = KuiklyRouter.createDelegator(window.location.href)

    // modify image cdn
//    KuiklyProcessor.imageProcessor = CustomImageProcessor

    // Register visibility event
    document.addEventListener("visibilitychange", {
        val hidden = document.asDynamic().hidden as Boolean
        if (hidden) {
            // Page hidden
            delegator.pause()
        } else {
            // Page restored
            delegator.resume()
        }
    })

    // Register Kuikly event listener for Web host to receive events from Kuikly pages
    // When Kuikly page calls NotifyModule.postNotify(), Web host can receive the event here
    registerKuiklyEventListener()
}

/**
 * Register listener to receive events from Kuikly pages
 * 
 * Usage in Kuikly page:
 * ```kotlin
 * acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
 *     .postNotify("your_event_name", JSONObject().apply { put("key", "value") })
 * ```
 */
private fun registerKuiklyEventListener() {
    window.addEventListener("kuikly_to_host_event", { event: Event ->
        val detail = event.asDynamic().detail
        val eventName = detail.eventName as? String ?: ""
        val data = detail.data as? String ?: "{}"
        
        console.log("[Web Host] Received Kuikly event: $eventName")
        console.log("[Web Host] Event data: $data")
    })
    console.log("[Web Host] Kuikly event listener registered")
}
