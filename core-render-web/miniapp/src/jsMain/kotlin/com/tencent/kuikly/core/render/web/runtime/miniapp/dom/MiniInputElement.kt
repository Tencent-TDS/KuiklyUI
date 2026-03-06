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
            // Use lowercase 'maxlength' to match WeChat mini program input component attribute
            this.setAttribute("maxlength", value)
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
            // Map web input types to WeChat mini program input types
            val miniType = when (value) {
                "number" -> "number"  // Number keyboard
                "email" -> "text"     // Mini program doesn't have email type, use text
                "text" -> "text"      // Default text keyboard
                PASSWORD -> "text"    // Password uses text type with password attribute
                // New extended types for mini program
                "idcard" -> "idcard"
                "digit" -> "digit"
                "safe-password" -> "safe-password"
                "nickname" -> "nickname"
                else -> "text"
            }
            this.setAttribute("type", miniType)
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
        // Use lowercase 'maxlength' to match WeChat mini program input component attribute
        this.setAttribute("maxlength", maxLength)
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
        } else if (type == "change") {
            // Mini program's change event (triggered on blur or confirm)
            callback
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
