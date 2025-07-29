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

import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.reactive.handler.ObservableCollectionElementChangeHandler

class ObservableList<T>(
    private val innerList: MutableList<T> = fastArrayListOf(),
    handler: ObservableCollectionElementChangeHandler? = null,
    private val collectionMethodPropertyDelegate: CollectionMethodPropertyDelegate<T>
    = CollectionMethodPropertyDelegate(handler)
) : MutableList<T> by innerList, IObservableCollection by collectionMethodPropertyDelegate {

    private inner class Itr(private val innerIterator: MutableListIterator<T>) : MutableIterator<T> by innerIterator {
        override fun remove() {
            collectionMethodPropertyDelegate.removeByIterator(innerIterator)
        }
    }

    override fun iterator(): MutableIterator<T> {
        return Itr(innerList.listIterator())
    }

    override fun add(element: T): Boolean {
        return collectionMethodPropertyDelegate.add(innerList, element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return collectionMethodPropertyDelegate.addAll(innerList, elements)
    }

    override fun clear() {
        collectionMethodPropertyDelegate.clear(innerList)
    }

    override fun add(index: Int, element: T) {
        collectionMethodPropertyDelegate.add(innerList, index, element)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        return collectionMethodPropertyDelegate.addAll(innerList, index, elements)
    }

    override fun removeAt(index: Int): T {
        return collectionMethodPropertyDelegate.removeAt(innerList, index)
    }

    override fun remove(element: T): Boolean {
        return collectionMethodPropertyDelegate.remove(innerList, element)
    }

    override fun set(index: Int, element: T): T {
        return collectionMethodPropertyDelegate.set(innerList, index, element)
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return collectionMethodPropertyDelegate.removeAll(innerList, elements)
    }
}