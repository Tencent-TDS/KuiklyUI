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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.collection.fastMutableListOf
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.FlexPositionType
import com.tencent.kuikly.core.layout.MeasureFunction
import com.tencent.kuikly.core.layout.MeasureOutput
import com.tencent.kuikly.core.layout.isUndefined
import com.tencent.kuikly.core.module.FontModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.InputAttr.Companion.ENABLES_RETURN_KEY_AUTOMATICALLY
import com.tencent.kuikly.core.views.InputEvent.Companion.INPUT_RETURN
import com.tencent.kuikly.core.views.shadow.TextShadow

open class TextAreaView : DeclarativeBaseView<TextAreaAttr, TextAreaEvent>(), MeasureFunction {

    companion object {
        private val NON_SHADOW_PROPS by lazy(LazyThreadSafetyMode.NONE) {
            setOf(
                Attr.StyleConst.TRANSFORM,
                Attr.StyleConst.OPACITY,
                Attr.StyleConst.VISIBILITY,
                Attr.StyleConst.BACKGROUND_COLOR,
                TextConst.TEXT_COLOR,
                TextConst.TINT_COLOR,
                TextConst.TEXT_SHADOW,
                TextConst.PLACEHOLDER,
                TextConst.PLACEHOLDER_COLOR
            )
        }
    }

    private var shadow: TextShadow? = null
    private val remeasureObserver: InputEventHandlerFn = {
        if (getViewAttr().getProp(TextConst.VALUES) != null) {
            // 如果设置了inputSpans，则必须主动监听textDidChange更新输入文本，无需自动重测量
        } else {
            shadow?.setProp(TextConst.VALUE, it.text)
            flexNode.markDirty()
        }
    }
    // 标记是否正在处理原生事件，避免反向同步导致选择状态被重置
    internal var isProcessingNativeEvent: Boolean = false
    // 记录上一次原生层真实生效的编辑态，用于判断是否需要重新同步
    internal var lastSyncedTextInputState: TextInputState? = null

    override fun willInit() {
        super.willInit()
        shadow = TextShadow(pagerId, nativeRef, ViewConst.TYPE_RICH_TEXT)
        getViewAttr().apply {
            fontSize(15)
            lineHeight(20f)
        }
    }

    override fun createAttr(): TextAreaAttr {
        return TextAreaAttr()
    }

    override fun createEvent(): TextAreaEvent {
        val event = TextAreaEvent()
        // 设置原生事件触发前的回调，用于标记正在处理原生事件
        event.beforeNativeEventCallback = {
            this.isProcessingNativeEvent = true
        }
        event.nativeTextInputStateCallback = {
            this.lastSyncedTextInputState = it
        }
        return event
    }

    override fun viewName(): String {
        return ViewConst.TYPE_TEXT_AREA
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        flexNode.measureFunction = null
        shadow?.removeFromParentComponent()
        shadow = null
    }

    override fun createFlexNode() {
        super.createFlexNode()
        flexNode.measureFunction = this
    }

    override fun createRenderView() {
        super.createRenderView()
        if (attr.autofocus) {
            focus()
        }
    }

    override fun didSetProp(propKey: String, propValue: Any) {
        // 处理 TEXT_INPUT_STATE prop，添加防循环同步逻辑
        if (propKey == TextAreaAttr.TEXT_INPUT_STATE) {
            val state = TextInputState.decode(JSONObject(propValue.toString()))
            val hasSameEditingState = lastSyncedTextInputState?.hasSameEditingState(state) ?: false
            // 只有完整编辑态真的不同，或者不是来自原生事件时，才同步到原生层
            val shouldSyncToNative = !isProcessingNativeEvent || !hasSameEditingState
            if (shouldSyncToNative) {
                super.didSetProp(propKey, propValue)
            }
            lastSyncedTextInputState = state
            // 重置标志，等待下一次原生事件
            isProcessingNativeEvent = false
        } else {
            super.didSetProp(propKey, propValue)
            if (propKey !in NON_SHADOW_PROPS) {
                shadow?.setProp(propKey, propValue)
                flexNode.markDirty()
            }
        }
    }

