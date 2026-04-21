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

internal actual object GlobalSnapshotManager {

    private enum class NotificationKind { MESSAGE_CHANNEL, PROMISE }

    private var started = false
    private var notificationPending = false
    private var kind = NotificationKind.PROMISE

    private var messagePort1: dynamic = null
    private var messagePort2: dynamic = null

    actual fun ensureStarted() {
        if (!started) {
            started = true
            setup()
        }
    }

    private fun setup() {
        kind = when {
            trySetupMessageChannel() -> NotificationKind.MESSAGE_CHANNEL
            hasPromise() -> NotificationKind.PROMISE
            else -> error("GlobalSnapshotManager requires MessageChannel or Promise support")
        }
        Snapshot.registerGlobalWriteObserver {
            if (!ComposeContainer.enableConsumeSnapshot) return@registerGlobalWriteObserver
            scheduleNotification()
        }
    }

    private fun trySetupMessageChannel(): Boolean = try {
        if (jsTypeOf(js("MessageChannel")) == "undefined") false
        else {
            val mc = js("new MessageChannel()")
            messagePort1 = mc.port1
            messagePort2 = mc.port2
            messagePort2.onmessage = flushOnce
            true
        }
    } catch (_: Throwable) { false }

    private fun hasPromise(): Boolean = try {
        jsTypeOf(js("Promise")) != "undefined"
    } catch (_: Throwable) { false }

    private fun scheduleNotification() {
        if (notificationPending) return
        notificationPending = true
        when (kind) {
            NotificationKind.MESSAGE_CHANNEL -> messagePort1.postMessage("flush")
            NotificationKind.PROMISE -> js("Promise.resolve()").then(flushOnce)
        }
    }

    private val flushOnce: (dynamic) -> Unit = { _ ->
        notificationPending = false
        Snapshot.sendApplyNotifications()
    }
}
