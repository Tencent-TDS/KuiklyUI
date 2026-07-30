/*
 * Copyright 2022 The Android Open Source Project
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

package com.tencent.kuikly.compose.material3.internal

import com.tencent.kuikly.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.unit.dp

// TODO(https://github.com/JetBrains/compose-multiplatform/issues/3373) revert to expect get()
internal val WindowInsets.Companion.systemBarsForVisualComponents: WindowInsets
    @Composable get() = systemBarsForVisualComponents()

@Composable
internal fun WindowInsets.Companion.systemBarsForVisualComponents(): WindowInsets {
    val safeArea = LocalConfiguration.current.safeAreaInsets
    return WindowInsets(
        left = safeArea.left.dp,
        top = safeArea.top.dp,
        bottom = safeArea.bottom.dp,
        right = safeArea.right.dp
    )
}