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

import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.collection.fastHashMapOf
import com.tencent.kuikly.core.collection.fastHashSetOf
import com.tencent.kuikly.core.collection.fastLinkedHashSetOf
import com.tencent.kuikly.core.collection.toFastList
import com.tencent.kuikly.core.collection.toFastMutableSet
import com.tencent.kuikly.core.collection.toFastSet
import com.tencent.kuikly.core.exception.ReactiveObserverNotFoundException
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.collection.ObservableSet
import com.tencent.kuikly.core.reactive.handler.PropertyAccessHandler
import kotlin.properties.ReadWriteProperty

/*
 * @brief 响应式实现类
 */
class ReactiveObserver {

    private val activeReadPropertyNames = fastHashSetOf<String>() // 当前待收集依赖的key
    private val activeWritePropertyNames = fastHashSetOf<String>()
    private val propertyObserverFnMap = fastHashMapOf<String, MutableSet<ObserverFn>>() // 对key感兴趣的观察者
    private val observerRemoveFnOwnerMap = fastHashMapOf<Int, MutableSet<ObserverRemoveFn>>()
    private val observerFnCollectionPropertiesMap = fastHashMapOf<ObserverFn, Set<String>>() // 闭包收集的属性
    private val endCollectDependencyTasks = fastArrayListOf<() -> Unit>()
    private var startCollectDependency = false
    var currentObservablePropertyKey: String = ""
        private set
    // 当前修改中的属性key，与上面这个区别是该属性read操作不会记录
    var currentChangingPropertyKey: String? = null
        private set

    // 该方法为外部使用
    fun bindValueChange(
        valueBlock: () -> Any,
        byOwner: Any,
        valueChange: (value: Any) -> Unit
    ) {
        bindValueChange(byOwner) {
            val value = valueBlock()
            addLazyTaskUtilEndCollectDependency {
                valueChange(value)
            }
        }
    }

    // 该方法仅Core内部使用，若有需求，请使用方法bindValueChange(valueBlock,byOwner,valueChanged)代替
    fun bindValueChange(byOwner: Any, valueChange: (first: Boolean) -> Unit) : Boolean {
        startCollectDependency() // 开启感兴趣的key收集标记
        valueChange(true)
        val result = addObserver(byOwner) {
            valueChange(false)
            byOwner
        }
        endCollectDependency()
        return result
    }

    fun unbindValueChange(byOwner: Any) {
        removeObserver(byOwner)
    }

    private fun startCollectDependency() {
        startCollectDependency = true
    }

    private fun endCollectDependency() {
        startCollectDependency = false
        if (endCollectDependencyTasks.isNotEmpty()) {
            val tasks = endCollectDependencyTasks.toFastList()
            endCollectDependencyTasks.clear()
            tasks.forEach {
                it()
            }
        }
    }

    fun addLazyTaskUtilEndCollectDependency(task: () -> Unit) {
        if (startCollectDependency) {
            endCollectDependencyTasks.add(task)
        } else {
            task()
        }
    }

    private fun addActivePropertyObserver(
        propertyName: String,
        observerFnOwner: Any,
        observerFn: ObserverFn
    ) {
        // 正向收集依赖
        val observerFnSet = collectObserver(observerFn, propertyName)
        // 逆向解依赖收集
        observerFnSet?.also {
            collectObserverOwnerRemoveFn(observerFnOwner) {
                observerFnSet.also {
                    it.remove(observerFn)
                    observerFnCollectionPropertiesMap.remove(observerFn)
                }
                if (observerFnSet.isEmpty()) {
                    propertyObserverFnMap.remove(propertyName)
                }
            }
        }
    }

    fun addObserver(observerFnOwner: Any, observerFn: ObserverFn) : Boolean {
        if (activeReadPropertyNames.isEmpty()) {
            activeWritePropertyNames.clear()
            return false
        }
        // 如果收集读的key集合中包含写集合的key，需要取消对该key的依赖收集，避免循环嵌套递归响应
        val readPropertyKeyList = activeReadPropertyNames.toFastMutableSet()

        activeWritePropertyNames.toFastSet().forEach { propertyName ->
            if (readPropertyKeyList.contains(propertyName)) {
                readPropertyKeyList.remove(propertyName)
            }
        }
        try {
            readPropertyKeyList.forEach { propertyName ->
                addActivePropertyObserver(propertyName, observerFnOwner, observerFn)
            }
            observerFnCollectionPropertiesMap[observerFn] = readPropertyKeyList
        } finally {
            activeReadPropertyNames.clear()
            activeWritePropertyNames.clear()
        }
        return true
    }

