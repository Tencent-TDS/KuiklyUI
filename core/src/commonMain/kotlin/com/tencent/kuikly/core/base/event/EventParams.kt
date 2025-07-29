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

package com.tencent.kuikly.core.base.event

import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 点击事件（单击和双击）的参数定义，并且提供了面向JSON的decode方法
 */
data class ClickParams(
    val x: Float,
    val y: Float,
    val pageX: Float, // 触摸点在根视图Page下的坐标X
    val pageY: Float, // 触摸点在根视图Page下的坐标Y
    val params: Any? = null
) {
    companion object {
        fun decode(params: Any?): ClickParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val x = tempParams.optDouble("x").toFloat()
            val y = tempParams.optDouble("y").toFloat()
            val pageX = tempParams.optDouble("pageX").toFloat()
            val pageY = tempParams.optDouble("pageY").toFloat()
            return ClickParams(x, y, pageX, pageY, params)
        }
    }
}

data class TouchParams(
    val x: Float, // 触摸点在自身view坐标系下的坐标X
    val y: Float, // 触摸点在自身view坐标系下的坐标Y
    val pageX: Float, // 触摸点在根视图Page下的坐标X
    val pageY: Float, // 触摸点在根视图Page下的坐标Y
    val timestamp: Long, // 触发事件时，距离系统启动的毫秒数
    val pointerId: Int, // 触摸点的ID
    val action: String, // 事件类型, 该属性从1.1.86版本开始支持，之前的版本获取为空
    val touches: List<Touch> // 包含所有多指触摸信息
) {
    companion object {
        fun decode(params: Any?): TouchParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val x = tempParams.optDouble("x").toFloat()
            val y = tempParams.optDouble("y").toFloat()
            val pageX = tempParams.optDouble("pageX").toFloat()
            val pageY = tempParams.optDouble("pageY").toFloat()
            val timestamp = tempParams.optDouble("timestamp").toLong()
            val pointerId = tempParams.optInt("pointerId")
            val action =  tempParams.optString("action")
            val touches = fastArrayListOf<Touch>()
            tempParams.optJSONArray("touches")?.also {
                for (i in 0 until it.length()) {
                    touches.add(Touch.decode(it.opt(i)))
                }
            }

            return TouchParams(x, y, pageX, pageY, timestamp, pointerId, action, touches)
        }
    }
}

data class Touch(
    val x: Float,
    val y: Float,
    val pageX: Float,
    val pageY: Float,
    val hash: Float,
    val pointerId: Long
) {
    companion object {
        fun decode(params: Any?): Touch {
            val tempParams = params as? JSONObject ?: JSONObject()
            val x = tempParams.optDouble("x").toFloat()
            val y = tempParams.optDouble("y").toFloat()
            val pageX = tempParams.optDouble("pageX").toFloat()
            val pageY = tempParams.optDouble("pageY").toFloat()
            val hash = tempParams.optLong("hash").toFloat()
            val pointerId = tempParams.optLong("pointerId", 5566L)
            return Touch(x, y, pageX, pageY, hash, pointerId)
        }
    }

    override fun toString(): String {
        return "x:${x}, y:${y}, pageX:${pageX}, pageY:${pageY} hash:${hash} pointerId:${pointerId}"
    }
}

/**
 * 长按事件的参数定义，并且提供了面向JSON的decode方法
 */
data class LongPressParams(
    val x: Float,
    val y: Float,
    val pageX: Float, // 触摸点在根视图Page下的坐标X
    val pageY: Float, // 触摸点在根视图Page下的坐标Y
    val state: String // "start" | "move" | "end"
) {
    companion object {
        fun decode(params: Any?): LongPressParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val x = tempParams.optDouble("x").toFloat()
            val y = tempParams.optDouble("y").toFloat()
            val pageX = tempParams.optDouble("pageX").toFloat()
            val pageY = tempParams.optDouble("pageY").toFloat()
            val state = tempParams.optString("state")
            return LongPressParams(x, y, pageX, pageY, state)
        }
    }
}

/**
 * 滑动事件的参数定义，并且提供了面向JSON的decode方法
 */
data class PanGestureParams(
    val x: Float,   // 当前view坐标系下的触摸点x
    val y: Float,    // 当前view坐标系下的触摸点y
    val state: String, // "start" | "move" | "end"
    val pageX: Float,
    val pageY: Float
) {
    companion object {
        fun decode(params: Any?): PanGestureParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val x = tempParams.optDouble("x").toFloat()
            val y = tempParams.optDouble("y").toFloat()
            val state = tempParams.optString("state")
            val pageX = tempParams.optDouble("pageX").toFloat()
            val pageY = tempParams.optDouble("pageY").toFloat()
            return PanGestureParams(x, y, state, pageX, pageY)
        }
    }
}

/**
 * 捏合事件的参数定义，并且提供了面向JSON的decode方法
 */
data class PinchGestureParams(
    val x: Float,   // 捏合中心点在自身view坐标系下的坐标X
    val y: Float,    // 捏合中心点在自身view坐标系下的坐标Y
    val pageX: Float,  // 捏合中心点在根视图Page下的坐标X
    val pageY: Float,  // 捏合中心点在根视图Page下的坐标Y
    val scale: Float, // 缩放倍数
    val state: String // "start" | "move" | "end"
) {
    companion object {
        fun decode(params: Any?): PinchGestureParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val x = tempParams.optDouble("x").toFloat()
            val y = tempParams.optDouble("y").toFloat()
            val scale = tempParams.optDouble("scale").toFloat()
            val pageX = tempParams.optDouble("pageX").toFloat()
            val pageY = tempParams.optDouble("pageY").toFloat()
            val state = tempParams.optString("state")
            return PinchGestureParams(x, y, pageX, pageY, scale, state)
        }
    }
}

/**
 * 动画结束事件的参数定义，并且提供了面向JSON的decode方法
 */
data class AnimationCompletionParams(
    val finish: Int,
    val attr: String,
    val animationKey: String
) {
    companion object {
        fun decode(params: Any?): AnimationCompletionParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val finish = tempParams.optInt("finish")
            val attr = tempParams.optString("attr")
            val key = tempParams.optString("animationKey")
            return AnimationCompletionParams(finish, attr, key)
        }
    }
}