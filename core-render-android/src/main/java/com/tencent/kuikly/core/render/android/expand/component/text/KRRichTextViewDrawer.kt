/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.text.Layout
import android.text.Spanned
import android.text.style.ReplacementSpan
import com.tencent.kuikly.core.render.android.expand.component.KRTextProps
import com.tencent.kuikly.core.render.android.expand.component.SelectionEdge
import com.tencent.kuikly.core.render.android.expand.component.SelectionType
import java.lang.ref.WeakReference
import java.text.BreakIterator
import java.util.Locale

private const val INVALID_OFFSET = -1

/**
 * 富文本绘制器，封装 [Layout]，用于富文本视图的测量与绘制。
 */
class KRRichTextViewDrawer(val textLayout: Layout) {
    interface Callback {
        fun invalidate()
    }

    private var callback: WeakReference<Callback>? = null

    private var selectionStart = -1
    private var selectionEnd = -1
    internal val hasSelection: Boolean get() = 0 <= selectionStart && selectionStart < selectionEnd

    private val wordIterator by lazy(LazyThreadSafetyMode.NONE) {
        WordIterator(textLayout.text, 0, textLayout.text.length, Locale.getDefault())
    }

    private val sentenceIterator by lazy(LazyThreadSafetyMode.NONE) {
        BreakIterator.getSentenceInstance(Locale.getDefault()).apply {
            text = CharSequenceCharacterIterator(textLayout.text)
        }
    }

    internal fun setCallback(callback: Callback?) {
        this.callback = if (callback != null) WeakReference(callback) else null
    }

    private fun invalidate() {
        callback?.get()?.invalidate()
    }

    /**
     * 将文本内容绘制到 [canvas]，对接到 [Layout.draw]。
     */
    fun draw(canvas: Canvas) {
        textLayout.draw(canvas)
    }

    internal fun setSelectionByCoordinate(
        x: Float,
        y: Float,
        type: SelectionType,
        force: Boolean
    ): Boolean {
        val layout = textLayout
        val size = layout.text.length
        if (size > 0) {
            val position = layout.getOffsetForPosition(x, y)
            if (0 <= position && position <= size) {
                val (start, end) = when (type) {
                    SelectionType.CHARACTER -> layout.expandSelectionToCharacter(position, x, y)

                    SelectionType.WORD -> layout.expandSelectionToWord(position, wordIterator, x, y)

                    SelectionType.SENTENCE -> layout.expandSelectionToSentence(position, sentenceIterator)

                    SelectionType.PARAGRAPH -> layout.expandSelectionToParagraph(position)
                }
                setTextSelection(start, end)
                return true
            }
        }
        setTextSelection(INVALID_OFFSET, INVALID_OFFSET)
        return false
    }

    internal fun setSelectionByCoordinates(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        force: Boolean
    ): Boolean {
        val layout = textLayout
        var pos1: Int
        var pos2: Int
        val size = layout.text.length
        if (size == 0) {
            pos1 = INVALID_OFFSET
            pos2 = INVALID_OFFSET
        } else {
            pos1 = layout.getOffsetForPosition(x1, y1)
            pos2 = layout.getOffsetForPosition(x2, y2)
            if (pos1 == pos2) {
                if (force) {
                    if (layout.shouldExpandSelectionBackward(x1, y1, x2, y2, pos2)) {
                        pos1 = layout.prevOffset(pos2)
                    } else {
                        pos2 = layout.nextOffset(pos1)
                    }
                } else {
                    pos1 = INVALID_OFFSET
                    pos2 = INVALID_OFFSET
                }
            } else if (pos1 > pos2) {
                val temp = pos1
                pos1 = pos2
                pos2 = temp
            }
        }
        setTextSelection(pos1, pos2)
        return hasSelection
    }

    internal fun setSelectAll(): Boolean {
        val size = textLayout.text.length
        if (size > 0) {
            setTextSelection(0, size)
            return true
        }
        return false
    }

    internal fun clearSelection() {
        setTextSelection(INVALID_OFFSET, INVALID_OFFSET)
    }

