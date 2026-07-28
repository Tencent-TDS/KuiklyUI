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
 * 行度量数据（单位 px，已在 commonMain 侧乘以 pageDensity 换算，与 DrawScope 坐标系一致）。
 * 由 TextStringRichNode 解析 native 桥接字符串（"N top0 bottom0 start0 end0 ..."）后构造，
 * 经 [MultiParagraph] 的 lineMetricsFn 惰性回填。
 *
 * 可见性：随 [MultiParagraph]（public）暴露为 public；实际只在 compose 模块内部构造与消费，
 * 无对外 API 用途。
 */
class LineMetrics(
    val lineCount: Int,
    val lineTops: FloatArray,
    val lineBottoms: FloatArray,
    val lineStarts: IntArray,
    val lineEnds: IntArray,
)

/**
 * Lays out and renders multiple paragraphs at once. Unlike [Paragraph], supports multiple
 * [ParagraphStyle]s in a given text.
 *
 * Kuikly 下文本由原生 RichTextView 渲染，行度量信息由 native 端（Android StaticLayout /
 * iOS / OHOS）通过 callMethod 桥接回填。
 *
 * 行度量采用 lazy 设计：measure 热路径不触发桥调用，[lineMetricsFn] 仅在
 * getLineTop / getLineBottom / getLineStart / getLineEnd / lineCount 首次被读取时
 * 调用一次并缓存，业务不使用行度量时零桥开销。与 [getBoundingBoxFn] 的按需查询同思路。
 */
class MultiParagraph(
    private val initialLineCount: Int = 0,
    val placeholderRects: List<Rect?>,
    private val lineMetricsFn: (() -> LineMetrics)? = null,
    private val getBoundingBoxFn: ((Int) -> Rect)? = null,
) {

    /**
     * 行度量惰性缓存：首次读取时触发 [lineMetricsFn]（一次跨端桥调用），之后命中缓存。
     * measure 与后续读取同在 UI 线程，lazy 默认同步模式仅首次有加锁开销。
     */
    private val lineMetrics: LineMetrics? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        lineMetricsFn?.invoke()
    }

    /**
     * 文本行数。提供 [lineMetricsFn] 时取自 native 回填值，否则取构造传入值
     *（如 CoreTextField 等无行度量场景）。
     */
    val lineCount: Int get() = lineMetrics?.lineCount ?: initialLineCount

    /**
     * Returns the top y coordinate of the given line.
     */
    fun getLineTop(lineIndex: Int): Float = lineMetrics?.lineTops?.getOrElse(lineIndex) { 0f } ?: 0f

    /**
     * Returns the bottom y coordinate of the given line.
     */
    fun getLineBottom(lineIndex: Int): Float = lineMetrics?.lineBottoms?.getOrElse(lineIndex) { 0f } ?: 0f

    /**
     * Returns the start offset of the given line, inclusive.
     */
    fun getLineStart(lineIndex: Int): Int = lineMetrics?.lineStarts?.getOrElse(lineIndex) { 0 } ?: 0

    /**
     * Returns the end offset of the given line, exclusive.
     * 当前 Kuikly 仅回填 logical line end，暂不区分 visibleEnd。
     *
     * 当 lineEnds 缺失对应行（旧格式 native 只回填 top/bottom、未带 start/end 的兼容路径）时走
     * getOrElse fallback：非最后一行用下一行的 start（exclusive 语义下 line[i].end == line[i+1].start）；
     * 最后一行找不到下一行，只能退化为 getLineStart(lineIndex)，即 end == start（空区间）。
     * 这是旧格式兼容路径的有意退化，非 bug，请勿改为其它 fallback。
     */
    fun getLineEnd(lineIndex: Int, visibleEnd: Boolean = false): Int =
        lineMetrics?.lineEnds?.getOrElse(lineIndex) {
            if (lineIndex < lineCount - 1) getLineStart(lineIndex + 1) else getLineStart(lineIndex)
        } ?: 0

    /**
     * Returns the bounding box of the character for given character offset.
     * 通过 [getBoundingBoxFn] 向 native 端查询（Android / iOS / OHOS 原生文本布局）。
     */
    fun getBoundingBox(offset: Int): Rect =
        getBoundingBoxFn?.invoke(offset) ?: Rect(0f, 0f, 0f, 0f)
}
