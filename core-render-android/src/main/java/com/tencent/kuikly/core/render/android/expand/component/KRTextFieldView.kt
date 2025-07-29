/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.render.android.expand.component

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.util.SizeF
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.adapter.TextPostProcessorInput
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.const.KRViewConst
import com.tencent.kuikly.core.render.android.css.ktx.drawCommonDecoration
import com.tencent.kuikly.core.render.android.css.ktx.drawCommonForegroundDecoration
import com.tencent.kuikly.core.render.android.css.ktx.spToPxI
import com.tencent.kuikly.core.render.android.css.ktx.toColor
import com.tencent.kuikly.core.render.android.css.ktx.toDpF
import com.tencent.kuikly.core.render.android.css.ktx.toPxF
import com.tencent.kuikly.core.render.android.css.ktx.toPxI
import com.tencent.kuikly.core.render.android.expand.component.input.KRTextLengthLimitInputFilter
import com.tencent.kuikly.core.render.android.expand.component.list.KRRecyclerView
import com.tencent.kuikly.core.render.android.expand.component.text.FontWeightSpan
import com.tencent.kuikly.core.render.android.expand.component.text.HRLineHeightSpan
import com.tencent.kuikly.core.render.android.expand.component.text.KRRichTextBuilder
import com.tencent.kuikly.core.render.android.expand.module.KRKeyboardModule
import com.tencent.kuikly.core.render.android.expand.module.KeyboardStatusListener
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback

/**
 * KTV单行输入组件
 */
