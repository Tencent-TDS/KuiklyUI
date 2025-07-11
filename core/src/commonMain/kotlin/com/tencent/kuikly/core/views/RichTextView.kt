/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.EdgeInsets
import com.tencent.kuikly.core.base.ScopeMarker
import com.tencent.kuikly.core.base.Size
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.IImageAttr
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.base.domChildren
import com.tencent.kuikly.core.base.event.ClickParams
import com.tencent.kuikly.core.base.event.addLayoutFrameDidChange
import com.tencent.kuikly.core.base.isVirtualView
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.FlexPositionType
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.MeasureFunction
import com.tencent.kuikly.core.layout.MeasureOutput
import com.tencent.kuikly.core.layout.isUndefined
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.nvi.serialization.serialization
import com.tencent.kuikly.core.reactive.ReactiveObserver
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.shadow.RichTextShadow

fun ViewContainer<*, *>.RichText(init: RichTextView.() -> Unit) {
    var richTextView = createViewFromRegister(ViewConst.TYPE_RICH_TEXT_CLASS_NAME) as? RichTextView
    if (richTextView == null) {
        richTextView = RichTextView()
    }
    addChild(richTextView) {
        ReactiveObserver.bindValueChange(this) {
            getViewAttr().resetSpans()
            isWillInit = true
            init()
            isWillInit = false
            flexNode.markDirty()
        }
        attrInitBlock?.also {
            attr(it)
        }
    }
}

fun RichTextView.Span(textSpanInit: TextSpan.() -> Unit) {
    val textSpan = TextSpan()
    textSpan.pagerId = pagerId
    textSpan.textSpanInit()
    getViewAttr().addSpan(textSpan)
}

/**
 * 文本中插入空白占位Span，监听SpanFrame变化来配合叠加其他任意View使用
 */
fun RichTextView.PlaceholderSpan(spanInit: PlaceholderSpan.() -> Unit) {
    val placeholderSpan = PlaceholderSpan()
    placeholderSpan.spanInit()
    getViewAttr().addSpan(placeholderSpan)
}

/**
 * 文本中插入 Image
 */
fun RichTextView.ImageSpan(spanInit: ImageSpan.() -> Unit) {
    val imageSpan = ImageSpan()
    imageSpan.spanInit()
    imageSpan.build(this)
    getViewAttr().addSpan(imageSpan)
}

