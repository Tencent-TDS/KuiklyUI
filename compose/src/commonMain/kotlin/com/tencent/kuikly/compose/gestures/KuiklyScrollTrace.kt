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

package com.tencent.kuikly.compose.gestures

/**
 * 滚动链路诊断：统计一次手势内各层调用次数，在 scrollEnd 时汇总打印。
 * 过滤：hilog | grep KuiklyScrollTrace
 */
internal object KuiklyScrollTrace {
    /** 调试时设为 true；发布前保持 false */
    const val ENABLED = false

    private const val TAG = "KuiklyScrollTrace"

    var composeScrollReceived = 0
    var composeDeltaFiltered = 0
    var composeEarlyReturn = 0
    var calculateContentSize = 0
    var kuiklyOnScroll = 0
    var tryExpandStartSize = 0
    var contentSizeToRender = 0
    var contentSizeDeduped = 0
    var lazyRemeasure = 0
    var lazyScrollWithoutRemeasure = 0

    inline fun ifEnabled(block: () -> Unit) {
        if (ENABLED) block()
    }

    fun reset() {
        composeScrollReceived = 0
        composeDeltaFiltered = 0
        composeEarlyReturn = 0
        calculateContentSize = 0
        kuiklyOnScroll = 0
        tryExpandStartSize = 0
        contentSizeToRender = 0
        contentSizeDeduped = 0
        lazyRemeasure = 0
        lazyScrollWithoutRemeasure = 0
    }

    fun dumpSummary(phase: String) {
        if (!ENABLED) return
        println(
            "[$TAG] $phase | " +
                "composeIn=$composeScrollReceived " +
                "filtered=$composeDeltaFiltered earlyRet=$composeEarlyReturn " +
                "calcSize=$calculateContentSize kuiklyScroll=$kuiklyOnScroll expand=$tryExpandStartSize " +
                "setFrame=$contentSizeToRender dedup=$contentSizeDeduped " +
                "remeasure=$lazyRemeasure noRemeasure=$lazyScrollWithoutRemeasure " +
                "(native fireToBridge see hilog KuiklyScrollTrace)"
        )
    }
}
