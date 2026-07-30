package com.tencent.kuikly.core.render.web.utils

import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow

/**
 * Safely call matchMedia on kuiklyWindow.
 * Returns false when matchMedia is not available (e.g. in mini-program environments).
 */
fun safeMatchMedia(query: String): Boolean {
    val matchMedia = kuiklyWindow.asDynamic().matchMedia ?: return false
    return matchMedia.call(kuiklyWindow, query).matches.unsafeCast<Boolean>()
}