open class RichTextView : DeclarativeBaseView<RichTextAttr, RichTextEvent>(),
    MeasureFunction {
    var shadow: RichTextShadow? = null
    private var didLayout = false
    internal var attrInitBlock: (RichTextAttr.() -> Unit)? = null
    internal var isWillInit = false
    override fun willInit() {
        super.willInit()
        shadow = RichTextShadow(pagerId, nativeRef, viewName())
    }

    override fun attr(init: RichTextAttr.() -> Unit) {
        if (isWillInit) {
            attrInitBlock = init
        } else {
            super.attr(init)
        }
    }

    internal fun markDirty(){
        flexNode.markDirty()
        shadow?.markDirty()
    }

    override fun didSetProp(propKey: String, propValue: Any) {
        if (canSyncToRenderView(propKey)) {
            super.didSetProp(propKey, propValue)
        }
        if (isShadowProp(propKey)) {
            shadow?.setProp(propKey, propValue)
            flexNode.markDirty()
        }
    }

    override fun syncProp(propKey: String, propValue: Any) {
        if (canSyncToRenderView(propKey)) {
            super.syncProp(propKey, propValue)
        }
    }

    private fun canSyncToRenderView(propKey: String): Boolean {
        if (propKey == TextConst.VALUE
            || propKey == TextConst.VALUES
            || propKey == TextConst.TEXT_COLOR
            || propKey == TextConst.FONT_SIZE
        ) {
            return false
        }
        return true
    }

    private fun isShadowProp(propKey: String): Boolean {
        if (propKey == Attr.StyleConst.TRANSFORM
            || propKey == Attr.StyleConst.OPACITY
            || propKey == Attr.StyleConst.VISIBILITY
            || propKey == Attr.StyleConst.BACKGROUND_COLOR
        ) {
            return false
        }
        return true
    }

    override fun createEvent(): RichTextEvent {
        return RichTextEvent()
    }

    override fun createFlexNode() {
        super.createFlexNode()
        flexNode.measureFunction = this
    }

    override fun createRenderView() {
        super.createRenderView()
        if (didLayout) {
            renderView?.setShadow()
        }
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        flexNode.measureFunction = null
        shadow?.removeFromParentComponent()
        shadow = null
        attrInitBlock = null
    }

    override fun createAttr(): RichTextAttr {
        return RichTextAttr()
    }

    override fun viewName(): String {
        return if (attr.didSetTextGradient) {
            ViewConst.TYPE_GRADIENT_RICH_TEXT
        } else {
            ViewConst.TYPE_RICH_TEXT
        }
    }

    fun updateShadow() {
        if (shadow?.calculateFromCache != true) {
            renderView?.setShadow()
        }
    }

    override fun measure(
        node: FlexNode,
        width: Float,
        height: Float,
        measureOutput: MeasureOutput
    ) {
        val cHeight = measureHeightToFloat(height)
        val cWidth = measureWidthToFloat(width)
        shadow?.setValuesProp(buildValuesPropValue())
        var size = shadow?.calculateRenderViewSize(cWidth, cHeight) ?: Size(0f, 0f)
        if (flexNode.flex != 0f && flexNode.positionType == FlexPositionType.RELATIVE) {
            size = flexLayoutSize(size ?: Size(0f, 0f), width, height)
        }
        if (!flexNode.styleWidth.isUndefined()) {
            size = Size(flexNode.styleWidth, size.height)
        }
        if (!flexNode.styleMinWidth.isUndefined()) {
            size = widthLayoutSize(size, flexNode.styleMinWidth, height)
        }
        if (!flexNode.styleHeight.isUndefined()) {
            size = Size(size.width, flexNode.styleHeight)
        }
        if (!flexNode.styleMinHeight.isUndefined()) {
            size = heightLayoutSize(size, width, flexNode.styleMinHeight)
        }
        didLayout = true
        if (shadow?.calculateFromCache != true) {
            renderView?.setShadow()
        }
        measureOutput.width = size!!.width
        measureOutput.height = size!!.height
        dispatchPlaceholderSpanLayoutEventIfNeed()
        tryFireLineBreakMarginEvent()
    }

    private fun tryFireLineBreakMarginEvent() {
        if (attr.getProp(TextConst.LINE_BREAK_MARGIN) != null) {
            getPager().addTaskWhenPagerDidCalculateLayout {
                val isLineBreakMargin = shadow?.callMethod(TextConst.SHADOW_METHOD_IS_LINE_BREAK_MARGIN, "") == "1"
                if (isLineBreakMargin) {
                    event.handler?.invoke(null)
                }
            }
        }
    }

    fun buildValuesPropValue(): String {
        val values = arrayListOf<Map<String, Any>>()
        attr.spans.forEach { child ->
            val props = hashMapOf<String, Any>()
            child.spanPropsMap().also {
                props.putAll(it)
            }
            if (!props.isEmpty()) {
                values.add(props)
            }
        }
        return values.serialization().toString()
    }

    private fun measureHeightToFloat(height: Float): Float {
        return if (height.isUndefined()) {
            -1f
        } else {
            height
        }
    }

    private fun measureWidthToFloat(width: Float): Float {
        return if (width.isUndefined()) {
            100000f
        } else {
            width
        }
    }

    private fun flexLayoutSize(measureOutputSize: Size, fitWidth: Float, fitHeight: Float): Size {
        val flexDirection = flexNode.parent?.flexDirection
        if (flexDirection == FlexDirection.ROW || flexDirection == FlexDirection.ROW_REVERSE) {
            if (fitWidth.isUndefined()) {
                return measureOutputSize
            }
            var outWidth = measureOutputSize.width
            if (outWidth < fitWidth) outWidth = fitWidth
            return Size(outWidth, measureOutputSize.height)
        } else {
            if (fitHeight.isUndefined()) {
                return measureOutputSize
            }
            var outHeight = measureOutputSize.height
            if (outHeight < fitHeight) outHeight = fitHeight
            return Size(measureOutputSize.width, outHeight)
        }
    }

    private fun widthLayoutSize(measureOutputSize: Size, fitWidth: Float, fitHeight: Float): Size {
        if (fitWidth.isUndefined()) {
            return measureOutputSize
        }
        var outWidth = measureOutputSize.width
        if (outWidth < fitWidth) outWidth = fitWidth
        return Size(outWidth, measureOutputSize.height)
    }

    private fun heightLayoutSize(measureOutputSize: Size, fitWidth: Float, fitHeight: Float): Size {
        if (fitHeight.isUndefined()) {
            return measureOutputSize
        }
        var outputHeight = measureOutputSize.height
        if (outputHeight < fitHeight) outputHeight = fitHeight
        return Size(measureOutputSize.width, outputHeight)
    }

    // 分发span布局位置变化
    private fun dispatchPlaceholderSpanLayoutEventIfNeed() {
        attr.spans.forEach { child ->
            if (child is PlaceholderSpan && child.spanFrameDidChangedHandlerFn != null) {
                val placeholderSpan = child
                getPager().addTaskWhenPagerUpdateLayoutFinish {
                    val index = attr.spans.indexOf(placeholderSpan)
                    if (index >= 0) {
                        val rectStr = shadow?.callMethod("spanRect", index.toString())
                        if (rectStr?.isNotEmpty() == true) {
                            rectStr.split(" ").apply {
                                if (this.size >= 4) {
                                    placeholderSpan.spanFrame = Frame(
                                        this[0].toFloatOrNull() ?: 0f,
                                        this[1].toFloatOrNull() ?: 0f,
                                        this[2].toFloatOrNull() ?: 0f,
                                        this[3].toFloatOrNull() ?: 0f
                                    )
                                } else {
                                    KLog.e("KuiklyCore", "spanRect result is error:${rectStr}")
                                }

                            }
                        }
                    }
                }
            }
        }
    }

}

