package com.tencent.kuikly.core.render.web.collection

fun <E> fastMutableListOf(list: dynamic = js("[]")) = FastMutableList<E>(list)

class FastMutableList<E>(list: dynamic) : MutableList<E> {

    var fastList: dynamic = list

    override val size: Int
        get() = fastList.length.unsafeCast<Int>()

    val length: Int
        get() = fastList.length.unsafeCast<Int>()

    override fun clear() {
        fastList.length = 0
    }

    override fun addAll(elements: Collection<E>): Boolean {
        if (elements is FastMutableList) {
            fastList = fastList.concat(elements.fastList)
            return elements.isNotEmpty()
        }
        fastList.push.apply(fastList, elements.toTypedArray())
        return elements.isNotEmpty()
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        fastList.splice(index, 0, elements.toTypedArray())
        return elements.isNotEmpty()
    }

    override fun add(index: Int, element: E) {
        fastList.splice(index, 0, element)
    }

    override fun add(element: E): Boolean {
        fastList[fastList.length] = element
        return true
    }

    override fun get(index: Int): E = fastList[index].unsafeCast<E>()

    override fun isEmpty(): Boolean = fastList.length === 0

    override fun iterator(): MutableIterator<E> = listIterator()

    override fun listIterator(): MutableListIterator<E> = listIterator(0)

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
                this@FastMutableList.add(currentIndex++, element)
            }

            override fun remove() {
                if (currentIndex == 0) {
                    throw IllegalArgumentException("delete index 0 is invalid")
                }
                this@FastMutableList.removeAt(--currentIndex)
            }

            override fun set(element: E) {
                if (currentIndex == 0) {
                    throw IllegalArgumentException("set index 0 is invalid")
                }
                this@FastMutableList[currentIndex - 1] = element
            }
        }
    }

    override fun removeAt(index: Int): E = fastList.splice(index, 1)[0].unsafeCast<E>()

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> =
        FastMutableList(fastList.slice(fromIndex, toIndex))

    override fun set(index: Int, element: E): E {
        val oldValue = fastList[index].unsafeCast<E>()
        fastList[index] = element
        return oldValue
    }

    override fun retainAll(elements: Collection<E>): Boolean =
        removeAll(this.filter { it !in elements })

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

    override fun lastIndexOf(element: E): Int = fastList.lastIndexOf(element).unsafeCast<Int>()

    override fun indexOf(element: E): Int = fastList.indexOf(element).unsafeCast<Int>()

    override fun containsAll(elements: Collection<E>): Boolean = elements.all { contains(it) }

    override fun contains(element: E): Boolean = indexOf(element) != -1
}
