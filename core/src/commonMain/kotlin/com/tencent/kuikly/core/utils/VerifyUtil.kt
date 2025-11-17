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
package com.tencent.kuikly.core.utils

internal var VERIFY_THREAD = false
internal var VERIFY_THREAD_LEGACY = false
internal var VERIFY_REACTIVE_OBSERVER = false
internal var verifyFailedHandler: (RuntimeException) -> Unit = { throw it }

internal fun checkThread(property: String, action: String) {
    if (VERIFY_THREAD) {
        platformCheckThread {
            verifyFailedHandler(
                IllegalStateException("$property must $action on context thread")
            )
        }
    }
}

internal fun checkThreadLegacy() {
    if (VERIFY_THREAD_LEGACY || VERIFY_THREAD) {
        platformCheckThread {
            verifyFailedHandler(
                IllegalStateException("observable must access on context thread")
            )
        }
    }
}

internal expect inline fun platformCheckThread(failure: () -> Unit)