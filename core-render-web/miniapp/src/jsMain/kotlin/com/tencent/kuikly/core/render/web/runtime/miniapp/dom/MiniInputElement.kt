package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.ktx.toPxF
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst

/**
 * Mini program input node, which will eventually be rendered as input in the mini program
 */
class MiniInputElement(
    nodeName: String = TransformConst.INPUT,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    private val defaultFontSize = 13

    @JsName("placeholder")
    var placeholder: String = ""
        set(value) {
            this.setAttribute("placeholder", value)
            field = value
        }

    @JsName("maxLength")
    var maxLength: Int = -1
        set(value) {
            this.setAttribute("maxLength", value)
            field = value
        }

    @JsName("readOnly")
    var readOnly: Boolean = false
        set(value) {
            this.setAttribute("disabled", value)
            field = value
        }

    @JsName("autofocus")
    var autofocus: Boolean = false
        set(value) {
            this.setAttribute("focus", value)
            field = value
        }

    @JsName("type")
    var type: String = "text"
        set(value) {
            this.setAttribute("type", value)
            if (value == PASSWORD) {
                this.setAttribute(PASSWORD, true)
            }
            field = value
        }

    /**
     * Empty string may be set here, need to call setAttributeForce to force setting
     */
    @JsName("value")
    var value: String = ""
        set(value) {
            field = value
            setAttributeForce(VALUE, value)
        }

    init {
        // Initialize some default styles, otherwise it will display abnormally
        this.style.fontSize = defaultFontSize.toPxF()
        this.setAttribute("value", value)
        this.setAttribute("placeholder", placeholder)
        this.setAttribute("maxLength", maxLength)
        this.setAttribute("disabled", readOnly)
        this.setAttribute("type", type)
    }

    @JsName("focus")
    fun focus() {
        setAttribute("focus", true)
    }

    @JsName("blur")
    fun blur() {
        removeAttribute("focus")
    }

    /**
     * Add event listener
     */
    override fun addEventListener(type: String, callback: EventHandler, options: dynamic) {
        val inputCallback = if (type == "input") {
            {   event ->
                if (jsTypeOf(event.target.value) != "undefined") {
                    // input event return value
                    value = event.target.value.unsafeCast<String>()
                }
                callback(event)
            }
        } else {
            callback
        }
        super.addEventListener(type, inputCallback, options)
    }

    companion object {
        private const val VALUE = "value"
        private const val PASSWORD = "password"
    }
}
