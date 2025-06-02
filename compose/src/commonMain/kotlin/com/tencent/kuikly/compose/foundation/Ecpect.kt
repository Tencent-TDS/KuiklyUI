// ktlint-disable filename

/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.foundation

import kotlinx.atomicfu.atomic

internal class AtomicReference<V> constructor(value: V) {
    private val delegate = atomic(value)
    fun get() = delegate.value
    fun set(value: V) {
        delegate.value = value
    }
    fun getAndSet(value: V) = delegate.getAndSet(value)
    fun compareAndSet(expect: V, newValue: V) = delegate.compareAndSet(expect, newValue)
}

internal class AtomicLong constructor(value: Long) {
    private val delegate = atomic(value)
    fun get(): Long = delegate.value
    fun set(value: Long) {
        delegate.value = value
    }
    fun getAndIncrement(): Long = delegate.getAndIncrement()
}