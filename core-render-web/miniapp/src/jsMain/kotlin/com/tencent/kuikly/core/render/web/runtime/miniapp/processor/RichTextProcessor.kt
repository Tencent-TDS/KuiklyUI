package com.tencent.kuikly.core.render.web.runtime.miniapp.processor

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.clear
import com.tencent.kuikly.core.render.web.collection.array.get
import com.tencent.kuikly.core.render.web.expand.components.KRRichTextView
import com.tencent.kuikly.core.render.web.expand.components.RichTextSpan
import com.tencent.kuikly.core.render.web.ktx.SizeF
import com.tencent.kuikly.core.render.web.const.KRCssConst.TEXT_SHADOW
import com.tencent.kuikly.core.render.web.ktx.height
import com.tencent.kuikly.core.render.web.ktx.pxToFloat
import com.tencent.kuikly.core.render.web.ktx.toNumberFloat
import com.tencent.kuikly.core.render.web.ktx.toRgbColor
import com.tencent.kuikly.core.render.web.ktx.width
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.processor.IRichTextProcessor
import com.tencent.kuikly.core.render.web.processor.TextMeasureCache
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal.isIOS
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.RenderConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniImageElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniSpanElement
import com.tencent.kuikly.core.render.web.utils.Log
import kotlinx.dom.clear
import org.w3c.dom.HTMLElement
import kotlin.js.json
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

/**
 * mini app text process object
 */
object RichTextProcessor : IRichTextProcessor {
    // Fixed android width ratio magic value, temporarily set to 1.05,
    // because Android real machine canvas measurement width result is smaller
    private const val WIDTH_RATIO_MAGIC = 1.05f

    // Default font weight
    private const val FONT_WEIGHT_DEFAULT = 400

    // Prevent additional width added due to special situation during text rendering
    private const val TEXT_WIDTH_DEFAULT = 0.5f

    // When business does not specify a line-height, use this factor multiplied
    // by font-size to approximate the browser's default "normal" line-height.
    // This is used as a strut fallback for lines that only contain inline
    // placeholders (e.g. images), so the measured height matches the real
    // rendered height of the `rich-text` host element.
    private const val LINE_HEIGHT_FACTOR = 1.2f
    /**
     * Real rendered line-height factor for an emoji-bearing line.
     *
     * Pure-emoji lines (and lines containing emoji clusters) are drawn taller
     * than the canvas `fontBoundingBox*` metrics imply, because emoji glyphs
     * carry larger ascent/descent than the surrounding latin font. Using only
     * `fontSize * 1.2` per line under-estimates each line's height, so when a
     * long emoji string auto-wraps to N lines the cumulative shortfall clips
     * the bottom of the last line.
     */
    private const val EMOJI_LINE_HEIGHT_FACTOR = 1.4f

    // Fallback font size when the container has no explicit font-size set.
    private const val DEFAULT_FONT_SIZE = 16f

    // Emoji glyphs are usually rendered at ~1.0em in real <text>/<rich-text>
    // components, but the offscreen canvas measureText() returns 0 (or a
    // tofu-box width) because the canvas font does NOT contain emoji glyphs.
    // Use this factor as a per-grapheme fallback width so that the measured
    // line width is close to the real rendered width and the last
    // characters don't get truncated when the line contains emojis.
    /**
     * Approximate advance-width of one emoji grapheme cluster, expressed as a
     * multiple of `fontSize`. Real-world emoji clusters render slightly wider
     * than `fontSize` (square glyph + small inter-glyph padding), so a 1.0
     * factor consistently under-estimates the line width, which in turn
     * causes `safeLineCount` to report N-1 lines for a long pure-emoji string
     * that the renderer actually wraps to N lines, clipping the last line.
     */
    private const val EMOJI_WIDTH_FACTOR = 1.0f

    /**
     * Compensation factor for iOS real-device text width measurement.
     *
     * Background: mini-program OffscreenCanvas always measures text with a
     * generic `sans-serif` font, but the real <text> element on a physical
     * iOS device falls back to PingFang SC, whose glyphs are typically
     * ~5-7% wider than the canvas-measured ones at the same size/weight.
     *
     * If we hand the un-compensated value to the layout engine, the parent
     * container reserves slightly less width than what PingFang actually
     * needs, and on iOS the <text> renderer then DROPS the entire trailing
     * word instead of wrapping (e.g. "Change Theme" → "Change").
     *
     * 1.06 leaves ~1px slack at fontSize=14px and grows linearly with the
     * font size, which empirically matches what we observe on iOS 17/18.
     * Keep it iOS-real-device only — Android (Roboto) and the WeChat
     * DevTools simulator (desktop fonts) match canvas measurement well
     * and do not need any compensation.
     */
    private const val IOS_REAL_FONT_WIDTH_FACTOR = 1.06f

    // Threshold to trigger an extra-line guard: when the geometric ratio
    // (totalWidth / constraintWidth) has a fractional part close to an
    // integer (e.g. 1.92, 2.95), real word-break / break-word may push
    // one more glyph onto a new line, while ceil() would still treat it as
    // the same line. Reserving one extra line at this boundary prevents
    // the multi-line height from being underestimated.
    private const val LINE_FRACTION_GUARD = 0.85f

    // mini app measure text canvas context
    private var measureTextCtx: dynamic = null
    
    // OffscreenCanvas object reference
    private var measureCanvas: dynamic = null
    
    // Cache last used font string to avoid redundant font setting
    private var lastCanvasFont: String = ""

    // Rich text placeholder attribute setting
    private const val PLACEHOLDER_WIDTH = "placeholderWidth"
    private const val PLACEHOLDER_HEIGHT = "placeholderHeight"
    private const val COLOR = "color"
    private const val FONT_SIZE = "fontSize"
    private const val TEXT_DECORATION = "textDecoration"
    private const val FONT_WEIGHT = "fontWeight"
    private const val FONT_STYLE = "fontStyle"
    private const val FONT_FAMILY = "fontFamily"
    private const val LETTER_SPACING = "letterSpacing"
    private const val STROKE_WIDTH = "strokeWidth"
    private const val STROKE_COLOR = "strokeColor"
    private const val FONT_VARIANT = "fontVariant"
    private const val HEAD_INDENT = "headIndent"
    private const val LINE_HEIGHT = "lineHeight"

    // Set placeholder image delay time
    private const val IMAGE_SPAN_DELAY = 100

    /**
     * Reset the offscreen canvas context and font cache after custom font loaded.
     * This forces the next measureTextWidth call to re-create the canvas context
     * and re-set the font, which may pick up the newly loaded custom font.
     */
    fun resetMeasureContext() {
        measureTextCtx = null
        measureCanvas = null
        lastCanvasFont = ""
    }

    /**
     * Set webkit multi-line text style
     */
    private fun setWebkitMultiLineStyle(lines: Int = 0, ele: HTMLElement)  {
        ele.style.display = "-webkit-box"
        ele.style.whiteSpace = "pre-wrap"
        ele.style.asDynamic().webkitBoxOrient = "vertical"
        ele.style.asDynamic().webkitLineClamp = lines.toString()
    }

    /**
     * Clear webkit multi-line text style
     */
    private fun clearWebkitMultiLineStyle(ele: HTMLElement) {
        ele.style.display = ""
        ele.style.whiteSpace = "pre-wrap"
        ele.style.asDynamic().webkitLineClamp = ""
        ele.style.asDynamic().webkitBoxOrient = ""
    }

    /**
     * Set multi-line text style
     */
    private fun setMultiLineStyle(lines: Int = 0, view: KRRichTextView) {
        if (view.isRichText && view.richTextSpanList.length > 0) {
            // Rich text style string setting
            if (lines > 1) {
                // If greater than one line, then process, otherwise, empty
                setWebkitMultiLineStyle(lines, view.ele)
            } else {
                // Clear multi-line style
                clearWebkitMultiLineStyle(view.ele)
            }
        } else {
            // Plain text, directly set multi-line style properties
            if (lines > 0) {
                setWebkitMultiLineStyle(lines, view.ele)
            } else {
                // Clear multi-line style
                clearWebkitMultiLineStyle(view.ele)
            }
        }
    }

