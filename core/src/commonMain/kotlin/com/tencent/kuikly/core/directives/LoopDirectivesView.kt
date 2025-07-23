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

package com.tencent.kuikly.core.directives

import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.domChildren
import com.tencent.kuikly.core.base.isVirtualView
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.collection.toFastList
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.ReactiveObserver
import com.tencent.kuikly.core.reactive.collection.CollectionOperation
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.ListContentView
import com.tencent.kuikly.core.views.ListView

/**
 * 循环指令标签节点, 如vfor模板指令
 */
class LoopDirectivesView<T>(
    private val itemList: VItemList<T>,
    private val itemCreator: VItemIndexCreator<T>,
    private var didInit: Boolean = false
) :
    DirectivesView() {
    lateinit var curList: ObservableList<T>
    internal var lazySyncOperation: (LoopDirectivesView<*>.() -> Unit)? = null
    internal var didRemove = false

    override fun didInit() {
        super.didInit()
        ReactiveObserver.bindValueChange(this) {
            val list = itemList()
            if (didInit) {
                performLazySyncOperationIfNeed()
                if (list != curList) {
                    // newList to set，need remove old list allElement and add new list allElement
                    ReactiveObserver.addLazyTaskUtilEndCollectDependency {
                        val removeAllOperation = CollectionOperation(CollectionOperation.OPERATION_TYPE_REMOVE, 0, curList.count())
                        syncRemoveChildOperationToDom(removeAllOperation)
                        curList = list
                        val addAllOperation = CollectionOperation(CollectionOperation.OPERATION_TYPE_ADD, 0, curList.count())
                        syncAddChildOperationToDom(addAllOperation, itemList())
                    }
                } else {
                    val collectionOperation = list.collectionOperation.toFastList()
                    ReactiveObserver.addLazyTaskUtilEndCollectDependency {
                        if (collectionOperation.isNotEmpty()) {
                            collectionOperation.forEach { operation ->
                                syncListOperationToDom(operation)
                            }
                        }
                    }
                }

            }
        }
        didInit = true
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        didRemove = true
        lazySyncOperation = null
    }

    private fun syncListOperationToDom(operation: CollectionOperation) {
        if (realParent === null) {
            return
        }
        if (operation.isAddOperation()) {
            syncAddChildOperationToDom(operation, itemList())
        } else if (operation.isRemoveOperation()) {
            syncRemoveChildOperationToDom(operation)
        }
    }

    internal fun syncAddChildOperationToDom(operation: CollectionOperation, list: List<T>) {
        val addedChildren = fastArrayListOf<DeclarativeBaseView<*, *>>()
        var index = operation.index
        val size = list.size
        while (index < operation.index + operation.count) {
            if (!(index < list.size && index >= 0)) {
                KLog.e("KuiklyError", "sync add operation out index with index:${index} listSize:${list.size} oIndex:${operation.index} oSize:${operation.count} ")
                break
            }
            val item = list[index]
            invokeItemCreator(item, index, size, itemCreator)
            if (index != children.lastIndex) {
                val child = children.removeAt(children.lastIndex)
                children.add(index, child)
                addedChildren.add(child)
            } else {
                addedChildren.add(children.last())
            }
            index++
        }
        realParent?.also { parent ->
            val domChildren = parent.domChildren()
            addedChildren.forEach { child ->
                val insertIndex = domChildren.lastIndexOf(child)
                parent.insertDomSubView(child, insertIndex)
            }
        }
    }

    private fun syncRemoveChildOperationToDom(operation: CollectionOperation) {
        var index = operation.index
        val removedChildren = fastArrayListOf<DeclarativeBaseView<*, *>>()
        while (index < operation.index + operation.count) {
            if (!(index < children.size && index >= 0)) {
                KLog.e("KuiklyError", "sync remove operation out index with index:${index} listSize:${children.size} oIndex:${operation.index} oSize:${operation.count} ")
                break
            }
            removedChildren.add(children[index])
            index++
        }
        realParent?.also { parent ->
            removedChildren.forEach { child ->
                parent.removeDomSubView(child)
                removeChild(child)
            }
        }

    }

    internal fun performLazySyncOperationIfNeed() {
        if (!didRemove && lazySyncOperation != null) {
            lazySyncOperation?.invoke(this)
            lazySyncOperation = null
        }
    }

}

private fun <T> LoopDirectivesView<T>.invokeItemCreator(
    item: T,
    index: Int,
    size: Int,
    itemCreator: VItemIndexCreator<T>
) {
    val beforeChildrenSize = childrenSize()
    itemCreator(item, index, size)
    val currentChildrenSize = childrenSize()
    if (childrenSize() - beforeChildrenSize != 1) {
        throwRuntimeError("vfor creator闭包内必须需要且仅一个孩子节点的生成")
    }
    val child = getChild(currentChildrenSize - 1)
    if (child.isVirtualView()) {
        throwRuntimeError("vfor creator闭包内子孩子必须为非条件指令，如vif , vfor")
    }
}

fun <T> ViewContainer<*, *>.vforIndex(
    itemList: VItemList<T>,
    itemCreator: VItemIndexCreator<T>
) {
    val view = LoopDirectivesView<T>(itemList, itemCreator)
    addChild(view) {
        curList = itemList()
        if (realParent is ListContentView) { // list首屏分帧加载
            val list = curList.toFastList()
            val contentView = realParent as ListContentView
            val maxIndex = (contentView.parent as ListView<*, *>).getViewAttr().firstContentLoadMaxIndex
            if (maxIndex > 0 && list.count() > maxIndex) {
                val size = list.size
                list.subList(0, maxIndex).forEachIndexed { index, item ->
                    invokeItemCreator(item, index, size, itemCreator)
                }
                lazySyncOperation = {
                    val addAllOperation = CollectionOperation(CollectionOperation.OPERATION_TYPE_ADD, maxIndex, list.count() - maxIndex)
                    this@addChild.syncAddChildOperationToDom(addAllOperation, list)
                }
                getPager().addNextTickTask { // next tick
                    performLazySyncOperationIfNeed()
                }
                return@addChild
            }
        }
        val size = curList.size
        curList.forEachIndexed { index, item ->
            invokeItemCreator(item, index, size, itemCreator)
        }
    }
}

inline fun <T> ViewContainer<*, *>.vfor(
    noinline itemList: VItemList<T>,
    crossinline itemCreator: VItemCreator<T>
) {
    vforIndex(itemList) { item, _, _ ->
        itemCreator(item)
    }
}

typealias VItemList<T> = () -> ObservableList<T>
typealias VItemCreator<T> = LoopDirectivesView<T>.(item: T) -> Unit
typealias VItemIndexCreator<T> = LoopDirectivesView<T>.(item: T, index: Int, count: Int) -> Unit
