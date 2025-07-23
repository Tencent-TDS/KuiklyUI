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

package com.tencent.kuikly.core.render.android.performace.frame

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import androidx.annotation.UiThread
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.css.ktx.stackTraceToString

/**
 * 系统渲染帧回调监听管理
 */
class KRFrameDetector : Choreographer.FrameCallback {

    companion object {
        private const val TAG = "KRFrameManager"

        private val frameManager = KRFrameDetector()
        private val handler = Handler(Looper.getMainLooper())

        /**
         * 注册帧回调监听
         */
        fun register(listener: IKRFrameCallback) {
            if (isMainThread()) {
                frameManager.register(listener)
            } else {
                handler.post { frameManager.register(listener) }
            }
        }

        /**
         * 取消帧回调监听
         */
        fun unRegister(listener: IKRFrameCallback) {
            if (isMainThread()) {
                frameManager.unRegister(listener)
            } else {
                handler.post { frameManager.unRegister(listener) }
            }
        }
    }

    private val callbackList: HashSet<IKRFrameCallback> = HashSet<IKRFrameCallback>()
    private var choreographer: Choreographer? = null
    private var isStart = false
    private var isInit = false

    /**
     * 注册帧率监控
     *
     * @param listener 帧率回调接口
     */
    fun register(listener: IKRFrameCallback) {
        callbackList.add(listener)
        checkAndStart()
    }

    /**
     * 注销帧率监控
     *
     * @param listener 帧率回调接口
     */
    fun unRegister(listener: IKRFrameCallback) {
        callbackList.remove(listener)
        checkAndStop()
    }

    /**
     * 初始化, need call in Main Thread
     */
    @UiThread
    fun init() {
        if (isInit) {
            return
        }
        try {
            choreographer = Choreographer.getInstance()
            isInit = true
        } catch (t: Throwable) {
            KuiklyRenderLog.e(TAG, t.stackTraceToString())
        }
    }

    /**
     * 开启帧率监控
     */
    private fun checkAndStart() {
        if (isStart || callbackList.size == 0) {
            return
        }
        if (!isInit) {
            init()
        }
        choreographer?.apply {
            try {
                postFrameCallback(this@KRFrameDetector)
                isStart = true
            } catch (t: Throwable) {
                KuiklyRenderLog.e(TAG, t.stackTraceToString())
            }
            KuiklyRenderLog.d(TAG, "checkAndStart")
        }
    }

    /**
     * 关闭帧率监控
     */
    private fun checkAndStop() {
        if (!isStart || callbackList.size > 0) {
            return
        }
        try {
            choreographer?.removeFrameCallback(this)
            isStart = false
        } catch (t: Throwable) {
            KuiklyRenderLog.e(TAG, t.stackTraceToString())
        }
        KuiklyRenderLog.d(TAG, "checkAndStop")
    }

    /**
     * 帧回调
     */
    override fun doFrame(frameTimeNanos: Long) {
        callbackList.filter {
            it.isOpen()
        }.forEach {
            it.doFrame(frameTimeNanos)
        }
        if (isStart) {
            choreographer?.postFrameCallback(this)
        }
    }
}