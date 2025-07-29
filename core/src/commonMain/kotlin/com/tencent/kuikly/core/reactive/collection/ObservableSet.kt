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

package com.tencent.kuikly.core.reactive.collection

import com.tencent.kuikly.core.collection.fastHashSetOf
import com.tencent.kuikly.core.reactive.handler.ObservableCollectionElementChangeHandler

class ObservableSet<T>(
    private val innerSet: MutableSet<T> = fastHashSetOf(),
    handler: ObservableCollectionElementChangeHandler? = null,
    private val collectionMethodPropertyDelegate: CollectionMethodPropertyDelegate<T>
    = CollectionMethodPropertyDelegate(handler)
) : MutableSet<T> by innerSet, IObservableCollection by collectionMethodPropertyDelegate {

    override fun add(element: T): Boolean {
        return collectionMethodPropertyDelegate.add(innerSet, element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return collectionMethodPropertyDelegate.addAll(innerSet, elements)
    }

    override fun clear() {
        collectionMethodPropertyDelegate.clear(innerSet)
    }

    override fun remove(element: T): Boolean {
        return collectionMethodPropertyDelegate.remove(innerSet, element)
    }
    override fun removeAll(elements: Collection<T>): Boolean {
        return collectionMethodPropertyDelegate.removeAll(innerSet, elements)
    }
}