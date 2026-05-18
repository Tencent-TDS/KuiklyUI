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

package com.tencent.kuikly.compose.ui.node

import com.tencent.kuikly.compose.gestures.KuiklyScrollInfo
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Matrix
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.core.base.DeclarativeBaseView

/**
 * 渲染相关属性的持有对象,用于优化属性访问性能
 * 避免通过 extProps Map 查找的开销
 */
internal class RenderProperties {
    var matrix: Matrix? = null
    var matrixChanged: Boolean = false
    var alpha: Float = 1f
    var measuredSize: IntSize = IntSize(0, 0)
    var borderRadius: FloatArray = FloatArray(4)
    var clip: Boolean = false
    var shadowElevation: Float = 0f
    var shadowColor: Color = Color.Transparent
    var shadowHasSet: Boolean = false
    var kuiklyScrollInfo: KuiklyScrollInfo? = null
    var clipPath: String = ""
}
