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
package com.tencent.kuikly.compose.coroutines.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

internal class ComposeDispatcher(
    private val pagerId: String,
) : CoroutineDispatcher() {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        KuiklyContextScheduler.runOnKuiklyThread(pagerId) { cancel ->
            if (cancel) {
                context.cancel(CancellationException("The task was rejected, Pager($pagerId) is closed."))
            }
            block.run()
        }
    }

    override fun toString(): String = "ComposeDispatcher($pagerId)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ComposeDispatcher && pagerId == other.pagerId
    }

    override fun hashCode(): Int = pagerId.hashCode()

}