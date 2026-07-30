package com.tencent.kuikly.core.collection

import com.tencent.kuikly.core.base.CrossPlatFeature

actual inline fun <E> fastArrayListOf(): MutableList<E> = if (CrossPlatFeature.isUseFastCollection) { FastArrayList<E>() } else { arrayListOf<E>() }

actual inline fun <E> fastMutableListOf(): MutableList<E> = if (CrossPlatFeature.isUseFastCollection) { FastArrayList<E>() } else { mutableListOf<E>() }

actual inline fun <K, V> fastMutableMapOf(): MutableMap<K, V> = if (CrossPlatFeature.isUseFastCollection) {  FastLinkedHashMap<K, V>() } else { mutableMapOf<K, V>() }

actual inline fun <K, V> fastHashMapOf(): MutableMap<K, V> = if (CrossPlatFeature.isUseFastCollection) {  FastHashMap<K, V>() } else { hashMapOf<K, V>() }

actual inline fun <E> fastMutableSetOf(): MutableSet<E> = if (CrossPlatFeature.isUseFastCollection) {  FastLinkedHashSet<E>() } else { mutableSetOf<E>() }

actual inline fun <E> fastHashSetOf(): MutableSet<E> = if (CrossPlatFeature.isUseFastCollection) {  FastHashSet<E>() } else { hashSetOf<E>() }

actual inline fun <K, V> fastLinkedMapOf(): MutableMap<K, V>  =  if (CrossPlatFeature.isUseFastCollection) { FastLinkedHashMap<K, V>() } else { linkedMapOf<K, V>() }

actual inline fun <E> fastLinkedHashSetOf(): MutableSet<E> = if (CrossPlatFeature.isUseFastCollection) { FastLinkedHashSet<E>() } else { linkedSetOf<E>() }

fun <E> Collection<E>.toJsFastMutableSet(): MutableSet<E> {
    if (CrossPlatFeature.isUseFastCollection) {
        if (this is FastHashSet) {
            return this.toMutableSet()
        }
        if (this is FastLinkedHashSet) {
            return this.toMutableSet()
        }
        if (this is FastArrayList) {
            return this.toMutableSet()
        }
    }
    return toMutableSet()
}

fun <E> Collection<E>.toJsFastSet(): Set<E> {
    if (CrossPlatFeature.isUseFastCollection) {
        if (this is FastHashSet) {
            return this.toSet()
        }
        if (this is FastLinkedHashSet) {
            return this.toSet()
        }
        if (this is FastArrayList) {
            return this.toSet()
        }
    }
    return toSet()
}

fun <E> Collection<E>.toJsFastMutableList(): MutableList<E> {
    if (CrossPlatFeature.isUseFastCollection) {
        if (this is FastHashSet) {
            return this.toMutableList()
        }
        if (this is FastLinkedHashSet) {
            return this.toMutableList()
        }
        if (this is FastArrayList) {
            return this.toMutableList()
        }
    }
    return toMutableList()
}

fun <E> Collection<E>.toJsFastList(): List<E> {
    if (CrossPlatFeature.isUseFastCollection) {
        if (this is FastHashSet) {
            return this.toList()
        }
        if (this is FastLinkedHashSet) {
            return this.toList()
        }
        if (this is FastArrayList) {
            return this.toList()
        }
    }
    return toList()
}


fun <K, V> MutableMap<K, V>.toJsFastMap(): Map<K, V> {
    if (CrossPlatFeature.isUseFastCollection) {
        if (this is FastHashMap) {
            return this.toNewMap()
        }
        if (this is FastLinkedHashMap) {
            return this.toNewMap()
        }
    }
    return toMap()
}

actual inline fun <E> Collection<E>.toFastMutableSet(): MutableSet<E> {
    return toJsFastMutableSet()
}

actual inline fun <E> Collection<E>.toFastSet(): Set<E> {
    return toJsFastSet()
}

actual inline fun <E> Collection<E>.toFastMutableList(): MutableList<E> {
    return toJsFastMutableList()
}

actual inline fun <E> Collection<E>.toFastList(): List<E> {
    return toJsFastList()
}

actual inline fun <K, V> MutableMap<K, V>.toFastMap(): Map<K, V> {
    return toJsFastMap()
}