    /**
     * get global single offscreen canvas context
     */
    private fun getCanvasContext(): dynamic {
        if (measureTextCtx == null) {
            measureCanvas = NativeApi.plat.createOffscreenCanvas(json("type" to "2d"))
            measureTextCtx = measureCanvas.getContext("2d")
        }
        return measureTextCtx
    }

    /**
     * Get default font family
     */
    private fun getDefaultFontFamily(): String {
        // iOS: 'San Francisco', 'PingFang SC', sans-serif
        // San Francisco for English, PingFang SC for Chinese.
        // Android: Roboto, 'Noto Sans CJK SC', sans-serif
        // Roboto for English, Noto Sans CJK SC for Chinese.
        // sans-serif: Fallback.
        if (MiniGlobal.isDevTools) {
            // Simulator forces the use of "sans-serif"
            return "sans-serif"
        }
        return if (isIOS)
            RenderConst.IOS_TEXT_FONT
        else
            RenderConst.ANDROID_TEXT_FONT
    }

    /**
     * measure given text width by canvas
     */
    private fun measureTextWidth(
        text: String,
        fontSize: dynamic,
        fontWeight: Int,
        fontFamily: String,
        fontStyle: String = ""
    ): dynamic {
        val ctx = getCanvasContext()
        var usedFontFamily = fontFamily
        if (fontFamily.isEmpty()) {
            usedFontFamily = getDefaultFontFamily()
        }
        val fontStr = "$fontStyle $fontWeight ${fontSize}px $usedFontFamily"
        
        // Only set font if it changed to avoid redundant operations
        if (fontStr != lastCanvasFont) {
            ctx.font = fontStr
            lastCanvasFont = fontStr
        }
        
        return ctx.measureText(text)
    }

    /**
     * Detect whether a code unit is part of an emoji / pictographic /
     * symbol sequence that the offscreen canvas usually mis-measures.
     *
     * The offscreen canvas font (PingFang / Roboto / sans-serif) does not
     * contain color emoji glyphs, so `ctx.measureText` returns either 0 or
     * a tofu-box width for them. We have to detect them and apply a
     * per-grapheme fallback width.
     *
     * Detection covers:
     *   - Surrogate pair leading code unit (U+D800..U+DBFF) — almost all
     *     SMP characters used as emoji (😀, 👨, 🏳 ...).
     *   - Variation Selector-16 (U+FE0F).
     *   - Zero-Width Joiner (U+200D) used to glue emoji sequences.
     *   - BMP misc-symbol/pictograph ranges that are commonly emoji:
     *     U+2600..U+27BF (☀,★,⚡,✈ ...), U+2300..U+23FF, U+2B00..U+2BFF,
     *     U+3030, U+303D, U+3297, U+3299.
     */
    private fun isEmojiCodeUnit(code: Int): Boolean {
        // High surrogate — start of a 2-code-unit SMP character
        if (code in 0xD800..0xDBFF) return true
        // Low surrogate — paired with a preceding high surrogate
        if (code in 0xDC00..0xDFFF) return true
        // Variation Selector-16, ZWJ
        if (code == 0xFE0F || code == 0x200D) return true
        // Common BMP pictograph / dingbat / symbol blocks
        if (code in 0x2600..0x27BF) return true
        if (code in 0x2300..0x23FF) return true
        if (code in 0x2B00..0x2BFF) return true
        if (code == 0x3030 || code == 0x303D || code == 0x3297 || code == 0x3299) return true
        return false
    }

    /**
     * Quick scan: does the text contain any emoji-ish code unit?
     * When false, we keep the original fast canvas measure path.
     */
    private fun containsEmoji(text: String): Boolean {
        for (i in 0 until text.length) {
            if (isEmojiCodeUnit(text.asDynamic().charCodeAt(i).unsafeCast<Int>())) {
                return true
            }
        }
        return false
    }

    /**
     * Measure a single line that may contain emoji glyphs.
     *
     * Strategy: split the line into runs — each run is either a pure
     * non-emoji string (measured by canvas as usual) or a single emoji
     * grapheme cluster (estimated as `fontSize * EMOJI_WIDTH_FACTOR`).
     *
     * Returns a Triple(width, height, graphemeCount):
     *   - graphemeCount counts emoji as 1 each (instead of UTF-16 length
     *     2~8) so the caller can apply letter-spacing accurately.
     */
    private fun measureMixedLine(
        text: String,
        fontSize: Float,
        fontWeight: Int,
        fontFamily: String,
        fontStyle: String,
    ): dynamic {
        var totalWidth = 0f
        var maxHeight = 0f
        var graphemeCount = 0
        var nonEmojiBuf = ""
        // Conservative fallback line height when canvas metrics are unusable
        // (e.g. WeChat mini-program returns NaN for fontBoundingBox*).
        val fallbackLineHeight = (fontSize * 1.2f).let {
            if (it.isFinite() && it > 0f) it else (DEFAULT_FONT_SIZE * 1.2f)
        }

        // A safe number reader: returns the number value only when it is a
        // finite real number; otherwise returns the supplied default. This
        // protects against `undefined` / `NaN` / `Infinity` produced by some
        // mini-program canvas implementations.
        fun safeNum(v: dynamic, default: Float): Float {
            if (jsTypeOf(v) != "number") return default
            val f = v.unsafeCast<Float>()
            return if (f.isFinite()) f else default
        }

        fun flushNonEmoji() {
            if (nonEmojiBuf.isEmpty()) return
            val metrics = measureTextWidth(
                nonEmojiBuf, fontSize.toInt(), fontWeight, fontFamily, fontStyle
            )
            // Width: prefer actualBoundingBox, fall back to metrics.width,
            // both must be finite numbers.
            val abl = safeNum(metrics.actualBoundingBoxLeft, Float.NaN)
            val abr = safeNum(metrics.actualBoundingBoxRight, Float.NaN)
            val boxW = if (abl.isFinite() && abr.isFinite()) abl + abr else 0f
            val mW = safeNum(metrics.width, 0f)
            var w = max(boxW, mW)
            // Last-resort fallback when canvas returns nothing usable.
            if (!w.isFinite() || w < 0f) w = 0f

            // Height: prefer fontBoundingBox metrics, fall back to fontSize * 1.2.
            val asc = safeNum(metrics.fontBoundingBoxAscent, Float.NaN)
            val desc = safeNum(metrics.fontBoundingBoxDescent, Float.NaN)
            var h = if (asc.isFinite() && desc.isFinite()) asc + desc + 1f
                    else fallbackLineHeight
            if (!h.isFinite() || h <= 0f) h = fallbackLineHeight

            totalWidth += w
            if (h > maxHeight) maxHeight = h
            graphemeCount += nonEmojiBuf.length
            nonEmojiBuf = ""
        }

        var i = 0
        while (i < text.length) {
            val code = text.asDynamic().charCodeAt(i).unsafeCast<Int>()
            // Try to detect a full emoji grapheme cluster starting at i.
            // A cluster may be:
            //   high-surrogate + low-surrogate (+ VS16)? (+ ZWJ + ...)*
            // For simplicity we walk forward while the next code unit is a
            // continuation (low-surrogate, VS16, ZWJ, or another emoji code
            // unit). This treats e.g. 👨‍👩‍👧 as one grapheme.
            val isEmojiStart = isEmojiCodeUnit(code)
            if (isEmojiStart) {
                flushNonEmoji()
                // Walk to the end of this grapheme cluster
                var end = i
                // Consume current code unit
                end += 1
                // Consume the low-surrogate paired with a high-surrogate
                if (code in 0xD800..0xDBFF && end < text.length) {
                    val nextCode = text.asDynamic().charCodeAt(end).unsafeCast<Int>()
                    if (nextCode in 0xDC00..0xDFFF) {
                        end += 1
                    }
                }
                // Consume any trailing VS16 / ZWJ + next emoji loop
                while (end < text.length) {
                    val c = text.asDynamic().charCodeAt(end).unsafeCast<Int>()
                    if (c == 0xFE0F) {
                        end += 1
                        continue
                    }
                    if (c == 0x200D && end + 1 < text.length) {
                        // ZWJ followed by another emoji-starting code unit
                        val c2 = text.asDynamic().charCodeAt(end + 1).unsafeCast<Int>()
                        if (isEmojiCodeUnit(c2)) {
                            end += 1 // consume ZWJ, the emoji itself will be
                            continue   // handled in the next outer iteration
                        }
                    }
                    break
                }
                // Add fallback width for this whole cluster (1 grapheme).
                val clusterW = (fontSize * EMOJI_WIDTH_FACTOR).let {
                    if (it.isFinite() && it > 0f) it else (DEFAULT_FONT_SIZE * EMOJI_WIDTH_FACTOR)
                }
                totalWidth += clusterW
                // Emoji clusters render taller than `fontSize * 1.2` because
                // their ascent/descent exceeds the surrounding latin font.
                // If we only reserve 1.2x per line, a long pure-emoji string
                // that auto-wraps to N lines accumulates ~0.25*fontSize*N of
                // shortfall and the bottom of the last line gets clipped.
                val emojiH = (fontSize * EMOJI_LINE_HEIGHT_FACTOR).let {
                    if (it.isFinite() && it > 0f) it else fallbackLineHeight
                }
                if (emojiH > maxHeight) maxHeight = emojiH
                graphemeCount += 1
                i = end
            } else {
                nonEmojiBuf += text.asDynamic().charAt(i).unsafeCast<String>()
                i += 1
            }
        }
        flushNonEmoji()
        // Final guard: a NaN/Infinity/<=0 here would later be serialized into
        // `style.lineHeight = "NaNpx"` and crash the layout (the text would
        // collapse to a 0.5px hairline). Always return a renderable size.
        if (!totalWidth.isFinite() || totalWidth < 0f) totalWidth = 0f
        if (!maxHeight.isFinite() || maxHeight <= 0f) maxHeight = fallbackLineHeight
        // IMPORTANT: do NOT return a Kotlin `SizeF` here. Kotlin/JS mangles
        // `SizeF.width` / `SizeF.height` to accessor methods like
        // `width_xxx_k$()`, so reading `ret.width` on the JS side yields
        // `undefined`. The caller treats `undefined` as a zero-size span and
        // the whole emoji line collapses into a 0.5-px hairline.
        //
        // Return a plain JS object whose `width`, `height` and `graphemeCount`
        // are real own properties accessible from any JS / dynamic context.
        val ret: dynamic = js("({})")
        ret.width = totalWidth
        ret.height = maxHeight
        ret.graphemeCount = graphemeCount
        return ret
    }