    @Deprecated("Use TextAreaAttr#text() instead",
        ReplaceWith("TextAreaAttr { attr{ text() }")
    )
    fun setText(text: String) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setText", text)
        }
    }

    fun focus() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("focus", "")
        }
    }

    fun blur() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("blur", "")
        }
    }

    /**
     * 获取光标当前位置
     * @param callback 结果回调
     */
    fun cursorIndex(callback: (cursorIndex: Int) -> Unit) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("getCursorIndex", "") {
                val index = it?.optInt("cursorIndex") ?: -1
                callback(index)
            }
        }
    }

    /**
     * 设置当前光标位置
     * @param index 光标位置
     */
    fun setCursorIndex(index: Int) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setCursorIndex", index.toString())
        }
    }

    /**
     * Atomically set raw text, selection, and composition state.
     */
    fun setTextInputState(state: TextInputState) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setTextInputState", state.encode())
        }
    }

    /**
     * Get raw text, selection, and composition state from native input view.
     */
    fun getTextInputState(callback: (TextInputState) -> Unit) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("getTextInputState", "") {
                callback(TextInputState.decode(it))
            }
        }
    }

    override fun measure(
        node: FlexNode,
        width: Float,
        height: Float,
        measureOutput: MeasureOutput
    ) {
        node.layoutDimensions.run {
            if (!this[0].isUndefined() && !this[1].isUndefined()) {
                getViewEvent().removeTextDidChangeObserver(remeasureObserver)
                measureOutput.width = this[0]
                measureOutput.height = this[1]
                return
            } else {
                getViewEvent().addTextDidChangeObserver(remeasureObserver)
            }
        }
        val cWidth = if (width.isUndefined()) 100000f else width
        val cHeight = if (height.isUndefined()) -1f else height
        // measure by shadow
        val size = shadow?.calculateRenderViewSize(cWidth, cHeight)
        var outWidth = size?.width ?: 0f
        var outHeight = size?.height ?: 0f
        // check stretch
        if (!width.isUndefined() && outWidth < width && node.stretchWidth()) {
            outWidth = width
        }
        if (!height.isUndefined() && outHeight < height && node.stretchHeight()) {
            outHeight = height
        }
        // check minWidth
        node.styleMinWidth.also {
            if (!it.isUndefined() && outWidth < it) {
                outWidth = it
            }
        }
        // check maxHeight
        node.styleMaxHeight.also {
            if (!it.isUndefined() && outHeight > it) {
                outHeight = it
            }
        }
        // check minHeight
        node.styleMinHeight.also {
            if (!it.isUndefined() && outHeight < it) {
                outHeight = it
            }
        }

        measureOutput.width = outWidth
        measureOutput.height = outHeight
    }

    private fun FlexNode.stretchWidth(): Boolean {
        if (positionType != FlexPositionType.RELATIVE) {
            return false
        }
        val direction = parent?.flexDirection
        return if (direction == FlexDirection.ROW || direction == FlexDirection.ROW_REVERSE) {
            stretchMainAxis
        } else {
            parent?.layoutWidth?.isUndefined() == false && stretchCrossAxis
        }
    }

    private fun FlexNode.stretchHeight(): Boolean {
        if (positionType != FlexPositionType.RELATIVE) {
            return false
        }
        val direction = parent?.flexDirection
        return if (direction == FlexDirection.ROW || direction == FlexDirection.ROW_REVERSE) {
            parent?.layoutHeight?.isUndefined() == false && stretchCrossAxis
        } else {
            stretchMainAxis
        }
    }

    private inline val FlexNode.stretchMainAxis: Boolean get() = flex != 0f

    private inline val FlexNode.stretchCrossAxis: Boolean
        get() = alignSelf == FlexAlign.STRETCH ||
                (alignSelf == FlexAlign.AUTO && parent?.alignItems == FlexAlign.STRETCH)

    internal fun markDirty() {
        flexNode.markDirty()
        shadow?.markDirty()
    }

}

open class TextAreaAttr : Attr() {

