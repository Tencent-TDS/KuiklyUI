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

package com.tencent.kuikly.compose.ui

import androidx.compose.runtime.snapshots.Snapshot
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coroutines.internal.KuiklyContextScheduler
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

/**
 * Android implementation of GlobalSnapshotManager using the original Channel-based approach.
 * This maintains compatibility and proven stability on Android platform.
 */
internal actual object GlobalSnapshotManager {
    private val started = atomic(0)

    private val sent = atomic(0)

    private fun runOnKuiklyThread(block: () -> Unit) {
        if (KuiklyContextScheduler.isOnKuiklyThread("")) {
            block()
        } else {
            KuiklyContextScheduler.runOnKuiklyThread("") {
                block()
            }
        }
    }

    actual fun ensureStarted() {
        if (started.compareAndSet(0, 1)) {
            val channel = Channel<Unit>(1)
            CoroutineScope(Dispatchers.Unconfined).launch {
                channel.consumeEach {
                    runOnKuiklyThread {
                        sent.compareAndSet(1, 0)
                        Snapshot.sendApplyNotifications()
                    }
                }
            }
            Snapshot.registerGlobalWriteObserver {
                if (!ComposeContainer.enableConsumeSnapshot) {
                    return@registerGlobalWriteObserver
                }
                if (sent.compareAndSet(0, 1)) {
                    channel.trySend(Unit)
                }
            }
        }
    }
}