    /**
     * Compute a "safe" line count from the geometric ratio totalWidth /
     * constraintWidth.
     *
     * The native text components break by word / break-word, while the
     * canvas-based measurement here only knows the total drawn width. When
     * the fractional part of the ratio is close to 1 (e.g. 1.92), the real
     * renderer is very likely to push the trailing word/grapheme onto a
     * brand-new line, but `ceil()` would still report the same line count
     * and underestimate the height. Reserve one extra line in that case.
     */
    private fun safeLineCount(totalWidth: Float, constraintWidth: Float): Int {
        if (constraintWidth <= 0f || totalWidth <= 0f) return 1
        val ratio = totalWidth / constraintWidth
        val ceiled = ceil(ratio).toInt()
        val frac = ratio - ratio.toInt()
        return if (frac > LINE_FRACTION_GUARD) ceiled + 1 else ceiled
    }

    /**
     * Process the size data of the remaining lines
     *
     * @param constraintSize Parent container constraint size
     * @param linesSizeList Line data list
     */
    private fun processRemainingLine(
        constraintSize: SizeF,
        linesSizeList: JsArray<SizeF>,
        view: KRRichTextView
    ) {
        // Resolve the strut height that every line must contribute at minimum:
        //   1) explicit container line-height takes precedence;
        //   2) otherwise fall back to container fontSize * LINE_HEIGHT_FACTOR;
        //   3) finally, never below DEFAULT_FONT_SIZE * LINE_HEIGHT_FACTOR.
        // This guards against the multi-line-height-too-small problem when
        // the last physical line's accumulated `currentLineHeight` is smaller
        // than the real rendered strut (e.g. a span with very small fontSize
        // ended the line, but the container actually reserves a taller box).
        val containerLineHeightStr = view.ele.style.lineHeight
        val containerFontSizeStr = view.ele.style.fontSize
        val strutHeight: Float = when {
            containerLineHeightStr.isNotEmpty() -> containerLineHeightStr.pxToFloat()
            containerFontSizeStr.isNotEmpty() ->
                containerFontSizeStr.pxToFloat() * LINE_HEIGHT_FACTOR
            else -> DEFAULT_FONT_SIZE * LINE_HEIGHT_FACTOR
        }
        // Effective height for the trailing line(s): take the larger of the
        // accumulated per-span height and the container strut, otherwise a
        // shorter trailing span would shrink the whole final line below the
        // real rendered height.
        val effectiveLineHeight = max(view.currentLineHeight, strutHeight)
        if (constraintSize.width > 0f) {
            // Total number of lines, using safe count to absorb word-break
            // induced extra lines that the geometric measurement ignores.
            val totalLines = safeLineCount(view.currentLineWidth, constraintSize.width)
            // Remaining width that is not full
            val remainWidth = view.currentLineWidth % constraintSize.width
            if (totalLines > 1) {
                // For parts greater than one line, directly insert full line size list
                repeat(totalLines - 1) {
                    linesSizeList.add(SizeF(constraintSize.width, effectiveLineHeight))
                }
            }
            // Last insert width that is not full line
            linesSizeList.add(SizeF(remainWidth, effectiveLineHeight))
        } else {
            // If there is no constraint, just use the remaining width and height directly
            linesSizeList.add(SizeF(view.currentLineWidth, effectiveLineHeight))
        }
    }

    /**
     * Process the size data of placeholder spans
     *
     * @param childSpan Child span node
     * @param constraintSize Parent container constraint size
     * @param linesSizeList Line size list
     */
    private fun processPlaceHolderSpan(
        view: KRRichTextView,
        childSpan: RichTextSpan,
        constraintSize: SizeF,
        linesSizeList: JsArray<SizeF>,
    ) {
        // A placeholder (e.g. inline image) alone is not enough to decide the
        // real line height: when rendered inside a `rich-text`, the host text
        // element always contributes a "strut" whose height is at least
        // `fontSize * line-height`. If we only take the placeholder's own
        // height, the measurement will be smaller than the actual rendered
        // height, which may cause the following lines to be clipped.
        //
        // Use business-specified lineHeight when available; otherwise fall
        // back to `container fontSize * LINE_HEIGHT_FACTOR`.
        val businessLineHeight = view.ele.style.lineHeight
        val strutHeight = if (businessLineHeight.isNotEmpty()) {
            businessLineHeight.pxToFloat()
        } else {
            val containerFontSize = view.ele.style.fontSize
            val fs = if (containerFontSize.isNotEmpty()) {
                containerFontSize.pxToFloat()
            } else {
                DEFAULT_FONT_SIZE
            }
            fs * LINE_HEIGHT_FACTOR
        }
        // Use placeholder height as current line height, need to consider
        // multiple placeholder situations, use the highest as height.
        // Also make sure it is not smaller than the container strut.
        val targetLineHeight = max(childSpan.height, strutHeight)
        if (targetLineHeight > view.currentLineHeight) {
            view.currentLineHeight = targetLineHeight
        }
        // Current placeholder plus after width
        val sumWidth = view.currentLineWidth + childSpan.width
        if (sumWidth <= constraintSize.width || constraintSize.width == 0f) {
            // If placeholder span width plus does not exceed one line, or no constraint width,
            // then directly add current line Record offsetLeft, because the current text width
            // calculation has been multiplied by the ratio value, so the actual placeholder
            // offset needs to be divided by the offset ratio
            childSpan.offsetLeft = view.currentLineWidth
            if (MiniGlobal.isAndroid) {
                childSpan.offsetLeft /= WIDTH_RATIO_MAGIC
            }
            // Then just add width to current line
            view.currentLineWidth = sumWidth
            // Record line index
            childSpan.lineIndex = linesSizeList.length.toInt()
        } else {
            // If it exceeds one line, it needs to be changed, because placeholder span cannot be
            // folded, so placeholder span needs to start from new line head Here, it needs to be
            // noted that because placeholder span will not exceed one line, so no need to consider
            // adding width that exceeds two lines after placeholder span
            // First insert need to be changed line size
            linesSizeList.add(SizeF(view.currentLineWidth, view.currentLineHeight))
            // Save new line width
            view.currentLineWidth = childSpan.width
            // Record offsetLeft
            childSpan.offsetLeft = 0f
            // Record line index
            childSpan.lineIndex = linesSizeList.length.toInt()
        }
    }

