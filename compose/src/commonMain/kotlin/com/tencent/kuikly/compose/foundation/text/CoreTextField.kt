/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.foundation.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.KuiklyApplier
import com.tencent.kuikly.compose.extension.SetEventElement
import com.tencent.kuikly.compose.extension.SetPropElement
import com.tencent.kuikly.compose.foundation.interaction.Interaction
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.FocusManager
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.layout.Measurable
import com.tencent.kuikly.compose.ui.layout.MeasurePolicy
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.MeasureScope
import com.tencent.kuikly.compose.ui.layout.Placeable
import com.tencent.kuikly.compose.ui.node.ComposeUiNode
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.requireOwner
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.platform.LocalLayoutDirection
import com.tencent.kuikly.compose.ui.platform.LocalSoftwareKeyboardController
import com.tencent.kuikly.compose.ui.platform.SoftwareKeyboardController
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.MultiParagraph
import com.tencent.kuikly.compose.ui.text.TextLayoutInput
import com.tencent.kuikly.compose.ui.text.TextLayoutResult
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.input.ImeAction
import com.tencent.kuikly.compose.ui.text.input.ImeOptions
import com.tencent.kuikly.compose.ui.text.input.KeyboardType
import com.tencent.kuikly.compose.ui.text.input.TextFieldValue
import com.tencent.kuikly.compose.ui.text.resolveDefaults
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.TextUnit
import com.tencent.kuikly.compose.ui.unit.constrain
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.isSpecified
import com.tencent.kuikly.compose.ui.util.fastRoundToInt
import com.tencent.kuikly.core.views.AutoHeightTextAreaView
import com.tencent.kuikly.core.views.TextAreaAttr
import com.tencent.kuikly.core.views.TextAreaEvent
import com.tencent.kuikly.core.views.TextConst
import kotlin.math.ceil

/**
 * Base composable that enables users to edit text via hardware or software keyboard.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder.
 *
 * If the editable text is larger than the size of the container, the vertical scrolling
 * behaviour will be automatically applied. To enable a single line behaviour with horizontal
 * scrolling instead, set the [maxLines] parameter to 1, [softWrap] to false, and
 * [ImeOptions.singleLine] to true.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [TextFieldValue]. [TextFieldValue] contains the text entered by user, as well
 * as selection, cursor and text composition information. Please check [TextFieldValue] for the
 * description of its contents.
 *
 * It is crucial that the value provided in the [onValueChange] is fed back into [CoreTextField] in
 * order to have the final state of the text being displayed. Example usage:
 *
 * Please keep in mind that [onValueChange] is useful to be informed about the latest state of the
 * text input by users, however it is generally not recommended to modify the values in the
 * [TextFieldValue] that you get via [onValueChange] callback. Any change to the values in
 * [TextFieldValue] may result in a context reset and end up with input session restart. Such
 * a scenario would cause glitches in the UI or text input experience for users.
 *
 * @param value The [com.tencent.kuikly.compose.ui.text.input.TextFieldValue] to be shown in the [CoreTextField].
 * @param onValueChange Called when the input service updates the values in [TextFieldValue].
 * @param modifier optional [Modifier] for this text field.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param visualTransformation The visual transformation filter for changing the visual
 * representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw a cursor or selection around the text.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this CoreTextField. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this CoreTextField in different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 * provided, there will be no cursor drawn
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space.
 * @param maxLines The maximum height in terms of maximum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines].
 * @param imeOptions Contains different IME configuration options.
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param enabled controls the enabled state of the text field. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [CoreTextField]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit
 * @param decorationBox Composable lambda that allows to add decorations around text field, such
 * as icon, placeholder, helper messages or similar, and automatically increase the hit target area
 * of the text field. To allow you to control the placement of the inner text field relative to your
 * decorations, the text field implementation will pass in a framework-controlled composable
 * parameter "innerTextField" to the decorationBox lambda you provide. You must call
 * innerTextField exactly once.
 */
