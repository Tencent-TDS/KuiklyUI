@file:JsExport

package com.tencent.kuikly.core.render.web.runtime.web.expand

import kotlin.js.JsExport
import kotlin.js.JsName
import com.tencent.kuikly.core.render.web.collection.FastMutableMap
import com.tencent.kuikly.core.render.web.ktx.SizeI

/**
 * JS Interop Helper Functions
 * 
 * This file contains helper classes and functions for JavaScript interoperability.
 * All exports use @JsExport and @JsName to ensure stable, predictable names in JS.
 */

/**
 * Create SizeI (Pair<Int, Int>) for JS interop
 */
@JsExport
@JsName("SizeI")
fun createSizeI(width: Int, height: Int): SizeI = Pair(width, height)

/**
 * Create an empty Kotlin List for JS interop
 */
@JsExport
@JsName("emptyList")
fun emptyListForJs(): List<Any> = emptyList()

/**
 * Convert a JS object to a Kotlin Map using FastMutableMap
 */
@JsExport
@JsName("jsObjectToMap")
fun jsObjectToMap(jsObject: dynamic, keys: Array<String>): MutableMap<String, Any> {
    val map = FastMutableMap<String, Any>(jsObject)
    return map
}