    /**
     * Get and calculate the size list of each line of text
     */
    private fun calculateLinesSize(constraintSize: SizeF, view: KRRichTextView): JsArray<SizeF> {
        val linesSizeList: JsArray<SizeF> = JsArray()
        val lineHeight = view.ele.style.lineHeight
        val containerLineHeight = if (lineHeight.isNotEmpty()) lineHeight.pxToFloat() else 0f

        // Process child text nodes in a loop to calculate actual line size of each line
        view.richTextSpanList.forEach { childSpan ->
            if (childSpan.width != 0f) {
                // Placeholder Span processing
                processPlaceHolderSpan(view, childSpan, constraintSize, linesSizeList)
            } else {
                // Plain span processing — use this span's own letter-spacing
                // so the measured width matches the real rendering width when
                // different spans carry different letter-spacing values.
                // Per-span line-height takes precedence over the container's
                // line-height so that a span with a larger line-height
                // correctly contributes to the line box height instead of
                // being collapsed by the container default.
                val effectiveLineHeight = if (childSpan.lineHeight > 0f) {
                    max(childSpan.lineHeight, containerLineHeight)
                } else {
                    containerLineHeight
                }
                processTextSpan(
                    view,
                    childSpan,
                    constraintSize,
                    linesSizeList,
                    effectiveLineHeight,
                    childSpan.letterSpacing
                )
            }
        }
        // Process remaining lines
        processRemainingLine(constraintSize, linesSizeList, view)
        // Reset current line data
        resetCurrentLineSize(view)
        // Return line size data list
        return linesSizeList
    }

    /**
     * New line size calculation
     */
    private fun processNewLineSize(
        view: KRRichTextView,
        spanSize: SizeF,
        constraintSize: SizeF,
        realLineHeight: Float,
        linesSizeList: JsArray<SizeF>,
        isLastItem: Boolean,
    ) {
        // Calculate by new line, line height is current span height or parent container specified line height
        view.currentLineHeight = if (realLineHeight > 0f) realLineHeight else spanSize.height
        if (isLastItem) {
            // Last element as remain element processing
            view.currentLineWidth = spanSize.width
        } else {
            // For elements that are not the last, each line of the line is independent.
            // Use safe line count so that word-break-induced extra lines don't
            // shrink the measured height below the actual rendered height.
            val totalLines = if (constraintSize.width > 0f) {
                safeLineCount(spanSize.width, constraintSize.width)
            } else 1
            // Current line remaining width — when the safe count was bumped
            // by 1, the remaining width is just the leftover above the last
            // full-line; when no bump happened, fall back to width % constraint.
            view.currentLineWidth = if (constraintSize.width > 0f) {
                val mod = spanSize.width % constraintSize.width
                if (mod == 0f && totalLines >= 1 && spanSize.width > 0f) constraintSize.width else mod
            } else spanSize.width
            // If there are multiple lines, add them to full line size list
            if (totalLines > 1) {
                repeat(totalLines - 1) {
                    linesSizeList.add(SizeF(constraintSize.width, view.currentLineHeight))
                }
            }
            // Then add the last line
            linesSizeList.add(SizeF(view.currentLineWidth, view.currentLineHeight))
        }
    }

    /**
     * Process the size data of participating lines for inline calculation
     */
    private fun processInlineLineSize(
        view: KRRichTextView,
        spanSize: SizeF,
        constraintSize: SizeF,
        linesSizeList: JsArray<SizeF>,
        realLineHeight: Float,
    ) {
        val sumWidth = view.currentLineWidth + spanSize.width
        if (sumWidth <= constraintSize.width || constraintSize.width == 0f) {
            // If it does not exceed one line or no constraint width, then directly add width
            view.currentLineWidth += spanSize.width
            // If line height is greater than current line height, it means line height has not been
            // set or has been set, but current line height is larger, need to set
            if (spanSize.height > view.currentLineHeight) {
                view.currentLineHeight = spanSize.height
            }
        } else {
            // Width that exceeds one line
            val subWidth = sumWidth - constraintSize.width
            // The first "full" line we are about to flush should also account
            // for THIS span's height, because the span occupies a non-zero
            // portion of that line. Otherwise, when the previous accumulated
            // line was empty (currentLineHeight == 0) or shorter than this
            // span, the flushed line would be too short.
            val firstLineHeight = max(view.currentLineHeight, spanSize.height)
            // First record full line size, because it is not empty line, so it must have been set line height
            linesSizeList.add(SizeF(constraintSize.width, firstLineHeight))
            // Remaining non-one line width — guard against negative / zero
            // so we don't accidentally emit an empty extra line.
            val safeSub = if (subWidth < 0f) 0f else subWidth
            view.currentLineWidth = safeSub % constraintSize.width
            // Remaining total number of lines, with the safe-count bump.
            val totalLines = safeLineCount(safeSub, constraintSize.width)
            // All parts that exceed one line in remaining lines are calculated as full lines
            if (totalLines > 0) {
                // Because it is already new line, actual line height will not be affected by placeholder
                // span, so need to use current span line height or parent container line height
                view.currentLineHeight = if (realLineHeight > 0f) realLineHeight else spanSize.height
                repeat(totalLines - 1) {
                    // Insert full line size, new line height uses actual size
                    linesSizeList.add(SizeF(constraintSize.width, view.currentLineHeight))
                }
            }
        }
    }

