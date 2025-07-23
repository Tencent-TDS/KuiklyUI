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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.BaseObject
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.RenderView
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.toInt
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.module.ImageRef
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.ReactiveObserver
import com.tencent.kuikly.core.views.shadow.TextShadow
import kotlin.math.PI

class CanvasView : DeclarativeBaseView<Attr, Event>() {
    var drawCallback: CanvasDrawCallback? = null
    private var reactiveObserverOwner = BaseObject()

    override fun createAttr(): Attr {
        return Attr()
    }

    override fun createEvent(): Event {
        return Event()
    }

    override fun viewName(): String {
        return ViewConst.TYPE_CANVAS
    }

    override fun createRenderView() {
        val isNewRenderView = renderView == null
        super.createRenderView()
        if (isNewRenderView) {
            draw()
        }
    }

    override fun setFrameToRenderView(frame: Frame) {
        super.setFrameToRenderView(frame)
        draw()
    }

    private fun draw() {
        if (renderView == null || flexNode.layoutFrame.isDefaultValue()) {
            return
        }
        ReactiveObserver.unbindValueChange(reactiveObserverOwner)
        ReactiveObserver.bindValueChange(reactiveObserverOwner) {
            if (renderView == null || flexNode.layoutFrame.isDefaultValue()) {
            } else if (drawCallback != null){
                val context = CanvasContext(renderView!!, pagerId, nativeRef)
                context.reset()
                drawCallback!!(context, flexNode.layoutFrame.width, flexNode.layoutFrame.height)
            }
        }
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        ReactiveObserver.unbindValueChange(reactiveObserverOwner)
    }
}

class TextMetrics(
    val width: Float,
    val actualBoundingBoxLeft: Float,
    val actualBoundingBoxRight: Float,
    val actualBoundingBoxAscent: Float,
    val actualBoundingBoxDescent: Float
)

// api 对齐 h5 canvas 标准
interface ContextApi {
    fun beginPath()
    fun moveTo(x: Float, y: Float)
    fun lineTo(x: Float, y: Float)
    fun arc(
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        endAngle: Float,
        counterclockwise: Boolean
    )
    fun closePath()
    fun stroke()
    fun fill()
    fun strokeStyle(color: Color)
    fun strokeStyle(linearGradient: CanvasLinearGradient)
    fun fillStyle(color: Color)
    fun fillStyle(linearGradient: CanvasLinearGradient)
    fun lineWidth(width: Float)
    fun setLineDash(intervals: List<Float>)
    fun lineCapRound()
    fun lineCapButt()
    fun lineCapSquare()
    fun quadraticCurveTo(
        controlPointX: Float,
        controlPointY: Float,
        pointX: Float,
        pointY: Float
    )
    fun bezierCurveTo(
        controlPoint1X: Float,
        controlPoint1Y: Float,
        controlPoint2X: Float,
        controlPoint2Y: Float,
        pointX: Float,
        pointY: Float
    )

    fun save()
    fun saveLayer(x: Float, y: Float, width: Float, height: Float)
    fun restore()
    fun clip(intersect: Boolean = true)
    fun clipPathIntersect()
    fun clipPathDifference()
    fun translate(x: Float, y: Float)
    fun scale(x: Float, y: Float)
    fun rotate(angle: Float)
    fun skew(x: Float, y: Float)
    fun transform(array: FloatArray)

    fun createLinearGradient(x0: Float, y0: Float, x1: Float, y1: Float): CanvasLinearGradient
    fun createRadialGradient(x0: Float, y0: Float, r0: Float, x1: Float, y1: Float, r1: Float, alpha: Float, vararg colors: Color)

    fun textAlign(textAlign: TextAlign)
    fun font(size: Float, family: String = "")
    fun font(style: FontStyle = FontStyle.NORMAL, weight: FontWeight = FontWeight.NORMAL, size: Float = 15f, family: String = "")
    fun measureText(value: String): TextMetrics
    fun fillText(text: String, x: Float, y: Float)
    fun strokeText(text: String, x: Float, y: Float)

