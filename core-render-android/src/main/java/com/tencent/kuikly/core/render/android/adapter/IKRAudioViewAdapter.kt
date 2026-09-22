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
import com.tencent.kuikly.core.render.android.expand.component.KRAudioPlayState

/**
 * 音频播放器适配器。宿主通过实现该接口提供具体的音频播放实现（如 MediaPlayer / ExoPlayer），
 * 并注册到 [KuiklyRenderAdapterManager.krAudioViewAdapter]。
 *
 * 设计上对齐 [IKRVideoViewAdapter]，但去除了画面相关能力（resizeMode / 首帧）。
 */
interface IKRAudioViewAdapter {

    /**
     * @param context 上下文
     * @param src 数据源（音频 URL）
     * @param listener 播放器事件变化回调代理（播放状态变化 / 播放时间变化）
     */
    fun createAudioView(context: Context, src: String, listener: IKRAudioViewListener): IKRAudioView
}

interface IKRAudioView {

    /**
     * 播放音频
     */
    fun play()

    /**
     * 暂停音频
     */
    fun pause()

    /**
     * 停止并销毁音频
     */
    fun stop()

    /**
     * 设置倍速（1.0, 1.5, 2.0）
     */
    fun setRate(rate: Float)

    /**
     * seek 音频
     * @param seekToTimeMs 时间，单位毫秒
     */
    fun seekToTime(seekToTimeMs: Long)

    /**
     * kuikly 侧设置的属性，一般用于业务扩展使用
     */
    fun setProp(propKey: String, propValue: Any): Boolean

    /**
     * kuikly 侧调用方法，一般用于业务扩展使用
     */
    fun call(method: String, params: String?)
}

interface IKRAudioViewListener {

    /**
     * 播放状态发生变化时回调
     * @param playState 播放状态
     * @param extInfo 扩展参数
     */
    fun audioPlayStateDidChangedWithState(playState: KRAudioPlayState, extInfo: Map<String, String>)

    /**
     * 播放时间发生变化时回调
     * @param currentTime 当前播放时间，单位毫秒
     * @param totalTime 音频总时长，单位毫秒
     */
    fun playTimeDidChangedWithCurrentTime(currentTime: Long, totalTime: Long)
}
