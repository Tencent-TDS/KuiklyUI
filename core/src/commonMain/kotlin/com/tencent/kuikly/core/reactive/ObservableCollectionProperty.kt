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

package com.tencent.kuikly.core.reactive

import com.tencent.kuikly.core.collection.fastHashSetOf
import com.tencent.kuikly.core.collection.toFastSet
import com.tencent.kuikly.core.reactive.collection.IObservableCollection
import com.tencent.kuikly.core.reactive.handler.ObservableCollectionElementChangeHandler
import com.tencent.kuikly.core.reactive.handler.PropertyAccessHandler
import kotlin.reflect.KProperty

class ObservableCollectionProperty<T : IObservableCollection>(
    initValue: T,
    handler: PropertyAccessHandler
) : ObservableProperties<T>(initValue, handler), ObservableCollectionElementChangeHandler {

    private var propertyName = ""
    private val otherCollectionElementChangeHandlerSet by lazy(LazyThreadSafetyMode.NONE) {
        fastHashSetOf<ObservableCollectionElementChangeHandler>()
    }

    init {
        value.collectionElementChangeHandler = this
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        propertyName = property.name
        return super.getValue(thisRef, property)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (this.value == value) {
            return
        }
        dismissOldValue(this.value)
        this.value = value
        if (this.value.collectionElementChangeHandler == null) {
            this.value.collectionElementChangeHandler = this
        } else {
            this.value.collectionElementChangeHandler?.addOtherHandler(this)
        }
        notifyValueChange(propertyName)
    }

    override fun onElementChange() {
        notifyValueChange(propertyName)
        if (otherCollectionElementChangeHandlerSet.isNotEmpty()) {
            otherCollectionElementChangeHandlerSet.toFastSet().forEach {
                it.onElementChange()
            }
        }
    }

    override fun addOtherHandler(otherHandler: ObservableCollectionElementChangeHandler) {
        if (otherHandler != this) {
            otherCollectionElementChangeHandlerSet.add(otherHandler)
        }
    }

    override fun removeOtherHandler(otherHandler: ObservableCollectionElementChangeHandler) {
        otherCollectionElementChangeHandlerSet.remove(otherHandler)
    }

    override fun firstOtherHandler(): ObservableCollectionElementChangeHandler? {
        if (otherCollectionElementChangeHandlerSet.isNotEmpty()) {
            return otherCollectionElementChangeHandlerSet.first()
        }
        return null
    }

    override fun getReactiveObserver() = handler?.getReactiveObserver()

    private fun dismissOldValue(oldValue: T) {
        oldValue.collectionElementChangeHandler?.removeOtherHandler(this)
        if (oldValue.collectionElementChangeHandler == this) { //old Value set nil 之前 handler流转
            oldValue.collectionElementChangeHandler?.firstOtherHandler()?.also {
                oldValue.collectionElementChangeHandler?.removeOtherHandler(it)
                oldValue.collectionElementChangeHandler = it
            }
            oldValue.collectionElementChangeHandler = null
        }
    }
}