    /**
     * Get size list of non-placeholder spans
     */
    private fun getSpanSizeList(
        value: String,
        fontSize: Float,
        fontWeight: Int,
        fontFamily: String,
        fontStyle: String,
        letterSpacing: Float = 0f,
    ): JsArray<SizeF> {
        // First split the text into a list based on line breaks
        val textArray = value.asDynamic().split("\n").unsafeCast<JsArray<String>>()
        // Span size list
        val textSizeList: JsArray<SizeF> = JsArray()
        // Fallback empty-line height, used when a "\n" produces an empty segment
        // (e.g. leading/trailing "\n" or consecutive "\n"). We must NOT skip
        // such segments — otherwise the line-break semantics would be lost and
        // the outer logic would mistakenly concatenate the next span onto the
        // current line. Height uses fontSize * 1.2 as a conservative estimation
        // aligned with other canvas-measure fallbacks in this file.
        val emptyLineHeight = (fontSize * 1.2).toFloat()
        // Process in a loop
        textArray.forEach { it ->
            if (it == "") {
                // Preserve an empty segment as a zero-width placeholder so the
                // caller (processTextSpan) can emit a real line break.
                textSizeList.add(SizeF(0f, emptyLineHeight))
            } else {
                // Pick the measurement strategy based on whether the line
                // contains emoji-ish code units. The mixed path is more
                // expensive but is the only way to avoid undercounting the
                // emoji width on canvas (which lacks emoji glyphs).
                val hasEmoji = containsEmoji(it)
                var textWidth: Float
                var textHeight: Float
                // Number of graphemes used to multiply letter-spacing.
                // For pure ASCII / CJK lines this equals it.length; for
                // emoji lines it counts each emoji cluster as 1 instead of
                // its UTF-16 length (2~8), avoiding the over-compensation
                // that would otherwise push the line width way too wide.
                var graphemeCount: Int

                if (hasEmoji) {
                    val mixed = measureMixedLine(
                        it, fontSize, fontWeight, fontFamily, fontStyle
                    )
                    textWidth = mixed.width.unsafeCast<Float>()
                    textHeight = mixed.height.unsafeCast<Float>()
                    graphemeCount = mixed.graphemeCount.unsafeCast<Int>()
                    // If for any reason the mixed measure produced a zero
                    // height (very short emoji-only string with broken
                    // canvas metrics), fall back to fontSize * 1.2.
                    if (textHeight <= 0f) {
                        textHeight = (fontSize * 1.2f)
                    }
                    // Same for width — emoji glyphs always render with a
                    // non-zero width on screen; if our measurement collapsed
                    // to 0, use a conservative grapheme-based fallback so the
                    // outer logic does not treat this whole line as "empty".
                    if (textWidth <= 0f && graphemeCount > 0) {
                        textWidth = graphemeCount * fontSize * EMOJI_WIDTH_FACTOR
                    }
                } else {
                    // Calculate the width of each line
                    val textMetrics =
                        measureTextWidth(it, fontSize.toInt(), fontWeight, fontFamily, fontStyle)
                    // Text width
                    textWidth = if (jsTypeOf(textMetrics.actualBoundingBoxLeft) == "number") {
                        (textMetrics.actualBoundingBoxLeft + textMetrics.actualBoundingBoxRight)
                            .unsafeCast<Float>()
                    } else {
                        0f
                    }
                    // Text height，use canvas measure result，plus 1px for round missing
                    textHeight = if (textMetrics.fontBoundingBoxAscent != null && textMetrics.fontBoundingBoxDescent != null) {
                        textMetrics.fontBoundingBoxAscent.unsafeCast<Float>() +
                                textMetrics.fontBoundingBoxDescent.unsafeCast<Float>() + 1f
                    } else {
                        // wechat 8.0.49 run in ios 18.2 can not get textMetrics.fontBoundingBoxAscent textMetrics.fontBoundingBoxDescent
                        // use fontSize * 1.2
                        (fontSize * 1.2).toFloat()
                    }
                    // Add the width and height of each line, using the larger of canvas width and actualBoundingBox
                    textWidth = max(textWidth, textMetrics.width.unsafeCast<Float>())
                    graphemeCount = it.length
                    // === iOS real-device font compensation ===
                    // Mini-program OffscreenCanvas measures text with a generic
                    // sans-serif font, but the real <text> element on an iOS
                    // physical device falls back to PingFang SC, which is
                    // typically ~6% wider per glyph at the same point size.
                    // Without this compensation, the parent container width
                    // (derived from our measurement) is smaller than what
                    // PingFang actually needs, and the last word is dropped
                    // (e.g. "Change Theme" rendered as just "Change").
                    // The compensation is intentionally NOT applied on Android
                    // (sans-serif == Roboto, measurement matches reality) nor
                    // on the WeChat DevTools simulator (uses desktop fonts).
                    if (textWidth > 0f && MiniGlobal.isIOS && !MiniGlobal.isDevTools) {
                        textWidth *= IOS_REAL_FONT_WIDTH_FACTOR
                    }
                }
                // Canvas measureText does NOT take letter-spacing into account, but the
                // real rendering of <text> does. Compensate it here otherwise the measured
                // width would be smaller than the actual drawn width and cause the last
                // character to be truncated.
                // NOTE: use `graphemeCount` instead of UTF-16 length so that emoji
                // clusters contribute exactly one letter-spacing gap (matching the
                // real renderer), instead of 2~8 of them.
                if (letterSpacing != 0f && graphemeCount > 0) {
                    textWidth += graphemeCount * letterSpacing
                }
                if (MiniGlobal.isAndroid) {
                    // Android canvas measurement is not accurate, so we need to multiply a magic number.
                    // Apply AFTER letter-spacing compensation so both glyph width and spacing
                    // are scaled by the same factor, keeping them consistent with real rendering.
                    textWidth *= WIDTH_RATIO_MAGIC
                }
                textSizeList.add(SizeF(textWidth, textHeight))
            }
        }
        // Return overall width and height
        return textSizeList
    }

    /**
     * Process the size data of text spans
     *
     * @param childSpan Child span node
     * @param constraintSize Parent container constraint size
     * @param realLineHeight Line height
     * @param linesSizeList Line size list
     */
    private fun processTextSpan(
        view: KRRichTextView,
        childSpan: RichTextSpan,
        constraintSize: SizeF,
        linesSizeList: JsArray<SizeF>,
        realLineHeight: Float,
        letterSpacing: Float = 0f,
    ) {
        // Get text span line size list, split by line break
        val spanSizeList = getSpanSizeList(
            childSpan.value,
            childSpan.fontSize,
            childSpan.fontWeight,
            childSpan.fontFamily,
            childSpan.fontStyle,
            letterSpacing,
        )
        spanSizeList.forEach { item, index ->
            // Zero-width item means this segment came from a "\n" that produced
            // an empty line (leading/trailing/consecutive "\n"). We must flush
            // whatever has been accumulated on the current line to the result
            // list and then start a fresh empty line, otherwise the line-break
            // semantics would be lost.
            if (item.width == 0f) {
                if (view.currentLineWidth != 0f || view.currentLineHeight != 0f) {
                    // Close the currently accumulated line
                    linesSizeList.add(
                        SizeF(view.currentLineWidth, view.currentLineHeight)
                    )
                } else {
                    // Even an already-empty current line represents a real blank
                    // line introduced by the "\n", we still need to record it so
                    // the total line count is correct.
                    linesSizeList.add(
                        SizeF(0f, if (realLineHeight > 0f) realLineHeight else item.height)
                    )
                }
                // Reset current line — the next segment (if any) will start at
                // the beginning of a brand-new line.
                view.currentLineWidth = 0f
                view.currentLineHeight = 0f
                return@forEach
            }
            if (index == 0 && view.currentLineWidth != 0f) {
                // If it is multi-line text, it means there is line break, then the first line, and current
                // line width is not 0, then should participate in line accumulation calculation, rather than
                // Start a new line
                processInlineLineSize(view, item, constraintSize, linesSizeList, realLineHeight)
            } else {
                // Other cases are calculated by new line
                // Whether it is the last element, the last element as remain element processing,
                // because there may be other span to be appended later
                val isLastItem = index == spanSizeList.length - 1
                // Process new line
                processNewLineSize(view, item, constraintSize, realLineHeight, linesSizeList, isLastItem)
            }
        }
    }

    /**
     * Reset the size data of the current line
     */
    private fun resetCurrentLineSize(view: KRRichTextView) {
        view.currentLineWidth = 0f
        view.currentLineHeight = 0f
    }

    /**
     * Calculate the size data of plain text
     */
    private fun calculateTextSize(constraintSize: SizeF, view: KRRichTextView): SizeF {
        val ele = view.ele
        
        // Try cache first for plain text
        val cacheKey = TextMeasureCache.generateKey(
            text = view.rawText,
            fontSize = ele.style.fontSize,
            fontWeight = ele.style.fontWeight,
            fontFamily = ele.style.fontFamily,
            fontStyle = ele.style.fontStyle,
            letterSpacing = "",
            lineHeight = ele.style.lineHeight,
            constraintWidth = constraintSize.width,
            numberOfLines = view.numberOfLines
        )
        
        TextMeasureCache.get(cacheKey)?.let { cachedSize ->
            Log.trace("Using cached text size: ", cachedSize.width, cachedSize.height)
            return cachedSize
        }
        
        // Font size string
        val fontSizeStr = ele.style.fontSize.asDynamic().split("px")[0].unsafeCast<String>()
        // Font size
        val fontSize = fontSizeStr.toFloat()
        // Font weight
        val fontWeight = if (ele.style.fontWeight != "") {
            ele.style.fontWeight.toInt()
        } else {
            FONT_WEIGHT_DEFAULT
        }
        // Actual text height
        val realLineHeight: Float
        // Style's lineHeight
        val lineHeight = ele.style.lineHeight
        // If lineHeight is set, use lineHeight as line height, otherwise use measured line height
        realLineHeight = if (lineHeight != "") {
            lineHeight.pxToFloat()
        } else {
            0f
        }
        // letter-spacing from inline style (e.g. "1.5px"), canvas measureText
        // does not include it, we need to add it back manually so the measured
        // width matches the real rendering width.
        val letterSpacingStr = ele.style.letterSpacing
        val letterSpacing = if (letterSpacingStr.isNotEmpty()) letterSpacingStr.pxToFloat() else 0f
        // Text span size list
        var linesSizeList = JsArray<SizeF>()
        // Process Text span size list
        processTextSpan(
            view,
            RichTextSpan(
                value = view.rawText,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = ele.style.fontFamily,
                fontStyle = ele.style.fontStyle
            ), constraintSize, linesSizeList, realLineHeight, letterSpacing
        )
        // Process remaining lines
        processRemainingLine(constraintSize, linesSizeList, view)

        // Apply lineBreakMargin before line-clamp so that the "is truncated"
        // judgement can observe the real overflow state.
        applyLineBreakMargin(constraintSize, view, linesSizeList)

        // If maximum number of lines is set, and actual exceeds maximum number of lines,
        // use maximum number of lines, otherwise use actual number of lines for processing
        if (view.numberOfLines in 1..linesSizeList.length) {
            // Set multi-line style
            setMultiLineStyle(view.numberOfLines, view)
            // Keep specified height
            linesSizeList = linesSizeList.slice(0, view.numberOfLines)
        } else {
            // Clear multi-line style
            setMultiLineStyle(0, view)
        }

        // Reset current line width and height
        resetCurrentLineSize(view)
        // save current line size
        view.ele.asDynamic().linesCount = linesSizeList.length
        
        // Return width and height occupied by plain Text
        val result = calculateTotalSize(linesSizeList)
        
        // Cache the result
        TextMeasureCache.put(cacheKey, result)
        
        return result
    }

