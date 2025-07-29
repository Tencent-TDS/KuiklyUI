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
import com.tencent.kuikly.core.render.android.adapter.IKRVideoView
import com.tencent.kuikly.core.render.android.adapter.IKRVideoViewListener
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.css.ktx.frameHeight
import com.tencent.kuikly.core.render.android.css.ktx.frameWidth
import com.tencent.kuikly.core.render.android.css.ktx.toNumberFloat
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback

/**
 * Created by kam on 2023/6/9.
 */
class KRVideoView(context: Context) : KRView(context), IKRVideoViewListener {

    private var videoView: IKRVideoView? = null
    private var src: String = ""
    private var playControl = KRVideoViewPlayControl.KRVideoViewPlayControlNone
    private var resizeMode = KRVideoViewContentMode.KRVideoViewContentModeScaleAspectFit
    private var muted = false
    private var rate = -1f

    private var firstFrameCallback: KuiklyRenderCallback? = null
    private var stateChangeCallback: KuiklyRenderCallback? = null
    private var playTimeChangeCallback: KuiklyRenderCallback? = null
    private var customEventCallback: KuiklyRenderCallback? = null

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
                PROP_RESIZE_MODE_LEGACY,
                PROP_RESIZE_MODE -> {
                    setResizeMode(KRVideoViewContentMode.from(propValue as String))
                    true
                }
                PROP_MUTED -> {
                    setMuted(propValue as Int == 1)
                    true
                }
                PROP_RATE -> {
                    setRate(propValue.toNumberFloat())
                    true
                }
                PROP_PLAY_CONTROL -> {
                    setPlayControl(KRVideoViewPlayControl.from(propValue as Int))
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
                PROP_FIRST_FRAME_CALLBACK -> {
                    firstFrameCallback = propValue as KuiklyRenderCallback
                    true
                }
                PROP_CUSTOM_EVENT_CALLBACK -> {
                    customEventCallback = propValue as KuiklyRenderCallback
                    true
                }
                else -> videoView?.setProp(propKey, propValue) ?: false
            }
        }
        return result
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        val result = super.call(method, params, callback)
        videoView?.call(method, params)
        return result
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        createVideoViewIfNeed()
        (videoView as? View)?.also {
            it.layoutParams = LayoutParams(frameWidth, frameHeight)
        }
    }

    override fun videoPlayStateDidChangedWithState(
        playState: KRVideoPlayState,
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

    override fun videoFirstFrameDidDisplay() {
        firstFrameCallback?.invoke(mapOf<String, Any>())
    }

    override fun customEventWithInfo(eventInfo: Map<String, String>) {
        customEventCallback?.invoke(eventInfo)
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView?.stop()
    }

    private fun setPlayControl(playControl: KRVideoViewPlayControl) {
        this.playControl = playControl
        when(playControl) {
            KRVideoViewPlayControl.KRVideoViewPlayControlPreplay -> {
                videoView?.preplay()
            }
            KRVideoViewPlayControl.KRVideoViewPlayControlPlay -> {
                videoView?.play()
            }
            KRVideoViewPlayControl.KRVideoViewPlayControlPause -> {
                videoView?.pause()
            }
            KRVideoViewPlayControl.KRVideoViewPlayControlStop -> {
                videoView?.stop()
            }
            else -> {}
        }
    }

    private fun setRate(rate: Float) {
        this.rate = rate
        videoView?.setRate(this.rate)
    }

    private fun setMuted(muted: Boolean) {
        this.muted = muted
        videoView?.setMuted(muted)
    }

    private fun setResizeMode(resizeMode: KRVideoViewContentMode) {
        this.resizeMode = resizeMode
        videoView?.setVideoContentMode(this.resizeMode)
    }

    private fun setSrc(src: String) {
        if (src.isNotEmpty()) {
            this.src = src
            createVideoViewIfNeed()
        }
    }

    private fun createVideoViewIfNeed() {
        if (videoView != null) {
            return
        }

        val w = frameWidth
        val h = frameHeight
        if (src.isNotEmpty() && w != 0 && h != 0) {
            videoView = KuiklyRenderAdapterManager.krVideoViewAdapter?.createVideoView(context, src, this)
            assert(videoView != null)
            assert(videoView is View)
            (videoView as View).also {
                it.layoutParams = LayoutParams(w, h)
                addView(it)
            }
            setResizeMode(this.resizeMode)
            setMuted(this.muted)
            if (this.rate != -1f) {
                setRate(this.rate)
            }
            setPlayControl(this.playControl)
        }
    }

    companion object {
        const val VIEW_NAME = "KRVideoView"
        private const val PROP_SRC = "src"
        @Deprecated("use PROP_RESIZE_MODE instead", replaceWith = ReplaceWith("PROP_RESIZE_MODE"))
        private const val PROP_RESIZE_MODE_LEGACY = "resizeMod" // 兼容1.1.71以下版本的拼写错误
        private const val PROP_RESIZE_MODE = "resizeMode"
        private const val PROP_MUTED = "muted"
        private const val PROP_RATE = "rate"
        private const val PROP_PLAY_CONTROL = "playControl"
        private const val PROP_STATE_CHANGE_CALLBACK = "stateChange"
        private const val PROP_PLAY_TIME_CHANGE_CALLBACK = "playTimeChange"
        private const val PROP_FIRST_FRAME_CALLBACK = "firstFrame"
        private const val PROP_CUSTOM_EVENT_CALLBACK = "customEvent"
    }
}

enum class KRVideoViewPlayControl {
    KRVideoViewPlayControlNone,
    KRVideoViewPlayControlPreplay, //操作预播放视频
    KRVideoViewPlayControlPlay, // 操作播放视频
    KRVideoViewPlayControlPause, // 操作暂停视频
    KRVideoViewPlayControlStop; // 操作停止视频

    companion object {

        fun from(value: Int): KRVideoViewPlayControl {
            return KRVideoViewPlayControl.values()
                .firstOrNull { it.ordinal == value } ?: KRVideoViewPlayControl.KRVideoViewPlayControlNone
        }
    }
}

enum class KRVideoPlayState {
    KRVideoPlayStateUnknown,
    KRVideoPlayStatePlaying, // 正在播放中 （注：回调该状态时，视频应该是有画面的）
    KRVideoPlayStateCaching, // 缓冲中  （注：如果未调用过VAVideoPlayStatusPlaying状态，不能调用该状态）
    KRVideoPlayStatePaused,  // 播放暂停 （注：如果一个视频处于PrepareToPlay状态，此时调用了暂停操作， 应该回调该状态）
    KRVideoPlayStatePlayEnd, // 播放结束
    KRVideoPlayStateFaild   // 播放失败
}

enum class KRVideoViewContentMode(private val value: String) {
    KRVideoViewContentModeScaleAspectFit("contain"), // 按原视频比例显示，是竖屏的就显示出竖屏的，两边留黑
    KRVideoViewContentModeScaleAspectFill("cover"), // 按原比例拉伸视频，直到两边都占满
    KRVideoViewContentModeScaleToFill("stretch"); // 拉伸视频内容达到边框占满，但不按原比例拉伸

    companion object {
        fun from(resizeMode: String): KRVideoViewContentMode {
            return KRVideoViewContentMode.values().firstOrNull { it.value == resizeMode } ?: KRVideoViewContentModeScaleAspectFit
        }
    }
}