open class RichTextAttr : TextAttr() {
    internal var spans: ArrayList<ISpan> = arrayListOf()
    fun spans(spans: ArrayList<ISpan>) {
        this.spans = spans
        spans.forEach {
            addSpanClickIfNeed(it)
        }
        setNeedLayout()
    }

    fun getSpans(): Array<ISpan> {
        return spans.toTypedArray()
    }

    internal open fun resetSpans() {
        spans.forEach {
            it.willDestroy()
        }
        spans = arrayListOf()
    }

    override fun viewDidRemove() {
        super.viewDidRemove()
        resetSpans()
    }

    protected fun hasSpans(): Boolean {
        return spans.isNotEmpty()
    }

    internal  fun addSpan(span: ISpan) {
        if (span.isEmptySpan()) {
            return
        }
        spans.add(span)
        addSpanClickIfNeed(span)
    }

    internal fun addSpanClickIfNeed(span: ISpan) {
        if (span.hasClickEvent()) {
            val event = (view() as? RichTextView)?.getViewEvent()
            if (event?.hasInterceptClick() == true) {
                return
            }
            event?.interceptClick { clickParams ->
                val index = (clickParams.params as? JSONObject)?.optInt("index")
                if (index != null && spans.count() > index && index >= 0) {
                    return@interceptClick spans[index].performClickHandler(clickParams)
                }
                return@interceptClick false
            }
        }
    }
}

@ScopeMarker
interface ISpan {
    abstract fun isEmptySpan(): Boolean
    abstract fun spanPropsMap(): Map<String, Any>
    abstract fun performClickHandler(clickParams: ClickParams): Boolean
    abstract fun hasClickEvent(): Boolean
    abstract fun willDestroy()
}

open class TextSpan : TextAttr(), ISpan {
    internal var text: String = ""
    private var clickHandlerFn: ((ClickParams) -> Unit)? = null

    /**
     * 单击事件的定义
     * @param handler 事件处理函数
     */
    fun click(handler: (ClickParams) -> Unit) {
        clickHandlerFn = handler
    }

    override fun text(text: String): TextAttr {
        this.text = text
        return super.text(text)
    }

    override fun value(value: String): TextAttr {
        this.text = value
        return super.value(value)
    }

    override fun hasClickEvent(): Boolean {
        return clickHandlerFn != null
    }

    override fun performClickHandler(clickParams: ClickParams): Boolean {
        clickHandlerFn?.invoke(clickParams)
        return clickHandlerFn != null
    }

    // ISpan
    override fun isEmptySpan(): Boolean {
        return text.isEmpty()
    }

