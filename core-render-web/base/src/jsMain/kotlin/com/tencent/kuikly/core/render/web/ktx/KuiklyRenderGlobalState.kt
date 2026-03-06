package com.tencent.kuikly.core.render.web.ktx

/**
 * Global state centralized management, avoid using unless necessary
 */
object GlobalState {
    /**
     * Marks whether scrolling has occurred, used to prevent click events from triggering during scrolling
     */
    var hasScrolled: Boolean = false
}