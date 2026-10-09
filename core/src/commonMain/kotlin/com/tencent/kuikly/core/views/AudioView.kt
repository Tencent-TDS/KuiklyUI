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
 * 音频播放组件。
 *
 * 与 [VideoView] 流程一致：viewName 对应原生注册的 KRAudioView，宿主需注册
 * 音频播放器适配器（Android: krAudioViewAdapter / iOS: registerAudioViewCreator）。
 * 与视频不同，音频没有画面相关属性（resizeMode / firstFrame）。
 */
class AudioView : DeclarativeBaseView<AudioAttr, AudioEvent>() {
    override fun createAttr() = AudioAttr()

    override fun createEvent() = AudioEvent()

    override fun viewName(): String {
        return ViewConst.TYPE_AUDIO_VIEW
    }
}

/**
 * 音频播控枚举。
 */
enum class AudioPlayControl(val value: Int) {
    PLAY(1), // 播放音频
    PAUSE(2), // 暂停音频
    STOP(3) // 停止音频
}

/**
 * 音频组件属性类。
 */
class AudioAttr : Attr() {
    /**
     * 设置播放源属性。
     * @param src 音频源 URL。
     */
    fun src(src: String) {
        SRC with src
    }

    /**
     * 设置播放控制属性（播放、暂停、停止）。
     * @param playControl 播放控制枚举。
     */
    fun playControl(playControl: AudioPlayControl) {
        PLAY_CONTROL with playControl.value
    }

    /**
     * 设置倍速属性（1.0, 1.25, 1.5, 2.0）。
     * @param rate 倍速值。
     */
    fun rate(rate: Float) {
        RATE with rate
    }

    /**
     * seek 到指定时间。
     * @param timeMs 时间，单位毫秒。
     */
    fun seek(timeMs: Int) {
        SEEK with timeMs
    }

    companion object {
        const val PLAY_CONTROL = "playControl"
        const val SRC = "src"
        const val RATE = "rate"
        const val SEEK = "seek"
    }
}

class AudioEvent : Event() {
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
     * 业务自定义扩展事件通道。
     * @param handlerFn 自定义事件处理函数。
     */
    fun customEvent(handlerFn: EventHandlerFn) {
        register(CUSTUM_EVENT, handlerFn)
    }

    companion object {
        const val PLAY_STATE_CHANGE = "stateChange"
        const val PLAY_TIME_CHANGE = "playTimeChange"
        const val CUSTUM_EVENT = "customEvent"
    }
}

/**
 * 音频播放组件。
 * @param init 初始化函数。
 */
fun ViewContainer<*, *>.Audio(init: AudioView.() -> Unit) {
    addChild(AudioView(), init)
}
