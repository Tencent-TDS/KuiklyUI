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

import com.tencent.kuikly.core.reactive.handler.PropertyAccessHandler
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class ObservableProperties<V>(
    protected var value: V,
    protected val handler: PropertyAccessHandler?
) : ReadWriteProperty<Any?, V> {

    private val propertyOwnerId = propertyOwnerIdProducer++.toString()

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        notifyGetValue(property.name)
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        if (this.value == value) {
            return
        }
        val oldValue = this.value
        this.value = value
        notifyValueChange(property.name)
    }

    protected fun notifyGetValue(propertyName: String) {
        handler?.onGetValue(propertyOwnerId, propertyName)
    }

    protected fun notifyValueChange(propertyName: String) {
        handler?.onValueChange(propertyOwnerId, propertyName)
    }

    companion object {
        private var propertyOwnerIdProducer = 0
    }

}

internal typealias PropertyOwner = String