    // ISpan
    override fun spanPropsMap(): Map<String, Any> {
        return copyPropsMap()
    }

    override fun willDestroy() {

    }

    fun getText(): String {
        return text
    }
}

/*
 * 占位Span
 */
open class PlaceholderSpan : ISpan {

    companion object {
        const val PROP_KEY_PLACEHOLDER_WIDTH = "placeholderWidth"
        const val PROP_KEY_PLACEHOLDER_HEIGHT = "placeholderHeight"
        const val PROP_KEY_PLACEHOLDER_TEXT = "text"
    }

    private var placeholderSize: Size = Size(0f, 0f)
    internal var spanFrameDidChangedHandlerFn: ((Frame) -> Unit)? = null
    var spanFrame: Frame = Frame.zero
        set(value) {
            if (field.isDefaultValue() || !value.equals(field)) {
                field = value
                spanFrameDidChangedHandlerFn?.invoke(value)
            }
        }

    fun placeholderSize(width: Float, height: Float) {
        placeholderSize = Size(width, height)
    }

    fun spanFrameDidChanged(handler: (frame: Frame) -> Unit) {
        spanFrameDidChangedHandlerFn = handler
    }

    override fun isEmptySpan(): Boolean {
        return placeholderSize.width == 0f && placeholderSize.height == 0f
    }

    override fun spanPropsMap(): Map<String, Any> {
        return hashMapOf<String, Any>().apply {
            put(PROP_KEY_PLACEHOLDER_WIDTH, placeholderSize.width)
            put(PROP_KEY_PLACEHOLDER_HEIGHT, placeholderSize.height)
            put(PROP_KEY_PLACEHOLDER_TEXT, " ")
        }
    }

    override fun performClickHandler(clickParams: ClickParams): Boolean {
        return false
    }

    override fun hasClickEvent(): Boolean {
        return false
    }

    override fun willDestroy() {

    }
}

/**
 * ImageSpan 是通过在 Placeholder 贴一个 ImageView 实现的
 */
open class ImageSpan: PlaceholderSpan(), IImageAttr {

    private var size: Size = Size(0f, 0f)

    private var src: String = ""
    private var uri: ImageUri? = null

    private var placeholder: String = ""
    private var resizeMode: String = ""
    private var blurRadius: Float = 0f
    private var tintColor: Color? = null
    private var isDotNineImage: Boolean = false
    private var borderRadius = 0f
    private var capInsets: EdgeInsets = EdgeInsets.default
    private var verticalAlignOffset = 0f

    private var richTextFrame by observable(Frame.zero)
    private var placeholderFrame by observable(Frame.zero)
    private var view : ImageView? = null
    private var clickHandlerFn: ((ClickParams) -> Unit)? = null

    fun size(width: Float, height: Float): IImageAttr {
        size = Size(width, height)
        return this
    }

    fun borderRadius(borderRadius: Float): IImageAttr {
        this.borderRadius = borderRadius
        return this
    }
    /**
     * ImageSpan被单击时回调
     * @param handler 事件处理函数
     */
    fun click(handler: (ClickParams) -> Unit) {
        clickHandlerFn = handler
    }

    /**
     * 设置ImageSpan在垂直方向对齐的偏移，默认居中对齐
     */
    fun verticalAlignOffset(offset: Float) {
        this.verticalAlignOffset = offset
    }

    override fun src(src: String, isDotNineImage: Boolean): IImageAttr {
        this.src = src
        this.isDotNineImage = isDotNineImage
        this.uri = null
        return this
    }

    override fun src(uri: ImageUri, isDotNineImage: Boolean): IImageAttr {
        this.uri = uri
        this.isDotNineImage = isDotNineImage
        this.src = ""
        return this
    }

    override fun placeholderSrc(placeholder: String): IImageAttr {
        this.placeholder = placeholder
        return this
    }

    override fun blurRadius(blurRadius: Float): IImageAttr {
        this.blurRadius = blurRadius
        return this
    }

    override fun tintColor(color: Color?): IImageAttr {
        this.tintColor = color
        return this
    }

    override fun resizeCover(): IImageAttr {
        resizeMode = ImageConst.RESIZE_MODE_COVER
        return this
    }

