/*
 * Copyright 2024 The Android Open Source Project
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

package com.tencent.kuikly.compose.animation

import com.tencent.kuikly.compose.animation.core.AnimationVector4D
import com.tencent.kuikly.compose.animation.core.FiniteAnimationSpec
import com.tencent.kuikly.compose.animation.core.Spring
import com.tencent.kuikly.compose.animation.core.Transition
import com.tencent.kuikly.compose.animation.core.VisibilityThreshold
import com.tencent.kuikly.compose.animation.core.spring
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ui.geometry.Rect

private val DefaultBoundsAnimation = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Rect.VisibilityThreshold
)
