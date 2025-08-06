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

package com.tencent.kuikly.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import com.tencent.kuikly.compose.KuiklyApplier
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.foundation.layout.columnMeasurePolicy
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.drawBehind
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.materialize
import com.tencent.kuikly.compose.ui.node.ComposeUiNode
import com.tencent.kuikly.compose.ui.node.ComposeUiNode.Companion.SetModifier
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.core.views.CanvasView

/**
 * Component that allow you to specify an area on the screen and perform canvas drawing on this
 * area. You MUST specify size with modifier, whether with exact sizes via [Modifier.size] modifier,
 * or relative to parent, via [Modifier.fillMaxSize], [ColumnScope.weight], etc. If parent wraps
 * this child, only exact sizes must be specified.
 *
 * @sample androidx.compose.foundation.samples.CanvasSample
 * @param modifier mandatory modifier to specify size strategy for this composable
 * @param onDraw lambda that will be called to perform drawing. Note that this lambda will be called
 *   during draw stage, you have no access to composition scope, meaning that [Composable] function
 *   invocation inside it will result to runtime exception
 */
@Composable
fun Canvas(
    modifier: Modifier,
    onDraw: DrawScope.() -> Unit,
) {
    val compositeKeyHash = currentCompositeKeyHash
    val localMap = currentComposer.currentCompositionLocalMap

    val measurePolicy = columnMeasurePolicy(Arrangement.Top, Alignment.Start)
    val materialized = currentComposer.materialize(modifier.drawBehind(onDraw))

    ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
        factory = {
            val canvasView = CanvasView()
            KNode(canvasView) {}
        },
        update = {
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(localMap, ComposeUiNode.SetResolvedCompositionLocals)
            @OptIn(ExperimentalComposeUiApi::class)
            set(compositeKeyHash, ComposeUiNode.SetCompositeKeyHash)
            set(materialized, SetModifier)
        }
    )
}

/**
 * Component that allow you to specify an area on the screen and perform canvas drawing on this
 * area. You MUST specify size with modifier, whether with exact sizes via [Modifier.size] modifier,
 * or relative to parent, via [Modifier.fillMaxSize], [ColumnScope.weight], etc. If parent wraps
 * this child, only exact sizes must be specified.
 *
 * @sample androidx.compose.foundation.samples.CanvasPieChartSample
 * @param modifier mandatory modifier to specify size strategy for this composable
 * @param contentDescription text used by accessibility services to describe what this canvas
 *   represents. This should be provided unless the canvas is used for decorative purposes or as
 *   part of a larger entity already described in some other way. This text should be localized,
 *   such as by using [androidx.compose.ui.res.stringResource]
 * @param onDraw lambda that will be called to perform drawing. Note that this lambda will be called
 *   during draw stage, you have no access to composition scope, meaning that [Composable] function
 *   invocation inside it will result to runtime exception
 */
@Composable
inline fun Canvas(
    modifier: Modifier,
    contentDescription: String,
    noinline onDraw: DrawScope.() -> Unit
) {
    Canvas(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        onDraw = onDraw
    )
}
