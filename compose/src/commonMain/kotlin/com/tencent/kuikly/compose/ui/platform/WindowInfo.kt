/*
 * Copyright 2023 The Android Open Source Project
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

package com.tencent.kuikly.compose.ui.platform

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.internal.JvmDefaultWithCompatibility
import com.tencent.kuikly.compose.ui.unit.IntSize

/**
 * Provides information about the Window that is hosting this compose hierarchy.
 */
@Stable
@JvmDefaultWithCompatibility
interface WindowInfo {
    /**
     * Indicates whether the window hosting this compose hierarchy is in focus.
     *
     * When there are multiple windows visible, either in a multi-window environment or if a
     * popup or dialog is visible, this property can be used to determine if the current window
     * is in focus.
     */
    val isWindowFocused: Boolean

    /**
     * Size of the window's content container in pixels.
     */
    @ExperimentalComposeUiApi
    val containerSize: IntSize get() = IntSize.Zero
}

internal class WindowInfoImpl : WindowInfo {
    override var isWindowFocused: Boolean by mutableStateOf(false)

    @ExperimentalComposeUiApi
    override var containerSize: IntSize by mutableStateOf(IntSize.Zero)
}
