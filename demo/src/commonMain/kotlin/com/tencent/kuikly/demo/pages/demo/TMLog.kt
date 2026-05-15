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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.log.KLog

/**
 * TMLog - 统一日志管理类
 * 
 * 对KLog进行封装，提供统一的日志接口，方便后续日志管理和扩展。
 * 所有方法签名与KLog保持一致，确保无缝替换。
 */
object TMLog {
    // 如果是js的代码
    val isJSCode = true
    
    /**
     * 信息级别日志
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun i(tag: String, message: String) {
        KLog.i(tag, message)
        if (isJSCode){
            println("[${formatCurrentTime()}][${tag}]: ${message}")
        }
    }
    
    /**
     * 调试级别日志
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun d(tag: String, message: String) {
        KLog.d(tag, message)
        if (isJSCode) {
            println("[${formatCurrentTime()}][${tag}]: ${message}")
        }
    }
    
    /**
     * 错误级别日志
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun e(tag: String, message: String) {
        KLog.e(tag, message)
        if (isJSCode) {
            println("[${formatCurrentTime()}][${tag}]: ${message}")
        }
    }

    /**
     * 格式化当前时间戳为可读格式
     * @return 格式化的时间字符串，格式为 "HH:mm:ss.SSS"
     */
    fun formatCurrentTime(): String {
        return formatTimestamp(DateTime.currentTimestamp())
    }
    
    /**
     * 格式化时间戳为可读格式
     * @param timestamp 时间戳（毫秒）
     * @return 格式化的时间字符串，格式为 "HH:mm:ss.SSS"
     */
    fun formatTimestamp(timestamp: Long): String {
        // 获取小时、分钟、秒和毫秒
        val totalSeconds = timestamp / 1000
        val milliseconds = timestamp % 1000
        
        val hours = (totalSeconds / 3600) % 24
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

         return buildString {
             append(hours.toString().padStart(2, '0'))
             append(':')
             append(minutes.toString().padStart(2, '0'))
             append(':')
             append(seconds.toString().padStart(2, '0'))
             append('.')
             append(milliseconds.toString().padStart(3, '0'))
         }
    }
    
    /**
     * 带时间戳的信息级别日志
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun iWithTime(tag: String, message: String) {
        KLog.i(tag, "[${formatCurrentTime()}] $message")
    }
    
    /**
     * 带时间戳的调试级别日志
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun dWithTime(tag: String, message: String) {
        KLog.d(tag, "[${formatCurrentTime()}] $message")
    }
    
    /**
     * 带时间戳的错误级别日志
     * @param tag 日志标签
     * @param message 日志消息
     */
    fun eWithTime(tag: String, message: String) {
        KLog.e(tag, "[${formatCurrentTime()}] $message")
    }

}