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

package com.tencent.kuikly.compose.animation.core

import com.tencent.kuikly.core.base.NativeAnimationBridge
import com.tencent.kuikly.core.manager.PagerManager

internal fun commitPendingNativeAnimationProperties() {
    NativeAnimationCoordinator.currentOrNull()?.commitStagedProperties()
}

internal fun destroyCurrentNativeAnimationCoordinator() {
    try {
        (PagerManager.getCurrentPager().getValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY)
            as? NativeAnimationCoordinator)?.destroy()
    } catch (_: Throwable) {
        // The pager may already have been removed by its owner.
    }
}
