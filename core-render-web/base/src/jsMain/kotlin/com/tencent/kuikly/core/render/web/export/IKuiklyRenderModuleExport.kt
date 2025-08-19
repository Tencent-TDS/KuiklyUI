package com.tencent.kuikly.core.render.web.export

import com.tencent.kuikly.core.render.web.IKuiklyRenderContext
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback

/**
 * Module component protocol, modules expose themselves to kuikly by implementing [IKuiklyRenderModuleExport]
 */
interface IKuiklyRenderModuleExport {
    /**
     * KuiklyRender context
     */
    var kuiklyRenderContext: IKuiklyRenderContext?

    /**
     * Call instance methods of current module, module calls support both synchronous and asynchronous.
     * 1. If module method is synchronous on kotlin side, return value is returned via return
     * 2. If module method is asynchronous on kotlin side, return value is returned via callback
     *
     * @param method Method name
     * @param params Request parameters (passed through kotlin side data, can be String, Array, ByteArray, Int, Float)
     * @param callback Callback method
     *
     * @return If method is synchronous, return value is valid, can be String, Array, ByteArray, Int, Float
     */
    fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {
        if (params == null || params is String) {
            return call(method, params.unsafeCast<String?>(), callback)
        }
        return null
    }

    fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? = null

    /**
     * Called when module is destroyed, used to clean up module resources
     */
    fun onDestroy() {

    }
}