    internal var autofocus = false

    fun text(text: String): TextAreaAttr {
        TextConst.VALUE with text
        return this
    }

    /**
     * Atomically set raw text, selection, and composition state.
     */
    fun textInputState(state: TextInputState): TextAreaAttr {
        TEXT_INPUT_STATE with state.encode()
        return this
    }

    fun textInputState(stateProvider: () -> TextInputState): TextAreaAttr {
        TEXT_INPUT_STATE with { stateProvider().encode() }
        return this
    }

    /**
     * 设置输入文本的文本样式
     * 配合TextArea的textDidChange来更改spans实现输入框富文本化
     * 注：设置新inputSpans后，光标会保持原index
     * @param spans 富文本样式
     */
    fun inputSpans(spans: InputSpans): TextAreaAttr {
        TextConst.VALUES with spans.toJSONArray().toString()
        return this
    }

    fun fontSize(size: Any): TextAreaAttr {
        TextConst.FONT_SIZE with size
        return this
    }

    fun fontSize(size: Float, scaleFontSizeEnable: Boolean? = null): TextAreaAttr {
        TextConst.FONT_SIZE with FontModule.scaleFontSize(size, scaleFontSizeEnable)
        return this
    }

    fun lines(lines: Int): TextAreaAttr {
        TextConst.LINES with lines
        return this
    }

    fun fontWeightNormal(): TextAreaAttr {
        TextConst.FONT_WEIGHT with "400"
        return this
    }

    fun fontWeightBold(): TextAreaAttr  {
        TextConst.FONT_WEIGHT with "700"
        return this
    }

    fun fontWeightMedium(): TextAreaAttr  {
        TextConst.FONT_WEIGHT with "500"
        return this
    }

    fun textAlignCenter(): TextAreaAttr {
        TextConst.TEXT_ALIGN with "center"
        return this
    }

    fun textAlignLeft(): TextAreaAttr {
        TextConst.TEXT_ALIGN with "left"
        return this
    }

    fun textAlignRight(): TextAreaAttr {
        TextConst.TEXT_ALIGN with "right"
        return this
    }

    fun color(color: Color): TextAreaAttr {
        TextConst.TEXT_COLOR with color.toString()
        return this
    }

    fun tintColor(color: Color): TextAreaAttr {
        TextConst.TINT_COLOR with color.toString()
        return this
    }

    fun placeholderColor(color: Color): TextAreaAttr {
        TextConst.PLACEHOLDER_COLOR with color.toString()
        return this
    }

    fun placeholder(placeholder: String): TextAreaAttr {
        TextConst.PLACEHOLDER with placeholder
        return this
    }

    /**
     * Set text post-processor name.
     * Works with KRTextPostProcessorAdapter to enable features like emoji shortcode replacement.
     * @param processor processor name, e.g. "input"
     */
    fun textPostProcessor(processor: String): TextAreaAttr {
        "textPostProcessor" with processor
        return this
    }

    @Deprecated(
        "Use maxTextLength(length: Int, type: LengthLimitType) instead, and choose the type explicitly when migrating."
    )
    fun maxTextLength(maxLength: Int) {
        "maxTextLength" with maxLength
    }

    /**
     * 设置最大文本长度限制
     * @param length 最大长度值
     * @param type 内置长度限制类型
     */
    fun maxTextLength(length: Int, type: LengthLimitType) {
        "lengthLimitType" with type.value
        "maxTextLength" with length
    }

    fun keyboardTypePassword(): TextAreaAttr {
        KEYBOARD_TYPE with "password"
        return this
    }

    fun keyboardTypeNumber() {
        KEYBOARD_TYPE with "number"
    }

    fun keyboardTypeEmail() {
        KEYBOARD_TYPE with "email"
    }

    fun returnKeyTypeNone() {
        RETURN_KEY_TYPE with "none"
    }

    fun returnKeyTypeSearch() {
        RETURN_KEY_TYPE with "search"
    }

    fun returnKeyTypeSend() {
        RETURN_KEY_TYPE with "send"
    }

