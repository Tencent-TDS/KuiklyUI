package com.tencent.kuikly.compose.ui

import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import kotlin.coroutines.cancellation.CancellationException

internal actual fun areObjectsOfSameType(a: Any, b: Any): Boolean {
    return js(
        "Object.getPrototypeOf(a).constructor == Object.getPrototypeOf(b).constructor"
    ) as Boolean
}

// TODO: For non-JVM platforms, you can revive the kotlin-reflect implementation from
//  https://android-review.googlesource.com/c/platform/frameworks/support/+/2441379
@OptIn(ExperimentalComposeUiApi::class)
internal actual fun InspectorInfo.tryPopulateReflectively(
    element: ModifierNodeElement<*>
) {
}

internal actual abstract class PlatformOptimizedCancellationException actual constructor(
    message: String?
) : CancellationException(message)

internal actual fun getCurrentThreadId(): Long = 123
