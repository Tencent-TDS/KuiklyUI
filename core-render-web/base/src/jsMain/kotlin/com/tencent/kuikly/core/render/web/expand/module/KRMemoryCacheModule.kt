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
            // Mini-program / non-browser environment.
            //
            // We MUST resolve real width/height before reporting Complete, otherwise
            // upstream callers (e.g. compose KuiklyImageBitmap) will paint with a 0x0
            // destination size and the picture won't show up at all.
            //
            // Strategy:
            //   1. Pre-seed the cache with the resolved src string so that any synchronous
            //      drawImage path that does not depend on width/height (rare) still has
            //      something to work with.
            //   2. Try to create a real Image via an offscreen canvas (`wx.createOffscreenCanvas`)
            //      and load asynchronously. On `onload` we replace the cached value with the
            //      decoded image object and notify the caller via callback.
            //   3. Synchronously return InProgress so compose treats the bitmap as not yet
            //      ready and waits for the callback to flip status to Complete.
            cacheMap[cacheKey] = resolvedSrc
            loadMiniProgramImageAsync(cacheKey, resolvedSrc, callback)
            return JSONObject().apply {
                put("state", STATE_IN_PROGRESS)
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
     * Mini-program asynchronous image loader.
     *
     * Uses an offscreen Canvas 2D node (`wx.createOffscreenCanvas({ type: '2d' })`) to
     * create a real Image object that supports data: URLs, http(s) URLs and resolved
     * asset paths. Once loaded we:
     *   - replace `cacheMap[cacheKey]` with the decoded Image so canvas drawImage gets
     *     a ready-to-paint object instead of a string;
     *   - invoke `callback` with the resolved width/height + Complete state so the
     *     upstream compose layer flips its `status` and triggers a recompose.
     */
    private fun loadMiniProgramImageAsync(
        cacheKey: String,
        resolvedSrc: String,
        callback: KuiklyRenderCallback?
    ) {
        try {
            val plat: dynamic = js(
                "(typeof wx !== 'undefined') ? wx : ((typeof qq !== 'undefined') ? qq : null)"
            )
            if (plat == null) {
                notifyImageLoadFailed(cacheKey, callback, "no mini-program global")
                return
            }
            val offscreen: dynamic = try {
                plat.createOffscreenCanvas(js("({ type: '2d' })"))
            } catch (_: Throwable) {
                null
            }
            val img: dynamic = offscreen?.createImage()
            if (img == null) {
                notifyImageLoadFailed(cacheKey, callback, "createImage unavailable")
                return
            }
            img.onload = {
                val w = (img.width as? Number)?.toInt() ?: 0
                val h = (img.height as? Number)?.toInt() ?: 0
                // Cache the decoded Image object so drawImage skips the lazy createImage
                // path and paints synchronously when invoked later.
                cacheMap[cacheKey] = img.unsafeCast<Any>()
                callback?.invoke(JSONObject().apply {
                    put("state", STATE_COMPLETE)
                    put("errorCode", 0)
                    put("errorMsg", "")
                    put("cacheKey", cacheKey)
                    put("width", w)
                    put("height", h)
                })
            }
            img.onerror = {
                notifyImageLoadFailed(cacheKey, callback, "image load error")
            }
            img.src = resolvedSrc
        } catch (t: Throwable) {
            notifyImageLoadFailed(cacheKey, callback, t.message ?: "unknown error")
        }
    }

    private fun notifyImageLoadFailed(
        cacheKey: String,
        callback: KuiklyRenderCallback?,
        errorMsg: String
    ) {
        callback?.invoke(JSONObject().apply {
            put("state", STATE_COMPLETE)
            put("errorCode", -1)
            put("errorMsg", errorMsg)
            put("cacheKey", cacheKey)
            put("width", 0)
            put("height", 0)
        })
    }

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
