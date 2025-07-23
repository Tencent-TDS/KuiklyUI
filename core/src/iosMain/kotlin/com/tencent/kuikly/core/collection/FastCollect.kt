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

package com.tencent.kuikly.core.collection

actual inline fun <E> fastArrayListOf(): MutableList<E> = arrayListOf<E>()

actual inline fun <K, V> fastHashMapOf(): MutableMap<K, V> = hashMapOf<K, V>()

actual inline fun <E> fastHashSetOf(): MutableSet<E> = hashSetOf<E>()

actual inline fun <K, V> fastLinkedMapOf(): MutableMap<K, V>  =  linkedMapOf<K, V>()

actual inline fun <E> fastLinkedHashSetOf(): MutableSet<E> = linkedSetOf<E>()

actual inline fun <E> Collection<E>.toFastMutableSet(): MutableSet<E> = toMutableSet()

actual inline fun <E> Collection<E>.toFastSet(): Set<E> = toSet()

actual inline fun <E> Collection<E>.toFastMutableList(): MutableList<E> = toMutableList()

actual inline fun <E> Collection<E>.toFastList(): List<E> = toList()

actual inline fun <K, V> MutableMap<K, V>.toFastMap(): Map<K, V> = toMap()
