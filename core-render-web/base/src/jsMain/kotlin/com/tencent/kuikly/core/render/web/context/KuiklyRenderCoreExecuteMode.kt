package com.tencent.kuikly.core.render.web.context

/**
 * Kuikly module loading mode, Web is JS
 */
enum class KuiklyRenderCoreExecuteMode(val mode: Int) {
    JVM(0),

    // Framework omitted
    JS(2),
    DEX(3),
    SO(4)
}
