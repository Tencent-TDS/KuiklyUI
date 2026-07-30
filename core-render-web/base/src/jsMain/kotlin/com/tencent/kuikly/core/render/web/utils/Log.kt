package com.tencent.kuikly.core.render.web.utils

import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor


/**
 * Logging utility class
 */
object Log {
    /**
     * Log debug information
     */
    fun trace(vararg msg: Any) {
        // Only log in development environment
        if (KuiklyProcessor.isDev) {
            log(msg)
        }
    }

    /**
     * Log info level messages
     */
    fun info(vararg msg: Any) {
        js("console.info.apply(console, msg)")
    }

    /**
     * Log general messages
     */
    fun log(vararg msg: Any) {
        js("console.log.apply(console, msg)")
    }

    /**
     * Log warning messages
     */
    fun warn(vararg msg: Any) {
        js("console.warn.apply(console, msg)")
    }

    /**
     * Log error messages
     */
    fun error(vararg msg: Any) {
        js("console.error.apply(console, msg)")
    }
}
