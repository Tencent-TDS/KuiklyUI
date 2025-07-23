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

package com.tencent.kuikly.core.log

import com.tencent.kuikly.core.manager.BridgeManager

/*
 * @brief 日志模块，支持宿主自定义实现具体打印接口
 */
object KLog {
    /*
     * @brief 打印info等级类型日志
     */
    fun i(tag: String, msg: String) {
        logToNative(METHOD_LOG_INFO, "[KLog][$tag]:$msg")
    }
    /*
     * @brief 打印debug等级类型日志
     */
    fun d(tag: String, msg: String) {
        logToNative(METHOD_LOG_DEBUG, "[KLog][$tag]:$msg")
    }
    /*
     * @brief 打印error等级类型日志
     */
    fun e(tag: String, msg: String) {
        logToNative(METHOD_LOG_ERROR, "[KLog][$tag]:$msg")
    }
    private fun logToNative(method: String, msg: String) {
        BridgeManager.callModuleMethod(BridgeManager.currentPageId, MODULE_NAME, method, msg, null, 1)
    }

    private const val MODULE_NAME = "KRLogModule"
    private const val METHOD_LOG_INFO = "logInfo"
    private const val METHOD_LOG_DEBUG = "logDebug"
    private const val METHOD_LOG_ERROR = "logError"
}