    fun removeObserver(observerFnOwner: Any) {
        observerRemoveFnOwnerMap.remove(observerFnOwner.hashCode())?.onEach { observerRemoveFn ->
            observerRemoveFn()
        }
    }

    internal fun destroy() {
        activeReadPropertyNames.clear()
        activeWritePropertyNames.clear()
        propertyObserverFnMap.clear()
        observerRemoveFnOwnerMap.clear()
        observerFnCollectionPropertiesMap.clear()
        endCollectDependencyTasks.clear()
        endCollectDependencyTasks.clear()
    }

    private fun collectObserver(
        observerFn: ObserverFn,
        propertyName: String
    ): MutableSet<ObserverFn>? {
        var observerList = propertyObserverFnMap[propertyName]
        if (observerList == null) {
            observerList = fastLinkedHashSetOf<ObserverFn>()
            propertyObserverFnMap[propertyName] = observerList
        }
        if (observerList.add(observerFn)) {
            return observerList
        }
        return null
    }

    private fun collectObserverOwnerRemoveFn(
        observerFnOwner: Any,
        observerRemoveFn: ObserverRemoveFn
    ) {
        val hashCode = observerFnOwner.hashCode()
        var observerRemoveFnSet = observerRemoveFnOwnerMap[hashCode]
        if (observerRemoveFnSet == null) {
            observerRemoveFnSet = fastLinkedHashSetOf()
            observerRemoveFnOwnerMap[hashCode] = observerRemoveFnSet
        }
        observerRemoveFnSet.add(observerRemoveFn)
    }

    // get value callback
    internal fun notifyGetValue(propertyOwner: PropertyOwner, propertyName: String) {
        checkThread()
        if (!startCollectDependency) {
            return
        }
        activeReadPropertyNames.add(buildPropertyKey(propertyOwner, propertyName))
    }

    // set value callback
    internal fun notifyPropertyObserver(propertyOwner: PropertyOwner, propertyName: String) {
        checkThread()
        val propertyKey = buildPropertyKey(
            propertyOwner,
            propertyName
        )

        //  如果正在收集依赖，则将其时序放在依赖收集结束后响应，避免嵌套收集依赖
        if (this.startCollectDependency) {
            activeWritePropertyNames.add(propertyKey)
            propertyObserverFnMap[propertyKey]?.also {
                if (it.isEmpty()) {
                    return
                }
                val fromObserverFnSet = it.toFastSet()
                addLazyTaskUtilEndCollectDependency {
                    currentChangingPropertyKey = propertyKey
                    fireObserverFn(propertyKey, fromObserverFnSet)
                    currentObservablePropertyKey = ""
                    currentChangingPropertyKey = null
                }
            }
        } else {
            currentChangingPropertyKey = propertyKey
            fireObserverFn(propertyKey, propertyObserverFnMap[propertyKey])
            currentObservablePropertyKey = ""
            currentChangingPropertyKey = null
        }
    }

    private fun fireObserverFn(propertyKey: String, fromObserverFnSet: Set<ObserverFn>?) {
        fromObserverFnSet?.also {
            it.toFastList().forEach { observerFn ->
                if (it.contains(observerFn)) {
                    observerFnCollectionPropertiesMap[observerFn]?.also {
                        if (it.contains(propertyKey)) {
                            // 重新收集依赖
                            startCollectDependency()
                            val observerFnOwner = observerFn()
                            addObserver(observerFnOwner, observerFn)
                            endCollectDependency()
                        }
                    }
                }
            }
        }

    }

    private fun buildPropertyKey(propertyOwner: PropertyOwner, propertyName: String): String {
        currentObservablePropertyKey = "${propertyOwner}_$propertyName"
        return currentObservablePropertyKey
    }

    private fun checkThread() {
        if (VERIFY_THREAD) {
            platformCheckThread {
                verifyFailedHandler(
                    IllegalStateException("observable must access on context thread")
                )
            }
        }
    }

