package com.tencent.kuikly.core.render.web.runtime.miniapp.core

external val global: dynamic
external fun getCurrentPages(): Any
external fun getApp(): Any
external fun Page(params: Any): Any
external fun App(params: dynamic): Any
external fun Component(): Any
external fun encodeURIComponent(params: String): String
external fun decodeURIComponent(params: String): String
interface MpInstance {
    val config: Map<String, Any>
    val setData: (data: Any, cb: () -> Unit) -> Unit
    val data: Any
    val selectComponent: (selector: String) -> Any
}

/**
 * Mini program Native interface
 */
object NativeApi {
    private val isWx: Boolean = js("typeof wx !== undefined").unsafeCast<Boolean>()

    /**
     * Use js regex to transform camelCase, more efficient
     */
    private val jsToCamelCase = js(
        "function toCamelCase(str) { " +
                "return str.replace(/-(\\w)/g, function(match, letter) { return letter.toUpperCase()});" +
                "}"
    )


    val plat: dynamic by lazy {
        val retPlat = if (isWx) {
            js("wx")
        } else {
            js("qq")
        }
        retPlat
    }

    fun getWindowInfo(): dynamic = plat["getWindowInfo"]()

    fun getDeviceInfo(): dynamic = plat["getDeviceInfo"]()

    fun getAppBaseInfo(): dynamic = plat["getAppBaseInfo"]()

    fun createCanvasContext(canvasId: String): dynamic = plat["createCanvasContext"](canvasId)

    fun toCamelCase(str: String): String = jsToCamelCase(str).unsafeCast<String>()

    fun getVideoContext(id: String): dynamic = plat.createVideoContext(id)
}
