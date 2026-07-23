/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

import com.tencent.kuikly.core.collection.fastHashMapOf
import com.tencent.kuikly.core.collection.fastMutableSetOf
import com.tencent.kuikly.core.pager.PageData

/**
 * Tracks the raw pointer lifetime for each Kuikly page.
 *
 * Native RecyclerView does not enter DRAGGING until MOVE crosses touch slop. Pager offset
 * alignment must also be blocked between DOWN and that transition, otherwise it can move child
 * frames while the pending gesture still owns the old native offset.
 */
internal object TouchActivityTracker {
    private val activeOwnersByPage = fastHashMapOf<PageData, MutableSet<Any>>()

    fun onTouchDown(pageData: PageData, owner: Any) {
        activeOwnersByPage.getOrPut(pageData) { fastMutableSetOf() }.add(owner)
    }

    fun onTouchEnd(pageData: PageData, owner: Any) {
        val activeOwners = activeOwnersByPage[pageData] ?: return
        activeOwners.remove(owner)
        if (activeOwners.isEmpty()) {
            activeOwnersByPage.remove(pageData)
        }
    }

    fun isTouchActive(pageData: PageData?): Boolean {
        return pageData != null && activeOwnersByPage[pageData]?.isNotEmpty() == true
    }
}