    companion object {
        fun bindValueChange(observer: Any, valueChange: (first: Boolean) -> Unit) : Boolean {
            return PagerManager.getCurrentReactiveObserver().bindValueChange(observer, valueChange)
        }

        fun removeObserver(observerFnOwner: Any) {
            PagerManager.getCurrentReactiveObserver().removeObserver(observerFnOwner)
        }

        fun unbindValueChange(observer: Any) {
            removeObserver(observer)
        }

        fun addLazyTaskUtilEndCollectDependency(task: () -> Unit) {
            PagerManager.getCurrentReactiveObserver().addLazyTaskUtilEndCollectDependency(task)
        }

        @Deprecated("Use PagerScope.observable instead")
        internal fun <T> observable(init: T): ReadWriteProperty<Any?, T> {
            return ObservableProperties(init, object : PropertyAccessHandler {
                override fun onValueChange(
                    propertyOwner: PropertyOwner,
                    propertyName: String
                ) {
                    PagerManager.getCurrentReactiveObserver().notifyPropertyObserver(
                        propertyOwner,
                        propertyName
                    )
                }

                override fun onGetValue(propertyOwner: PropertyOwner, propertyName: String) {
                    PagerManager.getCurrentReactiveObserver().notifyGetValue(
                        propertyOwner,
                        propertyName
                    )
                }

                override fun getReactiveObserver() = PagerManager.getCurrentReactiveObserver()
            })
        }

        @Deprecated("Use PagerScope.observableList instead")
        internal fun <T> observableList(): ReadWriteProperty<Any, ObservableList<T>> {
            return ObservableCollectionProperty(ObservableList(), object : PropertyAccessHandler {
                override fun onValueChange(
                    propertyOwner: PropertyOwner,
                    propertyName: String
                ) {
                    PagerManager.getCurrentReactiveObserver().notifyPropertyObserver(
                        propertyOwner,
                        propertyName
                    )

                }

                override fun onGetValue(propertyOwner: PropertyOwner, propertyName: String) {
                    PagerManager.getCurrentReactiveObserver().notifyGetValue(
                        propertyOwner,
                        propertyName
                    )
                }

                override fun getReactiveObserver() = PagerManager.getCurrentReactiveObserver()
            })
        }

        @Deprecated("Use PagerScope.observableSet instead")
        internal fun <T> observableSet(): ReadWriteProperty<Any, ObservableSet<T>> {
            return ObservableCollectionProperty(ObservableSet(), object : PropertyAccessHandler {
                override fun onValueChange(
                    propertyOwner: PropertyOwner,
                    propertyName: String
                ) {
                    PagerManager.getCurrentReactiveObserver().notifyPropertyObserver(
                        propertyOwner,
                        propertyName
                    )

                }

                override fun onGetValue(propertyOwner: PropertyOwner, propertyName: String) {
                    PagerManager.getCurrentReactiveObserver().notifyGetValue(
                        propertyOwner,
                        propertyName
                    )
                }

                override fun getReactiveObserver() = PagerManager.getCurrentReactiveObserver()
            })
        }

        var VERIFY_THREAD = false
        var VERIFY_OBSERVER = false
        internal var verifyFailedHandler: (RuntimeException) -> Unit = { throw it }
        fun verifyFailed(handler: (RuntimeException) -> Unit) {
            verifyFailedHandler = handler
        }
    }
}

typealias Observer = Any
typealias ObserverFn = () -> Observer
typealias ObserverRemoveFn = () -> Unit

abstract class ObservableThreadSafetyMode {
    companion object {
        val NONE: ObservableThreadSafetyMode = object : ObservableThreadSafetyMode() {}
    }
}

interface ObservableProvider {
    fun <T> observable(scope: PagerScope, init: T): ReadWriteProperty<Any, T>
    fun <T> observableList(scope: PagerScope): ReadWriteProperty<Any, ObservableList<T>>
    fun <T> observableSet(scope: PagerScope): ReadWriteProperty<Any, ObservableSet<T>>
}

internal class UnsafePropertyAccessHandlerImpl(
    private val scope: PagerScope
) : PropertyAccessHandler {
    override fun onValueChange(propertyOwner: PropertyOwner, propertyName: String) {
        getReactiveObserver()?.also { observer ->
            val lastPageId = BridgeManager.currentPageId
            BridgeManager.currentPageId = scope.pagerId
            try {
                observer.notifyPropertyObserver(propertyOwner, propertyName)
            } finally {
                BridgeManager.currentPageId = lastPageId
            }
        }
    }

    override fun onGetValue(propertyOwner: PropertyOwner, propertyName: String) {
        getReactiveObserver()?.notifyGetValue(propertyOwner, propertyName)
    }

    override fun getReactiveObserver(): ReactiveObserver? {
        val pagerId = scope.pagerId.ifEmpty {
            if (ReactiveObserver.VERIFY_OBSERVER) {
                ReactiveObserver.verifyFailedHandler(
                    ReactiveObserverNotFoundException("PagerScope not initialized")
                )
            }
            // 用currentPageId兜底，以保持向前兼容
            BridgeManager.currentPageId
        }
        val observer = PagerManager.getReactiveObserver(pagerId)
        if (observer == null && ReactiveObserver.VERIFY_OBSERVER) {
            ReactiveObserver.verifyFailedHandler(
                ReactiveObserverNotFoundException("ReactiveObserver not found: $pagerId")
            )
        }
        return observer
    }
}

internal expect inline fun platformCheckThread(block: () -> Unit)