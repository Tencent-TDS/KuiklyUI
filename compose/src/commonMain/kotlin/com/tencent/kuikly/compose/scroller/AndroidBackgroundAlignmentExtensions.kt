/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.compose.scroller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.tencent.kuikly.compose.foundation.drawer.DrawerInternalPagerState
import com.tencent.kuikly.compose.foundation.gestures.ScrollableState
import com.tencent.kuikly.compose.foundation.pager.PagerState
import com.tencent.kuikly.lifecycle.DefaultLifecycleObserver
import com.tencent.kuikly.lifecycle.LifecycleOwner
import com.tencent.kuikly.lifecycle.compose.LocalLifecycleOwner

internal class AndroidBackgroundAlignmentState {
    private var isPageVisible = true
    private var needsOffsetRealignment = false

    fun deferIfPageInvisible(): Boolean {
        if (isPageVisible) {
            return false
        }
        needsOffsetRealignment = true
        return true
    }

    fun onPageInvisible() {
        isPageVisible = false
    }

    fun consumeRealignmentOnPageVisible(): Boolean {
        isPageVisible = true
        if (!needsOffsetRealignment) {
            return false
        }
        needsOffsetRealignment = false
        return true
    }
}

private fun ScrollableState.isPagerState(): Boolean {
    return this is PagerState || this is DrawerInternalPagerState
}

internal fun ScrollableState.shouldDeferAndroidOffsetAlignment(): Boolean {
    val scrollInfo = kuiklyInfo
    return scrollInfo.pageData?.isAndroid == true &&
        !isPagerState() &&
        scrollInfo.androidBackgroundAlignmentState.deferIfPageInvisible()
}

@Composable
internal fun ScrollableState.AndroidBackgroundAlignmentEffect() {
    if (kuiklyInfo.pageData?.isAndroid != true || isPagerState()) {
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, this) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                kuiklyInfo.androidBackgroundAlignmentState.onPageInvisible()
            }

            override fun onResume(owner: LifecycleOwner) {
                if (kuiklyInfo.androidBackgroundAlignmentState.consumeRealignmentOnPageVisible()) {
                    kuiklyInfo.updateContentSizeToRender()
                    tryExpandStartSizeNoScroll(forceExpand = true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
