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

package com.tencent.kuikly.core.module

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 文件读写 Module，提供 App sandbox 目录的文件写入能力。
 * 主要用于 RecompositionProfiler 导出 JSON 报告供 AI 分析。
 */
class FileModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    companion object {
        const val MODULE_NAME = ModuleConst.FILE
        private const val METHOD_WRITE_FILE = "writeFile"
        private const val METHOD_APPEND_FILE = "appendFile"
        private const val METHOD_GET_FILES_DIR = "getFilesDir"

        private const val PARAM_FILENAME = "filename"
        private const val PARAM_CONTENT = "content"
        private const val KEY_PATH = "path"
        private const val KEY_ERROR = "error"
    }

    /**
     * 将内容写入 App 可写目录下的文件（异步，覆盖写）。
     *
     * @param filename 文件名（不含路径，如 "profiler_report.json"）
     * @param content 文件内容
     * @param callback 完成回调，result["path"] 为写入路径，result["error"] 为错误信息
     */
    fun writeFile(filename: String, content: String, callback: CallbackFn? = null) {
        val param = JSONObject().apply {
            put(PARAM_FILENAME, filename)
            put(PARAM_CONTENT, content)
        }
        asyncToNativeMethod(METHOD_WRITE_FILE, param, callback)
    }

    /**
     * 追加内容到 App 可写目录下的文件末尾（异步，不存在时创建）。
     * 每次追加会在末尾加换行符，适合写 JSONL 格式。
     *
     * @param filename 文件名（不含路径，如 "profiler_frames.jsonl"）
     * @param content 追加内容（通常是一行 JSON）
     * @param callback 完成回调，result["path"] 为写入路径，result["error"] 为错误信息
     */
    fun appendFile(filename: String, content: String, callback: CallbackFn? = null) {
        val param = JSONObject().apply {
            put(PARAM_FILENAME, filename)
            put(PARAM_CONTENT, content)
        }
        asyncToNativeMethod(METHOD_APPEND_FILE, param, callback)
    }

    /**
     * 获取 App 可写目录的绝对路径（异步）。
     *
     * @param callback 回调，result["path"] 为目录绝对路径
     */
    fun getFilesDir(callback: CallbackFn? = null) {
        asyncToNativeMethod(METHOD_GET_FILES_DIR, null, callback)
    }
}
