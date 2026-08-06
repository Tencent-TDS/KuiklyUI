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
import kotlin.js.JSON

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

    // Raw text/selection/composition state change callback
    private var textInputStateChangedEventCallback: KuiklyRenderCallback? = null

    // Cursor/selection-only change callback
    private var selectionChangedEventCallback: KuiklyRenderCallback? = null

    // Keyboard height change callback (iOS/Android native parity)
    private var keyboardHeightChangeCallback: KuiklyRenderCallback? = null

    // Whether textarea selection listeners have been bound.
    private var selectionTrackingBound = false

    // Last emitted selection range, used to de-dup selection-only callbacks.
    private var lastSelectionStart = -1
    private var lastSelectionEnd = -1

    // Suppress selection callback when selection is changed programmatically.
    private var suppressSelectionChange = false

    // Selection tracking listener references for deterministic unbinding on destroy.
    private var onSelectionRelatedEventListener: ((dynamic) -> Unit)? = null
    private var onDocumentSelectionChangeListener: ((dynamic) -> Unit)? = null

    // Whether a VisualViewport-based keyboard listener has been bound (H5 only).
    private var keyboardTrackingBound = false
    // Last reported keyboard height, used to de-dup resize events.
    private var lastKeyboardHeight: Float = 0f
    // VisualViewport + listener references for deterministic unbinding on destroy.
    private var keyboardViewport: dynamic = null
    private var keyboardTrackingFocused = false
    private var onKeyboardFocusListener: ((dynamic) -> Unit)? = null
    private var onKeyboardBlurListener: ((dynamic) -> Unit)? = null
    private var onKeyboardViewportResizeListener: ((dynamic) -> Unit)? = null

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
                val maxTextLength = propValue.unsafeCast<Int>()
                if (maxTextLength <= 0) {
                    // Treat negative values as unlimited input length on web.
                    ele.removeAttribute("maxlength")
                } else {
                    ele.maxLength = maxTextLength
                }
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
                    map[MAP_KEY_TEXT] = ele.value
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
                    map[MAP_KEY_TEXT] = ele.value
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
                        map[MAP_KEY_TEXT] = ele.value
                        // Return key clicked
                        clickReturnEventCallback?.invoke(map)
                    }
                })
                true
            }

            TEXT_INPUT_STATE_CHANGE -> {
                textInputStateChangedEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                bindSelectionTrackingIfNeeded()
                true
            }

            SELECTION_CHANGE -> {
                selectionChangedEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                bindSelectionTrackingIfNeeded()
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
                    map[MAP_KEY_HEIGHT] = height
                    map[MAP_KEY_DURATION] = duration
                    map[MAP_KEY_CURVE] = curve
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
                        map[MAP_KEY_TEXT] = ele.value
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
                        map[MAP_KEY_TEXT] = ele.value
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
                        MAP_KEY_CURSOR_INDEX to ele.selectionStart
                    ))
                }
            }

            SET_CURSOR_INDEX -> {
                val index = params?.toIntOrNull() ?: return null
                // set input cursor index, focus first
                ele.focus()
                updateSelection(index, index)
            }

            SET_TEXT_INPUT_STATE -> {
                val stateText = params ?: return null
                applyTextInputState(stateText)
            }

            GET_TEXT_INPUT_STATE -> {
                KuiklyRenderCoreContextScheduler.scheduleTask {
                    callback?.invoke(buildCurrentTextInputStateMap(includeLength = true))
                }
            }

            else -> super.call(method, params, callback)
        }
    }

    /**
     * Text content changed, notify kuikly side
     */
    private fun notifyTextValueChanged(text: String) {
        val map = mutableMapOf<String, Any>()
        map[MAP_KEY_TEXT] = text
        // Notify kotlin side
        textDidChangedEventCallback?.invoke(map)
        // Keep textInputStateChange in parity with native renderers.
        textInputStateChangedEventCallback?.invoke(buildCurrentTextInputStateMap(includeLength = true))
    }

    /**
     * Bind selection listeners once and emit selection/textInputState changes in a unified payload.
     *
     * Besides element-level events, we also listen to document-level `selectionchange`.
     * On some mobile browsers, long-press "Select All" may skip `select`/`touchend`
     * on the target textarea while still dispatching `selectionchange` on document.
     */
    private fun bindSelectionTrackingIfNeeded() {
        if (selectionTrackingBound) return
        selectionTrackingBound = true

        onSelectionRelatedEventListener = selectionHandler@{
            if (suppressSelectionChange) return@selectionHandler
            notifySelectionChangedIfNeeded()
        }

        onDocumentSelectionChangeListener = selectionHandler@{
            if (suppressSelectionChange) return@selectionHandler
            val activeElement = kuiklyDocument.asDynamic().activeElement
            if (activeElement != ele) return@selectionHandler
            notifySelectionChangedIfNeeded()
        }

        onSelectionRelatedEventListener?.let {
            ele.addEventListener(EVENT_SELECT, it)
            ele.addEventListener(EVENT_KEYUP, it)
            ele.addEventListener(EVENT_MOUSEUP, it)
            ele.addEventListener(EVENT_TOUCHEND, it)
        }
        onDocumentSelectionChangeListener?.let {
            kuiklyDocument.addEventListener(EVENT_DOCUMENT_SELECTION_CHANGE, it)
        }
    }

    /**
     * Emit selectionChange when cursor/selection actually changed.
     */
    private fun notifySelectionChangedIfNeeded(force: Boolean = false) {
        val start = ele.selectionStart ?: ele.value.length
        val end = ele.selectionEnd ?: start
        if (!force && start == lastSelectionStart && end == lastSelectionEnd) {
            return
        }
        lastSelectionStart = start
        lastSelectionEnd = end
        selectionChangedEventCallback?.invoke(buildCurrentTextInputStateMap(includeLength = true))
    }

    /**
     * Build unified text input state payload.
     */
    private fun buildCurrentTextInputStateMap(includeLength: Boolean): Map<String, Any> {
        val text = ele.value
        val selectionStart = ele.selectionStart ?: text.length
        val selectionEnd = ele.selectionEnd ?: selectionStart
        val map = mutableMapOf<String, Any>(
            MAP_KEY_TEXT to text,
            MAP_KEY_SELECTION_START to selectionStart,
            MAP_KEY_SELECTION_END to selectionEnd,
            MAP_KEY_COMPOSITION_START to NO_COMPOSITION,
            MAP_KEY_COMPOSITION_END to NO_COMPOSITION,
        )
        if (includeLength) {
            map[MAP_KEY_LENGTH] = text.length
        }
        return map
    }

    /**
     * Update selection range and optionally suppress selection callback for programmatic changes.
     */
    private fun updateSelection(start: Int, end: Int, suppressSelectionEvent: Boolean = true) {
        val textLength = ele.value.length
        val safeStart = start.coerceIn(0, textLength)
        val safeEnd = end.coerceIn(0, textLength)
        if (suppressSelectionEvent) {
            suppressSelectionChange = true
        }
        ele.setSelectionRange(safeStart, safeEnd)
        if (suppressSelectionEvent) {
            suppressSelectionChange = false
            lastSelectionStart = safeStart
            lastSelectionEnd = safeEnd
        }
    }

    /**
     * Apply textInputState JSON payload from core layer.
     */
    private fun applyTextInputState(stateText: String) {
        val state = try {
            JSON.parse<dynamic>(stateText)
        } catch (_: Throwable) {
            return
        }

        val requestedText = (state?.text as? String) ?: ""
        ele.value = requestedText

        val requestedSelectionStart = (state?.selectionStart as? Number)?.toInt() ?: requestedText.length
        val requestedSelectionEnd = (state?.selectionEnd as? Number)?.toInt() ?: requestedSelectionStart
        updateSelection(requestedSelectionStart, requestedSelectionEnd, suppressSelectionEvent = true)

        notifyTextValueChanged(ele.value)
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
        keyboardViewport = vv

        onKeyboardFocusListener = { keyboardTrackingFocused = true }
        onKeyboardBlurListener = {
            keyboardTrackingFocused = false
            // Treat blur as keyboard fully collapsed.
            if (lastKeyboardHeight != 0f) {
                lastKeyboardHeight = 0f
                dispatchKeyboardHeightChangeEvent(0f, DEFAULT_KEYBOARD_DURATION, DEFAULT_KEYBOARD_CURVE)
            }
        }

        onKeyboardViewportResizeListener = {
            if (keyboardTrackingFocused) {
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

        onKeyboardFocusListener?.let { ele.addEventListener("focus", it) }
        onKeyboardBlurListener?.let { ele.addEventListener("blur", it) }
        onKeyboardViewportResizeListener?.let { vv.addEventListener("resize", it) }
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

    private fun unbindSelectionTrackingIfNeeded() {
        if (!selectionTrackingBound) return

        onSelectionRelatedEventListener?.let {
            ele.removeEventListener(EVENT_SELECT, it)
            ele.removeEventListener(EVENT_KEYUP, it)
            ele.removeEventListener(EVENT_MOUSEUP, it)
            ele.removeEventListener(EVENT_TOUCHEND, it)
        }
        onDocumentSelectionChangeListener?.let {
            kuiklyDocument.removeEventListener(EVENT_DOCUMENT_SELECTION_CHANGE, it)
        }

        onSelectionRelatedEventListener = null
        onDocumentSelectionChangeListener = null
        selectionTrackingBound = false
    }

    private fun unbindKeyboardHeightTrackingIfNeeded() {
        if (!keyboardTrackingBound) return

        onKeyboardFocusListener?.let { ele.removeEventListener("focus", it) }
        onKeyboardBlurListener?.let { ele.removeEventListener("blur", it) }
        onKeyboardViewportResizeListener?.let { listener ->
            keyboardViewport?.removeEventListener("resize", listener)
        }

        onKeyboardFocusListener = null
        onKeyboardBlurListener = null
        onKeyboardViewportResizeListener = null
        keyboardViewport = null
        keyboardTrackingFocused = false
        keyboardTrackingBound = false
    }

    override fun onDestroy() {
        unbindSelectionTrackingIfNeeded()
        unbindKeyboardHeightTrackingIfNeeded()
        super.onDestroy()
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
        private const val SET_TEXT_INPUT_STATE = "setTextInputState"
        private const val GET_TEXT_INPUT_STATE = "getTextInputState"

        // Events
        private const val TEXT_DID_CHANGE = "textDidChange"
        private const val TEXT_INPUT_STATE_CHANGE = "textInputStateChange"
        private const val SELECTION_CHANGE = "selectionChange"
        private const val INPUT_FOCUS = "inputFocus"
        private const val INPUT_BLUR = "inputBlur"
        private const val INPUT_RETURN = "inputReturn"
        private const val TEXT_LENGTH_BEYOND_LIMIT = "textLengthBeyondLimit"

        // Unified DOM event name used by both mini-program (real) and H5 (synthesized
        // from visualViewport.resize) to deliver keyboard height change signals.
        private const val EVENT_KEYBOARD_HEIGHT_CHANGE = "keyboardheightchange"
        private const val EVENT_SELECT = "select"
        private const val EVENT_KEYUP = "keyup"
        private const val EVENT_MOUSEUP = "mouseup"
        private const val EVENT_TOUCHEND = "touchend"
        private const val EVENT_DOCUMENT_SELECTION_CHANGE = "selectionchange"

        // Fallback animation values for H5 where VisualViewport does not provide
        // keyboard animation timing info; match typical iOS keyboard animation.
        private const val DEFAULT_KEYBOARD_DURATION = 0.25f
        private const val DEFAULT_KEYBOARD_CURVE = 0

        // TextInputState default composition sentinel.
        private const val NO_COMPOSITION = -1

        // Payload keys
        private const val MAP_KEY_TEXT = "text"
        private const val MAP_KEY_CURSOR_INDEX = "cursorIndex"
        private const val MAP_KEY_SELECTION_START = "selectionStart"
        private const val MAP_KEY_SELECTION_END = "selectionEnd"
        private const val MAP_KEY_COMPOSITION_START = "compositionStart"
        private const val MAP_KEY_COMPOSITION_END = "compositionEnd"
        private const val MAP_KEY_LENGTH = "length"
        private const val MAP_KEY_HEIGHT = "height"
        private const val MAP_KEY_DURATION = "duration"
        private const val MAP_KEY_CURVE = "curve"
    }
}