    private fun setTextSelection(start: Int, end: Int) {
        if (selectionStart != start || selectionEnd != end) {
            selectionStart = start
            selectionEnd = end
            invalidate()
        }
    }

    internal fun getStartSelectionEdge() = textLayout.getPositionForOffset(selectionStart)

    internal fun getEndSelectionEdge() = textLayout.getPositionForOffset(selectionEnd, true)

    internal fun getSelectionRect(dest: RectF): Boolean {
        return if (hasSelection) {
            val layout = textLayout
            val startline: Int = layout.getLineForOffset(selectionStart)
            val endline: Int = layout.getLineForOffset(selectionEnd)
            dest.top = layout.getLineTop(startline).toFloat()
            dest.bottom = layout.getLineBottom(endline).toFloat()
            if (startline == endline) {
                dest.left = layout.getPrimaryHorizontal(selectionStart)
                dest.right = layout.getPrimaryHorizontal(selectionEnd)
            } else {
                dest.left = 0f
                dest.right = layout.width.toFloat()
            }
            true
        } else {
            false
        }
    }

    internal fun getSelectionPath(dest: Path): Boolean {
        return if (hasSelection) {
            textLayout.getSelectionPath(selectionStart, selectionEnd, dest)
            true
        } else {
            false
        }
    }

    internal fun getSelectionText(): String? {
        return if (hasSelection) {
            textLayout.text.substring(selectionStart, selectionEnd)
        } else {
            null
        }
    }

    internal fun getPreSelectionText(): String? {
        return if (hasSelection && selectionStart > 0) {
            textLayout.text.substring(0, selectionStart)
        } else {
            null
        }
    }

    internal fun getPostSelectionText(): String? {
        val length = textLayout.text.length
        return if (hasSelection && selectionEnd < length) {
            textLayout.text.substring(selectionEnd, length)
        } else {
            null
        }
    }

