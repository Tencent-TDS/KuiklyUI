package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.setPlaceholderColor
import com.tencent.kuikly.core.render.web.ktx.toNumberFloat
import com.tencent.kuikly.core.render.web.ktx.toPxF
import com.tencent.kuikly.core.render.web.ktx.toRgbColor
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.scheduler.KuiklyRenderCoreContextScheduler
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent

/**
 * KRTextAreaView, corresponding to Kuikly's TextArea
 */
class KRTextAreaView : IKuiklyRenderViewExport {
    // Text change event callback
    private var textDidChangedEventCallback: KuiklyRenderCallback? = null

    // Focus event callback
    private var focusedEventCallback: KuiklyRenderCallback? = null

    // Blur event callback
    private var blurEventCallback: KuiklyRenderCallback? = null

    // Return key click callback
    private var clickReturnEventCallback: KuiklyRenderCallback? = null

    // Text length limit exceeded callback
    private var textLengthLimitEventCallback: KuiklyRenderCallback? = null

    // Text input element
    private val textarea = kuiklyDocument.createElement(ElementType.TEXT_AREA).apply {
        val style = this.unsafeCast<HTMLTextAreaElement>().style
        style.border = "none"
        style.backgroundColor = "transparent"
    }

    private var currentLength = 0

    override val ele: HTMLTextAreaElement
        get() = textarea.unsafeCast<HTMLTextAreaElement>()

    /**
     * Adapt differences between web and kotlin
     */
    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            SRC -> {
                ele.value = propValue.unsafeCast<String>()
                // Notify content change
                notifyTextValueChanged(ele.value)
                true
            }

            TEXT_DID_CHANGE -> {
                // Text change callback event, web needs adaptation, initiate notification
                textDidChangedEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                ele.addEventListener("input", {
                    notifyTextValueChanged(ele.value)
                })
                true
            }

            PLACEHOLDER -> {
                ele.placeholder = propValue.unsafeCast<String>()
                true
            }

            PLACEHOLDER_COLOR -> {
                // set through pseudo-class
                setPlaceholderColor(ele, propValue.unsafeCast<String>().toRgbColor())
                true
            }

            TEXT_ALIGN -> {
                ele.style.textAlign = propValue.unsafeCast<String>()
                true
            }

            FONT_WEIGHT -> {
                ele.style.fontWeight = propValue.unsafeCast<String>()
                true
            }

            FONT_SIZE -> {
                ele.style.fontSize = propValue.toNumberFloat().toPxF()
                true
            }

            MAX_TEXT_LENGTH -> {
                ele.maxLength = propValue.unsafeCast<Int>()
                true
            }

            EDIT_ABLE -> {
                ele.readOnly = propValue.unsafeCast<Int>() != 1
                true
            }

            AUTO_FOCUS -> {
                ele.autofocus = propValue.unsafeCast<Int>() == 1
                true
            }

            TINT_COLOR -> {
                ele.style.asDynamic().caretColor = propValue.unsafeCast<String>().toRgbColor()
                true
            }

            KEYBOARD_TYPE -> {
                // textarea does not support input type
                true
            }

            RETURN_KEY_TYPE -> {
                setReturnKeyType(propValue.unsafeCast<String>())
                true
            }

            INPUT_FOCUS -> {
                // Focus event callback
                focusedEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                ele.addEventListener("focus", {
                    val map = mutableMapOf<String, Any>()
                    map["text"] = ele.value
                    // Notify kotlin side
                    focusedEventCallback?.invoke(map)
                })
                true
            }

            INPUT_BLUR -> {
                // Blur event callback
                blurEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                ele.addEventListener("blur", {
                    val map = mutableMapOf<String, Any>()
                    map["text"] = ele.value
                    // Notify kotlin side
                    blurEventCallback?.invoke(map)
                })
                true
            }

            INPUT_RETURN -> {
                clickReturnEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                ele.addEventListener("keydown", {
                    val event = it.unsafeCast<KeyboardEvent>()
                    // Keyboard event
                    if (event.key === "Enter" || event.keyCode == 13) {
                        val map = mutableMapOf<String, Any>()
                        map["text"] = ele.value
                        // Return key clicked
                        clickReturnEventCallback?.invoke(map)
                    }
                })
                true
            }

