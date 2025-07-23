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

class CollectionMethodPropertyDelegate<T>(
    private var handler: ObservableCollectionElementChangeHandler? = null
) : IObservableCollection {

    private val collectionOperations = fastArrayListOf<CollectionOperation>()

    override val collectionOperation: List<CollectionOperation>
        get() = this.collectionOperations

    override var collectionElementChangeHandler: ObservableCollectionElementChangeHandler?
        get() = handler
        set(value) {
            handler = value
        }

    private fun notifyElementChanged() {
        handler?.onElementChange()
        val observer = handler?.getReactiveObserver()
        if (observer != null) {
            observer.addLazyTaskUtilEndCollectDependency {
                collectionOperations.clear()
            }
        } else {
            collectionOperations.clear()
        }
    }

    fun add(mutableCollection: MutableCollection<T>, element: T): Boolean {
        val result = mutableCollection.add(element)
        if (result) {
            addOperation(mutableCollection.size - 1, 1)
        }
        notifyElementChanged()
        return result
    }

    fun add(mutableCollection: MutableList<T>, index: Int, element: T) {
        mutableCollection.add(index, element)
        addOperation(index, 1)
        notifyElementChanged()
    }

    fun addAll(mutableCollection: MutableCollection<T>, elements: Collection<T>): Boolean {
        val result = mutableCollection.addAll(elements)
        if (result) {
            addOperation(mutableCollection.size - elements.size, elements.size)
        }
        notifyElementChanged()
        return result
    }

    fun addAll(mutableCollection: MutableList<T>, index: Int, elements: Collection<T>): Boolean {
        val result = mutableCollection.addAll(index, elements)
        if (result) {
            addOperation(index, elements.size)
        }
        notifyElementChanged()
        return result
    }

    fun removeAll(mutableCollection: MutableCollection<T>, elements: Collection<T>): Boolean {
        var result = false
        for (ele in elements) {
            val removeSuccess = remove(mutableCollection, ele)
            if (removeSuccess) {
                result = true
            }
        }
        return result
    }

    fun remove(mutableCollection: MutableCollection<T>, element: T): Boolean {
        val index = mutableCollection.indexOf(element)
        val result = mutableCollection.remove(element)
        if (result) {
            removeOperation(index, 1)
        }
        notifyElementChanged()
        return result
    }

    fun removeAt(mutableCollection: MutableList<T>, index: Int): T {
        val result = mutableCollection.removeAt(index)
        if (result != null) {
            removeOperation(index, 1)
        }
        notifyElementChanged()
        return result
    }

    fun removeByIterator(iterator: MutableListIterator<T>) {
        val index = iterator.previousIndex()
        if (index != -1) {
            iterator.remove()
            removeOperation(index, 1)
            notifyElementChanged()
        }
    }

    fun clear(mutableCollection: MutableCollection<T>) {
        val size = mutableCollection.size
        mutableCollection.clear()
        removeOperation(0, size)
        notifyElementChanged()
    }

    fun set(mutableCollection: MutableList<T>, index: Int, element: T): T {
        val oldElement = mutableCollection.set(index, element)
        if (oldElement != null) {
            removeOperation(index, 1)
        }
        addOperation(index, 1)
        notifyElementChanged()
        return oldElement
    }

    private fun addOperation(index: Int, count: Int) {
        collectionOperations.add(
            CollectionOperation(
                CollectionOperation.OPERATION_TYPE_ADD,
                index, count
            )
        )
    }

    private fun removeOperation(index: Int, count: Int) {
        collectionOperations.add(
            CollectionOperation(
                CollectionOperation.OPERATION_TYPE_REMOVE,
                index, count
            )
        )
    }
}