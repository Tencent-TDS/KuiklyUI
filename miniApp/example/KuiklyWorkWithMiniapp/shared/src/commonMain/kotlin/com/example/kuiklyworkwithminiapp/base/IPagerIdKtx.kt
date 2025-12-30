package com.example.kuiklyworkwithminiapp.base

import com.tencent.kuikly.core.base.IPagerId
import com.tencent.kuikly.core.base.pagerId

/**
 * 老的方式:，需要显式传递 pagerId
 * ```kotlin
 * Utils.bridgeModule(pagerId).reportPageCostTimeForError()
 * ```
 *
 * 新方式：无需显式传递 pagerId
 * ```kotlin
 * bridgeModule.reportPageCostTimeForError()
 * ```
 */
internal val IPagerId.bridgeModule: BridgeModule by pagerId {
    Utils.bridgeModule(it)
}

internal fun IPagerId.setTimeout(delay: Int, callback: () -> Unit): String {
    return com.tencent.kuikly.core.timer.setTimeout(pagerId, delay, callback)
}