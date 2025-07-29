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

import androidx.compose.runtime.mutableStateOf
import com.tencent.kuikly.compose.foundation.MutatePriority
import com.tencent.kuikly.compose.foundation.MutatorMutex
import com.tencent.kuikly.compose.foundation.gestures.ScrollScope
import com.tencent.kuikly.compose.foundation.gestures.ScrollableState
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.scroller.applyScrollViewOffsetDelta
import com.tencent.kuikly.compose.scroller.isValidOffsetDelta
import com.tencent.kuikly.core.views.ScrollParams
import kotlinx.coroutines.coroutineScope

internal class KuiklyScrollableState(val onDelta: (Float) -> Float) : ScrollableState {

    override var contentPadding: PaddingValues = PaddingValues(0.dp)

    val kuiklyInfo: KuiklyScrollInfo = KuiklyScrollInfo()

    private val scrollScope: ScrollScope = object : ScrollScope {
        override fun scrollBy(pixels: Float): Float {
            if (pixels.isNaN()) return 0f
            val delta = onDelta(pixels)
            if (delta == 0f) return 0f

            if (!kuiklyInfo.offsetDirty && isValidOffsetDelta(delta.toInt())) {
                applyScrollViewOffsetDelta(delta.toInt())
            } else {
                kuiklyInfo.offsetDirty = true
            }

            isLastScrollForwardState.value = delta > 0
            isLastScrollBackwardState.value = delta < 0
            return delta
        }
    }

    private val scrollMutex = MutatorMutex()

    private val isScrollingState = mutableStateOf(false)
    private val isLastScrollForwardState = mutableStateOf(false)
    private val isLastScrollBackwardState = mutableStateOf(false)

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ): Unit = coroutineScope {
        scrollMutex.mutateWith(scrollScope, scrollPriority) {
            isScrollingState.value = true
            try {
                block()
            } finally {
                isScrollingState.value = false
            }
        }
    }

    fun kuiklyOnScroll(pixels: Float): Float {
        isScrollingState.value = true
        if (pixels.isNaN()) return 0f
        val delta = onDelta(pixels)
        isLastScrollForwardState.value = delta > 0
        isLastScrollBackwardState.value = delta < 0
        return delta
    }

    fun kuiklyOnScrollEnd(params: ScrollParams) {
        isScrollingState.value = false
    }

    override fun dispatchRawDelta(delta: Float): Float {

        val consumed = onDelta(delta)
        if (consumed == 0f) return 0f

        if (!kuiklyInfo.offsetDirty && isValidOffsetDelta(consumed.toInt())) {
            applyScrollViewOffsetDelta(consumed.toInt())
        } else {
            kuiklyInfo.offsetDirty = true
        }
        return consumed
    }

    override val isScrollInProgress: Boolean
        get() = isScrollingState.value

    override val lastScrolledForward: Boolean
        get() = isLastScrollForwardState.value

    override val lastScrolledBackward: Boolean
        get() = isLastScrollBackwardState.value
}
