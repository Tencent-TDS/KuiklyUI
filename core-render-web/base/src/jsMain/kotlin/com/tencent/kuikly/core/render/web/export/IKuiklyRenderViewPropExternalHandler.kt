package com.tencent.kuikly.core.render.web.export

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * [IKuiklyRenderViewExport] Custom Prop Handler
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
interface IKuiklyRenderViewPropExternalHandler {

    /**
     * set custom prop
     *
     * @param renderViewExport
     * @param propKey
     * @param propValue
     *
     * @return should handle prop
     */
    @JsName("setViewExternalProp")
    fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean

    /**
     * reset prop，only call when [IKuiklyRenderViewExport] is reusable
     *
     * @param renderViewExport
     * @param propKey
     *
     * @return  handle prop reset yes or no
     */
    @JsName("resetViewExternalProp")
    fun resetViewExternalProp(renderViewExport: IKuiklyRenderViewExport, propKey: String): Boolean
}
