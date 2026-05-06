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

    /**
     * Bridge the shared
     * [com.tencent.kuikly.core.render.web.expand.components.KRTextFieldView] `placeholderColor`
     * prop onto WX native `<input>`'s `placeholder-style` attribute.
     *
     * H5 realizes placeholder color through a runtime-injected `::placeholder` pseudo-class
     * stylesheet (see [com.tencent.kuikly.core.render.web.ktx.setPlaceholderColor]), which does
     * NOT propagate into the WX native `<input>`. WX exposes `placeholder-style` instead —
     * camelCase `placeholderStyle` here, rewritten to kebab-case `p13` by Transform.kt.
     */
    @JsName("placeholderColor")
    var placeholderColor: String = ""
        set(value) {
            this.setAttribute("placeholderStyle", "color:$value;")
            field = value
        }

    @JsName("maxLength")
    var maxLength: Int = -1
        set(value) {
            field = value
            // When a `beforeinput` listener has been installed, we need the WX native <input>
            // to report the keystroke that WOULD overflow so our shim can synthesize a
            // `beforeinput` event with `preventDefault`. WX natively hard-truncates at
            // `maxlength` and does NOT fire `bindinput` for the rejected character, so we
            // inflate the attribute by 1 and perform truncation ourselves inside the
            // `bindinput` bridge below. If nobody is listening to `beforeinput`, we keep the
            // original behavior (WX silently truncates, no callback) to avoid surprising
            // existing callers.
            val nativeMaxLength = if (beforeInputListenerBound && value > 0) value + 1 else value
            this.setAttribute("maxLength", nativeMaxLength)
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
     * Intercept the standard HTML `enterKeyHint` attribute set by the shared
     * [com.tencent.kuikly.core.render.web.expand.components.KRTextFieldView] on browsers, and map
     * it onto the WeChat mini-program native `<input>` `confirm-type` attribute, so that the
     * common `Input` component's `returnKeyType*()` DSL also works on the mini-program platform.
     *
     * Mapping: search / send / go / done / next are kept as-is (they are all valid
     * `confirm-type` values for WX input); any unknown value falls back to `done`.
     */
    @JsName("enterKeyHint")
    var enterKeyHint: String = ""
        set(value) {
            val confirmType = when (value) {
                CONFIRM_SEND, CONFIRM_SEARCH, CONFIRM_GO, CONFIRM_DONE, CONFIRM_NEXT -> value
                else -> CONFIRM_DONE
            }
            this.setAttribute(CONFIRM_TYPE, confirmType)
            field = value
        }

    /**
     * Empty string may be set here, need to call setAttributeForce to force setting
     */
    @JsName("value")
    var value: String = ""
        set(value) {
            field = value
            // Keep the shadow value (used by the `beforeinput` bridge for diffing) in sync
            // whenever someone assigns to `value` programmatically, e.g. the core layer
            // calls `ele.value = "..."` in `setProp(SRC)` / `call(SET_TEXT)`.
            shadowValue = value
            setAttributeForce(VALUE, value)
        }

    /**
     * Latest value observed by the `beforeinput` bridge; used to diff against the next
     * `bindinput` detail.value so we can classify the edit as insert / delete and derive
     * the inserted chunk without relying on the native `InputEvent` which WX does not
     * expose.
     */
    private var shadowValue: String = ""

    /**
     * Whether [addEventListener] has been called with `beforeinput`. Controls whether
     * [maxLength] inflates the native attribute by 1 to allow overflow detection.
     */
    private var beforeInputListenerBound: Boolean = false

    init {
        // Initialize some default styles, otherwise it will display abnormally
        this.style.fontSize = defaultFontSize.toPxF()
        this.setAttribute("value", value)
        this.setAttribute("placeholder", placeholder)
        this.setAttribute("maxLength", maxLength)
        this.setAttribute("disabled", readOnly)
        this.setAttribute("type", type)
        // Marker consumed by the shared KRTextFieldView to decide whether to take the WX
        // `placeholder-style` path vs. the H5 `::placeholder` CSS-injection path. Using a
        // custom marker (instead of sniffing the `placeholderColor` setter via `in`) keeps
        // the detection decoupled from Kotlin/JS codegen details of `@JsName` accessors.
        this.asDynamic().__krSupportsPlaceholderColor = true
        // Install a resident cursor-sync tap on `focus` / `blur` / `confirm` so that
        // manual caret moves (tapping into the middle of the text, long-press-drag caret
        // on iOS, etc.) propagate into `cachedCursorIndex`. WX native <input> does NOT
        // emit `bindselectionchange`, but every focus/blur/confirm event detail carries
        // the latest `cursor` offset. Registering here — before any upper-layer
        // `addEventListener("focus", ...)` call — ensures our shadow update runs first
        // and user handlers see a fresh `selectionStart` when they read it. The handlers
        // are registered via MiniElement.super so they don't route back into the
        // EVENT_KEYDOWN / input bridges below.
        installCursorSyncTap()
    }

    /**
     * Subscribe low-priority cursor-sync handlers to WX native `focus` / `blur` /
     * `confirm` events. These are append-style registrations (MiniEvent handler list
     * is a simple array, appending never displaces existing handlers), so they
     * coexist with any user-level listener the shared KRTextFieldView attaches.
     */
    private fun installCursorSyncTap() {
        val cursorSync: EventHandler = { event ->
            // `event` is already `dynamic` (EventHandler = (dynamic) -> Unit), so direct
            // property access is a raw JS member read — no method-call generation. Do NOT
            // route through `asDynamic()` here: for `dynamic` receivers the Kotlin/JS
            // backend emits a literal `.asDynamic()` method call in some codegen paths,
            // and the underlying WX mpEvent has no such method, producing
            // `i.asDynamic is not a function` at runtime.
            val rawDetail: dynamic = event.detail
            val rawCursor: dynamic =
                if (rawDetail != null) rawDetail.cursor else js("undefined")
            if (jsTypeOf(rawCursor) == "number") {
                cachedCursorIndex = rawCursor.unsafeCast<Int>()
            }
        }
        // Call super directly to bypass this class's override (which would re-enter the
        // KEYDOWN -> confirm adaptation for "keydown"). All three raw WX events are
        // already statically bound on <input> by the mini-program template.
        super.addEventListener(EVENT_FOCUS, cursorSync, null)
        super.addEventListener(EVENT_BLUR, cursorSync, null)
        super.addEventListener(EVENT_CONFIRM, cursorSync, null)
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
     * Latest cursor offset observed from the WX native `<input>`. Kept in sync from
     * three sources:
     * 1. `bindinput.detail.cursor` — post-edit caret after each keystroke / paste.
     * 2. `bindfocus.detail.cursor` / `bindblur.detail.cursor` / `bindconfirm.detail.cursor`
     *    — any focus-boundary carries the latest caret, which covers the common
     *    "user tapped into the middle of the text" interaction (WX fires blur+focus
     *    on a re-tap, both with the new `cursor`).
     * 3. Explicit programmatic write via [setSelectionRange].
     *
     * Platform limitation: WX does NOT expose a `bindselectionchange` (or equivalent)
     * for `<input>`, so we cannot observe a caret move that happens *within* a single
     * focus session without any text being inserted/deleted. In that rare case a
     * subsequent read of [selectionStart] returns the last synced value, not the
     * live caret — this matches the baseline behavior of every mini-program text
     * input wrapper shipped by Taro / wx-adapter / uni-app.
     *
     * A value of -1 means "never observed"; [selectionStart] then falls back to the
     * end of [value], matching typical append-at-end typing.
     */
    private var cachedCursorIndex: Int = -1

    /**
     * DOM-compatible `selectionStart` getter used by the shared
     * [com.tencent.kuikly.core.render.web.expand.components.KRTextFieldView] in its
     * `GET_CURSOR_INDEX` branch:
     *
     * ```kotlin
     * callback?.invoke(mapOf(MAP_KEY_CURSOR_INDEX to ele.selectionStart))
     * ```
     *
     * `@JsName("selectionStart")` guarantees the symbol name is preserved by the
     * Kotlin/JS compiler so the upper layer's `ele.selectionStart` resolves here at
     * runtime even though the static type is `HTMLInputElement`.
     */
    @JsName("selectionStart")
    val selectionStart: Int
        get() = if (cachedCursorIndex >= 0) cachedCursorIndex else value.length

    /**
     * DOM-compatible `setSelectionRange(start, end)` used by [KRTextFieldView] in its
     * `SET_CURSOR_INDEX` branch. WX native `<input>` exposes two attributes
     * `selection-start` / `selection-end` that only take effect when both are set and
     * the element is focused. We write both plus `focus=true` to mimic the DOM
     * behavior; `KRTextFieldView` already calls `ele.focus()` right before, but we
     * re-assert it here so this method is safe to call standalone too.
     *
     * Camel-case attribute names are used on purpose — the mini-program `Transform`
     * layer maps `selectionStart` → `p22` / `selectionEnd` → `p21` (see
     * `Transform.kt`) which is ultimately what the WX runtime reads. WX silently
     * ignores the write when `start < 0`, so we clamp to 0.
     */
    @JsName("setSelectionRange")
    fun setSelectionRange(start: Int, end: Int) {
        val safeStart = if (start < 0) 0 else start
        val safeEnd = if (end < safeStart) safeStart else end
        cachedCursorIndex = safeStart
        setAttribute("selectionStart", safeStart)
        setAttribute("selectionEnd", safeEnd)
        // WX contract: selection-start/end only apply while the field is focused.
        setAttribute("focus", true)
    }

    /**
     * Add event listener.
     *
     * Two pieces of adaptation are done here so the shared web-layer component
     * [com.tencent.kuikly.core.render.web.expand.components.KRTextFieldView] can work untouched
     * on WeChat mini-program:
     *
     * 1. `input` event: sync back `event.target.value` to [value] so later reads on this element
     *    always get the latest text.
     * 2. `keydown` event (Enter key): WX native `<input>` does NOT emit DOM `keydown` events, but
     *    it emits a `confirm` event when the user taps the soft-keyboard return key. We forward
     *    the `confirm` event and synthesize a minimal KeyboardEvent-like object
     *    ({ key: "Enter", keyCode: 13, target: { value } }) so that
     *    [KRTextFieldView]'s `INPUT_RETURN` branch (which listens on `keydown` and checks
     *    `event.key === "Enter"` || `event.keyCode == 13`) triggers correctly on mini-program.
     */
    override fun addEventListener(type: String, callback: EventHandler, options: dynamic) {
        when (type) {
            EVENT_INPUT -> {
                val inputCallback: EventHandler = { event ->
                    if (jsTypeOf(event.target.value) != "undefined") {
                        // input event return value
                        value = event.target.value.unsafeCast<String>()
                    }
                    // Sync cursor shadow from WX `bindinput` detail.cursor. WX sets this to
                    // the new caret offset (post-edit); fall back to value length when the
                    // field is absent on some adapter runtimes so our GET_CURSOR_INDEX path
                    // still returns a sensible value.
                    // NOTE: `event` is already `dynamic`; do NOT call `.asDynamic()` on it.
                    // On a `dynamic` receiver Kotlin/JS emits a literal `.asDynamic()`
                    // method invocation in some codegen paths, which blows up at runtime on
                    // the WX mpEvent object with `i.asDynamic is not a function`.
                    val rawDetail: dynamic = event.detail
                    val rawCursor: dynamic =
                        if (rawDetail != null) rawDetail.cursor else js("undefined")
                    cachedCursorIndex = if (jsTypeOf(rawCursor) == "number") {
                        rawCursor.unsafeCast<Int>()
                    } else {
                        value.length
                    }
                    callback(event)
                }
                super.addEventListener(type, inputCallback, options)
            }

            EVENT_BEFORE_INPUT -> {
                // WX native <input> does not emit a DOM `beforeinput` event. We simulate it by
                // subscribing to `bindinput` (WX's "post-input" event) and diffing the detail
                // against `shadowValue`. For each edit we synthesize an InputEvent-like
                // object compatible with what `KRTextFieldView` reads:
                //     { inputType, data, isComposing:false, preventDefault(), target:{value} }
                //
                // Overflow semantics: WX `<input maxlength=N>` hard-truncates, so if the
                // caller subscribed to beforeinput we've already inflated the native attr by 1
                // (see `maxLength` setter). When the user types the (N+1)-th character WX
                // still fires `bindinput` with a value of length N+1. We detect that, invoke
                // the upper callback (which will call `preventDefault()`), and then
                // RETURN the truncated string from the `bindinput` callback — WX has a
                // documented contract that a non-undefined return value replaces the
                // current input value, giving us an equivalent of `preventDefault`.
                beforeInputListenerBound = true
                // Re-apply maxLength so the native attribute picks up the inflated value
                // if it was set before the listener bound.
                if (maxLength > 0) {
                    this.setAttribute("maxLength", maxLength + 1)
                }
                val realMaxLength = { maxLength }
                val bridge: EventHandler = { event ->
                    // `event` is dynamic; direct field access avoids spurious `.asDynamic()`
                    // method-call generation (see EVENT_INPUT note above).
                    val rawDetail: dynamic = event.detail
                    val newValue: String = if (
                        rawDetail != null && jsTypeOf(rawDetail.value) == "string"
                    ) rawDetail.value.unsafeCast<String>() else ""
                    val oldValue = shadowValue
                    val newLen = newValue.length
                    val oldLen = oldValue.length
                    val isInsert = newLen > oldLen
                    val inserted: String = if (isInsert) {
                        // Derive the inserted chunk via a simple common-prefix / common-suffix
                        // diff. Works for typical single-char insertions, pastes, and IME
                        // composition commits emitted by WX as a single bindinput event.
                        var prefix = 0
                        val minLen = if (oldLen < newLen) oldLen else newLen
                        while (prefix < minLen && oldValue[prefix] == newValue[prefix]) prefix++
                        var suffix = 0
                        while (
                            suffix < (minLen - prefix) &&
                            oldValue[oldLen - 1 - suffix] == newValue[newLen - 1 - suffix]
                        ) suffix++
                        newValue.substring(prefix, newLen - suffix)
                    } else ""
                    val inputType: String = when {
                        isInsert -> INPUT_TYPE_INSERT_TEXT
                        newLen < oldLen -> INPUT_TYPE_DELETE_BACKWARD
                        else -> INPUT_TYPE_INSERT_TEXT
                    }

                    // Synthesize the InputEvent-like object.
                    val limit = realMaxLength()
                    val overflow = limit > 0 && newLen > limit
                    var prevented = false
                    val syntheticEvent: dynamic = js("({})")
                    syntheticEvent.type = EVENT_BEFORE_INPUT
                    syntheticEvent.inputType = inputType
                    syntheticEvent.data = inserted
                    syntheticEvent.isComposing = false
                    syntheticEvent.preventDefault = { prevented = true; Unit }
                    val syntheticTarget: dynamic = js("({})")
                    // KRTextFieldView reads `ele.value` directly (not event.target.value) in
                    // its beforeinput branch, so target.value is mostly cosmetic; still, we
                    // expose both the pre-edit and post-edit values so future consumers can
                    // use either.
                    syntheticTarget.value = if (overflow) oldValue else newValue
                    syntheticEvent.target = syntheticTarget
                    syntheticEvent.originalEvent = event

                    // Before dispatching upstream, keep `ele.value` consistent with what the
                    // user WILL see after this edit, so that KRTextFieldView's
                    //     `val newLength = ele.value.length + inserted.length`
                    // evaluates to the expected overflow total. We temporarily set value to
                    // the pre-edit string so the formula sums to `newLen` when `inserted` is
                    // derived from the diff.
                    val persistedShadow = shadowValue
                    // Do NOT update shadowValue here; the post-edit sync happens after we
                    // decide whether to accept or truncate.

                    // Expose pre-edit value for the duration of the upstream callback so
                    // that `ele.value.length + inserted.length == newLen`. We use
                    // `setAttributeForce(VALUE, oldValue)` to avoid perturbing field storage.
                    setAttributeForce(VALUE, oldValue)
                    callback(syntheticEvent)

                    if (overflow) {
                        // WX already accepted the (N+1)-th character on screen; we MUST
                        // enforce the real limit here regardless of whether upstream
                        // called preventDefault. Truncate and push the corrected value
                        // back to the native <input> via setAttributeForce. Note: WX does
                        // support "return a string from bindinput to replace value", but
                        // our `EventHandler` signature is `(dynamic) -> Unit`, so the
                        // return value can't be plumbed through Taro anyway. Explicit
                        // attribute write is both sufficient and portable.
                        val truncated = newValue.substring(0, limit)
                        shadowValue = truncated
                        setAttributeForce(VALUE, truncated)
                        // Caret after truncation lives at the end of the accepted string.
                        cachedCursorIndex = truncated.length
                        // Silence "unused" warnings for diagnostic locals captured above.
                        val unusedPrevented = prevented
                        val unusedTruncated = truncated
                    } else {
                        // Accept the edit. Restore post-edit value and update shadow.
                        shadowValue = newValue
                        setAttributeForce(VALUE, newValue)
                        // Prefer WX-reported cursor when present; fall back to new value
                        // length, which matches append-at-end typing — the common case.
                        val rawCursor: dynamic =
                            if (rawDetail != null) rawDetail.cursor else js("undefined")
                        cachedCursorIndex = if (jsTypeOf(rawCursor) == "number") {
                            rawCursor.unsafeCast<Int>()
                        } else {
                            newLen
                        }
                    }
                    val unusedPersistedShadow = persistedShadow
                    Unit
                }
                super.addEventListener(EVENT_INPUT, bridge, options)
            }

            EVENT_KEYDOWN -> {
                // Map DOM `keydown` listener onto WX <input> `confirm` event.
                val confirmCallback: EventHandler = { event ->
                    // Sync latest text value from confirm event to this element.
                    // `event` is dynamic; direct field access avoids spurious `.asDynamic()`
                    // method-call generation on the WX mpEvent.
                    val detailValue: dynamic = event.detail?.value
                    if (jsTypeOf(detailValue) != "undefined" && detailValue != null) {
                        value = detailValue.unsafeCast<String>()
                    }
                    // Synthesize a KeyboardEvent-like object that can satisfy the
                    // `event.key === "Enter" || event.keyCode == 13` check on the caller side.
                    val syntheticEvent: dynamic = js("({})")
                    syntheticEvent.key = KEY_ENTER
                    syntheticEvent.keyCode = ENTER_KEY_CODE
                    syntheticEvent.which = ENTER_KEY_CODE
                    syntheticEvent.type = EVENT_KEYDOWN
                    syntheticEvent.target = event.target ?: this
                    syntheticEvent.originalEvent = event
                    callback(syntheticEvent)
                }
                super.addEventListener(EVENT_CONFIRM, confirmCallback, options)
            }

            EVENT_KEYBOARD_HEIGHT_CHANGE -> {
                // WX native <input> emits `bindkeyboardheightchange` whose detail carries
                // { height: Number(px), duration: Number(sec) }. Reshape it into a DOM
                // CustomEvent-like payload `{ detail: { height, duration, curve } }` so that
                // the shared KRTextFieldView listener can consume mini-program and H5 events
                // in a single code path. WX does not expose an animation curve, so we report
                // 0 (linear) which keeps the core-side KeyboardParams well-formed.
                //
                // Platform note (WX real devices): unlike Android/iOS native, a single focus
                // on real WX devices typically fires `bindkeyboardheightchange` 2–3 times —
                // commonly a synchronous snapshot with `duration=0` at the final height,
                // followed by one or more transitional frames (e.g. an intermediate height
                // with `duration>0`) before the last frame reports the final height again.
                // This is WX runtime behaviour and differs by device / IME. We intentionally
                // forward every frame as-is (no debounce / coalescing) so the mini-program
                // callback has the same "fires-per-native-event" semantics as every other
                // WX event this class forwards; any smoothing is left to upper layers (the
                // Kuikly business code can filter by `duration` / last-seen `height`).
                val heightChangeCallback: EventHandler = { event ->
                    // `event` is dynamic; direct field access avoids spurious `.asDynamic()`
                    // method-call generation on the WX mpEvent.
                    val rawDetail: dynamic = event.detail
                    val normalizedDetail: dynamic = js("({})")
                    normalizedDetail.height =
                        if (rawDetail != null && jsTypeOf(rawDetail.height) != "undefined") rawDetail.height else 0
                    normalizedDetail.duration =
                        if (rawDetail != null && jsTypeOf(rawDetail.duration) != "undefined") rawDetail.duration else 0
                    normalizedDetail.curve = 0
                    val syntheticEvent: dynamic = js("({})")
                    syntheticEvent.type = EVENT_KEYBOARD_HEIGHT_CHANGE
                    syntheticEvent.detail = normalizedDetail
                    syntheticEvent.target = event.target ?: this
                    syntheticEvent.originalEvent = event
                    callback(syntheticEvent)
                }
                super.addEventListener(type, heightChangeCallback, options)
            }

            else -> super.addEventListener(type, callback, options)
        }
    }

    companion object {
        private const val VALUE = "value"
        private const val PASSWORD = "password"

        // Key of the WX native <input> attribute controlling the soft-keyboard confirm button.
        // NOTE: keep camelCase here, mini-program Transform layer will map it to `confirm-type`.
        private const val CONFIRM_TYPE = "confirmType"

        // Valid confirm-type enum values accepted by WX <input>.
        private const val CONFIRM_SEND = "send"
        private const val CONFIRM_SEARCH = "search"
        private const val CONFIRM_GO = "go"
        private const val CONFIRM_DONE = "done"
        private const val CONFIRM_NEXT = "next"

        // Event names.
        private const val EVENT_INPUT = "input"
        // DOM `beforeinput` — not supported natively by WX <input>; synthesized from
        // `bindinput` via a diff against the previous value. Used by KRTextFieldView's
        // `textLengthBeyondLimit` branch.
        private const val EVENT_BEFORE_INPUT = "beforeinput"
        // DOM `keydown` — not supported natively by WX <input>; adapted via `confirm`.
        private const val EVENT_KEYDOWN = "keydown"
        // WX native <input> return-key event.
        private const val EVENT_CONFIRM = "confirm"
        // WX native <input> focus / blur events. We subscribe to them internally to keep
        // `cachedCursorIndex` in sync with manual caret moves (their detail carries the
        // latest `cursor` offset). Do NOT intercept these for any other purpose — user
        // handlers registered via addEventListener("focus"/"blur", ...) must still
        // propagate through `super.addEventListener` unchanged.
        private const val EVENT_FOCUS = "focus"
        private const val EVENT_BLUR = "blur"
        // Unified DOM-level keyboard-height-change event that both H5 (dispatched by
        // KRTextFieldView via VisualViewport) and mini-program (forwarded from
        // WX native `bindkeyboardheightchange`) agree on.
        private const val EVENT_KEYBOARD_HEIGHT_CHANGE = "keyboardheightchange"

        // Enter key identifiers used when synthesizing a KeyboardEvent-like object.
        private const val KEY_ENTER = "Enter"
        private const val ENTER_KEY_CODE = 13

        // InputEvent.inputType values consumed by KRTextFieldView's beforeinput handler.
        // Kept as string literals (instead of reusing KRInputTypeConst) so this file has no
        // extra dependency; values must match `KRInputTypeConst.INSERT_TEXT` /
        // `KRInputTypeConst.DELETE_BACKWARD` in the base module.
        private const val INPUT_TYPE_INSERT_TEXT = "insertText"
        private const val INPUT_TYPE_DELETE_BACKWARD = "deleteContentBackward"
    }
}
