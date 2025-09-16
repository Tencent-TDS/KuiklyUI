package com.tencent.kuikly.core.collection

class FastLinkedHashSet<E> : MutableSet<E> {
    var hashSet: dynamic

    constructor() {
        hashSet = js("new Set()")
    }

    constructor(initialCapacity: Int) {
        hashSet = js("new Set()")
    }

    constructor(initialCapacity: Int, loadFactor: Float) {
        hashSet = js("new Set()")
    }

    constructor(elements: Collection<E>) {
        hashSet = js("new Set()")
        elements.forEach { hashSet.add(it) }
    }

    override val size: Int
        get() = hashSet.size.unsafeCast<Int>()

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun contains(element: @UnsafeVariance E): Boolean {
        return hashSet.has(element).unsafeCast<Boolean>()
    }

    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean {
        return elements.all {
            contains(it)
        }
    }

    override fun iterator(): MutableIterator<E> {
        return object : MutableIterator<E> {
            private val jsSetIterator = hashSet.values()
            private var current: E? = null

            private var currentIndex = 0

            override fun hasNext(): Boolean = currentIndex < size

            override fun next(): E {
                currentIndex += 1
                current = jsSetIterator.next().value.unsafeCast<E>()
                return current!!
            }

            override fun remove() {
                if (current == null) {
                    throw IllegalStateException("next() must be called before remove()")
                }
                hashSet.delete(current)
                current = null
            }
        }
    }

    override fun add(element: E): Boolean {
        val oldSize = size
        hashSet.add(element)
        return size != oldSize
    }

    override fun remove(element: E): Boolean {
        val oldSize = size
        hashSet.delete(element)
        return size != oldSize
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val oldSize = size
        elements.forEach {
            add(it)
        }
        return size != oldSize
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        val oldSize = size
        elements.forEach {
            remove(it)
        }
        return size != oldSize
    }

    // todo 暂未验证
    override fun retainAll(elements: Collection<E>): Boolean {
        val oldSize = size
        val toRemove = fastArrayListOf<E>()
        hashSet.forEach { it->
            val setElement = it.unsafeCast<E>()
            if (setElement !in elements) {
                toRemove.add(setElement)
            }
        }
        toRemove.forEach {
            hashSet.delete(it).unsafeCast<Unit>()
        }
        return size != oldSize
    }

    override fun clear() {
        hashSet.clear()
    }

    fun toMutableSet(): MutableSet<E> {
        val ret = js("function() { return new Set(this) }").call(hashSet)
        val newSet = FastLinkedHashSet<E>()
        newSet.hashSet = ret
        return newSet
    }

    fun toSet(): Set<E> {
        return toMutableSet()
    }

    fun toMutableList(): MutableList<E> {
        val list = FastArrayList<E>()
        list.fastList =  js("Array.from")(hashSet)
        return list
    }

    fun toList(): List<E> {
        return toMutableList()
    }

}
