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
import android.graphics.text.PositionedGlyphs
import android.os.Build
import android.text.Layout
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextShaper
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import android.text.style.ReplacementSpan
import androidx.annotation.RequiresApi
import java.text.BreakIterator
import java.util.Locale

/**
 * 仅支持从左到右的两端对齐 [Layout]，最低 API 23。
 *
 * 换行、行高和行边界使用传入的 [StaticLayout]。
 * 水平位置按字素计算，不使用 [StaticLayout] 的水平坐标。不支持 RTL。
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class JustifiedLayout(
    text: CharSequence,
    paint: TextPaint,
    width: Int,
    alignment: Alignment = Alignment.ALIGN_NORMAL,
    spacingMult: Float = 1f,
    spacingAdd: Float = 0f,
    private val rightIndents: IntArray? = null,
    private val staticLayout: StaticLayout
) : Layout(
    text, paint, width, alignment, spacingMult, spacingAdd
) {

    companion object {
        private const val ELLIPSIS = "\u2026"
    }

    private val lineData: Array<LineData>

    init {
        require(width >= 0) {
            "width < 0"
        }
        lineData = buildLines()
    }

    private fun buildLines(): Array<LineData> {

        val count = staticLayout.lineCount

        val graphemeIterator = if (Build.VERSION.SDK_INT < 29) {
            BreakIterator.getCharacterInstance(Locale.ROOT)
        } else {
            null
        }

        return Array(count) { lineIndex ->

            val start = staticLayout.getLineStart(lineIndex)

            val visibleEnd = staticLayout.getLineVisibleEnd(lineIndex)

            val hardEnd = staticLayout.getLineEnd(lineIndex)

            /*
             * getLineVisibleEnd() drops trailing whitespace. Placeholder
             * text defaults to a single space, so a ReplacementSpan at the
             * line end would lose its width and be positioned on the
             * justified right edge.
             */
            val keptEnd = includeTrailingReplacementSpans(
                lineStart = start, visibleEnd = visibleEnd, hardEnd = hardEnd
            )

            val contentEnd = ellipsisContentEnd(
                lineIndex = lineIndex, lineStart = start, lineVisibleEnd = keptEnd
            )

            val clusters = Clusterizer.build(
                text = text,
                start = start,
                end = contentEnd,
                paint = paint,
                graphemeIterator = graphemeIterator
            )

            var naturalWidth = 0f

            for (cluster in clusters) {

                cluster.paint = obtainPaintForRange(
                    text = text, basePaint = paint, start = cluster.start, end = cluster.end
                )

                cluster.naturalWidth = measureCluster(
                    text = text,
                    clusterPaint = cluster.paint,
                    start = cluster.start,
                    end = cluster.end,
                    replacementSpan = cluster.replacementSpan
                )

                naturalWidth += cluster.naturalWidth
            }

            /*
             * A line ending in an explicit newline must not be justified.
             *
             * getLineVisibleEnd() also removes trailing whitespace, so
             * we explicitly inspect the hard line ending instead of merely
             * comparing hardEnd and visibleEnd.
             */
            val hardBreak = hasHardLineBreak(
                text = text, visibleEnd = visibleEnd, hardEnd = hardEnd
            )

            val isLastLine = lineIndex == count - 1

            /*
             * Stretch only inside the box StaticLayout used for breaking.
             * LeadingMarginSpan and right indents stay outside that box.
             */
            val contentLeft = staticLayout.getParagraphLeft(lineIndex).toFloat()

            val contentRight = staticLayout.getParagraphRight(lineIndex).toFloat() - lineIndent(rightIndents, lineIndex)

            val contentWidth = (contentRight - contentLeft).coerceAtLeast(0f)

            val shouldJustify =
                clusters.size >= 2 && !hardBreak && !isLastLine &&
                    staticLayout.getEllipsisCount(lineIndex) == 0 && naturalWidth < contentWidth

            val gapCount = if (shouldJustify) {
                clusters.size - 1
            } else {
                0
            }

            val extraGap = if (gapCount > 0) {
                (contentWidth - naturalWidth) / gapCount
            } else {
                0f
            }

            val visualWidth = naturalWidth + extraGap * gapCount

            val startX = contentLeft + resolveLineStartX(
                alignment = alignment, visualWidth = visualWidth, contentWidth = contentWidth
            )

            val positions = FloatArray(
                clusters.size + 1
            )

            var x = startX

            positions[0] = x

            for (index in clusters.indices) {

                val cluster = clusters[index]

                val extraAfter = if (shouldJustify && index < clusters.lastIndex) {
                    extraGap
                } else {
                    0f
                }

                x += cluster.naturalWidth
                x += extraAfter

                positions[index + 1] = x
            }

            val ellipsisWidth = measureEllipsisWidth(
                lineIndex = lineIndex, clusters = clusters
            )

            if (ellipsisWidth > 0f) {
                val textEnd = positions[positions.lastIndex]
                val ellipsisEnd = minOf(textEnd + ellipsisWidth, contentRight)
                positions[positions.lastIndex] = maxOf(ellipsisEnd, textEnd)
            }

            LineData(
                line = lineIndex,
                start = start,
                visibleEnd = contentEnd,
                clusters = clusters,
                positions = positions,
                ellipsisWidth = ellipsisWidth
            )
        }
    }

    /**
     * Ellipsized characters stay in the source line but must not be drawn.
     * Cut before a ReplacementSpan when the ellipsis lands inside it.
     */
    private fun ellipsisContentEnd(
        lineIndex: Int, lineStart: Int, lineVisibleEnd: Int
    ): Int {

        if (staticLayout.getEllipsisCount(lineIndex) <= 0) {
            return lineVisibleEnd
        }

        var end = (lineStart + staticLayout.getEllipsisStart(lineIndex)).coerceIn(
            lineStart, lineVisibleEnd
        )

        val spanned = text
        if (spanned !is Spanned || end <= lineStart) {
            return end
        }

        val spans = spanned.getSpans(
            lineStart, end, ReplacementSpan::class.java
        )

        for (span in spans) {

            val spanStart = spanned.getSpanStart(span)

            val spanEnd = spanned.getSpanEnd(span)

            if (spanStart < end && spanEnd > end) {
                end = minOf(end, spanStart)
            }
        }

        return end.coerceAtLeast(lineStart)
    }

    /**
     * Put back ReplacementSpans that [Layout.getLineVisibleEnd] trimmed
     * only because their placeholder text is trailing whitespace.
     *
     * A span is restored when it is contiguous with the visible end
     * (or already overlaps it) and stays on this line, before any newline.
     * Plain trailing spaces after the span stay trimmed.
     */
    private fun includeTrailingReplacementSpans(
        lineStart: Int, visibleEnd: Int, hardEnd: Int
    ): Int {

        val spanned = text as? Spanned ?: return visibleEnd

        var limit = hardEnd

        for (index in visibleEnd until hardEnd) {

            val ch = text[index]

            if (ch == '\n' || ch == '\r') {
                limit = index
                break
            }
        }

        var end = visibleEnd

        if (end >= limit) {
            return visibleEnd
        }

        val spans = spanned.getSpans(
            lineStart, limit, ReplacementSpan::class.java
        )

        var expanded = true

        while (expanded) {

            expanded = false

            for (span in spans) {

                val spanStart = spanned.getSpanStart(span)

                val spanEnd = spanned.getSpanEnd(span)

                if (spanStart < lineStart || spanEnd > limit) {
                    continue
                }

                if (spanEnd <= end || spanStart > end) {
                    continue
                }

                end = spanEnd

                expanded = true
            }
        }

        return end
    }

    private fun measureEllipsisWidth(
        lineIndex: Int, clusters: List<Cluster>
    ): Float {

        if (staticLayout.getEllipsisCount(lineIndex) <= 0) {
            return 0f
        }

        val ellipsisPaint = clusters.lastOrNull()?.paint ?: paint

        return ellipsisPaint.measureText(ELLIPSIS)
    }

    private fun hasHardLineBreak(
        text: CharSequence, visibleEnd: Int, hardEnd: Int
    ): Boolean {

        if (visibleEnd >= hardEnd) {
            return false
        }

        /*
         * StaticLayout normally stores the line terminator in hardEnd.
         *
         * We only regard CR/LF as a hard paragraph break.
         * Ordinary trailing spaces must not disable justification.
         */
        var index = hardEnd - 1

        while (index >= visibleEnd) {

            when (text[index]) {

                '\n', '\r' -> return true

                else -> index--
            }
        }

        return false
    }

    private fun lineIndent(indents: IntArray?, lineIndex: Int): Int {
        if (indents == null || lineIndex !in indents.indices) {
            return 0
        }
        return indents[lineIndex]
    }

    private fun resolveLineStartX(
        alignment: Alignment, visualWidth: Float, contentWidth: Float
    ): Float {

        return when (alignment) {

            Alignment.ALIGN_CENTER -> (contentWidth - visualWidth) * 0.5f

            Alignment.ALIGN_OPPOSITE -> contentWidth - visualWidth

            else -> 0f
        }
    }

    private fun measureCluster(
        text: CharSequence,
        clusterPaint: TextPaint,
        start: Int,
        end: Int,
        replacementSpan: ReplacementSpan?
    ): Float {

        if (start >= end) {
            return 0f
        }

        if (replacementSpan != null) {

            val fm = Paint.FontMetricsInt()

            clusterPaint.getFontMetricsInt(fm)

            return replacementSpan.getSize(
                    clusterPaint, text, start, end, fm
                ).toFloat()
        }

        /*
         * API 23+.
         *
         * Because this method measures one grapheme cluster at a time,
         * start/end/contextStart/contextEnd are identical.
         */
        return clusterPaint.getRunAdvance(
            text, start, end, start, end, false, end
        )
    }

    /**
     * Creates the effective paint for a text range.
     *
     * MetricAffectingSpan:
     *   updateMeasureState()
     *
     * CharacterStyle:
     *   updateDrawState()
     *
     * ReplacementSpan is deliberately excluded because it is measured
     * and drawn explicitly.
     */
    private fun obtainPaintForRange(
        text: CharSequence, basePaint: TextPaint, start: Int, end: Int
    ): TextPaint {

        val result = TextPaint()

        result.set(basePaint)

        if (text !is Spanned) {
            return result
        }

        val metricSpans = text.getSpans(
            start, end, MetricAffectingSpan::class.java
        )

        for (span in metricSpans) {

            if (span is ReplacementSpan) {
                continue
            }

            span.updateMeasureState(result)
        }

        val characterSpans = text.getSpans(
            start, end, CharacterStyle::class.java
        )

        for (span in characterSpans) {

            if (span is ReplacementSpan) {
                continue
            }

            span.updateDrawState(result)
        }

        return result
    }

    override fun draw(canvas: Canvas) {

        /*
         * Layout.draw() normally draws background first.
         *
         * We intentionally draw our custom text here. Background spans are
         * not part of the custom horizontal positioning system.
         */
        drawInternal(canvas)
    }

    override fun draw(
        canvas: Canvas,
        selectionHighlight: Path?,
        selectionHighlightPaint: Paint?,
        cursorOffsetVertical: Int
    ) {

        if (selectionHighlight != null && selectionHighlightPaint != null) {

            if (cursorOffsetVertical != 0) {
                canvas.save()

                canvas.translate(
                    0f, cursorOffsetVertical.toFloat()
                )

                canvas.drawPath(
                    selectionHighlight, selectionHighlightPaint
                )

                canvas.restore()

            } else {

                canvas.drawPath(
                    selectionHighlight, selectionHighlightPaint
                )
            }
        }

        drawInternal(canvas)
    }

    private fun drawInternal(
        canvas: Canvas
    ) {

        for (line in lineData) {
            drawLine(
                canvas, line
            )
        }
    }

    private fun drawLine(
        canvas: Canvas, line: LineData
    ) {

        val baseline = getLineBaseline(line.line)

        val top = getLineTop(line.line)

        val bottom = getLineBottom(line.line)

        for (index in line.clusters.indices) {

            val cluster = line.clusters[index]

            val x = line.positions[index]

            val replacement = cluster.replacementSpan

            if (replacement != null) {

                drawReplacementSpan(
                    canvas = canvas,
                    cluster = cluster,
                    x = x,
                    top = top,
                    baseline = baseline,
                    bottom = bottom
                )

            } else {

                drawTextCluster(
                    canvas = canvas, cluster = cluster, x = x, baseline = baseline
                )
            }
        }

        /*
         * API 31+ draws glyphs with Canvas.drawGlyphs(), which ignores
         * underline and strikethrough. Framework TextLine strokes those
         * decorations itself after shaping. API 23-30 still goes through
         * drawTextRun(), which paints the flags.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            drawApi31Decorations(
                canvas = canvas, line = line, baseline = baseline
            )
        }

        if (line.ellipsisWidth > 0f) {
            val ellipsisPaint = line.clusters.lastOrNull()?.paint ?: paint
            canvas.drawText(
                ELLIPSIS,
                line.positions.last() - line.ellipsisWidth,
                baseline.toFloat(),
                ellipsisPaint
            )
        }
    }

    private fun drawReplacementSpan(
        canvas: Canvas, cluster: Cluster, x: Float, top: Int, baseline: Int, bottom: Int
    ) {

        val span = cluster.replacementSpan ?: return

        span.draw(
            canvas, text, cluster.start, cluster.end, x, top, baseline, bottom, cluster.paint
        )
    }

    private fun drawTextCluster(
        canvas: Canvas, cluster: Cluster, x: Float, baseline: Int
    ) {

        if (cluster.start >= cluster.end) {
            return
        }

        val clusterPaint = cluster.paint

        if (Build.VERSION.SDK_INT >= 31) {

            Api31Renderer.draw(
                canvas = canvas,
                text = text,
                start = cluster.start,
                end = cluster.end,
                x = x,
                baseline = baseline.toFloat(),
                paint = clusterPaint
            )

        } else {

            drawTextClusterLegacy(
                canvas = canvas, cluster = cluster, x = x, baseline = baseline, paint = clusterPaint
            )
        }
    }

    /**
     * API 23 - 30.
     */
    private fun drawTextClusterLegacy(
        canvas: Canvas, cluster: Cluster, x: Float, baseline: Int, paint: TextPaint
    ) {

            canvas.drawTextRun(
            text,
            cluster.start,
            cluster.end,
            cluster.start,
            cluster.end,
            x,
            baseline.toFloat(),
            false,
            paint
        )
    }

    /**
     * Underline and strikethrough for the [Canvas.drawGlyphs] path.
     *
     * Stroke geometry matches framework [android.text.TextLine]:
     * top = baseline + position, height = thickness, fill rect.
     * Adjacent clusters that share the same decoration are merged so
     * justification gaps stay inside one continuous stroke.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun drawApi31Decorations(
        canvas: Canvas, line: LineData, baseline: Int
    ) {

        var index = 0

        while (index < line.clusters.size) {

            val cluster = line.clusters[index]

            if (cluster.replacementSpan != null) {
                index++
                continue
            }

            val clusterPaint = cluster.paint

            if (!hasTextDecoration(clusterPaint)) {
                index++
                continue
            }

            var end = index + 1

            while (end < line.clusters.size) {

                val next = line.clusters[end]

                if (next.replacementSpan != null) {
                    break
                }

                val nextPaint = next.paint

                if (!sameTextDecoration(clusterPaint, nextPaint)) {
                    break
                }

                end++
            }

            drawTextDecorations(
                canvas = canvas,
                paint = clusterPaint,
                left = line.positions[index],
                right = decorationRight(line, end - 1),
                baseline = baseline.toFloat()
            )

            index = end
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun decorationRight(
        line: LineData, index: Int
    ): Float {

        val cluster = line.clusters[index]

        /*
         * The trailing position of the last cluster is extended for the
         * ellipsis. Decorations stop at the glyph advance. Earlier clusters
         * include the justification gap that was inserted after them.
         */
        return if (index < line.clusters.lastIndex) {
            line.positions[index + 1]
        } else {
            line.positions[index] + cluster.naturalWidth
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasTextDecoration(paint: TextPaint): Boolean {
        return paint.isUnderlineText || paint.isStrikeThruText || paint.underlineColor != 0
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun sameTextDecoration(
        left: TextPaint, right: TextPaint
    ): Boolean {

        return left.isUnderlineText == right.isUnderlineText &&
            left.isStrikeThruText == right.isStrikeThruText &&
            left.underlineColor == right.underlineColor &&
            left.underlineThickness == right.underlineThickness &&
            left.color == right.color &&
            left.getUnderlinePosition() == right.getUnderlinePosition() &&
            left.getUnderlineThickness() == right.getUnderlineThickness() &&
            left.getStrikeThruPosition() == right.getStrikeThruPosition() &&
            left.getStrikeThruThickness() == right.getStrikeThruThickness()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun drawTextDecorations(
        canvas: Canvas, paint: TextPaint, left: Float, right: Float, baseline: Float
    ) {

        if (left >= right) {
            return
        }

        /*
         * TextPaint.underlineColor draws a custom stroke first.
         * Paint underline / strikethrough flags then draw the font stroke.
         * Font metrics come from getUnderlinePosition/Thickness.
         * TextPaint.underlineThickness is a separate custom-thickness field.
         */
        if (paint.underlineColor != 0) {
            drawDecorationStroke(
                canvas = canvas,
                paint = paint,
                color = paint.underlineColor,
                position = paint.getUnderlinePosition(),
                thickness = paint.underlineThickness,
                left = left,
                right = right,
                baseline = baseline
            )
        }

        if (paint.isUnderlineText) {
            drawDecorationStroke(
                canvas = canvas,
                paint = paint,
                color = paint.color,
                position = paint.getUnderlinePosition(),
                thickness = kotlin.math.max(paint.getUnderlineThickness(), 1f),
                left = left,
                right = right,
                baseline = baseline
            )
        }

        if (paint.isStrikeThruText) {
            drawDecorationStroke(
                canvas = canvas,
                paint = paint,
                color = paint.color,
                position = paint.getStrikeThruPosition(),
                thickness = kotlin.math.max(paint.getStrikeThruThickness(), 1f),
                left = left,
                right = right,
                baseline = baseline
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun drawDecorationStroke(
        canvas: Canvas,
        paint: TextPaint,
        color: Int,
        position: Float,
        thickness: Float,
        left: Float,
        right: Float,
        baseline: Float
    ) {

        if (thickness <= 0f) {
            return
        }

        val strokeTop = baseline + position

        val previousColor = paint.color

        val previousStyle = paint.style

        val previousAntiAlias = paint.isAntiAlias

        paint.style = Paint.Style.FILL

        paint.isAntiAlias = true

        paint.color = color

        canvas.drawRect(
            left, strokeTop, right, strokeTop + thickness, paint
        )

        paint.style = previousStyle

        paint.color = previousColor

        paint.isAntiAlias = previousAntiAlias
    }

    override fun getPrimaryHorizontal(
        offset: Int
    ): Float {

        val safeOffset = offset.coerceIn(
            0, text.length
        )

        val lineIndex = getLineForOffset(safeOffset)

        return getPrimaryHorizontalInLine(
            lineData[lineIndex], safeOffset
        )
    }

    override fun getSecondaryHorizontal(
        offset: Int
    ): Float {

        /*
         * LTR-only.
         */
        return getPrimaryHorizontal(offset)
    }

    private fun getPrimaryHorizontalInLine(
        line: LineData, offset: Int
    ): Float {

        if (line.clusters.isEmpty()) {
            return line.positions.firstOrNull() ?: 0f
        }

        if (offset <= line.start) {
            return line.positions.first()
        }

        if (offset >= line.visibleEnd) {
            return line.positions.last()
        }

        val index = line.findClusterForOffset(offset)

        if (index < 0) {
            return line.positions.first()
        }

        val cluster = line.clusters[index]

        return when {

            offset <= cluster.start -> line.positions[index]

            offset >= cluster.end -> line.positions[index + 1]

            else ->/*
                 * Inside grapheme cluster / ReplacementSpan.
                 *
                 * Cursor and horizontal mapping are atomic.
                 */
                line.positions[index]
        }
    }

    override fun getOffsetForHorizontal(
        line: Int, horiz: Float
    ): Int {

        if (lineData.isEmpty()) {
            return 0
        }

        val safeLine = line.coerceIn(
            0, lineData.lastIndex
        )

        val data = lineData[safeLine]

        if (data.clusters.isEmpty()) {
            return data.start
        }

        val clusters = data.clusters

        /*
         * Before the first cluster.
         */
        if (horiz <= data.positions.first()) {
            return clusters.first().start
        }

        /*
         * After the last cluster.
         */
        if (horiz >= data.positions.last()) {
            return clusters.last().end
        }

        /*
         * Find the cluster containing the horizontal coordinate.
         */
        var low = 0
        var high = clusters.lastIndex

        while (low <= high) {

            val mid = (low + high) ushr 1

            val left = data.positions[mid]

            val right = data.positions[mid + 1]

            when {

                horiz < left -> high = mid - 1

                horiz > right -> low = mid + 1

                else -> {

                    val distanceLeft = kotlin.math.abs(
                        horiz - left
                    )

                    val distanceRight = kotlin.math.abs(
                        horiz - right
                    )

                    return if (distanceLeft <= distanceRight) {
                        clusters[mid].start
                    } else {
                        clusters[mid].end
                    }
                }
            }
        }

        /*
         * This should normally be unreachable because the previous
         * boundary checks cover both ends.
         *
         * Still return a deterministic boundary.
         */
        val boundary = low.coerceIn(
            1, clusters.size - 1
        )

        return clusters[boundary - 1].end
    }

    override fun getCursorPath(
        point: Int, dest: Path, editingBuffer: CharSequence?
    ) {

        dest.reset()

        if (lineData.isEmpty()) {
            return
        }

        val safePoint = point.coerceIn(
            0, text.length
        )

        val lineIndex = getLineForOffset(safePoint)

        val line = lineData[lineIndex]

        val x = getPrimaryHorizontalInLine(
            line, safePoint
        )

        val top = getLineTop(lineIndex)

        val bottom = getLineBottom(lineIndex)

        dest.moveTo(
            x, top.toFloat()
        )

        dest.lineTo(
            x, bottom.toFloat()
        )
    }

    override fun getSelectionPath(
        start: Int, end: Int, dest: Path
    ) {

        dest.reset()

        if (lineData.isEmpty()) {
            return
        }

        var selectionStart = start.coerceIn(
            0, text.length
        )

        var selectionEnd = end.coerceIn(
            0, text.length
        )

        if (selectionStart > selectionEnd) {

            val tmp = selectionStart

            selectionStart = selectionEnd

            selectionEnd = tmp
        }

        if (selectionStart == selectionEnd) {
            return
        }

        val firstLine = getLineForOffset(selectionStart)

        val lastLine = getLineForOffset(selectionEnd)

        for (lineIndex in firstLine..lastLine) {

            val data = lineData[lineIndex]

            val lineStart = data.start

            val lineEnd = data.visibleEnd

            val selectedStart = maxOf(
                selectionStart, lineStart
            )

            val selectedEnd = minOf(
                selectionEnd, lineEnd
            )

            if (selectedStart >= selectedEnd) {
                continue
            }

            val x1 = getPrimaryHorizontalInLine(
                data, selectedStart
            )

            val x2 = getPrimaryHorizontalInLine(
                data, selectedEnd
            )

            val left = minOf(
                x1, x2
            )

            val right = maxOf(
                x1, x2
            )

            val top = getLineTop(lineIndex)

            val bottom = getLineBottom(lineIndex)

            dest.addRect(
                left, top.toFloat(), right, bottom.toFloat(), Path.Direction.CW
            )
        }
    }

    override fun getLineCount(): Int = staticLayout.lineCount

    override fun getLineTop(
        line: Int
    ): Int = staticLayout.getLineTop(line)

    override fun getLineDescent(
        line: Int
    ): Int = staticLayout.getLineDescent(line)

    override fun getLineStart(
        line: Int
    ): Int = staticLayout.getLineStart(line)

    override fun getParagraphDirection(
        line: Int
    ): Int = DIR_LEFT_TO_RIGHT

    override fun getEllipsisStart(
        line: Int
    ): Int = staticLayout.getEllipsisStart(line)

    override fun getEllipsisCount(
        line: Int
    ): Int = staticLayout.getEllipsisCount(line)

    override fun getLineContainsTab(
        line: Int
    ): Boolean = staticLayout.getLineContainsTab(line)

    override fun getLineDirections(
        line: Int
    ): Directions = staticLayout.getLineDirections(line)

    override fun getTopPadding(): Int = staticLayout.topPadding

    override fun getBottomPadding(): Int = staticLayout.bottomPadding

    override fun getHeight(): Int = staticLayout.height

    override fun getLineForVertical(
        vertical: Int
    ): Int = staticLayout.getLineForVertical(vertical)

    override fun getLineForOffset(
        offset: Int
    ): Int {

        if (lineData.isEmpty()) {
            return 0
        }

        val safeOffset = offset.coerceIn(
            0, text.length
        )

        return staticLayout.getLineForOffset(
            safeOffset
        )
    }

    override fun getLineLeft(
        line: Int
    ): Float {

        if (line !in lineData.indices) {
            return 0f
        }

        val positions = lineData[line].positions

        return positions.firstOrNull() ?: 0f
    }

    override fun getLineRight(
        line: Int
    ): Float {

        if (line !in lineData.indices) {
            return width.toFloat()
        }

        val positions = lineData[line].positions

        return positions.lastOrNull() ?: 0f
    }

    override fun getLineMax(
        line: Int
    ): Float {

        if (line !in lineData.indices) {
            return 0f
        }

        val data = lineData[line]

        if (data.positions.isEmpty()) {
            return 0f
        }

        return kotlin.math.abs(
            data.positions.last() - data.positions.first()
        )
    }

    private class LineData(
        val line: Int,
        val start: Int,
        val visibleEnd: Int,
        val clusters: List<Cluster>,
        val positions: FloatArray,
        val ellipsisWidth: Float
    ) {

        fun findClusterForOffset(
            offset: Int
        ): Int {

            if (clusters.isEmpty()) {
                return -1
            }

            if (offset <= clusters.first().start) {
                return 0
            }

            if (offset >= clusters.last().end) {
                return clusters.lastIndex
            }

            /*
             * Number of clusters is normally small, and this method is
             * primarily used for cursor/horizontal mapping.
             */
            for (index in clusters.indices) {

                val cluster = clusters[index]

                if (offset >= cluster.start && offset <= cluster.end) {
                    return index
                }
            }

            return clusters.lastIndex
        }
    }

    private class Cluster(
        val start: Int, val end: Int, val replacementSpan: ReplacementSpan?
    ) {

        var naturalWidth: Float = 0f

        lateinit var paint: TextPaint
    }

    private object Clusterizer {

        fun build(
            text: CharSequence,
            start: Int,
            end: Int,
            paint: TextPaint,
            graphemeIterator: BreakIterator?
        ): MutableList<Cluster> {

            val result = ArrayList<Cluster>()

            if (start >= end) {
                return result
            }

            var position = start

            while (position < end) {

                /*
                 * ReplacementSpan always wins over grapheme segmentation.
                 */
                val replacement = findReplacementSpan(
                    text = text, position = position, end = end
                )

                if (replacement != null) {

                    val spanned = text as Spanned

                    val spanStart = spanned.getSpanStart(
                        replacement
                    )

                    val spanEnd = spanned.getSpanEnd(
                        replacement
                    )

                    /*
                     * A ReplacementSpan must be atomic.
                     *
                     * StaticLayout normally keeps such spans together.
                     * If it nevertheless crosses this custom visual range,
                     * fail rather than silently drawing only part of it.
                     */
                    require(
                        spanStart >= start && spanEnd <= end
                    ) {
                        "ReplacementSpan crosses line boundary: " + "span=[$spanStart,$spanEnd), " + "line=[$start,$end)"
                    }

                    result += Cluster(
                        start = spanStart, end = spanEnd, replacementSpan = replacement
                    )

                    position = spanEnd

                    continue
                }

                val next = nextGraphemeBoundary(
                    text = text,
                    start = position,
                    end = end,
                    paint = paint,
                    graphemeIterator = graphemeIterator
                )

                val actualNext = when {

                    next <= position -> minOf(
                        position + 1, end
                    )

                    next > end -> end

                    else -> next
                }

                result += Cluster(
                    start = position, end = actualNext, replacementSpan = null
                )

                position = actualNext
            }

            return result
        }

        private fun findReplacementSpan(
            text: CharSequence, position: Int, end: Int
        ): ReplacementSpan? {

            if (text !is Spanned) {
                return null
            }

            if (position >= end) {
                return null
            }

            val spans = text.getSpans(
                position, minOf(
                    position + 1, end
                ), ReplacementSpan::class.java
            )

            if (spans.isEmpty()) {
                return null
            }

            /*
             * If multiple ReplacementSpans overlap, use the longest one.
             */
            var best = spans[0]

            var bestLength = text.getSpanEnd(best) - text.getSpanStart(best)

            for (index in 1 until spans.size) {

                val candidate = spans[index]

                val length = text.getSpanEnd(candidate) - text.getSpanStart(candidate)

                if (length > bestLength) {

                    best = candidate

                    bestLength = length
                }
            }

            return best
        }

        private fun nextGraphemeBoundary(
            text: CharSequence,
            start: Int,
            end: Int,
            paint: TextPaint,
            graphemeIterator: BreakIterator?
        ): Int {

            /*
             * API 29+:
             *
             * Paint.getTextRunCursor() uses Android's text shaping cursor
             * rules and avoids placing the cursor inside:
             *
             *   - surrogate pairs
             *   - combining sequences
             *   - conjuncts
             *   - reordering clusters
             */
            if (Build.VERSION.SDK_INT >= 29) {

                val result = paint.getTextRunCursor(
                    text, start, end, false, start, Paint.CURSOR_AFTER
                )

                if (result > start && result <= end) {
                    return result
                }
            }

            /*
             * API 23 - 28 fallback.
             *
             * CharSequenceCharacterIterator keeps absolute indices, so
             * following() still returns offsets into the original text.
             */
            val iterator = graphemeIterator ?: BreakIterator.getCharacterInstance(Locale.ROOT)
            iterator.setText(CharSequenceCharacterIterator(text, start, end))

            val next = iterator.following(start)

            return when {

                next == BreakIterator.DONE -> end

                next <= start -> minOf(
                    start + 1, end
                )

                next > end -> end

                else -> next
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31Renderer {

        fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            baseline: Float,
            paint: TextPaint
        ) {

            if (start >= end) {
                return
            }

            /*
             * The cluster has already had its spans resolved into `paint`.
             *
             * Passing a plain String prevents TextShaper from applying the
             * same CharacterStyle/MetricAffectingSpan a second time.
             */
            val value = text.subSequence(
                start, end
            ).toString()

            if (value.isEmpty()) {
                return
            }

            TextShaper.shapeText(
                value, 0, value.length, TextDirectionHeuristics.LTR, paint
            ) { _, _, glyphs, shapedPaint ->

                drawGlyphs(
                    canvas = canvas,
                    glyphs = glyphs,
                    originX = x,
                    originY = baseline,
                    paint = shapedPaint
                )
            }
        }

        private fun drawGlyphs(
            canvas: Canvas,
            glyphs: PositionedGlyphs,
            originX: Float,
            originY: Float,
            paint: TextPaint
        ) {

            val count = glyphs.glyphCount()

            if (count <= 0) {
                return
            }

            var groupStart = 0

            while (groupStart < count) {

                val font = glyphs.getFont(groupStart)

                var groupEnd = groupStart + 1

                while (groupEnd < count) {

                    /*
                     * Android's own TextShaper sample groups consecutive
                     * glyphs using the same Font.
                     */
                    if (glyphs.getFont(groupEnd) != font) {
                        break
                    }

                    groupEnd++
                }

                val groupCount = groupEnd - groupStart

                val glyphIds = IntArray(groupCount)

                val positions = FloatArray(
                    groupCount * 2
                )

                for (i in 0 until groupCount) {

                    val glyphIndex = groupStart + i

                    glyphIds[i] = glyphs.getGlyphId(
                        glyphIndex
                    )

                    positions[i * 2] = originX + glyphs.getGlyphX(
                        glyphIndex
                    )

                    positions[i * 2 + 1] = originY + glyphs.getGlyphY(
                        glyphIndex
                    )
                }

                canvas.drawGlyphs(
                    glyphIds, 0, positions, 0, groupCount, font, paint
                )

                groupStart = groupEnd
            }
        }
    }
}