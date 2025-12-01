package com.tencent.kuikly.h5app

import kotlinx.browser.document
import kotlinx.browser.window
import com.tencent.kuikly.h5app.manager.KuiklyRouter

/**
 * WebApp entry, use renderView delegate method to initialize and create renderView
 */
fun main() {
    // modify image cdn
    // KuiklyProcessor.imageProcessor = CustomImageProcessor

    // Takes over control if "use_spa=1" is present in URL or ENABLE_BY_DEFAULT is true
    if (KuiklyRouter.handleEntry()) {
        return
    }

    console.log("##### Kuikly H5 #####")

    // Create and initialize the page delegator using shared logic
    val delegator = KuiklyRouter.createDelegator(window.location.href)

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
}