    fun drawImage(image: ImageRef, dx: Float, dy: Float)
    fun drawImage(image: ImageRef, dx: Float, dy: Float, dWidth: Float, dHeight: Float)
    fun drawImage(image: ImageRef, sx: Float, sy: Float, sWidth : Float, sHeight: Float, dx: Float, dy: Float, dWidth: Float, dHeight: Float)
}

open class CanvasContext(private val renderView: RenderView, private val pagerId: String, private val nativeRef: Int) : ContextApi {

    private var fontStyle: FontStyle = FontStyle.NORMAL
    private var fontWeight: FontWeight = FontWeight.NORMAL
    private var fontFamily: String = ""
    private var fontSize: Float = 15f
    private var textAlign: TextAlign = TextAlign.LEFT

    internal companion object {
        private fun FloatArray.isIdentity(): Boolean {
            return this[0] == 1f && this[1] == 0f && this[2] == 0f &&
                    this[3] == 0f && this[4] == 1f && this[5] == 0f &&
                    this[6] == 0f && this[7] == 0f && this[8] == 1f
        }
    }

    /**
     * 开始创建一个新的路径。
     */
    override fun beginPath() {
        renderView.callMethod("beginPath", "")
    }

    /**
     * 重置 CanvasContext。
     */
    internal fun reset() {
        renderView.callMethod("reset", "")
    }

    /**
     * 将绘图游标移动到指定的坐标 (x, y)。
     * @param x 目标 x 坐标
     * @param y 目标 y 坐标
     */
    override fun moveTo(x: Float, y: Float) {
        val params = JSONObject().apply {
            put("x", x)
            put("y", y)
        }
        renderView.callMethod("moveTo", params.toString())
    }

    /**
     * 从当前位置画一条直线到指定的坐标 (x, y)。
     * @param x 目标 x 坐标
     * @param y 目标 y 坐标
     */
    override fun lineTo(x: Float, y: Float) {
        val params = JSONObject().apply {
            put("x", x)
            put("y", y)
        }
        renderView.callMethod("lineTo", params.toString())
    }

    /**
     * 绘制一个圆弧。
     * @param centerX 圆心 x 坐标
     * @param centerY 圆心 y 坐标
     * @param radius 半径
     * @param startAngle 起始角度（以弧度为单位）
     * @param endAngle 终止角度（以弧度为单位）
     * @param counterclockwise 是否逆时针绘制
     */
    override fun arc(
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float, // PI单位
        endAngle: Float, // PI单位
        counterclockwise: Boolean // 是否为逆时针
    ) {
        val sAngle = startAngle
        var eAngle = endAngle
        var rClockWise = counterclockwise
        if (sAngle + 2 * PI.toFloat() == endAngle) {
            eAngle -= 0.01f // 避免起角和终角同一个点
            rClockWise = false
        }
        val params = JSONObject().apply {
            put("x", centerX)
            put("y", centerY)
            put("r", radius)
            put("sAngle", sAngle)
            put("eAngle", eAngle)
            put("counterclockwise", rClockWise.toInt())
        }
        renderView.callMethod("arc", params.toString())
    }

    /**
     * 关闭当前路径。
     */
    override fun closePath() {
        renderView.callMethod("closePath", "")
    }

    /**
     * 绘制当前路径的边框。
     */
    override fun stroke() {
        renderView.callMethod("stroke", "")
    }

    /**
     * 填充当前路径。
     */
    override fun fill() {
        renderView.callMethod("fill", "")
    }

    /**
     * 设置描边颜色。
     * @param color 描边颜色
     */
    override fun strokeStyle(color: Color) {
        val params = JSONObject().apply {
            put("style", color.toString())
        }
        renderView.callMethod("strokeStyle", params.toString())
    }

    override fun strokeStyle(linearGradient: CanvasLinearGradient) {
        val params = JSONObject().apply {
            put("style", linearGradient.toString())
        }
        renderView.callMethod("strokeStyle", params.toString())
    }

    /**
     * 设置填充颜色。
     * @param color 填充颜色
     */
    override fun fillStyle(color: Color) {
        val params = JSONObject().apply {
            put("style", color.toString())
        }
        renderView.callMethod("fillStyle", params.toString())
    }

