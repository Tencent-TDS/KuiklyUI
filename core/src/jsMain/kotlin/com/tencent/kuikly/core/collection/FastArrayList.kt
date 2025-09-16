package com.tencent.kuikly.core.collection

class FastArrayList<E> : MutableList<E> {

    var fastList: dynamic

    constructor() {
        fastList = js("[]")
    }

    constructor(initialCapacity: Int) {
        fastList = js("[]")
    }
    constructor(elements: Collection<E>) {
        fastList = js("[]")
        elements.forEach {
            add(it)
        }
    }

    fun toList(): List<E> {
        val newList = FastArrayList<E>()
        newList.fastList = fastList.slice()
        return newList
    }

    fun toMutableList(): MutableList<E> {
        val newList = FastArrayList<E>()
        newList.fastList = fastList.slice()
        return newList
    }

    fun toMutableSet(): MutableSet<E> {
        val newSet = FastHashSet<E>()
        val hashSet = js("function() { return new Set(this) }").call(fastList)
        newSet.hashSet = hashSet
        return newSet
    }

    fun toSet(): Set<E> {
        return toMutableSet()
    }

    override val size: Int
        get() = fastList.length.unsafeCast<Int>()

    override fun clear() {
        fastList.length = 0
    }

    override fun addAll(elements: Collection<E>): Boolean {
        if (elements.isEmpty()) return false
        if (elements is FastArrayList) {
            fastList.push.apply(fastList, elements.fastList)
        } else {
            val array = elements.toTypedArray()
            fastList.push.apply(fastList, array)
        }
        return true
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        if (elements.isEmpty()) return false
        val args = js("[]")
        args.push(index, 0)
        elements.forEach { args.push(it) }
        fastList.splice.apply(fastList, args)
        return true
    }

    override fun add(index: Int, element: E) {
        fastList.splice(index, 0, element)
    }

    override fun add(element: E): Boolean {
        fastList[fastList.length.unsafeCast<Int>()] = (element)
        return true
    }

    override fun get(index: Int): E {
        return fastList[index].unsafeCast<E>()
    }

    override fun isEmpty(): Boolean {
        return fastList.length === 0
    }

    override fun iterator(): MutableIterator<E> {
        return listIterator()
    }

    override fun listIterator(): MutableListIterator<E> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        return object : MutableListIterator<E> {
            private var currentIndex = index

            override fun hasNext(): Boolean = currentIndex < size

            override fun hasPrevious(): Boolean = currentIndex > 0

            override fun next(): E {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                return fastList[currentIndex++].unsafeCast<E>()
            }

            override fun nextIndex(): Int = currentIndex

            override fun previous(): E {
                if (!hasPrevious()) {
                    throw NoSuchElementException()
                }
                return fastList[--currentIndex].unsafeCast<E>()
            }

            override fun previousIndex(): Int = currentIndex - 1

            override fun add(element: E) {
                this@FastArrayList.add(currentIndex++, element)
            }

            override fun remove() {
                if (currentIndex == 0) {
                    throw IllegalStateException()
                }
                this@FastArrayList.removeAt(--currentIndex)
            }

            override fun set(element: E) {
                if (currentIndex == 0) {
                    throw IllegalStateException()
                }
                this@FastArrayList[currentIndex - 1] = element
            }
        }
    }

    override fun removeAt(index: Int): E {
        return fastList.splice(index, 1)[0].unsafeCast<E>()
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        val list = FastArrayList<E>()
        list.fastList = fastList.slice(fromIndex, toIndex)
        return list
    }

    override fun set(index: Int, element: E): E {
        val oldValue = fastList[index].unsafeCast<E>()
        fastList[index] = element
        return oldValue
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return removeAll(this.filter { it !in elements })
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return elements.fold(false) { acc, e ->
            acc or remove(e)
        }
    }

    override fun remove(element: E): Boolean {
        val index = indexOf(element)
        if (index != -1) {
            fastList.splice(index, 1)
            return true
        }
        return false
    }

    override fun lastIndexOf(element: E): Int {
        return fastList.lastIndexOf(element).unsafeCast<Int>()
    }

    override fun indexOf(element: E): Int {
        return fastList.indexOf(element).unsafeCast<Int>()
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return elements.all { contains(it) }
    }

    override fun contains(element: E): Boolean {
        return indexOf(element) != -1
    }

}
