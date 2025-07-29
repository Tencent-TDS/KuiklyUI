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

package com.tencent.kuikly.compose.ui

/**
 * 给CanvasContext操作增加Density支持
 *  * Created by zhenhuachen on 2025/4/30.
 */
fun CanvasContextEx.getDensity(): Float {
    return this.densityValue
}

fun CanvasContextEx.moveToWithDensity(x: Float, y: Float) {
    moveTo(x / getDensity(), y / getDensity())
}

fun CanvasContextEx.lineToWithDensity(x: Float, y: Float) {
    lineTo(x / getDensity(), y / getDensity())
}

fun CanvasContextEx.arcWithDensity(
    centerX: Float,
    centerY: Float,
    radius: Float,
    startAngle: Float,
    endAngle: Float,
    counterclockwise: Boolean
) {
    arc(
        centerX / getDensity(),
        centerY / getDensity(),
        radius / getDensity(),
        startAngle,
        endAngle,
        counterclockwise
    )
}

fun CanvasContextEx.lineWidthWithDensity(width: Float) {
    lineWidth(width / getDensity())
}

fun CanvasContextEx.quadraticCurveToWithDensity(
    controlPointX: Float,
    controlPointY: Float,
    pointX: Float,
    pointY: Float
) {
    quadraticCurveTo(
        controlPointX / getDensity(),
        controlPointY / getDensity(),
        pointX / getDensity(),
        pointY / getDensity()
    )
}

fun CanvasContextEx.bezierCurveToWithDensity(
    controlPoint1X: Float,
    controlPoint1Y: Float,
    controlPoint2X: Float,
    controlPoint2Y: Float,
    pointX: Float,
    pointY: Float
) {
    bezierCurveTo(
        controlPoint1X / getDensity(),
        controlPoint1Y / getDensity(),
        controlPoint2X / getDensity(),
        controlPoint2Y / getDensity(),
        pointX / getDensity(),
        pointY / getDensity()
    )
}

fun CanvasContextEx.saveLayerWithDensity(x: Float, y: Float, width: Float, height: Float) {
    saveLayer(
        x / getDensity(),
        y / getDensity(),
        width / getDensity(),
        height / getDensity()
    )
}

fun CanvasContextEx.translateWithDensity(x: Float, y: Float) {
    translate(x / getDensity(), y / getDensity())
}

fun CanvasContextEx.transformWithDensity(array: FloatArray) {
    val density = getDensity()
    if (array.size < 6) {
        throw IllegalArgumentException("The array must have at least 6 elements.")
    }
    val transformedArray = array.copyOf()
    transformedArray[2] = array[2] / density // adjusting the tx (translation x)
    transformedArray[5] = array[5] / density // adjusting the ty (translation y)
    transform(transformedArray)
}