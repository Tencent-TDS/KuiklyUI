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

package com.tencent.kuikly.core.pager

/**
 * 铰链开合状态，[rawValue] 对齐 iOS `UIHingeStatus`。
 * 无铰链设备或系统未提供更新时保持 [UNKNOWN]。
 */
enum class HingeStatus(val rawValue: Int) {
    UNKNOWN(0),
    CLOSED(1),
    PARTIALLY_OPEN(2),
    FULLY_OPEN(3);

    companion object {
        fun fromRaw(rawValue: Int): HingeStatus =
            values().firstOrNull { it.rawValue == rawValue } ?: UNKNOWN
    }
}