    /**
     * 设置填充样式为指定的线性渐变。
     *
     * @param linearGradient 要设置的线性渐变。
     */
    override fun fillStyle(linearGradient: CanvasLinearGradient) {
        // 将线性渐变转换为 JSON 格式
        val params = JSONObject().apply {
            put("style", linearGradient.toString())
        }
        renderView.callMethod("fillStyle", params.toString())
    }

    /**
     * 设置线宽。(默认为0f)
     * @param width 线宽
     */
    override fun lineWidth(width: Float) {
        val params = JSONObject().apply {
            put("width", width)
        }
        renderView.callMethod("lineWidth", params.toString())
    }

    /**
     * 设置虚线样式
     * @param intervals 线和间隙的交替长度
     * 如果要切换至实线模式，设置为空数组。
     */
    override fun setLineDash(intervals: List<Float>) {
        val params = JSONObject()
        if (intervals.isEmpty()) {
            params.put("intervals", null)
        } else {
            val jsonArray = JSONArray()
            for (i in intervals) {
                jsonArray.put(i)
            }
            params.put("intervals", jsonArray)
        }
        renderView.callMethod("lineDash", params.toString())
    }

    /**
     * 设置线条端点为圆形。
     */
    override fun lineCapRound() {
        lineCap("round")
    }

    /**
     * 设置线条端点为平头。
     */
    override fun lineCapButt() {
        lineCap("butt")
    }

    /**
     * 设置线条端点为方形。
     */
    override fun lineCapSquare() {
        lineCap("square")
    }

    /**
     * 设置线条端点样式。
     * @param style 线条端点样式（"round"、"butt" 或 "square"）
     */
    private fun lineCap(style: String) {
        val params = JSONObject().apply {
            put("style", style)
        }
        renderView.callMethod("lineCap", params.toString())
    }

    /**
     * 绘制二次贝塞尔曲线。
     * @param controlPointX 控制点 x 坐标
     * @param controlPointY 控制点 y 坐标
     * @param pointX 结束点 x 坐标
     * @param pointY 结束点 y 坐标
     */
    override fun quadraticCurveTo(controlPointX: Float,
                                  controlPointY: Float,
                                  pointX: Float,
                                  pointY: Float) {
        val params = JSONObject()
        params.put("cpx", controlPointX)
        params.put("cpy", controlPointY)
        params.put("x", pointX)
        params.put("y", pointY)
        renderView.callMethod("quadraticCurveTo", params.toString())
    }

    /**
     * 绘制三次贝塞尔曲线。
     * @param controlPoint1X 第一个控制点 x 坐标
     * @param controlPoint1Y 第一个控制点 y 坐标
     * @param controlPoint2X 第二个控制点 x 坐标
     * @param controlPoint2Y 第二个控制点 y 坐标
     * @param pointX 结束点 x 坐标
     * @param pointY 结束点 y 坐标
     */
    override fun bezierCurveTo(
        controlPoint1X: Float,
        controlPoint1Y: Float,
        controlPoint2X: Float,
        controlPoint2Y: Float,
        pointX: Float,
        pointY: Float
    ) {
        val params = JSONObject()
        params.put("cp1x", controlPoint1X)
        params.put("cp1y", controlPoint1Y)
        params.put("cp2x", controlPoint2X)
        params.put("cp2y", controlPoint2Y)
        params.put("x", pointX)
        params.put("y", pointY)
        renderView.callMethod("bezierCurveTo", params.toString())
    }

    /**
     * 创建线性渐变
     * @param x0 起始点 x 坐标
     * @param y0 起始点 y 坐标
     * @param x1 结束点 x 坐标
     * @param y1 结束点 y 坐标
     */
    override fun createLinearGradient(x0: Float, y0: Float, x1: Float, y1: Float): CanvasLinearGradient {
        return CanvasLinearGradient(x0, y0, x1, y1)
    }