    fun returnKeyTypeDone() {
        RETURN_KEY_TYPE with "done"
    }

    fun returnKeyTypeNext() {
        RETURN_KEY_TYPE with "next"
    }

    fun returnKeyTypeContinue() {
        RETURN_KEY_TYPE with "continue"
    }

    fun returnKeyTypeGo() {
        RETURN_KEY_TYPE with "go"
    }

    fun returnKeyTypeGoogle() {
        RETURN_KEY_TYPE with "google"
    }

    fun returnKeyTypePrevious() {
        RETURN_KEY_TYPE with "previous"
    }

    fun autofocus(focus: Boolean) {
        autofocus = focus
    }

    fun editable(editable: Boolean) {
        "editable" with editable.toInt()
    }

    /**
     * 是否使用dp作为字体单位
     * android上，字体默认是映射到sp, 如果不想字体跟随系统的字体大小，
     * 可指定文本使用useDpFontSizeDim(true)来表示不跟随系统字体大小
     * @param useDp 是否使用dp单位作为字体大小单位
     * @return 对象本身
     */
    fun useDpFontSizeDim(useDp: Boolean = true): TextAreaAttr {
        TextConst.TEXT_USE_DP_FONT_SIZE_DIM with useDp.toInt()
        return this
    }

    fun lineHeight(lineHeight: Float): TextAreaAttr {
        TextConst.LINE_HEIGHT with lineHeight
        return this
    }

    /**
     * 仅iOS支持
     * 当设置为true的时候，输入框中如果是空的，则软键盘的Return Key会自动置灰禁用，非空的时候自动启用。
     */
    fun enablesReturnKeyAutomatically(flag: Boolean): TextAreaAttr{
        ENABLES_RETURN_KEY_AUTOMATICALLY with if( flag ) 1 else 0
        return this
    }

    /**
     * 是否启用拼音输入回调
     * @param enable 是否启用，默认为false
     */
    fun enablePinyinCallback(enable: Boolean = false): TextAreaAttr {
        "enablePinyinCallback" with (if (enable) 1 else 0)
        return this
    }

    /**
     * 设置是否在点击 IME 动作按钮（如 Send/Go/Search）时自动收起键盘
     *
     * @param autoHide 是否自动收起键盘, 默认状态由三端各自的autoHideKeyboardOnImeAction决定
     *                 - 若设置为true: 点击 Send 等按钮后自动收起键盘
     *                 - 若设置为false: 点击 Send 等按钮后保持键盘打开，由业务自己控制
     */
    fun autoHideKeyboardOnImeAction(enable: Boolean): TextAreaAttr {
        TextConst.AUTO_HIDE_KEYBOARD_ON_IME_ACTION with (if (enable) 1 else 0)
        return this
    }

    companion object {
        const val RETURN_KEY_TYPE = "returnKeyType"
        const val KEYBOARD_TYPE = "keyboardType"
        const val TEXT_INPUT_STATE = "textInputState"
    }
}

class InputSpans {

    private val spans = fastMutableListOf<InputSpan>()

    fun addSpan(span: InputSpan): InputSpans {
        spans.add(span)
        return this
    }

    fun removeSpan(span: InputSpan): InputSpans {
        spans.remove(span)
        return this
    }

    fun removeSpanAt(index: Int): InputSpans {
        if (index < 0 || index >= spans.size) {
            return this
        }
        spans.removeAt(index)
        return this
    }

    fun clear(): InputSpans {
        spans.clear()
        return this
    }

    internal fun toJSONArray(): JSONArray {
        val values = JSONArray()
        spans.forEach {
            values.put(it.toJSON())
        }
        return values
    }
}

@ScopeMarker
class InputSpan {
    private val propMap = JSONObject()

    fun text(text: String): InputSpan {
        propMap.put(TextConst.VALUE, text)
        return this
    }

    fun fontSize(size: Any): InputSpan {
        propMap.put(TextConst.FONT_SIZE, size)
        return this
    }

    fun color(color: Color): InputSpan {
        propMap.put(TextConst.TEXT_COLOR, color.toString())
        return this
    }

