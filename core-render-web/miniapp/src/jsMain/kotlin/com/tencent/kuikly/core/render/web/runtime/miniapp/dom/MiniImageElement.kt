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
    
    // Current image mode
    private var currentMode = ""

    init {
        style.onStyleSet = ::resetStyleSet
        style.onStyleGet = ::resetStyleGet
    }

    private fun resetStyleGet(styleName: String, defaultValue: Any): Any {
        if (styleName == OBJECT_FIT) {
            val mode = getAttribute(MODE_ATTR).unsafeCast<String>()
            // adapt image cover value for mini app
            return when (mode) {
                MODE_SCALE_FILL -> "stretch"
                MODE_ASPECT_FIT -> "contain"
                MODE_WIDTH_FIX -> "widthFix"
                MODE_HEIGHT_FIX -> "heightFix"
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

                "widthFix" -> {
                    MODE_WIDTH_FIX
                }

                "heightFix" -> {
                    MODE_HEIGHT_FIX
                }

                else -> MODE_ASPECT_FILL
            }
            currentMode = mode
            setAttribute(MODE_ATTR, mode)
            
            // For widthFix mode, remove height to allow mini program to auto calculate
            // For heightFix mode, remove width to allow mini program to auto calculate
            if (mode == MODE_WIDTH_FIX) {
                style.setProperty("height", null)
            } else if (mode == MODE_HEIGHT_FIX) {
                style.setProperty("width", null)
            }
            
            return false
        }
        
        // For widthFix mode, skip setting height to allow auto calculation
        if (styleName == "height" && currentMode == MODE_WIDTH_FIX) {
            return false
        }
        
        // For heightFix mode, skip setting width to allow auto calculation
        if (styleName == "width" && currentMode == MODE_HEIGHT_FIX) {
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
        private const val MODE_WIDTH_FIX = "widthFix"
        private const val MODE_HEIGHT_FIX = "heightFix"
    }
}
