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

/**
 * 帧回调结果
 */
data class KRFrameData(
    var totalDuration: Long = 0L,
    var hitchesDuration: Long = 0L,
    var driveHitchesDuration: Long = 0L,
    var frameCount: Long = 0,
    var driveFrameCount: Long = 0L) {

    fun getFps(): Float {
        if (totalDuration > 0) {
            return (((totalDuration - hitchesDuration).toFloat() / totalDuration) * 60)
        }
        return 0f
    }

    fun getKuiklyFps(): Float {
        if (totalDuration > 0) {
            return (((totalDuration - driveHitchesDuration).toFloat() / totalDuration) * 60)
        }
        return 0f
    }

    override fun toString() = "[KRFrameMeta] \n" +
            "totalDuration: $totalDuration \n" +
            "hitchesDuration: $hitchesDuration \n" +
            "driveHitchesDuration: $driveHitchesDuration \n" +
            "frameCount: $frameCount \n" +
            "driveFrameCount: $driveFrameCount \n"

}