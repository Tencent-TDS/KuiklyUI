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
 *
 * 验收时临时设 [ENABLED]=true；合入前保持 false。
 */
internal object KuiklyScrollTrace {
    /** 调试/验收时设为 true；发布前保持 false */
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
    var calcSizeSkipped = 0
    var kuiklyScrollNs = 0L
    var calcSizeNs = 0L

    // scroll audit（极致性能验收指标）
    var updateKuiklyViewFrameCalls = 0
    /** compute 之前的脏检查跳过（避免 viewPositionOf 坐标链 walk） */
    var framePreSkip = 0
    /** compute 之后 frame 未变跳过 */
    var frameSyncSkipped = 0
    /** placeSelf + delegate 同轮 placement 去重 */
    var framePlacementDedup = 0
    var frameComputeNs = 0L
    var resetVisibleSkipped = 0
    var contentOffsetWrites = 0
    var contentOffsetSkipped = 0
    var isDraggingWrites = 0
    var isDraggingSkipped = 0
    var contentSizeStateWrites = 0
    var contentSizeStateSkipped = 0
    var coordAccessMark = 0
    var coordAccessRelayout = 0
    var placementCoordAccess = 0

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
        calcSizeSkipped = 0
        kuiklyScrollNs = 0L
        calcSizeNs = 0L
        updateKuiklyViewFrameCalls = 0
        framePreSkip = 0
        frameSyncSkipped = 0
        framePlacementDedup = 0
        frameComputeNs = 0L
        resetVisibleSkipped = 0
        contentOffsetWrites = 0
        contentOffsetSkipped = 0
        isDraggingWrites = 0
        isDraggingSkipped = 0
        contentSizeStateWrites = 0
        contentSizeStateSkipped = 0
        coordAccessMark = 0
        coordAccessRelayout = 0
        placementCoordAccess = 0
    }

    fun dumpSummary(phase: String) {
        if (!ENABLED) return
        val scrollMs = (kuiklyScrollNs / 1_000_000.0 * 10).toLong() / 10.0
        val calcMs = (calcSizeNs / 1_000_000.0 * 10).toLong() / 10.0
        val frameMs = (frameComputeNs / 1_000_000.0 * 10).toLong() / 10.0
        val remeasureRate = if (kuiklyOnScroll > 0) {
            (lazyRemeasure * 1000 / kuiklyOnScroll) / 10.0
        } else 0.0
        println(
            "[$TAG] $phase | " +
                "composeIn=$composeScrollReceived filtered=$composeDeltaFiltered earlyRet=$composeEarlyReturn " +
                "calcSize=$calculateContentSize skipped=$calcSizeSkipped kuiklyScroll=$kuiklyOnScroll expand=$tryExpandStartSize " +
                "setFrame=$contentSizeToRender dedup=$contentSizeDeduped " +
                "remeasure=$lazyRemeasure noRemeasure=$lazyScrollWithoutRemeasure remeasureRate=${remeasureRate}% " +
                "scrollMs=$scrollMs calcMs=$calcMs frameMs=$frameMs | " +
                "audit: frameCalls=$updateKuiklyViewFrameCalls preSkip=$framePreSkip postSkip=$frameSyncSkipped placeDedup=$framePlacementDedup resetSkip=$resetVisibleSkipped " +
                "offW=$contentOffsetWrites offSkip=$contentOffsetSkipped " +
                "dragW=$isDraggingWrites dragSkip=$isDraggingSkipped " +
                "sizeW=$contentSizeStateWrites sizeSkip=$contentSizeStateSkipped " +
                "coordMark=$coordAccessMark coordRelayout=$coordAccessRelayout placeCoord=$placementCoordAccess"
        )
    }
}
