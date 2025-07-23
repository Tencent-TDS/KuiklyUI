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

package com.tencent.kuikly.core.render.android.expand.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.LeadingMarginSpan
import android.util.SizeF
import android.view.ViewGroup
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.IKuiklyRenderContextWrapper
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.adapter.TextPostProcessorInput
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.const.KRExtConst
import com.tencent.kuikly.core.render.android.const.KRViewConst
import com.tencent.kuikly.core.render.android.css.decoration.BoxShadow
import com.tencent.kuikly.core.render.android.css.ktx.*
import com.tencent.kuikly.core.render.android.expand.component.text.*
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderShadowExport
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONArray
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * KTV 富文本组件，支持简单文本和富文本两种模式
 */
class KRRichTextView(context: Context) : KRView(context) {

    private var richTextShadow: KRRichTextShadow? = null
    private var textLayout: Layout? = null
    private var isRichTextMode = false

    override val reusable: Boolean
        get() = true

    override fun setShadow(shadow: IKuiklyRenderShadowExport) {
        super.setShadow(shadow)
        richTextShadow = (shadow as KRRichTextShadow).also {
            isRichTextMode = it.isRichTextMode
        }
        initTextLayout(richTextShadow)
        invalidate()
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        initTextLayout(richTextShadow)
    }

    override fun resetShadow() {
        super.resetShadow()
        richTextShadow = null
        textLayout = null
        isRichTextMode = false
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            KRCssConst.CLICK -> {
                super.setProp(propKey, createRichTextClickProxy(propValue))
            }
            KRCssConst.BACKGROUND_IMAGE -> { // 文字渐变在textShadow实现
                true
            }
            else -> {
                super.setProp(propKey, propValue)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawText(canvas)
    }

    private fun drawText(canvas: Canvas) {
       textLayout?.also {
            canvas.save()
            canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
            it.draw(canvas)
            canvas.restore()
        }
    }

    private fun createRichTextClickProxy(propValue: Any): KuiklyRenderCallback {
        return object : KuiklyRenderCallback {
            val originClickCallback = propValue as KuiklyRenderCallback

            override fun invoke(result: Any?) {
                if (!isRichTextMode) {
                    originClickCallback.invoke(result)
                } else {
                    originClickCallback.invoke(createRichTextCallbackParams(result))
                }
            }
        }
    }

    private fun createRichTextCallbackParams(result: Any?): Map<String, Any> {
        val map = result as Map<String, Any>
        // 1.获取点击的坐标位置
        val x = kuiklyRenderContext.toPxF((map[KRViewConst.X] as? Float) ?: 0f)
        val y = kuiklyRenderContext.toPxF((map[KRViewConst.Y] as? Float) ?: 0f).toInt()

        // 2. 计算spanIndex
        var spanIndex = -1
        val line = textLayout?.getLineForVertical(y) ?: 0
        val lineLeft: Float = textLayout?.getLineLeft(line) ?: Float.MIN_VALUE
        val lineRight: Float = textLayout?.getLineRight(line) ?: Float.MAX_VALUE

        if (x < lineLeft || x > lineRight) { // 点击区域超出文本区域
            spanIndex = -1
        } else {
            val off = textLayout?.getOffsetForHorizontal(line, x) ?: 0
            (textLayout?.text as? Spanned)?.getSpans(off, off, FontWeightSpan::class.java)?.also {
                if (it.isNotEmpty()) {
                    spanIndex = it[0].index
                }
            }
        }

        // 3.添加spanIndex参数
        val resultMap = mutableMapOf<String, Any>()
        resultMap.putAll(map)
        resultMap["index"] = spanIndex
        return resultMap
    }

    private fun initTextLayout(richTextShadow: KRRichTextShadow?) {
        val textShadow = richTextShadow ?: return
        textLayout = tryReMeasureTextLayout(textShadow, layoutParams)
    }

    private fun tryReMeasureTextLayout(textShadow: KRRichTextShadow, layoutParams: ViewGroup.LayoutParams?): Layout? {
        val lp = layoutParams ?: return textShadow.textLayout

        if (needReMeasureTextLayout(lp, textShadow)) {
            textShadow.measureLayoutExactly(SizeF(lp.width.toFloat(), lp.height.toFloat()))
        }
        return textShadow.textLayout
    }

    /**
     * 是否需要重新计算文本布局
     * @param layoutParams HRRichTextView的布局参数
     * @param textShadow 文本测量类
     * @return 是否需要重新计算文本。只有当layoutParams.width != textLayout.width才会重新计算
     */
    private fun needReMeasureTextLayout(
        layoutParams: ViewGroup.LayoutParams,
        textShadow: KRRichTextShadow
    ): Boolean {
        val textLayout = textShadow.textLayout
        if (textLayout == null || layoutParamsNotHasSize(layoutParams)) {
            return false
        }
        return layoutParams.width != textLayout.width
    }

    private fun layoutParamsNotHasSize(params: ViewGroup.LayoutParams): Boolean =
        params.width == ViewGroup.LayoutParams.MATCH_PARENT || params.width == ViewGroup.LayoutParams.WRAP_CONTENT
                || params.width == 0

    companion object {
        const val VIEW_NAME = "KRRichTextView"
        const val GRADIENT_RICH_TEXT_VIEW = "KRGradientRichTextView"
    }
}

/**
 * 文本相关属性
 */
open class KRTextProps(private val kuiklyContext: IKuiklyRenderContext?) {

