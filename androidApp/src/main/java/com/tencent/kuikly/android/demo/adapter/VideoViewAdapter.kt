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

package com.tencent.kuikly.android.demo.adapter

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.tencent.kuikly.core.render.android.adapter.IKRVideoView
import com.tencent.kuikly.core.render.android.adapter.IKRVideoViewAdapter
import com.tencent.kuikly.core.render.android.adapter.IKRVideoViewListener
import com.tencent.kuikly.core.render.android.expand.component.KRVideoPlayState
import com.tencent.kuikly.core.render.android.expand.component.KRVideoViewContentMode

/**
 * Created by kam on 2023/6/17.
 */
class VideoViewAdapter : IKRVideoViewAdapter {
    override fun createVideoView(context: Context, src: String, listener: IKRVideoViewListener): IKRVideoView {
        return KuiklyVideoView(context, src, listener)
    }
}

class KuiklyVideoView(context: Context, private val src: String, private val listener: IKRVideoViewListener) : PlayerView(context), IKRVideoView {

    private val exoPlayer = ExoPlayer.Builder(context)
        .setTrackSelector(DefaultTrackSelector())
        .build()

    init {
        useController = false
        val item = MediaItem.fromUri(Uri.parse(src))
        exoPlayer.addMediaItem(item)
        exoPlayer.prepare()
        player = exoPlayer

        exoPlayer.addListener(object : Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when(playbackState) {
                    STATE_ENDED -> {
                        listener.videoPlayStateDidChangedWithState(KRVideoPlayState.KRVideoPlayStatePlayEnd, mapOf())
                    }
                    STATE_BUFFERING -> {
                        listener.videoPlayStateDidChangedWithState(KRVideoPlayState.KRVideoPlayStateCaching, mapOf())
                    }
                    STATE_READY -> {
                        if (exoPlayer.playWhenReady) {
                            listener.videoPlayStateDidChangedWithState(KRVideoPlayState.KRVideoPlayStatePlaying, mapOf())
                        } else {
                            listener.videoPlayStateDidChangedWithState(KRVideoPlayState.KRVideoPlayStatePaused, mapOf())
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                listener.videoPlayStateDidChangedWithState(KRVideoPlayState.KRVideoPlayStateFaild, mapOf())
            }

            override fun onRenderedFirstFrame() {
                super.onRenderedFirstFrame()
                listener.videoFirstFrameDidDisplay()
            }
        })
    }

    override fun preplay() {
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun stop() {
        exoPlayer.stop()
    }

    override fun setVideoContentMode(videoViewContentMode: KRVideoViewContentMode) {
    }

    override fun setMuted(muted: Boolean) {
        exoPlayer.isDeviceMuted = muted
    }

    override fun setRate(rate: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(rate)
    }

    override fun seekToTime(seekToTimeMs: Long) {
        exoPlayer.seekTo(seekToTimeMs)
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        // 处理自定义属性, 处理返回true，不处理返回false
        return false
    }

    override fun call(method: String, params: String?) {
        // 处理自定义方法调用
    }

}