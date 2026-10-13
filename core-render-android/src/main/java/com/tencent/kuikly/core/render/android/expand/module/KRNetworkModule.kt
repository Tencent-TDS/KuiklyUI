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
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by kam on 2023/4/19.
 */
class KRNetworkModule : KuiklyRenderBaseModule() {

    /**
     * Lifecycle of one stream request. [finished] and [connection] are only changed under
     * [streamRequestLock], so close() and the worker agree on whether a connection may still start.
     */
    private class StreamRequest(val requestId: String) {
        /**
         * Set by close/onDestroy or once a terminal event has been claimed; never cleared.
         * Only flipped through [markStreamFinished], which also returns the request's stream slot.
         */
        @Volatile
        var finished = false
        var connection: HttpURLConnection? = null
    }

    private val streamRequests = ConcurrentHashMap<String, StreamRequest>()
    private val streamRequestLock = Any()

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_HTTP_REQUEST -> KuiklyRenderAdapterManager.krThreadAdapter?.executeOnSubThread {
                httpRequest(params, null, callback)
            }
            METHOD_HTTP_STREAM_REQUEST -> {
                val requestId = params.toJSONObjectSafely().optString("requestId")
                val request = StreamRequest(requestId)
                val rejectReason = synchronized(streamRequestLock) {
                    when {
                        requestId.isEmpty() -> "requestId is empty"
                        streamRequests.containsKey(requestId) -> "duplicate requestId"
                        !streamSlots.tryAcquire() -> TOO_MANY_STREAMS_MESSAGE
                        else -> {
                            streamRequests[requestId] = request
                            null
                        }
                    }
                }
                if (rejectReason != null) {
                    callback?.invoke(streamErrorEvent(rejectReason))
                    return null
                }
                // Streams block a worker for their whole lifetime, so they must not share the
                // krThreadAdapter pool with ordinary requests.
                try {
                    streamExecutor.execute {
                        httpStreamRequest(request, params, callback)
                    }
                } catch (e: RejectedExecutionException) {
                    markStreamFinished(request)
                    callback?.invoke(streamErrorEvent(TOO_MANY_STREAMS_MESSAGE))
                }
                null
            }
            METHOD_CLOSE_STREAM_REQUEST -> {
                // Do not enqueue close behind long-lived stream reads. A small shared pool can
                // otherwise be fully occupied by streams, preventing their own close tasks.
                closeStreamRequest(params)
                null
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

    private fun httpStreamRequest(request: StreamRequest, params: String?, callback: KuiklyRenderCallback?) {
        val paramsJSON = params.toJSONObjectSafely()
        val url = paramsJSON.optString("url")
        val method = paramsJSON.optString("method")
        val param = paramsJSON.optJSONObject("param")
        val header = paramsJSON.optJSONObject("headers")
        val cookie = paramsJSON.optString("cookie")
        val timeoutS = paramsJSON.optInt("timeout", DEFAULT_TIMEOUT_S)

        if (request.finished) return

        var reader: InputStreamReader? = null
        var errorStream: InputStream? = null
        var connection: HttpURLConnection? = null
        try {
            val requestUrl = if (method == HTTP_METHOD_GET) createRequestUrl(url, param, nullAsEmpty = true) else url
            val conn = URL(requestUrl).openConnection() as HttpURLConnection
            connection = conn
            // readTimeout bounds each blocking read, so this is an idle timeout rather than a
            // limit on the total stream duration.
            val timeoutMs = if (timeoutS > 0) timeoutS * 1000 else DEFAULT_TIMEOUT_S * 1000
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.useCaches = false
            conn.doInput = true
            setRequestMethod(conn, method)
            addHeaderToConnection(conn, header, cookie)
            val body = prepareStreamBody(conn, method, header, param)

            if (!attachStreamConnection(request, conn)) return
            // disconnect() can abort an in-flight socket connect, but not DNS resolution or a
            // close() that lands before connect() has created its engine. Such a worker may stay
            // blocked until connectTimeout; close() has already released its stream slot, and the
            // flag check below keeps it from sending the request.
            conn.connect()
            if (request.finished) return
            if (body != null) {
                DataOutputStream(conn.outputStream).use { out -> out.write(body) }
            }
            if (request.finished) return

            val responseCode = conn.responseCode
            if (request.finished) return
            val headers: String = try {
                if (conn.headerFields != null) conn.headerFields.toJSONObject().toString() else ""
            } catch (e: Throwable) {
                KuiklyRenderLog.e(MODULE_NAME, "stream headerFields to json exception: $e")
                ""
            }
            if (responseCode >= 200 && responseCode <= 299) {
                val rawStream = conn.inputStream
                reader = InputStreamReader(rawStream, Charsets.UTF_8)
                var isFirstCallback = true
                val buffer = CharArray(8192)
                var charsRead: Int
                while (reader.read(buffer).also { charsRead = it } != -1) {
                    if (request.finished) {
                        break
                    }
                    val chunk = String(buffer, 0, charsRead)
                    val eventData = mapOf(
                        KEY_STREAM_EVENT to STREAM_EVENT_DATA,
                        KEY_STREAM_DATA to chunk
                    ).let {
                        if (isFirstCallback) {
                            isFirstCallback = false
                            it + mapOf(
                                KEY_HEADERS to headers,
                                KEY_STATUS_CODE to responseCode
                            )
                        } else {
                            it
                        }
                    }
                    callback?.invoke(eventData)
                }
                if (claimStreamTerminal(request)) {
                    callback?.invoke(
                        mapOf(
                            KEY_STREAM_EVENT to STREAM_EVENT_COMPLETE,
                            KEY_STREAM_DATA to ""
                        )
                    )
                }
            } else {
                errorStream = conn.errorStream
                val errorMsg = readStreamErrorBody(errorStream).ifEmpty { "HTTP error $responseCode" }
                if (claimStreamTerminal(request)) {
                    callback?.invoke(
                        mapOf(
                            KEY_STREAM_EVENT to STREAM_EVENT_ERROR,
                            KEY_STREAM_DATA to errorMsg,
                            KEY_HEADERS to headers,
                            KEY_STATUS_CODE to responseCode
                        )
                    )
                }
            }
        } catch (e: Exception) {
            KuiklyRenderLog.e(MODULE_NAME, "Stream request error: $e")
            // 主动关闭导致的异常（请求已被标记为 finished）不回调 error
            if (claimStreamTerminal(request)) {
                val isTimeout = e is SocketTimeoutException
                callback?.invoke(
                    streamErrorEvent(
                        if (isTimeout) "request timeout" else (e.message ?: "io exception"),
                        if (isTimeout) STATE_CODE_TIMEOUT else STATE_CODE_UNKNOWN
                    )
                )
            }
        } finally {
            markStreamFinished(request)
            try {
                reader?.close()
                errorStream?.close()
                connection?.disconnect()
            } catch (e: IOException) {
                KuiklyRenderLog.e(MODULE_NAME, "Stream request close error: $e")
            }
        }
    }

    /**
     * Reads at most [MAX_STREAM_ERROR_BODY_BYTES] of an HTTP error body. A read failure only
     * truncates the body, so the caller still reports the status code and headers it already has.
     */
    private fun readStreamErrorBody(inputStream: InputStream?): String {
        inputStream ?: return ""
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        try {
            while (output.size() < MAX_STREAM_ERROR_BODY_BYTES) {
                val toRead = minOf(buffer.size, MAX_STREAM_ERROR_BODY_BYTES - output.size())
                val len = inputStream.read(buffer, 0, toRead)
                if (len == -1) break
                output.write(buffer, 0, len)
            }
        } catch (e: IOException) {
            KuiklyRenderLog.e(MODULE_NAME, "Stream error body read error: $e")
        }
        return String(output.toByteArray(), Charsets.UTF_8)
    }

    private fun streamErrorEvent(message: String, statusCode: Int = STATE_CODE_UNKNOWN): Map<String, Any> {
        return mapOf(
            KEY_STREAM_EVENT to STREAM_EVENT_ERROR,
            KEY_STREAM_DATA to message,
            KEY_STATUS_CODE to statusCode
        )
    }

    /** Returns false if the request was closed before the connection could be published. */
    private fun attachStreamConnection(request: StreamRequest, connection: HttpURLConnection): Boolean {
        synchronized(streamRequestLock) {
            if (request.finished) return false
            request.connection = connection
            return true
        }
    }

    /** Claims the single terminal event; false if the request was closed or already terminated. */
    private fun claimStreamTerminal(request: StreamRequest): Boolean = markStreamFinished(request)

    /**
     * Moves the request to its final state exactly once: unregisters it and returns its stream
     * slot, even if the worker thread is still blocked. Returns false if it was already finished.
     */
    private fun markStreamFinished(request: StreamRequest): Boolean {
        synchronized(streamRequestLock) {
            if (request.finished) return false
            request.finished = true
            streamRequests.remove(request.requestId, request)
            streamSlots.release()
            return true
        }
    }

    private fun cancelStreamRequest(request: StreamRequest) {
        val connection = synchronized(streamRequestLock) {
            markStreamFinished(request)
            request.connection
        }
        try {
            connection?.disconnect()
        } catch (e: Exception) {
            KuiklyRenderLog.e(MODULE_NAME, "Close stream request error: $e")
        }
    }

    private fun closeStreamRequest(params: String?) {
        val requestId = params.toJSONObjectSafely().optString("requestId")
        val request = synchronized(streamRequestLock) { streamRequests.remove(requestId) } ?: return
        cancelStreamRequest(request)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnect directly for the same reason as closeStreamRequest: teardown must not wait
        // for a pool whose workers may all be blocked reading these streams.
        val requests = synchronized(streamRequestLock) {
            streamRequests.values.toList().also { streamRequests.clear() }
        }
        requests.forEach { cancelStreamRequest(it) }
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
            setRequestMethod(connection, method)
            addHeaderToConnection(connection, header, cookie)
            addBodyParamsIfNeed(connection, method, header, param, bytes)

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

    /**
     * Stream requests use the cross-platform body contract: JSON when Content-Type contains
     * "json", otherwise UTF-8 application/x-www-form-urlencoded. Configures the connection
     * (must happen before connect()) and returns the body to write afterwards, or null.
     */
    private fun prepareStreamBody(
        connection: URLConnection,
        method: String,
        header: JSONObject?,
        param: JSONObject?
    ): ByteArray? {
        if (method != HTTP_METHOD_POST || param == null) {
            return null
        }
        val contentType = findHeaderIgnoreCase(header, HEADER_CONTENT_TYPE)
        val body = if (contentType?.contains("json", ignoreCase = true) == true) {
            param.toString()
        } else {
            if (contentType == null) {
                connection.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_FORM)
            }
            buildFormUrlEncodedBody(param)
        }
        connection.doOutput = true
        return body.toByteArray(Charsets.UTF_8)
    }

    private fun buildFormUrlEncodedBody(param: JSONObject): String {
        val sb = StringBuilder()
        for (key in param.keys()) {
            if (sb.isNotEmpty()) {
                sb.append('&')
            }
            sb.append(URLEncoder.encode(key, "UTF-8"))
                .append('=')
                .append(URLEncoder.encode(streamParamValue(param.opt(key)), "UTF-8"))
        }
        return sb.toString()
    }

    /**
     * Stream form / query value rule shared with iOS and HarmonyOS: JSON null becomes an empty
     * value (`key=`), booleans become true/false, objects and arrays become JSON text.
     */
    private fun streamParamValue(value: Any?): String {
        return if (value == null || value == JSONObject.NULL) "" else value.toString()
    }

    private fun findHeaderIgnoreCase(header: JSONObject?, name: String): String? {
        header ?: return null
        for (key in header.keys()) {
            if (key.equals(name, ignoreCase = true)) {
                return header.opt(key)?.toString()
            }
        }
        return null
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

    /**
     * Appends [param] to the query of [url] (before any fragment). Keys and values are UTF-8
     * percent-encoded. [nullAsEmpty] applies the stream value rule ([streamParamValue]);
     * ordinary requests keep sending JSON null as "null".
     */
    private fun createRequestUrl(url: String, param: JSONObject?, nullAsEmpty: Boolean = false): String {
        if (param == null || param.length() == 0) {
            return url
        }

        val query = StringBuilder()
        for (key in param.keys()) {
            val rawValue = param.opt(key)
            val value = if (nullAsEmpty) streamParamValue(rawValue) else rawValue?.toString() ?: ""
            if (query.isNotEmpty()) {
                query.append('&')
            }
            query.append(encodeQueryComponent(key)).append('=').append(encodeQueryComponent(value))
        }

        val fragmentIndex = url.indexOf('#')
        val base = if (fragmentIndex >= 0) url.substring(0, fragmentIndex) else url
        val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""
        val separator = when {
            !base.contains('?') -> "?"
            base.endsWith('?') || base.endsWith('&') -> ""
            else -> "&"
        }
        return base + separator + query + fragment
    }

    private fun encodeQueryComponent(value: String): String {
        // URLEncoder produces form encoding; a literal '+' is already %2B, so only spaces map here.
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
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
        private const val METHOD_HTTP_STREAM_REQUEST = "httpStreamRequest"
        private const val METHOD_CLOSE_STREAM_REQUEST = "closeStreamRequest"
        private const val HTTP_METHOD_GET = "GET"
        private const val HTTP_METHOD_POST = "POST"
        private const val KEY_SUCCESS = "success"
        private const val KEY_ERROR_MSG = "errorMsg"
        private const val KEY_HEADERS = "headers"
        private const val KEY_STATUS_CODE = "statusCode"
        private const val KEY_STREAM_EVENT = "event"
        private const val KEY_STREAM_DATA = "data"
        private const val STREAM_EVENT_DATA = "data"
        private const val STREAM_EVENT_COMPLETE = "complete"
        private const val STREAM_EVENT_ERROR = "error"

        private const val HEADER_CONTENT_TYPE = "Content-Type"
        private const val CONTENT_TYPE_FORM = "application/x-www-form-urlencoded; charset=UTF-8"

        private const val STATE_CODE_UNKNOWN = -1000
        private const val STATE_CODE_TIMEOUT = -1001
        private const val DEFAULT_TIMEOUT_S = 30
        private const val MAX_CONCURRENT_STREAMS = 8
        private const val MAX_STREAM_ERROR_BODY_BYTES = 64 * 1024
        /** Room for workers of closed requests that are still blocked in DNS / connect. */
        private const val MAX_STREAM_THREADS = MAX_CONCURRENT_STREAMS * 4
        private const val TOO_MANY_STREAMS_MESSAGE =
            "too many concurrent stream requests (max $MAX_CONCURRENT_STREAMS)"

        private val streamThreadIndex = AtomicInteger()

        /** Process-wide limit of open (not yet finished or closed) stream requests. */
        private val streamSlots = Semaphore(MAX_CONCURRENT_STREAMS)

        /**
         * Process-wide pool for long-lived stream reads. SynchronousQueue makes submissions fail
         * fast instead of queueing without any timeout.
         */
        private val streamExecutor: ThreadPoolExecutor by lazy {
            ThreadPoolExecutor(
                0,
                MAX_STREAM_THREADS,
                30L,
                TimeUnit.SECONDS,
                SynchronousQueue(),
                ThreadFactory { runnable ->
                    Thread(runnable, "KRNetworkStream-${streamThreadIndex.incrementAndGet()}").apply {
                        isDaemon = true
                    }
                }
            )
        }
    }
}
