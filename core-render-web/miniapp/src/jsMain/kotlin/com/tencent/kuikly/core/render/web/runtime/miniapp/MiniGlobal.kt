package com.tencent.kuikly.core.render.web.runtime.miniapp

import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.global
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

/**
 * mini app http response class, used to transfer the response from wx.request to web Response type
 */
class MiniResponse(rsp: dynamic) {
    // response status code
    @JsName("status")
    val status: Int = rsp.statusCode as Int
    // response headers
    @JsName("headers")
    val headers: dynamic = if (jsTypeOf(rsp.headers) == "object")
        JSON.parse(JSON.stringify(rsp.headers))
    else json()
    // response is success
    @JsName("ok")
    val ok = true

    // real response data store by inner
    private val data: dynamic = rsp.data

    /**
     * return response data as json
     */
    @JsName("json")
    fun miniJson(): Promise<dynamic> = Promise { resolve, _ ->
        // direct return data when is object type
        if (jsTypeOf(data) == "object") {
            resolve(data)
        } else if (jsTypeOf(data) == "string") {
            // try to parse json string
            try {
                resolve(JSON.parse(data.unsafeCast<String>()))
            } catch (e: dynamic) {
                resolve(json())
            }
        } else {
            resolve(json())
        }
    }

    /**
     * return response data as ArrayBuffer
     */
    @JsName("arrayBuffer")
    fun arrayBuffer(): Promise<ArrayBuffer> = Promise { resolve, _ ->
        val dataType = jsTypeOf(data)
        // direct return data when is ArrayBuffer type
        if (dataType == "object") {
            resolve(data.unsafeCast<ArrayBuffer>())
        } else if (dataType == "string") {
            // convert string to ArrayBuffer
            val str = data.unsafeCast<String>()
            val buf = js("new TextEncoder().encode(str).buffer").unsafeCast<ArrayBuffer>()
            resolve(buf)
        } else {
            resolve(ArrayBuffer(0))
        }
    }
}

/**
 * mini app http headers class
 */
class Headers(init: Json?) {
    private val headersMap = mutableMapOf<String, MutableList<String>>()

    /**
     * init headers
     */
    private fun initData(init: Json) {
        val keys = js("Object.keys(init)") as Array<String>
        for (key in keys) {
            append(key, init[key].unsafeCast<String>())
        }
    }

    init {
        if (init != null) {
            initData(init)
        }
    }

    @JsName("append")
    fun append(name: String, value: String) {
        val key = name.lowercase()
        headersMap.getOrPut(key) { mutableListOf() }.add(value)
    }

    @JsName("set")
    fun set(name: String, value: String) {
        val key = name.lowercase()
        headersMap[key] = mutableListOf(value)
    }

    @JsName("get")
    fun get(name: String): String? {
        val key = name.lowercase()
        return headersMap[key]?.joinToString(", ")
    }

    @JsName("getAll")
    fun getAll(name: String): List<String> {
        val key = name.lowercase()
        return headersMap[key]?.toList() ?: emptyList()
    }

    @JsName("has")
    fun has(name: String): Boolean {
        val key = name.lowercase()
        return headersMap.containsKey(key)
    }

    @JsName("delete")
    fun delete(name: String) {
        val key = name.lowercase()
        headersMap.remove(key)
    }

    @JsName("forEach")
    fun forEach(action: (name: String, value: String) -> Unit) {
        for ((key, values) in headersMap) {
            action(key, values.joinToString(", "))
        }
    }

    @JsName("entries")
    fun entries(): List<Pair<String, String>> =
        headersMap.map { (k, v) -> k to v.joinToString(", ") }
}

/**
 * simulate LocalStorage
 */
object LocalStorage {
    /**
     * Get LocalStorage cached content
     */
    @JsName("getItem")
    fun getItem(key: String): String? =
        NativeApi.plat["getStorageSync"](key).unsafeCast<String?>()

    /**
     * Set LocalStorage cached content
     */
    @JsName("setItem")
    fun setItem(key: String, value: String) {
        NativeApi.plat["setStorageSync"](key, value)
    }

    /**
     * Remove LocalStorage cached content
     */
    @JsName("removeItem")
    fun removeItem(key: String) {
        NativeApi.plat["removeStorageSync"](key)
    }

}

/**
 * Mini program host global related operations
 */
object MiniGlobal {
    private const val BASE64CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="

    private const val DEV_TOOL = "devtools"