    override fun resizeContain(): IImageAttr {
        resizeMode = ImageConst.RESIZE_MODE_CONTAIN
        return this
    }

    override fun resizeStretch(): IImageAttr {
        resizeMode = ImageConst.RESIZE_MODE_STRETCH
        return this
    }

    /**
     * 设置拉伸区域
     * @param top 距离上边偏移
     * @param left 距离左边偏移
     * @param bottom 距离下边偏移
     * @param right 距离右边偏移
     * @return 一个新的 ImageAttr 实例。
     */
    override fun capInsets(top: Float, left: Float, bottom: Float, right: Float): IImageAttr{
        capInsets = EdgeInsets(top, left, bottom, right)
        return this
    }

    override fun willDestroy() {
        this.view?.also { subView ->
            subView.parent?.also { parentView ->
                parentView.removeDomSubView(subView)
                parentView.removeChild(subView)
            }
        }
        this.view = null
    }
    /**
     * 构建 Placeholder + ImageView 组合
     */
    fun build(richTextView: RichTextView) {
        // Placeholder
        apply {
            placeholderSize(size.width, size.height)
            spanFrameDidChanged { frame ->
                placeholderFrame = frame
            }
        }
        val ctx = this
        richTextFrame = richTextView.flexNode.layoutFrame
        richTextView.event {
            addLayoutFrameDidChange { frame ->
                ctx.richTextFrame = frame
            }
        }

        ReactiveObserver.addLazyTaskUtilEndCollectDependency {
            // 添加图片节点
            var richTextViewParent = richTextView.parent
            while (richTextViewParent?.isVirtualView() == true) {
                richTextViewParent = richTextViewParent.parent
            }
            richTextViewParent?.addChild(ImageView()) {
                ctx.view = this
                attr {
                    visibility(ctx.placeholderFrame.width != 0f && ctx.placeholderFrame.height != 0f)
                    absolutePosition(
                        top = ctx.richTextFrame.y + ctx.placeholderFrame.y + ctx.verticalAlignOffset,
                        left = ctx.richTextFrame.x + ctx.placeholderFrame.x
                    )
                    size(ctx.size.width, ctx.size.height)
                    if (ctx.uri != null) {
                        src(ctx.uri!!, ctx.isDotNineImage)
                    } else {
                        src(ctx.src, ctx.isDotNineImage)
                    }
                    when(ctx.resizeMode) {
                        ImageConst.RESIZE_MODE_COVER -> resizeCover()
                        ImageConst.RESIZE_MODE_CONTAIN -> resizeContain()
                        ImageConst.RESIZE_MODE_STRETCH -> resizeStretch()
                    }
                    if (ctx.blurRadius > 0) {
                        blurRadius(ctx.blurRadius)
                    }
                    ctx.tintColor?.also {
                        tintColor(it)
                    }
                    if (ctx.placeholder.isNotEmpty()) {
                        placeholderSrc(ctx.placeholder)
                    }
                    borderRadius(ctx.borderRadius)
                    capInsets(ctx.capInsets.top, ctx.capInsets.left, ctx.capInsets.bottom, ctx.capInsets.right)

                }
                ctx.clickHandlerFn?.also {
                    getViewEvent().click(it)
                }
            }
            richTextViewParent?.also {
                ctx.view?.also {
                    richTextViewParent.insertDomSubView(it, richTextViewParent.domChildren().indexOf(it))
                }
            }
        }

    }

    override fun hasClickEvent(): Boolean {
        return clickHandlerFn != null
    }

    override fun performClickHandler(clickParams: ClickParams): Boolean {
        clickHandlerFn?.invoke(clickParams)
        return clickHandlerFn != null
    }

}

open class RichTextEvent : TextEvent() {
    private var interceptClickClickHandler: ((ClickParams) -> Boolean)? = null
    private var didListenClick = false
    internal fun interceptClick(handler: (ClickParams) -> Boolean) {
        interceptClickClickHandler = handler
        if (!didListenClick) {
            click { }
        }
    }

    internal fun hasInterceptClick(): Boolean {
        return interceptClickClickHandler != null
    }

    override fun click(handler: (ClickParams) -> Unit) {
        didListenClick = true
        super.click {
            if (!(interceptClickClickHandler?.invoke(it) == true)) {
                handler(it)
            }
        }
    }
}