open class KRTextFieldView(context: Context, private val softInputMode: Int?) : EditText(context),
    IKuiklyRenderViewExport {

    /**
     * text变化回调
     */
    private var textDidChangeCallback: KuiklyRenderCallback? = null

    /**
     * 聚焦回调
     */
    private var inputFocusCallback: KuiklyRenderCallback? = null

    /**
     * 失焦回调
     */
    private var inputBlurCallback: KuiklyRenderCallback? = null

    /**
     * 输入字体超过限制回调
     */
    private var textLengthBeyondLimitCallback: KuiklyRenderCallback? = null

    /**
     * 键盘高度变化回调
     */
    private var keyboardHeightChangeCallback: KuiklyRenderCallback? = null

    /**
     * 键盘状态变化监听
     */
    private var keyboardStatusListener: KeyboardStatusListener? = null

    /**
     * 点击回车回调
     */
    private var inputReturnCallBack: KuiklyRenderCallback? = null

    /**
     * 键盘IME动作回调
     */
    private var imeActionCallBack: KuiklyRenderCallback? = null

    private var fontSize = -1f
    private var lineHeight = -1f
    private var lineHeightSpan: HRLineHeightSpan? = null
    private var textWatcher: TextWatcher? = null
    /**
     * 是否使用dp来作为字体单位
     */
    var useDpFontSizeDim = false

    /**
     * 当前光标位置
     */
    private var cursorIndex = 0

    private var hadSetEditorFactory = false
    private var textProps: KRTextProps? = null

    /**
     * 键盘显示需要 window 和 view 两者同时处于 focus才能显示
     * 如果这两者没处于 focus 时，收到显示键盘的请求, lazy 住，等两者都 focus 时，才显示键盘
     */
    private var pendingFocus = false

    private var currentKeyboardHeight = 0
    private var maxTextLength: Int? = null

    init {
        resetDefaultStyle()
        enableFocusInTouchMode()
        initSingleLine()
    }

    protected open fun initSingleLine() {
        isSingleLine = true
    }

    /**
     * 设置HRTextFieldView的Prop
     *
     * <p>这里为啥不用使用map<key, handler>来处理?
     *
     * <p>1.属性不会太多, 使用when语句的可读性比map<key，handler>的方式好
     *
     * <p>2.一般只有维护者一人编写
     *
     * <p>3.降低内存开销
     *
     * <p>这里的value类型是与kuiklyCore侧约定好的，因此没判断就使用强转
     *
     * @param propKey 属性key
     * @param propValue 属性值
     * @return 是否处理
     */
    @Suppress("UNCHECKED_CAST")
    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            TEXT -> setText(propValue)
            KRTextProps.PROP_KEY_VALUES -> setValues(propValue)
            FONT_SIZE -> setFontSize(propValue)
            FONT_WEIGHT -> setFontWeight(propValue)
            COLOR -> setColor(propValue)
            TINT_COLOR -> setTintColor(propValue)
            PLACE_HOLDER_COLOR -> setPlaceHolderColor(propValue)
            PLACE_HOLDER -> setPlaceHolder(propValue)
            KEYBOARD_TYPE -> setKeyboardType(propValue)
            RETURN_KEY_TYPE -> setReturnKeyType(propValue)
            TEXT_ALIGN -> setTextAlign(propValue)
            MAX_TEXT_LENGTH -> setMaxTextLength(propValue)
            EDITABLE -> setEditable(propValue)
            TEXT_DID_CHANGE -> observeTextChanged(propValue)
            INPUT_RETURN -> observeInputReturn(propValue)
            IME_ACTION -> observeIMEAction(propValue)
            INPUT_FOCUS -> {
                inputFocusCallback = propValue as KuiklyRenderCallback
                observeFocusChanged()
            }
            INPUT_BLUR -> {
                inputBlurCallback = propValue as KuiklyRenderCallback
                observeFocusChanged()
            }
            TEXT_LENGTH_BEYOND_LIMIT -> observeTextLengthBeyondLimit(propValue)
            KEYBOARD_HEIGHT_CHANGE -> observeKeyboardHeightChange(propValue)
            KRTextProps.PROP_KEY_TEXT_USE_DP_FONT_SIZE_DIM -> setUseDpFontSizeDim(propValue)
            KRTextProps.PROP_KEY_NUMBER_OF_LINES -> setNumberLines(propValue)
            IME_NO_FULLSCREEN -> setImeNoFullscreen(propValue)
            KRTextProps.PROP_KEY_LINE_HEIGHT -> setLineHeight(propValue)
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun setNumberLines(propValue: Any): Boolean {
        maxLines = propValue as Int
        if (maxLines == 1) {
            isSingleLine = true
        } else {
            isSingleLine = false
        }
        return true
    }

    /**
     * 处理HRTextFieldView的方法调用
     *
     * <p>这里为啥不用使用map<key, handler>来处理?
     *
     * <p>1.方法列表不会太多, 使用when语句的可读性比map<key，handler>的方式好
     *
     * <p>2.一般只有维护者一人编写
     *
     * <p>3.降低内存开销
     *
     * <p>这里的value类型是与kuiklyCore侧约定好的，因此没判断就使用强转
     *
     * @param method 方法名字
     * @param params 参数
     * @param callback 回调
     * @return 如果是同步调用的话，为同步调用结果的返回值
     */
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_SET_TEXT -> setInputText(params)
            METHOD_FOCUS -> setFocus()
            METHOD_BLUR -> setBlur()
            METHOD_GET_CURSOR_INDEX -> getCursorIndex(callback)
            METHOD_SET_CURSOR_INDEX -> setCursorIndex(params)
            else -> super.call(method, params, callback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        keyboardStatusListener?.let { listener ->
            kuiklyRenderContext?.module<KRKeyboardModule>(KRKeyboardModule.MODULE_NAME)?.removeListener(listener)
        }
        keyboardStatusListener = null
    }

    override fun onDraw(canvas: Canvas) {
        val scrollX = scrollX.toFloat()
        val scrollY = scrollY.toFloat()
        val needTrans = needTranslate(scrollX, scrollY)
        if (needTrans) {
            canvas.translate(scrollX, scrollY)
        }
        drawCommonDecoration(canvas)
        if (needTrans) {
            canvas.translate(-scrollX, -scrollY)
        }
        super.onDraw(canvas)
        drawCommonForegroundDecoration(canvas)
    }

    private fun needTranslate(scrollX: Float, scrollY: Float): Boolean {
        return scrollX != 0f || scrollY != 0f
    }

    private fun setInputText(params: String?) {
        val text = params ?: KRCssConst.EMPTY_STRING
        super.setText(text)
        setSelection(text.length)
    }

    private fun setEditable(value: Any): Boolean {
        val editable = value as Int == TYPE_ENABLE_EDIT
        isFocusable = editable
        isFocusableInTouchMode = editable
        return true
    }

    private fun setMaxTextLength(value: Any): Boolean {
        maxTextLength = value as Int
        filters = arrayOf<InputFilter>(
            KRTextLengthLimitInputFilter(
                value,
                kuiklyRenderContext,
                { fontSize },
                { textLengthBeyondLimitCallback?.invoke(null) }
            )
        )
        return true
    }

    private fun setTextAlign(value: Any): Boolean {
        when (value as String) {
            "left" -> {
                gravity = (gravity and Gravity.HORIZONTAL_GRAVITY_MASK.inv()) or Gravity.LEFT
            }
            "center" -> {
                gravity = (gravity and Gravity.HORIZONTAL_GRAVITY_MASK.inv()) or Gravity.CENTER_HORIZONTAL
            }
            "right" -> {
                gravity = (gravity and Gravity.HORIZONTAL_GRAVITY_MASK.inv()) or Gravity.RIGHT
            }
        }
        return true
    }

    private fun setReturnKeyType(value: Any): Boolean {
        val returnKeyType = when (value as String) {
            "search" -> {
                EditorInfo.IME_ACTION_SEARCH
            }
            "send" -> {
                EditorInfo.IME_ACTION_SEND
            }
            "done" -> {
                EditorInfo.IME_ACTION_DONE
            }
            "next" -> {
                EditorInfo.IME_ACTION_NEXT
            }
            "go" -> {
                EditorInfo.IME_ACTION_GO
            }
            else -> {
                EditorInfo.IME_NULL
            }
        }
        val currentImeOptions = imeOptions
        imeOptions = (currentImeOptions and EditorInfo.IME_MASK_ACTION.inv()) or returnKeyType
        immRestartInput()
        return true
    }

    private fun setKeyboardType(propValue: Any): Boolean {
        inputType = when (propValue as String) {
            "password" -> {
                InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            }
            "number" -> {
                InputType.TYPE_CLASS_NUMBER
            }
            "email" -> {
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            else -> {
                InputType.TYPE_CLASS_TEXT
            }
        }
        return true
    }

    private fun setImeNoFullscreen(propValue: Any): Boolean {
        val currentImeOptions = imeOptions
        imeOptions = if (propValue as Boolean) {
            currentImeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
        } else {
            currentImeOptions and EditorInfo.IME_FLAG_NO_FULLSCREEN.inv()
        }
        immRestartInput()
        return true
    }

    private fun setPlaceHolder(propValue: Any): Boolean {
        hint = propValue as String
        return true
    }

    private fun setPlaceHolderColor(propValue: Any): Boolean {
        setHintTextColor((propValue as String).toColor())
        return true
    }

    private fun setColor(propValue: Any): Boolean {
        setTextColor((propValue as String).toColor())
        return true
    }

    private fun setCursorDrawableColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textCursorDrawable = ShapeDrawable().apply {
                paint.color = color
                intrinsicWidth = kuiklyRenderContext.toPxI(2f)
            }
        } else {
            try {
                val fCursorDrawableRes = TextView::class.java.getDeclaredField("mCursorDrawableRes")
                fCursorDrawableRes.isAccessible = true
                val mCursorDrawableRes = fCursorDrawableRes.getInt(this)
                val fEditor = TextView::class.java.getDeclaredField("mEditor")
                fEditor.isAccessible = true
                val editor = fEditor.get(this)
                val clazz: Class<*> = editor.javaClass
                val fCursorDrawable = clazz.getDeclaredField("mCursorDrawable")
                fCursorDrawable.isAccessible = true

                val drawables: Array<Drawable?> = arrayOfNulls(2)
                val cursorDrawable = resources.getDrawable(mCursorDrawableRes)
                cursorDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                drawables[0] = cursorDrawable
                drawables[1] = cursorDrawable
                fCursorDrawable.set(editor, drawables)
            } catch (e: Exception) {
                // 由于不清楚系统mCursorDrawable底层会抛出哪种类型的异常，因此这里使用顶层异常来处理
                // 并且异常不影响主路径
                KuiklyRenderLog.e(KRRecyclerView.VIEW_NAME, "set mCursorDrawable error, $e")
            }
        }
    }

    override fun setSelection(index: Int) {
        val maxTextLength = this.maxTextLength
        if (maxTextLength != null && maxTextLength <= index) {
            super.setSelection(maxTextLength)
        } else {
            super.setSelection(index)
        }
    }

    private fun setTintColor(propValue: Any): Boolean {
        KuiklyRenderAdapterManager.krColorParseAdapter?.toColor(propValue as String)?.also {
            setCursorDrawableColor(it)
        }
        return true
    }

    private fun setText(propValue: Any): Boolean {
        setInputText((propValue as? String)?: "")
        return true
    }

    private fun setValues(propValue: Any): Boolean {
        val textProps = KRTextProps(kuiklyRenderContext).also {
            it.fontSize = -1f
        }
        this.textProps = textProps
        textProps.setProp(KRTextProps.PROP_KEY_VALUES, propValue)
        val richTextBuilder = KRRichTextBuilder(kuiklyRenderContext)
        val text = richTextBuilder.build(textProps, mutableListOf()) {
            SizeF(0f, 0f)
        }
        setInputEditorAdapterIfNeed()
        val selStart = selectionStart
        text?.also(::ensureLineHeightSpan)
        super.setText(text)
        setSelection(selStart)
        return true
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        cursorIndex = selStart
    }

    private fun setFontSize(propValue: Any): Boolean {
        val rawFontSize = (propValue as Number).toFloat()
        fontSize = rawFontSize
        setTextSize(TypedValue.COMPLEX_UNIT_PX, if (useDpFontSizeDim) {
            kuiklyRenderContext.toPxF(rawFontSize)
        } else {
            kuiklyRenderContext.spToPxI(rawFontSize).toFloat()
        })
        return true
    }

    private fun setFontWeight(propValue: Any): Boolean {
        val fontWeightSpan = FontWeightSpan(propValue as String)
        fontWeightSpan.updateDrawState(paint)
        return true
    }

    private fun setFocus() {
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        post {
            if (hasWindowFocus() && hasFocus()) {
                showKeyboard()
            } else {
                pendingFocus = true
            }
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (pendingFocus && hasWindowFocus && hasFocus()) {
            showKeyboard()
            pendingFocus = false
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (pendingFocus && focused && hasWindowFocus()) {
            showKeyboard()
            pendingFocus = false
        }
    }

    private fun showKeyboard() {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun setBlur() {
        clearFocus()
        post {
            val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    private fun getCursorIndex(callback: KuiklyRenderCallback?) {
        callback?.invoke(mapOf(
            "cursorIndex" to cursorIndex
        ))
    }

    private fun setCursorIndex(params: String?) {
        val index = params?.toIntOrNull() ?: return
        setSelection(index)
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeTextChanged(propValue: Any): Boolean {
        textDidChangeCallback = propValue as KuiklyRenderCallback
        observeTextWatcher()
        return true
    }

    private fun observeInputReturn(propValue: Any): Boolean {
        inputReturnCallBack = propValue as KuiklyRenderCallback
        setOnEditorActionListener { _, _, _ ->
            inputReturnCallBack?.invoke(createCallbackParamMap())
            true
        }
        return true
    }

    private fun observeIMEAction(propValue: Any): Boolean {
        imeActionCallBack = propValue as KuiklyRenderCallback
        setOnEditorActionListener { _, actionId, _ ->
            val imeAction = when (actionId) {
                EditorInfo.IME_ACTION_GO -> "go"
                EditorInfo.IME_ACTION_SEARCH -> "search"
                EditorInfo.IME_ACTION_SEND -> "send"
                EditorInfo.IME_ACTION_NEXT -> "next"
                EditorInfo.IME_ACTION_DONE -> "done"
                EditorInfo.IME_ACTION_PREVIOUS -> "previous"
                else -> ""
            }

            imeActionCallBack?.invoke(mapOf(
                "ime_action" to imeAction
            ))
            true
        }
        return true
    }

    private fun observeFocusChanged(): Boolean {
        if (onFocusChangeListener != null) {
            return true
        }

        setOnFocusChangeListener { _, focus ->
            if (focus) {
                inputFocusCallback?.invoke(createCallbackParamMap())
            } else {
                inputBlurCallback?.invoke(createCallbackParamMap())
            }
        }
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeTextLengthBeyondLimit(propValue: Any): Boolean {
        textLengthBeyondLimitCallback = propValue as KuiklyRenderCallback
        return true
    }

    private fun observeKeyboardHeightChange(propValue: Any): Boolean {
        if (keyboardHeightChangeCallback != null) { // 已经设置过了, 直接返回
            return true
        }

        // 修改 Activity softInputMode
        softInputMode?.let {
            val window = activity?.window ?: return false
            window.setSoftInputMode(softInputMode)
        }

        @Suppress("UNCHECKED_CAST")
        keyboardHeightChangeCallback = propValue as KuiklyRenderCallback
        // 键盘状态监听
        keyboardStatusListener = object : KeyboardStatusListener {
            override fun onHeightChanged(keyboardHeight: Int) {
                if (keyboardHeight == currentKeyboardHeight) {
                    return
                }
                currentKeyboardHeight = keyboardHeight
                keyboardHeightChangeCallback?.invoke(
                    mapOf(
                        KRViewConst.HEIGHT to kuiklyRenderContext.toDpF(keyboardHeight.toFloat()),
                        KEY_KEYBOARD_CHANGED_DURATION to DEFAULT_KEYBOARD_CHANGED_ANIMATION_DURATION
                    )
                )
            }
        }
        keyboardStatusListener?.let { listener ->
            kuiklyRenderContext?.module<KRKeyboardModule>(KRKeyboardModule.MODULE_NAME)?.addListener(listener)
        }
        return true
    }

    private fun createCallbackParamMap(): Map<String, Any> {
        return mapOf(
            "text" to text.toString()
        )
    }

    private fun resetDefaultStyle() {
        setPadding(0, 0, 0, 0)
        background = null
        gravity = Gravity.LEFT or Gravity.CENTER
    }

    private fun enableFocusInTouchMode() {
        isFocusableInTouchMode = true
        isFocusable = true
    }

    private fun setUseDpFontSizeDim(propValue: Any): Boolean {
        val useDp = (propValue as Int) == 1
        val change = useDpFontSizeDim != useDp
        useDpFontSizeDim = useDp
        if (change) {
            if (fontSize != -1f) {
                setFontSize(fontSize)
            }
            if (lineHeight != -1f) {
                setLineHeight(lineHeight)
            }
        }
        return true
    }

    private fun setInputEditorAdapterIfNeed() {
        val textPostProcessorAdapter = KuiklyRenderAdapterManager.krTextPostProcessorAdapter ?: return
        if (hadSetEditorFactory) {
            return
        }
        hadSetEditorFactory = true
        setEditableFactory(object : Editable.Factory() {
            override fun newEditable(source: CharSequence?): Editable {
                if (source == null) {
                    return SpannableStringBuilder()
                }
                val tp = textProps ?: return SpannableStringBuilder()
                val outputText = textPostProcessorAdapter.onTextPostProcess(kuiklyRenderContext, TextPostProcessorInput("input",
                    source, tp)).text
                return if (outputText is Editable) {
                    outputText
                } else {
                    SpannableStringBuilder()
                }
            }
        })
    }

    private fun immRestartInput() {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive(this)) {
            imm.restartInput(this)
        }
    }

    private fun setLineHeight(propValue: Any): Boolean {
        val rawLineHeight = (propValue as Number).toFloat()
        lineHeight = rawLineHeight
        val pxLineHeight = if (useDpFontSizeDim) {
            kuiklyRenderContext.toPxI(rawLineHeight)
        } else {
            kuiklyRenderContext.spToPxI(rawLineHeight)
        }
        lineHeightSpan?.also {
            if (it.height == pxLineHeight) {
                return true // not changed
            }
            text.removeSpan(it)
        }
        lineHeightSpan = HRLineHeightSpan(pxLineHeight)
        ensureLineHeightSpan(text)
        observeTextWatcher()
        return true
    }

    private fun ensureLineHeightSpan(text: Spannable) {
        val span = lineHeightSpan ?: return
        text.apply {
            if (getSpanStart(span) != 0 || getSpanEnd(span) != length) {
                // range changed, call setSpan to update
                setSpan(span, 0, length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            }
        }
    }

    private fun observeTextWatcher() {
        if (textWatcher == null) {
            textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    s?.also(::ensureLineHeightSpan)
                    textDidChangeCallback?.invoke(createCallbackParamMap())
                }

                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                }
            }.also {
                addTextChangedListener(it)
            }
        }
    }

    companion object {
        const val VIEW_NAME = "KRTextFieldView"
        private const val TEXT = "text"
        private const val FONT_SIZE = "fontSize"
        private const val FONT_WEIGHT = "fontWeight"
        private const val TINT_COLOR = "tintColor"
        private const val COLOR = "color"
        private const val PLACE_HOLDER_COLOR = "placeholderColor"
        private const val PLACE_HOLDER = "placeholder"
        private const val KEYBOARD_TYPE = "keyboardType"
        private const val RETURN_KEY_TYPE = "returnKeyType"
        private const val TEXT_ALIGN = "textAlign"
        private const val MAX_TEXT_LENGTH = "maxTextLength"
        private const val EDITABLE = "editable"
        private const val TEXT_DID_CHANGE = "textDidChange"
        private const val INPUT_RETURN = "inputReturn"
        private const val INPUT_FOCUS = "inputFocus"
        private const val INPUT_BLUR = "inputBlur"
        private const val IME_ACTION = "imeAction"
        private const val TEXT_LENGTH_BEYOND_LIMIT = "textLengthBeyondLimit"
        private const val KEYBOARD_HEIGHT_CHANGE = "keyboardHeightChange"
        private const val IME_NO_FULLSCREEN = "imeNoFullscreen"

        private const val METHOD_SET_TEXT = "setText"
        private const val METHOD_FOCUS = "focus"
        private const val METHOD_BLUR = "blur"
        private const val METHOD_GET_CURSOR_INDEX = "getCursorIndex"
        private const val METHOD_SET_CURSOR_INDEX = "setCursorIndex"

        private const val TYPE_ENABLE_EDIT = 1

        private const val KEY_KEYBOARD_CHANGED_DURATION = "duration"
        private const val DEFAULT_KEYBOARD_CHANGED_ANIMATION_DURATION = 0.2
    }
}

private class TextLengthLimitInputFilter(
    private val maxLength: Int,
    private val textLengthBeyondLimitCallback: () -> Unit
) :
    InputFilter {
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        var keep: Int = maxLength - (Character.codePointCount(dest, 0, dest.length) - (dend - dstart))
        return when {
            keep <= 0 -> {
                textLengthBeyondLimitCallback.invoke()
                KRCssConst.EMPTY_STRING
            }
            keep >= Character.codePointCount(source ?: "", start, end) -> {
                null // keep original
            }
            else -> {
                keep += start
                if (Character.isHighSurrogate(source!![keep - 1])) {
                    --keep
                    if (keep == start) {
                        return KRCssConst.EMPTY_STRING
                    }
                }
                source.subSequence(start, keep)
            }
        }
    }
}