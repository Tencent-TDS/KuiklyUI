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

@file:OptIn(ExperimentalFoundationApi::class, InternalResourceApi::class)

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.extension.RefFunc
import com.tencent.kuikly.compose.extension.keyboardHeightChange
import com.tencent.kuikly.compose.extension.nativeRef
import com.tencent.kuikly.compose.extension.placeHolder
import com.tencent.kuikly.compose.extension.setProp
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.gestures.forEachGesture
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.KeyboardActions
import com.tencent.kuikly.compose.foundation.text.KeyboardOptions
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.focus.focusRequester
import com.tencent.kuikly.compose.ui.focus.onFocusChanged
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.input.pointer.PointerId
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.input.pointer.positionChanged
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.layout.positionInRoot
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.text.TextLayoutResult
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.ui.window.DialogProperties
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.views.KeyboardParams
import com.tencent.kuikly.core.views.ModalView
import kotlinx.coroutines.delay

fun Modifier.touchListener(
    onTouchEvent: (type: TouchType, position: Offset) -> Unit
) = pointerInput(Unit) {
    forEachGesture {
        awaitPointerEventScope {
            // 初始化触控点跟踪
            var currentPointerId: PointerId? = null
            while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { change ->
                    when {
                        // 按下事件
                        change.pressed && currentPointerId == null -> {
                            currentPointerId = change.id
                            onTouchEvent(TouchType.Down, change.position)
                        }
                        // 移动事件（需匹配当前跟踪的指针）
                        change.id == currentPointerId && change.positionChanged() -> {
                            onTouchEvent(TouchType.Move, change.position)
                        }
                        // 抬起/取消事件
                        !change.pressed && change.id == currentPointerId -> {
                            onTouchEvent(TouchType.Up, change.position)
                            currentPointerId = null
                            return@awaitPointerEventScope
                        }
                    }
                }
            }
        }
    }
}

enum class TouchType { Down, Move, Up }

/**
 * 创建一个 Modal 实例。Modal 是一个自定义的模态窗口组件，用于在当前页面上显示一个浮动窗口。
 * 当模态窗口显示时，用户无法与背景页面进行交互，只能与模态窗口内的内容进行交互。
 * 模态窗口可以用于显示表单、提示信息、详细信息等场景。
 * 注：1.Modal容器尺寸和屏幕等大
 *    2.若想关闭模态，可直接通过if为false条件不创建Modal即可
 *  @param modifier 可不用设置size，因为默认固定和屏幕等大
 *  @param content 构建全屏模态下的UI内容
 */
@Composable
fun Modal(
    modifier: Modifier = Modifier,
    ref: RefFunc<ModalView>? = null,
    /* 物理键触发时调用该事件（目前仅支持鸿蒙平台） */
    onWillDismiss: EventHandlerFn? = null,
    content: (@Composable () -> Unit)
) {
    Dialog(
        onDismissRequest = { onWillDismiss?.invoke("") },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            scrimColor = Color.Transparent
        )
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun Modifier.dragEnable(enable: Boolean): Modifier {
    return this.setProp("dragEnable", enable)
}

@Composable
internal fun Button(
    onClick: () -> Unit = {},
    onClick2: (Offset) -> Unit = {},
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit) = {}
) {
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    val onClickUpdate = rememberUpdatedState(onClick)
    val onClick2Update = rememberUpdatedState(onClick2)
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .onGloballyPositioned {
                rootPosition = it.positionInRoot()
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // 获取点击的相对于 Box 的坐标
                    val clickedPositionInBox = offset
                    // 转换为相对于根节点的位置
                    val clickedPositionInRoot = with(density) {
                        Offset(
                            (clickedPositionInBox.x + rootPosition.x).toDp().value,
                            (clickedPositionInBox.y + rootPosition.y).toDp().value
                        )
                    }
                    onClickUpdate.value.invoke()
                    onClick2Update.value.invoke(clickedPositionInRoot)
                }
            } then modifier, contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
internal fun TextField(
    modifier: Modifier = Modifier,
    value: String = "",
    placeholder: String = "",
    autoFocus: Boolean = true,
    onValueChange: (String) -> Unit,
    onBlur: () -> Unit = {},
    onFocus: () -> Unit = {},
    keyboardHeightChange: (KeyboardParams) -> Unit = {},
    textStyle: TextStyle = TextStyle.Default,
    placeholderColor: Color? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    var lastFocus by remember { mutableStateOf(false) }

    val currentOnBlur by rememberUpdatedState(onBlur)
    val currentOnFocus by rememberUpdatedState(onFocus)
    val currentKeyboardHeightChange by rememberUpdatedState(keyboardHeightChange)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnTextLayout by rememberUpdatedState(onTextLayout)

    val updatedModifier = modifier
        .keyboardHeightChange(currentKeyboardHeightChange)
        .focusRequester(focusRequester)
        .onFocusChanged {
            if (it.isFocused) {
                if (!lastFocus) {
                    currentOnFocus()
                    lastFocus = true
                }
            } else if (lastFocus) {
                currentOnBlur()
                lastFocus = false
            }
        }
        .let {
            if (placeholderColor != null) {
                it.placeHolder(placeholder, placeholderColor)
            } else it
        }

    BasicTextField(
        modifier = updatedModifier,
        value = value,
        onValueChange = currentOnValueChange,
        textStyle = textStyle,
        maxLines = maxLines,
        onTextLayout = currentOnTextLayout,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = cursorBrush
    )

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun ComposeNavigationBar(titleInput: String = "", content: @Composable () -> Unit) {
    val localPager = LocalActivity.current.getPager()
    val routerModule = localPager.getModule<RouterModule>(RouterModule.MODULE_NAME)
    val title = localPager.pageData.params.optString("pageTitle", titleInput)

    Column {
        Spacer(modifier = Modifier.height(40.dp))
        Box {
            Image(
                modifier = Modifier
                    .clickable {
                        routerModule?.closePage()
                    }
                    .padding(vertical = 16.dp, horizontal = 8.dp)
                    .size(18.dp),
                painter = painterResource(DrawableResource("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII=")),
                contentDescription = ""
            )
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF333333),
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 12.dp)
                    .fillMaxWidth(),
            )
        }
        content.invoke()
    }
}