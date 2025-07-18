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

@file:Suppress("DEPRECATION")

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
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
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
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.constrain
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.views.AutoHeightTextAreaView
import com.tencent.kuikly.core.views.TextAreaAttr
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
    (this as? KNode<*>)?.run {
        (view as? AutoHeightTextAreaView)?.run(action)
    }
}

const val CHANGE_LINE_SPACE = 3

@Composable
internal fun CoreTextField(
    value: TextFieldValue = TextFieldValue("你好中国"),
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier.size(200.dp, 50.dp),
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions? = null,
    singleLine: Boolean = false,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Unspecified),
) {
    val compositeKeyHash = currentCompositeKeyHash
    val localMap = currentComposer.currentCompositionLocalMap
    var singleLineNew = singleLine
    if (!singleLineNew) {
        singleLineNew = keyboardOptions?.keyboardType == KeyboardType.Password
    }

    val autoHeightTextAreaView by remember { mutableStateOf(AutoHeightTextAreaView(singleLineNew)) }

    var lineHeight by remember { mutableStateOf(0f) }
    var oldSize by remember { mutableStateOf(IntSize.Zero) }

    val measurePolicy = remember(value) {
        object : MeasurePolicy {
            private val placementBlock: Placeable.PlacementScope.() -> Unit = {}
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                // 计算文本实际需要的尺寸
                autoHeightTextAreaView.onTextChanged(" ")
                autoHeightTextAreaView.shadow!!.calculateRenderViewSize(
                    constraints.maxWidth.toFloat() / density,
                    constraints.maxHeight.toFloat() / density
                ).apply {
                    lineHeight = this.height * density
                }

                val size = if (value.text == "") {
                    com.tencent.kuikly.core.base.Size(0f, lineHeight / density)
                } else {
                    autoHeightTextAreaView.onTextChanged(value.text)
                    if (singleLine) {
                        com.tencent.kuikly.core.base.Size(
                            autoHeightTextAreaView.shadow
                                ?.calculateRenderViewSize(
                                    constraints.maxWidth / density,
                                    lineHeight / density
                                )!!.width,
                            lineHeight / density
                        )
                    } else {
                        autoHeightTextAreaView.shadow?.calculateRenderViewSize(
                            constraints.maxWidth / density,
                            constraints.maxHeight / density
                        )
                    }
                }

                var intSize = IntSize(0, 0)
                size?.also {
                    intSize = IntSize(
                        ceil(it.width * density).toInt(),
                        ceil(it.height * density).toInt()
                    )
                }

                // 在 Row 中使用 weight=1f 时，应该使用父容器提供的最大宽度
                val layoutSize = constraints.constrain(
                    IntSize(
                        constraints.maxWidth,  // 使用最大可用宽度，确保占满剩余空间
                        intSize.height.coerceAtMost(constraints.maxHeight)  // 高度保持内容所需的最小高度
                    )
                )

                if (layoutSize != oldSize) {
                    val placeholderRects = mutableListOf<Rect>()
                    placeholderRects.add(
                        Rect(
                            offset = Offset(0f, 0f),
                            size = Size(
                                layoutSize.width.toFloat(),
                                layoutSize.height.toFloat()
                            )
                        )
                    )
                    val lineCount = (layoutSize.height / lineHeight).toInt()
                    onTextLayout.invoke(
                        TextLayoutResult(
                            TextLayoutInput(
                                AnnotatedString(value.text),
                                textStyle,
                                maxLines,
                                softWrap,
                                TextOverflow.Visible,
                            ),
                            MultiParagraph(lineCount, placeholderRects),
                            layoutSize
                        )
                    )
                }
                oldSize = layoutSize

                return layout(
                    layoutSize.width,
                    (layoutSize.height + lineHeight * 0.08f).toInt(),
                    placementBlock = placementBlock
                )
            }
        }
    }
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
    val combinedModifier = modifier.then(focusModifier)

    Box(modifier = Modifier
        .clickable {
            if (!hasFocus && !readOnly && enabled) {
                focusRequester.requestFocus()
            }
        }
    ) {
        decorationBox {
            ComposeNode<ComposeUiNode, KuiklyApplier>(
                factory = {
                    val textView = autoHeightTextAreaView
                    KNode(textView) {
                        getViewAttr().autofocus(false)
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
                    set(combinedModifier) {
                        this.modifier = combinedModifier
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
                        this.modifier = combinedModifier
                    }
                    set(readOnly) {
                        if (readOnly == null) return@set
                        withTextAreaView {
                            getViewAttr().editable(!readOnly)
                        }
                    }
                    set(placeholder) {
                        if (placeholder == null) return@set
                        withTextAreaView {
                            getViewAttr().placeholder(placeholder)
                        }
                    }
                    set(textStyle) {
                        if (textStyle == null || textStyle == TextStyle.Default) return@set
                        withTextAreaView {
                            getViewAttr().setTextStyle(
                                resolveDefaults(
                                    textStyle,
                                    LayoutDirection.Rtl
                                )
                            )
                        }
                    }
                    set(keyboardOptions) {
                        if (keyboardOptions == null) return@set
                        withTextAreaView {
                            updateKeyboardOptions(keyboardOptions, getViewAttr())
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

private fun updateKeyboardOptions(options: KeyboardOptions, attr: TextAreaAttr) {
    // 处理键盘类型
    when (options.keyboardType) {
        KeyboardType.Number -> attr.keyboardTypeNumber()
        KeyboardType.Email -> attr.keyboardTypeEmail()
        KeyboardType.Password -> attr.keyboardTypePassword()
        else -> {} // 默认键盘类型不需要特殊处理
    }

    // 处理输入法动作
    when (options.imeAction) {
        ImeAction.Search -> attr.returnKeyTypeSearch()
        ImeAction.Send -> attr.returnKeyTypeSend()
        ImeAction.Next -> attr.returnKeyTypeNext()
        ImeAction.Done -> attr.returnKeyTypeDone()
        // 其他情况使用默认行为
        ImeAction.Unspecified,
        ImeAction.Default,
        ImeAction.None,
        ImeAction.Previous -> {}
    }
}