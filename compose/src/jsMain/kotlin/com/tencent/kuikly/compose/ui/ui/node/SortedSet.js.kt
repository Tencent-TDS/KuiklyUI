package com.tencent.kuikly.compose.ui.node

internal actual class SortedSet<E> actual constructor(
    private val comparator: Comparator<in E>
) {
    private val list = mutableListOf<E>()

    actual fun first(): E = list.first()

    actual fun add(element: E): Boolean {
        var index = list.binarySearch(element, comparator)
        if (index < 0) {
            index = -index - 1
        } else {
            return false
        }
        list.add(index, element)
        return true
    }

    actual fun remove(element: E): Boolean {
        val index = list.binarySearch(element, comparator)
        val found = index in list.indices
        if (found) {
            list.removeAt(index)
        }
        return found
    }

    actual fun contains(element: E): Boolean {
        val index = list.binarySearch(element, comparator)
        return index in list.indices && list[index] == element
    }

    actual fun isEmpty(): Boolean = list.isEmpty()
}