@file:JsExport

package com.tencent.kuikly.core.render.web.exception

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Error reason
 */
@JsExport
@JsName("ErrorReason")
enum class ErrorReason {
    UNKNOWN,
    INITIALIZE,
    CALL_KOTLIN,
    CALL_NATIVE,
    UPDATE_VIEW_TREE
}

class KuiklyRenderModuleExportException(message: String) : Exception(message)

class KuiklyRenderViewExportException(message: String) : Exception(message)

class KuiklyRenderShadowExportException(message: String) : Exception(message)
