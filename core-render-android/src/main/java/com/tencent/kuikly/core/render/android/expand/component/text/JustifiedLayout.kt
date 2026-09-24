/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.render.android.expand.component.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.text.Layout
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import android.text.style.ReplacementSpan
import androidx.annotation.RequiresApi
import java.text.BreakIterator
import java.util.Locale

/**
 * 从左到右文本的两端对齐 [Layout]，最低 API 23。
 *
 * 换行、行高、省略号沿用 [staticLayout]；水平位置按字素簇重新分配，[ReplacementSpan] 视为一个字素。
 * 末行、硬换行行、带省略号的行、字素少于 2 的行不拉伸。水平查询与绘制使用同一套坐标。
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class JustifiedLayout(private val staticLayout: StaticLayout) : Layout(
    staticLayout.text, staticLayout.paint, staticLayout.width, staticLayout.alignment,
    staticLayout.spacingMultiplier, staticLayout.spacingAdd
) {

    // 仅测量尺寸时不会触发，首次绘制或水平查询时才按行计算
    private val lines: Array<Line> by lazy {
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        val measurePaint = TextPaint()
        Array(staticLayout.lineCount) { buildLine(it, iterator, measurePaint) }
    }
    private val drawPaint = TextPaint()
    private val clipRect = Rect()

    private fun buildLine(index: Int, iterator: BreakIterator, tp: TextPaint): Line {
        val spanned = text as? Spanned
        val start = staticLayout.getLineStart(index)
        val lineEnd = staticLayout.getLineEnd(index)
        val visibleEnd = staticLayout.getLineVisibleEnd(index)
        val ellipsized = staticLayout.getEllipsisCount(index) > 0
        val end = contentEnd(index, start, keepTrailingReplacements(start, visibleEnd, lineEnd))
        val length = end - start

        // 在行内按 MetricAffectingSpan 分段测量，保留字距调整，宽度与 StaticLayout 一致
        val charWidths = FloatArray(length)
        val runWidths = FloatArray(length)
        var m = start
        while (m < end) {
            val next = spanned?.nextSpanTransition(m, end, MetricAffectingSpan::class.java) ?: end
            applyMeasureState(tp, m, next)
            tp.getTextWidths(text, m, next, runWidths)
            System.arraycopy(runWidths, 0, charWidths, m - start, next - m)
            m = next
        }

        // 字素边界；ReplacementSpan 整体为一个字素
        val bounds = IntArray(length + 1)
        val widths = FloatArray(length)
        val replacements = arrayOfNulls<ReplacementSpan>(length)
        var count = 0
        var natural = 0f
        var broken = false
        bounds[0] = start
        if (length > 0) {
            iterator.setText(CharSequenceCharacterIterator(text, start, end))
        }
        var p = start
        while (p < end) {
            val replacement = spanned?.let { replacementAt(it, p, end) }
            val next: Int
            val width: Float
            if (replacement != null) {
                val spanEnd = spanned.getSpanEnd(replacement)
                // 跨行的 ReplacementSpan 无法整体摆放，该行退化为不拉伸
                broken = broken || spanned.getSpanStart(replacement) < p || spanEnd > end
                next = minOf(spanEnd, end)
                applyMeasureState(tp, p, next)
                width = replacement.getSize(tp, text, p, next, null).toFloat()
            } else {
                next = nextGrapheme(iterator, p, start, spanned?.nextSpanTransition(p, end, ReplacementSpan::class.java) ?: end)
                var sum = 0f
                for (c in p until next) sum += charWidths[c - start]
                width = sum
            }
            // 连字、合体字的宽度记在首个字素上，后续字素宽度为 0，并入前一个字素才不会被拆开
            if (replacement == null && width == 0f && count > 0 && replacements[count - 1] == null) {
                bounds[count] = next
            } else {
                replacements[count] = replacement
                widths[count] = width
                bounds[++count] = next
            }
            natural += width
            p = next
        }

        var hardBreak = false
        for (i in visibleEnd until lineEnd) {
            if (text[i] == '\n' || text[i] == '\r') hardBreak = true
        }
        val left = staticLayout.getParagraphLeft(index).toFloat()
        val justify = !broken && !hardBreak && !ellipsized && count >= 2 && index < staticLayout.lineCount - 1
        // 自然宽度略超内容宽度时 gap 为负，压缩到行内而不是溢出
        val gap = if (justify) (staticLayout.getParagraphRight(index) - left - natural) / (count - 1) else 0f
        val xs = FloatArray(count + 1)
        xs[0] = left
        for (k in 0 until count) {
            xs[k + 1] = xs[k] + widths[k] + if (k < count - 1) gap else 0f
        }

        // 绘制分段：CharacterStyle 变化处切分并对齐到字素边界，ReplacementSpan 单独成段
        val runs = ArrayList<Run>()
        var k = 0
        while (k < count) {
            val replacement = replacements[k]
            var e = k + 1
            val styleEnd = if (replacement == null) {
                val transition = spanned?.nextSpanTransition(bounds[k], end, CharacterStyle::class.java) ?: end
                while (e < count && bounds[e] < transition && replacements[e] == null) e++
                transition
            } else {
                bounds[e]
            }
            runs += Run(k, e, stylesOf(spanned, bounds[k], styleEnd, replacement != null), replacement)
            k = e
        }
        if (runs.isEmpty() && ellipsized) {
            runs += Run(0, 0, stylesOf(spanned, start, minOf(start + 1, text.length), false), null)
        }
        val ellipsisWidth = if (ellipsized) applyDrawState(tp, runs.last()).measureText(ELLIPSIS) else 0f
        return Line(bounds.copyOf(count + 1), xs, runs.toTypedArray(), justify && gap != 0f, ellipsisWidth)
    }

    /** 省略号之后的字符不绘制；省略号落在 ReplacementSpan 内部时截到其起点。 */
    private fun contentEnd(index: Int, start: Int, end: Int): Int {
        if (staticLayout.getEllipsisCount(index) <= 0) return end
        var cut = (start + staticLayout.getEllipsisStart(index)).coerceIn(start, end)
        val spanned = text as? Spanned ?: return cut
        for (span in spanned.getSpans(start, cut, ReplacementSpan::class.java)) {
            if (spanned.getSpanStart(span) < cut && spanned.getSpanEnd(span) > cut) cut = spanned.getSpanStart(span)
        }
        return cut.coerceAtLeast(start)
    }

    /**
     * getLineVisibleEnd 会去掉行尾空白，而占位文本默认是空格。
     * 把与可见末尾相连、且在换行符之前的 ReplacementSpan 补回，普通行尾空格仍然去掉。
     */
    private fun keepTrailingReplacements(start: Int, visibleEnd: Int, lineEnd: Int): Int {
        val spanned = text as? Spanned ?: return visibleEnd
        var limit = visibleEnd
        while (limit < lineEnd && text[limit] != '\n' && text[limit] != '\r') limit++
        if (limit == visibleEnd) return visibleEnd
        val spans = spanned.getSpans(start, limit, ReplacementSpan::class.java)
        var end = visibleEnd
        var expanded = true
        while (expanded) {
            expanded = false
            for (span in spans) {
                val spanStart = spanned.getSpanStart(span)
                val spanEnd = spanned.getSpanEnd(span)
                if (spanStart >= start && spanEnd <= limit && spanStart <= end && spanEnd > end) {
                    end = spanEnd
                    expanded = true
                }
            }
        }
        return end
    }

    /** 覆盖 [position] 的最长 ReplacementSpan。 */
    private fun replacementAt(spanned: Spanned, position: Int, end: Int): ReplacementSpan? {
        var best: ReplacementSpan? = null
        var bestLength = 0
        for (span in spanned.getSpans(position, minOf(position + 1, end), ReplacementSpan::class.java)) {
            val length = spanned.getSpanEnd(span) - spanned.getSpanStart(span)
            if (length > bestLength) {
                best = span
                bestLength = length
            }
        }
        return best
    }

    private fun nextGrapheme(iterator: BreakIterator, position: Int, lineStart: Int, limit: Int): Int {
        var next = iterator.following(position)
        while (next != BreakIterator.DONE && next < limit && joinsPrevious(next, lineStart)) {
            next = iterator.following(next)
        }
        return if (next == BreakIterator.DONE || next > limit) limit else next
    }

    /** 旧版 ICU（API 23-28）会拆开的 emoji 序列：ZWJ、变体选择符、肤色修饰符、标签序列、成对的区域指示符。 */
    private fun joinsPrevious(offset: Int, lineStart: Int): Boolean {
        val before = Character.codePointBefore(text, offset)
        val after = Character.codePointAt(text, offset)
        if (before == ZWJ || after == ZWJ || after in 0xFE00..0xFE0F || after in 0x1F3FB..0x1F3FF ||
            after in 0xE0020..0xE007F
        ) {
            return true
        }
        if (!isRegionalIndicator(before) || !isRegionalIndicator(after)) return false
        var indicators = 0
        var i = offset
        while (i > lineStart && isRegionalIndicator(Character.codePointBefore(text, i))) {
            indicators++
            i -= 2
        }
        return indicators % 2 == 1
    }

    private fun isRegionalIndicator(codePoint: Int) = codePoint in 0x1F1E6..0x1F1FF

    private fun applyMeasureState(tp: TextPaint, start: Int, end: Int) {
        tp.set(paint)
        (text as? Spanned)?.getSpans(start, end, MetricAffectingSpan::class.java)?.forEach {
            if (it !is ReplacementSpan) it.updateMeasureState(tp)
        }
    }

    /** 绘制态在绘制时才应用：渐变等样式依赖最终的 Layout 尺寸。 */
    private fun applyDrawState(tp: TextPaint, run: Run): TextPaint {
        tp.set(paint)
        for (style in run.styles) style.updateDrawState(tp)
        return tp
    }

    private fun stylesOf(spanned: Spanned?, start: Int, end: Int, replacement: Boolean): Array<CharacterStyle> {
        val spans = spanned?.getSpans(start, end, CharacterStyle::class.java) ?: return NO_STYLES
        // 与 TextLine 一致：ReplacementSpan 只接收 MetricAffectingSpan 的绘制态
        return spans.filter { it !is ReplacementSpan && (!replacement || it is MetricAffectingSpan) }.toTypedArray()
    }

    override fun draw(canvas: Canvas) {
        draw(canvas, null, null, 0)
    }

    override fun draw(canvas: Canvas, highlight: Path?, highlightPaint: Paint?, cursorOffsetVertical: Int) {
        if (highlight != null && highlightPaint != null) {
            canvas.translate(0f, cursorOffsetVertical.toFloat())
            canvas.drawPath(highlight, highlightPaint)
            canvas.translate(0f, -cursorOffsetVertical.toFloat())
        }
        if (!canvas.getClipBounds(clipRect)) return
        val lines = lines
        val first = getLineForVertical(maxOf(clipRect.top, 0))
        val last = getLineForVertical(clipRect.bottom)
        for (index in first..last) {
            drawLine(canvas, index, lines[index])
        }
    }

    private fun drawLine(canvas: Canvas, index: Int, line: Line) {
        val top = getLineTop(index)
        val bottom = getLineBottom(index)
        val baseline = getLineBaseline(index)
        val bounds = line.bounds
        val xs = line.xs
        for (run in line.runs) {
            val tp = applyDrawState(drawPaint, run)
            val x = xs[run.start]
            val withEllipsis = line.ellipsisWidth > 0f && run === line.runs.last()
            val replacement = run.replacement
            if (replacement != null) {
                replacement.draw(canvas, text, bounds[run.start], bounds[run.end], x, top, baseline, bottom, tp)
                if (withEllipsis) canvas.drawText(ELLIPSIS, xs[run.end], baseline.toFloat(), tp)
                continue
            }
            val right = if (withEllipsis) line.right else xs[run.end]
            val y = (baseline + tp.baselineShift).toFloat()
            if (tp.bgColor != 0) {
                val color = tp.color
                val style = tp.style
                tp.color = tp.bgColor
                tp.style = Paint.Style.FILL
                canvas.drawRect(x, top.toFloat(), right, bottom.toFloat(), tp)
                tp.color = color
                tp.style = style
            }
            // 下划线、删除线自行绘制，才能连续覆盖拉伸出的空白
            val underline = tp.isUnderlineText
            val strikeThru = tp.isStrikeThruText
            tp.isUnderlineText = false
            tp.isStrikeThruText = false
            if (!line.stretched) {
                val start = bounds[run.start]
                val end = bounds[run.end]
                if (start < end) canvas.drawTextRun(text, start, end, start, end, x, y, false, tp)
            } else {
                for (k in run.start until run.end) {
                    canvas.drawTextRun(text, bounds[k], bounds[k + 1], bounds[k], bounds[k + 1], xs[k], y, false, tp)
                }
            }
            if (withEllipsis) canvas.drawText(ELLIPSIS, xs[run.end], y, tp)
            drawDecorations(canvas, tp, underline, strikeThru, x, right, y)
        }
    }

    /** 几何与 TextLine 一致：API 29+ 用字体度量，更低版本用 Skia 默认比例。 */
    private fun drawDecorations(
        canvas: Canvas, tp: TextPaint, underline: Boolean, strikeThru: Boolean, left: Float, right: Float, y: Float
    ) {
        val underlineColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tp.underlineColor else 0
        if (left >= right || (!underline && !strikeThru && underlineColor == 0)) return
        val color = tp.color
        tp.style = Paint.Style.FILL
        tp.isAntiAlias = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 设置了 underlineColor 时，TextPaint.getUnderlineThickness 返回自定义粗细
            if (underlineColor != 0) {
                drawStroke(canvas, tp, underlineColor, tp.getUnderlinePosition(), tp.getUnderlineThickness(), left, right, y)
            }
            if (underline) {
                val thickness = maxOf(tp.getUnderlineThickness(), 1f)
                drawStroke(canvas, tp, color, tp.getUnderlinePosition(), thickness, left, right, y)
            }
            if (strikeThru) {
                val thickness = maxOf(tp.getStrikeThruThickness(), 1f)
                drawStroke(canvas, tp, color, tp.getStrikeThruPosition(), thickness, left, right, y)
            }
        } else {
            val thickness = maxOf(tp.textSize * UNDERLINE_THICKNESS, 1f)
            if (underline) drawStroke(canvas, tp, color, tp.textSize * UNDERLINE_OFFSET, thickness, left, right, y)
            if (strikeThru) drawStroke(canvas, tp, color, tp.textSize * STRIKE_THRU_OFFSET, thickness, left, right, y)
        }
    }

    private fun drawStroke(
        canvas: Canvas, tp: TextPaint, color: Int, position: Float, thickness: Float, left: Float, right: Float, y: Float
    ) {
        if (thickness <= 0f) return
        tp.color = color
        canvas.drawRect(left, y + position, right, y + position + thickness, tp)
    }

    override fun getPrimaryHorizontal(offset: Int): Float {
        val line = lines[getLineForOffset(offset)]
        return line.xs[line.floorCluster(offset)]
    }

    override fun getSecondaryHorizontal(offset: Int): Float = getPrimaryHorizontal(offset)

    override fun getOffsetForHorizontal(line: Int, horiz: Float): Int {
        val data = lines[line.coerceIn(0, lines.lastIndex)]
        val xs = data.xs
        val count = xs.size - 1
        if (count == 0) return data.bounds[0]
        var low = 0
        var high = count - 1
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (xs[mid] <= horiz) low = mid else high = mid - 1
        }
        return if (horiz - xs[low] <= xs[low + 1] - horiz) data.bounds[low] else data.bounds[low + 1]
    }

    override fun getOffsetToLeftOf(offset: Int): Int {
        val index = getLineForOffset(offset)
        val line = lines[index]
        val bounds = line.bounds
        return when {
            offset > bounds.last() -> bounds.last()
            offset > bounds[0] -> bounds[line.floorCluster(offset - 1)]
            index > 0 -> lines[index - 1].bounds.last()
            else -> offset
        }
    }

    override fun getOffsetToRightOf(offset: Int): Int {
        val index = getLineForOffset(offset)
        val line = lines[index]
        val bounds = line.bounds
        return if (offset < bounds.last()) bounds[line.floorCluster(offset) + 1] else getLineEnd(index)
    }

    override fun getLineLeft(line: Int): Float = lines[line].xs[0]

    override fun getLineRight(line: Int): Float = lines[line].right

    // 与 Layout 相同，包含首行缩进等段落左边距
    override fun getLineMax(line: Int): Float = lines[line].right

    override fun getLineWidth(line: Int): Float = lines[line].right - lines[line].xs[0]

    override fun getCursorPath(point: Int, dest: Path, editingBuffer: CharSequence?) {
        dest.reset()
        val line = getLineForOffset(point)
        val x = getPrimaryHorizontal(point)
        dest.moveTo(x, getLineTop(line).toFloat())
        dest.lineTo(x, getLineBottom(line).toFloat())
    }

    override fun getSelectionPath(start: Int, end: Int, dest: Path) {
        dest.reset()
        val selectionStart = minOf(start, end).coerceIn(0, text.length)
        val selectionEnd = maxOf(start, end).coerceIn(0, text.length)
        if (selectionStart == selectionEnd) return
        for (index in getLineForOffset(selectionStart)..getLineForOffset(selectionEnd)) {
            val line = lines[index]
            val from = maxOf(selectionStart, line.bounds[0])
            val to = minOf(selectionEnd, line.bounds.last())
            if (from >= to) continue
            dest.addRect(
                line.xs[line.floorCluster(from)], getLineTop(index).toFloat(),
                line.xs[line.floorCluster(to)], getLineBottom(index).toFloat(), Path.Direction.CW
            )
        }
    }

    override fun getLineCount(): Int = staticLayout.lineCount
    override fun getLineTop(line: Int): Int = staticLayout.getLineTop(line)
    override fun getLineDescent(line: Int): Int = staticLayout.getLineDescent(line)
    override fun getLineStart(line: Int): Int = staticLayout.getLineStart(line)
    override fun getParagraphDirection(line: Int): Int = DIR_LEFT_TO_RIGHT
    override fun getLineContainsTab(line: Int): Boolean = staticLayout.getLineContainsTab(line)
    override fun getLineDirections(line: Int): Directions = staticLayout.getLineDirections(line)
    override fun getTopPadding(): Int = staticLayout.topPadding
    override fun getBottomPadding(): Int = staticLayout.bottomPadding
    override fun getEllipsisStart(line: Int): Int = staticLayout.getEllipsisStart(line)
    override fun getEllipsisCount(line: Int): Int = staticLayout.getEllipsisCount(line)
    override fun getHeight(): Int = staticLayout.height

    /**
     * @param bounds 字素边界，首个为行首，末个为内容末尾（不含行尾空白和省略掉的字符）
     * @param xs 各边界的横坐标
     * @param stretched 是否插入了字素间距；未插入时按段整体绘制
     */
    private class Line(
        val bounds: IntArray,
        val xs: FloatArray,
        val runs: Array<Run>,
        val stretched: Boolean,
        val ellipsisWidth: Float
    ) {
        val right: Float get() = xs[xs.size - 1] + ellipsisWidth

        /** 不大于 [offset] 的最后一个字素边界下标。 */
        fun floorCluster(offset: Int): Int {
            var low = 0
            var high = bounds.size - 1
            while (low < high) {
                val mid = (low + high + 1) ushr 1
                if (bounds[mid] <= offset) low = mid else high = mid - 1
            }
            return low
        }
    }

    /** 同一绘制样式的连续字素 [start, end)。 */
    private class Run(val start: Int, val end: Int, val styles: Array<CharacterStyle>, val replacement: ReplacementSpan?)

    private companion object {
        const val ELLIPSIS = "\u2026"
        const val ZWJ = 0x200D
        // Skia 默认的装饰线比例
        const val UNDERLINE_OFFSET = 1f / 9f
        const val UNDERLINE_THICKNESS = 1f / 18f
        const val STRIKE_THRU_OFFSET = -6f / 21f
        val NO_STYLES = emptyArray<CharacterStyle>()
    }
}