    companion object {
        const val PROP_KEY_NUMBER_OF_LINES = "numberOfLines"
        const val PROP_KEY_LINE_BREAK_MODE = "lineBreakMode"
        const val PROP_KEY_LINE_BREAK_MARGIN = "lineBreakMargin"
        const val PROP_KEY_VALUES = "values"
        const val PROP_KEY_TEXT = "text"
        const val PROP_KEY_VALUE = "value"
        const val PROP_KEY_TEXT_ALIGN = "textAlign"
        const val PROP_KEY_LINE_SPACING = "lineSpacing"
        const val PROP_KEY_LETTER_SPACING = "letterSpacing"
        const val PROP_KEY_COLOR = "color"
        const val PROP_KEY_FONT_SIZE = "fontSize"
        const val PROP_KEY_TEXT_DECORATION = "textDecoration"
        const val PROP_KEY_FONT_WEIGHT = "fontWeight"
        const val PROP_KEY_FONT_STYLE = "fontStyle"
        const val PROP_KEY_FONT_FAMILY = "fontFamily"
        const val PROP_KEY_FONT_VARIANT = "fontVariant"
        const val PROP_KEY_HEAD_INDENT = "headIndent"
        const val PROP_KEY_LINE_HEIGHT = "lineHeight"
        const val PROP_KEY_BACKGROUND_IMAGE = KRCssConst.BACKGROUND_IMAGE
        const val PROP_KEY_TEXT_SHADOW = "textShadow"
        const val PROP_KEY_TEXT_POST_PROCESSOR = "textPostProcessor"
        const val PROP_KEY_TEXT_USE_DP_FONT_SIZE_DIM = "useDpFontSizeDim"

        const val FONT_STYLE_ITALIC = "italic"

        const val TEXT_DECORATION_UNDERLINE = "underline"
        const val TEXT_DECORATION_LINE_THROUGH = "line-through"

        const val TEXT_ALIGN_CENTER = "center"
        const val TEXT_ALIGN_RIGHT = "right"

        const val DEFAULT_FONT_SIZE = 13f
        const val UNSET_LINE_HEIGHT = -1f
    }

    /**
     * 文本行数限制
     */
    var numberOfLines = 0

    /**
     * 文本换行策略
     */
    var lineBreakMode = KRCssConst.EMPTY_STRING

    /**
     * 富文本JSON描述数据
     */
    var values: JSONArray? = null

    /**
     * 简单文本
     */
    var text = KRCssConst.EMPTY_STRING

    /**
     * 行间距
     */
    var lineSpacing = 1f

    /**
     * 字体装饰。如字体下划线
     */
    var textDecoration = KRCssConst.EMPTY_STRING

    /**
     * 字体对齐。包括: 左对齐、居中对齐、右对齐
     */
    var textAlign = KRCssConst.EMPTY_STRING

    /**
     * 换行前空出的间距
     */
    var lineBreakMargin = 0f

    /**
     * 字号
     */
    var fontWeight = KRCssConst.EMPTY_STRING

    /**
     * 字体样式，例如斜体
     */
    var fontStyle = Typeface.NORMAL

    /**
     * 字体名字
     */
    var fontFamily = KRCssConst.EMPTY_STRING

