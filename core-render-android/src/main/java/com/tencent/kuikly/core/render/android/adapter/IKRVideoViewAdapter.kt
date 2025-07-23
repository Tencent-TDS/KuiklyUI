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

package com.tencent.kuikly.core.render.android.adapter

import android.content.Context
import com.tencent.kuikly.core.render.android.expand.component.KRVideoPlayState
import com.tencent.kuikly.core.render.android.expand.component.KRVideoViewContentMode

/**
 * Created by kam on 2023/6/17.
 */
interface IKRVideoViewAdapter {

    /**
     * @param context
     * @param src 数据源
     * @param listener 播放器事件变化回调代理(如：播放状态变化回调 or 播放时间变化回调该代理)
     */
    fun createVideoView(context: Context, src: String, listener: IKRVideoViewListener): IKRVideoView
}

interface IKRVideoView {

    /**
     * 预播放到第一帧（停在第一帧，用于预加载优化）
     */
    fun preplay()

    /**
     * 播放视频
     */
    fun play()

    /**
     * 暂停视频
     */
    fun pause()

    /**
     * 停止并销毁视频
     */
    fun stop()

    /**
     * 设置画面拉伸模式
     */
    fun setVideoContentMode(videoViewContentMode: KRVideoViewContentMode)

    /**
     * 设置静音属性
     */
    fun setMuted(muted: Boolean)

    /**
     * 设置倍速（1.0, 1.5, 2.0）
     */
    fun setRate(rate: Float)

    /**
     * seek视频
     * @param seekToTimeMs 时间，单位毫秒
     */
    fun seekToTime(seekToTimeMs: Long)

    /**
     * kuikly侧设置的属性，一般用于业务扩展使用
     */
    fun setProp(propKey: String, propValue: Any): Boolean

    /**
     * kuikly侧调用方法，一般用于业务扩展使用
     */
    fun call(method: String, params: String?)

}

interface IKRVideoViewListener {

    /**
     * 播放状态发生变化时回调
     * @param playState 播放状态
     * @param extInfo 扩展参数
     */
    fun videoPlayStateDidChangedWithState(playState: KRVideoPlayState, extInfo: Map<String, String>)

    /**
     * 播放时间发生变化时回调
     * @param currentTime 当前播放时间，单位毫秒
     * @param totalTime 视频总时长，单位毫秒
     */
    fun playTimeDidChangedWithCurrentTime(currentTime: Long, totalTime: Long)

    /**
     * 视频首帧画面上屏显示时回调该方法（kotlin侧通过该时机来隐藏视频封面）
     */
    fun videoFirstFrameDidDisplay()

    /**
     * 业务自定义扩展事件通用事件通道
     */
    fun customEventWithInfo(eventInfo: Map<String, String>)
}