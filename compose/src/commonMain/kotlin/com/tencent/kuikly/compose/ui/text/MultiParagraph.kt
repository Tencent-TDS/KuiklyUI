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

package com.tencent.kuikly.compose.ui.text

import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.unit.Constraints

/**
 * Lays out and renders multiple paragraphs at once. Unlike [Paragraph], supports multiple
 * [ParagraphStyle]s in a given text.
 *
 * Kuikly 下文本由原生 RichTextView 渲染，行度量信息由 native 端（Android StaticLayout /
 * iOS / OHOS）通过 callMethod 桥接回填到本类的 [lineTops]/[lineBottoms] 与 [getBoundingBoxFn]。
 * 单位为 px（已在 commonMain 侧乘以 pageDensity 换算，与 DrawScope 坐标系一致）。
 */
class MultiParagraph(
    val lineCount: Int = 0,
    val placeholderRects: List<Rect?>,
    private val lineTops: FloatArray = FloatArray(0),
    private val lineBottoms: FloatArray = FloatArray(0),
    private val getBoundingBoxFn: ((Int) -> Rect)? = null,
) {

    /**
     * Returns the top y coordinate of the given line.
     */
    fun getLineTop(lineIndex: Int): Float = lineTops.getOrElse(lineIndex) { 0f }

    /**
     * Returns the bottom y coordinate of the given line.
     */
    fun getLineBottom(lineIndex: Int): Float = lineBottoms.getOrElse(lineIndex) { 0f }

    /**
     * Returns the bounding box of the character for given character offset.
     * 通过 [getBoundingBoxFn] 向 native 端查询（Android StaticLayout）。
     */
    fun getBoundingBox(offset: Int): Rect =
        getBoundingBoxFn?.invoke(offset) ?: Rect(0f, 0f, 0f, 0f)
}