    /**
     * 创建径向渐变（创建即自动立马绘制该径向渐变，无需fillStyle）。
     * 注：暂时仅iOS平台支持
     * @param x0：圆心的x坐标（开始圆）。
     * @param y0：圆心的y坐标（开始圆）。
     * @param r0：开始圆的半径。
     * @param x1：圆心的x坐标（结束圆）。
     * @param y1：圆心的y坐标（结束圆）。
     * @param r1：结束圆的半径。
     * @param alpha 整体透明度，一般为1f, 取值[0f, 1f]
     * @param colors 渐变中的颜色数组
     */
    override fun createRadialGradient(x0: Float, y0: Float, r0: Float, x1: Float, y1: Float, r1: Float, alpha: Float, vararg colors: Color) {
        val params = JSONObject()
        params.put("x0", x0)
        params.put("y0", y0)
        params.put("r0", r0)
        params.put("x1", x1)
        params.put("y1", y1)
        params.put("r1", r1)
        params.put("alpha", alpha)
        var colorStopStr = ""
        for (item in colors) {
            colorStopStr += "$item,"
        }
        if (colorStopStr.isNotEmpty()) {
            params.put("colors", colorStopStr.substring(0, colorStopStr.length - 1))
        }
        renderView.callMethod("createRadialGradient", params.toString())
    }

    /**
     * 文本对齐方式
     */
    override fun textAlign(textAlign: TextAlign) {
        this.textAlign = textAlign
        renderView.callMethod("textAlign", textAlign.value)
    }

    /**
     * 文本样式
     */
    override fun font(size: Float, family: String) {
        font(FontStyle.NORMAL, FontWeight.NORMAL, size, family)
    }

    /**
     * 文本样式
     */
    override fun font(style: FontStyle, weight: FontWeight, size: Float, family: String) {
        this.fontStyle = style
        this.fontWeight = weight
        this.fontSize = size
        this.fontFamily = family
        val params = JSONObject()
        params.put("style", style.value)
        params.put("weight", weight.value)
        params.put("size", size)
        if (family.isNotEmpty()) {
            params.put("family", family)
        }
        renderView.callMethod("font", params.toString())
    }

    /**
     * 测量文本
     */
    override fun measureText(value: String): TextMetrics {
        val shadow: TextShadow = TextShadow(pagerId, nativeRef, ViewConst.TYPE_RICH_TEXT)
        shadow.setProp("text", value)
        shadow.setProp(TextConst.FONT_STYLE, fontStyle.value)
        shadow.setProp(TextConst.FONT_WEIGHT, fontWeight.value)
        shadow.setProp(TextConst.FONT_SIZE, fontSize)
        shadow.setProp(TextConst.TEXT_USE_DP_FONT_SIZE_DIM, 1)
        if (fontFamily.isNotEmpty()) {
            shadow.setProp(TextConst.FONT_FAMILY, fontFamily)
        }
        val size = shadow.calculateRenderViewSize(100000f, 100000f)
        shadow.removeFromParentComponent()
        var left: Float
        var right: Float
        if (textAlign == TextAlign.CENTER) {
            left = size.width / 2
            right = size.width - left
        } else if (textAlign == TextAlign.RIGHT) {
            left = size.width
            right = 0f
        } else {
            left = 0f
            right = size.width
        }
        val descent = (size.height - fontSize) / 2
        val ascent = size.height - descent
        return TextMetrics(size.width, left, right, ascent, descent)
    }

    override fun fillText(text: String, x: Float, y: Float) {
        val params = JSONObject()
        params.put("text", text)
        params.put("x", x)
        params.put("y", y)
        renderView.callMethod("fillText", params.toString())
    }

    override fun strokeText(text: String, x: Float, y: Float) {
        val params = JSONObject()
        params.put("text", text)
        params.put("x", x)
        params.put("y", y)
        renderView.callMethod("strokeText", params.toString())
    }

    override fun drawImage(image: ImageRef, dx: Float, dy: Float){
        val params = JSONObject()
        params.put("cacheKey", image.cacheKey)
        params.put("dx", dx)
        params.put("dy", dy)

        renderView.callMethod("drawImage", params.toString())
    }
    override fun drawImage(image: ImageRef, dx: Float, dy: Float, dWidth: Float, dHeight: Float){
        val params = JSONObject()
        params.put("cacheKey", image.cacheKey)
        params.put("dx", dx)
        params.put("dy", dy)
        params.put("dWidth", dWidth)
        params.put("dHeight", dHeight)

        renderView.callMethod("drawImage", params.toString())
    }
    override fun drawImage(image: ImageRef,
                           sx: Float, sy: Float,
                           sWidth : Float, sHeight: Float,
                           dx: Float, dy: Float,
                           dWidth: Float, dHeight: Float){
        val params = JSONObject()
        params.put("cacheKey", image.cacheKey)
        params.put("sx", sx)
        params.put("sy", sy)
        params.put("sWidth", sWidth)
        params.put("sHeight", sHeight)
        params.put("dx", dx)
        params.put("dy", dy)
        params.put("dWidth", dWidth)
        params.put("dHeight", dHeight)

        renderView.callMethod("drawImage", params.toString())
    }

