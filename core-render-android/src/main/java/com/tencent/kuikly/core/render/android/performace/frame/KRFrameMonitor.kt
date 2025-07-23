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

import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.performace.KRMonitor

/**
 * 渲染性能追踪
 */
class KRFrameMonitor : KRMonitor<KRFrameData>(), IKRFrameCallback {

    companion object {
        private const val TAG = "KRFrameMonitor"
        const val MONITOR_NAME = "KRFrameMonitor"

        // 60fps 的刷新间隔（ns）
        private const val FRAME_INTERVAL_NANOS = 16666667L

        // 1毫秒的纳秒表示 1ms = 1000,000 ns
        private const val ONE_MILLI_SECOND_IN_NANOS = 1000000L
    }

    private var isStarted = false
    private var isResumed = false
    private var isInForeground = false
    private var lastFrameTimeNanos = 0L
    private var frameData = KRFrameData()
    val driveFrameDetector = KRDriveFrameDetector()

    override fun name(): String = MONITOR_NAME

    override fun onFirstFramePaint() {
        start()
    }

    /**
     * 开始监控
     */
    private fun start() {
        KuiklyRenderLog.i(TAG, "star")
        if (!isMainThread()) {
            KuiklyRenderLog.i(TAG, "star, must in main thread.")
            return
        }
        if (!isInForeground) {
            KuiklyRenderLog.i(TAG, "activity is not in foreground.")
            return
        }
        if (isStarted) {
            KuiklyRenderLog.i(TAG, "has start before.")
            return
        }
        lastFrameTimeNanos = 0L
        isStarted = true
        isResumed = true
        KRFrameDetector.register(this)
    }

    /**
     * 暂停监控
     */
    private fun pause() {
        if (!isStarted || !isResumed) {
            KuiklyRenderLog.i(TAG, "pause, isStarted: $isStarted, isResumed: $isResumed")
            return
        }
        isResumed = false
        lastFrameTimeNanos = 0L
        KRFrameDetector.unRegister(this)
    }

    /**
     * 恢复监控
     */
    private fun resume() {
        if (!isStarted || isResumed) {
            KuiklyRenderLog.i(TAG, "resume, isStarted: $isStarted, isResumed: $isResumed")
            return
        }
        isResumed = true
        lastFrameTimeNanos = 0L
        KRFrameDetector.register(this)
    }

    /**
     * 停止监控
     */
    private fun stop() {
        if (!isStarted) {
            KuiklyRenderLog.i(TAG, "stop, not start yet.")
            return
        }
        isStarted = false
        isResumed = false
        lastFrameTimeNanos = 0L
        KRFrameDetector.unRegister(this)
    }

    override fun onResume() {
        isInForeground = true
        resume()
    }

    override fun onPause() {
        isInForeground = false
        pause()
    }

    override fun onDestroy() {
        stop()
        KuiklyRenderLog.d(
            TAG,
            "${frameData}, fps: ${frameData.getFps()}, driveFps: ${frameData.getKuiklyFps()}"
        )
    }

    override fun getMonitorData(): KRFrameData {
        return frameData
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (frameTimeNanos < lastFrameTimeNanos || lastFrameTimeNanos == 0L) {
            lastFrameTimeNanos = frameTimeNanos
            return
        }

        val frameDurationTimeNanos = frameTimeNanos - lastFrameTimeNanos
        // 总耗时
        frameData.totalDuration += frameDurationTimeNanos / ONE_MILLI_SECOND_IN_NANOS

        // UI 线程卡顿耗时
        var hitchesDurationTimeMills = 0L
        if (frameDurationTimeNanos > FRAME_INTERVAL_NANOS) {
            hitchesDurationTimeMills =
                (frameDurationTimeNanos - FRAME_INTERVAL_NANOS) / ONE_MILLI_SECOND_IN_NANOS
        }

        frameData.hitchesDuration += hitchesDurationTimeMills
        frameData.frameCount++

        // core 线程卡顿耗时
        if (driveFrameDetector.isDriveFrame(lastFrameTimeNanos, frameTimeNanos)) {
            frameData.driveFrameCount++
            frameData.driveHitchesDuration += hitchesDurationTimeMills
        } else {
            // 如果非驱动帧，累计帧间隔
            frameData.driveHitchesDuration += frameDurationTimeNanos / ONE_MILLI_SECOND_IN_NANOS
        }

        // 更新帧时间戳
        lastFrameTimeNanos = frameTimeNanos
    }

    override fun isOpen(): Boolean {
        // 监控未启动，不进行帧回调
        return isStarted && isResumed
    }

}