private inline fun ComposeUiNode.withTextAreaView(action: AutoHeightTextAreaView.() -> Unit) {
    val textArea = (this as? KNode<*>)?.view as? AutoHeightTextAreaView ?: return
    textArea.action()
}

const val CHANGE_LINE_SPACE = 3

@Composable
internal fun CoreTextField(
    value: TextFieldValue = TextFieldValue(""),
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier.size(200.dp, 50.dp),
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions? = null,
    keyboardActions: KeyboardActions? = null,
    singleLine: Boolean = false,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Unspecified),
    extraHeight: TextUnit = TextUnit.Unspecified,
    extraHeightScale: Float? = null,
) {
    val compositeKeyHash = currentCompositeKeyHash
    val localMap = currentComposer.currentCompositionLocalMap
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    var singleLineNew = singleLine
    if (!singleLineNew) {
        singleLineNew = keyboardOptions?.keyboardType == KeyboardType.Password
    }

    val autoHeightTextAreaView by remember { mutableStateOf(AutoHeightTextAreaView(singleLineNew)) }

    var lineHeight by remember { mutableStateOf(0f) }
    var oldSize by remember { mutableStateOf(IntSize.Zero) }
    var editable = enabled && !readOnly

    val measurePolicy = remember(value) { object : MeasurePolicy {
        private val placementBlock: Placeable.PlacementScope.() -> Unit = {}
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            val pageDensity = autoHeightTextAreaView.getPager().pagerDensity()

            autoHeightTextAreaView.onTextChanged(" ")
            autoHeightTextAreaView.shadow!!.calculateRenderViewSize(
                constraints.maxWidth.toFloat() / pageDensity,
                constraints.maxHeight.toFloat() / pageDensity
            ).apply {
                // 通过空串计算出行高
                lineHeight = this.height * pageDensity
            }

            val maxWidth = constraints.maxWidth - lineHeight / CHANGE_LINE_SPACE
            val size = if (value.text == "") {
                com.tencent.kuikly.core.base.Size(0f, lineHeight / pageDensity)
            } else {
                autoHeightTextAreaView.onTextChanged(value.text)
                if (singleLine) {
                    com.tencent.kuikly.core.base.Size(autoHeightTextAreaView.shadow
                        ?.calculateRenderViewSize(
                            maxWidth / pageDensity ,
                            lineHeight / pageDensity
                        )!!.width,
                        lineHeight / pageDensity)
                } else {
                    autoHeightTextAreaView.shadow?.calculateRenderViewSize(
                        maxWidth / pageDensity,
                        constraints.maxHeight/ pageDensity)
                }
            }

            // 高度变化，重新测量
            var intSize = IntSize(1001000)
            size?.also {
                intSize = IntSize(
                    ceil(maxWidth).toInt(),
                    (it.height * pageDensity).toInt()
                        .coerceAtLeast(constraints.minHeight)
                )
            }
            val layoutSize = constraints.constrain(
                intSize
            )
            if (layoutSize != oldSize) {
                val placeholderRects = mutableListOf<Rect>()
                placeholderRects.add(
                    Rect(
                        offset = Offset(0f, 0f),
                        size = Size(layoutSize.width.toFloat(),
                            layoutSize.height.toFloat())
                    )
                )
                val lineCount = (layoutSize.height / lineHeight).toInt()
                // TODO 这个LineCount目前是不超过剩余空间的行高，而且不确定是准的 后续可能考虑Kuikly层面优化
                onTextLayout.invoke( TextLayoutResult(
                    TextLayoutInput(
                        AnnotatedString(value.text),
                        textStyle,
                        maxLines,
                        softWrap,
                        TextOverflow.Visible,
                    ),
                    MultiParagraph(lineCount, placeholderRects),
                    layoutSize
                ))
            }
            oldSize = layoutSize

            val targetHeight = if (extraHeightScale != null) {
                layoutSize.height * extraHeightScale
            } else {
                layoutSize.height.toFloat()
            }

            val targetExtraBottom = if (extraHeight.isSpecified) {
                extraHeight.toPx()
            } else {
                lineHeight * 0.08f
            }

            return layout(
                layoutSize.width,
                (targetHeight + targetExtraBottom).fastRoundToInt(),
                placementBlock = placementBlock
            )
        }
    }}

    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    // Focus
    val focusModifier = Modifier.textFieldFocusModifier(
        enabled = enabled,
        focusRequester = focusRequester,
        interactionSource = interactionSource
    ) {
        if (hasFocus == it.isFocused) {
            return@textFieldFocusModifier
        }
        hasFocus = it.isFocused

        if (it.isFocused && enabled && !readOnly) {
            requireOwner().softwareKeyboardController.startInput(autoHeightTextAreaView)
        } else {
            requireOwner().softwareKeyboardController.stopInput(autoHeightTextAreaView)
        }
    }

    val state = remember(keyboardController) {
        LegacyTextFieldState(
//            TextDelegate(
//                text = visualText,
//                style = textStyle,
//                softWrap = softWrap,
//                density = density,
//                fontFamilyResolver = fontFamilyResolver
//            ),
//            recomposeScope = scope,
            keyboardController = keyboardController
        )
    }

    if (keyboardActions != null) {
        state.update(keyboardActions, focusManager)
    }

    val pointerModifier = Modifier.textFieldPointer(
//        manager,
        enabled,
        interactionSource,
        state,
        focusRequester,
        readOnly,
//        offsetMapping,
    )

    // 一次遍历拆分 Modifier，使用 remember 避免重复计算
    val (propsAndEvents, others) = remember(modifier) { modifier.splitByPropOrEvent() }
    val combinedModifier = others.then(focusModifier)

    Box(modifier = pointerModifier.then(combinedModifier), propagateMinConstraints = true) {
        decorationBox {
            ComposeNode<ComposeUiNode, KuiklyApplier>(
                factory = {
                    val textView = autoHeightTextAreaView
                    KNode(textView) {
                        getViewAttr().autofocus(false)
                        getViewAttr().enablePinyinCallback(true)
                        getViewEvent().inputFocus {
                            focusRequester.requestFocus()
                        }

                    }
                },
                update = {
                    set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
                    set(localMap, ComposeUiNode.SetResolvedCompositionLocals)
                    @OptIn(ExperimentalComposeUiApi::class)
                    set(compositeKeyHash, ComposeUiNode.SetCompositeKeyHash)
                    set(propsAndEvents) {
                        // 从父亲抽取 TextField 相关的Modifier
                        this.modifier = propsAndEvents
                    }
                    set(hasFocus) {
                        withTextAreaView {
                            if (hasFocus) {
                                focus()
                            }
                        }
                    }
                    set(value) {
                        if (it == null) return@set
                        withTextAreaView {
                            getViewAttr().text(value.text)
                        }
                    }
                    set(editable) {
                        withTextAreaView {
                            getViewAttr().editable(editable)
                        }
                    }
                    set(placeholder) {
                        if (placeholder == null) return@set
                        withTextAreaView {
                            getViewAttr().placeholder(placeholder)
                        }
                    }

                    var isUpdate = false
                    set(textStyle) {
                        if (textStyle == null ||  textStyle == TextStyle.Default) return@set
                        withTextAreaView {
                            getViewAttr().setTextStyle(resolveDefaults(textStyle, layoutDirection), density)
                        }
                        isUpdate = true
                    }
                    if (!isUpdate) {
                        set(density) {
                            withTextAreaView {
                                getViewAttr().setTextStyle(resolveDefaults(textStyle, layoutDirection), density)
                            }
                        }
                    }

                    if (keyboardOptions?.keyboardType != KeyboardType.Password) {
                        // line要晚于KeyboardType设置 否则安卓平台上会出现Number输入时候不支持换行的情况
                        set(maxLines) {
                            withTextAreaView {
                                getViewAttr().lines(maxLines)
                            }
                        }
                    }
                    set(keyboardOptions) {
                        if (keyboardOptions == null) return@set
                        withTextAreaView {
                            updateKeyboardOptions(singleLineNew, keyboardOptions, getViewAttr())
                        }
                    }
                    set(keyboardActions) {
                        if (keyboardActions == null || keyboardActions == KeyboardActions.Default) return@set
                        withTextAreaView {
                            updateKeyboardActions(getViewEvent(), state)
                        }
                    }
                    set(onValueChange) {
                        withTextAreaView {
                            getViewEvent().textDidChange {
                                autoHeightTextAreaView.getViewAttr()
                                    .updatePropCache(TextConst.VALUE, it.text)
                                onValueChange(TextFieldValue(it.text))
                            }
                        }
                    }
                    set(cursorBrush) {
                        withTextAreaView {
                            if (cursorBrush is SolidColor) {
                                getViewAttr().tintColor(cursorBrush.value.toKuiklyColor())
                            }
                        }
                    }
                },
            )
        }
    }
}
/**
 * 将 Modifier 拆分为两部分：SetPropElement/SetEventElement 和其他 Element
 * 使用 foldOut 从内到外遍历，保持原始顺序
 */
