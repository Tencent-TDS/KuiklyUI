package com.tencent.kuikly.core.render.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor

/**
 * Kuikly memory cache module
 */
class KRMemoryCacheModule : KuiklyRenderBaseModule() {
    private val cacheMap = mutableMapOf<String, Any>()

    /**
     * Handle method calls
     */
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            // Method for caching objects in memory cache module
            SET_OBJECT -> {
                val json = JSONObject(params ?: "{}")
                // Get value to cache
                val value = json.opt("value") ?: return null
                val key = json.optString("key")
                // Associate key with value
                set(key, value)
                null
            }
            // Pre-load and cache an image so that KRCanvasView.drawImage can use it later.
            // The core layer issues this as a synchronous call expecting a JSON string back
            // containing at least { state, cacheKey, width, height }.
            CACHE_IMAGE -> cacheImage(params, callback)
            else -> null
        }
    }

    /**
     * Get memory cache value by key
     */
    fun <T> get(key: String): T? = cacheMap[key].unsafeCast<T?>()

    /**
     * Associate key with value
     */
    fun set(key: String, value: Any) {
        cacheMap[key] = value
    }

    /**
     * Pre-load and cache an image. Two execution paths:
     *
     * - In a browser environment we create an HTMLImageElement and load it asynchronously,
     *   immediately returning InProgress. Once the image is loaded the callback fires with
     *   the natural width / height.
     * - In a mini-program environment (no global Image constructor) we cannot create a real
     *   image element here, so we just cache the resolved src string and immediately return
     *   Complete; the canvas drawImage on mini-program side will receive the path directly.
     */
    private fun cacheImage(params: String?, callback: KuiklyRenderCallback?): String {
        val json = JSONObject(params ?: "{}")
        val src = json.optString("src")
        if (src.isEmpty()) {
            return JSONObject().apply {
                put("state", STATE_COMPLETE)
                put("errorCode", -1)
                put("errorMsg", "empty src")
                put("cacheKey", "")
                put("width", 0)
                put("height", 0)
            }.toString()
        }
        // Use the original src as cacheKey to keep alignment with other platforms.
        // The actual URL handed to <img> / mini-program canvas needs platform-specific
        // resolution (e.g. assets:// -> assets/ on H5, /assets/ on mini-program).
        val cacheKey = src
        val resolvedSrc = resolveImageSrc(src)

        if (!isBrowserEnv()) {
            // Mini-program / non-browser environment: just remember the resolved path
            // so drawImage on mini-program side can hand it to canvas directly.
            cacheMap[cacheKey] = resolvedSrc
            return JSONObject().apply {
                put("state", STATE_COMPLETE)
                put("errorCode", 0)
                put("errorMsg", "")
                put("cacheKey", cacheKey)
                put("width", 0)
                put("height", 0)
            }.toString()
        }

        // Reuse an already-cached HTMLImageElement if present.
        val existing = cacheMap[cacheKey]
        val image: dynamic = if (existing != null) {
            existing.asDynamic()
        } else {
            val img: dynamic = js("new Image()")
            cacheMap[cacheKey] = img.unsafeCast<Any>()
            // Only enable CORS for real cross-origin http(s) URLs. Setting it for
            // relative / data: URLs is unnecessary and can break loading when the
            // server does not emit Access-Control-Allow-Origin headers.
            if (resolvedSrc.startsWith("http://") || resolvedSrc.startsWith("https://")) {
                img.crossOrigin = "anonymous"
            }
            img.onload = {
                callback?.invoke(JSONObject().apply {
                    put("state", STATE_COMPLETE)
                    put("errorCode", 0)
                    put("errorMsg", "")
                    put("cacheKey", cacheKey)
                    put("width", img.naturalWidth as? Int ?: 0)
                    put("height", img.naturalHeight as? Int ?: 0)
                })
            }
            img.onerror = {
                callback?.invoke(JSONObject().apply {
                    put("state", STATE_COMPLETE)
                    put("errorCode", -1)
                    put("errorMsg", "image load error")
                    put("cacheKey", cacheKey)
                    put("width", 0)
                    put("height", 0)
                })
            }
            img.src = resolvedSrc
            img
        }

        // Always return Complete + cacheKey synchronously so callers that only inspect
        // the synchronous return value (matching iOS/Android behaviour where bitmap
        // decoding is synchronous) can immediately use the cacheKey. If the underlying
        // <img> hasn't finished decoding yet, KRCanvasView.drawImage will wait for the
        // load event and repaint when ready.
        val w = image.naturalWidth as? Int ?: 0
        val h = image.naturalHeight as? Int ?: 0
        return JSONObject().apply {
            put("state", STATE_COMPLETE)
            put("errorCode", 0)
            put("errorMsg", "")
            put("cacheKey", cacheKey)
            put("width", w)
            put("height", h)
        }.toString()
    }

    /**
     * Detect whether we are running in a real browser (has the global Image constructor).
     */
    private fun isBrowserEnv(): Boolean =
        js("typeof Image !== 'undefined' && typeof window !== 'undefined'") as Boolean

    /**
     * Resolve internal image scheme (e.g. assets://, file://) to a real URL the host
     * platform can actually load. data:, http(s): and other already-loadable schemes
     * are passed through untouched. Falls back to the original src if no processor is
     * available yet (e.g. very early initialisation).
     */
    private fun resolveImageSrc(src: String): String {
        // data: URLs are loadable as-is
        if (src.startsWith("data:")) return src
        return try {
            KuiklyProcessor.imageProcessor.getImageAssetsSource(src)
        } catch (_: Throwable) {
            src
        }
    }

    companion object {
        const val MODULE_NAME = "KRMemoryCacheModule"
        const val SET_OBJECT = "setObject"
        const val CACHE_IMAGE = "cacheImage"

        private const val STATE_IN_PROGRESS = "InProgress"
        private const val STATE_COMPLETE = "Complete"
    }
}
