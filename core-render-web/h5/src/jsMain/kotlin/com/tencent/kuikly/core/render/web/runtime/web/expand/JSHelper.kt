@file:JsExport

package com.tencent.kuikly.core.render.web.runtime.web.expand

import kotlin.js.JsExport
import kotlin.js.JsName
import com.tencent.kuikly.core.render.web.collection.FastMutableMap
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.ktx.setCommonProp
import com.tencent.kuikly.core.render.web.processor.IImageProcessor
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import org.w3c.dom.Element
import org.w3c.dom.HTMLImageElement

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
 * Internal helpers wrapping raw `js(...)` calls.
 *
 * IMPORTANT: Do NOT inline `js(...)` blocks inside functions annotated with
 * `@JsExport`. Kotlin/JS 1.9.22 has a known code-gen bug: when an exported
 * function body contains multiple `js(...)` blocks, the compiler outlines
 * them into a helper (e.g. `jsValueToKotlin$outlinedJsCode$`) but sometimes
 * DCE's the helper definition while still emitting the module-level export
 * assignment referencing it, producing runtime `ReferenceError:
 * jsValueToKotlin$outlinedJsCode$ is not defined` when the bundle loads.
 *
 * Wrapping each `js(...)` block in a private, non-`@JsExport` function keeps
 * exported function bodies free of raw `js(...)` and avoids the outlining
 * path entirely.
 */
private fun newEmptyJsObject(): dynamic = js("({})")

private fun isJsArray(value: dynamic): Boolean = js("Array.isArray(value)").unsafeCast<Boolean>()

private fun jsObjectOwnKeys(value: dynamic): Array<String> =
    js("Object.keys(value)").unsafeCast<Array<String>>()

private fun hasImageProcessorRequiredMethods(jsProcessor: dynamic): Boolean {
    return jsTypeOf(jsProcessor.getImageAssetsSource) == "function" &&
        jsTypeOf(jsProcessor.isSVGFilterSupported) == "function" &&
        jsTypeOf(jsProcessor.applyTintColor) == "function"
}

/**
 * Convert a JS object to a Kotlin Map.
 *
 * Notes:
 * - This keeps backward compatibility for existing JS calls.
 * - Nested JS objects/arrays are recursively converted to Kotlin Map/List.
 */
@JsExport
@JsName("jsObjectToMap")
@Suppress("UNUSED_PARAMETER")
fun jsObjectToMap(jsObject: Any?, keys: Array<String> = emptyArray()): MutableMap<String, Any?> {
    val converted = jsValueToKotlin(jsObject)
    return if (converted is MutableMap<*, *>) {
        converted.unsafeCast<MutableMap<String, Any?>>()
    } else {
        FastMutableMap<String, Any?>(newEmptyJsObject())
    }
}

/**
 * Convert a JS array to a Kotlin List with recursive conversion.
 */
@JsExport
@JsName("jsArrayToList")
fun jsArrayToList(jsArray: Array<Any?>): List<Any?> {
    return jsArray.map { value ->
        jsValueToKotlin(value)
    }
}

/**
 * Convert a JS value to Kotlin value recursively.
 * - JS Object => Kotlin MutableMap<String, Any?>
 * - JS Array  => Kotlin List<Any?>
 * - Primitive => unchanged
 *
 * NOTE: All `js(...)` blocks are intentionally delegated to the private
 * helpers above to avoid a Kotlin/JS 1.9.22 code-gen bug on `@JsExport`
 * functions (see comment on `newEmptyJsObject`).
 */
@JsExport
@JsName("jsValueToKotlin")
fun jsValueToKotlin(value: Any?): Any? {
    if (value == null) {
        return null
    }

    val dynamicValue = value.asDynamic()
    if (jsTypeOf(dynamicValue) != "object") {
        return value
    }

    if (isJsArray(dynamicValue)) {
        val arrayValue = dynamicValue.unsafeCast<Array<Any?>>()
        return jsArrayToList(arrayValue)
    }

    val map = FastMutableMap<String, Any?>(newEmptyJsObject())
    val keys = jsObjectOwnKeys(dynamicValue)
    keys.forEach { key ->
        map[key] = jsValueToKotlin(dynamicValue[key])
    }
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
 * Stable JS bridge for invoking KuiklyRenderCallback.
 * This avoids JS depending on Kotlin compiler generated internal field names.
 */
@JsExport
@JsName("invokeKuiklyRenderCallback")
fun invokeKuiklyRenderCallback(callback: dynamic, result: dynamic): Boolean {
    if (callback == null) {
        return false
    }
    return runCatching {
        callback.unsafeCast<KuiklyRenderCallback>().invoke(result)
        true
    }.getOrElse { false }
}

/**
 * Register a JavaScript image processor with Kuikly.
 *
 * Returns true when registration succeeds, false when input is invalid.
 */
@JsExport
@JsName("setImageProcessor")
fun setImageProcessor(imageProcessor: Any?): Boolean {
    if (imageProcessor == null) {
        return false
    }

    val jsProcessor = imageProcessor.asDynamic()
    if (!hasImageProcessorRequiredMethods(jsProcessor)) {
        return false
    }

    KuiklyProcessor.imageProcessor = object : IImageProcessor {
        override fun getImageAssetsSource(src: String): String {
            return runCatching {
                jsProcessor.getImageAssetsSource(src).unsafeCast<String>()
            }.getOrElse { src }
        }

        override fun isSVGFilterSupported(): Boolean {
            return runCatching {
                jsProcessor.isSVGFilterSupported().unsafeCast<Boolean>()
            }.getOrElse { false }
        }

        override fun applyTintColor(imageElement: HTMLImageElement, tintColorValue: String, frameHeight: Double) {
            runCatching {
                jsProcessor.applyTintColor(imageElement, tintColorValue, frameHeight)
            }
        }
    }

    return true
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
