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

package com.tencent.kuikly.core.render.android.fold

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.util.Consumer
import androidx.window.java.layout.WindowInfoTrackerCallbackAdapter
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import java.util.concurrent.Executor

/** Jetpack [FoldingFeature] → [hingeStatus] rawValue。
 *
 * - `HALF_OPENED` → [PARTIALLY_OPEN]（2）；`FLAT` → [FULLY_OPEN]（3）
 * - 否则：折叠屏 → [CLOSED]（1），否则 → [UNKNOWN]（0）
 * - [hingeRaw] 初始为 [UNKNOWN]，首次 [WindowLayoutInfo] 回调后再写入 1/2/3。
 */
internal class KuiklyWindowFoldTracker(context: Context) {

    companion object {
        private const val TAG = "KuiklyFoldHinge"

        const val UNKNOWN = 0
        const val CLOSED = 1
        const val PARTIALLY_OPEN = 2
        const val FULLY_OPEN = 3

        @Volatile
        private var isFoldableDevice: Boolean? = null

        private fun isFoldableDeviceOnce(context: Context): Boolean {
            isFoldableDevice?.let { return it }
            val detected =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    false
                } else {
                    context.applicationContext.packageManager.hasSystemFeature(
                        PackageManager.FEATURE_SENSOR_HINGE_ANGLE,
                    )
                }
            isFoldableDevice = detected
            return detected
        }
    }

    private val appContext = context.applicationContext
    private val foldableDevice = isFoldableDeviceOnce(context)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor: Executor = Executor { command -> mainHandler.post(command) }

    private var tracker: WindowInfoTrackerCallbackAdapter? = null
    private var layoutListener: Consumer<WindowLayoutInfo>? = null
    /** [start] 因宿主 Window 库不兼容失败时置 true，避免反复 crash。 */
    private var windowLayoutUnavailable = false

    private var hingeRaw = UNKNOWN

    val lastHingeStatusRaw: Int
        get() = hingeRaw

    var onHingeStatusChanged: (() -> Unit)? = null

    fun start(activity: Activity) {
        if (layoutListener != null || windowLayoutUnavailable) {
            return
        }
        val listener = Consumer<WindowLayoutInfo> { info ->
            val next = mapHingeRaw(info)
            if (next == hingeRaw) {
                return@Consumer
            }
            hingeRaw = next
            onHingeStatusChanged?.invoke()
        }
        try {
            val adapter = tracker
                ?: WindowInfoTrackerCallbackAdapter(WindowInfoTracker.getOrCreate(appContext))
                    .also { tracker = it }
            adapter.addWindowLayoutInfoListener(activity, mainExecutor, listener)
            layoutListener = listener
        } catch (t: Throwable) {
            layoutListener = null
            tracker = null
            windowLayoutUnavailable = true
            KuiklyRenderLog.e(
                TAG,
                "WindowLayoutInfo listener unavailable (check androidx.window version): " +
                    "${t.javaClass.simpleName}",
            )
        }
    }

    fun stop() {
        val listener = layoutListener ?: return
        try {
            tracker?.removeWindowLayoutInfoListener(listener)
        } catch (t: Throwable) {
            KuiklyRenderLog.e(TAG, "removeWindowLayoutInfoListener failed: ${t.javaClass.simpleName}")
        }
        layoutListener = null
    }

    private fun mapHingeRaw(info: WindowLayoutInfo): Int {
        val folds = info.displayFeatures.filterIsInstance<FoldingFeature>()
        if (folds.any { it.state == FoldingFeature.State.HALF_OPENED }) {
            return PARTIALLY_OPEN
        }
        if (folds.any { it.state == FoldingFeature.State.FLAT }) {
            return FULLY_OPEN
        }
        return if (foldableDevice) CLOSED else UNKNOWN
    }
}
