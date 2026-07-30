package com.tencent.kuikly.core.render.web.runtime.miniapp.ktx

import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement


// 全局对象
external var globalThis: dynamic

/**
 * Get data associated with MiniElement by key
 * @param T Data type associated with key
 * @param key Key for associated data
 * @return Associated data
 */
fun <T> MiniElement.getViewData(key: String): T? = this.asDynamic()[key].unsafeCast<T?>()

/**
 * Associate key with value and save to View
 * @param key Key to associate
 * @param value Data to associate
 */
fun MiniElement.putViewData(key: String, value: Any) {
    this.asDynamic()[key] = value
}

/**
 * Remove data associated with key
 * @param T Data type
 * @param key Key to remove
 * @return Removed data
 */
fun <T> MiniElement.removeViewData(key: String): T? {
    this.asDynamic()[key] = null

    return null
}