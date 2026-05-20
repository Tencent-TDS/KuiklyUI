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

package com.tencent.kuikly.compose.ui.node

import com.tencent.kuikly.core.base.ViewContainer

/**
 * Compose-side helpers for movableContentOf support.
 *
 * The methods that need access to private/protected fields of [ViewContainer]
 * (removeChildForMove, reinsertChild, removeDomSubViewForMove, removeChildrenForMoveAll)
 * live in [ViewContainer] itself, grouped under the "Compose movableContent support" region.
 *
 * This file holds the higher-level orchestration helpers used by [KNode] that only
 * need the public API of [ViewContainer].
 */

/**
 * Removes [count] children starting at [index] using the lightweight move path:
 * flex node is removed but the native render view is kept alive for reinsertion.
 */
internal fun ViewContainer<*, *>.removeChildrenForMove(index: Int, count: Int) {
    val parentView = realContainerView()
    for (i in index until index + count) {
        val childView = parentView.getChild(index)
        parentView.removeDomSubViewForMove(childView)
        parentView.removeChildForMove(childView)
    }
}
