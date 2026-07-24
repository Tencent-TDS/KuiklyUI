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

package com.tencent.kuikly.core.module

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

data class VsyncFrameInfo(
    val timestampMillis: Double,
    val targetTimestampMillis: Double,
    val frameIntervalMillis: Double,
)

/**
 *  监听Vsync回调
 *  created by zhenhuachen on 2025/4/27.
 */
class VsyncModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    fun registerVsync(callback: () -> Unit) {
        registerVsyncInternal(
            maxFramesPerSecond = 0,
            callback = { callback() },
        )
    }

    fun registerVsyncWithFrameInfo(
        maxFramesPerSecond: Int,
        callback: (VsyncFrameInfo) -> Unit,
    ) {
        registerVsyncInternal(maxFramesPerSecond) { data ->
            val rawFrameIntervalMillis =
                data?.optDouble(KEY_FRAME_INTERVAL_SECONDS, 0.0)?.times(MILLIS_PER_SECOND) ?: 0.0
            val frameIntervalMillis =
                rawFrameIntervalMillis.takeIf {
                    it in MIN_FRAME_INTERVAL_MILLIS..MAX_FRAME_INTERVAL_MILLIS
                } ?: DEFAULT_FRAME_INTERVAL_MILLIS
            callback(
                VsyncFrameInfo(
                    timestampMillis =
                        data?.optDouble(KEY_TIMESTAMP_SECONDS, 0.0)?.times(MILLIS_PER_SECOND) ?: 0.0,
                    targetTimestampMillis =
                        data?.optDouble(KEY_TARGET_TIMESTAMP_SECONDS, 0.0)?.times(MILLIS_PER_SECOND)
                            ?: 0.0,
                    frameIntervalMillis = frameIntervalMillis,
                ),
            )
        }
    }

    private fun registerVsyncInternal(
        maxFramesPerSecond: Int,
        callback: (JSONObject?) -> Unit,
    ) {
        toNative(
            keepCallbackAlive = true,
            methodName = METHOD_REGISTER_VSYNC,
            syncCall = false,
            param = JSONObject().apply {
                put(KEY_MAX_FRAMES_PER_SECOND, maxFramesPerSecond)
            },
            callback = callback,
        )
    }

    fun unRegisterVsync() {
        toNative(
            keepCallbackAlive = false,
            methodName = METHOD_UNREGISTER_VSYNC,
            syncCall = false,
            param = null
        )
    }

    companion object {
        const val MODULE_NAME = ModuleConst.VSYNC
        const val METHOD_REGISTER_VSYNC = "registerVsync"
        const val METHOD_UNREGISTER_VSYNC = "unRegisterVsync"

        private const val KEY_MAX_FRAMES_PER_SECOND = "maxFramesPerSecond"
        private const val KEY_TIMESTAMP_SECONDS = "timestampSeconds"
        private const val KEY_TARGET_TIMESTAMP_SECONDS = "targetTimestampSeconds"
        private const val KEY_FRAME_INTERVAL_SECONDS = "frameIntervalSeconds"

        private const val MILLIS_PER_SECOND = 1_000.0
        private const val DEFAULT_FRAME_INTERVAL_MILLIS = 1_000.0 / 60.0
        private const val MIN_FRAME_INTERVAL_MILLIS = 1.0
        private const val MAX_FRAME_INTERVAL_MILLIS = 100.0
    }
}