    fun fontWeightNormal(): InputSpan {
        propMap.put(TextConst.FONT_WEIGHT, "400")
        return this
    }

    fun fontWeightMedium(): InputSpan  {
        propMap.put(TextConst.FONT_WEIGHT, "500")
        return this
    }

    fun fontWeightBold(): InputSpan  {
        propMap.put(TextConst.FONT_WEIGHT, "700")
        return this
    }

    fun lineHeight(lineHeight: Float): InputSpan {
        propMap.put(TextConst.LINE_HEIGHT, lineHeight)
        return this
    }

    internal fun toJSON(): JSONObject {
        return propMap
    }
}

open class TextAreaEvent : Event() {

    private val syncTextDidChangeObservers = fastMutableListOf<InputEventHandlerFn>()
    private var textDidChangeHandler: InputEventHandlerFn? = null
    private var isSyncEdit = false
    // 原生事件触发前的回调，用于设置 View 的标志位
    internal var beforeNativeEventCallback: (() -> Unit)? = null
    // 原生层真实编辑态回调，用于避免业务回填同一状态时反向覆盖 selection/composition
    internal var nativeTextInputStateCallback: ((TextInputState) -> Unit)? = null

    internal fun addTextDidChangeObserver(observer: InputEventHandlerFn) {
        if (observer in syncTextDidChangeObservers) {
            return
        }
        val empty = syncTextDidChangeObservers.isEmpty()
        syncTextDidChangeObservers.add(observer)
        if (empty) {
            updateTextDidChangeInternal()
        }
    }

    internal fun removeTextDidChangeObserver(observer: InputEventHandlerFn) {
        if (observer !in syncTextDidChangeObservers) {
            return
        }
        syncTextDidChangeObservers.remove(observer)
        if (syncTextDidChangeObservers.isEmpty()) {
            updateTextDidChangeInternal()
        }
    }

    private fun updateTextDidChangeInternal() {
        if (textDidChangeHandler == null && syncTextDidChangeObservers.isEmpty()) {
            super.unRegister(TEXT_DID_CHANGE)
        } else {
            super.register(TEXT_DID_CHANGE, {
                it as JSONObject
                val text = it.optString("text")
                val length = if (it.has("length")) it.optInt("length") else null
                val params = InputParams(text, length = length)
                syncTextDidChangeObservers.forEach { observer -> observer.invoke(params) }
                textDidChangeHandler?.invoke(params)
            }, isSync = isSyncEdit || syncTextDidChangeObservers.isNotEmpty())
        }
    }

    override fun unRegister(eventName: String) {
        if (eventName == TEXT_DID_CHANGE) {
            if (textDidChangeHandler == null) {
                return
            }
            textDidChangeHandler = null
            isSyncEdit = false
            updateTextDidChangeInternal()
        } else {
            super.unRegister(eventName)
        }
    }

    /**
     * 当文本发生变化时调用的方法
     * @param isSyncEdit 是否同步编辑，该值为true则可以实现同步修改输入文本不会异步更新带来的跳变
     * @param handler 处理文本变化事件的回调函数
     */
    fun textDidChange(isSyncEdit: Boolean = false, handler: InputEventHandlerFn) {
        if (handler == this.textDidChangeHandler && isSyncEdit == this.isSyncEdit) {
            return
        }
        this.isSyncEdit = isSyncEdit
        this.textDidChangeHandler = handler
        updateTextDidChangeInternal()
    }

    /**
     * Called when raw text, selection, or composition changes.
     */
    fun textInputStateChange(isSyncEdit: Boolean = true, handler: TextInputStateHandlerFn) {
        this.register(TEXT_INPUT_STATE_CHANGE, {
            // 标记正在处理原生事件，避免 didSetProp 反向同步导致选择状态被重置
            beforeNativeEventCallback?.invoke()
            val state = TextInputState.decode(it as? JSONObject)
            nativeTextInputStateCallback?.invoke(state)
            handler(state)
        }, isSync = isSyncEdit)
    }

