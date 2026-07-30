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

package com.tencent.kuikly.compose.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

class SlotProvider {
    private var nextSlotId = 0
    private val _slots = mutableStateListOf<Pair<Int, (@Composable () -> Unit)?>>()
    val slots: List<Pair<Int, (@Composable () -> Unit)?>> get() = _slots

    fun addSlot(content: @Composable () -> Unit): Int {
        val id = nextSlotId++
        _slots.add(id to content)
        return id
    }

    fun removeSlot(slotId: Int) {
        _slots.removeAll { it.first == slotId }
    }
}

val LocalSlotProvider = compositionLocalOf { SlotProvider() }

@Composable
fun ProvideSlotProvider(content: @Composable () -> Unit) {
    val slotProvider = remember { SlotProvider() }
    CompositionLocalProvider(LocalSlotProvider provides slotProvider, content = content)
}