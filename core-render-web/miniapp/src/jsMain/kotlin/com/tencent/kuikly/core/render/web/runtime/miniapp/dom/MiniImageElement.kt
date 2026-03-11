package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst

/**
 * Mini program image node, which will eventually be rendered as image in the mini program
 */
class MiniImageElement(
    nodeName: String = TransformConst.IMAGE,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    // Original set src, temporarily stored for later conversion and value setting
    private var rawSrc = ""

    init {
        style.onStyleSet = ::resetStyleSet
        style.onStyleGet = ::resetStyleGet
    }

    override fun addEventListener(type: String, callback: EventHandler, options: dynamic) {
        if (type.lowercase() == "load") {
            val wrappedCallback: EventHandler = { event ->
                val detail = event.unsafeCast<dynamic>().detail
                val w = detail?.width?.unsafeCast<Int?>()
                val h = detail?.height?.unsafeCast<Int?>()
                if (w != null && w > 0) setAttribute("naturalWidth", w.toString())
                if (h != null && h > 0) setAttribute("naturalHeight", h.toString())
                callback(event)
            }
            super.addEventListener(type, wrappedCallback, options)
        } else {
            super.addEventListener(type, callback, options)
        }
    }

    private fun resetStyleGet(styleName: String, defaultValue: Any): Any {
        if (styleName == OBJECT_FIT) {
            val mode = getAttribute(MODE_ATTR).unsafeCast<String>()
            // adapt image cover value for mini app
            return when (mode) {
                MODE_SCALE_FILL -> "stretch"
                MODE_ASPECT_FIT -> "contain"
                else -> "cover"
            }
        }
        return defaultValue
    }

    private fun resetStyleSet(styleName: String, value: Any): Boolean {
        if (styleName == OBJECT_FIT) {
            // adapt image cover value for mini app
            val mode = when (value) {
                "stretch" -> {
                    MODE_SCALE_FILL
                }

                "contain" -> {
                    MODE_ASPECT_FIT
                }

                "cover" -> {
                    MODE_ASPECT_FILL
                }
                else -> MODE_ASPECT_FILL
            }
            setAttribute(MODE_ATTR, mode)
            return false
        }
        return true
    }

    // Image scaling mode
    @JsName("mode")
    var mode = ""

    // Image URL
    @JsName("src")
    var src: String
        get() = rawSrc
        set(value) {
            if (rawSrc == value) {
                return
            }
            rawSrc = value
            setAttribute("src", value)
        }

    @JsName("naturalWidth")
    val naturalWidth: Int
        get() {
            val width = getAttribute("naturalWidth") ?: return 0

            return width.unsafeCast<String>().toInt()
        }

    @JsName("naturalHeight")
        val naturalHeight: Int
        get() {
            val height = getAttribute("naturalHeight") ?: return 0

            return height.unsafeCast<String>().toInt()
        }

    companion object {
        private const val MODE_ATTR = "mode"
        private const val OBJECT_FIT = "objectFit"
        private const val MODE_SCALE_FILL = "scaleToFill"
        private const val MODE_ASPECT_FIT = "aspectFit"
        private const val MODE_ASPECT_FILL = "aspectFill"
    }
}
