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
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tencent.kuikly.core.render.android.adapter.IKRVideoView
import com.tencent.kuikly.core.render.android.adapter.IKRVideoViewAdapter
import com.tencent.kuikly.core.render.android.adapter.IKRVideoViewListener
import com.tencent.kuikly.core.render.android.expand.component.KRVideoPlayState
import com.tencent.kuikly.core.render.android.expand.component.KRVideoViewContentMode

class VideoViewAdapter : IKRVideoViewAdapter {
    override fun createVideoView(context: Context, src: String, listener: IKRVideoViewListener): IKRVideoView {
        return KuiklyVideoView(context, src, listener)
    }
}

class KuiklyVideoView(context: Context, private val src: String, private val listener: IKRVideoViewListener) : PlayerView(context), IKRVideoView {

    private val exoPlayer = ExoPlayer.Builder(context)
        .setTrackSelector(DefaultTrackSelector())
        .build()

    private var isPreload = false
    init {
        useController = false
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
                            changeProgressHandler(true)
                            listener.videoPlayStateDidChangedWithState(KRVideoPlayState.KRVideoPlayStatePlaying, mapOf())
                        } else {
                            changeProgressHandler(false)
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
                listener.videoFirstFrameDidDisplay()
                super.onRenderedFirstFrame()
            }
        })
    }

    override fun preplay() {
        if (!isPreload) {
            val mediaItem = MediaItem.fromUri(src)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            isPreload = true
        }

    }

    override fun play() {
        preplay()
        changeProgressHandler(true)
        exoPlayer.play()
    }

    override fun pause() {
        changeProgressHandler(false)
        exoPlayer.pause()
    }

    override fun stop() {
        progressHandler.removeCallbacks(progressRunnable)
        exoPlayer.stop()
        isPreload = false
        player = null
    }

    override fun setVideoContentMode(videoViewContentMode: KRVideoViewContentMode) {
    }

    override fun setMuted(muted: Boolean) {
//        if (exoPlayer?.isDeviceMuted != muted) {
//            exoPlayer?.isDeviceMuted = muted
//        }
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
        if (method == "preload") {
            preplay()
        } else if (method == "seekToTime") {
            params?.toLong()?.let { seekToTime(it) }
        }
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            val currentPlayer = player
            if (currentPlayer != null && currentPlayer.playbackState == Player.STATE_READY && currentPlayer.isPlaying) {
                val currentPosition = currentPlayer.currentPosition
                val bufferedPosition = currentPlayer.bufferedPosition
                val duration = currentPlayer.duration
                listener.playTimeDidChangedWithCurrentTime(currentPosition, duration)
            }
            // 0.1秒后再次执行
            progressHandler.postDelayed(this, 100)
        }
    }

    private val progressHandler = Handler(Looper.myLooper()!!)

    private var isProgressRunning = false

    private fun changeProgressHandler(isRun: Boolean) {
        if (isRun && !isProgressRunning) {
            progressHandler.post(progressRunnable)
            isProgressRunning = true
        } else if (!isRun && isProgressRunning) {
            progressHandler.removeCallbacks(progressRunnable)
            isProgressRunning = false
        }
    }
}