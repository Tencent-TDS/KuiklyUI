@file:JsExport

package com.tencent.kuikly.core.render.web.runtime.web.expand

import kotlin.js.JsExport
import kotlin.js.JsName
import com.tencent.kuikly.core.render.web.collection.FastMutableMap
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.ktx.setCommonProp
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import org.w3c.dom.Element

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

/**
 * Stable JS bridge for common prop handling.
 */
@JsExport
@JsName("setCommonProp")
fun setCommonPropForJs(element: Element, key: String, value: Any): Boolean {
    return element.setCommonProp(key, value)
}

/**
 * Set global switch for contextmenu default behavior.
 */
@JsExport
@JsName("setPreventDefaultContextMenu")
fun setPreventDefaultContextMenu(preventDefault: Boolean) {
    KuiklyProcessor.preventDefaultContextMenu = preventDefault
}

/**
 * Set global switch for selection default behavior.
 */
@JsExport
@JsName("setPreventDefaultSelect")
fun setPreventDefaultSelect(preventDefault: Boolean) {
    KuiklyProcessor.preventDefaultSelect = preventDefault
}

/**
 * Set global switch for drag default behavior.
 */
@JsExport
@JsName("setPreventDefaultDrag")
fun setPreventDefaultDrag(preventDefault: Boolean) {
    KuiklyProcessor.preventDefaultDrag = preventDefault
}

/**
 * Set global switch for drag and selection default behavior.
 */
@JsExport
@JsName("setPreventDefaultDragAndSelect")
fun setPreventDefaultDragAndSelect(preventDefault: Boolean) {
    KuiklyProcessor.preventDefaultDragAndSelect = preventDefault
}

/**
 * Set global switch for auto updating root view size on resize.
 */
@JsExport
@JsName("setAutoUpdateRootViewSizeOnResize")
fun setAutoUpdateRootViewSizeOnResize(autoUpdate: Boolean) {
    KuiklyProcessor.autoUpdateRootViewSizeOnResize = autoUpdate
}
