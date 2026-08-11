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

package com.tencent.kuikly.core.render.android.expand.module

import android.content.Context
import android.view.Choreographer
import android.view.Display
import android.view.WindowManager
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import kotlin.math.roundToInt

/**
 *  监听Vsync回调
 *
 *  created by zhenhuachen on 2025/4/27.
 */
class KRVsyncModule : KuiklyRenderBaseModule() {

    private var vsyncFrameCallback: Choreographer.FrameCallback? = null
    private var windowManager: WindowManager? = null

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_REGISTER_VSYNC -> registerVsync(callback)
            METHOD_UNREGISTER_VSYNC -> unRegisterVsync(callback)
            else -> super.call(method, params, callback)
        }
    }

    private fun registerVsync(callback: KuiklyRenderCallback?) {
        if (vsyncFrameCallback == null) {
            windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            vsyncFrameCallback = Choreographer.FrameCallback {
                callback?.invoke(currentFrameIntervalNanos())
                Choreographer.getInstance().postFrameCallback(vsyncFrameCallback)
            }
            Choreographer.getInstance().postFrameCallback(vsyncFrameCallback)
        }
    }

    @Suppress("DEPRECATION")
    private fun defaultDisplay(): Display? {
        return windowManager?.defaultDisplay
    }

    private fun currentFrameIntervalNanos(): Int {
        val refreshRate = defaultDisplay()?.refreshRate?.toDouble()
        val frameIntervalNanos =
            if (refreshRate != null && refreshRate.isFinite() && refreshRate > 0.0) {
                (NANOS_PER_SECOND / refreshRate).roundToInt()
            } else {
                DEFAULT_FRAME_INTERVAL_NANOS
            }
        return frameIntervalNanos.takeIf {
            it in MIN_FRAME_INTERVAL_NANOS..MAX_FRAME_INTERVAL_NANOS
        } ?: DEFAULT_FRAME_INTERVAL_NANOS
    }

    private fun unRegisterVsync(callback: KuiklyRenderCallback?) {
        if (vsyncFrameCallback != null) {
            Choreographer.getInstance().removeFrameCallback(vsyncFrameCallback)
            vsyncFrameCallback = null
            windowManager = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (vsyncFrameCallback != null) {
            Choreographer.getInstance().removeFrameCallback(vsyncFrameCallback)
            vsyncFrameCallback = null
            windowManager = null
        }
    }

    companion object {
        const val MODULE_NAME = "KRVsyncModule"
        const val METHOD_REGISTER_VSYNC = "registerVsync"
        const val METHOD_UNREGISTER_VSYNC = "unRegisterVsync"

        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val DEFAULT_FRAME_INTERVAL_NANOS = 16_666_667
        private const val MIN_FRAME_INTERVAL_NANOS = 1_000_000
        private const val MAX_FRAME_INTERVAL_NANOS = 100_000_000
    }
}