    /**
     * 字符间距
     */
    var letterSpacing = 0f

    /**
     * 字体颜色
     */
    var color = Color.BLACK

    /**
     * 字体大小
     */
    var fontSize = DEFAULT_FONT_SIZE

    /**
     * 字体行高
     */
    var lineHeight = UNSET_LINE_HEIGHT

    /**
     * 首行缩进
     */
    var richTextHeadIndent = 0

    var backgroundImage = ""

    /**
     * 字体阴影
     */
    var textShadow: BoxShadow? = null

    /**
     * 文本自定义后置处理器标识
     */
    var textPostProcessor = ""

    /**
     * 是否使用dp来作为字体单位
     */
    var useDpFontSizeDim = false

    /**
     * LineBreakMargin属性是否触发
     */
    var isLineBreakMargin = false

    /**
     * 设置文本属性
     */
    fun setProp(propKey: String, propValue: Any) {
        when (propKey) {
            PROP_KEY_NUMBER_OF_LINES -> numberOfLines = propValue as Int
            PROP_KEY_LINE_BREAK_MARGIN -> lineBreakMargin = kuiklyContext.toPxF(propValue.toNumberFloat())
            PROP_KEY_LINE_BREAK_MODE -> lineBreakMode = propValue as String
            PROP_KEY_VALUES -> values = JSONArray(propValue as String)
            PROP_KEY_TEXT -> text = propValue as String
            PROP_KEY_COLOR -> color = (propValue as String).toColor()
            PROP_KEY_LETTER_SPACING -> letterSpacing = propValue.toNumberFloat()
            PROP_KEY_TEXT_DECORATION -> textDecoration = propValue as String
            PROP_KEY_TEXT_ALIGN -> textAlign = propValue as String
            PROP_KEY_LINE_SPACING -> lineSpacing = kuiklyContext.toPxI(propValue.toNumberFloat()).toFloat()
            PROP_KEY_FONT_WEIGHT -> fontWeight = propValue as String
            PROP_KEY_FONT_STYLE -> {
                fontStyle = if (propValue as String == FONT_STYLE_ITALIC) {
                    Typeface.ITALIC
                } else {
                    Typeface.NORMAL
                }
            }
            PROP_KEY_FONT_FAMILY -> fontFamily = propValue as String
            PROP_KEY_FONT_SIZE -> fontSize = propValue.toNumberFloat()
            PROP_KEY_LINE_HEIGHT -> lineHeight = kuiklyContext.toPxF(propValue.toNumberFloat())
            PROP_KEY_BACKGROUND_IMAGE -> backgroundImage = propValue as String
            PROP_KEY_HEAD_INDENT -> richTextHeadIndent = kuiklyContext.toPxI(propValue.toNumberFloat())
            PROP_KEY_TEXT_SHADOW -> {
                if (propValue is String) {
                    textShadow = BoxShadow(propValue, kuiklyContext)
                }
            }
            PROP_KEY_TEXT_POST_PROCESSOR -> textPostProcessor = propValue as String
            PROP_KEY_TEXT_USE_DP_FONT_SIZE_DIM -> useDpFontSizeDim = (propValue as Int) == 1
        }
    }

    /**
     * 是否为富文本
     */
    fun isRichText(): Boolean {
        return (values?.length() ?: 0) > 0
    }

}

/**
 * 富文本shadow对象，用于在子线程提前测量文本
 */
class KRRichTextShadow : IKuiklyRenderShadowExport, IKuiklyRenderContextWrapper {

    /**
     * 文本属性
     */
    private var textProps = KRTextProps(null)

    /**
     * 文本Layout, 目前实现为StaticLayout
     */
    var textLayout: Layout? = null

    /**
     * 是否是富文本形式
     */
    var isRichTextMode = false	

    private val textPaint by lazy {
        // kuiklyRenderContext是在KRRichTextShadow创建后才赋值的，因此这里需要 lazy，保证获取textPaint
        // 时，kuiklyRenderContext已经初始化
        TextPaint().apply {
            isAntiAlias = true
            isDither = true
            color = Color.BLACK
            textSize = kuiklyRenderContext.spToPxI(textProps.fontSize).toFloat()
        }
    }

    /**
     * 用于记录富文本中 DSL Span 对应的文本区间
     */
    private var spanTextRanges: MutableList<SpanTextRange> = mutableListOf()

