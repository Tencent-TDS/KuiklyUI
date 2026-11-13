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
import com.tencent.kuikly.core.nvi.serialization.json.JSON
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
    val touches: List<Touch>, // 包含所有多指触摸信息
    val consumed: Boolean // 是否已经被消费了，来自渲染层的消费状态，目前用于滑动中
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
            val consumed = tempParams.optInt("consumed", 0) == 1
            val touches = arrayListOf<Touch>()
            tempParams.optJSONArray("touches")?.also {
                for (i in 0 until it.length()) {
                    touches.add(Touch.decode(it.opt(i)))
                }
            }
            return TouchParams(x, y, pageX, pageY, timestamp, pointerId, action, touches, consumed)
        }
    }
}

private const val TOUCH_COMPAT_ONLY =
    "Touch 已不再是 data class；该成员仅为兼容旧代码而保留，请直接调用包含 pressed 的构造方法或访问属性"

/**
 * 单个触点的信息。自 2.32.0 起使用普通类，以便新增 pressed 属性的同时保留既有成员的签名。
 * 后续新增属性时，应在主构造方法末尾添加参数，并增加保留旧参数列表的次构造方法；既有兼容成员的签名不得改变。
 * 旧的 copy、component1 至 component6 和六参数构造方法标记为 Deprecated（WARNING），旧代码仍可编译并维持二进制兼容。
 * [equals]、[hashCode] 和 [toString] 应覆盖全部属性。
 */
class Touch(
    val x: Float,
    val y: Float,
    val pageX: Float,
    val pageY: Float,
    val hash: Float,
    val pointerId: Long,
    /**
     * 该触点当前是否处于按下状态。自 2.32.0 起，渲染层为每个触点分别上报此状态。
     *
     * 多指场景中某根手指抬起时，[TouchParams.touches] 仍包含全部触点，且抬起触点的坐标仍有效。
     * 仅凭坐标和 pointerId 无法判断本次抬起的是哪根手指；`pressed` 为抬起触点提供这一信息，其他仍按下的触点保持为 `true`。
     *
     * `null` 表示渲染层版本低于 2.32.0，或渲染层无法确定该状态。此时接收端只能依据事件类型推断本次事件中所有触点的状态，无法准确处理多指操作。
     */
    val pressed: Boolean?
) {
    @Deprecated(
        "Touch 已不再是 data class；旧的六参数构造方法仅为兼容旧代码而保留，pressed 为 null，请使用包含 pressed 参数的构造方法",
        ReplaceWith("Touch(x, y, pageX, pageY, hash, pointerId, null)"),
        level = DeprecationLevel.WARNING
    )
    constructor(
        x: Float,
        y: Float,
        pageX: Float,
        pageY: Float,
        hash: Float,
        pointerId: Long
    ) : this(x, y, pageX, pageY, hash, pointerId, null)

    companion object {
        fun decode(params: Any?): Touch {
            val tempParams = params as? JSONObject ?: JSONObject()
            val x = tempParams.optDouble("x").toFloat()
            val y = tempParams.optDouble("y").toFloat()
            val pageX = tempParams.optDouble("pageX").toFloat()
            val pageY = tempParams.optDouble("pageY").toFloat()
            val hash = tempParams.optLong("hash").toFloat()
            val pointerId = tempParams.optLong("pointerId", 5566L)
            // 字段缺失、为 null 或无法识别时必须得到 null，不可默认为 false，否则旧版渲染层的单指点击会被误判为已抬起。
            val pressed = JSON.toBoolean(tempParams.opt("pressed"))
            return Touch(x, y, pageX, pageY, hash, pointerId, pressed)
        }
    }

    // region 为兼容旧代码保留的成员（签名不得改变）

    @Deprecated(TOUCH_COMPAT_ONLY, level = DeprecationLevel.WARNING)
    fun copy(
        x: Float = this.x,
        y: Float = this.y,
        pageX: Float = this.pageX,
        pageY: Float = this.pageY,
        hash: Float = this.hash,
        pointerId: Long = this.pointerId
    ): Touch = Touch(x, y, pageX, pageY, hash, pointerId, pressed)

    @Deprecated(TOUCH_COMPAT_ONLY, level = DeprecationLevel.WARNING)
    operator fun component1(): Float = x

    @Deprecated(TOUCH_COMPAT_ONLY, level = DeprecationLevel.WARNING)
    operator fun component2(): Float = y

    @Deprecated(TOUCH_COMPAT_ONLY, level = DeprecationLevel.WARNING)
    operator fun component3(): Float = pageX

    @Deprecated(TOUCH_COMPAT_ONLY, level = DeprecationLevel.WARNING)
    operator fun component4(): Float = pageY

    @Deprecated(TOUCH_COMPAT_ONLY, level = DeprecationLevel.WARNING)
    operator fun component5(): Float = hash

    @Deprecated(TOUCH_COMPAT_ONLY, level = DeprecationLevel.WARNING)
    operator fun component6(): Long = pointerId

    // endregion

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Touch) return false
        // Float 用 compareTo 与 data class 生成的 equals 语义保持一致（NaN == NaN，0.0 != -0.0）
        return x.compareTo(other.x) == 0 &&
            y.compareTo(other.y) == 0 &&
            pageX.compareTo(other.pageX) == 0 &&
            pageY.compareTo(other.pageY) == 0 &&
            hash.compareTo(other.hash) == 0 &&
            pointerId == other.pointerId &&
            pressed == other.pressed
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + pageX.hashCode()
        result = 31 * result + pageY.hashCode()
        result = 31 * result + hash.hashCode()
        result = 31 * result + pointerId.hashCode()
        result = 31 * result + (pressed?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "x:${x}, y:${y}, pageX:${pageX}, pageY:${pageY} hash:${hash} pointerId:${pointerId} pressed:${pressed}"
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
    val state: String, // "start" | "move" | "end"
    val isCancel: Boolean,
    val params: Any? = null
) {

    @Deprecated("use constructor with params", level = DeprecationLevel.HIDDEN)
    constructor(
        x: Float,
        y: Float,
        pageX: Float,
        pageY: Float,
        state: String,
        isCancel: Boolean
    ) : this(x, y, pageX, pageY, state, isCancel, null)

    companion object {
        fun decode(params: Any?): LongPressParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val x = tempParams.optDouble("x").toFloat()
            val y = tempParams.optDouble("y").toFloat()
            val pageX = tempParams.optDouble("pageX").toFloat()
            val pageY = tempParams.optDouble("pageY").toFloat()
            val state = tempParams.optString("state")
            val isCancel = tempParams.optBoolean("isCancel")
            return LongPressParams(x, y, pageX, pageY, state, isCancel, params)
        }
    }
    inline val isStart get() = state == "start"
    inline val isMove get() = state == "move"
    inline val isEnd get() = state == "end"
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
    inline val isStart get() = state == "start"
    inline val isMove get() = state == "move"
    inline val isEnd get() = state == "end"
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
    inline val isStart get() = state == "start"
    inline val isMove get() = state == "move"
    inline val isEnd get() = state == "end"
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