private data class SplitModifiers(
    val propsAndEvents: Modifier,
    val others: Modifier
)

private fun Modifier.splitByPropOrEvent(): SplitModifiers {
    var propsAndEvents: Modifier = Modifier
    var others: Modifier = Modifier
    // 使用 foldOut 从内到外遍历，element.then(acc) 保持原始顺序
    foldOut(Unit) { element, _ ->
        if (element is SetPropElement || element is SetEventElement) {
            propsAndEvents = element.then(propsAndEvents)
        } else {
            others = element.then(others)
        }
    }
    return SplitModifiers(propsAndEvents, others)
}

private fun updateKeyboardOptions(
    singleLine: Boolean,
    options: KeyboardOptions,
    attr: TextAreaAttr
) {
    // 处理键盘类型
    when (options.keyboardType) {
        KeyboardType.Number -> attr.keyboardTypeNumber()
        KeyboardType.Email -> attr.keyboardTypeEmail()
        KeyboardType.Password -> attr.keyboardTypePassword()
        else -> {} // 默认键盘类型不需要特殊处理
    }

    // 处理输入法动作
    when (options.imeAction) {
        ImeAction.Go -> attr.returnKeyTypeGo()
        ImeAction.Search -> attr.returnKeyTypeSearch()
        ImeAction.Send -> attr.returnKeyTypeSend()
        ImeAction.Previous -> attr.returnKeyTypePrevious()
        ImeAction.Next -> attr.returnKeyTypeNext()
        ImeAction.Done -> attr.returnKeyTypeDone()
        ImeAction.Unspecified, // fall through
        ImeAction.Default -> {
            if (singleLine) {
                attr.returnKeyTypeDone()
            }
        }
        ImeAction.None -> {
            if (singleLine) {
                attr.returnKeyTypeNone()
            }
        }
    }
}

