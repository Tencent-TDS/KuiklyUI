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
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ImageSpan
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
import com.tencent.kuikly.core.render.android.expand.component.input.KRBaseLengthFilter
import com.tencent.kuikly.core.render.android.expand.component.input.KRByteLengthFilter
import com.tencent.kuikly.core.render.android.expand.component.input.KRCharacterLengthFilter
import com.tencent.kuikly.core.render.android.expand.component.input.KRTextLengthLimitInputFilter
import com.tencent.kuikly.core.render.android.expand.component.input.KRVisualWidthLengthFilter
import com.tencent.kuikly.core.render.android.expand.component.list.KRRecyclerView
import com.tencent.kuikly.core.render.android.expand.component.text.FontWeightSpan
import com.tencent.kuikly.core.render.android.expand.component.text.HRLineHeightSpan
import com.tencent.kuikly.core.render.android.expand.component.text.KRRichTextBuilder
import com.tencent.kuikly.core.render.android.expand.module.KRKeyboardModule
import com.tencent.kuikly.core.render.android.expand.module.KeyboardStatusListener
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject

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
     * Raw text input state callback.
     */
    private var textInputStateChangeCallback: KuiklyRenderCallback? = null

    /**
     * Selection-only change callback.
     */
    private var selectionChangeCallback: KuiklyRenderCallback? = null

    private var isSettingTextInputState = false

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
    private var textPostProcessor: String = ""

    /**
     * 键盘显示需要 window 和 view 两者同时处于 focus才能显示
     * 如果这两者没处于 focus 时，收到显示键盘的请求, lazy 住，等两者都 focus 时，才显示键盘
     */
    private var pendingFocus = false

    private var currentKeyboardHeight = 0
    private var lengthLimitType: Int = -1
    private var maxTextLength: Int? = null

    /**
     * 是否在点击 IME 动作按钮时自动收起键盘
     * 默认值为 false，即不自动收起，由业务自己控制
     */
    private var autoHideKeyboardOnImeAction: Boolean = false

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
            PROP_KEY_TEXT_POST_PROCESSOR -> {
                textPostProcessor = propValue as String
                setInputEditorAdapterIfNeed()
                // Apply emoji processing to existing text (handles case where text was set before textPostProcessor)
                applyEmojiSpans(editableText)
                true
            }
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
            LENGTH_LIMIT_TYPE -> setLengthLimitType(propValue)
            MAX_TEXT_LENGTH -> setMaxTextLength(propValue)
            EDITABLE -> setEditable(propValue)
            TEXT_DID_CHANGE -> observeTextChanged(propValue)
            TEXT_INPUT_STATE -> {
                setTextInputState(propValue.toString())
                true
            }
            TEXT_INPUT_STATE_CHANGE -> observeTextInputStateChanged(propValue)
            SELECTION_CHANGE -> observeSelectionChanged(propValue)
            INPUT_RETURN -> observeInputReturn(propValue)
            IME_ACTION -> observeInputReturn(propValue)
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
            AUTO_HIDE_KEYBOARD_ON_IME_ACTION -> {
                autoHideKeyboardOnImeAction = (propValue as Int == TYPE_ENABLE_HIDE_KEYBOARD)
                true
            }
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
            METHOD_SET_TEXT_INPUT_STATE -> setTextInputState(params)
            METHOD_GET_TEXT_INPUT_STATE -> getTextInputState(callback)
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
        val checkpoint: Int = if (hasCustomClipPath()) {
            canvas.save()
        } else {
            -1
        }
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
        if (checkpoint != -1) {
            canvas.restoreToCount(checkpoint)
        }
        drawCommonForegroundDecoration(canvas)
    }

    private fun needTranslate(scrollX: Float, scrollY: Float): Boolean {
        return scrollX != 0f || scrollY != 0f
    }

    private fun setInputText(params: String?) {
        val text = params ?: KRCssConst.EMPTY_STRING
        lineHeightSpan?.let { span ->
            val spannable = SpannableString(text)
            if (text.isNotEmpty()) {
                spannable.setSpan(span, 0, text.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            }
            super.setText(spannable, BufferType.EDITABLE)
        } ?: super.setText(text, BufferType.EDITABLE)
        setSelection(getText()?.length ?: 0)
    }

    private fun setEditable(value: Any): Boolean {
        val editable = value as Int == TYPE_ENABLE_EDIT
        isFocusable = editable
        isFocusableInTouchMode = editable
        return true
    }

    private fun setLengthLimitType(value: Any): Boolean {
        if (lengthLimitType != value) {
            lengthLimitType = value as Int
            if (maxTextLength != null) {
                updateInputFilter()
            }
        }
        return true
    }

    private fun setMaxTextLength(value: Any): Boolean {
        if (maxTextLength != value) {
            maxTextLength = value as Int
            updateInputFilter()
        }
        return true
    }

    private fun updateInputFilter() {
        val activeMaxLength = maxTextLength
        filters = if (activeMaxLength != null && activeMaxLength > 0) {
            filters.filterNot { it is KRBaseLengthFilter }.toTypedArray() + createInputFilter(lengthLimitType)
        } else {
            filters.filterNot { it is KRBaseLengthFilter }.toTypedArray()
        }
    }

    private fun createInputFilter(type: Int): InputFilter {
        val length = maxTextLength!!
        val beyondLimitCallback: () -> Unit = { textLengthBeyondLimitCallback?.invoke(null) }

        return when (type) {
            LENGTH_LIMIT_TYPE_BYTE -> {
                KRByteLengthFilter(
                    length,
                    kuiklyRenderContext,
                    { fontSize },
                    beyondLimitCallback,
                    { textPostProcessor }
                )
            }

            LENGTH_LIMIT_TYPE_CHARACTER -> {
                KRCharacterLengthFilter(
                    length,
                    kuiklyRenderContext,
                    { fontSize },
                    beyondLimitCallback,
                    { textPostProcessor }
                )
            }

            LENGTH_LIMIT_TYPE_VISUAL_WIDTH -> {
                KRVisualWidthLengthFilter(
                    length,
                    kuiklyRenderContext,
                    { fontSize },
                    beyondLimitCallback,
                    { textPostProcessor }
                )
            }

            else -> {
                // otherwise, use the deprecated filter for backward compatibility
                @Suppress("DEPRECATION")
                KRTextLengthLimitInputFilter(
                    length,
                    kuiklyRenderContext,
                    { fontSize },
                    beyondLimitCallback
                )
            }
        }
    }

    private fun getLengthLimitInputFilter(): KRBaseLengthFilter? {
        if (lengthLimitType == LENGTH_LIMIT_TYPE_UNSET) {
            return null
        }
        filters.forEach { filter ->
            if (filter is KRBaseLengthFilter) {
                return filter
            }
        }
        return null
    }

    private fun calculateLengthWithFilter(text: CharSequence): Int? {
        return getLengthLimitInputFilter()?.calculateLength(text)
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
            "none" -> {
                EditorInfo.IME_ACTION_NONE
            }
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
            "previous" -> {
                EditorInfo.IME_ACTION_PREVIOUS
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
        super.setSelection(index.coerceIn(0, text?.length ?: 0))
    }

    private fun setTintColor(propValue: Any): Boolean {
        if (propValue is String && propValue.isNotEmpty()) {
            setCursorDrawableColor(propValue.toColor())
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                textCursorDrawable = null
            }
        }
        return true
    }

    private fun setText(propValue: Any): Boolean {
        setInputEditorAdapterIfNeed()
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
        if (!isSettingTextInputState) {
            selectionChangeCallback?.invoke(createTextInputStateParamMap())
        }
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
        setSelection(index.coerceIn(0, text?.length ?: 0))
    }

    private fun setTextInputState(params: String?) {
        val json = runCatching { JSONObject(params ?: "{}") }.getOrElse { JSONObject() }
        val rawText = json.optString(KEY_TEXT, "")
        if (shouldRejectProgrammaticShortcodeInput(rawText)) {
            textLengthBeyondLimitCallback?.invoke(null)
            textInputStateChangeCallback?.invoke(createTextInputStateParamMap())
            return
        }
        val limitedRawText = applyProgrammaticInputFilters(rawText)
        val selectionStart = json.optInt(KEY_SELECTION_START, limitedRawText.length).coerceIn(0, limitedRawText.length)
        val selectionEnd = json.optInt(KEY_SELECTION_END, selectionStart).coerceIn(0, limitedRawText.length)
        isSettingTextInputState = true
        try {
            setInputEditorAdapterIfNeed()
            lineHeightSpan?.let { span ->
                val spannable = SpannableString(limitedRawText)
                if (limitedRawText.isNotEmpty()) {
                    spannable.setSpan(span, 0, limitedRawText.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                }
                super.setText(spannable, BufferType.EDITABLE)
            } ?: super.setText(limitedRawText, BufferType.EDITABLE)
            applyEmojiSpans(editableText)
            // 程序化文本也可能被长度限制截断，需要基于实际文本长度调整 selection
            val actualLength = editableText?.length ?: 0
            val actualStart = selectionStart.coerceIn(0, actualLength)
            val actualEnd = selectionEnd.coerceIn(0, actualLength)
            setSelection(actualStart, actualEnd)
        } finally {
            isSettingTextInputState = false
        }
        // 程序化文本变更后，手动触发回调，确保 Kotlin 层能获取到正确的 length
        val callbackData = createTextInputStateParamMap()
        textInputStateChangeCallback?.invoke(callbackData)
    }

    private fun applyProgrammaticInputFilters(rawText: String): String {
        if (rawText.isEmpty() || filters.isEmpty()) {
            return rawText
        }
        var result: CharSequence = SpannableStringBuilder(rawText)
        filters.forEach { filter ->
            val filtered = filter.filter(result, 0, result.length, SpannableStringBuilder(), 0, 0)
            result = when {
                filtered == null -> result
                filtered is SpannableStringBuilder -> filtered
                else -> SpannableStringBuilder(filtered)
            }
        }
        return result.toString()
    }

    private fun shouldRejectProgrammaticShortcodeInput(rawText: String): Boolean {
        return shouldRejectProgrammaticShortcodeInputRequest(
            rawText = rawText,
            maxTextLength = maxTextLength,
            lengthLimitType = lengthLimitType,
            calculateProgrammaticLength = ::calculateProgrammaticLength
        )
    }

    private fun calculateProgrammaticLength(rawText: String): Int? {
        val processedText = applyTextPostProcessorForLengthCalculation(rawText)
        return calculateLengthWithFilter(processedText)
    }

    private fun getTextInputState(callback: KuiklyRenderCallback?) {
        callback?.invoke(createTextInputStateParamMap())
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeTextChanged(propValue: Any): Boolean {
        textDidChangeCallback = propValue as KuiklyRenderCallback
        observeTextWatcher()
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeTextInputStateChanged(propValue: Any): Boolean {
        textInputStateChangeCallback = propValue as KuiklyRenderCallback
        observeTextWatcher()
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeSelectionChanged(propValue: Any): Boolean {
        selectionChangeCallback = propValue as KuiklyRenderCallback
        return true
    }

    private fun observeInputReturn(propValue: Any): Boolean {
        inputReturnCallBack = propValue as KuiklyRenderCallback
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
            inputReturnCallBack?.invoke(mapOf(
                "ime_action" to imeAction,
                "text" to text.toString()
            ))

            // 根据 autoHideKeyboardOnImeAction 属性决定是否收起键盘
            // 默认值为 false（不自动收起），只有显式设置为 true 才自动收起
            if (autoHideKeyboardOnImeAction) {
                setBlur()
            }

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
        val rawText = text
        // 应用 textPostProcessor 获取处理后的文本（如将 [smile] 转为 ImageSpan）
        val processedText = applyTextPostProcessorForLengthCalculation(rawText)
        val length = if (lengthLimitType != LENGTH_LIMIT_TYPE_UNSET) {
            getLengthLimitInputFilter()?.calculateLength(processedText)
        } else {
            null
        }
        return if (length == null) {
            mapOf(
                KEY_TEXT to rawText.toString()
            )
        } else {
            mapOf(
                KEY_TEXT to rawText.toString(),
                KEY_LENGTH to length!!
            )
        }
    }

    private fun createTextInputStateParamMap(): Map<String, Any> {
        val rawText = text?.toString() ?: KRCssConst.EMPTY_STRING
        val selectionStart = selectionStart.coerceIn(0, rawText.length)
        val selectionEnd = selectionEnd.coerceIn(0, rawText.length)
        val result = mutableMapOf<String, Any>(
            KEY_TEXT to rawText,
            KEY_SELECTION_START to selectionStart,
            KEY_SELECTION_END to selectionEnd,
            KEY_COMPOSITION_START to NO_COMPOSITION,
            KEY_COMPOSITION_END to NO_COMPOSITION
        )
        // 应用 textPostProcessor 获取处理后的文本（如将 [smile] 转为 ImageSpan）
        val processedText = applyTextPostProcessorForLengthCalculation(text)
        val length = if (lengthLimitType != LENGTH_LIMIT_TYPE_UNSET) {
            calculateLengthWithFilter(processedText)
        } else {
            null
        }
        length?.let { result[KEY_LENGTH] = it }
        return result
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

    /**
     * 应用 textPostProcessor 处理文本，用于长度计算
     * 将短码（如 [smile]）转为 ImageSpan，以便正确计算字符数
     */
    private fun applyTextPostProcessorForLengthCalculation(text: CharSequence?): CharSequence {
        if (text == null) return ""
        if (textPostProcessor.isEmpty()) return text
        val adapter = KuiklyRenderAdapterManager.krTextPostProcessorAdapter ?: return text

        val tp = textProps ?: KRTextProps(kuiklyRenderContext).also {
            it.fontSize = if (fontSize > 0) fontSize else 16f
        }
        val output = adapter.onTextPostProcess(
            kuiklyRenderContext,
            TextPostProcessorInput(textPostProcessor, text, tp)
        )
        return output.text
    }

    /**
     * Apply emoji ImageSpans to the given Editable by delegating to the text post-processor adapter.
     * Called from afterTextChanged to ensure emoji spans are refreshed on every text change.
     */
    private fun applyEmojiSpans(editable: Editable?) {
        if (editable == null) return
        val adapter = KuiklyRenderAdapterManager.krTextPostProcessorAdapter ?: return
        val tp = textProps ?: KRTextProps(kuiklyRenderContext).also {
            it.fontSize = if (fontSize > 0) fontSize else 16f
        }
        if (textPostProcessor.isEmpty()) return
        val output = adapter.onTextPostProcess(
            kuiklyRenderContext,
            TextPostProcessorInput(textPostProcessor, editable, tp)
        )
        val outputText = output.text
        if (outputText is Spannable) {
            // Remove existing ImageSpans and apply new ones from the processed output
            val oldSpans = editable.getSpans(0, editable.length, ImageSpan::class.java)
            for (span in oldSpans) {
                editable.removeSpan(span)
            }
            val newSpans = outputText.getSpans(0, outputText.length, ImageSpan::class.java)
            for (span in newSpans) {
                val start = outputText.getSpanStart(span)
                val end = outputText.getSpanEnd(span)
                if (start >= 0 && end <= editable.length) {
                    editable.setSpan(span, start, end, outputText.getSpanFlags(span))
                }
            }
        }
    }

    private fun setInputEditorAdapterIfNeed() {
        if (textPostProcessor.isEmpty()) return
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
                val tp = textProps ?: KRTextProps(kuiklyRenderContext).also {
                    it.fontSize = if (fontSize > 0) fontSize else 16f
                }
                val outputText = textPostProcessorAdapter.onTextPostProcess(kuiklyRenderContext, TextPostProcessorInput(textPostProcessor,
                    source, tp)).text
                return if (outputText is Editable) {
                    outputText
                } else {
                    SpannableStringBuilder(source)
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
                    if (isSettingTextInputState) {
                        return
                    }
                    s?.also(::ensureLineHeightSpan)
                    applyEmojiSpans(s)
                    textInputStateChangeCallback?.invoke(createTextInputStateParamMap())
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
        private const val PROP_KEY_TEXT_POST_PROCESSOR = "textPostProcessor"
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
        private const val LENGTH_LIMIT_TYPE = "lengthLimitType"
        private const val MAX_TEXT_LENGTH = "maxTextLength"
        private const val EDITABLE = "editable"
        private const val TEXT_DID_CHANGE = "textDidChange"
        private const val TEXT_INPUT_STATE = "textInputState"
        private const val TEXT_INPUT_STATE_CHANGE = "textInputStateChange"
        private const val SELECTION_CHANGE = "selectionChange"
        private const val INPUT_RETURN = "inputReturn"
        private const val INPUT_FOCUS = "inputFocus"
        private const val INPUT_BLUR = "inputBlur"
        private const val IME_ACTION = "imeAction"
        private const val TEXT_LENGTH_BEYOND_LIMIT = "textLengthBeyondLimit"
        private const val KEYBOARD_HEIGHT_CHANGE = "keyboardHeightChange"
        private const val IME_NO_FULLSCREEN = "imeNoFullscreen"
        private const val AUTO_HIDE_KEYBOARD_ON_IME_ACTION = "autoHideKeyboardOnImeAction"

        private const val METHOD_SET_TEXT = "setText"
        private const val METHOD_FOCUS = "focus"
        private const val METHOD_BLUR = "blur"
        private const val METHOD_GET_CURSOR_INDEX = "getCursorIndex"
        private const val METHOD_SET_CURSOR_INDEX = "setCursorIndex"
        private const val METHOD_SET_TEXT_INPUT_STATE = "setTextInputState"
        private const val METHOD_GET_TEXT_INPUT_STATE = "getTextInputState"

        private const val TYPE_ENABLE_EDIT = 1
        private const val TYPE_ENABLE_HIDE_KEYBOARD = 1

        private const val KEY_KEYBOARD_CHANGED_DURATION = "duration"
        private const val DEFAULT_KEYBOARD_CHANGED_ANIMATION_DURATION = 0.2

        private const val KEY_TEXT = "text"
        private const val KEY_SELECTION_START = "selectionStart"
        private const val KEY_SELECTION_END = "selectionEnd"
        private const val KEY_COMPOSITION_START = "compositionStart"
        private const val KEY_COMPOSITION_END = "compositionEnd"
        private const val KEY_LENGTH = "length"
        private const val NO_COMPOSITION = -1

        private const val LENGTH_LIMIT_TYPE_UNSET = -1
        private const val LENGTH_LIMIT_TYPE_BYTE = 0
        private const val LENGTH_LIMIT_TYPE_CHARACTER = 1
        private const val LENGTH_LIMIT_TYPE_VISUAL_WIDTH = 2
    }
}

private val PROGRAMMATIC_SHORTCODE_REGEX = Regex("\\[[a-zA-Z0-9_\\-]+\\]")

internal fun shouldRejectProgrammaticShortcodeInputRequest(
    rawText: String,
    maxTextLength: Int?,
    lengthLimitType: Int,
    calculateProgrammaticLength: (String) -> Int?
): Boolean {
    if (maxTextLength == null || maxTextLength <= 0) {
        return false
    }
    if (lengthLimitType == -1 || !PROGRAMMATIC_SHORTCODE_REGEX.containsMatchIn(rawText)) {
        return false
    }
    val requestedLength = calculateProgrammaticLength(rawText) ?: return false
    return requestedLength > maxTextLength
}