    private companion object {

        private fun Layout.expandSelectionToCharacter(
            position: Int,
            x: Float,
            y: Float
        ): Pair<Int, Int> {
            val (cX, cY) = getPositionForOffset(position)
            return if (shouldExpandSelectionBackward(cX, cY, x, y, position)) {
                Pair(prevOffset(position), position)
            } else {
                Pair(position, nextOffset(position))
            }
        }

        private fun Layout.expandSelectionToWord(
            position: Int,
            wordIterator: WordIterator,
            x: Float,
            y: Float
        ): Pair<Int, Int> {
            var start = wordIterator.prevBoundary(position)
            start = if (wordIterator.isOnPunctuation(start)) {
                // On punctuation boundary or within group of punctuation, find punctuation start.
                wordIterator.getPunctuationBeginning(position)
            } else {
                // Not on a punctuation boundary, find the word start.
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(position)
            }

            if (start != BreakIterator.DONE) {
                var end = wordIterator.nextBoundary(position)
                end = if (wordIterator.isAfterPunctuation(end)) {
                    // On punctuation boundary or within group of punctuation, find punctuation end.
                    wordIterator.getPunctuationEnd(position)
                } else { // Not on a punctuation boundary, find the word end.
                    wordIterator.getNextWordEndOnTwoWordBoundary(position)
                }

                if (end != BreakIterator.DONE && start < end) {
                    return Pair(getUnitStart(start), getUnitEnd(end))
                }
            }

            return expandSelectionToCharacter(position, x, y)
        }

        private fun Layout.expandSelectionToSentence(
            position: Int,
            sentenceIterator: BreakIterator
        ): Pair<Int, Int> {
            val start = sentenceIterator.preceding(position).let { prev ->
                if (prev == BreakIterator.DONE) 0 else prev
            }

            val end = sentenceIterator.following(position).let { next ->
                if (next == BreakIterator.DONE) text.length else next
            }
            return Pair(getUnitStart(start), getUnitEnd(end))
        }

        private fun Layout.expandSelectionToParagraph(position: Int): Pair<Int, Int> {
            val text = text
            var end = position
            while (end < text.length) {
                val c = Character.codePointAt(text, end)
                end += Character.charCount(c)
                if (c == '\n'.toInt()) {
                    break
                }
            }
            var start = if (position < text.length) {
                position
            } else {
                position - Character.charCount(Character.codePointBefore(text, position))
            }
            while (start > 0) {
                val c = Character.codePointBefore(text, start)
                if (c == '\n'.toInt()) {
                    break
                }
                start -= Character.charCount(c)
            }
            return Pair(getUnitStart(start), getUnitEnd(end))
        }

        private fun Layout.getPositionForOffset(
            offset: Int,
            isEnd: Boolean = false
        ): SelectionEdge {
            var line: Int = getLineForOffset(offset)
            val x: Float
            if (isEnd && line > 0 && offset == getLineStart(line) && text[offset - 1] != '\n') {
                // end-offset is at the beginning of line, use end of previous line
                line -= 1
                x = if (getParagraphDirection(line) == Layout.DIR_LEFT_TO_RIGHT) {
                    width + 0.1f // add 0.1f to stay out of the previous character's bounds
                } else {
                    -0.1f
                }
            } else {
                x = getPrimaryHorizontal(offset)
            }
            val bottom: Float = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                getLineBottom(line, false)
            } else {
                getLineBottom(line)
            }).toFloat()
            val top = getLineTop(line).toFloat()
            return SelectionEdge(x, top, bottom)
        }

        private fun Layout.shouldExpandSelectionBackward(
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float,
            position: Int
        ): Boolean {
            val line = getLineForOffset(position)
            return (/*end of previous line*/line > 0 && y2 < getLineTop(line)) ||
                    (/*end of text*/position == text.length) ||
                    (/*left side and not line start*/x2 < x1 && position != getLineStart(line))
        }

        private fun Layout.getUnitStart(offset: Int): Int {
            if (offset <= 0) {
                return 0
            }
            val text = text
            // 检查是否在ReplacementSpan范围内
            if (text is Spanned) {
                val spans = text.getSpans(offset, offset, ReplacementSpan::class.java)
                if (spans.isNotEmpty()) {
                    var unitStart = offset
                    for (span in spans) {
                        val spanStart = text.getSpanStart(span)
                        val spanEnd = text.getSpanEnd(span)
                        if (spanStart < unitStart && offset < spanEnd) {
                            unitStart = spanStart
                        }
                    }
                    return unitStart
                }
            }
            // 检查是否是低位代理字符（emoji等）
            if (Character.isLowSurrogate(text[offset]) &&
                Character.isHighSurrogate(text[offset - 1])
            ) {
                return offset - 1
            }
            return offset
        }

        private fun Layout.getUnitEnd(offset: Int): Int {
            if (offset >= text.length) {
                return text.length
            }
            val text = text
            // 检查是否在ReplacementSpan范围内
            if (text is Spanned) {
                val spans = text.getSpans(offset, offset, ReplacementSpan::class.java)
                if (spans.isNotEmpty()) {
                    var unitEnd = offset
                    for (span in spans) {
                        val spanStart = text.getSpanStart(span)
                        val spanEnd = text.getSpanEnd(span)
                        if (spanStart < offset && unitEnd < spanEnd) {
                            unitEnd = spanEnd
                        }
                    }
                    return unitEnd
                }
            }
            // 检查是否是高位代理字符（emoji等）
            if (offset > 0 && Character.isLowSurrogate(text[offset]) &&
                Character.isHighSurrogate(text[offset - 1])
            ) {
                return offset + 1
            }
            return offset
        }

        private inline fun Layout.nextOffset(offset: Int) = getUnitEnd(offset + 1)

        private inline fun Layout.prevOffset(offset: Int) = getUnitStart(offset - 1)

        private fun Layout.getOffsetForPosition(x: Float, y: Float): Int {
            val line: Int = getLineForVertical(y.toInt())
            if ((x > width && getParagraphDirection(line) == Layout.DIR_LEFT_TO_RIGHT) ||
                (x < 0 && getParagraphDirection(line) == Layout.DIR_RIGHT_TO_LEFT)) {
                return getLineEnd(line)
            }
            return getOffsetForHorizontal(line, x)
        }

    }

}