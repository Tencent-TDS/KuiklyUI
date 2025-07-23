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

package com.tencent.kuikly.core.render.android.expand.vendor

import android.os.Handler
import android.os.Looper
import android.util.ArrayMap
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.expand.module.KRNetworkModule
import java.io.File
import java.io.IOException

object KRFileManager {
    private val downloadingMap = ArrayMap<String, MutableList<(String?) -> Unit>>()

    private const val TAG = "KRFileManager"
    /*
     *  获取文件(优先磁盘缓存，其次网络下载)
     */
    fun fetchFile(context: IKuiklyRenderContext, cdnUrl: String, storePath: String, resultCallback: (filePath: String?) -> Unit) {
        assert(isMainThread())
        val storeFile = File(storePath)
        if (storeFile.exists()) {
            if (downloadingFile(cdnUrl)) {
                recordDownloadingFile(cdnUrl, resultCallback)
            } else {
                resultCallback(storePath)
            }
        } else {
            try {
                storeFile.createNewFile()
            } catch (e: IOException) {
                KuiklyRenderLog.e(TAG, "fetchFile: $e")
                resultCallback(null)
                return
            }
            recordDownloadingFile(cdnUrl, resultCallback)
            context.module<KRNetworkModule>(KRNetworkModule.MODULE_NAME)?.downloadFile(cdnUrl, storePath) { filePath ->
                if (filePath == null && storeFile.exists()) {
                    storeFile.delete()
                }
                Handler(Looper.getMainLooper()).post {
                    dispatchDownloadFileFinish(cdnUrl, filePath)
                }
            }
        }
    }

    private fun downloadingFile(url: String): Boolean {
        return downloadingMap[url]?.isNotEmpty() == true
    }

    private fun recordDownloadingFile(url: String, resultBlock: (String?) -> Unit) {
        val list = downloadingMap[url] ?: mutableListOf<(String?) -> Unit>().apply {
            downloadingMap[url] = this
        }
        list.add(resultBlock)
    }

    private fun dispatchDownloadFileFinish(url: String, filePath: String?) {
        downloadingMap.remove(url)?.forEach {
            it.invoke(filePath)
        }
    }
}