    // js base64
    private val jsEncodeBase64 = js(
        """
        function base64Encode(str, base64Table) {
            function stringToUTF8Bytes(input) {
            var bytes = [];
            for (var i = 0; i < input.length; i++) {
                var charCode = input.charCodeAt(i);
                if (charCode <= 0x7F) {
                    bytes.push(charCode);
                } else if (charCode <= 0x7FF) {
                    bytes.push(0xC0 | (charCode >> 6), 0x80 | (charCode & 0x3F));
                } else if (charCode <= 0xFFFF) {
                    bytes.push(0xE0 | (charCode >> 12), 0x80 | ((charCode >> 6) & 0x3F), 0x80 | (charCode & 0x3F));
                } else {
                    // Handle 4-byte UTF-8 characters (like emojis)
                    bytes.push(
                        0xF0 | (charCode >> 18),
                        0x80 | ((charCode >> 12) & 0x3F),
                        0x80 | ((charCode >> 6) & 0x3F),
                        0x80 | (charCode & 0x3F)
                    );
                }
            }
            return bytes;
            }
            
            // Convert UTF-8 byte stream to Base64 string
            var utf8Bytes = stringToUTF8Bytes(str);
            var base64String = '';
            for (var i = 0; i < utf8Bytes.length; i += 3) {
                var block = utf8Bytes[i] << 16;
                if (i + 1 < utf8Bytes.length) {
                    block |= utf8Bytes[i + 1] << 8;
                }
                if (i + 2 < utf8Bytes.length) {
                    block |= utf8Bytes[i + 2];
                }
                
                base64String += base64Table[(block >> 18) & 0x3F];
                base64String += base64Table[(block >> 12) & 0x3F];
                base64String += (i + 1 < utf8Bytes.length) ? base64Table[(block >> 6) & 0x3F] : '=';
                base64String += (i + 2 < utf8Bytes.length) ? base64Table[block & 0x3F] : '=';
            }
            
            return base64String;
        }
    """
    )

    // js base64 decode
    private val jsDecodeBase64 = js(
        """
        function (str, base64Chars) {
            var result = '';
            var i = 0;
            while (i < str.length) {
              var a = base64Chars.indexOf(str.charAt(i++));
              var b = base64Chars.indexOf(str.charAt(i++));
              var c = base64Chars.indexOf(str.charAt(i++));
              var d = base64Chars.indexOf(str.charAt(i++));
        
              var a1 = (a << 2) | (b >> 4);
              var a2 = ((b & 15) << 4) | (c >> 2);
              var a3 = ((c & 3) << 6) | d;
        
              result += String.fromCharCode(a1);
              if (c != 64) {
                result += String.fromCharCode(a2);
              }
              if (d != 64) {
                result += String.fromCharCode(a3);
              }
            }
            return result;
          }
    """
    )

    // GET request method
    private const val HTTP_METHOD_GET = "GET"

    @JsName("global")
    val globalThis: dynamic
        get() = js("global")

    private val miniSystemInitInfo = NativeApi.plat.getSystemInfoSync()

    // Basic information of the application
    val appBaseInfo = NativeApi.getAppBaseInfo()

    /**
     * Whether the current mini program platform is iOS
     */
    val isIOS: Boolean = miniSystemInitInfo.platform.unsafeCast<String>() == "ios"

    /**
     * Whether the current mini program platform is Android
     */
    val isAndroid: Boolean = miniSystemInitInfo.platform.unsafeCast<String>() == "android"

    /**
     * Whether the current mini program platform is developer tools
     */
    val isDevTools: Boolean = miniSystemInitInfo.platform.unsafeCast<String>() == "devtools"

    /**
     * Whether the current mini program platform is mobile
     */
    val isMobile: Boolean = isAndroid || isIOS

    val windowWidth: Int
        get() = miniSystemInitInfo.windowWidth.unsafeCast<Int>()

    val windowHeight: Int
        get() = miniSystemInitInfo.windowHeight.unsafeCast<Int>()


    val statusBarHeight: Int
        get() = miniSystemInitInfo.statusBarHeight.unsafeCast<Int>()

    /**
     * is debugger mode
     */
    var isDebuggerMode: Boolean = false

    @JsName("screen")
    val screen: Json = json(
        "width" to windowWidth,
        "height" to windowHeight,
    )

    @JsName("navigator")
    val navigator: Json = json(
        "appVersion" to appBaseInfo.SDKVersion.unsafeCast<String>(),
        "userAgent" to listOf(
            miniSystemInitInfo.platform,
            appBaseInfo.SDKVersion,
            miniSystemInitInfo.model,
            miniSystemInitInfo.system
        ).joinToString(" ").unsafeCast<String>()
    )

    @JsName("devicePixelRatio")
    val devicePixelRatio = miniSystemInitInfo.pixelRatio.unsafeCast<Double>()

    @JsName("localStorage")
    val localStorage = LocalStorage

