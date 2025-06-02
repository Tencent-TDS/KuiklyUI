/*
 * Copyright 2019 The Android Open Source Project
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

package com.tencent.kuikly.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.Layout
import com.tencent.kuikly.compose.ui.layout.Measurable
import com.tencent.kuikly.compose.ui.layout.MeasurePolicy
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.MeasureScope
import com.tencent.kuikly.compose.ui.unit.Constraints

/**
 * Component that represents an empty space layout, whose size can be defined using
 * [Modifier.width], [Modifier.height] and [Modifier.size] modifiers.
 *
 * @sample androidx.compose.foundation.layout.samples.SpacerExample
 *
 * @param modifier modifiers to set to this spacer
 */
@Composable
@NonRestartableComposable
fun Spacer(modifier: Modifier) {
    Layout(measurePolicy = SpacerMeasurePolicy, modifier = modifier)
}

private object SpacerMeasurePolicy : MeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return with(constraints) {
            val width = if (hasFixedWidth) maxWidth else 0
            val height = if (hasFixedHeight) maxHeight else 0
            layout(width, height) {}
        }
    }
}
