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

package com.tencent.kuikly.core.render.android.exception

/**
 * 异常原因
 */
enum class ErrorReason {
    UNKNOWN,
    INITIALIZE,
    CALL_KOTLIN,
    CALL_NATIVE,
    UPDATE_VIEW_TREE
}

class KuiklyRenderModuleExportException(message: String) : Exception(message)

class KuiklyRenderViewExportException(message: String) : Exception(message)

class KuiklyRenderShadowExportException(message: String) : Exception(message)

class KRNativeBizException(msg: String) : Exception(msg)

class KRAarBizException(t: Throwable) : Exception(t)

class KRKotlinBizException(message: String): Exception(message)