package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import kotlin.js.Json
import kotlin.js.json

/**
 * Convert Touch parameters to a specified format
 */
fun getTouchParams(params: dynamic): Json {
    val touchX = params.changedTouches[0]?.clientX ?: 0f
    val touchY = params.changedTouches[0]?.clientY ?: 0f
    val pageX = params.changedTouches[0]?.pageX ?: 0f
    val pageY = params.changedTouches[0]?.pageY ?: 0f

    return json(
        "x" to touchX,
        "y" to touchY,
        "pageX" to pageX,
        "pageY" to pageY,
    )
}