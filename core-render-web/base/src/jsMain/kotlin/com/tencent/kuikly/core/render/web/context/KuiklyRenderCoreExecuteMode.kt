@file:JsExport

package com.tencent.kuikly.core.render.web.context

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Kuikly module loading mode, Web is JS
 */
@JsExport
@JsName("KuiklyRenderCoreExecuteMode")
enum class KuiklyRenderCoreExecuteMode(val mode: Int) {
    JVM(0),

    // Framework omitted
    JS(2),
    DEX(3),
    SO(4)
}
