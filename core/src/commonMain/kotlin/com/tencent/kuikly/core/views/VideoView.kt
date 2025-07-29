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

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 播放器组件。
 */
class VideoView : DeclarativeBaseView<VideoAttr, VideoEvent>() {
    override fun createAttr() = VideoAttr()

    override fun createEvent() = VideoEvent()

    override fun viewName(): String {
        return ViewConst.TYPE_VIDEO_VIEW
    }

}

/**
 * 播控枚举。
 */
enum class VideoPlayControl(val value: Int) {
    PREPLAY(1), // 预播放视频到第一帧，用于预加载优化
    PLAY(2), // 播放视频
    PAUSE(3), // 暂停视频
    STOP(4) // 停止视频
}

/**
 * 播放状态。
 */
enum class PlayState {
    NONE,
    PLAYING, // 播放中
    BUFFERING, // 缓冲中
    PAUSED, // 暂停视频
    PLAY_END, // 播放结束
    ERROR; // 播放错误
    companion object {
        fun fromInt(value: Int): PlayState {
            return values().firstOrNull { it.ordinal == value } ?: PlayState.NONE
        }
    }
}

/**
 * 视频组件属性类。
 */
class VideoAttr : Attr() {
    /**
     * 设置播放源属性。
     * @param src 视频源 URL。
     */
    fun src(src: String) {
        SRC with src
    }
    /**
     * 设置播放控制属性（播放、暂停、停止）。
     * @param playControl 播放控制枚举。
     */
    fun playControl(playControl: VideoPlayControl) {
        PLAY_CONTROL with playControl.value
    }
    /**
     * 设置视频画面拉伸模式为等比例撑满。
     * @return 返回 VideoAttr 对象以支持链式调用。
     */
    fun resizeModeToCover(): VideoAttr {
        RESIZE_MODE with "cover"
        return this
    }
    /**
     * 设置是否静音属性。
     * @param muted 静音状态，true 为静音，false 为非静音。
     */
    fun muted(muted: Boolean) {
        MUTED with muted.toInt()
    }

    /**
     * 设置倍速属性（1.0, 1.25, 1.5, 2.0）。
     * @param rate 倍速值。
     */
    fun rate(rate: Float) {
        RATE with rate
    }
    /**
     * 设置视频画面拉伸模式为等比例不裁剪，保留黑边。
     * @return 返回 VideoAttr 对象以支持链式调用。
     */
    fun resizeModeToContain(): VideoAttr {
        RESIZE_MODE with "contain"
        return this
    }
    /**
     * 设置视频画面拉伸模式为缩放撑满（非等比例，会变形）。
     * @return 返回 VideoAttr 对象以支持链式调用。
     */
    fun resizeModeToStretch(): VideoAttr {
        RESIZE_MODE with "stretch"
        return this
    }

    companion object {
        const val PLAY_CONTROL = "playControl"
        const val SRC = "src"
        const val MUTED = "muted"
        const val RATE = "rate"
        const val RESIZE_MODE = "resizeMode"
    }

}
class VideoEvent : Event() {
    /**
     * 播放状态变化回调。
     * @param handlerFn 回调函数，参数为播放状态和扩展信息。
     */
    fun playStateDidChanged(handlerFn: (state: PlayState, extInfo: JSONObject) -> Unit) {
        register(PLAY_STATE_CHANGE) {
            val jsonObject = it as? JSONObject ?: JSONObject()
            val state = PlayState.fromInt(jsonObject.optInt("state"))
            val extInfo = jsonObject.optJSONObject("extInfo") ?: JSONObject()
            handlerFn.invoke(state, extInfo)
        }
    }

    /**
     * 播放时间变化回调（毫秒）。
     * @param handlerFn 回调函数，参数为当前播放时间和总时间。
     */
    fun playTimeDidChanged(handlerFn: (curTime: Int, totalTime: Int) -> Unit) {
        register(PLAY_TIME_CHANGE) {
            val jsonObject = it as? JSONObject ?: JSONObject()
            val currentTime = jsonObject.optInt("currentTime")
            val totalTime = jsonObject.optInt("totalTime")
            handlerFn.invoke(currentTime, totalTime)
        }
    }
    /**
     * 视频首帧画面显示时回调（一般用该时机隐藏封面）。
     * @param handlerFn 回调函数。
     */
    fun firstFrameDidDisplay(handlerFn: () -> Unit) {
        register(FIRST_FRAME) {
            handlerFn.invoke()
        }
    }
    /**
     * 业务自定义扩展事件通道。
     * @param handlerFn 自定义事件处理函数。
     */
    fun customEvent(handlerFn: EventHandlerFn) {
        register(CUSTUM_EVENT, handlerFn)
    }

    companion object {
        const val PLAY_STATE_CHANGE = "stateChange"
        const val PLAY_TIME_CHANGE = "playTimeChange"
        const val FIRST_FRAME = "firstFrame"
        const val CUSTUM_EVENT = "customEvent"
    }

}

/**
 * 播放器组件。
 * @param init 初始化函数。
 */
fun ViewContainer<*, *>.Video(init: VideoView.() -> Unit) {
    addChild(VideoView(), init)
}