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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.tencent.kuikly.core.render.android.adapter.IKRAudioView
import com.tencent.kuikly.core.render.android.adapter.IKRAudioViewListener
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.css.ktx.frameHeight
import com.tencent.kuikly.core.render.android.css.ktx.frameWidth
import com.tencent.kuikly.core.render.android.css.ktx.toNumberFloat
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback

/**
 * 音频播放原生组件。
 *
 * 定义流程对齐 [KRVideoView]：自身仅作为容器与属性桥接层，真正的播放与控件 UI
 * 由宿主注册的 [KuiklyRenderAdapterManager.krAudioViewAdapter] 创建的 [IKRAudioView] 提供。
 * 与视频不同，音频没有画面拉伸模式与首帧回调。
 */
class KRAudioView(context: Context) : KRView(context), IKRAudioViewListener {

    private var audioView: IKRAudioView? = null
    private var src: String = ""
    private var playControl = KRAudioViewPlayControl.KRAudioViewPlayControlNone
    private var rate = -1f

    private var stateChangeCallback: KuiklyRenderCallback? = null
    private var playTimeChangeCallback: KuiklyRenderCallback? = null

    override val reusable: Boolean
        get() = false

    override fun setProp(propKey: String, propValue: Any): Boolean {
        var result = super.setProp(propKey, propValue)
        if (!result) {
            result = when (propKey) {
                PROP_SRC -> {
                    setSrc(propValue as String)
                    true
                }
                PROP_RATE -> {
                    setRate(propValue.toNumberFloat())
                    true
                }
                PROP_SEEK -> {
                    audioView?.seekToTime((propValue.toNumberFloat()).toLong())
                    true
                }
                PROP_PLAY_CONTROL -> {
                    setPlayControl(KRAudioViewPlayControl.from(propValue as Int))
                    true
                }
                PROP_STATE_CHANGE_CALLBACK -> {
                    stateChangeCallback = propValue as KuiklyRenderCallback
                    true
                }
                PROP_PLAY_TIME_CHANGE_CALLBACK -> {
                    playTimeChangeCallback = propValue as KuiklyRenderCallback
                    true
                }
                else -> audioView?.setProp(propKey, propValue) ?: false
            }
        }
        return result
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        val result = super.call(method, params, callback)
        audioView?.call(method, params)
        return result
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        createAudioViewIfNeed()
        (audioView as? View)?.also {
            it.layoutParams = LayoutParams(frameWidth, frameHeight)
        }
    }

    override fun audioPlayStateDidChangedWithState(
        playState: KRAudioPlayState,
        extInfo: Map<String, String>
    ) {
        stateChangeCallback?.invoke(mapOf(
            "state" to playState.ordinal,
            "extInfo" to extInfo
        ))
    }

    override fun playTimeDidChangedWithCurrentTime(currentTime: Long, totalTime: Long) {
        playTimeChangeCallback?.invoke(mapOf(
            "currentTime" to currentTime,
            "totalTime" to totalTime
        ))
    }

    override fun onDestroy() {
        super.onDestroy()
        audioView?.stop()
    }

    private fun setPlayControl(playControl: KRAudioViewPlayControl) {
        this.playControl = playControl
        when (playControl) {
            KRAudioViewPlayControl.KRAudioViewPlayControlPlay -> audioView?.play()
            KRAudioViewPlayControl.KRAudioViewPlayControlPause -> audioView?.pause()
            KRAudioViewPlayControl.KRAudioViewPlayControlStop -> audioView?.stop()
            else -> {}
        }
    }

    private fun setRate(rate: Float) {
        this.rate = rate
        audioView?.setRate(this.rate)
    }

    private fun setSrc(src: String) {
        if (src.isNotEmpty()) {
            this.src = src
            createAudioViewIfNeed()
        }
    }

    private fun createAudioViewIfNeed() {
        if (audioView != null) {
            return
        }

        val w = frameWidth
        val h = frameHeight
        if (src.isNotEmpty() && w != 0 && h != 0) {
            audioView = KuiklyRenderAdapterManager.krAudioViewAdapter?.createAudioView(context, src, this)
            assert(audioView != null)
            assert(audioView is View)
            (audioView as View).also {
                it.layoutParams = LayoutParams(w, h)
                addView(it)
            }
            if (this.rate != -1f) {
                setRate(this.rate)
            }
            setPlayControl(this.playControl)
        }
    }

    companion object {
        const val VIEW_NAME = "KRAudioView"
        private const val PROP_SRC = "src"
        private const val PROP_RATE = "rate"
        private const val PROP_SEEK = "seek"
        private const val PROP_PLAY_CONTROL = "playControl"
        private const val PROP_STATE_CHANGE_CALLBACK = "stateChange"
        private const val PROP_PLAY_TIME_CHANGE_CALLBACK = "playTimeChange"
    }
}

enum class KRAudioViewPlayControl {
    KRAudioViewPlayControlNone,
    KRAudioViewPlayControlPlay, // 操作播放音频
    KRAudioViewPlayControlPause, // 操作暂停音频
    KRAudioViewPlayControlStop; // 操作停止音频

    companion object {
        // 对齐 com.tencent.kuikly.core.views.AudioPlayControl 的 value: PLAY(1) / PAUSE(2) / STOP(3)
        fun from(value: Int): KRAudioViewPlayControl {
            return when (value) {
                1 -> KRAudioViewPlayControlPlay
                2 -> KRAudioViewPlayControlPause
                3 -> KRAudioViewPlayControlStop
                else -> KRAudioViewPlayControlNone
            }
        }
    }
}

enum class KRAudioPlayState {
    KRAudioPlayStateUnknown,
    KRAudioPlayStatePlaying, // 正在播放中
    KRAudioPlayStateCaching, // 缓冲中
    KRAudioPlayStatePaused,  // 播放暂停
    KRAudioPlayStatePlayEnd, // 播放结束
    KRAudioPlayStateFaild    // 播放失败
}
