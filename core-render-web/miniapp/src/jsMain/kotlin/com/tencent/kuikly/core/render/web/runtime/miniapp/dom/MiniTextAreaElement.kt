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

    /**
     * Bridge the shared
     * [com.tencent.kuikly.core.render.web.expand.components.KRTextAreaView] `placeholderColor`
     * prop onto WX native `<textarea>`'s `placeholder-style` attribute.
     *
     * H5 sets placeholder color via a runtime-injected `::placeholder` pseudo-class stylesheet
     * (see [com.tencent.kuikly.core.render.web.ktx.setPlaceholderColor]), which has no effect
     * on the WX native `<textarea>` rendered off-DOM. WX instead honors `placeholder-style`
     * (camelCase attribute mapped by Transform.kt → `p15`), so we wrap the rgb color into a
     * single-declaration CSS snippet.
     *
     * Note: assigning to `setAttribute("placeholderStyle", value)` — keep camelCase here, the
     * Transform layer rewrites it to the final kebab-case attribute on the WX side.
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
            // Mirror MiniInputElement.maxLength: when a `beforeinput` listener has been
            // installed we need WX native <textarea> to actually report the keystroke that
            // would overflow the length limit, so we inflate the native attribute by 1 and
            // perform the real truncation ourselves inside the `bindinput` bridge below.
            // Without a beforeinput listener we keep the original behavior (WX silently
            // hard-truncates at `maxlength`, no callback) to avoid surprising existing callers.
            val nativeMaxLength = if (beforeInputListenerBound && value > 0) value + 1 else value
            this.setAttribute("maxLength", nativeMaxLength)
        }

    /**
     * Latest value observed by the `beforeinput` bridge; used to diff against the next
     * `bindinput` detail.value so we can classify the edit as insert / delete and derive
     * the inserted chunk without relying on the native `InputEvent` which WX does not
     * expose on <textarea>.
     */
    private var shadowValue: String = ""

    /**
     * Whether [addEventListener] has been called with `beforeinput`. Controls whether
     * [maxLength] inflates the native attribute by 1 to allow overflow detection.
     */
    private var beforeInputListenerBound: Boolean = false

    init {
        this.setAttribute("maxLength", maxLength)
        // Marker consumed by the shared KRTextAreaView to decide whether to take the WX
        // `placeholder-style` path vs. the H5 `::placeholder` CSS-injection path. Using a
        // custom marker (instead of sniffing the `placeholderColor` setter via `in`) keeps
        // the detection decoupled from Kotlin/JS codegen details of `@JsName` accessors.
        this.asDynamic().__krSupportsPlaceholderColor = true
        // Install a resident cursor-sync tap on `focus` / `blur` / `confirm` so that
        // manual caret moves (tapping into the middle of the text, long-press-drag caret
        // on iOS, etc.) propagate into `cachedCursorIndex`. WX native <textarea> does NOT
        // emit `bindselectionchange`, but every focus/blur/confirm event detail carries
        // the latest `cursor` offset. Registering here — before any upper-layer
        // `addEventListener("focus", ...)` call — ensures our shadow update runs first
        // and user handlers see a fresh `selectionStart` when they read it. Kept in
        // strict parity with MiniInputElement.installCursorSyncTap.
        installCursorSyncTap()
    }

    /**
     * Subscribe low-priority cursor-sync handlers to WX native `focus` / `blur` /
     * `confirm` events. These are append-style registrations (MiniEvent handler list
     * is a simple array, appending never displaces existing handlers), so they
     * coexist with any user-level listener the shared KRTextAreaView attaches.
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
        // Call super directly to bypass this class's override. All three raw WX events
        // are already statically bound on <textarea> by the mini-program template.
        super.addEventListener(EVENT_FOCUS, cursorSync, null)
        super.addEventListener(EVENT_BLUR, cursorSync, null)
        super.addEventListener(EVENT_CONFIRM, cursorSync, null)
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

    /**
     * Intercept the standard HTML `enterKeyHint` attribute set by the shared
     * [com.tencent.kuikly.core.render.web.expand.components.KRTextAreaView] on browsers, and map
     * it onto the WeChat mini-program native `<textarea>` `confirm-type` attribute, so that the
     * common `TextArea` component's `returnKeyType*()` DSL also works on the mini-program platform.
     *
     * Mapping: search / send / go / done / next are kept as-is (they are all valid
     * `confirm-type` values for WX textarea); any unknown value falls back to `done`.
     *
     * Kept in strict parity with
     * [com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniInputElement.enterKeyHint] so the
     * two form controls behave identically w.r.t. `returnKeyType`.
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

    @JsName("value")
    var value: String = ""
        set(value) {
            this.setAttribute("value", value)
            field = value
            // Keep the shadow value (used by the `beforeinput` bridge for diffing) in sync
            // whenever someone assigns to `value` programmatically, e.g. the core layer
            // calls `ele.value = "..."` in `setProp(SRC)` / `call(SET_TEXT)`.
            shadowValue = value
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
     * Latest cursor offset. See [MiniInputElement.cachedCursorIndex] for the full
     * rationale — WX native `<textarea>` shares the same event contract as `<input>`
     * (`bindinput.detail.cursor` / `bindfocus` / `bindblur` / `bindconfirm`), so we
     * mirror the exact same shadow-value strategy here and keep it in sync from:
     * 1. `bindinput` post-edit caret.
     * 2. `bindfocus` / `bindblur` / `bindconfirm` focus-boundary caret — covers
     *    "user tapped into the middle of the text" on re-tap (WX fires blur+focus
     *    on re-tap, both with the new `cursor`).
     * 3. Explicit programmatic write via [setSelectionRange].
     *
     * Platform limitation: WX does NOT expose a `bindselectionchange` for
     * `<textarea>`, so a caret move *within* a single focus session without any
     * text being inserted/deleted cannot be observed. Subsequent reads of
     * [selectionStart] return the last synced value — same as MiniInputElement.
     *
     * A value of -1 means "never observed"; [selectionStart] then falls back to
     * the end of [value], matching typical append-at-end typing.
     */
    private var cachedCursorIndex: Int = -1

    /**
     * DOM-compatible `selectionStart` getter consumed by
     * [com.tencent.kuikly.core.render.web.expand.components.KRTextAreaView]'s
     * `GET_CURSOR_INDEX` branch (`ele.selectionStart`). `@JsName` preserves the symbol
     * name across Kotlin/JS so the upper layer's static `HTMLTextAreaElement` typing
     * still resolves here at runtime.
     */
    @JsName("selectionStart")
    val selectionStart: Int
        get() = if (cachedCursorIndex >= 0) cachedCursorIndex else value.length

    /**
     * DOM-compatible `setSelectionRange(start, end)` consumed by
     * [com.tencent.kuikly.core.render.web.expand.components.KRTextAreaView]'s
     * `SET_CURSOR_INDEX` branch. Writes `selection-start` / `selection-end` on the
     * WX native `<textarea>`; these take effect only while focused, so we also
     * set `focus=true` (the upper layer's `ele.focus()` already does, but this
     * keeps the method self-contained).
     */
    @JsName("setSelectionRange")
    fun setSelectionRange(start: Int, end: Int) {
        val safeStart = if (start < 0) 0 else start
        val safeEnd = if (end < safeStart) safeStart else end
        cachedCursorIndex = safeStart
        setAttribute("selectionStart", safeStart)
        setAttribute("selectionEnd", safeEnd)
        setAttribute("focus", true)
    }

    /**
     * Add event listener.
     *
     * Mini-program adaptation so the shared web-layer
     * [com.tencent.kuikly.core.render.web.expand.components.KRTextAreaView] works untouched
     * on WeChat mini-program:
     *
     * 1. `input` event: sync back `event.target.value` to [value] and keep [cachedCursorIndex]
     *    in sync with WX's post-edit `detail.cursor`.
     * 2. `keyboardheightchange` event: reshape WX native `bindkeyboardheightchange`
     *    detail `{height, duration}` into a DOM CustomEvent-like payload
     *    `{ detail: { height, duration, curve } }` so `KRTextAreaView`'s listener
     *    can consume mini-program and H5 events in a single code path. Kept in
     *    strict parity with
     *    [com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniInputElement.addEventListener].
     */
    override fun addEventListener(type: String, callback: EventHandler, options: dynamic) {
        when (type) {
            EVENT_INPUT -> {
                val inputCallback: EventHandler = { event ->
                    if (jsTypeOf(event.target.value) != "undefined") {
                        // input event return value
                        value = event.target.value.unsafeCast<String>()
                    }
                    // Keep cursor shadow in sync with the post-edit caret reported by WX.
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
                // WX native <textarea> does not emit a DOM `beforeinput` event. We simulate it
                // by subscribing to `bindinput` (WX's "post-input" event) and diffing the
                // detail against `shadowValue`. For each edit we synthesize an InputEvent-like
                // object compatible with what `KRTextAreaView` reads:
                //     { inputType, data, isComposing:false, preventDefault(), target:{value} }
                //
                // Overflow semantics: WX `<textarea maxlength=N>` hard-truncates, so if the
                // caller subscribed to beforeinput we've already inflated the native attr by 1
                // (see `maxLength` setter). When the user types the (N+1)-th character WX
                // still fires `bindinput` with a value of length N+1. We detect that, invoke
                // the upper callback (which will call `preventDefault()`), and then RETURN
                // the truncated string from the `bindinput` callback — WX has a documented
                // contract that a non-undefined return value replaces the current input
                // value, giving us an equivalent of `preventDefault`.
                //
                // Kept in strict parity with
                // [com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniInputElement]'s
                // EVENT_BEFORE_INPUT branch so Input / TextArea behave identically w.r.t.
                // `textLengthBeyondLimit` on mini-program.
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
                    // KRTextAreaView reads `ele.value` directly (not event.target.value) in
                    // its beforeinput branch, so target.value is mostly cosmetic; still, we
                    // expose both the pre-edit and post-edit values so future consumers can
                    // use either.
                    syntheticTarget.value = if (overflow) oldValue else newValue
                    syntheticEvent.target = syntheticTarget
                    syntheticEvent.originalEvent = event

                    // Before dispatching upstream, keep `ele.value` consistent with what the
                    // user WILL see after this edit, so that KRTextAreaView's
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
                        // back to the native <textarea> via setAttributeForce. Note: WX
                        // does support "return a string from bindinput to replace value",
                        // but our `EventHandler` signature is `(dynamic) -> Unit`, so the
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

            EVENT_KEYBOARD_HEIGHT_CHANGE -> {
                // WX native <textarea> emits `bindkeyboardheightchange` whose detail carries
                // { height: Number(px), duration: Number(sec) }. Reshape it into a DOM
                // CustomEvent-like payload `{ detail: { height, duration, curve } }` so that
                // the shared KRTextAreaView listener can consume mini-program and H5 events
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
        // Key of the WX native <textarea> attribute controlling the soft-keyboard confirm button.
        // NOTE: keep camelCase here, mini-program Transform layer will map it to `confirm-type`.
        private const val CONFIRM_TYPE = "confirmType"

        // Valid confirm-type enum values accepted by WX <textarea>. Kept identical to
        // MiniInputElement's set so `Input` / `TextArea` share the exact same `returnKeyType`
        // mapping semantics on the mini-program platform.
        private const val CONFIRM_SEND = "send"
        private const val CONFIRM_SEARCH = "search"
        private const val CONFIRM_GO = "go"
        private const val CONFIRM_DONE = "done"
        private const val CONFIRM_NEXT = "next"

        // WX native <textarea> `value` attribute name; used with `setAttributeForce` in the
        // `beforeinput` bridge where we need to write empty strings to reset the value.
        private const val VALUE = "value"

        // Event names.
        private const val EVENT_INPUT = "input"
        // DOM `beforeinput` — not supported natively by WX <textarea>; synthesized from
        // `bindinput` via a diff against the previous value. Used by KRTextAreaView's
        // `textLengthBeyondLimit` branch.
        private const val EVENT_BEFORE_INPUT = "beforeinput"
        // Unified DOM-level keyboard-height-change event that both H5 (dispatched by
        // KRTextAreaView via VisualViewport) and mini-program (forwarded from
        // WX native `bindkeyboardheightchange`) agree on.
        private const val EVENT_KEYBOARD_HEIGHT_CHANGE = "keyboardheightchange"
        // WX native <textarea> focus / blur / confirm events. We subscribe to them
        // internally to keep `cachedCursorIndex` in sync with manual caret moves (their
        // detail carries the latest `cursor` offset). Do NOT intercept for any other
        // purpose — user handlers registered via addEventListener must still propagate
        // through `super.addEventListener` unchanged.
        private const val EVENT_FOCUS = "focus"
        private const val EVENT_BLUR = "blur"
        private const val EVENT_CONFIRM = "confirm"

        // InputEvent.inputType values consumed by KRTextAreaView's beforeinput handler.
        // Kept as string literals (instead of reusing KRInputTypeConst) so this file has no
        // extra dependency; values must match `KRInputTypeConst.INSERT_TEXT` /
        // `KRInputTypeConst.DELETE_BACKWARD` in the base module.
        private const val INPUT_TYPE_INSERT_TEXT = "insertText"
        private const val INPUT_TYPE_DELETE_BACKWARD = "deleteContentBackward"
    }
}
