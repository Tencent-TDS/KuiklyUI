package com.tencent.kuikly.core.render.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.utils.Log
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.fetch.CORS
import org.w3c.fetch.Headers
import org.w3c.fetch.INCLUDE
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.RequestMode
import org.w3c.fetch.Response
import kotlin.js.Promise
import kotlin.js.json

/** Kuikly network request module */
class KRNetworkModule : KuiklyRenderBaseModule() {
    private val activeStreamRequests = mutableMapOf<String, dynamic>()

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_HTTP_REQUEST -> httpRequest(params, callback)
            METHOD_HTTP_STREAM_REQUEST -> httpStreamRequest(params, callback)
            METHOD_CLOSE_STREAM_REQUEST -> closeStreamRequest(params)
            else -> super.call(method, params, callback)
        }
    }

    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_HTTP_REQUEST_BINARY -> httpRequestBinary(params, callback)
            else -> super.call(method, params, callback)
        }
    }

    override fun onDestroy() {
        activeStreamRequests.keys.toList().forEach { closeStreamRequest(it, true) }
        super.onDestroy()
    }

    /**
     * Initiate Web host HTTP request
     */
    private fun httpRequest(params: String?, callback: KuiklyRenderCallback?) {
        // Get HTTP request parameters
        val paramsJSON = params.toJSONObjectSafely()
        var url = paramsJSON.optString("url")
        val method = paramsJSON.optString("method")
        val param = paramsJSON.optJSONObject("param")
        val header = paramsJSON.optJSONObject("headers")
        val cookie = paramsJSON.optString("cookie")
        // Request timeout duration, original unit is seconds
        val timeout = paramsJSON.optInt("timeout") * 1000
        // HTTP response status code
        var httpCode = 0
        // HTTP response header information
        var httpHeaders = ""
        // Get the actual request url link
        url = getRequestUrl(method, url, param)
        Log.trace("httpRequestParams", param.toString(), getPostParams(param))

        // Timeout judgment
        val requestTimeoutPromise = Promise<Any> { _, reject ->
            // Directly throw an exception after timeout
            kuiklyWindow.setTimeout({
                reject(Error("request timeout"))
            }, timeout)
        }
        // Use window.fetch to initiate network request
        val fetchPromise = kuiklyWindow.fetch(
            url, RequestInit(
                // Request method
                method = method,
                // Include all cookies
                credentials = RequestCredentials.INCLUDE,
                // Request header information
                headers = getRequestHeaders(header, cookie),
                // Request data, non-POST request passes null
                body = if (method == HTTP_METHOD_POST) getPostParams(param) else null,
                // Request mode is cross-domain mode
                mode = RequestMode.CORS
            ).also {
                // Attach the timeout via a non-standard field so that MiniGlobal.fetch in the
                // mini program runtime can forward it to wx.request. Browsers ignore unknown
                // RequestInit fields, so this is safe for the H5 environment as well.
                if (timeout > 0) {
                    it.asDynamic()["timeout"] = timeout
                }
            }
        )
        // Timeout and response, whoever comes first is processed first
        Promise.race(arrayOf(requestTimeoutPromise, fetchPromise.unsafeCast<Promise<*>>()))
            .then { rsp ->
                // Since timeout won't go to the then logic, this is definitely a Response object
                val response = rsp.unsafeCast<Response>()

                // Save the status code of this response
                httpCode = response.status.toInt()
                // Save headers
                httpHeaders = JSON.stringify(response.headers)
                if (!response.ok) {
                    // Response is not normal, return empty content
                    ""
                } else {
                    // Convert the result to json
                    response.json()
                }
            }
            .then { data ->
                if (httpCode in 200..299) {
                    // Normal response
                    fireSuccessCallback(callback, JSON.stringify(data), httpHeaders, httpCode)
                } else {
                    // Exception or error occurred, callback
                    fireErrorCallback(callback, "request error", httpCode)
                }
            }
            .catch { error ->
                val errorMsg = error.message ?: "Unknown error"
                val errorType = determineErrorType(error, errorMsg)
                Log.error("HTTP request failed: $errorType - $errorMsg")
                
                // Exception or error occurred, callback with detailed error info
                fireErrorCallback(
                    callback,
                    "$errorType: $errorMsg",
                    when {
                        httpCode != 0 -> httpCode
                        errorType == ERROR_TYPE_TIMEOUT -> STATE_CODE_TIMEOUT
                        errorType == ERROR_TYPE_NETWORK -> STATE_CODE_NETWORK_ERROR
                        else -> STATE_CODE_UNKNOWN
                    }
                )
            }
    }
    
    /**
     * Determine the type of error based on the error object and message
     */
    private fun determineErrorType(error: dynamic, errorMsg: String): String {
        return when {
            errorMsg.contains("timeout", ignoreCase = true) -> ERROR_TYPE_TIMEOUT
            errorMsg.contains("network", ignoreCase = true) -> ERROR_TYPE_NETWORK
            errorMsg.contains("abort", ignoreCase = true) -> ERROR_TYPE_ABORTED
            error.name == "TypeError" -> ERROR_TYPE_NETWORK
            error.name == "AbortError" -> ERROR_TYPE_ABORTED
            else -> ERROR_TYPE_UNKNOWN
        }
    }

    /**
     * Initiate Web host HTTP streaming request
     */
    private fun httpRequestBinary(params: Any?, callback: KuiklyRenderCallback?) {
        // Get http request related parameters
        val dataArray = params.unsafeCast<Array<*>>()

        val body = dataArray[1].unsafeCast<ByteArray>()
        val paramsString = dataArray[0].unsafeCast<String>()
        val paramsArray = JSONObject(paramsString)
        val url = paramsArray.optString("url")
        val headerStr = paramsArray.optString("headers")
        val cookie = paramsArray.optString("cookie")
        var method = paramsArray.optString("method")
        val timeout = paramsArray.optInt("timeout") * 1000

        Log.trace("httpStreamRequest", url ?: "", body, headerStr, cookie, timeout)

        // HTTP response status code
        var httpCode = 0
        // HTTP response header information
        var httpHeaders = ""

        // Timeout judgment
        val requestTimeoutPromise = Promise<Any> { _, reject ->
            // Directly throw an exception after timeout
            kuiklyWindow.setTimeout({
                reject(Error("httpStreamRequest timeout"))
            }, timeout)
        }
        // Use window.fetch to initiate network request
        val fetchPromise = kuiklyWindow.fetch(
            url, RequestInit(
                // Request method
                method = method,
                // Include all cookies
                credentials = RequestCredentials.INCLUDE,
                // Request header information
                headers = getRequestHeaders(if (headerStr.isNullOrEmpty()) null else JSONObject(headerStr), cookie),
                // Request data, non-POST request passes null
                body = body.toBlob(),
                // Request mode is cross-domain mode
                mode = RequestMode.CORS
            ).also {
                // Attach the timeout via a non-standard field so that MiniGlobal.fetch in the
                // mini program runtime can forward it to wx.request. Browsers ignore unknown
                // RequestInit fields, so this is safe for the H5 environment as well.
                if (timeout > 0) {
                    it.asDynamic()["timeout"] = timeout
                }
            }
        )
        // Timeout and response, whoever comes first is processed first
        Promise.race(arrayOf(requestTimeoutPromise, fetchPromise.unsafeCast<Promise<*>>()))
            .then { rsp ->
                // Since timeout won't go to the then logic, this is definitely a Response object
                val response = rsp.unsafeCast<Response>()

                // Save the status code of this response
                httpCode = response.status.toInt()

                // Save headers
                httpHeaders = JSON.stringify(response.headers)
                if (!response.ok) {
                    // Response is not normal, return empty content
                    ArrayBuffer(0)
                } else {
                    response.arrayBuffer()
                }
            }
            .then { data ->
                if (httpCode in 200..299) {
                    // Normal response
                    fireStreamRequestResultCallback(
                        callback,
                        data.unsafeCast<ArrayBuffer>(),
                        httpHeaders,
                        "",
                        httpCode
                    )
                } else {
                    // Exception or error occurred, callback
                    fireStreamRequestResultCallback(
                        callback,
                        ArrayBuffer(0),
                        httpHeaders,
                        "request error",
                        httpCode
                    )
                }
            }
            .catch { error ->
                val errorMsg = error.message ?: "Unknown error"
                val errorType = determineErrorType(error, errorMsg)
                Log.error("HTTP stream request failed: $errorType - $errorMsg")
                
                // Exception or error occurred, callback with detailed error info
                fireStreamRequestResultCallback(
                    callback,
                    ArrayBuffer(0),
                    httpHeaders,
                    "$errorType: $errorMsg",
                    when {
                        httpCode != 0 -> httpCode
                        errorType == ERROR_TYPE_TIMEOUT -> STATE_CODE_TIMEOUT
                        errorType == ERROR_TYPE_NETWORK -> STATE_CODE_NETWORK_ERROR
                        else -> STATE_CODE_UNKNOWN
                    }
                )
            }
    }

    /** Initiate a streaming request using the public JSON-string bridge protocol. */
    private fun httpStreamRequest(params: String?, callback: KuiklyRenderCallback?) {
        val paramsJSON = params.toJSONObjectSafely()
        val requestId = paramsJSON.optString("requestId")
        if (requestId.isEmpty() || activeStreamRequests.containsKey(requestId)) {
            callback?.invoke(
                mapOf(
                    KEY_STREAM_EVENT to STREAM_EVENT_ERROR,
                    KEY_STREAM_DATA to if (requestId.isEmpty()) "requestId is empty" else "duplicate requestId",
                    KEY_STATUS_CODE to STATE_CODE_UNKNOWN
                )
            )
            return
        }

        val streamingSupported = js(
            "typeof ReadableStream !== 'undefined' && typeof TextDecoder !== 'undefined'"
        ) as Boolean
        if (!streamingSupported) {
            // Mini-program fetch polyfills only resolve after the whole body arrives, which never
            // happens for a long-lived SSE connection. Fail explicitly instead of hanging.
            callback?.invoke(
                mapOf(
                    KEY_STREAM_EVENT to STREAM_EVENT_ERROR,
                    KEY_STREAM_DATA to ERROR_MSG_STREAM_UNSUPPORTED,
                    KEY_STATUS_CODE to STATE_CODE_UNKNOWN
                )
            )
            return
        }

        val method = paramsJSON.optString("method")
        val param = paramsJSON.optJSONObject("param")
        val headers = paramsJSON.optJSONObject("headers")
        val cookie = paramsJSON.optString("cookie")
        val timeoutS = paramsJSON.optInt("timeout")
        val idleTimeoutMs = (if (timeoutS > 0) timeoutS else DEFAULT_TIMEOUT_S) * 1000
        val url = getRequestUrl(method, paramsJSON.optString("url"), param)
        val controller: dynamic = js("typeof AbortController !== 'undefined' ? new AbortController() : null")
        val state: dynamic = json(
            "controller" to controller,
            "reader" to null,
            "timeoutHandle" to null
        )
        activeStreamRequests[requestId] = state

        val requestHeaders = getStreamRequestHeaders(headers, cookie)
        val requestInit = RequestInit(
            method = method,
            credentials = RequestCredentials.INCLUDE,
            headers = requestHeaders.first,
            body = if (method == HTTP_METHOD_POST && param != null) {
                if (requestHeaders.second) param.toString() else buildFormUrlEncodedBody(param)
            } else {
                null
            },
            mode = RequestMode.CORS
        ).also {
            if (controller != null) it.asDynamic()["signal"] = controller.signal
        }
        armStreamIdleTimer(requestId, idleTimeoutMs, callback)

        kuiklyWindow.fetch(url, requestInit)
            .then { response ->
                if (!activeStreamRequests.containsKey(requestId)) return@then null
                val statusCode = response.status.toInt()
                val responseHeaders = serializeHeaders(response.headers)
                if (!response.ok) {
                    finishStreamRequest(
                        requestId,
                        callback,
                        mapOf(
                            KEY_STREAM_EVENT to STREAM_EVENT_ERROR,
                            KEY_STREAM_DATA to "HTTP error $statusCode",
                            KEY_HEADERS to responseHeaders,
                            KEY_STATUS_CODE to statusCode
                        )
                    )
                    return@then null
                }

                val reader = response.asDynamic().body?.getReader()
                if (reader == null) {
                    finishStreamRequest(
                        requestId,
                        callback,
                        mapOf(
                            KEY_STREAM_EVENT to STREAM_EVENT_ERROR,
                            KEY_STREAM_DATA to ERROR_MSG_STREAM_UNSUPPORTED,
                            KEY_HEADERS to responseHeaders,
                            KEY_STATUS_CODE to statusCode
                        ),
                        abort = true
                    )
                    return@then null
                }
                state.reader = reader
                armStreamIdleTimer(requestId, idleTimeoutMs, callback)
                val decoder: dynamic = js("new TextDecoder('utf-8')")
                return@then readStreamChunks(
                    requestId,
                    reader,
                    decoder,
                    callback,
                    responseHeaders,
                    statusCode,
                    idleTimeoutMs,
                    true
                )
            }
            .catch { error ->
                finishStreamRequest(
                    requestId,
                    callback,
                    mapOf(
                        KEY_STREAM_EVENT to STREAM_EVENT_ERROR,
                        KEY_STREAM_DATA to (error.message ?: "Unknown error"),
                        KEY_STATUS_CODE to STATE_CODE_UNKNOWN
                    )
                )
            }
    }

    private fun readStreamChunks(
        requestId: String,
        reader: dynamic,
        decoder: dynamic,
        callback: KuiklyRenderCallback?,
        headers: String,
        statusCode: Int,
        idleTimeoutMs: Int,
        firstChunk: Boolean
    ): Promise<dynamic> {
        val readPromise = reader.read().unsafeCast<Promise<dynamic>>()
        return readPromise.then { result: dynamic ->
            if (!activeStreamRequests.containsKey(requestId)) return@then null
            if (result.done == true) {
                val tail = decoder.decode() as? String
                val events = mutableListOf<Map<String, Any>>()
                if (!tail.isNullOrEmpty()) {
                    events.add(mapOf(KEY_STREAM_EVENT to STREAM_EVENT_DATA, KEY_STREAM_DATA to tail))
                }
                events.add(mapOf(KEY_STREAM_EVENT to STREAM_EVENT_COMPLETE, KEY_STREAM_DATA to ""))
                finishStreamRequest(requestId, callback, *events.toTypedArray())
                return@then null
            }

            armStreamIdleTimer(requestId, idleTimeoutMs, callback)
            val text = decoder.decode(result.value, json("stream" to true)) as String
            var nextIsFirstChunk = firstChunk
            if (text.isNotEmpty()) {
                val event = mutableMapOf<String, Any>(
                    KEY_STREAM_EVENT to STREAM_EVENT_DATA,
                    KEY_STREAM_DATA to text
                )
                if (firstChunk) {
                    event[KEY_HEADERS] = headers
                    event[KEY_STATUS_CODE] = statusCode
                    nextIsFirstChunk = false
                }
                callback?.invoke(event)
            }
            readStreamChunks(requestId, reader, decoder, callback, headers, statusCode, idleTimeoutMs, nextIsFirstChunk)
        }
    }

    /**
     * The stream timeout is an idle timeout shared by all platforms: it is restarted whenever
     * response headers or a chunk arrive, so a connection that keeps pushing is never cut off.
     */
    private fun armStreamIdleTimer(requestId: String, idleTimeoutMs: Int, callback: KuiklyRenderCallback?) {
        val state = activeStreamRequests[requestId] ?: return
        if (state.timeoutHandle != null) kuiklyWindow.asDynamic().clearTimeout(state.timeoutHandle)
        state.timeoutHandle = kuiklyWindow.setTimeout({
            finishStreamRequest(
                requestId,
                callback,
                mapOf(
                    KEY_STREAM_EVENT to STREAM_EVENT_ERROR,
                    KEY_STREAM_DATA to "request timeout",
                    KEY_STATUS_CODE to STATE_CODE_TIMEOUT
                ),
                abort = true
            )
        }, idleTimeoutMs)
    }

    private fun closeStreamRequest(params: String?) {
        val requestId = params.toJSONObjectSafely().optString("requestId")
        closeStreamRequest(requestId, true)
    }

    /** Returns false if the request was already closed or finished. */
    private fun closeStreamRequest(requestId: String, abort: Boolean): Boolean {
        val state = activeStreamRequests.remove(requestId) ?: return false
        if (state.timeoutHandle != null) kuiklyWindow.asDynamic().clearTimeout(state.timeoutHandle)
        if (abort) {
            if (state.controller != null) {
                state.controller.abort()
            } else if (state.reader != null) {
                state.reader.cancel().unsafeCast<Promise<Any?>>().catch { }
            }
        }
        return true
    }

    /**
     * Single terminal-state entry: cleanup happens before notifying the caller, and a request
     * that was already closed or finished emits nothing.
     */
    private fun finishStreamRequest(
        requestId: String,
        callback: KuiklyRenderCallback?,
        vararg events: Map<String, Any>,
        abort: Boolean = false
    ) {
        if (!closeStreamRequest(requestId, abort)) return
        events.forEach { callback?.invoke(it) }
    }

    private fun serializeHeaders(headers: Headers): String {
        val values = json()
        headers.asDynamic().forEach { value: String, key: String -> values[key] = value }
        return JSON.stringify(values)
    }

    private fun ByteArray.toBlob(): ArrayBuffer {
        val buffer = ArrayBuffer(size)
        val uint8Array = Uint8Array(buffer)
        forEachIndexed { index, byte ->
            uint8Array[index] = byte
        }
        return buffer
    }

    private fun fireStreamRequestResultCallback(
        callback: KuiklyRenderCallback?,
        arrayBuffer: ArrayBuffer,
        headers: String,
        errorMsg: String,
        statusCode: Int,
    ) {
        val info = JSONObject().apply {
            put("headers", headers)
            put("errorMsg", errorMsg)
            put("statusCode", statusCode)
            put("success", 1)
        }
        callback?.invoke(
            arrayOf(
                info.toString(),
                arrayBuffer.toByteArray(),
            )
        )
    }

    private fun ArrayBuffer.toByteArray(): ByteArray {
        val int8Array = Uint8Array(this)
        return ByteArray(int8Array.length) { index ->
            int8Array[index]
        }
    }

    /**
     * Get the actual request url link
     */
    private fun getRequestUrl(method: String, url: String, params: JSONObject?): String {
        // If it's a GET request, parameters need to be appended to the url link
        if (method == HTTP_METHOD_GET) {
            var reqParams = ""
            if (params != null) {
                val urlParams = arrayListOf<String>()
                val keys = params.keys()
                for (key in keys) {
                    val v = params.opt(key)?.toString() ?: ""
                    urlParams.add("$key=${kuiklyWindow.asDynamic().encodeURIComponent(v)}")
                }
                reqParams = urlParams.joinToString("&")
            }

            // Append query parameters to the link
            return if (url.contains("?")) {
                "$url&$reqParams"
            } else {
                "$url?$reqParams"
            }
        } else {
            return url
        }
    }


    /**
     * Normal callback
     */
    private fun fireSuccessCallback(
        callback: KuiklyRenderCallback?,
        resultData: String,
        headers: String,
        statusCode: Int
    ) {
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

    /**
     * Exception callback
     */
    private fun fireErrorCallback(
        callback: KuiklyRenderCallback?,
        errorMsg: String,
        statusCode: Int
    ) {
        callback?.invoke(
            mapOf(
                KEY_SUCCESS to 0,
                KEY_ERROR_MSG to errorMsg,
                KEY_STATUS_CODE to statusCode
            )
        )
    }

    /**
     * Get post parameters
     */
    private fun getPostParams(param: JSONObject?): String {
        if (param == null) {
            return ""
        }
        return param.toString()
    }

    /**
     * Headers for stream requests. Unlike [getRequestHeaders], the caller's Content-Type is kept
     * and decides the body format (same contract as the native renderers): JSON when it contains
     * "json", otherwise application/x-www-form-urlencoded, which is also the default.
     *
     * @return the headers and whether the body should be JSON
     */
    private fun getStreamRequestHeaders(header: JSONObject?, cookie: String?): Pair<Headers, Boolean> {
        val headers = json()
        var contentType: String? = null
        if (header != null) {
            for (key in header.keys()) {
                val value = header.opt(key)?.toString() ?: ""
                headers[key] = value
                if (key.equals(HEADER_CONTENT_TYPE, ignoreCase = true)) {
                    contentType = value
                }
            }
        }
        if (!cookie.isNullOrEmpty()) {
            headers["Cookie"] = cookie
        }
        if (contentType == null) {
            headers[HEADER_CONTENT_TYPE] = CONTENT_TYPE_FORM
        }
        return Pair(Headers(headers), contentType?.contains("json", ignoreCase = true) == true)
    }

    private fun buildFormUrlEncodedBody(param: JSONObject): String {
        val parts = arrayListOf<String>()
        for (key in param.keys()) {
            val value = param.opt(key)?.toString() ?: ""
            parts.add("${formUrlEncode(key)}=${formUrlEncode(value)}")
        }
        return parts.joinToString("&")
    }

    /** application/x-www-form-urlencoded byte serializer (UTF-8, space as '+'). */
    private fun formUrlEncode(value: String): String {
        val encoded = kuiklyWindow.asDynamic().encodeURIComponent(value) as String
        val sb = StringBuilder(encoded.length)
        for (c in encoded) {
            when (c) {
                '!' -> sb.append("%21")
                '\'' -> sb.append("%27")
                '(' -> sb.append("%28")
                ')' -> sb.append("%29")
                '~' -> sb.append("%7E")
                else -> sb.append(c)
            }
        }
        return sb.toString().replace("%20", "+")
    }

    /**
     * Get custom header
     */
    private fun getRequestHeaders(header: JSONObject?, cookie: String? = null): Headers {
        val headers = json()
        // Process request header
        if (header != null) {
            val headerKeys = header.keys()
            for (key in headerKeys) {
                // Get the input header value
                val headerString = header.opt(key)
                headers[key] = if (headerString === null) {
                    // Empty value
                    ""
                } else if (headerString is String) {
                    // Original value
                    headerString
                } else {
                    // JSONObject, convert to String
                    headerString.toString()
                }
            }
        }

        if (cookie != null) {
            headers["Cookie"] = cookie
        }
        headers["Content-Type"] = "application/json"
        return Headers(headers)
    }

    companion object {
        const val MODULE_NAME = "KRNetworkModule"
        private const val METHOD_HTTP_REQUEST = "httpRequest"
        private const val METHOD_HTTP_REQUEST_BINARY = "httpRequestBinary"
        private const val METHOD_HTTP_STREAM_REQUEST = "httpStreamRequest"
        private const val METHOD_CLOSE_STREAM_REQUEST = "closeStreamRequest"
        // Network request success
        private const val KEY_SUCCESS = "success"
        // Network request failure
        private const val KEY_ERROR_MSG = "errorMsg"
        // Network request header field name
        private const val KEY_HEADERS = "headers"
        // Network request status code field
        private const val KEY_STATUS_CODE = "statusCode"
        private const val KEY_STREAM_EVENT = "event"
        private const val KEY_STREAM_DATA = "data"
        private const val STREAM_EVENT_DATA = "data"
        private const val STREAM_EVENT_COMPLETE = "complete"
        private const val STREAM_EVENT_ERROR = "error"
        private const val HEADER_CONTENT_TYPE = "Content-Type"
        private const val CONTENT_TYPE_FORM = "application/x-www-form-urlencoded; charset=UTF-8"
        private const val ERROR_MSG_STREAM_UNSUPPORTED = "streaming response is not supported in this environment"
        // Default stream idle timeout in seconds, used when timeout <= 0 (same on all platforms)
        private const val DEFAULT_TIMEOUT_S = 30
        // GET request method
        private const val HTTP_METHOD_GET = "GET"
        // POST request method
        private const val HTTP_METHOD_POST = "POST"
        // Unknown status code
        private const val STATE_CODE_UNKNOWN = -1000
        // Timeout status code
        private const val STATE_CODE_TIMEOUT = -1001
        // Network error status code
        private const val STATE_CODE_NETWORK_ERROR = -1002
        // HTTP success status code range
        private val HTTP_SUCCESS_RANGE = 200..299
        
        // Error type constants
        private const val ERROR_TYPE_TIMEOUT = "Timeout"
        private const val ERROR_TYPE_NETWORK = "NetworkError"
        private const val ERROR_TYPE_ABORTED = "Aborted"
        private const val ERROR_TYPE_UNKNOWN = "UnknownError"
    }
}