    /**
     * Calculate the size data of the element occupied
     */
    private fun calculateTotalSize(list: JsArray<SizeF>): SizeF {
        // Line width
        var realWidth = 0f
        // Line height
        var realHeight = 0f

        // Finally, calculate the width and height occupied by text based on the comprehensive line data list
        list.forEach { line ->
            if (line.width > realWidth) {
                // Use the widest line as element width
                realWidth = line.width
            }
            // Height is the height of each line added up
            realHeight += line.height
        }

        // Return element size data, + 0.5 to prevent small decimal calculation error
        return SizeF(realWidth + TEXT_WIDTH_DEFAULT, realHeight)
    }

    /**
     * Calculate the offset top of placeholder spans
     */
    private fun calculateSpanOffsetTop(linesSizeList: JsArray<SizeF>, view: KRRichTextView) {
        // Pre-compute the top Y coordinate of each line to avoid repeated accumulation
        val lineTopPositions: JsArray<Float> = JsArray()
        var accumulated = 0f
        linesSizeList.forEach { line ->
            lineTopPositions.add(accumulated)
            accumulated += line.height
        }
        lineTopPositions.add(accumulated)
        // Iterate all placeholder spans and restore the vertically-centered offsetTop using the stored line index
        view.richTextSpanList.forEach { span ->
            if (span.width != 0f) {
                val lineIndex = span.lineIndex  // Retrieve the line index stored earlier
                if (lineIndex < linesSizeList.length) {  // Skip spans in truncated lines (not visible)
                    val lineTop    = lineTopPositions[lineIndex]
                    val lineHeight = linesSizeList[lineIndex].height
                    // Vertically center: line top + (line height - placeholder height) / 2
                    span.offsetTop = lineTop + (lineHeight - span.height).coerceAtLeast(0f) / 2f
                } else {
                    span.offsetTop = lineTopPositions[lineTopPositions.length - 1]
                }
            }
        }
    }

    /**
     * Apply lineBreakMargin: decide whether the reserved right-side blank on
     * the last visible line would cause the text to wrap. When triggered we
     * just flip `isLineBreakMargin = true` so core can fire `onLineBreakMargin`.
     *
     * The actual visual blank is produced by two right-floating spans injected
     * in [setRichTextValues] (mirroring the H5 implementation).
     */
    private fun applyLineBreakMargin(
        constraintSize: SizeF,
        view: KRRichTextView,
        linesSizeList: JsArray<SizeF>,
    ) {
        val lineBreakMargin = view.getLineBreakMargin()
        val maxLines = view.numberOfLines
        // Reset previous state first so that successive layouts are correct
        view.setIsLineBreakMargin(false)
        if (lineBreakMargin <= 0f || maxLines <= 0) {
            return
        }
        if (constraintSize.width <= lineBreakMargin) {
            return
        }
        // Only meaningful when the content actually reaches the last visible line
        if (linesSizeList.length < maxLines) {
            return
        }
        val effectiveWidth = constraintSize.width - lineBreakMargin
        val lastVisibleIndex = maxLines - 1
        val lastVisibleLine = linesSizeList[lastVisibleIndex]
        val hasMoreLines = linesSizeList.length > maxLines
        // Strategy B (simplified): if the last visible line's text width already
        // exceeds `effectiveWidth`, or there are still overflow lines after it,
        // we consider that the text would have wrapped when the reserved margin
        // is taken into account.
        if (hasMoreLines || lastVisibleLine.width > effectiveWidth) {
            view.setIsLineBreakMargin(true)
        }
    }

    /**
     * Calculate the size data of rich text element occupied
     */
    private fun calculateRichTextSize(constraintSize: SizeF, view: KRRichTextView): SizeF {
        // Get the size data list of all lines of the element
        var linesSizeList = calculateLinesSize(constraintSize, view)
        // Apply lineBreakMargin before line-clamp so that the "is truncated"
        // judgement can observe the real overflow state.
        applyLineBreakMargin(constraintSize, view, linesSizeList)
        // If maximum number of lines is set, and actual exceeds maximum number of lines,
        // use maximum number of lines, otherwise use actual number of lines for processing
        if (view.numberOfLines in 1..linesSizeList.length) {
            // Set multi-line style
            setMultiLineStyle(view.numberOfLines, view)
            // Keep specified height
            linesSizeList = linesSizeList.slice(0, view.numberOfLines)
        } else {
            // Clear multi-line style
            setMultiLineStyle(0, view)
        }
        // Here style changed, need to re-set divHtml, considering update queue problem,
        // whether need to delay setting, only rich text needs setting
        view.ele.setAttribute("nodes", view.divHtml)

        // Calculate span offset top
        calculateSpanOffsetTop(linesSizeList, view)

        // Get occupied size position information based on the final size list
        return calculateTotalSize(linesSizeList)
    }

    /**
     * Get innerHTML of child spans
     */
    private fun getChildSpanHtml(view: KRRichTextView): String {
        var spanHtml = ""
        view.childSpanList.forEach { child ->
            val span = child.unsafeCast<MiniSpanElement>()
            spanHtml += "<span style=\"${span.style.cssText}\">${span.textContent}</span>"
        }
        return spanHtml
    }