    /**
     * Simulates the window.matchMedia API for mini program environment.
     * Mini programs run on mobile devices, so we return false for PC-related queries
     * and true for touch/coarse pointer queries.
     * 
     * @param query The media query string (e.g., "(pointer: fine)" for PC, "(pointer: coarse)" for mobile)
     * @return A MediaQueryResult object with matches property
     */
    @JsName("matchMedia")
    fun matchMedia(query: String): MediaQueryResult {
        // Mini programs always run on mobile devices (touch screen)
        // "(pointer: fine)" = PC/mouse device -> false for mini program
        // "(pointer: coarse)" = touch device -> true for mini program
        val matches = when {
            query.contains("pointer: fine") || query.contains("pointer:fine") -> false
            query.contains("pointer: coarse") || query.contains("pointer:coarse") -> true
            else -> false  // Default to false for unknown queries
        }
        return MediaQueryResult(matches)
    }

    /**
     * Whether the current device is iPhone X(has notch)
     */
    val isIphoneX: Boolean by lazy {
        hasIOSNotch(miniSystemInitInfo)
    }

    /**
     * Register call native method for kuikly core
     */
    fun registerCallNative(
        pageId: String,
        nativeMethod: (methodId: Int, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any?) -> Any?
    ) {
        globalThis.com.tencent.kuikly.core.nvi.registerCallNative(pageId, nativeMethod)
    }

    /**
     * Whether in development mode
     */
    fun isDev(): Boolean = isDebuggerMode

    /**
     * Mini program opens specified page
     */
    @JsName("open")
    fun open(params: String) {
        if (params.isEmpty()) {
            return
        }
        val pageData = params.split("?")
        if (pageData.size > 1) {
            val urlParams = pageData[1]
            NativeApi.plat.navigateTo(
                json(
                    "url" to "/pages/${getUrlParams("page_name", urlParams)}/index?${urlParams}"
                )
            )
        }
    }

    /**
     * Mini program closes current page
     */
    @JsName("close")
    fun close() {
        NativeApi.plat.navigateBack()
    }

    /**
     * First frame display callback
     */
    fun onFirstFrame(node: MiniElement, callback: (() -> Unit)?) {
        // todo temporarily not implemented, will check if support is possible later
    }

    /**
     * Add mini program host timer task
     */
    @JsName("setInterval")
    fun setInterval(handler: dynamic, timeout: Int): Int =
        js("setInterval")(handler, timeout).unsafeCast<Int>()

    /**
     * Clear mini program host timer task
     */
    @JsName("clearInterval")
    fun clearInterval(handler: dynamic) {
        js("clearInterval")(handler)
    }

    /**
     * Add mini program host delay task
     */
    @JsName("setTimeout")
    fun setTimeout(handler: dynamic, timeout: Int): Int =
        js("setTimeout")(handler, timeout).unsafeCast<Int>()

    /**
     * Clear mini program host delay task
     */
    @JsName("clearTimeout")
    fun clearTimeout(handler: Int) {
        js("clearTimeout")(handler)
    }

    /**
     * Mini program host url encode
     */
    @JsName("encodeURIComponent")
    fun encodeURIComponent(str: String, isBase64Handler: Boolean = false): String {
        return if (isBase64Handler) {
            str
        } else {
            js("encodeURIComponent")(str).unsafeCast<String>()
        }
    }

    /**
     * Mini program host url decode
     */
    @JsName("decodeURIComponent")
    fun decodeURIComponent(str: String, isBase64Handler: Boolean = false): String {
        return if (isBase64Handler) {
            // do nothing with base64
            str
        } else {
            js("decodeURIComponent")(str).unsafeCast<String>()
        }
    }

    /**
     * sames to window.escape
     */
    @JsName("escape")
    fun escape(str: String, isBase64Handler: Boolean = false): String {
        return if (isBase64Handler) {
            // do nothing with base64
            str
        } else {
            js("escape")(str).unsafeCast<String>()
        }
    }

    /**
     * same to window.unescape
     */
    @JsName("unescape")
    fun unescape(str: String, isBase64Handler: Boolean = false): String {
        return if (isBase64Handler) {
            str
        } else {
            js("unescape")(str).unsafeCast<String>()
        }
    }

    /**
     * Mini program host base64 encode
     */
    @JsName("btoa")
    fun btoa(str: String): String {
        // 1. string to Uint8Array
        val uint8Array = js("new TextEncoder().encode(str)").unsafeCast<Uint8Array>()
        // 2. Uint8Array.buffer to base64
        return NativeApi.plat.arrayBufferToBase64(uint8Array.buffer).unsafeCast<String>();
    }

