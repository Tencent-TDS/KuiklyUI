package com.tencent.kuikly.core.render.web.ktx

import com.tencent.kuikly.core.render.web.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.context.KuiklyRenderNativeMethod
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Window

// kuikly global document, same interface as web document
external var kuiklyDocument: Document

// kuikly global window, same interface as web window
external var kuiklyWindow: Window

// View size alias
typealias SizeF = Pair<Float, Float>

// View size width
val Pair<Float, Float>.width: Float
    get() = first

// View size height
val Pair<Float, Float>.height: Float
    get() = second

// Integer size alias
typealias SizeI = Pair<Int, Int>

// Integer size width
val Pair<Int, Int>.width: Int
    get() = first

// Integer size height
val Pair<Int, Int>.height: Int
    get() = second

// DOM parameter alias
typealias Frame = DOMRect

// Kuikly core task type
typealias KuiklyRenderCoreTask = () -> Unit

// Pre-run kuikly core task type
typealias PreRunKuiklyRenderCoreTask = () -> Unit

// Kuikly native method callback type
typealias KuiklyRenderNativeMethodCallback = (methodId: KuiklyRenderNativeMethod, args: JsArray<Any?>) -> Any?

// Element size definition
class DOMRect(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var width: Double = 0.0,
    var height: Double = 0.0
) {
    // Calculate left boundary of rectangle
    val left: Double
        get() = x

    // Calculate top boundary of rectangle
    val top: Double
        get() = y

    // Calculate right boundary of rectangle
    val right: Double
        get() = x + width

    // Calculate bottom boundary of rectangle
    val bottom: Double
        get() = y + height

    // Provide a method to print rectangle information
    override fun toString(): String =
        "DOMRect(x=$x, y=$y, width=$width, height=$height, left=$left, top=$top, right=$right, bottom=$bottom)"

}

/**
 * Kuikly rendering callback method
 */
class KuiklyRenderCallback(private val cb: (result: Any?) -> Unit) {
    operator fun invoke(result: Any?) {
        cb.invoke(result)
    }
}

/**
 * Convert [Map] to [JSONObject]
 */
internal fun Map<String, Any>.toJSONObject(): JSONObject {
    val serializationObject = JSONObject()
    forEach { (key, value) ->
        when (value) {
            is Int -> {
                serializationObject.put(key, value)
            }

            is Long -> {
                serializationObject.put(key, value)
            }

            is Double -> {
                serializationObject.put(key, value)
            }

            is Float -> {
                serializationObject.put(key, value)
            }

            is String -> {
                serializationObject.put(key, value)
            }

            is Boolean -> {
                serializationObject.put(key, value)
            }

            is Map<*, *> -> {
                val map = value.unsafeCast<Map<String, Any>>()
                serializationObject.put(key, map.toJSONObject())
            }

            is List<*> -> {
                val list = value.unsafeCast<List<Any>>()
                serializationObject.put(key, list.toJSONArray())
            }

        }
    }
    return serializationObject
}

/**
 * Convert [List] to [JSONArray]
 */
internal fun List<Any>.toJSONArray(): JSONArray {
    val serializationArray = JSONArray()
    forEach { value ->
        when (value) {
            is Int -> {
                serializationArray.put(value)
            }

            is Long -> {
                serializationArray.put(value)
            }

            is Float -> {
                serializationArray.put(value)
            }

            is Double -> {
                serializationArray.put(value)
            }

            is String -> {
                serializationArray.put(value)
            }

            is Boolean -> {
                serializationArray.put(value)
            }

            is Map<*, *> -> {
                val map = value.unsafeCast<Map<String, Any>>()
                serializationArray.put(map.toJSONObject())
            }

            is List<*> -> {
                val list = value.unsafeCast<List<Any>>()
                serializationArray.put(list.toJSONArray())
            }
        }
    }
    return serializationArray
}


fun Any.toPxF(): String = toString() + "px"

fun Any.toNumberFloat(): Float = (this as Number).toFloat()

/**
 * Safely convert string to JSONObject
 */
fun String?.toJSONObjectSafely(): JSONObject = JSONObject(this ?: "{}")

fun String.pxToFloat(): Float {
    val result = this.replace("px", "")

    return if (result.isEmpty()) 0f else result.toFloat()
}

/**
 * Convert string type of Any to floating point number
 */
fun String.pxToDouble(): Double {
    val result = this.replace("px", "")

    return if (result.isEmpty()) 0.0 else result.toDouble()
}

/**
 * Convert string to hexadecimal color
 * @return Color value
 */
fun String.toColor(): Long =
    KuiklyRenderAdapterManager.krColorParseAdapter?.toColor(this) ?: toLong()


/**
 * Format text content, currently no processing requirements
 */
fun parseTextContent(text: String): String {
    return text.asDynamic()
        .unsafeCast<String>()
}

/**
 * Split canvas color definitions and return, format like "rgba(255,255,0,1) 0,rgba(255,0,0,1) 1",
 * need to split and process
 */
fun splitCanvasColorDefinitions(colorString: String): JsArray<String> {
    val result = JsArray<String>()
    var currentPart = StringBuilder()
    var bracketCount = 0

    for (char in colorString) {
        when (char) {
            '(' -> {
                bracketCount++
                currentPart.append(char)
            }

            ')' -> {
                bracketCount--
                currentPart.append(char)
            }

            ',' -> {
                if (bracketCount == 0) {
                    // This is a comma outside brackets, used to separate color definitions
                    result.add(currentPart.toString().trim())
                    currentPart = StringBuilder()
                } else {
                    // This is a comma inside brackets, part of rgba function
                    currentPart.append(char)
                }
            }

            else -> currentPart.append(char)
        }
    }

    // Add last part
    if (currentPart.isNotEmpty()) {
        result.add(currentPart.toString().trim())
    }

    return result
}

fun String.toRgbColor(): String {
    if (isEmpty()) {
        return this
    }

    // set direct if is rgba color format
    if (this.startsWith("rgba")) {
        return this
    }

    // handle by color adapter if exists
    var color = if (KuiklyRenderAdapterManager.krColorParseAdapter != null) {
        // handle by adapter
        KuiklyRenderAdapterManager.krColorParseAdapter?.toColor(this)
    } else {
        toLongOrNull()
    }
    if (color == null) {
        // default by black
        color = 0L
    }

    // alpha channel, web platform should divide 255 to get the actual alpha
    val alpha = ((color shr 24) and 0xFF).toFloat() / 255
    val red = (color shr 16) and 0xFF
    val green = (color shr 8) and 0xFF
    val blue = color and 0xFF

    return "rgba($red, $green, $blue, $alpha)"
}

/**
 * determine if string is all digits
 */
fun String.isAllDigits(): Boolean = this.all { it.isDigit() }

/**
 * get string to double or default
 */
fun String?.toDoubleOrDefault(default: Double): Double =
    this?.takeIf { it.isNotEmpty() }?.toDoubleOrNull() ?: default