    /**
     * Create span for internal use
     */
    private fun createSpan(value: JSONObject, view: KRRichTextView): MiniSpanElement {
        val span = MiniSpanElement()
        val text = view.getText(value) ?: return span
        span.innerHTML = text
        span.textContent = text
        val style = span.style
        val color = value.optString(COLOR, "")
        if (color.isNotEmpty()) {
            style.color = color.toRgbColor()
        }
        val fontSize = value.optDouble(FONT_SIZE, 0.0)
        if (fontSize != 0.0) {
            style.fontSize = "${round(fontSize)}px"
        }
        if (style.fontSize.isEmpty()) {
            style.fontSize = view.ele.style.fontSize
        }

        val fontFamily = value.optString(FONT_FAMILY)
        if (fontFamily.isNotEmpty()) {
            style.fontFamily = fontFamily
        } else {
            style.fontFamily = getDefaultFontFamily()
        }

        val textShadow = value.optString(TEXT_SHADOW)
        if (textShadow.isNotEmpty()) {
            val textShadowSpilt = textShadow.asDynamic().split(" ")
            val offsetX = "${textShadowSpilt[0]}px"
            val offsetY = "${textShadowSpilt[1]}px"
            val radius = "${textShadowSpilt[2]}px"
            val shadowColor = textShadowSpilt[3].unsafeCast<String>().toRgbColor()
            style.textShadow = "$offsetX $offsetY $radius $shadowColor"
        }

        val fontWeight = value.optString(FONT_WEIGHT)
        if (fontWeight.isNotEmpty()) {
            style.fontWeight = fontWeight
        } else {
            style.fontWeight = "400"
        }

        val fontStyle = value.optString(FONT_STYLE)
        if (fontStyle.isNotEmpty()) {
            style.fontStyle = fontStyle
        }

        val fontVariant = value.optString(FONT_VARIANT)
        if (fontVariant.isNotEmpty()) {
            style.fontVariant = fontVariant
        }
        val letterSpacing = value.optDouble(LETTER_SPACING, -1.0)
        if (letterSpacing != -1.0) {
            // Set letterSpacing according to letter spacing
            style.letterSpacing = "${letterSpacing.toNumberFloat()}px"
        }

        val strokeColor = value.optString(STROKE_COLOR).toRgbColor()
        if (strokeColor.isNotEmpty()) {
            style.webkitTextStroke = strokeColor
        }

        val strokeWidth = value.optDouble(STROKE_WIDTH, 0.0)
        if (strokeWidth != 0.0) {
            style.webkitTextStroke = "$strokeColor ${strokeWidth / 4}px"
        }

        val lineHeight = value.optDouble(LINE_HEIGHT, -1.0)
        if (lineHeight != -1.0) {
            // Set lineHeight according to line height
            style.lineHeight = "${lineHeight.toNumberFloat()}px"
        }
        val textDecoration = value.optString(TEXT_DECORATION)
        if (textDecoration.isNotEmpty()) {
            style.textDecoration = textDecoration
        }
        val textIndent = value.optDouble(HEAD_INDENT, 0.0)
        if (textIndent != 0.0) {
            style.textIndent = "${textIndent}px"
        }
        // Placeholder span width
        val placeHolderWidth = value.optDouble(PLACEHOLDER_WIDTH, 0.0)
        // Placeholder span height
        val placeHolderHeight = value.optDouble(PLACEHOLDER_HEIGHT, 0.0)
        // If placeholder span has width and height, then set it
        if (placeHolderWidth != 0.0 && placeHolderHeight != 0.0) {
            style.width = "${placeHolderWidth}px"
            span.offsetWidth = placeHolderWidth.toFloat()
            style.height = "${placeHolderHeight}px"
            span.offsetHeight = placeHolderHeight.toFloat()
            // This type of span is set as inline-block type
            style.display = "inline-block"
            // Vertically centered
            style.verticalAlign = "middle"
        }
        return span
    }

    /**
     * Get placeholder image style
     */
    private fun getPlaceHolderImageStyle(view: MiniElement): String {
        // Style list
        val styleList: JsArray<String> = JsArray()
        // First insert fixed style
        styleList.push("width:100%;height:100%;display:block;")
        // Then insert external style
        styleList.push("border-top-left-radius:${view.style.borderTopLeftRadius};")
        styleList.push("border-top-right-radius:${view.style.borderTopLeftRadius};")
        styleList.push("border-bottom-left-radius:${view.style.borderBottomLeftRadius};")
        styleList.push("border-bottom-right-radius:${view.style.borderBottomRightRadius};")

        return styleList.join("")
    }

    /**
     * Throttled execution function (supports parameters)
     */
    private fun throttle(view: KRRichTextView, action: () -> Unit) {
        // Cancel unexecuted task
        if (view.pendingJob != 0) {
            MiniGlobal.clearTimeout(view.pendingJob)
        }
        // Delay execution task
        view.pendingJob = MiniGlobal.setTimeout({
            action()
        }, IMAGE_SPAN_DELAY)
    }

    /**
     * measure real text size
     */
    override fun measureTextSize(constraintSize: SizeF, view: KRRichTextView, renderText: String): SizeF {
        // real eom
        val ele = view.ele
        // Set element maximum width constraint
        if (constraintSize.width > 0f) {
            ele.style.maxWidth = "${constraintSize.width}px"
        }

        val rawSize = if (view.isRichText && view.richTextSpanList.length > 0) {
            // Rich text needs loop calculation and comprehensive calculation
            calculateRichTextSize(constraintSize, view)
        } else {
            calculateTextSize(constraintSize, view)
        }



        // ===== FINAL GUARD =====
        // Whatever happened upstream (canvas returning NaN for fontBoundingBox*,
        // a mis-typed font-size, an empty span list, etc.) the value we hand
        // back to the layout engine MUST be a renderable finite positive size,
        // otherwise the mini-program will write
        //   `width: NaNpx; height: NaNpx; line-height: NaNpx`
        // and collapse the whole element into a 0.5px hairline (which then
        // breaks the surrounding List layout).
        val fontSizeStr = ele.style.fontSize.asDynamic().split("px")[0].unsafeCast<String>()
        val fs = fontSizeStr.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_FONT_SIZE
        var sw = rawSize.width
        var sh = rawSize.height
        if (!sw.isFinite() || sw < 0f) sw = 0f
        if (!sh.isFinite() || sh <= 0f) sh = fs * 1.2f
        val textSize = SizeF(sw, sh)

        if (!(view.isRichText && view.richTextSpanList.length > 0)) {
            val webkitLineClamp = ele.style.asDynamic().webkitLineClamp.unsafeCast<String>()
            // set lineHeight for text to vertical center, only support single line.
            // safeH is now guaranteed finite and > 0 by the guard above.
            if (ele.style.lineHeight.isEmpty() &&
                !view.rawText.contains("\n") &&
                (webkitLineClamp.isEmpty() || webkitLineClamp.toInt() == 1) &&
                ele.asDynamic().linesCount == 1
            ) {
                ele.style.lineHeight = "${sh}px"
            }
        }
        return textSize
    }

    fun clearRichTextValues(view: KRRichTextView) {
        view.childSpanList.clear()
        view.richTextSpanList.clear()
        view.imageSpanList.clear()
        view.imageSpanCount = 0
    }

    /**
     * Plain-text path of `lineBreakMargin` on mini-app.
     *
     * Unlike H5, the mini-app `text` component only shows its `value`
     * attribute and cannot honor floating child spans. To reserve the
     * `lineBreakMargin` blank visually we have to promote the element to
     * a `rich-text` component and feed it HTML nodes:
     *   - Two right-floating spans that reserve the blank on the last line
     *     (exactly the same trick as the rich-text path);
     *   - A span that carries the plain text content with the element's
     *     own text styles inherited, so the visual looks identical.
     *
     * Returns `true` to tell the caller we've handled it and the legacy
     * DOM-based `insertBefore` branch should be skipped.
     */
    override fun applyPlainTextLineBreakMargin(view: KRRichTextView): Boolean {
        val lineBreakMargin = view.getLineBreakMargin()
        val measureResult = view.getMeasureResult()
        val rawText = view.rawText
        // Only handle when margin is set, measurement is ready and text
        // is not empty. Otherwise leave everything untouched so nothing
        // regresses for the common case.
        if (lineBreakMargin <= 0f || measureResult.height <= 0f || rawText.isEmpty()) {
            return false
        }
        // If we've already appended the float spans on a previous pass,
        // skip to avoid duplicating them.
        if (view.getHasAppendFloatSpans()) {
            return true
        }

        // Reset rich-text related state (the plain-text path has never
        // populated them, but be defensive for repeated layout passes).
        clearRichTextValues(view)

        // Promote the paragraph element to `rich-text`. The transform
        // switch in MiniParagraphElement.onTransformData() relies on both
        // `isRichText == true` and `richTextSpanList.length > 0`.
        view.isRichText = true
        view.richTextSpanList.add(RichTextSpan(value = rawText))

        // Build the inner text span. Inherit the core text styles from the
        // paragraph element so the rich-text renders visually the same as
        // the original plain text.
        val style = view.ele.style
        val textStyleParts = mutableListOf<String>()
        if (style.fontSize.isNotEmpty()) textStyleParts.add("font-size:${style.fontSize}")
        if (style.fontFamily.isNotEmpty()) textStyleParts.add("font-family:${style.fontFamily}")
        if (style.fontWeight.isNotEmpty()) textStyleParts.add("font-weight:${style.fontWeight}")
        if (style.fontStyle.isNotEmpty()) textStyleParts.add("font-style:${style.fontStyle}")
        if (style.color.isNotEmpty()) textStyleParts.add("color:${style.color}")
        if (style.letterSpacing.isNotEmpty()) textStyleParts.add("letter-spacing:${style.letterSpacing}")
        if (style.lineHeight.isNotEmpty()) textStyleParts.add("line-height:${style.lineHeight}")
        if (style.textDecoration.isNotEmpty()) textStyleParts.add("text-decoration:${style.textDecoration}")
        if (style.textAlign.isNotEmpty()) textStyleParts.add("text-align:${style.textAlign}")
        val textStyle = textStyleParts.joinToString(";")
        val escapedText = escapeHtml(rawText)
        val textSpan = "<span style=\"$textStyle\">$escapedText</span>"

        // Float spans + text span. buildFloatSpansHtml also flips
        // hasAppendFloatSpans to true internally.
        view.spanHtml = buildFloatSpansHtml(view) + textSpan
        // Write to rich-text `nodes` so the mini-app native component
        // picks up the reserved blank on the last line.
        view.ele.setAttribute("nodes", view.divHtml)
        return true
    }

