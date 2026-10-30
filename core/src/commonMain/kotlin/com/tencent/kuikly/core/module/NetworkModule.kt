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

import com.tencent.kuikly.core.base.toBoolean
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

typealias NMResponse = (data: JSONObject, success : Boolean , errorMsg: String) -> Unit

typealias NMAllResponse = (data: JSONObject, success : Boolean , errorMsg: String, response: NetworkResponse) -> Unit

typealias NMDataResponse = (data: ByteArray, success: Boolean, errorMsg: String, response: NetworkResponse) -> Unit

/**
 * 流式 HTTP 文本请求的事件回调，见 [NetworkModule.httpStreamRequest]。
 *
 * 每个请求最多收到一次终态事件（complete 或 error），之后不再回调；主动调用
 * [NetworkModule.StreamRequestHandle.close] 后不会再收到任何事件。
 *
 * @param event 事件类型："data" | "complete" | "error"
 * @param data "data" 时为本次收到的 UTF-8 文本分片（边界由网络决定，不是完整的 SSE 事件或业务消息，
 *             非法字节会被替换为 U+FFFD）；"complete" 时为空字符串；"error" 时为错误描述
 * @param response 首次 "data" 事件携带响应头与状态码；HTTP 错误导致的 "error" 事件同样携带；
 *                 其余事件为 null。鸿蒙平台在数据到达时无法取得状态码，首次 "data" 的
 *                 statusCode 为 null，非 2xx 响应以 "error" 事件的 statusCode 为准
 */
typealias NMStreamEventCallback = (event: String, data: String, response: NetworkResponse?) -> Unit

/**
 * 表示网络响应的数据类。
 *
 * @property headerFields 包含响应头字段的JSONObject。
 * @property statusCode 可选的Int，表示响应的状态码。如果此值为null，
 *                      则表示端版本较低，不支持传递状态码。
 */
data class NetworkResponse(val headerFields: JSONObject, val statusCode: Int? = null)

/**
 * Http网络请求模块
 */
class NetworkModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    /*
     * @brief get请求
     */
    @Deprecated(
        "Use requestGet with NMAllResponse instead",
        ReplaceWith("requestGet(url, param) { data, success, errorMsg, response -> responseCallback(data, success, errorMsg) }")
    )
    inline fun requestGet(url : String, param: JSONObject, crossinline responseCallback: NMResponse) {
        requestGet(url, param) { data, success, errorMsg, _ ->
            responseCallback(data, success, errorMsg)
        }
    }

    /*
     * @brief get请求(新)
     * 注：responseCallback中带有response.headers回包数据
     */
    fun requestGet(url : String, param: JSONObject, responseCallback: NMAllResponse) {
        httpRequest(url, false, param, null, null, 30, responseCallback)
    }

    /*
     * @brief post请求
     */
    @Deprecated(
        "Use requestPost with NMAllResponse instead",
        ReplaceWith("requestPost(url, param) { data, success, errorMsg, response -> responseCallback(data, success, errorMsg) }"))
    inline fun requestPost(url : String, param: JSONObject, crossinline responseCallback: NMResponse) {
        requestPost(url, param) { data, success, errorMsg, _ ->
            responseCallback(data, success, errorMsg)
        }
    }

    /*
     * @brief post请求(新)
     * 注：responseCallback中带有response.headers回包数据
     */
    fun requestPost(url : String, param: JSONObject, responseCallback: NMAllResponse) {
        httpRequest(url, true, param, null, null, 30, responseCallback)
    }

    /**
     * @brief 通用http请求
     * 注：1. headers中可添加"Content-Type": "application/json"
     *    2. 如果接口回包数据类型为非json格式，回包数据字符串会以{data:xxxx}被包装一层，其中xxxx为接口实际回包内容
     */
    @Deprecated(
        "Use httpRequest with NMAllResponse instead",
        ReplaceWith("httpRequest(url, isPost, param, headers, cookie, timeout) { data, success, errorMsg, response -> responseCallback(data, success, errorMsg) }"))
    inline fun httpRequest(url : String , isPost: Boolean, param: JSONObject, headers: JSONObject? = null, cookie : String? = null, timeout : Int = 30, crossinline responseCallback: NMResponse ) {
        httpRequest(url, isPost, param, headers, cookie, timeout) { data, success, errorMsg, _ ->
            responseCallback(data, success, errorMsg)
        }
    }

    /**
     * 通用http请求(新) 注：
     *
     *    1. headers中可添加"Content-Type": "application/json"
     *    2. 如果接口回包数据类型为非json格式，回包数据字符串会以{data:xxxx}被包装一层，其中xxxx为接口实际回包内容
     *    3. responseCallback中带有response.headers回包数据
     */
    fun httpRequest(
        url: String,
        isPost: Boolean,
        param: JSONObject,
        headers: JSONObject? = null,
        cookie: String? = null,
        timeout: Int = 30,
        responseCallback: NMAllResponse
    ) {
        val params = JSONObject().apply {
            put("url", url)
            put("method", if (isPost) "POST" else "GET")
            put("param", param)
            headers?.also {
                put("headers", it)
            }
            cookie?.also {
                put("cookie", it)
            }
            put("timeout", timeout)
        }
        toNative(
            false,
            METHOD_HTTP_REQUEST,
            params.toString(),
            callback = { res ->
                res?.also {
                    // optParsedChild 已经尝试过解析；返回 null 表示无法解析成对象，
                    // 这里按原语义包成 {data: 原文}，不再重复解析一次。
                    val dataJSON = it.optParsedChild("data") ?: JSONObject().apply {
                        put("data", it.optString("data", ""))
                    }
                    val headers = it.optString("headers", "{}").toJSONObjectSafely()
                    val success = it.optInt("success").toBoolean()
                    val errorMsg = it.optString("errorMsg")
                    var statusCode: Int? = null
                    if (it.has("statusCode")) {
                        statusCode = it.optInt("statusCode")
                    }
                    val response = NetworkResponse(headers ?: JSONObject(), statusCode)
                    responseCallback(dataJSON, success, errorMsg, response)
                }
            }
        )
    }

    /**
     * 二进制Get请求
     */
    fun requestGetBinary(url: String, param: JSONObject, responseCallback: NMDataResponse) {
        httpRequestBinary(url, false, ByteArray(0), param, null, null, 30, responseCallback)
    }

    /**
     * 二进制Post请求
     */
    fun requestPostBinary(url: String, bytes: ByteArray, responseCallback: NMDataResponse) {
        httpRequestBinary(url, true, bytes, null, null, null, 30, responseCallback)
    }

    /**
     * 通用http请求，二进制方式
     */
    fun httpRequestBinary(
        url: String,
        isPost: Boolean,
        bytes: ByteArray,
        param: JSONObject? = null,
        headers: JSONObject? = null,
        cookie: String? = null,
        timeout: Int = 30,
        responseCallback: NMDataResponse
    ) {
        val params = JSONObject().apply {
            put("url", url)
            put("method", if (isPost) "POST" else "GET")
            param?.also {
                put("param", it)
            }
            headers?.also {
                put("headers", it)
            }
            cookie?.also {
                put("cookie", it)
            }
            put("timeout", timeout)
        }

        // 将参数转换为数组格式，第一个元素是JSON字符串，第二个元素是二进制数据
        val args = arrayOf<Any>(params.toString(), bytes)

        asyncToNativeMethod(
            METHOD_HTTP_REQUEST_BINARY,
            args,
            callbackFn = { res ->
                if (res is Array<*> && res.size >= 2) {
                    val info = res[0]?.toJSONObjectSafely() ?: return@asyncToNativeMethod
                    val data = res[1] as? ByteArray ?: return@asyncToNativeMethod
                    val headers = info.optString("headers", "{}").toJSONObjectSafely()
                    val success = info.optInt("success").toBoolean()
                    val errorMsg = info.optString("errorMsg")
                    val statusCode = info.optInt("statusCode")
                    val response = NetworkResponse(headers ?: JSONObject(), statusCode)
                    responseCallback(data, success, errorMsg, response)
                }
            }
        )
    }

    /**
     * 流式 HTTP 文本请求：在响应体到达过程中按网络分片逐块回调 UTF-8 文本，无需等待完整响应。
     *
     * 本接口只负责传输文本，不解析上层协议。对接 SSE、NDJSON 等协议时，业务需将 "data" 分片
     * 按顺序拼接后自行增量解析（SSE 可参考 Demo 中的 SSEEventParser）。目前仅支持 Android、iOS、鸿蒙。
     *
     * @param url 请求 URL
     * @param isPost 是否为 POST 请求
     * @param param 请求参数。GET 时拼接到 URL；POST 时作为 body：Content-Type 含 json 时序列化为 JSON，
     *              否则按 UTF-8 application/x-www-form-urlencoded 编码（未指定 Content-Type 时自动补齐）
     * @param headers 自定义请求头
     * @param cookie Cookie 字符串
     * @param timeout 连接超时与空闲超时（秒）：连续 timeout 秒没有收到数据即以 error 结束，收到数据会重新计时，
     *                不限制持续推送的总时长；<= 0 时使用默认值 30 秒
     * @param eventCallback 流式事件回调
     * @return StreamRequestHandle 流式请求句柄
     */
    fun httpStreamRequest(
        url: String,
        isPost: Boolean,
        param: JSONObject? = null,
        headers: JSONObject? = null,
        cookie: String? = null,
        timeout: Int = 30,
        eventCallback: NMStreamEventCallback
    ): StreamRequestHandle {
        val requestId = "stream_${streamRequestIdCounter++}"
        val params = JSONObject().apply {
            put("url", url)
            put("method", if (isPost) "POST" else "GET")
            param?.also {
                put("param", it)
            }
            headers?.also {
                put("headers", it)
            }
            cookie?.also {
                put("cookie", it)
            }
            put("timeout", timeout)
            put("requestId", requestId)
        }
        var callbackRef: CallbackRef? = null
        var terminalEventBeforeRefAssigned = false
        val returnValue = toNative(
            true,
            METHOD_HTTP_STREAM_REQUEST,
            params.toString(),
            callback = { res ->
                res?.also {
                    val event = it.optString("event", "")
                    val data = it.optString("data", "")
                    var networkResponse: NetworkResponse? = null
                    if (it.has("headers") || it.has("statusCode")) {
                        val respHeaders = it.optString("headers", "{}").toJSONObjectSafely()
                        var statusCode: Int? = null
                        if (it.has("statusCode")) {
                            statusCode = it.optInt("statusCode")
                        }
                        networkResponse = NetworkResponse(respHeaders ?: JSONObject(), statusCode)
                    }
                    val isTerminalEvent = event == STREAM_EVENT_COMPLETE || event == STREAM_EVENT_ERROR
                    try {
                        eventCallback(event, data, networkResponse)
                    } finally {
                        if (isTerminalEvent) {
                            val ref = callbackRef
                            if (ref != null) {
                                removeCallback(ref)
                            } else {
                                terminalEventBeforeRefAssigned = true
                            }
                        }
                    }
                }
            },
            syncCall = false
        )
        callbackRef = returnValue.callbackRef
        if (terminalEventBeforeRefAssigned) {
            callbackRef?.also { removeCallback(it) }
        }
        return StreamRequestHandle(this, callbackRef, requestId)
    }

    /**
     * 流式请求句柄，用于主动关闭流式连接。
     */
    class StreamRequestHandle internal constructor(
        private val module: NetworkModule,
        private val callbackRef: CallbackRef?,
        private val requestId: String
    ) {
        private var closed = false

        /**
         * 关闭流式连接并释放资源。可重复调用；关闭后不会再回调任何事件（包括 complete/error）。
         */
        fun close() {
            if (closed) return
            closed = true
            val params = JSONObject().apply {
                put("requestId", requestId)
            }
            module.toNative(
                false,
                METHOD_CLOSE_STREAM_REQUEST,
                params.toString(),
                null,
                false
            )
            callbackRef?.also {
                module.removeCallback(it)
            }
        }
    }

    companion object {
        const val MODULE_NAME = ModuleConst.NETWORK
        private const val METHOD_HTTP_REQUEST = "httpRequest"
        private const val METHOD_HTTP_REQUEST_BINARY = "httpRequestBinary"
        private const val METHOD_HTTP_STREAM_REQUEST = "httpStreamRequest"
        private const val METHOD_CLOSE_STREAM_REQUEST = "closeStreamRequest"
        private const val STREAM_EVENT_COMPLETE = "complete"
        private const val STREAM_EVENT_ERROR = "error"
        private var streamRequestIdCounter = 0L

        private fun Any.toJSONObjectSafely(): JSONObject? {
            return when {
                this is JSONObject -> this
                this is String && this.isNotEmpty() -> {
                    try {
                        JSONObject(this)
                    } catch (e: Throwable) {
                        null
                    }
                }
                else -> null
            }
        }
    }

}
