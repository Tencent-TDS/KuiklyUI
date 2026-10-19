package com.tencent.kuikly.compose.ui.graphics

/**
 * Kotlin/JS `Long` is a `{low, high}` object. `toULong()`, `and` and `shl` each
 * allocate one. sRGB colors only need the ARGB bits in `high`, so construct that
 * one object directly. The constructor is not visible to Kotlin, so take it from
 * a single cached `0L`.
 */
private val longConstructor: dynamic = (0L.asDynamic()).constructor

private val newLongHigh: dynamic = js(
    """
    (function () {
        return function (ctor, high) { return new ctor(0, high); };
    })()
    """
)

internal actual fun packSrgbColor(argb: Int): ULong {
    // unsafeCast to ULong makes the compiler read a box field. Long.toULong()
    // is the inline ULong constructor, which on JS is the same object.
    val raw = newLongHigh(longConstructor, argb).unsafeCast<Long>()
    return raw.toULong()
}