    /**
     * Minimal HTML escape for plain text content before embedding into
     * a rich-text `nodes` string.
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    /**
     * Build the two right-floating span HTML snippets used to reserve
     * `lineBreakMargin` blank on the right side of the last visible line of
     * a rich-text. Equivalent to H5's `createFloatSpan` pair.
     */
    private fun buildFloatSpansHtml(view: KRRichTextView): String {
        val lineBreakMargin = view.getLineBreakMargin()
        val measureResult = view.getMeasureResult()
        if (lineBreakMargin <= 0f || measureResult.height <= 0f) {
            return ""
        }
        val singleLineHeight = view.getSingleLineHeight()
        val upperSpanHeight = (measureResult.height - singleLineHeight).coerceAtLeast(0f)
        // First span: occupies the full height of the lines above the last one,
        // zero width, floats right. Second span: `lineBreakMargin`px wide, 1px
        // tall, also floats right. Together they push the last line's text out
        // by `lineBreakMargin` on the right side.
        val style1 = "float:right;clear:right;width:0px;height:${upperSpanHeight}px;"
        val style2 = "float:right;clear:right;width:${lineBreakMargin}px;height:1px;"
        view.setHasAppendFloatSpans(true)
        return "<span style=\"$style1\"></span><span style=\"$style2\"></span>"
    }

    /**
     * Rich text content setting, here we need to calculate the overall width and height of the rich text,
     * the calculation method is quite complex:
     * 1. The width of each span added together. For placeholder spans, additional height needs to be calculated,
     *    but note that if the placeholder spans are within one line, then multiple height additions are not needed.
     * 2. For single spans, if there are line breaks, they also need to be processed.
     * 3. Use constraint width to calculate single line width, if it exceeds constraints then line break is needed.
     * 4. Also need to record the height of each line, because if a specific number of lines is specified,
     *    when some lines need to be omitted, the height of the preserved lines needs to be calculated.
     */
    override fun setRichTextValues(richTextValues: JSONArray, view: KRRichTextView) {
        // Clear all child nodes
        clearRichTextValues(view)
        // Reset float-span flag; it will be re-set inside buildFloatSpansHtml
        // when we actually inject the reserved blank on this layout pass.
        view.setHasAppendFloatSpans(false)

        for (i in 0 until richTextValues.length()) {
            val span = createSpan(richTextValues.optJSONObject(i) ?: JSONObject(), view)
            if (span.textContent != null) {
                // Save child span nodes
                view.childSpanList.add(span)
                val fontSize = span.style.fontSize.asDynamic().split("px")[0].unsafeCast<String>()
                if (span.offsetWidth != 0f) {
                    // If there is offsetWidth, it's a placeholder span case, save width and height
                    view.richTextSpanList.add(
                        RichTextSpan(
                            width = span.offsetWidth,
                            height = span.offsetHeight
                        )
                    )
                    // Placeholder image count plus 1
                    view.imageSpanCount += 1
                    // Placeholder span input
                    view.imageSpanList.add(span)
                } else {
                    // For non-placeholder spans, save the text content value.
                    // Also read back each span's own letter-spacing from its
                    // inline style — it has been set in `createSpan` per-span —
                    // so the measurement phase can apply the correct spacing
                    // for each segment instead of relying only on the
                    // RichText root container's letter-spacing.
                    val spanLetterSpacingStr = span.style.letterSpacing
                    val spanLetterSpacing = if (spanLetterSpacingStr.isNotEmpty()) {
                        spanLetterSpacingStr.pxToFloat()
                    } else {
                        0f
                    }
                    // Read each span's own line-height (set in createSpan when
                    // the business specifies LINE_HEIGHT). Carrying it into
                    // RichTextSpan lets the measurement phase honor a
                    // per-span line-height that differs from the container's,
                    // otherwise multi-line height would be underestimated.
                    val spanLineHeightStr = span.style.lineHeight
                    val spanLineHeight = if (spanLineHeightStr.isNotEmpty()) {
                        spanLineHeightStr.pxToFloat()
                    } else {
                        0f
                    }
                    view.richTextSpanList.add(
                        RichTextSpan(
                            value = span.textContent!!,
                            fontSize = fontSize.toFloat(),
                            fontWeight = span.style.fontWeight.toInt(),
                            fontFamily = span.style.fontFamily,
                            fontStyle = span.style.fontStyle,
                            letterSpacing = spanLetterSpacing,
                            lineHeight = spanLineHeight
                        )
                    )
                }
            }
        }
        // Calculate span innerHTML, prepending the two right-floating spans
        // that reserve `lineBreakMargin` blank on the last visible line.
        view.spanHtml = buildFloatSpansHtml(view) + getChildSpanHtml(view)
        // Set rich text content
        view.ele.setAttribute("nodes", view.divHtml)
    }

    /**
     * Return "offsetLeft offsetTop width height" for a placeholder span at [index].
     * Return "" to let the caller fall back to the default DOM-based measurement (H5).
     */
    override fun getPlaceholderSpanRect(index: Int, view: KRRichTextView): String {
        if (index < 0 || index >= view.richTextSpanList.length) return ""
        val span = view.richTextSpanList[index]
        // Only placeholder spans have width != 0
        if (span.width == 0f) return ""
        return "${span.offsetLeft} ${span.offsetTop} ${span.width} ${span.height}"
    }

    /**
     * Insert placeholder image for span
     */
    fun insertPlaceHolderImageView(parentView: KRRichTextView, view: MiniElement, insertIndex: Int) {
        if (view.firstElementChild is MiniImageElement) {
            // Placeholder image
            val image = view.firstElementChild.unsafeCast<MiniImageElement>()
            try {
                // Set placeholder image
                val imgSpan = parentView.imageSpanList[insertIndex]
                val childSpan = parentView.childSpanList[parentView.childSpanList.indexOf(imgSpan)]
                    .unsafeCast<MiniSpanElement>()
                childSpan.textContent = "<img style='${
                    getPlaceHolderImageStyle(view)
                }' mode='${
                    (view.firstElementChild as MiniImageElement).mode
                }' src='${image.src}' />"
                // Set image container corner
                childSpan.style.borderTopLeftRadius = view.style.borderTopLeftRadius
                childSpan.style.borderTopRightRadius = view.style.borderTopRightRadius
                childSpan.style.borderBottomLeftRadius = view.style.borderBottomLeftRadius
                childSpan.style.borderBottomRightRadius = view.style.borderBottomRightRadius
                // Update rich text
                parentView.spanHtml = getChildSpanHtml(parentView)
                // First save rich text placeholder html content
                val richTextViewHtml = parentView.divHtml
                // Throttled execution update node html
                throttle(parentView) {
                    parentView.ele.setAttribute("nodes", richTextViewHtml)
                }
            } catch (e: dynamic) {
                // Set placeholder image exception
                Log.error("set placeholder image error: $e")
            }

        }
    }
}
