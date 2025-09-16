package utils

import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * Declare URL decode method for JS execution environment
 */
external fun decodeURIComponent(encoded: String): String

/**
 * Unified UI operation encapsulation
 */
object Ui {
    // Default duration for toast display
    private const val TOAST_DELAY_DEFAULT = 3000

    /**
     * Show toast message on page
     */
    internal fun showToast(message: JSONObject) {
        val content = message.optString("content")
        if (content != "") {
            // Show message
            val wrapDiv = document.createElement("div")
            val contentDiv = document.createElement("div")
            wrapDiv.classList.add("toast-wrapper")
            contentDiv.classList.add("toast-content")
            contentDiv.innerHTML = content
            wrapDiv.appendChild(contentDiv)
            // Add wrapper
            document.body?.appendChild(wrapDiv)
            // Remove after timeout
            window.setTimeout({
                document.body?.removeChild(wrapDiv)
            }, TOAST_DELAY_DEFAULT)
        }
    }
}

/**
 * Unified URL operation encapsulation
 */
object URL {
    /**
     * Format and return URL parameters
     */
    internal fun parseParams(url: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (url.contains("?")) {
            val query = url.substringAfter("?")

            if (query != "") {
                // Only process if there are query parameters
                query.split("&").forEach { param ->
                    val (name, value) = param.split("=")
                    params[name] = decodeURIComponent(value)
                }
            }
        }
        return params
    }
}