    /**
     * 上下文对象[IKuiklyRenderContext]
     */
    override var kuiklyRenderContext: IKuiklyRenderContext? = null

    /**
     *
     * <p>这里为啥不用使用map<key, handler>来处理?
     *
     * <p>1.属性不会太多, 使用when语句的可读性比map<key，handler>的方式好
     *
     * <p>2.一般只有维护者一人编写
     *
     * <p>3.降低内存开销
     *
     * <p>这里的value类型是与kuiklyCore侧约定好的，因此没判断就使用强转
     *
     * @param propKey 属性key
     * @param propValue 属性值
     * @return 是否处理
     */
    override fun setProp(propKey: String, propValue: Any) {
        textProps.setProp(propKey, propValue)
    }

    override fun call(methodName: String, params: String): Any? {
        when(methodName) {
            METHOD_GET_PLACEHOLDER_SPAN_RECT -> {
                val index = params.toInt()
                val spanRect = getPlaceholderSpanRect(index)
                return "${spanRect.left} ${spanRect.top} ${spanRect.width()} ${spanRect.height()}"
            }
            METHOD_IS_LINE_BREAK_MARGIN -> {
                return if (textProps.isLineBreakMargin) "1" else "0"
            }
        }
        return null
    }

    /**
     * 根据 index 获取 PlaceholderSpan 的绘制区域
     */
    private fun getPlaceholderSpanRect(index: Int) : Rect {
        var rect = Rect(0, 0, 0, 0)
        textLayout?.let { layout ->
            var phSpanTextRange: SpanTextRange? = spanTextRanges.find { it.index == index }

            if (phSpanTextRange != null) {

                // layout可能会因为行高等因素导致文本截断
                // 检查PlaceholderSpan的索引是否在文本布局范围内
                val maxIndex = layout.getLineEnd(layout.lineCount - 1)
                // 需要考虑以...结尾等截断的情况，避免PlaceholderSpan和截断字符串重叠显示
                val ellipsisCount = layout.getEllipsisCount(layout.lineCount - 1)
                if (phSpanTextRange.start >= (maxIndex - ellipsisCount)) {
                    return rect
                }

                val text = layout.text
                if (text is Spanned) {
                    val textStartIndex = phSpanTextRange.start
                    val textEndIndex = phSpanTextRange.end
                    val phSpans = text.getSpans<KRPlaceholderSpan>(textStartIndex, textEndIndex,  KRPlaceholderSpan::class.java)
                    if (phSpans.size == 1) {
                        val phWidth = phSpans[0].width()
                        val phHeight = phSpans[0].height()
                        // 水平方向坐标
                        val x = layout.getPrimaryHorizontal(textStartIndex)
                        // 垂直方向坐标
                        val lineNum = layout.getLineForOffset(textStartIndex)
                        val top = layout.getLineTop(lineNum)
                        val bottom = layout.getLineBottom(lineNum)
                        // 获取行间距
                        val fm = FontMetricsInt()
                        layout.paint.getFontMetricsInt(fm)
                        var lineSpace = 0
                        if (lineNum < layout.lineCount - 1){//默认最后一行没有行间距，无需减去行间距。注意：后续如果text支持了多倍行距或setAddLastLineLineSpacing，这里需要适配
                            lineSpace = layout.spacingAdd.toInt()
                        }
                        // 行垂直中点
                        val lineMid = top + (bottom - lineSpace - top) / 2
                        // placeholder 矩形
                        rect.left = kuiklyRenderContext.toDpI(x)
                        rect.right = kuiklyRenderContext.toDpI(x + phWidth)
                        rect.top = kuiklyRenderContext.toDpI((lineMid - phHeight / 2).toFloat())
                        rect.bottom = kuiklyRenderContext.toDpI((lineMid + phHeight / 2).toFloat())

                    }
                }
            }
        }
        return rect
    }

    override fun calculateRenderViewSize(constraintSize: SizeF): SizeF {
        val layout = buildLayout(constraintSize, TextMeasureMode.AT_MOST)
        textLayout = layout
        return if (layout == null) {
            SizeF(0f, 0f)
        } else {
            SizeF(layout.width.toFloat(), layout.height.toFloat())
        }
    }