    override fun save() {
        renderView.callMethod("save", "")
    }

    override fun saveLayer(x: Float, y: Float, width: Float, height: Float) {
        val params = JSONObject()
        params.put("x", x)
        params.put("y", y)
        params.put("width", width)
        params.put("height", height)
        renderView.callMethod("saveLayer", params.toString())
    }

    override fun restore() {
        renderView.callMethod("restore", "")
    }

    /**
     * 裁剪当前路径。
     */
    override fun clip(intersect: Boolean) {
        val params = JSONObject()
        params.put("intersect", if (intersect) 1 else 0)
        renderView.callMethod("clip", params.toString())
    }

    override fun clipPathIntersect() {
        clip(true)
    }

    override fun clipPathDifference() {
        clip(false)
    }

    override fun translate(x: Float, y: Float) {
        if (x == 0f && y == 0f) {
            return
        }
        val params = JSONObject()
        params.put("x", x)
        params.put("y", y)
        renderView.callMethod("translate", params.toString())
    }

    override fun scale(x: Float, y: Float) {
        if (x == 1f && y == 1f) {
            return
        }
        val params = JSONObject()
        params.put("x", x)
        params.put("y", y)
        renderView.callMethod("scale", params.toString())
    }

    override fun rotate(angle: Float) {
        if (angle == 0f) {
            return
        }
        val params = JSONObject()
        params.put("angle", angle)
        renderView.callMethod("rotate", params.toString())
    }

    override fun skew(x: Float, y: Float) {
        if (x == 0f && y == 0f) {
            return
        }
        val params = JSONObject()
        params.put("x", x)
        params.put("y", y)
        renderView.callMethod("skew", params.toString())
    }

    override fun transform(array: FloatArray) {
        if (array.size < 9 || array.isIdentity()) {
            return
        }
        val values = JSONArray()
        for (i in 0 until 9) {
            values.put(array[i])
        }
        val params = JSONObject()
        params.put("values", values)
        renderView.callMethod("transform", params.toString())
    }

}

fun ViewContainer<*, *>.Canvas(
    init: CanvasView.() -> Unit,
    draw: CanvasDrawCallback
) {
    addChild(CanvasView()) {
        drawCallback = draw
        init()
    }

}

typealias CanvasDrawCallback = (context: CanvasContext, width: Float, height: Float) -> Unit

/**
 * CanvasLinearGradient 类表示一个线性渐变。
 *
 * @property x0 渐变起点的 x 坐标。
 * @property y0 渐变起点的 y 坐标。
 * @property x1 渐变终点的 x 坐标。
 * @property y1 渐变终点的 y 坐标。
 */
class CanvasLinearGradient(val x0: Float, val y0: Float, val x1: Float, val y1: Float) {
    // 用于存储渐变中的颜色停点
    private val colorStops = arrayListOf<ColorStop>()

    /**
     * 向渐变中添加一个颜色停点。
     *
     * @param stopIn01 颜色停点的位置，范围在 0 到 1 之间。
     * @param color 颜色停点的颜色。
     */
    fun addColorStop(stopIn01: Float, color: Color) {
        colorStops.add(ColorStop(color, stopIn01))
    }

    override fun toString(): String {
        var colorStopStr = ""
        for (item in colorStops) {
            colorStopStr += "$item,"
        }
        if (colorStopStr.isNotEmpty()) {
            colorStopStr = colorStopStr.substring(0, colorStopStr.length - 1)
        }
        return "linear-gradient" + JSONObject().apply {
            put("x0", x0)
            put("y0", y0)
            put("x1", x1)
            put("y1", y1)
            put("colorStops", colorStopStr)
        }.toString()
    }

}