    /**
     * Mini program host base64 decode
     */
    @JsName("atob")
    fun atob(str: String): String {
        // 2. Uint8Array.buffer to base64
        val uint8Array = NativeApi.plat.base64ToArrayBuffer(str).unsafeCast<Uint8Array>();
        // 2. ArrayBuffer to string
        return js("new TextDecoder().decode(uint8Array)").unsafeCast<String>()
    }

    /**
     * mini app send http request simulate window.fetch
     */
    @JsName("fetch")
    fun fetch(input: dynamic, init: RequestInit? = null): Promise<Response> {
        // success response
        var resolveFun: ((Response) -> Unit)? = null
        // error response
        var rejectFun: ((Throwable) -> Unit)? = null
        // fetch promise
        val fetchPromise = Promise { resolve, reject ->
            resolveFun = resolve
            rejectFun = reject
        }
        // params handle
        val url = input.unsafeCast<String>()
        val method = init?.method?.unsafeCast<String>() ?: HTTP_METHOD_GET
        val headers = init?.headers?.unsafeCast<Headers?>()
        val data = init?.body ?: null
        // is stream request or not
        val isStream = js("Object.prototype.toString.call(data)") == "[object ArrayBuffer]"
        val reqHeaders: Json = json()
        // translate headers to json format
        headers?.asDynamic()?.forEach { key, value, _ ->
            reqHeaders.set(key, value)
        }
        // real mini app request
        NativeApi.plat.request(MiniRequestInit(
            // Request URL
            url = url,
            // Request method
            method = method,
            // Request header information
            headers = reqHeaders,
            // Request data, null for non-POST requests
            data = data,
            // Request data type, default is json, configure others and mini program won't
            // automatically parse string
            dataType = if (isStream) "" else "json",
            // Default is text
            responseType = if (isStream) "arraybuffer" else "text",
            // Request success callback
            success = { rsp: Any ->
                resolveFun?.invoke(MiniResponse(rsp).unsafeCast<Response>())
            },
            // Request failure callback
            fail = { rsp: Any ->
                rejectFun?.invoke(rsp.unsafeCast<Throwable>())
            }
        ))

        return fetchPromise
    }

    /**
     * Does has iPhone notch
     */
    private fun hasIOSNotch(systemInfo: dynamic): Boolean {
        val platform = systemInfo.platform.unsafeCast<String?>() ?: return false
        val model = systemInfo.model.unsafeCast<String?>() ?: return false

        if ((platform != "ios" && platform != "devtools") || !model.startsWith("iPhone")) return false

        val safeArea = systemInfo.safeArea
        val safeAreaTop = safeArea?.top.unsafeCast<Double?>() ?: 0.0
        return safeAreaTop > 20
    }

    /**
     * get mini app request option
     */
    private fun MiniRequestInit(
        url: String? = undefined,
        method: String? = undefined,
        headers: dynamic = undefined,
        data: dynamic = undefined,
        dataType: String? = "json",
        responseType: String? = "text",
        success: (Any) -> Unit = {},
        fail: (Any) -> Unit = {}
    ): MiniRequestInit {
        val o = js("({})")
        o["url"] = url
        o["method"] = method
        o["header"] = headers
        o["data"] = data
        o["dataType"] = dataType
        o["responseType"] = responseType
        o["success"] = success
        o["fail"] = fail
        return o.unsafeCast<MiniRequestInit>()
    }

    /**
     * get field value of url params
     */
    private fun getUrlParams(field: String, url: String): String {
        val urlParamsMap = url
            .split("&")
            .mapNotNull { part ->
                val idx = part.indexOf("=")
                if (idx > 0) {
                    val key = decodeURIComponent(part.substring(0, idx))
                    val value = decodeURIComponent(part.substring(idx + 1))
                    key to value
                } else if (part.isNotBlank()) {
                    val key = decodeURIComponent(part)
                    key to ""
                } else {
                    null
                }
            }
            .toMap()

        return urlParamsMap[field] ?: ""
    }
}

external interface MiniRequestInit {
    var url: String?
        get() = definedExternally
        set(value) = definedExternally
    var method: String?
        get() = definedExternally
        set(value) = definedExternally
    var headers: dynamic
        get() = definedExternally
        set(value) = definedExternally
    var data: dynamic
        get() = definedExternally
        set(value) = definedExternally
    var dataType: String?
        get() = definedExternally
        set(value) = definedExternally
    var responseType: String?
        get() = definedExternally
        set(value) = definedExternally
    var success: (Any) -> Unit
    var fail: (Any) -> Unit
}

/**
 * Result object for matchMedia query, simulating the browser's MediaQueryList interface.
 * Used to provide compatibility for code that checks device type via media queries.
 */
class MediaQueryResult(
    @JsName("matches")
    val matches: Boolean
)