    /**
     * 使用[TextMeasureMode.EXACTLY]测量文本, 文本宽度为[constraintSize.width]
     * @param constraintSize 指定的文本大小
     */
    fun measureLayoutExactly(constraintSize: SizeF) {
        textLayout = buildLayout(constraintSize, TextMeasureMode.EXACTLY)
    }

    private fun buildLayout(constraintSize: SizeF, measureMode: TextMeasureMode): Layout? {
        val text = when {
            textProps.isRichText() -> {
                isRichTextMode = true
                buildRichText()
            }
            textProps.text.isNotEmpty() -> {
                isRichTextMode = false
                buildSimpleText()
            }
            else -> {
                null
            }
        }
        text?.also {
            return createLayout(it, constraintSize, measureMode)
        }
        return null
    }

    private fun buildSimpleText(): SpannableStringBuilder {
        when (textProps.textDecoration) {
            KRTextProps.TEXT_DECORATION_UNDERLINE -> textPaint.isUnderlineText = true
            KRTextProps.TEXT_DECORATION_LINE_THROUGH -> textPaint.isStrikeThruText = true
        }
        if (textProps.fontWeight.isNotEmpty()) {
            textPaint.style = Paint.Style.FILL_AND_STROKE
            textPaint.strokeWidth = FontWeightSpan.getFontWeight(textProps.fontWeight)
        }
        textPaint.typeface = kuiklyRenderContext?.getTypeFaceLoader()?.getTypeface(textProps.fontFamily,
            textProps.fontStyle == Typeface.ITALIC)
        textPaint.textSize = if (textProps.useDpFontSizeDim) {

            kuiklyRenderContext.toPxI(textProps.fontSize).toFloat()
        } else {
            kuiklyRenderContext.spToPxI(textProps.fontSize).toFloat()
        }
        textPaint.color = textProps.color
        textPaint.letterSpacing = kuiklyRenderContext.toPxF(textProps.letterSpacing) / max(textPaint.textSize, 1f)
        val simpleText = SpannableStringBuilder(textProps.text)
        if (textProps.lineHeight != KRTextProps.UNSET_LINE_HEIGHT) {
            simpleText.setSpan(
                HRLineHeightSpan(textProps.lineHeight.toInt()),
                0,
                simpleText.length,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
        if (textProps.backgroundImage.isNotEmpty()) {
            simpleText.setSpan(
                createLinearGradientForegroundSpan(textProps.backgroundImage),
                0,
                simpleText.length,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
        if (textProps.richTextHeadIndent != 0) {
            simpleText.setSpan(
                LeadingMarginSpan.Standard(textProps.richTextHeadIndent, 0),
                0,
                simpleText.length,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        textProps.textShadow?.let {
            if (!it.isEmpty()) {
                simpleText.setSpan(
                    TextShadowSpan(
                        it.shadowOffsetX,
                        it.shadowOffsetY,
                        it.shadowRadius,
                        it.shadowColor
                    ),
                    0, simpleText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
        }
        return simpleText
    }

    private fun buildRichText(): SpannableStringBuilder? {
        val newSpanTextRanges = mutableListOf<SpanTextRange>()
        val richTextBuilder = KRRichTextBuilder(kuiklyRenderContext)
        val result = richTextBuilder.build(textProps, newSpanTextRanges) {
            val layout = textLayout
            if (layout == null) {
                SizeF(0f, 0f)
            } else {
                SizeF(layout.width.toFloat(), layout.height.toFloat())
            }
        }
        spanTextRanges = newSpanTextRanges
        return result
    }

    private fun getTextAlign(): Layout.Alignment {
        return when (textProps.textAlign) {
            KRTextProps.TEXT_ALIGN_CENTER -> Layout.Alignment.ALIGN_CENTER
            KRTextProps.TEXT_ALIGN_RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
    }

    private fun createLayout(
        text: SpannableStringBuilder,
        constraintSize: SizeF,
        measureMode: TextMeasureMode
    ): Layout {
        val textSource = if (textProps.textPostProcessor.isNotEmpty()) {
            KuiklyRenderAdapterManager.krTextPostProcessorAdapter?.onTextPostProcess(
                kuiklyRenderContext, TextPostProcessorInput(textProps.textPostProcessor, text, textProps)
            )?.text ?: text
        } else {
            text
        }
        val desiredWidth = getDesiredWith(textSource, constraintSize, measureMode)
        if (!isBeforeM) {
            val builder = createStaticLayoutBuilder(textSource, desiredWidth)
            if (textProps.numberOfLines > 0 && textProps.lineBreakMargin == 0f) {
                builder.setMaxLines(textProps.numberOfLines)
                    .setEllipsize(TextUtils.TruncateAt.END)
            }
            return if (textProps.lineBreakMargin == 0f || textProps.numberOfLines == 0) {
                builder.build()
            } else {
                val staticLayout = builder.build()
                if (staticLayout.lineCount > textProps.numberOfLines) {
                    val newBuilder = createStaticLayoutBuilder(textSource, desiredWidth)
                    newBuilder.setMaxLines(textProps.numberOfLines)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .setIndents(null, createLineBreakMarginArray(textProps))
                    textProps.isLineBreakMargin = true
                    newBuilder.build()
                } else {
                    staticLayout
                }
            }
        } else {
            var staticLayout = StaticLayout(textSource, 0, textSource.length, textPaint, desiredWidth, getTextAlign(), 1.0f, textProps.lineSpacing, false)
            if (textProps.numberOfLines != 0 && staticLayout.lineCount > textProps.numberOfLines) {
                val endLine = staticLayout.getLineEnd(staticLayout.lineCount - 1)
                val extraMargin = if (textProps.lineBreakMargin != 0f) {
                    textProps.isLineBreakMargin = true
                    textProps.lineBreakMargin
                } else {
                    0f
                }
                val newText = TextUtils.ellipsize(textSource.subSequence(0, endLine), textPaint, (desiredWidth * textProps.numberOfLines - extraMargin), TextUtils.TruncateAt.END)
                staticLayout = StaticLayout(newText, 0, newText.length, textPaint, desiredWidth, getTextAlign(), 1.0f, textProps.lineSpacing, false)
            }
            return staticLayout
        }
    }

    @SuppressLint("NewApi")
    private fun createStaticLayoutBuilder(textSource: CharSequence, desiredWidth: Int): StaticLayout.Builder {
        return StaticLayout.Builder.obtain(textSource,
            0,
            textSource.length,
            textPaint,
            desiredWidth)
            .setAlignment(getTextAlign())
            .setTextDirection(TextDirectionHeuristics.LTR)
            .setLineSpacing(textProps.lineSpacing, 1.0f)
            .setIncludePad(false)
    }

    private fun createLineBreakMarginArray(textProps: KRTextProps): IntArray {
        val maxLines = textProps.numberOfLines
        val array = IntArray(maxLines)
        for (i in 0 until maxLines) {
            if (i == maxLines - 1) {
                array[i] = textProps.lineBreakMargin.toInt()
            } else {
                array[i] = 0
            }
        }
        return array
    }

    private fun getDesiredWith(
        text: CharSequence,
        constraintSize: SizeF,
        measureMode: TextMeasureMode
    ): Int {
        return when (measureMode) {
            TextMeasureMode.EXACTLY -> constraintSize.width.toInt() // 准确模式, 返回父亲给的宽度
            TextMeasureMode.AT_MOST -> getTextWidthAtMost(text, constraintSize) // 适应文本模式, 返回文本宽度
        }
    }

    private fun getTextWidthAtMost(text: CharSequence, constraintSize: SizeF): Int {
        val w = Layout.getDesiredWidth(text, textPaint)
        return if (w < constraintSize.width || constraintSize.width == 0f) {
            (w + KRExtConst.ROUND_SCALE_VALUE).roundToInt()
        } else {
            constraintSize.width.toInt()
        }
    }

    private fun createLinearGradientForegroundSpan(backgroundImage: String): LinearGradientForegroundSpan {
        return LinearGradientForegroundSpan(backgroundImage) {
            val layout = textLayout
            if (layout == null) {
                SizeF(0f, 0f)
            } else {
                SizeF(layout.width.toFloat(), layout.height.toFloat())
            }
        }
    }

    /**
     * 文本测量模式
     */
    private enum class TextMeasureMode {
        EXACTLY, // 准确测量模式
        AT_MOST, // 适应文本的真正宽度
    }

    companion object {
        private const val METHOD_GET_PLACEHOLDER_SPAN_RECT = "spanRect"
        private const val METHOD_IS_LINE_BREAK_MARGIN = "isLineBreakMargin"
    }
}
