/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *2
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.material3

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.animation.AnimatedVisibility
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import com.tencent.kuikly.compose.animation.core.MutableTransitionState
import com.tencent.kuikly.compose.ui.window.DefaultScrimColor
import com.tencent.kuikly.compose.ui.window.KuiklyDialogProperties

/**
 * A modal bottom sheet that slides up from the bottom of the screen.
 * 
 * This component provides a modal bottom sheet that slides up from the bottom of the screen,
 * with a background scrim and animation effects. It can be dismissed by tapping the scrim
 * or pressing the back button.
 * 
 * Parameters:
 * @param visible Controls the visibility of the bottom sheet
 * @param onDismissRequest Callback to be invoked when the bottom sheet needs to be dismissed
 * @param modifier Modifier to be applied to the bottom sheet
 * @param containerColor Background color of the bottom sheet
 * @param contentColor Color of the content inside the bottom sheet
 * @param tonalElevation Elevation of the bottom sheet
 * @param scrimColor Color of the background scrim
 * @param content Content of the bottom sheet
 * 
 * Example:
 * ```
 * var showBottomSheet by remember { mutableStateOf(false) }
 * 
 * Button(onClick = { showBottomSheet = true }) {
 *     Text("Show Bottom Sheet")
 * }
 * 
 * ModalBottomSheet(
 *     visible = showBottomSheet,
 *     onDismissRequest = { showBottomSheet = false }
 * ) {
 *     Column(
 *         modifier = Modifier.padding(16.dp),
 *         horizontalAlignment = Alignment.CenterHorizontally
 *     ) {
 *         Text("Bottom Sheet Content")
 *         Button(onClick = { showBottomSheet = false }) {
 *             Text("Close")
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun ModalBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    content: @Composable ColumnScope.() -> Unit
) {
    var visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) {
        if (visible && !visibleState.currentState) {
            // 启动动画
            visibleState.targetState = true
        } else if (!visible && visibleState.currentState) {
            visibleState.targetState = false
        }
    }

    LaunchedEffect(visibleState.isIdle, visibleState.currentState, visibleState.targetState) {
        // 动画播完，调用onDismissRequest;
        if (visibleState.isIdle && !visibleState.currentState) {
            onDismissRequest.invoke()
        }
    }

    if (visible || visibleState.currentState) {
        Dialog(
            onDismissRequest = {
                visibleState.targetState = false
            },
            properties = KuiklyDialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
                scrimColor = scrimColor,
                contentAlignment = Alignment.BottomCenter
            )
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = slideInVertically(
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                ),
            ) {
                Surface(
                    modifier = modifier.fillMaxWidth(),
                    color = containerColor,
                    contentColor = contentColor,
                    tonalElevation = tonalElevation
                ) {
                    Column(content = content)
                }
            }
        }
    }
}

/**
 * Contains useful Defaults for [ModalBottomSheet].
 */
internal object BottomSheetDefaults {
    /**
     * The default container color for [ModalBottomSheet].
     */
    val ContainerColor: Color = Color.White

    /**
     * The default scrim color used by [ModalBottomSheet].
     */
    val ScrimColor: Color = DefaultScrimColor
}
