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

package com.tencent.kuikly.compose.views

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.UiComposable
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.core.views.HoverView


@UiComposable
@Composable
internal fun StickHeader(
    content: @Composable @UiComposable () -> Unit,
    modifier: Modifier = Modifier,
    hoverMarginTop: Float = 0f
) {
    MakeKuiklyComposeNode<HoverView>(
        factory = {
            HoverView()
        },
        modifier = modifier,
        viewInit = { },
        viewUpdate = {
            it.getViewAttr().run {
                hoverMarginTop(hoverMarginTop)
            }
        },
        content = content
    )
}
