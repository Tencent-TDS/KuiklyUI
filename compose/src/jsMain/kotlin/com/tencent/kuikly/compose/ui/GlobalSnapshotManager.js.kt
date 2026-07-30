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

/**
 * JavaScript/JSCore optimized implementation of GlobalSnapshotManager.
 *
 * This implementation avoids the heavy BufferedChannel overhead by using native JS mechanisms:
 * 1. MessageChannel for zero-delay async communication (fastest option in JSC)
 * 2. Fallback to Promise.resolve() if MessageChannel is not available
 *
 * Performance benefits:
 * - Eliminates Long arithmetic operations from BufferedChannel
 * - Reduces atomic operation complexity
 * - Uses native JS async primitives optimized by JSC
 */
internal actual object GlobalSnapshotManager {
    private var started = false
    private var notificationPending = false
    private var resumed = false

    // Store references to avoid repeated JS calls
    private var messagePort1: dynamic = null
    private var messagePort2: dynamic = null
    private var useMessageChannel = false

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
        resumed = true
        if (!started) {
            started = true
            setupOptimizedNotifications()
        }
    }

    private fun setupOptimizedNotifications() {
        // Try to use MessageChannel for best performance in JSC
        if (trySetupMessageChannel()) {
//            println("[GlobalSnapshotManager] Using MessageChannel for optimal performance")
        } else if (trySetupPromise()) {
//            println("[GlobalSnapshotManager] Using Promise for good performance")
        }

        // Register the global write observer
        Snapshot.registerGlobalWriteObserver {
            if (!ComposeContainer.enableConsumeSnapshot) {
                return@registerGlobalWriteObserver
            }
            scheduleNotification()
        }
    }

    private fun trySetupMessageChannel(): Boolean {
        return try {
            // Check if MessageChannel is available (should be in most JSC environments)
            val messageChannelConstructor = js("typeof MessageChannel !== 'undefined' ? MessageChannel : null")
            if (messageChannelConstructor != null) {
                val messageChannel = js("new MessageChannel()")
                messagePort1 = messageChannel.port1
                messagePort2 = messageChannel.port2

                // Set up the message handler
                messagePort2.onmessage = { _: dynamic ->
                    notificationPending = false
                    runOnKuiklyThread {
                        Snapshot.sendApplyNotifications()
                    }
                }

                useMessageChannel = true
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    private fun trySetupPromise(): Boolean {
        return try {
            // Promise.resolve() should be available in all modern JS environments
            val promiseConstructor = js("typeof Promise !== 'undefined' ? Promise : null")
            promiseConstructor != null
        } catch (e: Throwable) {
            false
        }
    }


    private fun scheduleNotification() {
        if (notificationPending) {
            return // Already scheduled
        }

        notificationPending = true

        when {
            useMessageChannel -> {
                // MessageChannel: Zero-delay, highest performance
                messagePort1.postMessage("flush")
            }

            js("typeof Promise !== 'undefined'") as Boolean -> {
                // Promise.resolve(): Very fast, good compatibility
                js("Promise.resolve()").then { _: dynamic ->
                    if (notificationPending) {
                        notificationPending = false
                        runOnKuiklyThread {
                            Snapshot.sendApplyNotifications()
                        }
                    }
                }
            }
        }
    }
}
