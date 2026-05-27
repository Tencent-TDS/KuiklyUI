package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.setPlaceholderColor
import com.tencent.kuikly.core.render.web.ktx.setSelectionColor
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

    // Keyboard height change callback (iOS/Android native parity)
    private var keyboardHeightChangeCallback: KuiklyRenderCallback? = null

    // Whether a VisualViewport-based keyboard listener has been bound (H5 only).
    private var keyboardTrackingBound = false
    // Last reported keyboard height, used to de-dup resize events.
    private var lastKeyboardHeight: Float = 0f

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
                val rgbColor = propValue.unsafeCast<String>().toRgbColor()
                // On mini-program, `ele` is a MiniTextAreaElement which advertises
                // `__krSupportsPlaceholderColor = true` and hosts a `placeholderColor` setter
                // that forwards the value to WX native `<textarea>`'s `placeholder-style`
                // attribute. On H5 / real browsers, `ele` is a plain HTMLTextAreaElement with
                // no such marker, so we keep the original `::placeholder` pseudo-class
                // injection unchanged to preserve H5 behavior.
                if (jsTypeOf(ele.asDynamic().__krSupportsPlaceholderColor) != "undefined") {
                    ele.asDynamic().placeholderColor = rgbColor
                } else {
                    // set through pseudo-class
                    setPlaceholderColor(ele, rgbColor)
                }
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

            SELECTION_COLOR -> {
                setSelectionColor(ele, propValue.unsafeCast<String>().toRgbColor())
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

            KEYBOARD_HEIGHT_CHANGE -> {
                keyboardHeightChangeCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                // Listen for a unified DOM-level `keyboardheightchange` event on this element.
                // - On mini-program, MiniTextAreaElement translates WX native `bindkeyboardheightchange`
                //   into this DOM event and already provides `{height, duration, curve}` in detail.
                // - On H5 browsers, there is no native keyboardheightchange DOM event on <textarea>,
                //   so we additionally bind a VisualViewport-based tracker (see below) that
                //   dispatches the same DOM event on this element.
                ele.addEventListener(EVENT_KEYBOARD_HEIGHT_CHANGE, {
                    val detail = it.asDynamic().detail
                    val height = (detail?.height ?: 0).unsafeCast<Number>().toFloat()
                    val duration = (detail?.duration ?: 0).unsafeCast<Number>().toFloat()
                    val curve = (detail?.curve ?: 0).unsafeCast<Number>().toInt()
                    val map = mutableMapOf<String, Any>()
                    map["height"] = height
                    map["duration"] = duration
                    map["curve"] = curve
                    keyboardHeightChangeCallback?.invoke(map)
                })
                bindKeyboardHeightTrackingIfNeeded()
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
     * Bind a VisualViewport-based keyboard height tracker.
     *
     * Rationale: browsers (H5) do not emit a `keyboardheightchange` DOM event on <textarea>.
     * On mobile browsers the soft-keyboard shrinks the visual viewport, so the delta
     * `window.innerHeight - visualViewport.height` approximates the keyboard height.
     *
     * We only track while this textarea owns focus, so that viewport resize from
     * orientation change or browser UI does not accidentally fire spurious callbacks.
     * The tracker dispatches a synthetic `keyboardheightchange` CustomEvent on the
     * textarea element so it goes through the same code path as mini-program.
     *
     * This is a no-op on platforms where `MiniTextAreaElement` already feeds real
     * `keyboardheightchange` events (it does not expose `window.visualViewport`).
     * Kept in strict parity with KRTextFieldView.bindKeyboardHeightTrackingIfNeeded.
     */
    private fun bindKeyboardHeightTrackingIfNeeded() {
        if (keyboardTrackingBound) return
        val vv = js("(typeof window !== 'undefined' && window.visualViewport) ? window.visualViewport : null")
        if (vv == null) return
        keyboardTrackingBound = true

        var isFocused = false
        ele.addEventListener("focus", { isFocused = true })
        ele.addEventListener("blur", {
            isFocused = false
            // Treat blur as keyboard fully collapsed.
            if (lastKeyboardHeight != 0f) {
                lastKeyboardHeight = 0f
                dispatchKeyboardHeightChangeEvent(0f, DEFAULT_KEYBOARD_DURATION, DEFAULT_KEYBOARD_CURVE)
            }
        })

        val onResize: (dynamic) -> Unit = {
            if (isFocused) {
                val innerHeight = js("window.innerHeight").unsafeCast<Number>().toFloat()
                val viewportHeight = vv.height.unsafeCast<Number>().toFloat()
                val height = (innerHeight - viewportHeight).coerceAtLeast(0f)
                if (height != lastKeyboardHeight) {
                    lastKeyboardHeight = height
                    dispatchKeyboardHeightChangeEvent(
                        height,
                        DEFAULT_KEYBOARD_DURATION,
                        DEFAULT_KEYBOARD_CURVE
                    )
                }
            }
        }
        vv.addEventListener("resize", onResize)
    }

    /**
     * Dispatch a unified `keyboardheightchange` CustomEvent on this textarea element so the
     * listener installed in `setProp(KEYBOARD_HEIGHT_CHANGE, ...)` can handle it uniformly
     * on both H5 and mini-program.
     */
    private fun dispatchKeyboardHeightChangeEvent(height: Float, duration: Float, curve: Int) {
        val detail: dynamic = js("({})")
        detail.height = height
        detail.duration = duration
        detail.curve = curve
        val event = js("new CustomEvent('keyboardheightchange', { detail: detail })")
        ele.asDynamic().dispatchEvent(event)
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
        private const val SELECTION_COLOR = "selectionColor"
        private const val MAX_TEXT_LENGTH = "maxTextLength"
        private const val AUTO_FOCUS = "autofocus"
        private const val EDIT_ABLE = "editable"
        private const val KEYBOARD_TYPE = "keyboardType"
        private const val RETURN_KEY_TYPE = "returnKeyType"
        // Keyboard height change event name (aligns with core's TextAreaView.KEYBOARD_HEIGHT_CHANGE)
        private const val KEYBOARD_HEIGHT_CHANGE = "keyboardHeightChange"

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

        // Unified DOM event name used by both mini-program (real) and H5 (synthesized
        // from visualViewport.resize) to deliver keyboard height change signals.
        private const val EVENT_KEYBOARD_HEIGHT_CHANGE = "keyboardheightchange"

        // Fallback animation values for H5 where VisualViewport does not provide
        // keyboard animation timing info; match typical iOS keyboard animation.
        private const val DEFAULT_KEYBOARD_DURATION = 0.25f
        private const val DEFAULT_KEYBOARD_CURVE = 0
    }
}
