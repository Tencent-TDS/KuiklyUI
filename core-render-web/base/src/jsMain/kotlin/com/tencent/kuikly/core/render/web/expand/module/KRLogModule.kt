package com.tencent.kuikly.core.render.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.utils.Log
import kotlin.js.Date

/**
 * Log module
 */
class KRLogModule : KuiklyRenderBaseModule() {
    /**
     * Return current time in string format, including milliseconds, similar to 2024-02-29 19:30:15.123 format
     */
    private fun getKRCurrentTime(): String {
        // Current timestamp
        val now = Date()
        // Timestamp format
        return "${
            now.getFullYear()
        }-${
            (now.getMonth() + 1).toString().padStart(2, '0')
        }-${
            now.getDate().toString().padStart(2, '0')
        } ${
            now.getHours().toString().padStart(2, '0')
        }:${
            now.getMinutes().toString().padStart(2, '0')
        }:${
            now.getSeconds().toString().padStart(2, '0')
        }.${
            now.getMilliseconds().toString().padStart(3, '0')
        }"
    }

    private fun logInfo(params: String?) {
        val msg = params ?: ""
        Log.info("${getKRCurrentTime()}|I|${tag(msg)}|$msg")
    }

    private fun logDebug(params: String?) {
        val msg = params ?: ""
        Log.log("${getKRCurrentTime()}|D|${tag(msg)}|$msg")
    }

    private fun logError(params: String?) {
        val msg = params ?: ""
        Log.error("${getKRCurrentTime()}|E|${tag(msg)}|$msg")
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        when (method) {
            METHOD_LOG_INFO -> logInfo(params)
            METHOD_LOG_DEBUG -> logDebug(params)
            METHOD_LOG_ERROR -> logError(params)
            else -> super.call(method, params, callback)
        }
        return null
    }

    companion object {
        const val MODULE_NAME = "KRLogModule"
        private const val METHOD_LOG_INFO = "logInfo"
        private const val METHOD_LOG_DEBUG = "logDebug"
        private const val METHOD_LOG_ERROR = "logError"

        /**
         * Get unified tag identifier for Kuikly logs
         */
        fun tag(msg: String): String {
            val prefix = "[KLog]["
            val suffix = "]:"
            val startIndex = msg.indexOf(prefix)
            if (startIndex != -1) {
                val endIndex = msg.indexOf(suffix, startIndex + prefix.length)
                if (endIndex != -1) {
                    return msg.substring(startIndex + prefix.length, endIndex)
                }
            }
            return ""
        }
    }
}