            TEXT_LENGTH_BEYOND_LIMIT -> {
                textLengthLimitEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                // Whether it is in text combination state
                var isComposing = false

                ele.addEventListener("compositionstart", { isComposing = true })
                ele.addEventListener("compositionend", {
                    currentLength = ele.value.length + 1
                    isComposing = false
                    if (ele.maxLength > 0 && currentLength > ele.maxLength) {
                        val map = mutableMapOf<String, Any>()
                        map["text"] = ele.value
                        textLengthLimitEventCallback?.invoke(map)
                        ele.value = ele.value.substring(0, ele.maxLength)
                    }
                })
                ele.addEventListener("beforeinput", {
                    // Input text exceeds maximum limit, callback notification
                    val event = it.unsafeCast<InputEvent>()
                    if (event.isComposing || isComposing) return@addEventListener
                    // 针对safari浏览器中，若输入超过最大长度时，inserted为空的情况，采用手动计数方式
                    if (event.asDynamic().inputType == "insertText") {
                        currentLength = ele.value.length + 1
                    } else if (event.asDynamic().inputType == "deleteContentBackward") {
                        currentLength = ele.value.length - 1
                    }
                    val inserted = it.unsafeCast<InputEvent>().data ?: ""
                    val newLength = ele.value.length + inserted.length
                    if (ele.maxLength > 0 && (newLength > ele.maxLength || currentLength > ele.maxLength)) {
                        // Cancel the default behavior of this input event
                        it.preventDefault()
                        val map = mutableMapOf<String, Any>()
                        map["text"] = ele.value
                        textLengthLimitEventCallback?.invoke(map)
                    }
                })
                true
            }

            else -> super.setProp(propKey, propValue)
        }
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            SET_TEXT -> {
                // Set input value
                val text = params ?: return null
                ele.value = text
                // Notify content change
                notifyTextValueChanged(ele.value)
            }

            FOCUS -> {
                // Input gets focus, considering UI element insertion event issues, need to schedule execution
                KuiklyRenderCoreContextScheduler.scheduleTask {
                    ele.focus()
                }
            }

            BLUR -> {
                // Input loses focus, considering UI element insertion event issues, need to schedule execution
                KuiklyRenderCoreContextScheduler.scheduleTask {
                    ele.blur()
                }
            }

            GET_CURSOR_INDEX -> {
                // get input cursor index
                KuiklyRenderCoreContextScheduler.scheduleTask {
                    callback?.invoke(mapOf(
                        "cursorIndex" to ele.selectionStart
                    ))
                }
            }

            SET_CURSOR_INDEX -> {
                val index = params?.toIntOrNull() ?: return null
                // set input cursor index, focus first
                ele.focus()
                ele.setSelectionRange(index, index)
            }

            else -> super.call(method, params, callback)
        }
    }

    /**
     * Text content changed, notify kuikly side
     */
    private fun notifyTextValueChanged(text: String) {
        val map = mutableMapOf<String, Any>()
        map["text"] = text
        // Notify kotlin side
        textDidChangedEventCallback?.invoke(map)
    }

    /**
     * Set return key type
     */
    private fun setReturnKeyType(returnKeyType: String) {
        // 支持的返回键类型集合
        val supportedTypes = setOf("search", "send", "done", "go")

        val returnKey = if (returnKeyType in supportedTypes) {
            returnKeyType
        } else {
            // default
            "next"
        }
        ele.asDynamic().enterKeyHint = returnKey
    }

    companion object {
        const val VIEW_NAME = "KRTextAreaView"

        // Properties
        private const val SRC = "text"
        private const val PLACEHOLDER = "placeholder"
        private const val PLACEHOLDER_COLOR = "placeholderColor"
        private const val TEXT_ALIGN = "textAlign"
        private const val FONT_SIZE = "fontSize"
        private const val FONT_WEIGHT = "fontWeight"
        private const val TINT_COLOR = "tintColor"
        private const val MAX_TEXT_LENGTH = "maxTextLength"
        private const val AUTO_FOCUS = "autofocus"
        private const val EDIT_ABLE = "editable"
        private const val KEYBOARD_TYPE = "keyboardType"
        private const val RETURN_KEY_TYPE = "returnKeyType"

        // Methods
        private const val SET_TEXT = "setText"
        private const val FOCUS = "focus"
        private const val BLUR = "blur"
        private const val GET_CURSOR_INDEX = "getCursorIndex"
        private const val SET_CURSOR_INDEX = "setCursorIndex"

        // Events
        private const val TEXT_DID_CHANGE = "textDidChange"
        private const val INPUT_FOCUS = "inputFocus"
        private const val INPUT_BLUR = "inputBlur"
        private const val INPUT_RETURN = "inputReturn"
        private const val TEXT_LENGTH_BEYOND_LIMIT = "textLengthBeyondLimit"
    }
}
