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
package com.tencent.kuikly.core.base

import com.tencent.kuikly.core.base.Attr.StyleConst
import com.tencent.kuikly.core.base.attr.ClipPathBuilder
import com.tencent.kuikly.core.reactive.ReactiveObserver
import com.tencent.kuikly.core.views.PathApi

internal class ClipPathHandler(private val attr: Attr) {
    private var frameTaskRegistered: Boolean = false
    private lateinit var builder: ClipPathBuilder
    private var destroyed: Boolean = false

    private fun updateClipPath(
        width: Float,
        height: Float
    ) {
        if (width <= 0 || height <= 0) {
            return
        }
        val path = PropValuePath()
        builder(path, width, height)
        attr.setProp(StyleConst.CLIP_PATH, path.toPropValue())
    }

    private fun registerFrameTaskIfNeeded() {
        if (frameTaskRegistered) {
            return
        }
        frameTaskRegistered = true
        attr.setPropByFrameTask(StyleConst.CLIP_PATH) { (_, _, width, height) ->
            ReactiveObserver.unbindValueChange(this)
            ReactiveObserver.bindValueChange(this) {
                updateClipPath(width, height)
            }
        }
    }

    private fun unregisterFrameTask() {
        if (!frameTaskRegistered) {
            return
        }
        frameTaskRegistered = false
        attr.removePropFrameTask(StyleConst.CLIP_PATH)
        ReactiveObserver.unbindValueChange(this)
    }

    fun setBuilder(builder: ClipPathBuilder) {
        this.builder = builder
        unregisterFrameTask()
        ReactiveObserver.addLazyTaskUtilEndCollectDependency {
            val isDirty = attr.flexNode?.isDirty == true
            if (isDirty) {
                attr.getPager().addTaskWhenPagerUpdateLayoutFinish {
                    if (destroyed) {
                        return@addTaskWhenPagerUpdateLayoutFinish
                    }
                    registerFrameTaskIfNeeded()
                }
            } else {
                registerFrameTaskIfNeeded()
            }
        }
    }

    fun destroy() {
        destroyed = true
        unregisterFrameTask()
    }
}

class PropValuePath : PathApi {

    private val pathData: StringBuilder = StringBuilder()

    override fun beginPath() {
    }

    override fun moveTo(x: Float, y: Float) {
        if (pathData.isNotEmpty()) {
            pathData.append(' ')
        }
        pathData.append("M ").append(x).append(' ').append(y)
    }

    override fun lineTo(x: Float, y: Float) {
        if (pathData.isNotEmpty()) {
            pathData.append(' ')
        }
        pathData.append("L ").append(x).append(' ').append(y)
    }

    override fun arc(
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        endAngle: Float,
        counterclockwise: Boolean
    ) {
        if (pathData.isNotEmpty()) {
            pathData.append(' ')
        }
        pathData.append("R ").append(centerX).append(' ').append(centerY)
            .append(' ').append(radius)
            .append(' ').append(startAngle)
            .append(' ').append(endAngle)
            .append(' ').append(if (counterclockwise) 1 else 0)
    }

    override fun closePath() {
        if (pathData.isNotEmpty()) {
            pathData.append(' ')
        }
        pathData.append("Z")
    }

    override fun quadraticCurveTo(
        controlPointX: Float,
        controlPointY: Float,
        pointX: Float,
        pointY: Float
    ) {
        if (pathData.isNotEmpty()) {
            pathData.append(' ')
        }
        pathData.append("Q ").append(controlPointX).append(' ').append(controlPointY)
            .append(' ').append(pointX).append(' ').append(pointY)
    }

    override fun bezierCurveTo(
        controlPoint1X: Float,
        controlPoint1Y: Float,
        controlPoint2X: Float,
        controlPoint2Y: Float,
        pointX: Float,
        pointY: Float
    ) {
        if (pathData.isNotEmpty()) {
            pathData.append(' ')
        }
        pathData.append("C ").append(controlPoint1X).append(' ').append(controlPoint1Y)
            .append(' ').append(controlPoint2X).append(' ').append(controlPoint2Y)
            .append(' ').append(pointX).append(' ').append(pointY)
    }

    fun toPropValue(): String {
        return pathData.toString()
    }
}