    /**
     * Called when selection changes without requiring text changes.
     */
    fun selectionChange(handler: TextInputStateHandlerFn) {
        this.register(SELECTION_CHANGE, {
            // 标记正在处理原生事件，避免 didSetProp 反向同步导致选择状态被重置
            beforeNativeEventCallback?.invoke()
            val state = TextInputState.decode(it as? JSONObject)
            nativeTextInputStateCallback?.invoke(state)
            handler(state)
        }, isSync = true)
    }

    /**
     * 当输入框获得焦点时调用的方法
     * @param handler 处理输入框获得焦点事件的回调函数
     */
    fun inputFocus(handler: InputEventHandlerFn) {
        this.register(INPUT_FOCUS){
            it as JSONObject
            val text = it.optString("text")
            handler(InputParams(text))
        }
    }
    /**
     * 当输入框失去焦点时调用的方法
     * @param handler 处理输入框失去焦点事件的回调函数
     */
    fun inputBlur(handler: InputEventHandlerFn) {
        this.register(INPUT_BLUR){
            it as JSONObject
            val text = it.optString("text")
            handler(InputParams(text))
        }
    }

    /**
     * Called when keyboard height changes.
     * @param isSync Sync callback to ensure UI animation syncs with keyboard, default true
     * @param handler Callback handler with keyboard params
     */
    fun keyboardHeightChange(isSync: Boolean = true, handler: (KeyboardParams) -> Unit) {
        this.register(KEYBOARD_HEIGHT_CHANGE, {
            it as JSONObject
            val height = it.optDouble("height").toFloat()
            val duration = it.optDouble("duration").toFloat()
            val curve = it.optInt("curve")
            handler(KeyboardParams(height, duration, curve))
        }, isSync = isSync)
    }
    /**
     * 当文本长度超过限制时调用的方法(即输入长度超过attr.maxTextLength属性设置的长度)
     * @param handler 处理文本长度超过限制事件的回调函数
     */
    fun textLengthBeyondLimit(handler: EventHandlerFn /* = (parma: kotlin.Any?) -> kotlin.Unit */) {
        this.register(TEXT_LENGTH_BEYOND_LIMIT,handler)
    }

    /**
     * 当用户按下return键时调用的方法
     * @param handler 处理用户按下return键事件的回调函数
     */
    fun inputReturn(handler: InputEventHandlerFn) {
        register(INPUT_RETURN){
            it as JSONObject
            val text = it.optString("text")
            val imeAction = it.optString("ime_action").ifEmpty {
                getView()?.getViewAttr()?.getProp(InputAttr.RETURN_KEY_TYPE) as? String ?: ""
            }
            handler(InputParams(text, imeAction))
        }
    }

    /**
     * 当用户按下键盘IME动作按键是回调，例如 Send / Go / Search 等
     * @param handler 用户按下对应按键时的回调函数
     */
    @Deprecated("Use inputReturn instead", ReplaceWith("inputReturn { (_, it) -> handler(it) }"))
    fun onIMEAction(handler: IMEActionEventHandlerFn) {
        register(IME_ACTION){
            it as JSONObject
            val imeAction = it.optString("ime_action")
            handler(imeAction)
        }
    }

    override fun onViewDidRemove() {
        super.onViewDidRemove()
        syncTextDidChangeObservers.clear()
        textDidChangeHandler = null
        isSyncEdit = false
    }

    companion object {
        const val TEXT_DID_CHANGE = "textDidChange"
        const val TEXT_INPUT_STATE_CHANGE = "textInputStateChange"
        const val SELECTION_CHANGE = "selectionChange"
        const val INPUT_FOCUS = "inputFocus"
        const val INPUT_BLUR = "inputBlur"
        const val KEYBOARD_HEIGHT_CHANGE = "keyboardHeightChange"
        const val TEXT_LENGTH_BEYOND_LIMIT = "textLengthBeyondLimit"
        const val IME_ACTION = "imeAction"
    }
}

fun ViewContainer<*, *>.TextArea(init: TextAreaView.() -> Unit) {
    addChild(TextAreaView(), init)
}

typealias IMEActionEventHandlerFn = (imeAction: String) -> Unit
