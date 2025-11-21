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

    override fun toString(): String {
        return innerList.toString()
    }

    /**
     * Myers diff算法
     * 通过比较新旧列表的差异，只执行必要的添加、删除和移动操作
     * @param newList 新的列表数据
     * @param areItemsTheSame 用于判断两个元素是否相同的比较函数，默认使用 == 比较
     *                        对于复杂对象，可以传入自定义比较逻辑，例如：{ old, new -> old.id == new.id }
     */
    fun diffUpdate(newList: List<T>, areItemsTheSame: ((T, T) -> Boolean)? = null) {
        if (newList.isEmpty() && innerList.isEmpty()) {
            return
        }

        if (newList.isEmpty()) {
            clear()
            return
        }

        if (innerList.isEmpty()) {
            addAll(newList)
            return
        }

        // 使用Myers diff算法计算差异
        val diffs = myersDiff(innerList, newList, areItemsTheSame)

        // 应用差异操作
        applyDiffs(diffs, newList)
    }

    /**
     * Myers diff算法实现
     * 返回编辑脚本（操作序列）
     * 算法的本质上是，在走出一步后，计算这一步可能落在的每条 k 线上的最远位置，一直到碰到终点为止。
     */
    private fun myersDiff(
        oldList: List<T>,
        newList: List<T>,
        areItemsTheSame: ((T, T) -> Boolean)?
    ): List<DiffOperation> {
        val n = oldList.size
        val m = newList.size
        val max = n + m

        // 元素比较函数：优先使用自定义比较，否则使用默认的 == 比较
        val itemsEqual: (T, T) -> Boolean = areItemsTheSame ?: { a, b -> a == b }

        // v[k]表示在对角线k上能到达的最远的x坐标
        // 初始化为-1，表示未访问过的对角线
        val v = IntArray(2 * max + 1) { -1 }

        v[1 + max] = 0

        // 存储每一步的v数组快照，用于回溯路径
        val trace = mutableListOf<IntArray>()

        // 寻找最短编辑路径
        for (d in 0..max) {
            trace.add(v.copyOf())

            for (k in -d..d step 2) {
                // 决定是向下移动还是向右移动
                var x = if (k == -d || (k != d && v[k - 1 + max] < v[k + 1 + max])) {
                    v[k + 1 + max]
                } else {
                    v[k - 1 + max] + 1
                }

                var y = x - k

                // 沿对角线前进（相同元素）
                while (x < n && y < m && itemsEqual(oldList[x], newList[y])) {
                    x++
                    y++
                }

                v[k + max] = x

                // 找到终点
                if (x >= n && y >= m) {
                    return backtrack(trace, oldList, newList, d)
                }
            }
        }

        return emptyList()
    }

    /**
     * 回溯路径，生成差异操作序列
     */
    private fun backtrack(trace: List<IntArray>, oldList: List<T>, newList: List<T>, d: Int): List<DiffOperation> {
        val operations = mutableListOf<DiffOperation>()
        var x = oldList.size
        var y = newList.size

        for (depth in d downTo 0) {
            val v = trace[depth]
            val k = x - y
            val max = (v.size - 1) / 2

            val prevK = if (k == -depth || (k != depth && v[k - 1 + max] < v[k + 1 + max])) {
                k + 1
            } else {
                k - 1
            }

            val prevX = v[prevK + max]
            val prevY = prevX - prevK

            // 沿对角线回退（相同元素）
            while (x > prevX && y > prevY) {
                x--
                y--
                operations.add(0, DiffOperation.Keep(x, y))
            }

            if (depth > 0) {
                if (x == prevX) {
                    // 插入操作：在当前 oldList 的位置 x 插入 newList[y] 的元素
                    y--
                    operations.add(0, DiffOperation.Insert(y, prevX))
                } else {
                    // 删除操作
                    x--
                    operations.add(0, DiffOperation.Delete(x))
                }
            }
        }

        return operations
    }

    /**
     * 应用差异操作到列表
     * 从后向前应用操作，避免索引偏移问题
     */
    private fun applyDiffs(diffs: List<DiffOperation>, newList: List<T>) {
        // 从后向前遍历操作序列，避免索引偏移
        for (i in diffs.size - 1 downTo 0) {
            when (val diff = diffs[i]) {
                is DiffOperation.Delete -> {
                    // 删除操作：直接删除指定索引的元素
                    removeAt(diff.oldIndex)
                }
                is DiffOperation.Insert -> {
                    // 插入操作：在指定位置插入新元素
                    add(diff.insertPosition, newList[diff.newIndex])
                }
                is DiffOperation.Keep -> {
                    // 保持不变，无需操作
                }
            }
        }
    }

    /**
     * 差异操作类型
     */
    private sealed class DiffOperation {
        data class Delete(val oldIndex: Int) : DiffOperation()
        data class Insert(val newIndex: Int, val insertPosition: Int) : DiffOperation()
        data class Keep(val oldIndex: Int, val newIndex: Int) : DiffOperation()
    }
}
