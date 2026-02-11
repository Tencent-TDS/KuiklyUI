package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst
import com.tencent.kuikly.core.render.web.scheduler.KuiklyRenderCoreContextScheduler

/**
 * Mini program textarea node, eventually rendered as textarea in mini program
 */
class MiniTextAreaElement(
    nodeName: String = TransformConst.TEXT_AREA,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    @JsName("placeholder")
    var placeholder: String = ""
        set(value) {
            this.setAttribute("placeholder", value)
            field = value
        }
    @JsName("maxLength")
    var maxLength: Int = -1
        set(value) {
            // Note: WeChat mini program textarea uses 'maxlength' (lowercase) attribute
            // But Transform.kt maps it as 'maxLength', so we use the mapped name
            this.setAttribute("maxLength", value)
            field = value
        }

    init {
        this.setAttribute("maxLength", maxLength)
    }

    // Mini program doesn't have readOnly, using disabled instead
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
            field = value
        }

    @JsName("value")
    var value: String = ""
        set(value) {
            this.setAttribute("value", value)
            field = value
        }

    @JsName("focus")
    fun focus() {
        KuiklyRenderCoreContextScheduler.scheduleTask(100) {
            setAttribute("focus", "true")
        }
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
        } else if (type == "linechange") {
            // Mini program's linechange event (triggered when line count changes)
            callback
        } else {
            callback
        }
        super.addEventListener(type, inputCallback, options)
    }
}