fun updateKeyboardActions(
    event: TextAreaEvent,
    state: LegacyTextFieldState
) {
    event.inputReturn {
        val imeAction = when (it.imeAction) {
            "go" -> ImeAction.Go
            "search" -> ImeAction.Search
            "send" -> ImeAction.Send
            "previous" -> ImeAction.Previous
            "next" -> ImeAction.Next
            "done" -> ImeAction.Done
            else -> null
        }
        imeAction?.apply {
            state.onImeActionPerformed.invoke(this)
        }
    }
}


class LegacyTextFieldState(
//    var textDelegate: TextDelegate,
//    val recomposeScope: RecomposeScope,
    val keyboardController: SoftwareKeyboardController?,
) {
//    val processor = EditProcessor()
//    var inputSession: TextInputSession? = null

    /**
     * This should be a state as every time we update the value we need to redraw it.
     * state observation during onDraw callback will make it work.
     */
    var hasFocus by mutableStateOf(false)
    //
//    /**
//     * Set to a non-zero value for single line TextFields in order to prevent text cuts.
//     */
//    var minHeightForSingleLineField by mutableStateOf(0.dp)
//
//    /**
//     * The last layout coordinates for the inner text field LayoutNode, used by selection and
//     * notifyFocusedRect. Since this layoutCoordinates only used for relative position calculation,
//     * we are guarding ourselves from using it when it's not attached.
//     */
//    private var _layoutCoordinates: LayoutCoordinates? = null
//    var layoutCoordinates: LayoutCoordinates?
//        get() = _layoutCoordinates?.takeIf { it.isAttached }
//        set(value) {
//            _layoutCoordinates = value
//        }
//
//    /**
//     * You should be using proxy type [TextLayoutResultProxy] if you need to translate touch
//     * offset into text's coordinate system. For example, if you add a gesture on top of the
//     * decoration box and want to know the character in text for the given touch offset on
//     * decoration box.
//     * When you don't need to shift the touch offset, you should be using `layoutResult.value`
//     * which omits the proxy and calls the layout result directly. This is needed when you work
//     * with the text directly, and not the decoration box. For example, cursor modifier gets
//     * position using the [TextFieldValue.selection] value which corresponds to the text directly,
//     * and therefore does not require the translation.
//     */
////    private val layoutResultState: MutableState<TextLayoutResultProxy?> = mutableStateOf(null)
////    var layoutResult: TextLayoutResultProxy?
////        get() = layoutResultState.value
////        set(value) {
////            layoutResultState.value = value
////            isLayoutResultStale = false
////        }
//
//    /**
//     * [textDelegate] keeps a reference to the visually transformed text that is visible to the
//     * user. TextFieldState needs to have access to the underlying value that is not transformed
//     * while making comparisons that test whether the user input actually changed.
//     *
//     * This field contains the real value that is passed by the user before it was visually
//     * transformed.
//     */
//    var untransformedText: AnnotatedString? = null
//
//    /**
//     * The gesture detector state, to indicate whether current state is selection, cursor
//     * or editing.
//     *
//     * In the none state, no selection or cursor handle is shown, only the cursor is shown.
//     * TextField is initially in this state. To enter this state, input anything from the
//     * keyboard and modify the text.
//     *
//     * In the selection state, there is no cursor shown, only selection is shown. To enter
//     * the selection mode, just long press on the screen. In this mode, finger movement on the
//     * screen changes selection instead of moving the cursor.
//     *
//     * In the cursor state, no selection is shown, and the cursor and the cursor handle are shown.
//     * To enter the cursor state, tap anywhere within the TextField.(The TextField will stay in the
//     * edit state if the current text is empty.) In this mode, finger movement on the screen
//     * moves the cursor.
//     */
//    var handleState by mutableStateOf(HandleState.None)
//
//    /**
//     * A flag to check if the floating toolbar should show.
//     *
//     * This state is meant to represent the floating toolbar status regardless of if all touch
//     * behaviors are disabled (like if the user is using a mouse). This is so that when touch
//     * behaviors are re-enabled, the toolbar status will still reflect whether it should be shown
//     * at that point.
//     */
//    var showFloatingToolbar by mutableStateOf(false)
//
//    /**
//     * True if the position of the selection start handle is within a visible part of the window
//     * (i.e. not scrolled out of view) and the handle should be drawn.
//     */
//    var showSelectionHandleStart by mutableStateOf(false)
//
//    /**
//     * True if the position of the selection end handle is within a visible part of the window
//     * (i.e. not scrolled out of view) and the handle should be drawn.
//     */
//    var showSelectionHandleEnd by mutableStateOf(false)
//
//    /**
//     * True if the position of the cursor is within a visible part of the window (i.e. not scrolled
//     * out of view) and the handle should be drawn.
//     */
//    var showCursorHandle by mutableStateOf(false)
//
//    /**
//     * TextFieldState holds both TextDelegate and layout result. However, these two values are not
//     * updated at the same time. TextDelegate is updated during composition according to new
//     * arguments while layoutResult is updated during layout phase. Therefore, [layoutResult] might
//     * not indicate the result of [textDelegate] at a given time during composition. This variable
//     * indicates whether layout result is lacking behind the latest TextDelegate.
//     */
//    var isLayoutResultStale: Boolean = true
//        private set
//
//    var isInTouchMode: Boolean by mutableStateOf(true)
//
    private val keyboardActionRunner: KeyboardActionRunner =
        KeyboardActionRunner(keyboardController)
    //
//    /**
//     * DO NOT USE, use [onValueChange] instead. This is original callback provided to the TextField.
//     * In order the CoreTextField to work, the recompose.invalidate() has to be called when we call
//     * the callback and [onValueChange] is a wrapper that mainly does that.
//     */
//    private var onValueChangeOriginal: (TextFieldValue) -> Unit = {}
//
//    val onValueChange: (TextFieldValue) -> Unit = {
//        if (it.text != untransformedText?.text) {
//            // Text has been changed, enter the HandleState.None and hide the cursor handle.
//            handleState = HandleState.None
//        }
//        selectionPreviewHighlightRange = TextRange.Zero
//        deletionPreviewHighlightRange = TextRange.Zero
//        onValueChangeOriginal(it)
//        recomposeScope.invalidate()
//    }
//
    val onImeActionPerformed: (ImeAction) -> Unit = { imeAction ->
        keyboardActionRunner.runAction(imeAction)
    }
    //
//    /** The paint used to draw highlight backgrounds. */
//    val highlightPaint: Paint = Paint()
//    var selectionBackgroundColor = Color.Unspecified
//
//    /** Range of text to be highlighted to display handwriting gesture previews from the IME. */
//    var selectionPreviewHighlightRange: TextRange by mutableStateOf(TextRange.Zero)
//    var deletionPreviewHighlightRange: TextRange by mutableStateOf(TextRange.Zero)
//
//    fun hasHighlight() =
//        !selectionPreviewHighlightRange.collapsed || !deletionPreviewHighlightRange.collapsed
//
    fun update(
//        untransformedText: AnnotatedString,
//        visualText: AnnotatedString,
//        textStyle: TextStyle,
//        softWrap: Boolean,
//        density: Density,
//        fontFamilyResolver: FontFamily.Resolver,
//        onValueChange: (TextFieldValue) -> Unit,
        keyboardActions: KeyboardActions,
        focusManager: FocusManager,
//        selectionBackgroundColor: Color
    ) {
//        this.onValueChangeOriginal = onValueChange
//        this.selectionBackgroundColor = selectionBackgroundColor
        this.keyboardActionRunner.apply {
            this.keyboardActions = keyboardActions
            this.focusManager = focusManager
        }
//        this.untransformedText = untransformedText
//
//        val newTextDelegate = updateTextDelegate(
//            current = textDelegate,
//            text = visualText,
//            style = textStyle,
//            softWrap = softWrap,
//            density = density,
//            fontFamilyResolver = fontFamilyResolver,
//            placeholders = emptyList(),
//        )
//
//        if (textDelegate !== newTextDelegate) isLayoutResultStale = true
//        textDelegate = newTextDelegate
    }
}

/**
 * Request focus on tap. If already focused, makes sure the keyboard is requested.
 */
internal fun requestFocusAndShowKeyboardIfNeeded(
    state: LegacyTextFieldState,
    focusRequester: FocusRequester,
    allowKeyboard: Boolean
) {
    if (allowKeyboard && !state.hasFocus) {
        focusRequester.requestFocus()
    } else if (allowKeyboard) {
        state.keyboardController?.show()
    }
}