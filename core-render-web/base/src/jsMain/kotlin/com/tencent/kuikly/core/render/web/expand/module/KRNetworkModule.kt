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

/**
 * Kuikly network request module
 */
class KRNetworkModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_HTTP_REQUEST -> httpRequest(params, callback)
            else -> super.call(method, params, callback)
        }
    }

    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_HTTP_STREAM_REQUEST -> httpStreamRequest(params, callback)
            else -> super.call(method, params, callback)
        }
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
            )
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
            .catch {
                val errorMsg = it.message
                // Exception or error occurred, callback
                fireErrorCallback(
                    callback,
                    errorMsg ?: "io exception",
                    if (httpCode != 0) httpCode else STATE_CODE_UNKNOWN
                )
            }
    }

    /**
     * Initiate Web host HTTP streaming request
     */
    private fun httpStreamRequest(params: Any?, callback: KuiklyRenderCallback?) {
        // Get http request related parameters
        val dataArray = params.unsafeCast<Array<*>>()
        val url = dataArray[0]
        val body = dataArray[1].unsafeCast<ByteArray>()
        val headerStr = dataArray[2].unsafeCast<String>()
        // Web does not support custom cookie settings, can be handled in the host app
        val cookie = dataArray[3].unsafeCast<String>()
        // Request timeout duration, original unit is seconds
        val timeout = (dataArray[4].unsafeCast<Number>()).toInt() * 1000
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
                method = "POST",
                // Include all cookies
                credentials = RequestCredentials.INCLUDE,
                // Request header information
                headers = getRequestHeaders(JSONObject(headerStr), cookie),
                // Request data, non-POST request passes null
                body = body.toBlob(),
                // Request mode is cross-domain mode
                mode = RequestMode.CORS
            )
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
            .catch {
                val errorMsg = it.message
                // Exception or error occurred, callback
                fireStreamRequestResultCallback(
                    callback,
                    ArrayBuffer(0),
                    errorMsg ?: "io exception",
                    httpHeaders,
                    if (httpCode != 0) httpCode else STATE_CODE_UNKNOWN
                )
            }
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
        callback?.invoke(
            arrayOf(
                arrayBuffer.toByteArray(),
                headers,
                errorMsg,
                statusCode
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

        return Headers(headers)
    }

    companion object {
        const val MODULE_NAME = "KRNetworkModule"
        private const val METHOD_HTTP_REQUEST = "httpRequest"
        private const val METHOD_HTTP_STREAM_REQUEST = "httpStreamRequest"
        // Network request success
        private const val KEY_SUCCESS = "success"
        // Network request failure
        private const val KEY_ERROR_MSG = "errorMsg"
        // Network request header field name
        private const val KEY_HEADERS = "headers"
        // Network request status code field
        private const val KEY_STATUS_CODE = "statusCode"
        // GET request method
        private const val HTTP_METHOD_GET = "GET"
        // POST request method
        private const val HTTP_METHOD_POST = "POST"
        // Unknown status code
        private const val STATE_CODE_UNKNOWN = -1000

    }
}
