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

package com.tencent.kuikly.core.render.android.expand.module

import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.css.ktx.toJSONObject
import com.tencent.kuikly.core.render.android.css.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder

/**
 * Created by kam on 2023/4/19.
 */
class KRNetworkModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_HTTP_REQUEST -> KuiklyRenderAdapterManager.krThreadAdapter?.executeOnSubThread {
                httpRequest(params, null, callback)
            }
            else -> super.call(method, params, callback)
        }
    }

    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_HTTP_REQUEST_BINARY -> {
                val (jsonStr, bytes) = parseBinaryArgs(params) ?: return null
                KuiklyRenderAdapterManager.krThreadAdapter?.executeOnSubThread {
                    httpRequest(jsonStr, bytes, callback)
                }
            }
            else -> super.call(method, params, callback)
        }
    }

    fun downloadFile(url: String, storePath: String, timeoutS: Int = 30, resultCallback: (filePath: String?) -> Unit) {
        KuiklyRenderAdapterManager.krThreadAdapter?.executeOnSubThread {
            var bufferInputStream: BufferedInputStream? = null
            var fileOutputStream: FileOutputStream? = null
            var errorStream: InputStream? = null

            var connection: HttpURLConnection? = null
            try {
                connection = openConnection(url, HTTP_METHOD_GET, null) as HttpURLConnection
                connection.connectTimeout = timeoutS * 1000
                connection.readTimeout = timeoutS * 1000
                connection.useCaches = false
                connection.doInput = true
                setRequestMethod(connection, HTTP_METHOD_GET)

                val responseCode = connection.responseCode
                if (responseCode >= 200 && responseCode <= 299) {
                    val bis = BufferedInputStream(connection.inputStream)
                    bufferInputStream = bis
                    val fos = FileOutputStream(File(storePath))
                    fileOutputStream = fos
                    val data = ByteArray(2048)
                    var len: Int
                    while (bis.read(data).also { len = it } != -1) {
                        fos.write(data, 0, len)
                    }
                    fos.flush()
                    resultCallback.invoke(storePath)
                } else {
                    resultCallback.invoke(null)
                    errorStream = connection.errorStream
                    val errorMsg = readInputStreamAsString(errorStream)
                    KuiklyRenderLog.e(MODULE_NAME, "download file error: $errorMsg")
                }
            } catch (e: Exception) {
                KuiklyRenderLog.e(MODULE_NAME, "Network module error: $e")
            } finally {
                try {
                    bufferInputStream?.close()
                    fileOutputStream?.close()
                    errorStream?.close()
                    connection?.disconnect()
                } catch (e: IOException) {
                    KuiklyRenderLog.e(MODULE_NAME, "Network module close error: $e")
                }
            }
        }
    }

    private fun httpRequest(params: String?, bytes: ByteArray?, callback: KuiklyRenderCallback?) {
        val binaryMode = bytes != null
        val paramsJSON = params.toJSONObjectSafely()
        val url = paramsJSON.optString("url")
        val method = paramsJSON.optString("method")
        val param = paramsJSON.optJSONObject("param")
        val header = paramsJSON.optJSONObject("headers")
        val cookie = paramsJSON.optString("cookie")
        val timeoutS = paramsJSON.optInt("timeout")

        var rawStream: InputStream? = null
        var errorStream: InputStream? = null
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(url, method, param) as HttpURLConnection
            connection.connectTimeout = timeoutS * 1000
            connection.readTimeout = timeoutS * 1000
            connection.useCaches = false
            connection.doInput = true
            addHeaderToConnection(connection, header, cookie)
            addBodyParamsIfNeed(connection, method, header, param, bytes)
            setRequestMethod(connection, method)

            val responseCode = connection.responseCode
            if (responseCode >= 200 && responseCode <= 299) {
                rawStream = connection.inputStream
                val headers: String = try {
                    if (connection.headerFields != null) connection.headerFields.toJSONObject().toString() else ""
                } catch (e : Throwable) {
                    KuiklyRenderLog.e(MODULE_NAME, "headerFields to json occur exception: $e")
                    ""
                }
                val data = if (binaryMode) {
                    readInputStreamAsBytes(rawStream)
                } else {
                    readInputStreamAsString(rawStream)
                }
                fireSuccessCallback(binaryMode, callback, data, headers, responseCode)
            } else {
                errorStream = connection.errorStream
                val errorMsg = readInputStreamAsString(errorStream)
                fireErrorCallback(binaryMode, callback, errorMsg, responseCode)
            }
        } catch (e: Exception) {
            KuiklyRenderLog.e(MODULE_NAME, "Network module error: $e")
            fireErrorCallback(binaryMode, callback, "io exception", STATE_CODE_UNKNOWN)
        } finally {
            try {
                rawStream?.close()
                errorStream?.close()
                connection?.disconnect()
            } catch (e: IOException) {
                KuiklyRenderLog.e(MODULE_NAME, "Network module close error: $e")
            }
        }
    }

    private fun fireErrorCallback(
        binaryMode: Boolean,
        callback: KuiklyRenderCallback?,
        errorMsg: String,
        statusCode: Int
    ) {
        if (binaryMode) {
            callback?.invoke(
                arrayOf(
                    JSONObject().apply {
                        put(KEY_SUCCESS, 0)
                        put(KEY_ERROR_MSG, errorMsg)
                        put(KEY_STATUS_CODE, statusCode)
                    }.toString(),
                    ByteArray(0) // 二进制模式下返回空字节数组
                )
            )
        } else {
            callback?.invoke(
                mapOf(
                    KEY_SUCCESS to 0,
                    KEY_ERROR_MSG to errorMsg,
                    KEY_STATUS_CODE to statusCode
                )
            )
        }
    }

    private fun fireSuccessCallback(
        binaryMode: Boolean,
        callback: KuiklyRenderCallback?,
        resultData: Any,
        headers: String,
        statusCode: Int
    ) {
        if (binaryMode) {
            // 如果是二进制数据，直接返回字节数组
            callback?.invoke(
                arrayOf(
                    JSONObject().apply {
                        put(KEY_SUCCESS, 1)
                        put(KEY_ERROR_MSG, "")
                        put(KEY_HEADERS, headers)
                        put(KEY_STATUS_CODE, statusCode)
                    }.toString(),
                    resultData as ByteArray
                )
            )
        } else {
            callback?.invoke(
                mapOf(
                    "data" to resultData,
                    KEY_SUCCESS to 1,
                    KEY_ERROR_MSG to "",
                    KEY_HEADERS to headers,
                    KEY_STATUS_CODE to statusCode
                )
            )
        }
    }

    private fun openConnection(url: String, method: String, param: JSONObject?): URLConnection {
        val requestUrl = URL(if (method == HTTP_METHOD_GET) {
            createRequestUrl(url, param)
        } else {
            url
        })
        return requestUrl.openConnection()
    }

    private fun setRequestMethod(connection: HttpURLConnection, method: String) {
        connection.requestMethod = method
    }

    private fun addBodyParamsIfNeed(
        connection: URLConnection,
        method: String,
        header: JSONObject?,
        param: JSONObject?,
        bytes: ByteArray?
    ) {
        if (method != HTTP_METHOD_POST) {
            return
        }
        if (param == null && (bytes == null || bytes.isEmpty())) {
            return
        }
        connection.doOutput = true
        val outputBytes = when {
            bytes != null && bytes.isNotEmpty() -> bytes
            isContentTypeJson(header) -> param.toString().toByteArray()
            else -> buildBodyParamStr(param).toByteArray()
        }
        DataOutputStream(connection.getOutputStream()).use { out ->
            out.write(outputBytes)
        }
    }

    private fun addHeaderToConnection(
        connection: URLConnection,
        header: JSONObject?,
        cookie: String?
    ) {
        cookie?.also {
            connection.addRequestProperty("Cookie", it)
        }
        header?.also {
            val keys = it.keys()
            for (key in keys) {
                connection.addRequestProperty(key, it.opt(key)?.toString() ?: "")
            }
        }
    }

    private fun createRequestUrl(url: String, param: JSONObject?): String {
        if (param == null) {
            return url
        }

        val keys = param.keys()
        var paramsStringBuilder: java.lang.StringBuilder? = null
        for (key in keys) {
            var value = URLEncoder.encode(param.opt(key)?.toString())
            if (value.isNotEmpty()) {
                value = value.replace("+", "%20") // 安卓中，空格会被转码成"+"号，实际应该按HTTP标准转码为"%20"
            }
            if (paramsStringBuilder == null) {
                paramsStringBuilder = java.lang.StringBuilder()
                if (!url.contains("?")) {
                    paramsStringBuilder.append("?")
                } else {
                    paramsStringBuilder.append("&")
                }
            } else {
                paramsStringBuilder.append("&")
            }
            paramsStringBuilder.append(key).append("=").append(value)
        }
        return if (paramsStringBuilder?.isNotEmpty() == true) {
            url + paramsStringBuilder.toString()
        } else {
            url
        }
    }

    private fun buildBodyParamStr(param: JSONObject?): String {
        if (param == null) {
            return ""
        }

        var sb: java.lang.StringBuilder? = null
        val keys = param.keys()
        for (key in keys) {
            val v = param.opt(key)?.toString() ?: ""
            if (sb == null) {
                sb = StringBuilder()
            } else {
                sb.append("&")
            }
            sb.append(key).append("=").append(v)
        }
        return sb.toString()
    }

    private fun isContentTypeJson(header: JSONObject?): Boolean {
        if (header == null) {
            return false
        }

        val contentTypeKey = "application/json"
        return header.optString("Content-Type").contains(contentTypeKey)
                || header.optString("content-Type").contains(contentTypeKey)
                || header.optString("Content-type").contains(contentTypeKey)
                || header.optString("content-type").contains(contentTypeKey)
    }

    @Throws(IOException::class)
    private fun readInputStreamAsString(
        inputStream: InputStream?
    ): String {
        if (inputStream == null) {
            return "{}"
        }
        return inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
    }

    @Throws(IOException::class)
    private fun readInputStreamAsBytes(
        inputStream: InputStream?
    ): ByteArray {
        if (inputStream == null) {
            return ByteArray(0)
        }
        return inputStream.use(InputStream::readBytes)
    }

    private fun parseBinaryArgs(params: Any?): Pair<String, ByteArray>? {
        if (params is Array<*> && params.size >= 2) {
            val jsonStr = params[0] as? String ?: return null
            val data = params[1] as? ByteArray ?: return null
            return Pair(jsonStr, data)
        }
        return null
    }

    companion object {
        const val MODULE_NAME = "KRNetworkModule"
        private const val METHOD_HTTP_REQUEST = "httpRequest"
        private const val METHOD_HTTP_REQUEST_BINARY = "httpRequestBinary"
        private const val HTTP_METHOD_GET = "GET"
        private const val HTTP_METHOD_POST = "POST"
        private const val KEY_SUCCESS = "success"
        private const val KEY_ERROR_MSG = "errorMsg"
        private const val KEY_HEADERS = "headers"
        private const val KEY_STATUS_CODE = "statusCode"

        private const val STATE_CODE_UNKNOWN = -1000

    }
}